package ru.sosgps.wayrecall.monitoring.notifications.rules

import java.util.Date
import javax.annotation.PostConstruct

import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{GPSData, MongoDBManager}
import ru.sosgps.wayrecall.data.{PackagesStore, UserMessage}
import ru.sosgps.wayrecall.monitoring.notifications.AbstractNotificator
import ru.sosgps.wayrecall.monitoring.processing.parkings.{MovingStatesExtractor, ObjectTripSettings}
import ru.sosgps.wayrecall.utils

import scala.beans.BeanProperty
import scala.collection.Map


object TimedGPSNotificationRule {
  type EventType = (Long, String, PackagesStore)
}

import ru.sosgps.wayrecall.monitoring.notifications.rules.TimedGPSNotificationRule.EventType




abstract class TimedGPSNotificationRule(
                                         base: NotificationRule[EventType]
                                )
  extends NotificationRule[EventType](base) {


  override def process(data: EventType, state: Map[String, AnyRef]): NotificationStateChange[EventType] = {

    val (nowTime, uid, packStore) = data
    val firinPosition = checkFired(nowTime, uid, packStore)
    val mess = base.messageMask.getOrElse("empty message")
    new NotificationStateChange[(Long, String, PackagesStore)](this.notificationId, data, state, Some(new Date(nowTime))) {

      override def fired: Boolean = firinPosition.isDefined

      override def notifications: Seq[Notification[(Long, String, PackagesStore)]] =
        Seq(Notification[(Long, String, PackagesStore)](TimedGPSNotificationRule.this, new UserMessage(
          targetId = TimedGPSNotificationRule.this.user,
          `type` = typeName,
          message = mess,
          subjObject = Some(uid),
          fromUser = None,
          time = nowTime,
          additionalData = Predef.Map("readStatus" -> false,"lon" -> firinPosition.get.lon.asInstanceOf[AnyRef], "lat" -> firinPosition.get.lat)),
          uid, mess, data, eventTime = firinPosition.get.time
        ))
    }
  }

  def typeName: String = "notifications.timecontrol"

  def checkFired(nowTime: Long, uid: String, packStore: PackagesStore): Option[GPSData]
}

class NoDataException(msg: String) extends IllegalStateException(msg)

class LongParkingRule(
                       base: NotificationRule[EventType],
                       interval: Long
                       ) extends TimedGPSNotificationRule(base) {

  override def typeName: String = "notifications.longparking"

  def checkFired(nowTime: Long, uid: String, packStore: PackagesStore): Option[GPSData] = {
    val history = packStore.getHistoryFor(uid, new Date(nowTime - interval), new Date(nowTime)).iterator
    if (history.isEmpty)
      throw new NoDataException("no history in interval:" + (nowTime - interval) + " - " + nowTime + " for " + uid)
    val settings = new ObjectTripSettings(uid, applicationContext.getBean(classOf[MongoDBManager]))
    val mstates = new MovingStatesExtractor().detectStates(history,settings).toStream
    if (mstates.size == 1 && mstates.head.isParking) {
      Some(mstates.head.last)
    }
    else
      None
  }

}

class LongNoMessageRule(
                         base: NotificationRule[EventType],
                         interval: Long
                         ) extends TimedGPSNotificationRule(base) {

  override def typeName: String = "notifications.nomessages"

  def checkFired(nowTime: Long, uid: String, packStore: PackagesStore): Option[GPSData] = {
    val latest = packStore.getLatestFor(Iterable(uid)).headOption
    latest.filter(gps => gps.time.getTime < nowTime - interval)
  }

}
