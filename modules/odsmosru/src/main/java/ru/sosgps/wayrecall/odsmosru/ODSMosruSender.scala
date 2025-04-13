package ru.sosgps.wayrecall.odsmosru

import java.nio.file.{Files, Paths}
import java.util
import java.util.GregorianCalendar
import java.net.URL
import java.util.concurrent.{Executor, Executors}
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PreDestroy
import javax.xml.datatype.DatatypeFactory
import javax.xml.namespace.QName
import javax.xml.ws.BindingProvider

import kamon.Kamon
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.interceptor.{LoggingInInterceptor, LoggingOutInterceptor}
import org.apache.cxf.transport.http.HTTPConduit
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.odsmosru.TemporaryStorage
import ru.sosgps.wayrecall.odsmosrutelemetry.{TelemetryBa, TelemetryDetailBa, TelemetryService, TelemetryService_Service}
import ru.sosgps.wayrecall.utils.concurrent.Semaphore

import scala.beans.BeanProperty
import scala.concurrent.Lock
import scala.util.control

/**
  * Created by nickl on 15.07.14.
  */
class ODSMosruSender extends grizzled.slf4j.Logging {

  @BeanProperty
  var userName = "KSBSTELS"

  @BeanProperty
  var password = "KSBSTELS"

  @BeanProperty
  //var url = "http://ods.mos.ru/telemetry/telemetryWebService?wsdl"
  var url: String = null

  @BeanProperty
  var timeOut = 5000

  private lazy val port: ru.sosgps.wayrecall.odsmosrutelemetry.TelemetryService = try {

    //val wsdlURL: URL = TelemetryService_Service.WSDL_LOCATION
    //require(wsdlURL != null,"wsdlURL was not initializeted")

    //assert(url == null, "there are issues with no-internal wsdl, just let it be default value ")

    val wsdlURL: URL = if (url != null) new URL(url) else TelemetryService_Service.WSDL_LOCATION

    val ss: TelemetryService_Service = new TelemetryService_Service(wsdlURL)
    //val ss: TelemetryService_Service = new TelemetryService_Service()
    val port: TelemetryService = ss.getTelemetryServiceImplPort
    val ctx: java.util.Map[String, AnyRef] = (port.asInstanceOf[BindingProvider]).getRequestContext
    ctx.put("ws-security.username", userName)
    ctx.put("ws-security.password", password)

    val client = ClientProxy.getClient(port);

    if (logger.isTraceEnabled) {
      client.getInInterceptors.add(new LoggingInInterceptor());
      client.getOutInterceptors.add(new LoggingOutInterceptor());
    }

    /*
     factory.getInInterceptors().add(new LoggingInInterceptor());
 factory.getOutInterceptors().add(new LoggingOutInterceptor());
     */

    val httpConduit = client.getConduit().asInstanceOf[HTTPConduit]

    val httpClientPolicy = new HTTPClientPolicy();
    info(s"setting timeout = $timeOut")
    httpClientPolicy.setConnectionTimeout(timeOut);
    httpClientPolicy.setReceiveTimeout(timeOut);

    httpConduit.setClient(httpClientPolicy);

    port

  } catch {
    case e: Throwable => {
      error("port init error", e)
      null
    }
  }

  def canSend = port != null

  def sendToOdsMosRu(gpsdata: GPSData) {
    if (gpsdata.containsLonLat()) {
      val ba = new TelemetryBa()
      ba.setCoordX(gpsdata.lon)
      ba.setCoordY(gpsdata.lat)
      ba.setSpeed(gpsdata.speed)

      val calendar = new GregorianCalendar()
      calendar.setTime(gpsdata.time)
      val impl = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar)
      ba.setDate(impl)

      ba.setGpsCode(gpsdata.imei)

      val list = new util.ArrayList[TelemetryDetailBa]()
      port.storeTelemetry(ba, list)
      Kamon.metrics.counter("odsmosru-storetelemetry").increment()
    }
    else {
      debug("gpsdata does not contain lonlat")
    }
  }
}


class RepeatedlyODSMosruSender(executor: Executor, sendingExecutor: Executor, lim: Int) extends ODSMosruSender {

  def this(lim: Int) = this(Executors.newSingleThreadExecutor(), Executors.newFixedThreadPool(lim), lim)

  def this() = this(5)

  private val storage = new TemporaryStorage(Files.createDirectories(Paths.get("odsmosrutempStore")))
  val resender = new RepeatedlyResender(
    storage,
    super.sendToOdsMosRu,
    executor, sendingExecutor, lim
  )


  override def sendToOdsMosRu(gpsdata: GPSData): Unit = resender.safeSend(gpsdata)

  //def checkForResend(): Unit =  resender.checkForResend()

  @PreDestroy
  def destroy(): Unit = {
    storage.close()
  }

}