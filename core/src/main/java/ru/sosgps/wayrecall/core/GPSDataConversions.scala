

package ru.sosgps.wayrecall.core

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import ru.sosgps.wayrecall.utils.tryNumerics
import ru.sosgps.wayrecall.wialonparser.WialonPackage
import ru.sosgps.wayrecall.wialonparser.WialonCoordinatesBlock
import ru.sosgps.wayrecall.wialonparser.WialonPackageBlock
import java.util.Date
import java.util
import collection.JavaConversions.mapAsScalaMap
import collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ListBuffer

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 06.03.13
 * Time: 19:25
 * To change this template use File | Settings | File Templates.
 */
object GPSDataConversions extends grizzled.slf4j.Logging {

  val PRIVATEDATA_FIELD = "privateData"

  def toMongoDbObject(gpsdata: GPSData): DBObject = {

    val result = MongoDBObject(
      "uid" -> gpsdata.uid,
      "imei" -> gpsdata.imei,
      "time" -> gpsdata.time
    )

    if (gpsdata.containsLonLat())
      result ++= MongoDBObject(
        "lon" -> gpsdata.lon,
        "lat" -> gpsdata.lat,
        "spd" -> gpsdata.speed,
        "crs" -> gpsdata.course,
        "stn" -> gpsdata.satelliteNum
      )

    if (gpsdata.placeName != null) {
      result("pn") = gpsdata.placeName
    }

    if (gpsdata.insertTime != null) {
      result("insTime") = gpsdata.insertTime
    }

    if (gpsdata.data.size() > 0) {
      result.put("data", new BasicDBObject(gpsdata.data))
    }

    if (gpsdata.privateData != null && gpsdata.privateData.size() > 0) {
      result.put(PRIVATEDATA_FIELD, new BasicDBObject(gpsdata.privateData))
    }

    result
  }


  def fromDbo(dbo: DBObject) = fromMongoDBObject(dbo)

  def fromMongoDBObject(dbo: MongoDBObject) = {
    val result = new GPSData(
      dbo.getAs[String]("uid").orNull,
      dbo.getAs[String]("imei").orNull,
      dbo.getAs[Double]("lon").getOrElse(Double.NaN),
      dbo.getAs[Double]("lat").getOrElse(Double.NaN),
      dbo.as[Date]("time"),
      dbo.get("spd").map(_.tryInt.toShort).getOrElse(-1.toShort),
      dbo.get("crs").map(_.tryInt.toShort).getOrElse(-1.toShort),
      dbo.get("stn").map(_.tryInt.toByte).getOrElse(-1.toByte),
      dbo.getAs[String]("pn").orNull,
      dbo.getAs[Date]("insTime").orNull
    )

    result.data = dbo.getAs[DBObject]("data").map(toKeyInternedHashMap).getOrElse(result.data)

    result.privateData = dbo.getAs[DBObject](PRIVATEDATA_FIELD).map(toKeyInternedHashMap).getOrElse(result.privateData)

    result
  }


  private def toKeyInternedHashMap(dbo: Imports.DBObject): util.HashMap[String, AnyRef] = {
    val map = new util.HashMap[String, AnyRef]()
    for ((k, v) <- dbo.toMap.asInstanceOf[util.Map[String, AnyRef]]) {
      map.put(k.intern(), v)
    }
    map
  }

  def fromWialonPackage(uid: String, pack: WialonPackage): GPSData = {

    import scala.collection.JavaConversions.iterableAsScalaIterable

    val coordinates = pack.getCoordinatesBlock
    val data = if (coordinates != null) {
      var course = coordinates.course
      if (course < 0) {
        warn("negative course " + pack)
        while (course < 0)
          course = (course + 360).toShort
      }


      val gps = new GPSData(
        uid,
        pack.imei,
        coordinates.lon,
        coordinates.lat,
        pack.time,
        coordinates.speed,
        course,
        coordinates.satelliteNum
      )
      gps.data.put("alt", coordinates.height.asInstanceOf[AnyRef])
      gps
    }
    else {
      new GPSData(uid, pack.imei, pack.time)
    }

    for (x <- pack.blocks.filter(_ != coordinates)) {
      data.data.put(x.name, x.value)
    }

    Option(data.data.get("protocol"))
      .foreach(data.data.put("protocolPrev", _))

    data.data.put("protocol", "Wialon")

    data
  }

  //private val nonWlnFileds = Set("alt", "protocol")
  //private val nonWlnFileds = Set("alt")

  def toWialonPackage(gps: GPSData): WialonPackage = {
    val wln = new WialonPackage
    wln.imei = gps.imei
    wln.time = gps.time
    wln.flags = 3

    val block = new WialonCoordinatesBlock
    block.name = "posinfo"
    block.hidden = true
    block.`type` = 3003
    block.dataType = 2
    block.size = 39
    block.lon = gps.lon
    block.lat = gps.lat
    block.height = (gps.data: scala.collection.mutable.Map[String, AnyRef]).get("alt").map {
      case d: java.lang.Double => d.doubleValue()
      case s: java.lang.Short => s.doubleValue()
      case s: java.lang.Integer => s.doubleValue()
      case s: java.lang.Long => s.doubleValue()
      case e => {warn("cannot convert alt of type:" + e.getClass + " gps:" + gps); Double.NaN}
    }.getOrElse(Double.NaN)
    block.course = gps.course
    block.speed = gps.speed
    block.satelliteNum = gps.satelliteNum

    wln.blocks.add(block)

    for ((k, v) <- gps.data.toSeq.filter(kv => kv !=("protocol", "Wialon") && kv._1 != "alt").reverse) {
      //for ((k, v) <- gps.data.toSeq.reverse) {
      val block = new WialonPackageBlock
      block.name = k
      block.value = v
      block.`type` = 3003
      var variableSize = 0
      v match {
        case d: java.lang.Double => {
          block.dataType = 0x4
          variableSize = 8
        }
        case d: java.lang.Float => {
          block.dataType = 0x4
          block.value = d.toDouble.asInstanceOf[java.lang.Double]
          variableSize = 8
        }
        case d: java.lang.Long => {
          block.dataType = 0x5
          variableSize = 8
        }
        case d: java.lang.Integer => {
          block.dataType = 0x3
          variableSize = 4
        }
        case d: java.lang.Short => {
          block.dataType = 0x3
          block.value = d.toInt.asInstanceOf[java.lang.Integer]
          variableSize = 4
        }
        case d: java.lang.Byte => {
          block.dataType = 0x3
          block.value = d.toInt.asInstanceOf[java.lang.Integer]
          variableSize = 4
        }
        case d: String => {
          block.dataType = 0x1
          variableSize = d.length + 1
        }
        case d: Array[Byte] => {
          block.dataType = 0x2
          variableSize = d.length
        }
        case d: java.lang.Boolean => {
          block.dataType = 0x3
          block.value = (if (d) 1 else 0): java.lang.Integer
          variableSize = 4
        }
        case e => {
          warn("value " + e + " with name " + k + " of type " + e.getClass + " is not matched")
        }
      }



      if (variableSize > 0) {
        block.size = k.length + 2 + 1 + variableSize
        wln.blocks.add(block)
      }
    }
    wln.size = 4 + wln.imei.length + 4 + 4 + 4 + wln.blocks.map(_.size + 5).sum
    wln
  }

}
