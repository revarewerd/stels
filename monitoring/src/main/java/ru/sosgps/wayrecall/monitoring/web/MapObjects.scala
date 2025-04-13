/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.monitoring.web

import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.BasicDBList
import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.util.HtmlUtils
import ru.sosgps.wayrecall.core.GPSUtils._
import ru.sosgps.wayrecall.core.ObjectDataConversions.{getFuelData, getGPSDataSensors}
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.data._
import ru.sosgps.wayrecall.data.sleepers.{Moving, Power, SleeperData, SleepersDataStore}
import ru.sosgps.wayrecall.events.{DataEvent, EventsStore, OnTargets}
import ru.sosgps.wayrecall.regeocoding.ReGeocoder
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionValue}
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.DBQueryUtils._
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.ScalaServletConverters
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationLong
import scala.util.Random

@ExtDirectService
class MapObjects extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val modelBase = Model(
    "name",
    "uid",
    "description",
    "checked",
    "blocked",
    "requireMaintenance",
    "targeted",
    "hidden",
    "lon",
    "lat",
    "course",
    "speed",
    "ignition",
    "time",
    "latestmsg",
    "satelliteNum",
    //"controlEnabled",
    "canBlock",
    "canGetCoords",
    "canRestartTerminal",
    "blockingEnabled",
    "settingsViewEnabled",
    "sleeper",
    "radioUnit",
    "settingsWnd",
    "stateLatency"
  )

  val model = modelBase

  val name = "MapObjects"
  //override val storeType: String = "Ext.data.TreeStore"
  override val autoSync = false
  val idProperty = "uid"
  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var permissions: ObjectsPermissionsChecker = null

  @Autowired
  var packagesStore: PackagesStore = null

  @Autowired
  var sleepers: SleepersDataStore = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var regeocoder: ReGeocoder = null

  @Autowired
  var es: EventsStore = null

  val random = new Random()

  @Autowired
  var maintenanceService: MaintenanceService = null

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def loadObjects(request: ExtDirectStoreReadRequest): Iterator[Map[String, Any]] = {

    val smap = ScalaServletConverters.sessionToMap(utils.web.springCurrentRequest.getSession)

    val userData = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> permissions.username),
      MongoDBObject("selectedObjects" -> 1, "hiddenObjects" -> 1, "targetedObjects" -> 1))
    val selectedObjects: Set[String] = userData.flatMap(_.getAs[MongoDBList]("selectedObjects")).map(_.map(_.asInstanceOf[String]).toSet).getOrElse(Set.empty)
    val hiddenObjects: Set[String] = userData.flatMap(_.getAs[MongoDBList]("hiddenObjects")).map(_.map(_.asInstanceOf[String]).toSet).getOrElse(Set.empty)
    val targetedObjects: Set[String] = userData.flatMap(_.getAs[MongoDBList]("targetedObjects")).map(_.map(_.asInstanceOf[String]).toSet).getOrElse(Set.empty)

    debug("selectedObjects=" + selectedObjects)
    smap("selectedUids") = selectedObjects
    smap("targetedUids") = targetedObjects

    //TODO: убрать избыточное копирование
    getAvailableObjects().map(dbo => Map(
      "uid" -> dbo("uid"),
      //"name" -> dbo("name"),
      "name" -> HtmlUtils.htmlEscape((dbo.get("customName") match {
        case Some("") => dbo("name")
        case None => dbo("name")
        case Some(customName) => customName
      }).toString),
      "description" -> dbo("name"),
      "latestmsg" -> dbo("latestmsg"),
      "speed" -> dbo("speed"),
      "ignition" -> dbo("ignition"),
      "satelliteNum" -> dbo("satelliteNum"),
      "hidden" -> (if (dbo("latestmsg") == null) true else hiddenObjects.contains(dbo("uid").asInstanceOf[String])),
      "leaf" -> true,
      "checked" -> selectedObjects.contains(dbo("uid").asInstanceOf[String]),
      "blocked" -> dbo.get("blocked").getOrElse(false),
      "requireMaintenance" -> maintenanceService.maintenanceNeedded(dbo("uid").asInstanceOf[String], permissions.username),
      "targeted" -> targetedObjects.contains(dbo("uid").asInstanceOf[String]),
      //"controlEnabled" -> dbo.get("controlEnabled").getOrElse(false),
      "canBlock" -> dbo.get("block").getOrElse(false),
      "canGetCoords" -> dbo.get("getCoords").getOrElse(false),
      "canRestartTerminal" -> dbo.get("restartTerminal").getOrElse(false),
      "blockingEnabled" -> (dbo.getOrElse("ignitionLock", false) == true || dbo.getOrElse("fuelPumpLock", false) == true),
      "settingsViewEnabled" -> dbo.get("settingsViewEnabled").getOrElse(false),
      "sleeper" -> dbo("sleeper"),
      "radioUnit" -> dbo("radioUnit"),
      "settingsWnd" -> false
    )).iterator
  }

  @ExtDirectMethod
  def getSleeperInfo(uid: String) = {
    val eqList = or.getObjectEquipment(uid)

    eqList.filter(_.as[String]("eqtype").contains("Спящий блок")).map(sleeper =>
      Map(
        "loaded" -> true,
        "model" -> sleeper.as[String]("eqMark").concat(" " + sleeper.as[String]("eqModel")),
        "type" -> sleeper.as[String]("eqtype"),
        "state" -> sleeper.getAs[String]("eqStatus").orNull,
        "serialNum" -> sleeper.as[String]("eqSerNum"),
        "owner" -> sleeper.as[String]("eqOwner"),
        "rights" -> sleeper.as[String]("eqRightToUse"),
        "instDate" -> sleeper.getAs[String]("installDate").orNull,
        "simNumber" -> sleeper.as[String]("simNumber"),
        "workDate" -> sleeper.getAs[String]("eqWorkDate").orNull
      )
    )
  }

  @ExtDirectMethod
  def getLonLat(selectedUids: Seq[String]): Seq[Map[String, Any]] = {

    //updateSelectedUids(selectedUids)

    packagesStore.getLatestFor(selectedUids)
      .map(gpsdataToMap).toSeq
  }

  @ExtDirectMethod
  def getApproximateLonLat(uid: String, time: Long) = {
    val date = new Date(time)
    val (prev,at,next) = (packagesStore.prev(uid, date), packagesStore.at(uid, date), packagesStore.next(uid,date))
    val closest = Seq(prev,at,next).flatten.sortBy(data => math.abs(data.time.getTime - time)).headOption
    closest match {
      case Some(gpsData) => Map("lon" -> gpsData.lon, "lat" -> gpsData.lat)
      case _ => null
    }
  }

  @ExtDirectMethod
  def getSensorNames(uid: String): Seq[Map[String, Any]] = {
    val r = for(
      imei <- or.getMainTerminal(uid).flatMap(_.getAs[String]("eqIMEI"));
      sensors <- mdbm.getDatabase()("sensorNames").findOne(MongoDBObject("imei" -> imei))
    ) yield sensors.getAsOrElse[Seq[String]]("params", Seq.empty).sorted

    val sensors = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("sensors" -> 1)).flatMap(_.getAs[MongoDBList]("sensors")).getOrElse(new MongoDBList()).toSeq.map(_.asInstanceOf[BasicDBObject]);
    val sensorsSet = sensors.map(s => s.as[String]("paramName")).toSet
    val rset:Set[String] = r.getOrElse(Seq.empty).toSet[String]

    val restSensors = rset -- sensorsSet

    val sensorHide = Set("ME_Error_code", "ME_Error_valid", "ME_Left_signal", "ME_Low_beam", "ME_Night_time_indicator", "ME_Pedestrians_FCW", "ME_Right_LDW_on", "ME_Speed", "ME_Wipers_available", "cid", "lac", "lbs", "mainTerminal", "mcc", "mileage", "miliage", "mnc", "mode", "protocol", "ruptelaEventDataSource", "ruptelaEventName", "ruptelaEventSource", "text", "ME_Error_code", "ME_Error_valid", "alarm", "alarms", "battery", "cid", "cmd_answer")
    val sensorShow = Set("Internal_Battery_Voltage", "Modem_Temperature", "PCB_Temperature", "Power_Supply_Voltage", "TCO_vehicle_speed", "Temperature_Sensor_0", "Temperature_Sensor_1", "adc1", "adc2", "adc3", "adc4", "axle_10_weight", "axle_11_weight", "axle_12_weight", "axle_13_weight", "axle_14_weight", "axle_15_weight", "axle_1_weight", "axle_2_weight", "axle_3_weight", "axle_4_weight", "axle_5_weight", "axle_6_weight", "axle_7_weight", "axle_8_weight", "axle_9_weight", "battery", "battery_charge", "battery_current", "can1", "can2", "can6", "can_acc_pedal", "can_amb_air_temp", "can_axle_loc", "can_axle_weight", "can_breaks", "can_clutchs", "can_coolant_temp", "can_distance", "can_eng_hours", "can_fms", "can_fuel_economy", "can_fuel_used", "can_hfrc", "can_horas", "can_pto", "can_rpm", "can_service_dist", "counter1", "dist", "distance_record", "eng_temp", "engine_hours", "fuel_cons", "fuel_counter_1", "fuel_counter_2", "fuel_counter_3", "fuel_counter_4", "fuel_lvl", "gsm_signal", "hdop", "in1", "in4", "modem_temp", "odometer", "pulse_in1", "pulse_in2", "pwr_ext", "pwr_int", "tco_distance", "tco_rpm", "tco_trip", "tco_veh_speed", "temp_sens_0", "temp_sens_1", "temp_sens_2", "total_fuel", "vehicle_dist", "Internal_Battery_Voltage", "Modem_Temperature", "Movement", "Movement_sensor", "PCB_Temperature", "Power_Supply_Voltage", "Speedometer", "TCO_vehicle_speed", "TK_diesel_electric_status", "TK_high_speed_status", "Temperature_Sensor_0", "Temperature_Sensor_1", "adc1", "adc2", "adc3", "adc4", "alt", "axle_10_weight", "axle_11_weight", "axle_12_weight", "axle_13_weight", "axle_14_weight", "axle_15_weight", "axle_1_weight", "axle_2_weight", "axle_3_weight", "axle_4_weight", "axle_5_weight", "axle_6_weight", "axle_7_weight", "axle_8_weight", "axle_9_weight", "can1", "can2", "can6", "can_acc_pedal", "can_amb_air_temp", "can_axle_loc", "can_axle_weight", "can_breaks", "can_clutchs", "can_coolant_temp", "can_fuel_used", "can_hfrc", "can_rpm", "counter1", "eng_temp", "fuel_cons", "fuel_counter_1", "fuel_counter_2", "fuel_counter_3", "fuel_counter_4", "fuel_lvl", "gsm_signal", "in1", "in4", "Analog_Input_1", "Analog_Input_2", "Battery_Voltage", "CANBUS_AccPedal_position", "CANBUS_Axle_weight", "CANBUS_BreakS", "CANBUS_ClutchS", "CANBUS_CruiseCS", "CANBUS_Distance", "CANBUS_Engine_Hours", "CANBUS_Engine_PLCS", "CANBUS_Fuel_used", "CANBUS_HRFC", "CANBUS_RPM", "CANBUS__Secondary_fuel_level_%", "CANBUS_coolant_temperature", "CAN_5", "CAN_6", "Device Battery Voltage (Internal)", "Device Temperature", "Digital_Input_1", "Digital_Input_2", "Digital_Input_3", "Digital_Input_4", "Digital_Input_Status_1", "Digital_Input_Status_2", "Digital_fuel_sensorA5", "Digital_fuel_sensorA6", "Digital_fuel_sensorA7", "Digital_fuel_sensor_B1", "Digital_output_1_state", "Digital_output_2_state", "Din1_hours", "Din2_hours", "Din3_hours", "Din4_hours", "Dsitance_record", "Ext Power", "External_Power_Voltage", "Fuel_level_%", "GPS_speed", "GSM_Signal_Strength", "GSM_signal_level")

    val restData = (restSensors -- sensorHide).map(s => {
      if (sensorShow(s)) {
        Map("code" -> s, "name" -> s, "show" -> true)
      } else {
        Map("code" -> s, "name" -> s, "show" -> false)
      }
    })

    (sensors.map(s => Map(
        "code" -> s("paramName"),
        "name" -> s("name"),
        "show" -> true
      )) ++ restData
    )
  }

  @ExtDirectMethod
  def regeocode(lon: Double, lat: Double): String = {
    Await.result(regeocoder.getPosition(lon, lat), 30 seconds) match {
      case Right(position) => position
      case Left(exception: NoSuchElementException) => ""
      case Left(exception) => {
          warn("regeocoding exception:", exception)
          exception.getLocalizedMessage
        }
    }
  }

  @ExtDirectMethod
  def updateCheckedUids(selectedUids: Seq[String]) {
    val smap = ScalaServletConverters.sessionToMap(utils.web.springCurrentRequest.getSession)

    val selectedSet = smap.getTyped("selectedUids", Set.empty[String])
    val newSet = selectedUids.toSet
    if (selectedSet != newSet) {
      debug("sets are not equal, setting data to " + newSet)
      mdbm.getDatabase()("users").update(MongoDBObject("name" -> permissions.username), $set("selectedObjects" -> newSet))
      smap("selectedUids") = newSet
    }
  }

  @ExtDirectMethod
  def updateTargetedUids(selectedUids: Seq[String]) {
    val smap = ScalaServletConverters.sessionToMap(utils.web.springCurrentRequest.getSession)

    val selectedSet = smap.getTyped("targetedUids", Set.empty[String])
    val newSet = selectedUids.toSet
    if (selectedSet != newSet) {
      debug("sets are not equal, setting data to " + newSet)
      mdbm.getDatabase()("users").update(MongoDBObject("name" -> permissions.username), $set("targetedObjects" -> newSet))
      smap("targetedUids") = newSet
    }
  }

  @ExtDirectMethod
  def getUserSettings() = {
    val usersettings = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> permissions.username),
                                                           MongoDBObject("userSettings" -> 1)).flatMap(_.getAs[BasicDBObject]("userSettings")).getOrElse(new BasicDBObject())
    usersettings
  }

  @ExtDirectMethod
  def setUserSettings(settings: Map[String, Any]) {
    mdbm.getDatabase()("users").update(MongoDBObject("name" -> permissions.username), $set("userSettings" -> (settings)))
  }

  @ExtDirectMethod
  def setHiddenUids(selectedUids: Seq[String]) {
    val hiddenObjects: Set[String] = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> permissions.username),
                                                                         MongoDBObject("hiddenObjects" -> 1)).flatMap(_.getAs[MongoDBList]("hiddenObjects")).map(_.map(_.asInstanceOf[String]).toSet).getOrElse(Set.empty)

    debug("setting hidden uids " + selectedUids)
    mdbm.getDatabase()("users").update(MongoDBObject("name" -> permissions.username), $set("hiddenObjects" -> (hiddenObjects ++ selectedUids)))
  }

  @ExtDirectMethod
  def unsetHiddenUids(selectedUids: Seq[String]) {
    val hiddenObjects: Set[String] = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> permissions.username),
                                                                         MongoDBObject("hiddenObjects" -> 1)).flatMap(_.getAs[MongoDBList]("hiddenObjects")).map(_.map(_.asInstanceOf[String]).toSet).getOrElse(Set.empty)

    debug("setting unhidden uids to " + selectedUids)
    mdbm.getDatabase()("users").update(MongoDBObject("name" -> permissions.username), $set("hiddenObjects" -> (hiddenObjects -- selectedUids)))
  }

  @ExtDirectMethod
  def getUpdatedAfter(date: Date): Iterator[Map[String, Any]] = {

    trace("getUpdatedAfter date " + date + " " + Option(date).map(_.getTime).orNull)

    val allUids = permissions.getAvailableObjectsPermissions().keys //.filterNot(or.isRemoved)

    permissions.withCheckPermissionsDisabled({
        val allLatest = packagesStore.getLatestFor(allUids)

        val sleepersData = new mutable.HashMap[String, DataEvent[SleeperData]]() ++
        es.getEventsAfter[DataEvent[SleeperData]](
          new OnTargets[DataEvent[SleeperData]]("object", allUids.toSet, "newSleeperData"),
          Option(date).map(_.getTime).getOrElse(System.currentTimeMillis() - 10000)
        )
        .iterator.map(sevnt => (sevnt.data.uid, sevnt))

        trace("sleepersData:" + sleepersData.mapValues(_.time))

        trace({
            val latestOne: GPSData = allLatest.maxBy(_.insertTime)
            "latest insertTime " + latestOne.insertTime + " " + latestOne
          })

        val filterd = allLatest.filter(gps => if (date != null) (gps.insertTime after date) || sleepersData.contains(gps.uid) else true)

        trace("found after " + date + " count: " + filterd.size)

        filterd.map(
          g => {
            val correspondingSleeper = sleepersData.remove(g.uid)
            gpsdataToMap(g) +
            ("sleeper" -> correspondingSleeper.map(sevnt => this.sleeperData(sevnt.data)).orNull) +
            ("newTime" -> (Math.max(g.insertTime.getTime, correspondingSleeper.map(_.time).getOrElse(g.insertTime.getTime)) + 1))
          }
        ) ++
        sleepersData.map(kv => Map(
            "uid" -> kv._1,
            "sleeper" -> this.sleeperData(kv._2.data),
            "newTime" -> (kv._2.time + 1)
          )
        )
      }).toIterator

  }

  private[this] def radioUnit(eqList: Seq[DBObject]): Map[String, Any] = {
    eqList.filterNot(isRemoved(_)).find(_.as[String]("eqtype").equals("Радиозакладка")).map(radio =>
      Map(
        "installed" -> true,
        "model" -> radio.getAsOrElse[String]("eqMark", ""),
        "type" -> radio("eqtype"),
        "serialNum" -> radio.getAsOrElse[String]("eqSerNum", ""),
        "owner" -> radio.getAsOrElse[String]("eqOwner", ""),
        "work" -> radio.getAsOrElse[String]("eqWork", ""),
        "workDate" -> radio.getAs[String]("eqWorkDate").orNull
      )
    ).getOrElse(Map("installed" -> false))
  }

  private[this] def sleeperData(s: SleeperData): Map[String, Any] = {
    val alarm = s.latestMatchResult.flatMap(_.alarm)
    alarm.foreach(a => debug("sleeperDataalarm=" + a))
    Map(
      "time" -> s.time,
      "sleeperState" -> s.sleeperState.stateName,
      "alarm" -> alarm.collect({
          case Moving => "Зарегистрировано перемещение"
          case Power => "Отключение внешнего питания"
        }),
      "battery" -> Option(Seq(s.batValue, s.batPercentage).flatten).filter(_.nonEmpty).map(_.mkString("-"))
    )
  }

  private[this] def gpsdataToMap(gd: GPSData): Map[String, Any] = {
    Map(
      "uid" -> gd.uid,
      "lon" -> gd.lon,
      "lat" -> gd.lat,
      "course" -> gd.course,
      "speed" -> gd.speed,
      "time" -> gd.time.getTime,
      "insertTime" -> gd.insertTime.getTime,
      "satelliteNum" -> gd.satelliteNum,
      "canFuelLevel" -> getFuelData("can", gd, mdbm).round,
      "flsFuelLevel" -> getFuelData("fls", gd, mdbm).round,
      "extPower" -> detectPowerExt(gd).getOrElse(null),
      "canCoolantTemp" -> detectCANCoolantTemp(gd).getOrElse(null),
      "canRPM" -> detectCANRPM(gd).getOrElse(null),
      "canAccPedal" -> detectCANAccPedal(gd).getOrElse(null),
      "ignition" -> detectIgnition(gd).getOrElse(null),
      "sensorsData" -> getGPSDataSensors(gd, mdbm),
      "stateLatency" -> getStateLatency(gd).getOrElse(false)
    )
  }


  def getCustomName(uid: String): String = {
    val itemId=or.getObjectByUid(uid).getAs[ObjectId]("_id")
    val userId = mdbm.getDatabase()("users").findOne(MongoDBObject("name"->permissions.username),MongoDBObject("_id"->1)).get("_id").asInstanceOf[ObjectId]
    require(userId!=null,"userId can't be null")
    val permRecord=mdbm.getDatabase()("usersPermissions").findOne(MongoDBObject("userId"->userId,"recordType" -> "object","item_id" -> itemId)).getOrElse(null)
    if(permRecord==null) ""
    else permRecord.getAsOrElse[String]("customName","")
  }

  def getAvailableObjects(): Iterable[Map[String, Any]] = {

    //val smap = ScalaServletConverters.sessionToMap(utils.web.springCurrentRequest.getSession)
    //smap.getSoft("ReportObjects.cached", {
    val objdb = mdbm.getDatabase()("objects")
    permissions.getAvailableObjectsPermissions().toIndexedSeq
    .flatMap {
      case (oid, perm) => objdb.findOne(
          MongoDBObject("uid" -> oid),
          MongoDBObject("uid" -> 1, "name" -> 1, "customName" -> 1, "blocked" -> 1, "ignitionLock" -> 1, "fuelPumpLock" -> 1)).map(dbo =>
          wrapDBObj(dbo).toMap[String, Any] ++ {
            val latest = permissions.withCheckPermissionsDisabled(
              packagesStore.getLatestFor(Iterable(dbo("uid").asInstanceOf[String])).headOption
            )
            //val permissionsCustomName=getCustomName(dbo("uid").asInstanceOf[String])
            Map(
//              "customName" -> (if(permissionsCustomName=="") {
//                  if(dbo.getAs[Any]("customName").isDefined && dbo("customName")!="") dbo("customName")
//                  else ""}
//                               else  permissionsCustomName),
// "controlEnabled" -> permissions.hasPermission(oid, PermissionValue.CONTROL),
              "block" -> permissions.hasPermission(oid, PermissionValue.BLOCK),
              "getCoords" -> permissions.hasPermission(oid, PermissionValue.GET_COORDS),
              "restartTerminal" -> permissions.hasPermission(oid, PermissionValue.RESTART_TERMINAL),
              "settingsViewEnabled" -> permissions.hasPermission(oid, PermissionValue.VIEW_SETTINGS),
              "latestmsg" -> latest.map(_.time).orNull,
              //"speed" -> latest.map(_.speed).getOrElse(0),
              //"ignition" -> latest.map(g => detectIgnition(g).getOrElse(-1)).orNull,
              //"satelliteNum" -> latest.map(_.satelliteNum).getOrElse(0),
              "sleeper" -> (if (permissions.hasPermission(oid, PermissionValue.VIEW_SLEEPER)) sleepers.getLatestData(oid).map(s => sleeperData(s)).orNull else null),
              "radioUnit" -> (if (permissions.hasPermission(oid, PermissionValue.VIEW_SLEEPER)) radioUnit(or.getObjectEquipment(oid)) else false)
            ) ++ latest.map(gpsdataToMap).getOrElse(Map(
              "speed" -> 0,
              "ignition" -> -1,
              "satelliteNum" -> 0
            ))
          }
        )
    }.sortBy(i => i.get("customName").filter(nonEmptyString).orElse(i.get("name")).orNull.asInstanceOf[String])
    //})
  }

  private def nonEmptyString(a: Any):Boolean = a match {
    case s:String => s.nonEmpty
    case _ => true
  }

}

@ExtDirectService
class GroupedMapObjects extends MapObjects {

  @Autowired
  var translations: Translations = null

  override val name = "GroupedMapObjects"
  override val model = Model(modelBase.fields :+ ("group": this.Field): _*)
  override val idProperty = "_id"

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  override def loadObjects(request: ExtDirectStoreReadRequest): Iterator[Map[String, Any]] = {
    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val smap = ScalaServletConverters.sessionToMap(utils.web.springCurrentRequest.getSession)


    val userData = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> permissions.username),
      MongoDBObject("selectedObjects" -> 1, "hiddenObjects" -> 1, "targetedObjects" -> 1))
    val objectGroups = mdbm.getDatabase()("groupsOfObjects").find(MongoDBObject("uid" -> userData.get.get("_id"))).map(dbo=>dbo.map(tuple=>tuple)).toSeq
    val selectedObjects: Set[String] = userData.flatMap(_.getAs[MongoDBList]("selectedObjects")).map(_.map(_.asInstanceOf[String]).toSet).getOrElse(Set.empty)
    val hiddenObjects: Set[String] = userData.flatMap(_.getAs[MongoDBList]("hiddenObjects")).map(_.map(_.asInstanceOf[String]).toSet).getOrElse(Set.empty)
    val targetedObjects: Set[String] = userData.flatMap(_.getAs[MongoDBList]("targetedObjects")).map(_.map(_.asInstanceOf[String]).toSet).getOrElse(Set.empty)

    debug("selectedObjects=" + selectedObjects)
    smap("selectedUids") = selectedObjects
    smap("targetedUids") = targetedObjects

    //TODO: убрать избыточное копирование
    val availableObjects=getAvailableObjects().map(dbo => Map(
      "uid" -> dbo("uid"),
      //"name" -> dbo("name"),
      "name" -> HtmlUtils.htmlEscape((dbo.get("customName") match {
        case Some("") => dbo("name")
        case None => dbo("name")
        case _ => dbo("customName")
      }).toString),
      "description" -> dbo("name"),
      "latestmsg" -> dbo("latestmsg"),
      "speed" -> dbo("speed"),
      "ignition" -> dbo("ignition"),
      "satelliteNum" -> dbo("satelliteNum"),
      "hidden" -> (if (dbo("latestmsg") == null) true else hiddenObjects.contains(dbo("uid").asInstanceOf[String])),
      "leaf" -> true,
      "checked" -> selectedObjects.contains(dbo("uid").asInstanceOf[String]),
      "blocked" -> dbo.get("blocked").getOrElse(false),
      "targeted" -> targetedObjects.contains(dbo("uid").asInstanceOf[String]),
      //"controlEnabled" -> dbo.get("controlEnabled").getOrElse(false),
      "canBlock"-> dbo.get("block").getOrElse(false),
      "canGetCoords"-> dbo.get("getCoords").getOrElse(false),
      "canRestartTerminal"-> dbo.get("restartTerminal").getOrElse(false),
      "blockingEnabled" -> (dbo.getOrElse("ignitionLock", false) == true || dbo.getOrElse("fuelPumpLock", false) == true),
      "settingsViewEnabled" -> dbo.get("settingsViewEnabled").getOrElse(false),
      "sleeper" -> dbo("sleeper"),
      "radioUnit" -> dbo("radioUnit"),
      "settingsWnd" -> false
    )).toList

    val groupedObjects = objectGroups.flatMap(group => {
      val objects = group.get("objects")
        .map(dbl => dbl.asInstanceOf[BasicDBList]
        .map(dbo => dbo.asInstanceOf[BasicDBObject]
        .get("uid").asInstanceOf[String]))
        .get
      availableObjects
        .filter(map => { objects.contains(map("uid").asInstanceOf[String])})
        .map(dbo => {  dbo ++ Map("_id" -> new ObjectId() , "group" -> group("name"))})
    })
    groupedObjects.foreach(map=> debug(map))
    val ungroupedObjects =  availableObjects.filter(dbobject =>{
      val uid=dbobject("uid").asInstanceOf[String]
      !groupedObjects.exists(obj=> obj("uid").asInstanceOf[String]==uid)
    })
      .map(dbo => { dbo ++ Map("_id" -> new ObjectId(), "group" -> ("\uFF65 "+tr("main.groupsofobjects.withoutgroup")))})
    (groupedObjects ++ ungroupedObjects).toIterator
  }
}
