package ru.sosgps.wayrecall.monitoring.notifications.rules

import java.util.Date

import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData}
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.monitoring.processing.DistanceAggregationService
import ru.sosgps.wayrecall.utils._

class DistanceNotificationRule(base: NotificationRule[GPSData],
                               val interval: String,
                               val maxDistance: Double
                                ) extends NotificationRule[GPSData](base) with grizzled.slf4j.Logging {

  override def process(gps: GPSData, state: scala.collection.Map[String, AnyRef]) = {
    val distance = (for (lon <- state.getAs[java.lang.Double]("lon");
                         lat <- state.getAs[java.lang.Double]("lat")
    ) yield DistanceUtils.kmsBetween(lon.doubleValue(), lat.doubleValue(), gps)).getOrElse(0.0)

    val accomulated: Double = state.getAs[java.lang.Double]("accomulated").map(_.doubleValue()).getOrElse(0L)
    val accomulatedB: Double = state.getAs[java.lang.Double]("accomulatedB").map(_.doubleValue()).getOrElse(accomulated)

    val newDayStarted = state.getAs[Date]("time").exists(_.getDay != gps.time.getDay)

    val newAccomulated = if (newDayStarted) 0.0
    else
      accomulated + distance

    val newAccomulatedB = if (newDayStarted) 0.0
    else
      applicationContext.getBean(classOf[DistanceAggregationService]).updateAccumulator(gps, accomulated)

    //debug("newAccomulated=" + newAccomulated + " by " + new DateTime(gps.time) + " " + new DateTime(gps.insertTime))

    new GpsNotificationStateChange(notificationId, gps, state) {

      def fired = newAccomulated > maxDistance

      def notifications = {

        val mesString = messageMask.map(_.replace("%VALUE%", "" + newAccomulated)).getOrElse("distance notification")
        Seq(Notification[GPSData](DistanceNotificationRule.this, new UserMessage(
          user,
          mesString,
          "notifications.distancecontrol",
          Some(data.uid),
          None,
          data.time.getTime, Map("readStatus" -> false,"lon" -> data.lon, "lat" -> data.lat, "value" -> newAccomulated)),
          data.uid, mesString, data, data.time
        ))
      }

      override def updatedState = super.updatedState ++ Map(
        "lon" -> data.lon.asInstanceOf[AnyRef],
        "lat" -> data.lat.asInstanceOf[AnyRef],
        "time" -> data.time,
        "accomulatedB" -> newAccomulatedB.asInstanceOf[AnyRef],
        "accomulated" -> newAccomulated.asInstanceOf[AnyRef]
      )

    }

  }
}

