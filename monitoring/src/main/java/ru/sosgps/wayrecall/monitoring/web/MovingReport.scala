package ru.sosgps.wayrecall.monitoring.web

import java.text.{DecimalFormat, SimpleDateFormat}
import java.util.{Date, SimpleTimeZone}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.BasicDBList
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.tariff.TariffEDS
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.monitoring.processing.parkings.{MovingState, MovingStatesExtractor, ObjectTripSettings}
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionValue}
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConverters.{asJavaCollectionConverter, mapAsJavaMapConverter}
//import ru.sosgps.wayrecall.data.sleepers.Moving
import ru.sosgps.wayrecall.monitoring.processing.parkings.Moving

@ExtDirectService
@deprecated
class TripReport extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var tariffEDS: TariffEDS = null

  val model = Model("num", "type", "sDatetime", "fDatetime", "interval", "intervalRaw", "distance", "pointsCount", "maxSpeed", "sLon", "sLat", "fLon", "fLat", "sRegeo", "fRegeo")
  val name = "TripReport"
  val idProperty = "datetime"
  val lazyLoad = false


  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest) = {

    val (from, to) = tariffEDS.correctReportWorkingDates(ReportsTransitionalUtils.getWorkingDates(request.getParams))

    val selectedUid: String = ReportsTransitionalUtils.getSelectedId(request.getParams)

    debug("building MovingReport for " + selectedUid + " " + (from, to))

    val states  = getStates(selectedUid, from, to)

    prepareReport(states)
  }

  def prepareReport(movingStates: Iterable[MovingState]) = {
    movingStates.zipWithIndex.map({
      case (p, i) =>
        Map("num" -> (i + 1),
          "type" -> (if(p.isParking) "Parking" else "Trip"),
        "sDatetime" -> ReportsTransitionalUtils.ISODateTime(p.first.time),
        "sDatetimeRaw" -> p.first.time,
        "fDatetime" -> ReportsTransitionalUtils.ISODateTime(p.lastTime),
        "fDatetimeRaw" -> p.lastTime,
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
        "fRegeo" -> Option(p.last.placeName).getOrElse("")
      )
    })
  }

  @Autowired
  var mdbm: MongoDBManager = _

  def getStates(uid: String, from: Date, to: Date) = {
    val history = packStore.getHistoryFor(uid, from, to)
    val settings = new ObjectTripSettings(uid, mdbm)

    new MovingStatesExtractor().detectStates(history.iterator, settings).toArray.sortBy(_.firstTime)
  }
}

@ExtDirectService
class MovingReport extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("num", "sDatetime", "fDatetime", "interval", "intervalRaw", "distance", "pointsCount", "maxSpeed", "sLon", "sLat", "fLon", "fLat", "sRegeo", "fRegeo")
  val name = "MovingReport"
  val idProperty = "datetime"
  val lazyLoad = false

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var translations: Translations = null

  @Autowired
  var permissions: ObjectsPermissionsChecker = null

  @Autowired
  var tariffEDS: TariffEDS = null

  @Autowired
  var mdbm: MongoDBManager = _

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest) = {

    val (from, to) = tariffEDS.correctReportWorkingDates(ReportsTransitionalUtils.getWorkingDates(request.getParams))

    val selectedUid: String = ReportsTransitionalUtils.getSelectedId(request.getParams)

    debug("building MovingReport for " + selectedUid + " " + (from, to))

    val movings = getMovings(selectedUid, from, to)

    prepareReport(movings)
  }

  def prepareReport(movings: Iterable[Moving]) = {
    movings.zipWithIndex.map({
      case (p, i) => Map(
        "num" -> (i + 1),
        "sDatetime" -> ReportsTransitionalUtils.ISODateTime(p.first.time),
        "sDatetimeRaw" -> p.first.time,
        "fDatetime" -> ReportsTransitionalUtils.ISODateTime(p.lastTime),
        "fDatetimeRaw" -> p.lastTime,
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
        "fRegeo" -> Option(p.last.placeName).getOrElse("")
      )
    })
  }


  def getMovings(selectedUid: String, from: Date, to: Date) = {
    val history = packStore.getHistoryFor(selectedUid, from, to)
    val settings = new ObjectTripSettings(selectedUid,mdbm)
    new MovingStatesExtractor().detectStates(history.iterator,settings).collect { case e: Moving => e }
  }

  def genJRDataSource(selected: String, from: Date, to: Date) = {
    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val movings = getMovings(selected, from, to)
    val dateFormat = new SimpleDateFormat(tr("format.scala.datetime"))
    val nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale(tr("language"), tr("country")))
    val df = nf.asInstanceOf[DecimalFormat]
    df.applyPattern("#,##0.##")

    prepareReport(movings).map(p => {
      Map(
        "num" -> p("num"),
        "datetime" -> dateFormat.format(p("sDatetimeRaw")),
        "interval" -> p("interval"),
        "distance" -> (df.format(p("distance")) + " " + tr("movinggroupgrid.km")),
        "pointsCount" -> p("pointsCount"),
        "maxSpeed" -> (p("maxSpeed") + " " + tr("mapobject.laststate.kmsperhour")),
        "sRegeo" -> p("sRegeo"),
        "fRegeo" -> p("fRegeo")
      ).asJava
    }).toSeq.asJavaCollection.asInstanceOf[java.util.Collection[java.util.Map[String, _]]]

  }


}

@ExtDirectService
class GroupMovingReport extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("uid", "name", "sDate", "fDate", "parkingInterval", "parkingCount", "movingInterval", "movingCount", "distance", "pointsCount", "maxSpeed", "sRegeo", "fRegeo")
  val name = "GroupMovingReport"
  val idProperty = "_id"
  val lazyLoad = false


  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var translations: Translations = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var permissions: ObjectsPermissionsChecker = null

  @Autowired
  var tariffEDS: TariffEDS = null

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest): Seq[Map[String, Any]] = {

    val (from, to) = tariffEDS.correctReportWorkingDates(ReportsTransitionalUtils.getWorkingDates(request.getParams))

    val timezone = tryLong(request.getParams.get("timezone")).toInt
    val groupId: String = request.getParams.get("selected").toString
    val objectsGroup = mdbm.getDatabase()("groupsOfObjects").findOne(MongoDBObject("_id" -> new ObjectId(groupId))).get.map(dbl => dbl)

    debug("from,to =" + (from, to))
    debug("timezone =" + timezone)
    debug("selectedUid =" + groupId)
    debug("objectGroups=" + objectsGroup.toString())

    val groupName = objectsGroup("name").asInstanceOf[String]
    val groupedObjects = objectsGroup.get("objects")
      .map(dbl => dbl.asInstanceOf[BasicDBList]
        .map(dbo => dbo.asInstanceOf[BasicDBObject]
          .get("uid").asInstanceOf[String]))
      .get.filter(permissions.hasPermission(_, PermissionValue.VIEW))

    groupedObjects.map(objUid => prepareObjectsStatistic(objUid, from, to, timezone))
  }

  private[this] def prepareObjectsStatistic(objUid: String, from: Date, to: Date, timezone: Int = 0) = {

    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val history = packStore.getHistoryFor(objUid, from, to) //.toStream\
    val settings = new ObjectTripSettings(objUid, mdbm)
    val movingStates = new MovingStatesExtractor().detectStates(history.iterator, settings)

    var pointsCount = 0
    var distance = 0.0
    var maxSpeed = 0.0
    var parkingInterval: Long = 0
    var movingInterval: Long = 0
    var sRegeo = ""
    var fRegeo = ""
    var parkingCount: Long = 0
    var movingCount: Long = 0

    val dateformat = new SimpleDateFormat(tr("format.scala.datetime"))
    dateformat.setTimeZone(new SimpleTimeZone(timezone * 60 * 1000, ""))

    val baseMap = Map(
      "sDate" -> dateformat.format(from),
      "fDate" -> dateformat.format(to),
      "uid" -> objUid,
      "name" -> or.getUserObjectName(objUid)
    )
    movingStates.foreach(p => {
      sRegeo = if (sRegeo == "") Option(p.first.placeName).getOrElse("") else sRegeo
      fRegeo = Option(p.last.placeName).getOrElse("")
      pointsCount += p.pointsCount
      distance += p.distance
      maxSpeed = if (p.maxSpeed > maxSpeed) p.maxSpeed else maxSpeed
      if (p.isParking) {
        parkingCount += 1
        parkingInterval += p.intervalMills
      }
      else {
        movingCount += 1
        movingInterval += p.intervalMills
      }
    })

    val data = Map(
      "pointsCount" -> pointsCount,
      "distance" -> distance,
      "maxSpeed" -> maxSpeed,
      "parkingCount" -> parkingCount,
      "movingCount" -> movingCount,
      "parkingInterval" -> ReportsTransitionalUtils.formatPeriod(parkingInterval),
      "movingInterval" -> ReportsTransitionalUtils.formatPeriod(movingInterval),
      "sRegeo" -> sRegeo,
      "fRegeo" -> fRegeo
    )
    baseMap ++ data
  }


  @ExtDirectMethod
  def getReportPerObject(uid: String, from: Date, to: Date) = {

    val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(from, to)

    val history = packStore.getHistoryFor(uid, correctFrom, correctTo) //.toStream

    //    val writer = new DboWriter("report3.bson")
    //    writer.write(history.map(GPSDataConversions.toPerObjectMongoDb) : _*)
    //    writer.close()
    val settings = new ObjectTripSettings(uid, mdbm)

    val intervals = new MovingStatesExtractor().detectStates(history.iterator,settings)

    intervals.map(
      p => Map(
        "uid" -> uid,
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
    )
  }
}