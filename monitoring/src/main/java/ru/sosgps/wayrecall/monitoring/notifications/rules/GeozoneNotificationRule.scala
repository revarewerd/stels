package ru.sosgps.wayrecall.monitoring.notifications.rules

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.monitoring.geozones.GeozonesStore


import ru.sosgps.wayrecall.data.sleepers.{Alarm, Power, Moving, SleeperData}
import ru.sosgps.wayrecall.utils.typingMap
import collection.JavaConversions.mapAsScalaMap

class GeozoneNotificationRule(base: NotificationRule[GPSData],
                              val geozoneId: Int,
                              val onLeave: Boolean
                               ) extends NotificationRule[GPSData](base) {

  def messageType = "notifications.geozobecontrol"
  override def process(gpsData: GPSData, state: scala.collection.Map[String, AnyRef]) = new GpsNotificationStateChange(notificationId, gpsData, state) {

    def fired = throw new UnsupportedOperationException("cant test if geozone is fired, use higher level methods")

    def notifications = {
      val geozoneName = applicationContext.getBean(classOf[GeozonesStore]).getById(GeozoneNotificationRule.this.geozoneId).name
      val mesString = messageMask.map(_
        .replace("%GEOZONE%", geozoneName)
        .replace("%SPEED%", this.data.speed.toString)
      ).getOrElse((if (GeozoneNotificationRule.this.onLeave) "Вышел из " else "Вошел в ") + geozoneName)
      Seq(Notification[GPSData](GeozoneNotificationRule.this, new UserMessage(
        GeozoneNotificationRule.this.user,
        mesString,
        messageType,
        Some(gpsData.uid),
        None,
        gpsData.time.getTime, Map("readStatus" -> false,"lon" -> gpsData.lon, "lat" -> gpsData.lat, "geozoneId" -> GeozoneNotificationRule.this.geozoneId)),
        gpsData.uid, mesString, gpsData, gpsData.time
      ))
    }
  }

}
