package ru.sosgps.wayrecall.sms

import grizzled.slf4j.Logger
import org.springframework.context.ApplicationContext
import scala.collection.mutable.ListBuffer
import ru.sosgps.wayrecall.events.EventsStore
import java.util.Date

object SmsConversation {
  lazy val logger = Logger(getClass)
}


@SerialVersionUID(4746215475831260524L)
class SmsConversation(val phone: String) extends Serializable {

  @transient
  var context: ApplicationContext = null;

  @transient
  var commandProcessor: SMSCommandProcessor = null;

  def this(phone: String, toSend: Seq[SMSCommand]) = {
    this(phone)
    pendingForSend ++= toSend
  }

  protected val pendingForSend = new ListBuffer[SMSCommand]

  protected val pendingForAnswer = new ListBuffer[SMSCommand]

  def canBeDeleted: Boolean = pendingForAnswer.isEmpty && pendingForSend.isEmpty;

  def start() = if (!pendingForSend.isEmpty) {
    val toSend = pendingForSend.remove(0)
    if (toSend.responseNeeded)
      pendingForAnswer += toSend

    SmsConversation.logger.debug("sending sms:" + toSend.text + " to " + phone)
    val sms = commandProcessor.sendSms(phone, toSend.text)
    SmsConversation.logger.debug("created sms:" + sms)

    toSend.sentSMS = Some(sms)

  }

  def contentEquals(another:SmsConversation):Boolean = {
    (allPending zip another.allPending)
      .forall{case (c1,c2) => c1.text == c2.text}
  }


  def allPending: Seq[SMSCommand] = {
    (pendingForAnswer ++ pendingForSend)
  }

  def receive(sms: SMS): Unit = {
    SmsConversation.logger.debug("receiving " + sms)
    require(sms.fromObject)
    val acceptingCommand = pendingForAnswer.find(command => {
      val result = command.sentSMS.map(_.sendDate.before(sms.sendDate)).getOrElse(false) &&
        command.acceptResponse(sms)
      SmsConversation.logger.debug(s"pendingForAnswer command $command accepts $sms = $result")
      result
    }
    )
    acceptingCommand.foreach(command => SmsConversation.logger.info("sms accepted:" + command))
    acceptingCommand.foreach(command => pendingForAnswer -= command)

    if (acceptingCommand.isEmpty)
      SmsConversation.logger.debug("sms" + sms + " received, but there is no command waiting for it ")

    for (command <- acceptingCommand) {
      command.receivedAnswer = Some(sms)
      val es = context.getBean(classOf[EventsStore])
      es.publish(new SMSCommandEvent(phone, command))
      start()
    }

  }

  def updateDeliveryStatus(mid: Long, deliveryDate: Date) = {
    pendingForAnswer.find(c => c.sentSMS.map(_.smsId == mid).getOrElse(false)).foreach(command => {
      SmsConversation.logger.debug("updating command " + command + " delivered date")
      command.sentSMS.get.deliveredDate = deliveryDate;
    })
  }

  def lastModified = (pendingForSend.iterator ++ pendingForAnswer.iterator).map(_.lastUpdated()).flatten.reduceOption(Ordering[Date].max)

  override def toString = "SmsConversation( pendingForAnswer="+pendingForAnswer+" pendingForSend="+pendingForSend+" last modified="+lastModified+")"
}
