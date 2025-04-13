package ru.sosgps.wayrecall.odsmosru

import org.axonframework.eventhandling.annotation.EventHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.jms.{ObjectMessage, Message}
import ru.sosgps.wayrecall.billing.account.events.AccountStatusChanged
import ru.sosgps.wayrecall.core.{InstanceConfig, ObjectDataChangedEvent, GPSData}
import java.util
import util.GregorianCalendar
import java.net.{MalformedURLException, URL}
import ru.sosgps.wayrecall.odsmosrutelemetry.{TelemetryDetailBa, TelemetryBa, TelemetryService, TelemetryService_Service}
import java.io.File
import javax.xml.ws.BindingProvider
import javax.xml.namespace.QName
import java.nio.file._
import scala.collection.JavaConversions.iterableAsScalaIterable
import javax.annotation.PostConstruct
import scala.beans.BeanProperty
import javax.xml.datatype.DatatypeFactory
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import scala.io.Source


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 06.03.13
 * Time: 21:32
 * To change this template use File | Settings | File Templates.
 */


class Resender extends javax.jms.MessageListener with grizzled.slf4j.Logging {

  debug("creating resender")

  private var resendingUids = Set[String]()

  private val confFileName = "odsmosruimeis.txt"

  @Autowired
  var threadPool: ScalaExecutorService = null //new ScalaExecutorService("odsSendingpool", 5, 5, true, 1, TimeUnit.HOURS, new ArrayBlockingQueue[Runnable](3000))

  @Autowired
  var sender: ODSMosruSender = null

  @Autowired
  var enabledChecker: EnabledChecker = null

  @Autowired
  var instanceConf: InstanceConfig = null

  //@PostConstruct
  def init() {
    //val confDir: Path = FileSystems.getDefault().getPath(System.getenv("WAYRECALL_HOME"), "conf")
    val confDir: Path = instanceConf.path.resolve("retranslators")

    setResendingIMEIsFromFile(new File(confDir.toFile, confFileName))
    addFSListener(confDir, confFileName)
    debug("listening")
  }

  private def setResendingIMEIsFromFile(p: File) {
    info("reloading " + p)
    if (p.exists()) {
      val resendingUids = Source.fromFile(p, "UTF8").getLines().toSet
      this.resendingUids = resendingUids
      info("resendingOids=" + resendingUids)
    }
    else {
      warn("config " + p + " does not exists, no resendingIMEI to read")
    }
  }


  private[this] def checkAndSendToOdsMosRuAsync(gpsdata: GPSData) {

    import ru.sosgps.wayrecall.odsmosrutelemetry.{TelemetryDetailBa, TelemetryBa}
    import util.GregorianCalendar
    import javax.xml.datatype.DatatypeFactory

    if (resendingUids(gpsdata.uid) && enabledChecker.isAllowed(gpsdata.uid)) {
      if (sender.canSend)
        threadPool.execute {
          try {
            sender.sendToOdsMosRu(gpsdata)
          }
          catch {
            case e: Exception => warn("exception in async call:", e)
          }
        }
      else
        debug("cant soaping port is null")

    }
  }

  def onMessage(p1: Message) = try {
    trace("message received")
    p1 match {
      case m: ObjectMessage => {
        m.getObject match {
          case gpsdata: GPSData => {
            trace("received gpsdata " + gpsdata)
            checkAndSendToOdsMosRuAsync(gpsdata)
          }
          case e => warn("received unrecognized ObjectMessage with object" + e)
        }

      }
      case _ => warn("received unrecognized message")
    }
  } catch {
    case e: Exception => error("error while processing message", e)
  }

  private def addFSListener(confDir: Path, confFileName: String) {
    val newWatchService = FileSystems.getDefault().newWatchService()

    confDir.register(newWatchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE)

    new Thread(new Runnable {
      def run() {
        try {
          var work = true;

          while (work) {
            val key = newWatchService.take()

            for (event <- key.pollEvents()) {

              if (event.kind() != StandardWatchEventKinds.OVERFLOW) {
                val ev = event.asInstanceOf[WatchEvent[Path]]
                val fileName = ev.context()
                val file = confDir.resolve(fileName).toFile
                debug("modifiedFIleContext: file=" + file)
                if (file.getName == confFileName)
                  setResendingIMEIsFromFile(file)
              }

            }

            if (!key.reset()) {
              warn("stoppedWatching File")
              work = false
            }
          }
        }
        catch {
          case e: Exception => error("WatchService thread error", e)
        }
      }
    }, "WatchService thread").start()

  }

}
