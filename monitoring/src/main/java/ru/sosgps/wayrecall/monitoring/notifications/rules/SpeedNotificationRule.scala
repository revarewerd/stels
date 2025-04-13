package ru.sosgps.wayrecall.monitoring.notifications.rules

import java.util.Date

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.utils.{tryNumerics, OptMap, typingMap}

class SpeedNotificationRule(base: NotificationRule[GPSData],
                            val maxValue: Double
                             ) extends NotificationRule[GPSData](base) with grizzled.slf4j.Logging{


  override def process(gps: GPSData, state: scala.collection.Map[String, AnyRef]) = {
    //trace("processing " + gps)
    new GpsNotificationStateChange(notificationId, gps, state) {

      def lastUnfired = state.getAs[Date]("lastUnfired").getOrElse(new Date(0L))

      lazy val fired = {
        lazy val expeactable = {
          val prevspeed = state.get("prevSpeed").map(_.tryInt).getOrElse(0)
          val prevTime = state.getAs[Date]("prevTime").map(_.getTime).getOrElse(0L)
          val timeDiff = Math.abs(data.time.getTime - prevTime) / 1000
          val speedDiff = Math.abs(gps.speed - prevspeed)
          val r = timeDiff < 60 * 1000 && speedDiff < 20
          debug(s"expectable: timeDiff=$timeDiff, speedDiff=$speedDiff, r=$r")
          r
        }
        //debug(s"gps.speed=${gps.speed}")
        val firing = gps.speed > maxValue && expeactable && (data.time.getTime - lastUnfired.getTime > 1000 * 60 * 5)
        //debug(s"firing=$firing")
        firing
      }

      override def unfired = gps.speed <= maxValue - maxValue * 0.10

      override def updatedState = {
        super.updatedState ++ Map(
          "prevSpeed" -> data.speed.asInstanceOf[AnyRef],
          "prevTime" -> data.time.asInstanceOf[AnyRef]
        ) ++ OptMap(
          "lastUnfired" -> (if (unfiringNow) Some(data.time) else None)
        )
      }

      lazy val notifications = {
        // %VALUE%
        val mesString = messageMask.map(_.replace("%VALUE%", gps.speed.toString))
          .getOrElse("Превысил скорость " + gps.speed + " км/ч (" + SpeedNotificationRule.this.name + ")")
        Seq(Notification[GPSData](SpeedNotificationRule.this, new UserMessage(
          SpeedNotificationRule.this.user,
          mesString,
          "notifications.speedcontrol",
          Some(gps.uid),
          None,
          gps.time.getTime, Map("readStatus" -> false,"lon" -> gps.lon, "lat" -> gps.lat)),
          gps.uid, mesString, gps, gps.time
        ))
      }
    }
  }

}