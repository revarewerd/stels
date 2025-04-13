package ru.sosgps.wayrecall.billing

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod
import com.mongodb.BasicDBObject
import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionValue}
import ru.sosgps.wayrecall.utils.ExtDirectService
import collection.JavaConversions.mapAsJavaMap
import collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable.ArrayBuffer


/**
 * Created by IVAN on 14.10.2014.
 */
@ExtDirectService
class SensorsSettings {

  @Autowired
  var permissionsChecker: ObjectsPermissionsChecker = null

  @Autowired
  var mdbm: MongoDBManager = null

  @ExtDirectMethod
  def loadObjectSettings(uid: String): collection.Map[String, Any] = {
    permissionsChecker.checkPermissions(uid, PermissionValue.VIEW_SETTINGS)
    mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("settings" -> 1))
      .get.getAs[BasicDBObject]("settings").map((m: BasicDBObject) => m.toMap.asScala.asInstanceOf[collection.Map[String, Any]]).getOrElse(Map.empty)
  }

  @ExtDirectMethod
  def loadObjectSensors(uid: String): collection.Seq[collection.Map[String, Any]] = {
    permissionsChecker.checkPermissions(uid, PermissionValue.EDIT_SENSORS)
    mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("sensors" -> 1))
      .get.getAs[MongoDBList]("sensors").map((m: MongoDBList) => m.toList.asInstanceOf[collection.Seq[collection.Map[String, Any]]]).getOrElse(Seq.empty)
  }

  @ExtDirectMethod
  def saveObjectSettings(uid: String, settings: Map[String, Any], params: Map[String, Any]): Unit = {
    permissionsChecker.checkPermissions(uid, PermissionValue.EDIT_SETTINGS)
    var query = ArrayBuffer[(String, Any)]("settings" -> new BasicDBObject(settings))
    if (params.contains("sensors")) {
      query += ("sensors" -> params("sensors"))
    }
    mdbm.getDatabase()("objects").update(MongoDBObject("uid" -> uid), $set(query:_*))
  }
}
