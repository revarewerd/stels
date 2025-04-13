package ru.sosgps.wayrecall.monitoring.notifications

import java.util.concurrent.atomic.AtomicInteger

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, MongoDBManager}
import ru.sosgps.wayrecall.events.{DataEvent, PredefinedEvents, EventsStore}
import ru.sosgps.wayrecall.monitoring.notifications.rules.{Notification, NotificationStateChange, NotificationRule}
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker
import ru.sosgps.wayrecall.utils.MailSender
import scala.beans.BeanProperty
import ru.sosgps.wayrecall.sms.{SmsPublisher, SMS, SMSEvent, SmsGate}
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.data._
import java.util.Date

import ru.sosgps.wayrecall.data.sleepers._

import ru.sosgps.wayrecall.core.finance.{FeeProcessor, TariffPlans}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 18.11.13
 * Time: 21:13
 * To change this template use File | Settings | File Templates.
 */
class SleeperNotificationDetector extends AbstractNotificator with grizzled.slf4j.Logging {

  @PostConstruct
  def init() {
    es.subscribe(PredefinedEvents.objectNewSleeperData, onNewSleeperData)
  }

  def loadNotifications(uid: String) = {

    mdbm.getDatabase()("notificationRules").find(
      MongoDBObject("type" -> "ntfSlpr") ++
        $or("objects" -> uid, "allobjects" -> true))
      .filter(notificationDbo => {
      notificationDbo.getAs[Boolean]("allobjects") match {
        case Some(true) => permissions.isObjectAvaliableFor(notificationDbo.as[String]("user"), uid)
        case Some(false) => true
        case None => true
      }
    }).map(dbo => {
      //debug("loadNotifications dbo="+dbo)
      val params = dbo.as[BasicDBObject]("params")
      new SleeperNotificationRule(NotificationRule.fromDbo[SleeperData](dbo, appContext),
        ntfSlprExtPower = params.getAs[Boolean]("ntfSlprExtPower").getOrElse(false),
        ntfSlprMovement = params.getAs[Boolean]("ntfSlprMovement").getOrElse(false),
        ntfSlprLightInput = params.getAs[Boolean]("ntfSlprLightInput").getOrElse(false),
        ntfSlprMessage = dbo.getAsOrElse[String]("messagemask", "---")
      )
    })
  }

  def onNewSleeperData(de: DataEvent[SleeperData]) {
    debug("onNewSleeperData:" + de.data + " latestMatchResult=" + de.data.latestMatchResult)
    if (!de.data.lastIsMatched) {
      debug("not matched")
      return
    }


    val notifications = loadNotifications(de.data.uid).toList

    debug("notifications=" + notifications.map(n => n.name + " " + n.notificationId))

    val fired = notifications.map(_.process(de.data, null)).filter(_.fired)

    debug("notifications fired=" + fired.size)

    for (n <- fired; notification <- n.notifications) {
      notifyUser(notification, "Уведомление от спящих")
    }

  }

  def onNewSleeperData0(de: DataEvent[SleeperData]) {
    debug("onNewSleeperData:" + de)
    val sl = de.data

    sl.latestMatchResult.flatMap(_.alarm).collect({
      case Moving => "Зарегистрировано перемещение"
      case Power => "Отключение внешнего питания"
    }).foreach(message => {
      debug("publish:" + message)
      es.publish(new UserMessage(
        "1234",
        message,
        "Уведомление от спящих",
        Some(sl.uid),
        None,
        sl.time.map(_.getTime).getOrElse(0)))

    })
  }


  private lazy val smsPublisher = {
    val publisher = new SmsPublisher
    publisher.es = es;
    publisher.mdbm = mdbm
    publisher
  }

  private val emulatedCounter = new AtomicInteger(-1)

  def emulateSms(phone: String, text: String, now: Date = new Date()) {

    smsPublisher.onSmsReceived(Seq(new SMS(emulatedCounter.getAndDecrement, text, true, phone, "", now, now, SMS.DELIVERED)))
    //es.publish(new SMSEvent(new SMS(-1, text, true, phone, "", now, now)))

  }

  /*
{
    "_id": ObjectId("528cc85de4b0f290eb56bbe6"),
    "name": "Уведомление от спящего 00",
    "email": "xiexed@gmail.com",
    "params": {
    "ntfSlprExtPower": true,
    "ntfSlprMessage1": "Отключение внешнегооо питания.",
    "ntfSlprMovement": true,
    "ntfSlprMessage2": "Зарегистрированооо перемещение."
}, "showmessage": true,
    "objects": [ "o1603556978227868060" ],
    "type": "ntfSlpr",
    "user": "1234",
    "messagemask": "%SLEEPER_MESSAGE%",
    "phone": "+79164108305"
}

   */

  class SleeperNotificationRule(base: NotificationRule[SleeperData],
                                val ntfSlprExtPower: Boolean,
                                val ntfSlprMovement: Boolean,
                                val ntfSlprLightInput: Boolean,
                                val ntfSlprMessage: String
                                 ) extends NotificationRule[SleeperData](base) {


    override def process(sl: SleeperData, state: scala.collection.Map[String, AnyRef]) = new NotificationStateChange[SleeperData](notificationId, sl, state, sl.time) {


      lazy val fired = getAlarm(sl).exists {
        case Moving => ntfSlprMovement
        case Power => ntfSlprExtPower
        case LightInput => ntfSlprLightInput
        case _ => false
      }

      def notifications = {

//        val mess: String = getAlarm(sl).map {
//          case Moving if ntfSlprMovement => ntfSlprMessage2
//          case Power if ntfSlprExtPower => ntfSlprMessage1
//          case LightInput if ntfSlprLightInput => ntfSlprLightInputMessage
//          case _ => "----"
//        }.getOrElse("---")

        Seq(Notification[SleeperData](SleeperNotificationRule.this, new UserMessage(
          SleeperNotificationRule.this.user,
          ntfSlprMessage,
          "notifications.sleeper",
          Some(sl.uid),
          None,
          sl.time.map(_.getTime).getOrElse(0)),
          sl.uid, ntfSlprMessage, sl, sl.time.getOrElse(new Date(0))
        ))
      }
    }


    private def getAlarm(sl: SleeperData): Option[Alarm] = for (mt <- sl.latestMatchResult;
                                                                alarm <- mt.alarm) yield alarm

  }


}
