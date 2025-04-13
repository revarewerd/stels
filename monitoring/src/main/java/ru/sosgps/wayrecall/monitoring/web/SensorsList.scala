/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.monitoring.web

import java.util.regex.Pattern
import ru.sosgps.wayrecall.utils;
import utils.web.extjsstore.StoreAPI
import utils.containsMatcher
import ru.sosgps.wayrecall.core.{MongoDBManager, ObjectsRepositoryReader, Translations}
import com.mongodb.casbah.Imports._

import utils.ExtDirectService
import utils.web.extjsstore.EDSStoreServiceDescriptor
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.springframework.beans.factory.annotation.Autowired
import collection.immutable.IndexedSeq
import scala.collection.JavaConversions._
import scala.Predef._
import java.util.Date
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionValue}
import scala.collection.mutable

@ExtDirectService
class SensorsList extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model(
    "code",
    "name",
    "params"
  )
  val name = "SensorsList"
  //override val storeType: String = "Ext.data.TreeStore"
  override val autoSync = false
  val idProperty = "code"
  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null
  
  @Autowired
  var or: ObjectsRepositoryReader = null
  
  @Autowired
  var permissions: ObjectsPermissionsChecker = null
  
  @Autowired
  var translations: Translations = null
  
  val sensorHide = Set("ME_Error_code", "ME_Error_valid", "ME_Left_signal", "ME_Low_beam", "ME_Night_time_indicator", "ME_Pedestrians_FCW", "ME_Right_LDW_on", "ME_Speed", "ME_Wipers_available", "cid", "lac", "lbs", "mainTerminal", "mcc", "mileage", "miliage", "mnc", "mode", "protocol", "ruptelaEventDataSource", "ruptelaEventName", "ruptelaEventSource", "text", "ME_Error_code", "ME_Error_valid", "alarm", "alarms", "battery", "cid", "cmd_answer")
  
  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def loadObjects(request: ExtDirectStoreReadRequest) = {
    val objects = request.getParams.get("uids")
    debug("Object's uids = " + objects)
    
    if (objects != null) {
      objects match {
        case uids: java.util.ArrayList[String] => {
            val allsensors = uids.map(
              uid => {
                getObjectSensorsCodenames(uid).map(_.get("param").get).toSet
              }
            )
            val commonSet: Set[String] = allsensors.reduceOption(_ & _).getOrElse(Set.empty)
            debug("commonSet = " + commonSet)
            commonSet.map(s => {
                Map("code" -> s, "name" -> s)
              }
            ).toSeq
//            Seq(Map("code" -> "pwr_ext", "name" -> "Датчик напряжения", "params" -> ""))
        }
        case _ => {
            Seq.empty
        }
      }
    } else {
      Seq.empty
    }
  }
  
  @ExtDirectMethod
  def getCommonTypes() = {
    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr
    
    Seq(
      Map("id" -> "sFuelFP", "type" -> tr("sensorstype.sFuelFP"), "unit" -> ""),
      Map("id" -> "sFuelFA", "type" -> tr("sensorstype.sFuelFA"), "unit" -> tr("units.fuellevel")),
      Map("id" -> "sFuelFM", "type" -> tr("sensorstype.sFuelFM"), "unit" -> ""),
      Map("id" -> "sFuelL", "type" -> tr("sensorstype.sFuelL"), "unit" -> tr("units.fuellevel")),
      Map("id" -> "sFuelLP", "type" -> tr("sensorstype.sFuelLP"), "unit" -> tr("units.fuellevel")),
      Map("id" -> "sTmp", "type" -> tr("sensorstype.sTmp"), "unit" -> tr("units.temperature")),
      Map("id" -> "sEngS", "type" -> tr("sensorstype.sEngS"), "unit" -> tr("units.rpm")),
      Map("id" -> "sIgn", "type" -> tr("sensorstype.sIgn"), "unit" -> ""),
      Map("id" -> "sPwr", "type" -> tr("sensorstype.sPwr"), "unit" -> tr("units.voltage")),
      Map("id" -> "sDist", "type" -> tr("sensorstype.sDist"), "unit" -> tr("units.distance")),
      Map("id" -> "sEngW", "type" -> tr("sensorstype.sEngW"), "unit" -> ""),
      Map("id" -> "sAnyD", "type" -> tr("sensorstype.sAnyD"), "unit" -> ""),
      Map("id" -> "sAnyS", "type" -> tr("sensorstype.sAnyS"), "unit" -> ""),
      Map("id" -> "sEngHA", "type" -> tr("sensorstype.sEngHA"), "unit" -> tr("units.hour")),
      Map("id" -> "sEngHR", "type" -> tr("sensorstype.sEngHR"), "unit" -> tr("units.hour")),
      Map("id" -> "sTmpC", "type" -> tr("sensorstype.sTmpC"), "unit" -> ""),
      Map("id" -> "sOdoR", "type" -> tr("sensorstype.sOdoR"), "unit" -> tr("units.distance")),
      Map("id" -> "sAccr", "type" -> tr("sensorstype.sAccr"), "unit" -> ""),
      Map("id" -> "sCntr", "type" -> tr("sensorstype.sCntr"), "unit" -> "")
    )
    
  }
  
  @ExtDirectMethod
  def getObjectSensorsCodenames(selectedUid: String) = {
    if (permissions.hasPermission(selectedUid, PermissionValue.VIEW)) {
      val r = for(
        imei <- or.getMainTerminal(selectedUid).flatMap(_.getAs[String]("eqIMEI"));
        sensorsList <- mdbm.getDatabase()("sensorNames").findOne(MongoDBObject("imei" -> imei))
      ) yield sensorsList.getAsOrElse[Seq[String]]("params", Seq.empty)

      
      (r.getOrElse(Seq.empty).toSet -- sensorHide).map({p => Map("param" -> p)})
//      r.foreach(p => Map("param" -> p))
    } else {
      Seq.empty
    }
    
  }
}