package ru.sosgps.wayrecall.m2msms

import generated.{MTSX0020CommunicatorX0020M2MX0020XMLX0020API, MTSX0020CommunicatorX0020M2MX0020XMLX0020APISoap}
import java.security.MessageDigest
import org.apache.cxf.endpoint.Client
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.interceptor.{LoggingOutInterceptor, LoggingInInterceptor}
import scala.beans.BeanProperty
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy
import org.apache.cxf.transport.http.HTTPConduit

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.03.13
 * Time: 15:46
 * To change this template use File | Settings | File Templates.
 */
class M2MClientFactory {

  @BeanProperty
  var logRequests = false

  @BeanProperty
  val timeouts = 1000 * 60

  def createPort(): MTSX0020CommunicatorX0020M2MX0020XMLX0020APISoap = {

    val ss = new MTSX0020CommunicatorX0020M2MX0020XMLX0020API()
    val port = ss.getMTSX0020CommunicatorX0020M2MX0020XMLX0020APISoap12

    //var password: String = "e143f4685aa83d552c886a2a897a0c3c"

    val client: Client = ClientProxy.getClient(port)

    val http = client.getConduit().asInstanceOf[HTTPConduit];

    val httpClientPolicy = new HTTPClientPolicy();
    httpClientPolicy.setConnectionTimeout(timeouts);
    httpClientPolicy.setReceiveTimeout(timeouts);

    http.setClient(httpClientPolicy);

    if (logRequests) {
      client.getInInterceptors.add(new LoggingInInterceptor)
      client.getOutInterceptors.add(new LoggingOutInterceptor)
    }

    port

  }

}
