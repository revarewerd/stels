package ru.sosgps.wayrecall.monitoring.web

import org.springframework.security.core.context.SecurityContextHolder
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectDataSetCommand
import ru.sosgps.wayrecall.utils.ExtDirectService
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.events.EventsStore
import ru.sosgps.wayrecall.sms.SMSCommandProcessor
import ru.sosgps.wayrecall.core.{UserRolesChecker, SecureGateway, ObjectsRepositoryReader, MongoDBManager}
import javax.servlet.ServletContext
import java.io.{FilenameFilter, File}
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.casbah.Imports._
//import collection.JavaConversions.mapAsJavaMap
import collection.JavaConverters.mapAsScalaMapConverter
import com.mongodb.BasicDBObject
import ru.sosgps.wayrecall.security.{PermissionValue, ObjectsPermissionsChecker}
import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.06.13
 * Time: 18:08
 * To change this template use File | Settings | File Templates.
 */
@ExtDirectService
class ObjectSettings extends grizzled.slf4j.Logging  {

  @Autowired
  var permissionsChecker: ObjectsPermissionsChecker = null

  @Autowired
  val commandGateway: SecureGateway = null;

  @Autowired
  val roleChecker: UserRolesChecker = null;

  @Autowired
  var es: EventsStore = null

  @Autowired
  var smsCommandProcessor: SMSCommandProcessor = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null


  val tripsSettings = Set(
    "repTripsMinMovementSpeed",
    "repTripsMinParkingTime",
    "repTripsMaxDistanceBetweenMessages",
    "repTripsMinTripTime",
    "repTripsMinTripDistance",
    "repTripsUseIgnitionToDetectMovement",
    "repTripsMinSatelliteNum")



  val viewSettings = Set("imgArrow", "imgArrowSrc", "imgMarker", "imgRotate", "imgSize", "imgSource")
  @ExtDirectMethod
  def saveObjectSettings(uid: String, settings: Map[String, Any], params: Map[String, Any]): Unit = {
    permissionsChecker.checkPermissions(uid, PermissionValue.EDIT_SETTINGS)
    val hashSettings : HashMap[String,Any] = new HashMap[String,Any]() ++ settings
    var query: Map[String, java.io.Serializable] = Map.empty
    if (permissionsChecker.hasPermission(uid, PermissionValue.EDIT_FUELSETTINGS)) {
      query += ("settings" ->  hashSettings)
    } else {
      settings.filterKeys(viewSettings ++ tripsSettings).map(k => {
        query += (("settings." + k._1) -> k._2.asInstanceOf[java.io.Serializable])
      })
//      val fromDB = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("settings" -> 1))
//        .get.getAs[BasicDBObject]("settings").map((m: BasicDBObject) => m.toMap.asScala.asInstanceOf[collection.Map[String, Any]]).getOrElse(Map.empty)
    }
    
    if (params.contains("customName")) {
      debug("customName="+params("customName"))
      query += ("customName" -> params("customName").asInstanceOf[java.io.Serializable])
      //setCustomName(uid, params("customName").asInstanceOf[String])
    }
    if (params.contains("sensors") && permissionsChecker.hasPermission(uid, PermissionValue.EDIT_SENSORS)) {
      query += ("sensors" -> params("sensors").asInstanceOf[java.io.Serializable])
    }
    
    debug("Result save settings query = " + query)
    val objectData= mdbm.getDatabase()("objects").findOne(MongoDBObject("uid"->uid)).get
    val submitMap = scala.collection.mutable.Map.empty[String,java.io.Serializable]
    submitMap ++= objectData.mapValues(_.asInstanceOf[java.io.Serializable])
    submitMap ++= query
    commandGateway.sendAndWait(new ObjectDataSetCommand(uid,submitMap.toMap, None), roleChecker.getUserAuthorities, roleChecker.getUserName);
  }

  @ExtDirectMethod
  def loadObjectSettings(uid: String): collection.Map[String, Any] = {
    permissionsChecker.checkPermissions(uid, PermissionValue.VIEW_SETTINGS)
    val settings = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("settings" -> 1))
      .get.getAs[BasicDBObject]("settings").map((m: BasicDBObject) => m.toMap.asScala.asInstanceOf[collection.Map[String, Any]]).getOrElse(Map.empty)
      
    val sensorsEnabled = if (permissionsChecker.hasPermission(uid, PermissionValue.EDIT_SENSORS)) Map.empty else Map("hideSensorSettings" -> true)
    val editingEnabled = if (permissionsChecker.hasPermission(uid, PermissionValue.EDIT_SETTINGS)) Map.empty else Map("hideSaveButtons" -> true)
    if (permissionsChecker.hasPermission(uid, PermissionValue.EDIT_FUELSETTINGS)) {
      settings ++ sensorsEnabled ++ editingEnabled
    } else {
      settings.filterKeys(viewSettings ++ tripsSettings) ++ Map("hideFuelSettings" -> true) ++ sensorsEnabled ++ editingEnabled
    }
  }

  @ExtDirectMethod
  def loadObjectSensors(uid: String): collection.Seq[collection.Map[String, Any]] = {
    permissionsChecker.checkPermissions(uid, PermissionValue.EDIT_SENSORS)
    mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("sensors" -> 1))
    .get.getAs[MongoDBList]("sensors").map((m: MongoDBList) => m.toList.asInstanceOf[collection.Seq[collection.Map[String, Any]]]).getOrElse(Seq.empty)
  }

  @ExtDirectMethod
  def loadObjectMapSettings(uid: String): collection.Map[String, Any] = {
    permissionsChecker.checkPermissions(uid, PermissionValue.VIEW)
    mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("settings.imgSource" -> 1, "settings.imgRotate" -> 1, "settings.imgArrow" -> 1, "settings.imgArrowSrc" -> 1))
      .get.getAs[BasicDBObject]("settings").map((m: BasicDBObject) => m.toMap.asScala.asInstanceOf[collection.Map[String, Any]]).getOrElse(Map.empty)
  }

}

@ExtDirectService
class ObjectImagesStore extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("name", "src", "size")
  val name = "ObjectImages"
  //override val storeType: String = "Ext.data.TreeStore"
  override val autoSync = false
  val idProperty = "name"
  val lazyLoad = false

  private[this] val namereg = """(\w*?)(\d\d).png""".r

  @Autowired
  var context:ServletContext = null;

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = false)
  def loadObjects(request: ExtDirectStoreReadRequest): Seq[Map[String, Any]] = {
    new File( context.getRealPath("images/cars")).listFiles(new FilenameFilter {
      def accept(dir: File, name: String) = name.endsWith(".png")
    }).toSeq.map(f => f.getName match {
      case namereg(iname, size) => Map("name" -> f.getName, "src" -> ("images/cars/" + f.getName), "size" -> size)
      case _ => Map("name" -> ("??"+f.getName), "src" -> ("images/cars/" + f.getName), "size" -> 32 )
    })
  }

}
