package ru.sosgps.wayrecall.payment


import java.nio.file.{Paths, Path}
import java.util.Properties
import javax.annotation.PostConstruct
import org.eclipse.jetty.server._
import org.eclipse.jetty.webapp.WebAppContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.web.context.support.GenericWebApplicationContext
import ru.sosgps.wayrecall.initialization.{MultiserverConfig, SSLConnector}
import scala.beans.BeanProperty


/**
 * Created by IVAN on 29.02.2016.
 */
class PaymentServer extends grizzled.slf4j.Logging {

  @Autowired
  var appContext: ApplicationContext = null;

  @BeanProperty
  var port: Int = -1

  @BeanProperty
  var instanceName: String = null

  def this(port: Int, instanceName: String){
    this()
    this.port=port
    this.instanceName=instanceName
  }

  @PostConstruct
  def startJetty: Unit  = {
    if(port<0) {
      debug("Payment server undefined for instance: \""+instanceName+"\"")
      return
    }
    val server = new Server()

    val confPath: Path = Paths.get(System.getenv("WAYRECALL_HOME"), "conf")
    val mconfig: MultiserverConfig = new MultiserverConfig(confPath)
    val globalProperties: Properties = mconfig.gloabalProps

    val sslConnector = SSLConnector(server,port,globalProperties)
    server.addConnector(sslConnector)

    val rootPath = this.getClass.getClassLoader()
    val webxmlPath=rootPath.getResource("paymentwebapp" + "/WEB-INF/web.xml").toExternalForm;

    val webapp = new WebAppContext();
    webapp.setContextPath("/");
    webapp.setDescriptor(webxmlPath)
    webapp.setResourceBase(rootPath.getResource("paymentwebapp").toExternalForm)
    val webApplicationContext = new GenericWebApplicationContext()
    if (appContext != null)
      webApplicationContext.setParent(appContext);
    webapp.addEventListener(
      new org.springframework.web.context.ContextLoaderListener(webApplicationContext)
    );
    server.setHandler(webapp);

    debug("PaymentServer started for instance \""+instanceName+"\" on port: "+port)
    server.setStopAtShutdown(true)
    server.start
  }
}

