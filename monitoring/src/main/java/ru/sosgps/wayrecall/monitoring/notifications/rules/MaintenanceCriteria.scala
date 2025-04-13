package ru.sosgps.wayrecall.monitoring.notifications.rules

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils.{tryNumerics, typingMap, OptMap}

import scala.reflect.runtime.universe.TypeTag

class MaintenanceCriteria[T <: AnyVal : Numeric : TypeTag](
                                                            val inStateName: String,
                                                            val customFieldName: String,
                                                            val name: String,
                                                            val scale: Double,
                                                            val ruleValue: (MaintenanceNotificationRule) => Double,
                                                            val ruleEnabled: (MaintenanceNotificationRule) => Boolean,
                                                            val curExtractor: (GPSData) => T
                                                            ) extends grizzled.slf4j.Logging {


  val enabledFieldName = name + "Enabled"

  private val num = implicitly[Numeric[T]]

  import num._

  def forRuleAndState(rule: MaintenanceNotificationRule, state: scala.collection.Map[String, Any]) = new {

    val criteria: MaintenanceCriteria[T] = MaintenanceCriteria.this

    def lastValue: Option[T] = state.getAs[Any](inStateName).map(_.tryNum[T])

    def ruleLimit: Double = ruleValue(rule)

    def customLimit: Option[Double] = state.getAs[Any](customFieldName).map(_.tryDouble).filterNot(_.isNaN)

    def limit: Double = customLimit.getOrElse(ruleLimit)

    def enabled: Boolean = enabledInRule && state.getAs[Boolean](enabledFieldName).getOrElse(limit != 0)

    def enabledInRule: Boolean = ruleEnabled(rule)

    def fired(gps: GPSData): Boolean = {
      if(!enabled)
        return false

      val cur = curExtractor(gps)
      val accumulated = cur - lastValue.getOrElse(cur)
      val r = accumulated > (limit * scale).tryNum[T]
     // debug(s"checking $inStateName now is fired=$r because $cur - ${lastValue.getOrElse(cur)} = $accumulated > $limit * $scale  state=$state  by data=$gps")
      r
    }

  }

}

object MaintenanceCriteria {

  val distance = new MaintenanceCriteria[Double](
    "lastMntDistance", "customDistanceInterval", "distance", 1,
    _.ntfDistMax, _.ntfDistEnabled, gps => Option(gps.privateData.get("tdist")).map(_.tryDouble).getOrElse(0.0)
  )

  val moto = new MaintenanceCriteria[Double](
    "lastMntMoto", "customMotohoursInterval", "motohours", 1000 * 60 * 60,
    _.ntfMotoMax, _.ntfMotoEnabled ,gps => Option(gps.privateData.get("mh")).map(_.tryDouble).getOrElse(0L)
  )

  val time = new MaintenanceCriteria[Long](
    "lastMntTime", "customHoursInterval", "hours", 1000 * 60 * 60,
    _.ntfhoursMax, _.ntfhoursEnabled, _.time.getTime
  )


  val all = List(distance, moto, time)

}