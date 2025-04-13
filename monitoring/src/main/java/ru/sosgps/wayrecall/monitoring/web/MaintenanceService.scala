package ru.sosgps.wayrecall.monitoring.web

import javax.annotation.PostConstruct

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.TypeImports
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.annotation.{AnnotationEventListenerAdapter, EventHandler}
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{GPSData, MongoDBManager, ObjectDataChangedEvent}
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.monitoring.notifications.GpsNotificationDetector
import ru.sosgps.wayrecall.monitoring.notifications.rules.{MaintenanceCriteria, MaintenanceNotificationRule}
import ru.sosgps.wayrecall.utils._

import scala.reflect.runtime.universe.TypeTag
import MaintenanceCriteria.{distance, moto, time}
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker
import ru.sosgps.wayrecall.utils.errors.ImpossibleActionException

import scala.collection.Map

@ExtDirectService
class MaintenanceService extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var notificationDetector: GpsNotificationDetector = null

  @Autowired
  var packStore: PackagesStore = null


  @Autowired
  private var permissionsChecker: ObjectsPermissionsChecker = null


  //  @ExtDirectMethod
  //  def updateMaintenanceStatus(uid: String, status: Map[String, Any]) = {
  //    debug(s"updateMaintenanceStatus uid=$uid status=$status ")
  //  }

  def maintenanceNeedded(uid: String, user: String): Boolean = {
    val fired = _getMaintenanceState(uid, user).map(_.getAsOrElse[Boolean]("fired", false))
    debug(s"maintenanceNeedded for $uid : $fired")
    fired.getOrElse(false)
  }


  @ExtDirectMethod
  def saveSettings(uid: String, settings: Map[String, Any]): Unit = {
    debug(s"saving for $uid $settings")
    updateState(uid, settings, permissionsChecker.username)
  }

  private def _getMaintenanceState(uid: String, user: String): Option[DBObject] = {
    getMaintenanceRule(uid, user)
      .flatMap(maintenanceRule => {
      mdbm.getDatabase()("notificationRulesStates")
        .findOne(MongoDBObject("uid" -> uid, "notificationRule" -> maintenanceRule.notificationId))
    })
  }

  private def getMaintenanceRule(uid: String, user: String): Option[MaintenanceNotificationRule] = {
    notificationDetector.rules(uid).find(rule => rule.isInstanceOf[MaintenanceNotificationRule] && rule.user == user)
      .map(_.asInstanceOf[MaintenanceNotificationRule])
  }


  val fields = Seq(
    new MaintenanceField[Double](distance),
    new MaintenanceField[Double](moto),
    new MaintenanceField[Long](time)
  )

  @ExtDirectMethod
  def getMaintenanceState(uid: String) = getMaintenanceRule(uid, permissionsChecker.username).map(rule => {
    val state = getStateOrEmpty(uid, permissionsChecker.username)
    val gpsOpt = packStore.getLatestFor(Seq(uid)).headOption
    fields.flatMap(_.applying(rule, state, gpsOpt).fieldsToClient).toMap[String, Any]
  }).orNull

  private def updateState(uid: String, changed: Map[String, Any], user: String): Unit = {
    getMaintenanceRule(uid, user).foreach(rule => {

      debug(s"changed = $changed")

      val gpsOpt = packStore.getLatestFor(Seq(uid)).headOption

      val state = getStateOrEmpty(uid, user)

      val update: Map[String, Any] =
        fields.flatMap(_.applying(rule, state, gpsOpt).fieldsToDb(changed)).toMap[String, Any]

      debug(s"update = $update")

      state.remove("_id")
      state.putAll(update)

      val newState: MongoDBObject = gpsOpt.map(gps => MongoDBObject(rule.process(gps, state).updatedState.toList): MongoDBObject).getOrElse(state)

      debug(s"newState = $newState")

      mdbm.getDatabase()("notificationRulesStates")
        .update(MongoDBObject("uid" -> uid, "notificationRule" -> rule.notificationId), $set(newState.toSeq: _*), upsert = true)

    })
  }

  @ExtDirectMethod
  def resetMaintenance(uid: String): Map[String, Boolean] =  getMaintenanceRule(uid, permissionsChecker.username).map(rule => {

    val gpsOpt = packStore.getLatestFor(Seq(uid)).headOption

    val state = getStateOrEmpty(uid, permissionsChecker.username)

    val update: Map[String, Any] =
      fields.flatMap(f => {
        val ruleAndState = f.applying(rule, state, gpsOpt)
        ruleAndState.fieldsToDb(Map(f.inSettingsName -> ruleAndState.criteriaForState.limit))
      }).toMap[String, Any]

    debug(s"update = $update")

    state.remove("_id")
    state.putAll(update)

    val newState: MongoDBObject = gpsOpt.map(gps => MongoDBObject(rule.process(gps, state).updatedState.toList): MongoDBObject).getOrElse(state)

    debug(s"newState = $newState")

    mdbm.getDatabase()("notificationRulesStates")
      .update(MongoDBObject("uid" -> uid, "notificationRule" -> rule.notificationId), $set(newState.toSeq: _*), upsert = true)

    Map("requireMaintenance" -> maintenanceNeedded(uid, permissionsChecker.username))

  }).getOrElse(Map.empty)

  private def getStateOrEmpty(uid: String, user: String): MongoDBObject = {
    _getMaintenanceState(uid, user)
      .map(a => a: MongoDBObject)
      .getOrElse(MongoDBObject.empty): MongoDBObject
  }

  private def current(gpsOpt: Option[GPSData], field: String): Double = {
    gpsOpt.flatMap(gps => Option(gps.privateData.get(field)).map(_.tryDouble)).getOrElse(0.0)
  }


}

class MaintenanceField[T <: AnyVal : TypeTag : Numeric](
                                                         val criteria: MaintenanceCriteria[T]
                                                         ) extends grizzled.slf4j.Logging {

  val inSettingsName = criteria.name + "Until"
  val inSettingsIntervalName = criteria.name + "Interval"
  val inSettingsRuleEnabled = criteria.name + "RuleEnabled"
  val inSettingsIntervalNameDefault = inSettingsIntervalName + "Default"
  val enabledField = criteria.enabledFieldName

  private val num = implicitly[Numeric[T]]

  import num._

  def applying(rule: MaintenanceNotificationRule, state: scala.collection.Map[String, Any], lastGPS: Option[GPSData])
  = new ForRuleAndState(rule, state, lastGPS)

  class ForRuleAndState private[MaintenanceField](rule: MaintenanceNotificationRule, state: scala.collection.Map[String, Any], lastGPS: Option[GPSData]) {

    lazy val criteriaForState = criteria.forRuleAndState(rule, state)

    @inline
    private def scale = criteria.scale

    def untilToLast(settings: collection.Map[String, Any]): Option[(String, T)] = {

      val result = for (until <- settings.get(inSettingsName); current <- current)
        yield (criteria.inStateName -> (((until.tryDouble - maxVal.tryDouble) * scale).tryNum[T] + current))
      debug(s"untilToLast: $result from $settings")
      result

      //
      //      settings.get(inSettingsName).flatMap(until => current.map(current =>
      //        inStateName -> (((until.tryDouble - maxVal.tryDouble) * scale).tryNum[T] + current)))
    }

    def lastToUntil: Option[(String, Double)] = {
      //data.get(field).map(last => ((last.tryDouble - maxVal) * scale).tryNum[T] + current)

      state.get(criteria.inStateName).map(lastInState => inSettingsName -> {
        val lastInStateDouble = lastInState.tryDouble
        val value = maxVal - (current.map(_.toDouble()).getOrElse(lastInStateDouble) - lastInStateDouble) / scale
        debug(s"lastToUntil $inSettingsName maxVal = $maxVal scale = $scale current = $current last = $lastInState result = $value lastGPS=$lastGPS")
        value
      })
    }

    private def current = lastGPS.map(criteria.curExtractor).orElse(criteriaForState.lastValue)

    private def maxVal: Double = criteriaForState.limit

    def intervalValue: Option[(String, Double)] = criteriaForState.customLimit.map(inSettingsIntervalName -> _)

    def changedCustom(changed: collection.Map[String, Any]): Option[(String, Any)] =
      changed.get(inSettingsIntervalName).map(criteria.customFieldName -> _)

    def fieldsToClient: Iterable[(String, Any)] =
      intervalValue ++
        Seq(
          lastToUntil.getOrElse(inSettingsName -> criteriaForState.ruleLimit),
          inSettingsIntervalNameDefault -> criteriaForState.ruleLimit,
          enabledField -> criteriaForState.enabled,
          inSettingsRuleEnabled -> criteriaForState.enabledInRule
        )

    def fieldsToDb(changed: collection.Map[String, Any]): Iterable[(String, Any)] = {
      val limitsChange = changedCustom(changed)
      val newState = limitsChange.map(changedState).getOrElse(this)
      newState.untilToLast(changed) ++ limitsChange ++ enabledState(changed)
    }

    private def changedState(limitsChange: (String, Any)) = {
        val stateChange = state + limitsChange
        if (stateChange != state)
          new ForRuleAndState(rule, stateChange, lastGPS)
        else this
    }

    private def enabledState(changed: Map[String, Any]): Option[(String, Any)] =
      for(enabled <- changed.get(enabledField).map(_.asInstanceOf[Boolean])) yield {
        if(enabled && !criteriaForState.enabledInRule)
          throw new ImpossibleActionException("can't enable rule disabled in notification settings")

          criteria.enabledFieldName -> enabled
      }
  }

}