package ru.sosgps.wayrecall.initialization;

import kamon.Kamon;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.AnnotationParser;
import org.eclipse.jetty.nosql.mongodb.MongoSessionIdManager;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.context.support.XmlWebApplicationContext;
import ru.sosgps.wayrecall.core.InstanceConfig;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.utils.ScalaConverters;
import ru.sosgps.wayrecall.utils.errors.MailErrorReporter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

/**
 * Created by nickl on 31.03.14.
 */
public class JettyServer {
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JettyServer.class);

    final static ClassLoader webdir = JettyServer.class.getClassLoader();

    private static Server server;

    private static ThreadLocal<HttpServletRequest> currentRequest = new ThreadLocal<>();

    public static HttpServletRequest getCurrentRequest() {
        return currentRequest.get();
    }

    public static void main(String[] args) throws Exception {
        try {
            Kamon.start();
            //System.in.read();

            Path confPath = Paths.get(System.getenv("WAYRECALL_HOME"), "conf");

            final MultiserverConfig mconfig = new MultiserverConfig(confPath);

            server = new Server();

            ServerConnector httpConnector = new ServerConnector(server);

            final long startTime = System.currentTimeMillis();

            int port = 5193;
            if (args.length > 0)
                port = Integer.parseInt(args[0]);

            httpConnector.setPort(port);
            httpConnector.setIdleTimeout(180000);

            Properties globalProperties = mconfig.gloabalProps();
            int securePort = Integer.parseInt(globalProperties.getProperty("global.security.port"));

            ServerConnector sslConnector = SSLConnector.apply(server, securePort, globalProperties);
            server.setConnectors(new Connector[]{httpConnector, sslConnector});

            Context ctx = new InitialContext();
            ctx.createSubcontext("java:global").createSubcontext("env").createSubcontext("mdbm");
            for (InstanceConfig cfg : ScalaConverters.asJavaIterable(mconfig.instances())) {
                ctx.bind("java:global/env/mdbm/" + cfg.name(), new MongoDBManager(cfg));
            }

            final HandlerCollection handlerCollection = new ContextHandlerCollection() {
                @Override
                public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                    try {
                        currentRequest.set(request);
                        super.handle(target, baseRequest, request, response);
                    } finally {
                        currentRequest.remove();
                    }
                }
            };

            final ArrayList<ConfigurableApplicationContext> contexts = new ArrayList<>();

            ServletContextHandler servletContextHandler = new ServletContextHandler();
            servletContextHandler.setContextPath("/info");
            servletContextHandler.addServlet(new ServletHolder(new VhostLister(mconfig)), "/vhostslist");
            handlerCollection.addHandler(servletContextHandler);

            LetsEncryptRenewer letsEncryptRenewer = new LetsEncryptRenewer(globalProperties);
            ServletContextHandler acmeContextHandler = new ServletContextHandler();
            acmeContextHandler.setContextPath(letsEncryptRenewer.httpChallengePath());
            acmeContextHandler.addServlet(new ServletHolder(letsEncryptRenewer.httpChallengeServlet()), "/");
            handlerCollection.addHandler(acmeContextHandler);

            server.setHandler(handlerCollection);

            log.debug("jettyServer started");
            server.setStopAtShutdown(true);
            server.start();

            if (letsEncryptRenewer.updateCertificate()) {
                log.info("letsEncrypt certificate updated, creating new the \"wayrecall.keystore\"");

                String password = globalProperties.getProperty("global.security.keystorepassword");
                KeyStore keyStore = Pem2KeyStoreUtil.createKeyStore(
                        letsEncryptRenewer.getPrivateKeyFile(),
                        letsEncryptRenewer.getCertChainFile(),
                        password
                );

                try (FileOutputStream outputStream = new FileOutputStream(new File(letsEncryptRenewer.keyStoreDir, "wayrecall.keystore"))) {
                    keyStore.store(outputStream, password.toCharArray());
                }

                log.info("\"wayrecall.keystore\" created need to restart to use the new certificate");
            }


            try {
                for (final InstanceConfig insconf : ScalaConverters.asJavaIterable(mconfig.instances())) {
                    try {
                        log.info("initializing:{}", insconf.name());
                        AbstractRefreshableConfigApplicationContext mainContext = configureContext(insconf, mconfig.gloabalProps());
                        contexts.add(mainContext);
                        setupWrcInstance(handlerCollection, insconf, mainContext);
                    } catch (Exception e) {
                        log.error("error initializing " + insconf, e);
                        throw e;
                    }
                }

                log.info("Server started in: {}", (System.currentTimeMillis() - startTime));
                server.join();

            } finally {
                for (ConfigurableApplicationContext o : contexts) {
                    o.close();
                }
                Kamon.shutdown();
            }
        }catch (Throwable e){
            log.error("initialisation error", e);
        }

    }


    public static void setupWrcInstance(HandlerCollection handlerCollection, InstanceConfig config, ApplicationContext mainContext) throws Exception {

        MailErrorReporter mailSender = mainContext.getBean(MailErrorReporter.class);
        //String errorMail = mainContext.getEnvironment().getProperty("global.errornotificationAddress");
        ErrorHandler errorHandler = new WrcJettyErrorHandler(mailSender);

        final WebAppContext billingwebapp = configureWebApp("/billing", mainContext,
                config, "billingwebapp"
        );

        billingwebapp.setErrorHandler(errorHandler);
        final WebAppContext monitoringwebapp = configureWebApp("/", mainContext,
                config, "monitoringwebapp");
        monitoringwebapp.setErrorHandler(errorHandler);
        final WebAppContext workflowapp = configureWebApp("/workflow", mainContext,
                config, "workflowapp");
        workflowapp.setErrorHandler(errorHandler);

        Map<String, Handler> additionalHandlers = mainContext.getBeansOfType(Handler.class);

        log.info("additionalHandlers = " + additionalHandlers);

        for (Handler handler : additionalHandlers.values()) {
            handlerCollection.addHandler(handler);
            handler.start();
        }

        handlerCollection.addHandler(billingwebapp);
        handlerCollection.addHandler(monitoringwebapp);
        handlerCollection.addHandler(workflowapp);

        billingwebapp.start();
        monitoringwebapp.start();
        workflowapp.start();
    }

    public static WebAppContext configureWebApp(String contextPath, final ApplicationContext mainContext,
                                                InstanceConfig instanceConfig , final String webappPath) throws Exception {

        WebAppContext webapp = new WebAppContext();
        webapp.setParentLoaderPriority(true);
        WayrecallMongoSessionManager manager;
        final String sesBeanName = webappPath + "-SessionManager";
        try {
            manager = mainContext.getBean(sesBeanName, WayrecallMongoSessionManager.class);
        }
        catch (BeansException e){
            log.warn("cant load {} from mainContext, so using default", sesBeanName);
            manager = new WayrecallMongoSessionManager();
        }

        final String name = instanceConfig.name() + "-" + webappPath;
        final MongoSessionIdManager sessionIdManager = new MongoSessionIdManager(
                server,
                mainContext.getBean(MongoDBManager.class).getDatabase().getCollection(webappPath + "-sessions"));
        sessionIdManager.setScavengePeriod(120);
        sessionIdManager.setPurgeDelay(4 * 60 * 60 * 1000);
        sessionIdManager.setPurgeInvalidAge(6 * 60 * 60 * 1000);
        sessionIdManager.setWorkerName(name + "-worker"); //TODO: must be unique in cluster
        manager.instanceName = instanceConfig.name();
        manager.appname = webappPath;
        manager.setSessionIdManager(sessionIdManager);
        manager.setIdlePeriod(300);
        manager.setStalePeriod(60);

        manager.setSessionIdPathParameterName("none");

        manager.setName(name);
        manager.setSavePeriod(1);
        webapp.setSessionHandler(new SessionHandler(manager));

        log.debug("webapp.getSessionHandler().getSessionManager() = {}", webapp.getSessionHandler().getSessionManager());
        log.debug("webapp.getExtraClasspath() = {}", webapp.getExtraClasspath());

        webapp.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/target/classes");
        final AnnotationConfiguration annotationConfiguration = new AnnotationConfiguration() {
            @Override
            protected AnnotationParser createAnnotationParser() {
                log.debug("creatingAnnotationParser");
                return new MyAnnotationParser();
            }
        };

        webapp.setConfigurations(new Configuration[]{
                annotationConfiguration, new WebXmlConfiguration(),
                new WebInfConfiguration(),
                new PlusConfiguration(), new MetaInfConfiguration(),
                new FragmentConfiguration(), new EnvConfiguration()});
        webapp.setVirtualHosts(instanceConfig.vhosts());
        webapp.setContextPath(contextPath);


        final XmlWebApplicationContext xmlWebApplicationContext = new XmlWebApplicationContext();
        log.debug("XmlWebApplicationContext = {}", System.identityHashCode(xmlWebApplicationContext));
        xmlWebApplicationContext.setParent(mainContext);
        webapp.addEventListener(
                new org.springframework.web.context.ContextLoaderListener(xmlWebApplicationContext)
        );

        final String descriptor = webdir.getResource(webappPath + "/WEB-INF/web.xml").toExternalForm();
        log.debug("descriptor=" + descriptor);
        webapp.setDescriptor(descriptor);

        final String resourceBase =System.getProperty(
                "wayrecall.webappres." + webappPath,
                webdir.getResource(webappPath).toExternalForm()
        );
        log.debug("resourceBase=" + resourceBase);
        webapp.setResourceBase(resourceBase);
        return webapp;
    }

    static private AbstractRefreshableConfigApplicationContext configureContext(final InstanceConfig i, final Properties gloabalProps ){
        Path resolve = i.path().resolve("spring-main.xml");

        AbstractXmlApplicationContext mainContext = null;
        if (Files.exists(resolve)) {
            mainContext = new FileSystemXmlApplicationContext();
            mainContext.setConfigLocation(resolve.toAbsolutePath().toUri().toString());
        }
        else {
            mainContext = new ClassPathXmlApplicationContext();
            mainContext.setConfigLocation("/shared-beans-config.xml");
        }

        StandardEnvironment environment = new StandardEnvironment() {

            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                super.customizePropertySources(propertySources);
                propertySources.addLast(new PropertiesPropertySource("instance", i.properties()));
                propertySources.addLast(new PropertiesPropertySource("global", gloabalProps));
                Properties generatedProps = new Properties();
                generatedProps.setProperty("instance.name", i.name());
                propertySources.addLast(new PropertiesPropertySource("generated", generatedProps));
            }
        };

        mainContext.setEnvironment(environment);
        mainContext.addBeanFactoryPostProcessor(new BeanDefinitionRegistryPostProcessor() {
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {

            }

            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
                if (!beanFactory.containsBean("instanceConfig")) {
                    log.debug("beanFactory.registerSingleton(\"instanceConfig\", this)");
                    beanFactory.registerSingleton("instanceConfig", i);
                }
            }

        });
        // mainContext.getBeanFactory.registerSingleton("instanceConfig", this)
        mainContext.refresh();
        return mainContext;
    }


    private static File getSourceFolder() {
        return new File(JettyServer.class.getProtectionDomain().getCodeSource().getLocation().getFile())
                .getParentFile();
    }

    private static Properties loadProperties(Path props) throws IOException {
        final Properties source = new Properties();
        BufferedReader reader = Files.newBufferedReader(props, StandardCharsets.UTF_8);
        source.load(reader);
        reader.close();
        return source;
    }

    private static String[] readLines(Path vhosts2) throws IOException {
        BufferedReader vhosts1 = Files.newBufferedReader(vhosts2, StandardCharsets.UTF_8);
        ArrayList<String> strings = new ArrayList<>();
        String line;
        while ((line = vhosts1.readLine()) != null) {
            strings.add(line);
        }

        return strings.toArray(new String[strings.size()]);
    }
}
