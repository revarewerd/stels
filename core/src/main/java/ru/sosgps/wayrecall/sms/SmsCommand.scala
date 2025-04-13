package ru.sosgps.wayrecall.sms

import ru.sosgps.wayrecall.events.{PredefinedEvents, AbstractEvent, EventTopic, Event}
import java.util.Date
import scala.collection.mutable
import scala.{Option => ?}

trait DeviceCommand extends Serializable {

  def target: String

  def text: String

  def time: Date

  def login: String

  def password: String

  def meta: scala.collection.Map[String, Any]
}

trait MutableMetaDataDeviceCommand extends DeviceCommand {

  val meta = new mutable.HashMap[String, Any]()

  def withMeta(data: (String, Any)*) = {
    meta ++= data
    this
  }

}

case class DeviceCommandException(d: DeviceCommand, innerException: Throwable) extends RuntimeException(innerException)

@SerialVersionUID(2L)
case class DeviceCommandData(target: String, time: Date, login: String, password: String, text: String) extends MutableMetaDataDeviceCommand

@SerialVersionUID(1L)
case class IOSwitchDeviceCommand(target: String, time: Date, login: String, password: String, num: Option[Int], state: Boolean) extends MutableMetaDataDeviceCommand {
  def text = "IOSwitch(" + num.getOrElse("default") + ", " + state + ")"
}

@SerialVersionUID(1L)
case class DeviceCommandsBatch(target: String, batch: List[DeviceCommand]) extends MutableMetaDataDeviceCommand {
  override def text: String = throw new UnsupportedOperationException("DeviceCommandsBatch text")

  override def password: String = batch.head.password

  override def time: Date = batch.head.time

  override def login: String = batch.head.login

}

//TODO: наверное из стоит в перспективе унаследовать от  DeviceCommand
trait SMSCommand extends Serializable {

  val text: String

  val responseNeeded: Boolean = true;

  var sentSMS: Option[SMS] = None

  var receivedAnswer: Option[SMS] = None

  def lastUpdated() = (sentSMS.map(smsLatestDate) ++ receivedAnswer.map(smsLatestDate)).reduceOption((d1, d2) => if (d1.after(d2)) d1 else d2)

  private[this] def smsLatestDate(sms: SMS): Date = {
    (?(sms.sendDate) ++ ?(sms.deliveredDate)).reduce((d1, d2) => if (d1.after(d2)) d1 else d2)
  }

  def acceptResponse(s: SMS): Boolean

  override def toString = s"SMSCommand[${this.getClass}]( text=" + text + " sentSMS=" + sentSMS + " receivedAnswer=" + receivedAnswer + " lastUpdated=" + lastUpdated() + ")"
}

@SerialVersionUID(-508175999641622043L)
class SMSCommandEvent(phone: String, val command: SMSCommand) extends AbstractEvent(PredefinedEvents.commandIsDone) {
  val targetId = phone

  override val persistent = true

  override def toString = super.toString + "(phone=" + phone + " command=" + command + ")"
}
