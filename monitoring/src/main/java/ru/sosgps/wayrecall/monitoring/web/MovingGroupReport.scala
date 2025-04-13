package ru.sosgps.wayrecall.monitoring.web

import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.tariff.TariffEDS
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.monitoring.processing.parkings.{MovingState, MovingStatesExtractor, ObjectTripSettings}
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

import scala.collection.JavaConversions.mapAsScalaMap
//import ru.sosgps.wayrecall.data.sleepers.Moving
import org.joda.time.DateTime

@ExtDirectService
class MovingGroupReport extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("sDate", "fDate", "parkingInterval", "movingInterval", "distance", "pointsCount", "maxSpeed", "sRegeo", "fRegeo")
  val name = "MovingGroupReport"
  val idProperty = "sDate"
  val lazyLoad = false

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var tariffEDS: TariffEDS = null

  @Autowired
  var mdbm: MongoDBManager = _


  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest): Iterator[Map[String, Any]] = {

    val (from, to) = tariffEDS.correctReportWorkingDates(ReportsTransitionalUtils.getWorkingDates(request.getParams))
    val selectedUid: String = ReportsTransitionalUtils.getSelectedId(request.getParams)

    debug("building MovingGroupReport for " + selectedUid + " " + (from, to))
    val jFrom = new DateTime(from)
    val jTo = new DateTime(to)
    var stepTime = jFrom.withTimeAtStartOfDay.plusDays(1)
    val dateRange = Iterator.iterate(stepTime)(_.plusDays(1)).takeWhile(!_.isAfter(jTo)) ++ Iterator(jTo)

    stepTime = jFrom
    (for (d <- dateRange) yield {
      val repData = getReportData(selectedUid, stepTime.toDate, d.toDate)

      var pointsCount = 0
      var distance = 0.0
      var maxSpeed = 0.0
      var parkingInterval: Long = 0
      var movingInterval: Long = 0
      var sRegeo = "0"
      var fRegeo = ""
      repData.foreach(p => {
        sRegeo = if (sRegeo == "0") Option(p.first.placeName).getOrElse("") else sRegeo
        pointsCount += p.pointsCount
        distance += p.distance
        maxSpeed = if (p.maxSpeed > maxSpeed) p.maxSpeed else maxSpeed
        parkingInterval = if (p.isParking) parkingInterval + p.intervalMills else parkingInterval
        movingInterval = if (!p.isParking) movingInterval + p.intervalMills else movingInterval
        fRegeo = Option(p.last.placeName).getOrElse("")
      }
      )

      val map = Map(
        "sDate" -> ReportsTransitionalUtils.ISODateTime(stepTime.toDate),
        "fDate" -> ReportsTransitionalUtils.ISODateTime(d.toDate),
        "pointsCount" -> pointsCount,
        "distance" -> distance,
        "maxSpeed" -> maxSpeed,
        "parkingInterval" -> ReportsTransitionalUtils.formatPeriod(parkingInterval),
        "movingInterval" -> ReportsTransitionalUtils.formatPeriod(movingInterval),
        "sRegeo" -> sRegeo,
        "fRegeo" -> fRegeo
      )
      stepTime = d
      map
    }).filter(p => p("pointsCount") != 0)
  }


  def getReportData(selectedUid: String, from: Date, to: Date): Iterator[MovingState] = {
    val history = packStore.getHistoryFor(selectedUid, from, to) //.toStream

    //    val writer = new DboWriter("report3.bson")
    //    writer.write(history.map(GPSDataConversions.toPerObjectMongoDb) : _*)
    //    writer.close()
    val settings = new ObjectTripSettings(selectedUid, mdbm)
    new MovingStatesExtractor().detectStates(history.iterator, settings).toIterator
  }

  @ExtDirectMethod
  def getReportPerDay(uid: String, from: Date, to: Date) = {

    val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(from, to)

    val repData = getReportData(uid, correctFrom, correctTo)

    repData.map(
      p => Map(
        "sDate" -> ReportsTransitionalUtils.ISODateTime(p.first.time),
        "fDate" -> ReportsTransitionalUtils.ISODateTime(p.lastTime),
        "interval" -> ReportsTransitionalUtils.formatPeriod(p.intervalMills),
        "intervalRaw" -> p.intervalMills,
        "distance" -> p.distance,
        "pointsCount" -> p.pointsCount,
        "maxSpeed" -> p.maxSpeed,
        "sLon" -> p.first.lon.toString,
        "sLat" -> p.first.lat.toString,
        "fLon" -> p.last.lon.toString,
        "fLat" -> p.last.lat.toString,
        "sRegeo" -> Option(p.first.placeName).getOrElse(""),
        "fRegeo" -> Option(p.last.placeName).getOrElse(""),
        "isParking" -> p.isParking
      )
    ).toIterator
  }

}