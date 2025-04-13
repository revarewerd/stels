package ru.sosgps.wayrecall.monitoring.notifications.rules

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.utils.{tryNumerics, tryDouble}

class ParamNotification(base: NotificationRule[GPSData],
                        val paramName: String,
                        val operateOut: Boolean,
                        val minValue: Double,
                        val maxValue: Double
                         ) extends NotificationRule[GPSData](base) with grizzled.slf4j.Logging {

  private[this] val paramTranslation = Map("pwr_ext" -> "Датчик напряжения").withDefault(a => a)

  override def process(gpsData: GPSData, state: scala.collection.Map[String, AnyRef]) = new GpsNotificationStateChange(notificationId, gpsData, state) {

    lazy val fired = {
      paramValue(gpsData) match {
        case Some(value) =>
          (value <= maxValue && value >= minValue) != operateOut
        case None => wasFired
      }
    }

    lazy val notifications = Seq({

      val sensor = paramTranslation(paramName)
      val value = paramValue(gpsData).getOrElse(throwNoParamData).formatted("%.2f")

      val mesString = messageMask.map(_.replace("%SENSOR%", sensor).replace("%VALUE%", value))
        .getOrElse("Значение \"" + sensor + "\" = " + value + " (" + ParamNotification.this.name + ")")
      Notification(ParamNotification.this, new UserMessage(
        ParamNotification.this.user,
        mesString,
        "notifications.sensorscontrol",
        Some(gpsData.uid),
        None,
        gpsData.time.getTime, Map("readStatus" -> false,"lon" -> gpsData.lon, "lat" -> gpsData.lat)),
        gpsData.uid, mesString, gpsData, gpsData.time
      )
    })

    private def paramValue(gpsData: GPSData): Option[Double] =
      Option(gpsData.data.get(paramName)).map(_.tryDouble)


    private def throwNoParamData: Nothing = {
      throw new scala.IllegalArgumentException(s"no value $paramName in $gpsData")
    }

  }


}