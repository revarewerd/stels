package ru.sosgps.wayrecall.m2msms

import ru.sosgps.wayrecall.events.{Event, EventTopic, EventsStore}

import java.util.{Date, Objects, Timer, TimerTask}
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.m2msms.SendSms
import ru.sosgps.wayrecall.m2msms.SendSms.richMessage
import ru.sosgps.wayrecall.m2msms.SoapUtils.toDate
import ru.sosgps.wayrecall.utils.errors.ImpossibleActionException

import collection.JavaConversions.asScalaBuffer
import scala.beans.BeanProperty
import scala.collection.mutable
import javax.annotation.PostConstruct
import ru.sosgps.wayrecall.m2msms.generated.{MessageInfo, RequestMessageType}
import ru.sosgps.wayrecall.sms._

import java.lang
import javax.jms.{Message, ObjectMessage}
import kamon.Kamon
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.sms.{SMS, SmsGate, SmsListener}

class M2mSmsGate extends SmsGate with javax.jms.MessageListener with grizzled.slf4j.Logging {

  @BeanProperty
  var m2msms: SendSms = null

  @BeanProperty
  var readIntervalSecs = 30;

  @BeanProperty
  var readLastHours = 5

  @BeanProperty
  var enabled = true

  private val timer: Timer = new Timer();

  @PostConstruct
  def init() {
    timer.schedule(new TimerTask {
      def run() = try {
        readSmsFromServer(readLastHours)
      }
      catch {
        case e: Throwable => {
          error("error in readSmsFromServer", e)
        }
      }
    }, 0, readIntervalSecs * 1000)

  }

  def sendSms(phone: String, text: String): SMS = {
    if (!enabled)
      throw new ImpossibleActionException("m2m not enabled")
    ensurePhoneIsUser(phone)
    val sentSMSID = m2msms.sendSms(phone, text)
    Kamon.metrics.counter("m2m-sent-sms").increment()
    val sms = new SMS(sentSMSID, text, phone)
    sms.status = SMS.PENDING
    smsListener.onSmsSent(sms)
    sms
  }

  //private[this] val alreadySend = new mutable.HashSet[Long]()

  private[this] val knownUsersPhones = new mutable.HashSet[String]()

  private[this] def ensurePhoneIsUser(phone: String) {

    if (!knownUsersPhones(phone)) {
      try {
        debug("adding:" + phone)
        val res = m2msms.addUser(phone, phone, null, 6290)
        debug("res:" + res)
      }
      catch {
        case e: Exception =>
          info(e)
      }
      knownUsersPhones.add(phone)
    }
  }

  private[this] val knownDelivered = new NewSMSDetector

  private[this] val knownReceived = new NewSMSDetector
  private[this] val knownSent = new NewSMSDetector

  private[this] val statusMap = new mutable.HashMap[String, NewSMSDetector] {
    override def default(key: String) = getOrElseUpdate(key, new NewSMSDetector)
  }

  private def readSmsFromServer(hours: Int): Unit = {

    val (incomingAll, outgoingAll) = m2msms.readLastHourSms(Seq.empty, hours, RequestMessageType.ALL).getMessageInfo.partition(_.incoming)

    debug(s"reading incomingAll $hours-hours count=${incomingAll.size} outgoingAll= ${outgoingAll.size}")
    processOutgoing(outgoingAll)
    processIncoming(incomingAll)
  }


  private[this] def processIncoming(incomingAll: Seq[MessageInfo]) {

    val received = knownReceived.getNewAndReplace(incomingAll).map(_.toSms)

    Kamon.metrics.counter("m2m-incoming-sms").increment(received.size)

    //trace("incomingAll sms: " + incomingAll.map(mi => (mi.getSenderMsid, mi.getMessageText, mi.getCreationDate)).mkString("\n"))
    trace("received sms: " + received)

    smsListener.onSmsReceived(received)
  }

  def processOutgoing(outgoingAll: Seq[MessageInfo]) {
    //    val (deliveredAll, undeliveredAll) = outgoingAll.partition(_.delivered)
    //
    //    for (mi <- knownDelivered.getNewAndReplace(deliveredAll)) {
    //      trace("delivered sms:" + mi.getMessageID + " " + mi.getMessageText)
    //      smsListener.onSmsDelivered(mi.getDeviceMsid, mi.getMessageID, mi.getDeliveryInfo.getDeliveryInfoExt.map(_.getDeliveryDate).head)
    //    }

    val statusChanged = for ((k, v) <- outgoingAll.groupBy(_.deliveryStatus.map(_.value()).getOrElse("Unknown"))) yield {
      statusMap(k).getNewAndReplace(v)
    }

    smsListener.onSmsStatusChanged(statusChanged.flatten.map(_.toSms).toSeq)

    val newSent = knownSent.getNewAndReplace(outgoingAll).map(_.toSms)
    trace("new sent sms: " + newSent)
    smsListener.onSmsSentBatch(newSent)


  }

  private[this] var smsListener: SmsListener = new SmsListener {

    def onSmsReceived(smses: Seq[SMS]) = {
      debug("no sms listeners to process onSmsReceived: " + smses)
    }

    def onSmsSentBatch(smses: Seq[SMS]) = {
      debug("no sms listeners to process onSmsSent:" + smses)
    }

    def onSmsStatusChanged(smses: Seq[SMS]) = {
      debug("no sms listeners to process onSmsStatusChanged:" + smses)
    }

    override def onSmsSent(sms: SMS): Unit = {
      debug("no sms listeners to process onSmsSent:" + sms)
    }
  }

  def setSmsListener(l: SmsListener) = {
    smsListener = Objects.requireNonNull(l, "listener must not be null")
  }


  def onMessage(p1: Message) {
    p1 match {
      case m: ObjectMessage => {
        m.getObject match {
          case receivedsms: SMS => {
            debug("received sms " + receivedsms)
            readSmsFromServer(1)
          }
          case e: Any => warn("received unrecognized ObjectMessage with object" + e)
        }

      }
      case _ => warn("received unrecognized message")
    }
  }

  override def getSmsListener: SmsListener = smsListener
}

class NewSMSDetector {
  //  import com.google.common.cache.CacheBuilder
  //  import java.util.concurrent.TimeUnit
  //  import collection.JavaConverters.asScalaSetConverter
  //
  //  val cachedSet: mutable.Set[lang.Long] = java.util.Collections.newSetFromMap[java.lang.Long](CacheBuilder.newBuilder()
  //    .expireAfterAccess(10, TimeUnit.HOURS).build[java.lang.Long, java.lang.Boolean]().asMap()).asScala

  private[this] var alreadyKnown = Set.empty[Long]

  def getNewAndReplace(allNew: TraversableOnce[MessageInfo]): Seq[MessageInfo] = synchronized {
    val newAlreadyKnown = allNew.toList
    val reallyNew = newAlreadyKnown.filter(m => !alreadyKnown(m.getMessageID))
    alreadyKnown = newAlreadyKnown.map(_.getMessageID).toSet
    reallyNew
  }


}

