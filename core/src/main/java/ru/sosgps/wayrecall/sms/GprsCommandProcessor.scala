package ru.sosgps.wayrecall.sms

import java.util.Date
import java.util.concurrent.ConcurrentHashMap


import com.google.common.util.concurrent.MoreExecutors
import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.events.EventsStore
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionValue}
import ru.sosgps.wayrecall.utils.concurrent.FuturesUtils
import scala.beans.BeanProperty
import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.{concurrent, mutable}

import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.concurrent.duration.DurationDouble

/**
 * Created by nickl on 21.11.14.
 */
class GprsCommandProcessor extends grizzled.slf4j.Logging {

  @Autowired
  var permissionsChecker: ObjectsPermissionsChecker = null


  @Autowired(required = false)
  var jmsTemplate: JmsTemplate = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @BeanProperty
  var timeout = 20000

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  def sendCommand(uid: String, terminal: Option[DBObject], username: String, block: Boolean): Future[DeviceCommand] = {
    val t = terminal.get
    val imei = t.as[String]("eqIMEI")
    val login = ""
    val password = terminal.flatMap(_.getAs[String]("eqPass").filter(_.nonEmpty)).getOrElse("21s21")
    sendCommand(IOSwitchDeviceCommand(imei, new Date, login,
      password, None, block).withMeta("blocking" -> block))
  }


  private val pending: concurrent.Map[(String, Date), Promise[DeviceCommand]] = new ConcurrentHashMap[(String, Date), Promise[DeviceCommand]]()

  def sendCommand(d: DeviceCommand): Future[DeviceCommand] = {

    debug("sending command:" + d)
    val promisedCommand1 = scala.concurrent.promise[DeviceCommand]
    val f = FuturesUtils.withTimeout(promisedCommand1.future, timeout.millis)
    f.onComplete {
      case _ => pending.remove((d.target, d.time), promisedCommand1)
    }
    pending.put((d.target, d.time), promisedCommand1)
    debug("pending size=" + pending.size)
    doSend(d)
    f
  }

  protected def doSend(d: DeviceCommand) {
    jmsTemplate.convertAndSend("receiver.networkcommand.navtelecom", d)
  }

  def receive(a: Any): Unit = {
    debug("received:" + a)
    a match {
      case d: DeviceCommand => {
        pending.get((d.target, d.time)).foreach(_.success(d))
      }
      case t: DeviceCommandException =>
        warn("received DeviceCommandException:", t)
        pending.get((t.d.target, t.d.time)).foreach(_.failure(t))
      case t: Throwable => warn("received:", t)
      case o => warn("received:" + o)
    }
  }


}
