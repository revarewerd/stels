package ru.sosgps.wayrecall.initialization

import java.nio.file.{Files, Path, Paths}
import java.util.Properties

import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server._
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * Must be set he environment variable - WAYRECALL_HOME
 * set needClientAuth = true if connector needs client authentication
 */

object SSLConnector extends grizzled.slf4j.Logging {
  def apply(server: Server,port:Int,properties:Properties,needClientAuth:Boolean) : ServerConnector  = {
    val https_config: HttpConfiguration = new HttpConfiguration
    https_config.setSecureScheme("https")
    https_config.setSecurePort(port)

    val src: SecureRequestCustomizer = new SecureRequestCustomizer()

    /*  This option needs for HTTP Strict Transport Security
      to use this need update Jetty for v9.3+ but it works unstable (correct works only in Chrome)
      some problems with http1.1/http2 compatibility (wrong header etc.) */
    //src.setStsMaxAge(60);

    https_config.addCustomizer(src)


    val sslContextFactory: SslContextFactory = new SslContextFactory

    val keyStorePassword: String = properties.getProperty("global.security.keystorepassword")
    val keyStoreFilePath: Path = Paths.get(System.getenv("WAYRECALL_HOME"), "security", "wayrecall.keystore")
    if (Files.exists(keyStoreFilePath)) {
      sslContextFactory.setKeyStorePath(keyStoreFilePath.toAbsolutePath.toString)
    }
    sslContextFactory.setKeyStorePassword(keyStorePassword)

    if (needClientAuth) {
      sslContextFactory.setNeedClientAuth(true)
      sslContextFactory.setValidateCerts(true)
      sslContextFactory.setValidatePeerCerts(true)
    }

    val sslConnector: ServerConnector = new ServerConnector(
      server,
      new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString),
      new HttpConnectionFactory(https_config)
    )
    sslConnector.setPort(port)
    sslConnector.setIdleTimeout(180000)
    sslConnector
  }

  def apply (server:Server,port:Int,properties: Properties): ServerConnector = {
    apply(server,port,properties,false)
  }
}
