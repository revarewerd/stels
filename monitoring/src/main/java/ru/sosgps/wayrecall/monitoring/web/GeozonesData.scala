package ru.sosgps.wayrecall.monitoring.web

import java.util.regex.Pattern

import ru.sosgps.wayrecall.monitoring.geozones.{Geozone, GeozonesStore}
import ru.sosgps.wayrecall.utils
import utils.web.extjsstore.StoreAPI
import utils.typingMap
import ru.sosgps.wayrecall.core.{GPSData, MongoDBManager}
import com.mongodb.casbah.Imports._
import utils.ExtDirectService
import utils.web.extjsstore.EDSStoreServiceDescriptor
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.springframework.beans.factory.annotation.Autowired

import collection.immutable.IndexedSeq
import scala.Predef._
import java.util.Date

import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConverters.mapAsJavaMapConverter
import java.util

import org.deegree.cs.coordinatesystems.ICRS
import ru.sosgps.wayrecall.utils.web.ScalaJson
import ru.sosgps.wayrecall.events.EventsStore
import ru.sosgps.wayrecall.data.GPSEvent


@ExtDirectService
class GeozonesData extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model(
    "id",
    "name",
    "ftColor",
    "bounds",
    "points"
  )
  val name = "GeozonesData"
  //override val storeType: String = "Ext.data.TreeStore"
  override val autoSync = false
  val idProperty = "id"
  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var gzs: GeozonesStore = null

  @Autowired
  var permissions: ObjectsPermissionsChecker = null

  def toModelInstance(geoz: Geozone) =
  {
    Map(
      "id" -> geoz.id,
      "name" -> geoz.name,
      "ftColor" -> geoz.color,
      //"bounds" -> "",
      "points" -> ScalaJson.writeValueAsString(CoordsConverter.transform4326to900913(
        geoz.points.map(p => Map("x" -> p._1, "y" -> p._2).asJava)
      )))
  }

  @ExtDirectMethod
  def loadById(id: Int) =
  {
    val geozone = gzs.getById(id)
    if(geozone.user != permissions.username)
      throw new IllegalAccessException(s"Geozone user name '${geozone.user}' does not match supplied '${permissions.username}'")
    toModelInstance(geozone)
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def loadObjects(request: ExtDirectStoreReadRequest) = {

    //    val geozonesList = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> permissions.username),
    //      MongoDBObject("geozones" -> 1)).flatMap(_.getAs[MongoDBList]("geozones")).getOrElse(new MongoDBList())
    //
    //    val r = geozonesList.map(_.asInstanceOf[DBObject]).map(
    //      dbo => Map(
    //        "name" -> dbo("name"),
    //        "ftColor" -> dbo("ftColor"),
    //        "bounds" -> dbo("bounds").toString,
    //        "points" -> ScalaJson.writeValueAsString(CoordsConverter.transform4326to900913(
    //          dbo.as[MongoDBList]("points").underlying.asInstanceOf[util.List[util.Map[String, Double]]]
    //        ))
    //      )).toList

    val usersGeozones = gzs.getUsersGeozones(permissions.username)
    //    usersGeozones.foreach(geoz => {
    //      debug("geoz=" + geoz.name)
    //      geoz.points.foreach(p => println(p._1 + "\t" + p._2))
    //    })
    //debug("usersGeozones="+usersGeozones)
    val r = usersGeozones.map(toModelInstance)

    //debug("loadObjects="+r)
    r.iterator
  }

  @ExtDirectMethod
  def addGeozone(newZone0: Map[String, Any]) = {
    debug("Adding geozone " + newZone0)

    val newZone = newZone0.updated(
      "points",
      CoordsConverter.transform900913to4326(newZone0.as[util.List[util.Map[String, Double]]]("points"))
    )

    debug("Adding geozone converted" + newZone)

    gzs.addGeozone(
      permissions.username,
      newZone.as[String]("name"),
      newZone.as[String]("ftColor"),
      newZone.as[util.List[util.Map[String, Double]]]("points").map(m => (m.get("x"), m.get("y")))
    )

    true

    //    val findZone = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> permissions.username, "geozones.name" -> newZone("name")), MongoDBObject("geozones.$" -> 1))
    //    debug("Found geozone " + findZone)
    //    var result = true
    //    if (findZone == None) {
    //      mdbm.getDatabase()("users").update(MongoDBObject("name" -> permissions.username), $push("geozones" -> (newZone)))
    //    } else {
    //      result = false
    //    }
    //    result
  }

  @ExtDirectMethod
  def editGeozone(geoZone0: Map[String, Any]) = {
    debug("Editing geozone " + geoZone0)

    val geoZone = geoZone0.updated(
      "points",
      CoordsConverter.transform900913to4326(geoZone0.as[util.List[util.Map[String, Double]]]("points"))
    )

    debug("Editing geozone converted" + geoZone)

    gzs.editGeozone(
      geoZone.as[Int]("id"),
      permissions.username,
      geoZone.as[String]("name"),
      geoZone.as[String]("ftColor"),
      geoZone.as[util.List[util.Map[String, Double]]]("points").map(m => (m.get("x"), m.get("y")))
    )

    true
  }

  @ExtDirectMethod
  def delGeozone(id: Int) {
    debug("Deleting geozone " + id)
    gzs.deleteGeozone(permissions.username, id)
    //mdbm.getDatabase()("users").update(MongoDBObject("name" -> permissions.username), $pull("geozones" -> MongoDBObject("name" -> zoneName)))
  }

  @Autowired
  var es: EventsStore = null

  @ExtDirectMethod
  def testPoint(lon: Double, lat: Double, speed: Int, pwr_ext: Double) = {
    debug("testPoint: " + lon + ", " + lat)
    val (rlon, rlat) = CoordsConverter.transform(lon, lat, CoordsConverter.to4326, CoordsConverter.crs900913)
    debug("testPointresults: " + rlon + ", " + rlat)
    val geozones = gzs.getUsersGeozonesWithPoint(permissions.username, rlon, rlat)
    //debug("geozones: " +geozones)

    val gps = new GPSData("o639670882911962921", "352848023740703", rlon, rlat, new Date(), speed.toShort, 0, 14)
    gps.data.put("pwr_ext", pwr_ext.asInstanceOf[AnyRef])
    es.publish(new GPSEvent(gps))

    Map(
      "lon" -> rlon,
      "lat" -> rlat,
      "geozones" -> geozones.map(_.name)
    )
  }

  //  @ExtDirectMethod
  //  def updNotificationRule(newRule: Map[String, Any]) {
  //    debug("Updating notification rule " + newRule)
  //    mdbm.getDatabase()("users").update(MongoDBObject("name" -> permissions.username, "notificationRules.name" -> newRule("name")), $set("notificationRules.$" -> newRule))
  //  }
}


object CoordsConverter extends grizzled.slf4j.Logging {

  import org.deegree.cs.coordinatesystems.ICRS
  import org.deegree.feature.types.AppSchema
  import org.deegree.geometry.GeometryTransformer
  import org.deegree.geometry.standard.primitive.DefaultPoint
  import scala.collection.JavaConversions.seqAsJavaList
  import scala.collection.JavaConversions.asScalaBuffer
  import scala.Array

  val crs4326: ICRS = org.deegree.cs.persistence.CRSManager.lookup("EPSG:4326")
  val crs900913: ICRS = org.deegree.cs.persistence.CRSManager.lookup("EPSG:900913")

  val to4326 = new GeometryTransformer(crs4326)
  val to900913 = new GeometryTransformer(crs900913)


  def transform900913to4326(coords: util.List[util.Map[String, Double]]): util.List[util.Map[String, Double]]
  = coords.map(transform(_, to4326, crs900913))

  def transform4326to900913(coords: util.List[util.Map[String, Double]]): util.List[util.Map[String, Double]]
  = coords.map(transform(_, to900913, crs4326))

  def transform900913to4326(coords: java.util.Map[String, Double]): java.util.Map[String, Double]
  = transform(coords, to4326, crs900913)

  def transform4326to900913(coords: java.util.Map[String, Double]): java.util.Map[String, Double]
  = transform(coords, to900913, crs4326)

  def transform(coords: java.util.Map[String, Double],
                tranformer: GeometryTransformer,
                srcICRS: ICRS): java.util.Map[String, Double] = {

    val x = coords.get("x").asInstanceOf[Double]
    val y = coords.get("y").asInstanceOf[Double]

    val (rx, ry) = transform(x, y, tranformer, srcICRS)

    val result = new util.HashMap[String, Double]()
    result.put("x", rx)
    result.put("y", ry)
    result
  }


  def transform(x: Double, y: Double, tranformer: GeometryTransformer, srcICRS: ICRS) = {
    val point = tranformer.transform(new DefaultPoint(null, null, null, Array(x, y)), srcICRS)
    (point.get0(), point.get1())
  }
}