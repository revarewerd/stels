package ru.sosgps.wayrecall.monitoring.web

import ru.sosgps.wayrecall.utils.ExtDirectService
import java.util.{TimeZone, Date}
import ch.ralscha.extdirectspring.annotation.ExtDirectMethod
import java.text.DateFormat
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import ru.sosgps.wayrecall.core.MongoDBManager
import com.mongodb.DBObject
import scala.beans.BeanProperty
import ru.sosgps.wayrecall.data.PackagesStore

//import com.mongodb.casbah.Imports._

import com.mongodb.casbah.Implicits._
import com.mongodb.casbah.query.Imports._
import com.mongodb.casbah.query.dsl.FluidQueryBarewordOps


@ExtDirectService
class PositionService extends grizzled.slf4j.Logging {

  @Autowired
  var packStore: PackagesStore = null;

  @BeanProperty
  var maxQueryRadius = 0.01

  @ExtDirectMethod
  def getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius0: Double): Map[String, Any] = {

    var radius = radius0

    trace("getNearestPosition:" +(uid, from, DateFormat.getInstance().format(to), lon, lat))
    trace("radius=" + radius)
    if (maxQueryRadius > 0 && radius > maxQueryRadius) {
      radius = maxQueryRadius
      trace("radiustrimmed=" + radius)
    }

    val starttime = System.currentTimeMillis();
    packStore.getNearestPosition(uid, from, to, lon, lat, radius).foreach(gpsdata => debug("GPS DATA="+gpsdata.toString))
    val res = packStore.getNearestPosition(uid, from, to, lon, lat, radius)
      .map(gps => Map(
      "lon" -> gps.lon,
      "lat" -> gps.lat,
      "time" -> gps.time,
      "speed" -> gps.speed,
      "course" -> gps.course,
      "placeName" ->gps.placeName
    ))

    debug("GPS DATA="+res.toString)

    trace("getNearestPosition:" +(uid, from, DateFormat.getInstance().format(to), lon, lat) +
      " with radius=" + radius + " took " + (System.currentTimeMillis() - starttime) + "ms" + " res=" + res.map(_.toString()).getOrElse("noresult"))

    res.getOrElse(Map("error" -> "noresult"))
  }


  @ExtDirectMethod
  def getIndex(uid: String, from: Date, cur: Date): Map[String, Any] = {
    trace("getIndex" +(uid: String, from: Date, cur: Date))
    val res = packStore.getPositionsCount(uid: String, from: Date, cur: Date)
    trace("res.count=" + res)
    Map("n" -> res)
  }


}
