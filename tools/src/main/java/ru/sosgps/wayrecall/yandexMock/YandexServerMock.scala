package ru.sosgps.wayrecall.yandexMock


import java.nio.file.{Path, Paths}
import com.beust.jcommander.{Parameter, Parameters}
import org.eclipse.jetty.servlet.{ServletHolder, ServletHandler}
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server._
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.springframework.context.support.ClassPathXmlApplicationContext
import ru.sosgps.wayrecall.initialization.MultiserverConfig
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import ru.sosgps.wayrecall.payment.PaymentParams

/**
 * Created by IVAN on 08.04.2016.
 */

@Parameters(commandDescription = "Yandex-Server mock (YSM) service for testing payment features")
object YandexServerMock extends CliCommand with grizzled.slf4j.Logging {

  override val commandName: String = "startYSM"

  @Parameter(names = Array("-p", "--port"), required = false, description = "YSM's port")
  var port: Integer = 5196

  @Parameter(names = Array("-ho", "--host"), required = false, description = "YSM's hostname")
  var host: String = "localhost"

  @Parameter(names = Array("-ksp", "--keystorePassword"), required = false, description = "Password for yandex-mock keystore")
  var keystorePassword: String = "ksbstels"

  @Parameter(names = Array("-ksrp", "--keystoreRelativePath"), required = false, description = "Relative path from 'conf/' to yandex-mock keystore")
  var keystoreRelativePath: String = "../security/yandex-mock.keystore"

  @Parameter(names = Array("-su", "--shopUrl"), required = false, description = "Wayrecall url in format ( http:///host:port or https://host:port )")
  var shopUrl: String = "https://localhost:5194"

  @Parameter(names = Array("-pu", "--paymentUrl"), required = false, description = "Wayrecall payment system url in format ( host:port )")
  var paymentUrl: String = "localhost:5195"

  @Parameter(names = Array("-sp", "--shopPassword"), required = false, description = "Wayrecall payment system's shop password")
  var shopPassword: String = "secret"

  var yandexMockContext: ClassPathXmlApplicationContext = null

  var sslContextFactory: SslContextFactory = null

  def process() {
    if (port== null || port<= 0) {
      debug("Yandex server undefined")
      return
    }
    initYandexMockContext
    val shopParams=new ShopParams(host+":"+port,shopUrl,paymentUrl,shopPassword)
    yandexMockContext.getBeanFactory.registerSingleton("shopParams",shopParams)
    startJetty
  }

  def initYandexMockContext {
    yandexMockContext = new ClassPathXmlApplicationContext()
    yandexMockContext.setConfigLocation("yandex-mock-config.xml")
    yandexMockContext.refresh();
  }

  def startJetty {
    val server = new Server()

    val https_config: HttpConfiguration = new HttpConfiguration
    https_config.setSecureScheme("https")
    https_config.setSecurePort(port)
    val src: SecureRequestCustomizer = new SecureRequestCustomizer
    //src.setStsMaxAge(2000);
    //src.setStsIncludeSubDomains(true);
    https_config.addCustomizer(src)


    val confPath: Path = Paths.get(System.getenv("WAYRECALL_HOME"), "conf")
    val keystoreFilePath: Path = Paths.get(confPath.toString, keystoreRelativePath).toAbsolutePath

    sslContextFactory = new SslContextFactory
    sslContextFactory.setKeyStorePath(keystoreFilePath.toString)
    sslContextFactory.setKeyStorePassword(keystorePassword)
    //sslContextFactory.setTrustAll(true)

    val sslConnector: ServerConnector = new ServerConnector(server,
      new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString),
      new HttpConnectionFactory(https_config))

    sslConnector.setPort(port)
    sslConnector.setIdleTimeout(180000)

    server.addConnector(sslConnector)

    val handler = new ServletHandler();
    val paymentFormServlet = yandexMockContext.getBean("paymentFormServlet").asInstanceOf[PaymentFormServlet]
    val processPaymentServlet = yandexMockContext.getBean("processPaymentServlet").asInstanceOf[ProcessPaymentServlet]
    handler.addServletWithMapping(new ServletHolder(paymentFormServlet), "/getPaymentForm")
    handler.addServletWithMapping(new ServletHolder(processPaymentServlet), "/processPayment")
    server.setHandler(handler);

    server.setStopAtShutdown(true)
    server.start
    debug("YandexServerMock started")
  }
}

class ShopParams(val yandexMockUrl:String,val shopUrl: String, val paymentUrl: String, val shopPassword: String)