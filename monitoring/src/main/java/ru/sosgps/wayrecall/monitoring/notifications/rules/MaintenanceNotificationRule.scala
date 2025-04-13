package ru.sosgps.wayrecall.monitoring.notifications.rules

import java.util.Date

import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData}
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.monitoring.notifications.{Param, Rule}
import ru.sosgps.wayrecall.monitoring.processing.DistanceAggregationService
import ru.sosgps.wayrecall.utils.{tryNumerics, typingMap, OptMap}

import scala.collection
import scala.reflect.runtime.universe.TypeTag

/**
 * Created by nickl on 21.05.15.
 */
@Rule(id = "ntfMntnc")
class MaintenanceNotificationRule(base: NotificationRule[GPSData],
                                  @Param("ntfDistMax") val ntfDistMax: Double,
                                  @Param("ntfDistEnabled", default = true) val ntfDistEnabled: Boolean,
                                  @Param("ntfMotoMax") val ntfMotoMax: Double,
                                  @Param("ntfMotoEnabled", default = true) val ntfMotoEnabled: Boolean,
                                  @Param("ntfHoursMax", default = 0) val ntfhoursMax: Double,
                                  @Param("ntfHoursEnabled", default = true) val ntfhoursEnabled: Boolean
                                   ) extends NotificationRule[GPSData](base) with grizzled.slf4j.Logging {


  override def process(gps: GPSData, state: scala.collection.Map[String, AnyRef]) = {

    val statesFor = MaintenanceCriteria.all.map(_.forRuleAndState(this, state))

    //debug(s"curMoto = $curMoto curDist=$curDist")

    new GpsNotificationStateChange(notificationId, gps, state) {

      def fired = statesFor.exists(_.fired(gps))

      def notifications = {
        val mesString = messageMask.map(_.replace("%VALUE%", "???")).getOrElse("maintenance notification")
        Seq(Notification[GPSData](MaintenanceNotificationRule.this, new UserMessage(
          user,
          mesString,
          "notifications.maintenance",
          Some(data.uid),
          None,
          data.time.getTime, Map("readStatus" -> false,"lon" -> data.lon, "lat" -> data.lat)),
          data.uid, mesString, data, data.time
        ))
      }

      override def updatedState = super.updatedState ++
        statesFor.map(st => st.criteria.inStateName -> st.lastValue.getOrElse(st.criteria.curExtractor(gps)))
          .map(kv => kv._1 -> kv._2.asInstanceOf[AnyRef]) ++ Map(
        "time" -> data.time
      )

    }

  }

  //  import MaintenanceCriteria.{time, distance, moto}

  //  @deprecated("use maxFor")
  //  def timeMax(state: collection.Map[String, Any]): Double = maxFor(time, state)
  //
  //  @deprecated("use maxFor")
  //  def distanceMax(state: collection.Map[String, Any]): Double = maxFor(distance, state)
  //
  //  @deprecated("use maxFor")
  //  def motoMax(state: collection.Map[String, Any]): Double = maxFor(moto, state)

  //  def maxFor[T <: AnyVal : Numeric : TypeTag](criteria: MaintenanceCriteria[T], state: collection.Map[String, Any]): Double = {
  //    criteria.forRuleAndState(this, state).limit
  //  }
}

