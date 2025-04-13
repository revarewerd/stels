package ru.sosgps.wayrecall.monitoring.notifications.rules

import java.lang

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.data.sleepers.{Alarm, Moving, Power}
import ru.sosgps.wayrecall.utils.{OptMap, typingMap}

import scala.collection.JavaConversions.mapAsScalaMap

class GosafeAlarmNotificationRule(base: NotificationRule[GPSData],
                                  val ntfSlprExtPower: Boolean,
                                  val ntfSlprMovement: Boolean,
                                  val ntfSlprMessage: String
                                   ) extends NotificationRule[GPSData](base) with grizzled.slf4j.Logging {

  override def process(gps: GPSData, state: scala.collection.Map[String, AnyRef]) = new NotificationStateChange[GPSData](notificationId, gps, state, Option(gps.time)) {

    override def toString() = "GosafeAlarmNotificationRule.NotificationState(ntfSlprExtPower=" + ntfSlprExtPower + " ntfSlprMovement=" + ntfSlprMovement + " ntfSlprMessage=" + ntfSlprMessage + ")"


    private val statusString = gps.data.toMap.getAs[String]("statuses").getOrElse("")

    //    private val statuses = {
    //      statusString.stripPrefix("{").stripSuffix("}").split(", ")
    //        .map(str => {
    //        val s = str.split(":")
    //        (s(0), s(1))
    //      }).toMap
    //    }

    private val hasPanic = statusString.contains("Panic:On")

    private val hasMoving = statusString.contains("Move/Stop:Move")

    val prevMoving = state.getAs[lang.Boolean]("isMoving").map(_.booleanValue()).getOrElse(false)
    val accomulatedMovings = state.getAs[lang.Integer]("accomulatedMovings").map(_.intValue()).getOrElse(0)
    val newaccomulatedMovings = if (hasMoving) accomulatedMovings + 1 else 0
    val fireMoving = newaccomulatedMovings == 10
    val prevPanic = state.getAs[lang.Boolean]("panicOn").map(_.booleanValue()).getOrElse(false)
    val firePanic = !prevPanic && hasPanic

    lazy val alarms: Seq[Alarm] = (if (ntfSlprExtPower && firePanic) Seq(Power) else Seq.empty) ++
      (if (ntfSlprMovement && fireMoving) Seq(Moving) else Seq.empty)

    //    debug("gps=" + gps.time + " " + gps.data.get("statuses") + " gps=" + gps)
    //    debug("alarms=" + alarms)

    lazy val fired = alarms.nonEmpty

    def notifications = alarms.map {
      alarm =>

        //        val mess: String = alarm match {
        //          case Moving if ntfSlprMovement => ntfSlprMessage2
        //          case Power if ntfSlprExtPower => ntfSlprMessage1
        //          case _ => "----"
        //        }

        Notification[GPSData](GosafeAlarmNotificationRule.this, new UserMessage(
          GosafeAlarmNotificationRule.this.user,
          ntfSlprMessage,
          "notifications.gosafesleeperscontrol",
          Some(gps.uid),
          None,
          gps.time.getTime, Map("readStatus" -> false,"lon" -> gps.lon, "lat" -> gps.lat)),
          gps.uid, ntfSlprMessage, gps, gps.time
        )
    }

    override def updatedState = {
      super.updatedState ++ OptMap(
        "panicOn" -> hasPanic.asInstanceOf[AnyRef],
        "lastPanic" -> (if (hasPanic) Some(data.time) else None),
        "isMoving" -> hasMoving.asInstanceOf[AnyRef],
        "lastMovingStarted" -> (if (!prevMoving && hasMoving) Some(data.time) else None),
        "accomulatedMovings" -> newaccomulatedMovings.asInstanceOf[AnyRef]
      )
    }

  }


  val toBeFired: PartialFunction[Alarm, Boolean] = {
    {
      case Moving => ntfSlprMovement
      case Power => ntfSlprExtPower
    }
  }
}