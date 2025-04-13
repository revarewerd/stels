package ru.sosgps.wayrecall.monitoring.web

import java.text.{DecimalFormat, SimpleDateFormat}
import java.util.{Date, SimpleTimeZone}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.BasicDBList
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.joda.time.{DateTime, DateTimeZone}
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.tariff.TariffEDS
import ru.sosgps.wayrecall.core.DistanceUtils._
import ru.sosgps.wayrecall.core.{MongoDBManager, ObjectsRepositoryReader, Translations}
import ru.sosgps.wayrecall.data.{MovementHistory, PackagesStore}
import ru.sosgps.wayrecall.monitoring.processing.parkings.{Moving, MovingStatesExtractor, ObjectTripSettings, Parking}
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionValue}
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.immutable.IndexedSeq

/**
  * Created by IVAN on 09.09.2015.
  */
@ExtDirectService
class GroupPathReport extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var translations: Translations = null

  //@Autowired
  //var fuelings: FuelingReportService = _

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var permissions: ObjectsPermissionsChecker = null

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var tariffEDS: TariffEDS = null

  val model = Model("uid", "name", "intervalstart", "intervalfinish", "messagescount", "distance", "maxspeed",
    "avrspeed", "lastmessagetime", "lastpostiton")
  val name = "GroupPathReport"
  val idProperty = "_id"
  val lazyLoad = false

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
      .get.filter(permissions.hasPermission(_, PermissionValue.VIEW)) // Если пользователь потерял права на объект в группе

    groupedObjects.map(objUid => prepareObjectsStatistic(objUid, from, to, timezone))
  }

  private[this] def prepareObjectsStatistic(objUid: String, from: Date, to: Date, timezone: Int = 0) = {

    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val history: MovementHistory = packStore.getHistoryFor(objUid, from, to)

    val movingStates = new MovingStatesExtractor().detectStates(history.iterator, new ObjectTripSettings(objUid, mdbm)).toSeq
    val parks: IndexedSeq[Parking] = (movingStates.collect { case e: Parking => e }).toIndexedSeq
    val movings = movingStates.collect { case e: Moving => e }

    val cleanTime = parks.iterator.sliding(2).withPartial(false).map(s => s(1).firstTime.getTime - s(0).lastTime.getTime).sum + 1
    val distance = sumdistance(history.iterator)

    val nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale(tr("language"), tr("country")))
    val df = nf.asInstanceOf[DecimalFormat]
    df.applyPattern("#,##0.##")
    val dateformat = new SimpleDateFormat(tr("format.scala.datetime"))
    dateformat.setTimeZone(new SimpleTimeZone(timezone * 60 * 1000, ""))
    Map(
      "uid" -> objUid,
      "name" -> or.getUserObjectName(objUid),
      "intervalstart" -> dateformat.format(from),
      "intervalfinish" -> dateformat.format(to),
      "messagescount" -> history.total,
      "distance" -> (df.format(distance) + " " + tr("movinggroupgrid.km")),
      "maxspeed" -> (df.format(if (history.total > 0) history.iterator.map(_.speed).max else 0) + " " + tr("mapobject.laststate.kmsperhour")),
      "avrspeed" -> (df.format(if (movings.nonEmpty) (movings.map(_.distance).sum / movings.map(_.intervalMills).sum * 1000 * 60 * 60) else 0) + " " + tr("mapobject.laststate.kmsperhour")),
      "lastmessagetime" -> {
        if (!history.isEmpty) dateformat.format(history.last.time)
        else ""
      },
      "lastpostiton" -> {
        if (!history.isEmpty) history.last.placeName
        else ""
      }
    )
    //    Map(
    //      "name" -> tr("statistics.parkscount"),
    //      "value" -> parks.filter(_.intervalMills >= 5 * 60 * 1000).length
    //    ),
    //    Map(
    //      "name" -> tr("statistics.stopscount"),
    //      "value" -> parks.filter(_.intervalMills < 5 * 60 * 1000).length
    //    ),
    //    Map(
    //      "name" -> tr("statistics.stoplength"),
    //      "value" -> ReportsTransitionalUtils.formatPeriod(parks.map(_.intervalMills).sum)
    //    ),
  }

  @ExtDirectMethod
  def getReportPerObject(uid: String, from: Date, to: Date) = {

    val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(from, to)

    val history = packStore.getHistoryFor(uid, correctFrom, correctTo) //.toStream
    val objects = mdbm.getDatabase()("objects")

    history.zipWithIndex.map({
      case (g, i) => {
        Map(
          "num" -> (i + 1),
          "name" -> objects.findOne(MongoDBObject("uid" -> g.uid), MongoDBObject("name" -> 1)).map(_.get("name").asInstanceOf[String]).getOrElse(g.uid),
          ("uid" -> g.uid),
          ("lat" -> g.lat),
          ("lon" -> g.lon),
          ("satelliteNum" -> g.satelliteNum),
          ("speed" -> g.speed.toString),
          ("course" -> g.course.toString),
          //                  ("regeo" -> positions(i)),
          ("regeo" -> Option(g.placeName).getOrElse("")),
          ("time" -> ReportsTransitionalUtils.ISODateTime(g.time)),
          ("insertTime" -> Option(g.insertTime).map(ReportsTransitionalUtils.ISODateTime).getOrElse("")),
          "devdata" -> Option(g.data).map(_.map(kv => kv._1 + " = " + kv._2).toSeq.sorted.mkString(", ")).getOrElse("")
        )
      }
    }
    ).toSeq
  }

  @ExtDirectMethod
  def getObjectDayStatReport(uid: String, from: Date, to: Date, tz: Long) = {
    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(from, to)

    //val (from, to) = ReportsTransitionalUtils.getWorkingDates(request.getParams)
    //val selectedUid: String = request.getParams.get("uid").asInstanceOf[String]//ReportsTransitionalUtils.getSelectedId(request.getParams)

    debug("building ObjectDayStatReport for " + uid + " " + (correctFrom, correctTo))
    val timezone = tryLong(tz).toInt
    val dateTimeZone = DateTimeZone.forOffsetMillis(timezone * 60 * 1000);
    val jFrom = new DateTime(correctFrom, dateTimeZone)
    val jTo = new DateTime(to, dateTimeZone)

    var stepTime = jFrom.withTimeAtStartOfDay.plusDays(1)
    val dateRange = Iterator.iterate(stepTime)(_.plusDays(1)).takeWhile(!_.isAfter(jTo)) ++ Iterator(jTo)

    val nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale(tr("language"), tr("country")))
    val df = nf.asInstanceOf[DecimalFormat]
    df.applyPattern("#,##0.##")
    //val dateformat = new SimpleDateFormat(tr("format.scala.date"))
    //dateformat.setTimeZone(new SimpleTimeZone(timezone * 60 * 1000, ""))

    //val starttimeformat = new SimpleDateFormat(tr("format.scala.datetime"))
    val dateformat = new SimpleDateFormat(tr("format.scala.datetime"))
    dateformat.setTimeZone(new SimpleTimeZone(timezone * 60 * 1000, ""))

    stepTime = jFrom
    val res = (for (d <- dateRange) yield {
      val repData = packStore.getHistoryFor(uid, stepTime.toDate, d.toDate)
      val distance = sumdistance(repData.iterator)
      val movingStates = new MovingStatesExtractor().detectStates(repData.iterator, new ObjectTripSettings(uid, mdbm)).toSeq
      //val parks: IndexedSeq[Parking] = (movingStates.collect { case e: Parking => e }).toIndexedSeq
      val movings = movingStates.collect { case e: Moving => e }
      val map = Map(
        "uid" -> uid,
        "intervalstart" -> dateformat.format(stepTime.toDate),
        "intervalfinish" -> dateformat.format(d.toDate),
        //"sDate" -> ReportsTransitionalUtils.ISODateTime(stepTime.toDate),
        //"fDate" -> ReportsTransitionalUtils.ISODateTime(d.toDate),
        "messagescount" -> repData.total,
        "distance" -> (df.format(distance) + " " + tr("movinggroupgrid.km")),
        "maxspeed" -> (df.format(if (repData.total > 0) repData.iterator.map(_.speed).max else 0) + " " + tr("mapobject.laststate.kmsperhour")),
        "avrspeed" -> (df.format(if (movings.nonEmpty) (movings.map(_.distance).sum / movings.map(_.intervalMills).sum * 1000 * 60 * 60) else 0) + " " + tr("mapobject.laststate.kmsperhour")),
        "lastmessagetime" -> {
          if (!repData.isEmpty) dateformat.format(repData.last.time)
          else ""
        },
        "lastpostiton" -> {
          if (!repData.isEmpty) repData.last.placeName
          else ""
        }
      )
      stepTime = d
      map
    }).filter(p => p("messagescount") != 0)
    res
  }
}

@ExtDirectService
class ObjectDayStatService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var translations: Translations = null

  @Autowired
  var tariffEDS: TariffEDS = null

  val model = Model("intervalstart", "intervalfinish", /*"name",*/ "uid", "messagescount", "distance", "maxspeed",
    "avrspeed", "lastmessagetime", "lastpostiton")
  //val model = Model("uid","name","intervalstart",  "intervalfinish",  ")
  //val model = Model( "parkingInterval", "movingInterval", "distance", "pointsCount", "maxSpeed", "sRegeo", "fRegeo")

  val name = "ObjectDayStatService"
  val idProperty = "_id"
  val lazyLoad = false

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest): Iterator[Map[String, Any]] = {

    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val (from, to) = tariffEDS.correctReportWorkingDates(ReportsTransitionalUtils.getWorkingDates(request.getParams))
    val selectedUid: String = request.getParams.get("uid").asInstanceOf[String] //ReportsTransitionalUtils.getSelectedId(request.getParams)

    debug("building MovingGroupReport for " + selectedUid + " " + (from, to))
    val jFrom = new DateTime(from)
    val jTo = new DateTime(to)

    var stepTime = jFrom.withTimeAtStartOfDay.plusDays(1)
    val dateRange = Iterator.iterate(stepTime)(_.plusDays(1)).takeWhile(!_.isAfter(jTo)) ++ Iterator(jTo)
    val timezone = tryLong(request.getParams.get("timezone")).toInt
    val nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale(tr("language"), tr("country")))
    val df = nf.asInstanceOf[DecimalFormat]
    df.applyPattern("#,##0.##")
    //val dateformat = new SimpleDateFormat(tr("format.scala.date"))
    //dateformat.setTimeZone(new SimpleTimeZone(timezone * 60 * 1000, ""))
    val datetimeformat = new SimpleDateFormat(tr("format.scala.datetime"))
    datetimeformat.setTimeZone(new SimpleTimeZone(timezone * 60 * 1000, ""))

    stepTime = jFrom
    val res = (for (d <- dateRange) yield {
      val repData = packStore.getHistoryFor(selectedUid, stepTime.toDate, d.toDate)
      val distance = sumdistance(repData.iterator)
      val movingStates = new MovingStatesExtractor().detectStates(repData.iterator, new ObjectTripSettings(selectedUid, mdbm)).toSeq
      //val parks: IndexedSeq[Parking] = (movingStates.collect { case e: Parking => e }).toIndexedSeq
      val movings = movingStates.collect { case e: Moving => e }
      val map = Map(
        "uid" -> selectedUid,
        "intervalstart" -> datetimeformat.format(stepTime.toDate),
        "intervalfinish" -> datetimeformat.format(d.toDate),
        //"sDate" -> ReportsTransitionalUtils.ISODateTime(stepTime.toDate),
        //"fDate" -> ReportsTransitionalUtils.ISODateTime(d.toDate),
        "messagescount" -> repData.total,
        "distance" -> (df.format(distance) + " " + tr("movinggroupgrid.km")),
        "maxspeed" -> (df.format(if (repData.total > 0) repData.iterator.map(_.speed).max else 0) + " " + tr("mapobject.laststate.kmsperhour")),
        "avrspeed" -> (df.format(if (movings.nonEmpty) (movings.map(_.distance).sum / movings.map(_.intervalMills).sum * 1000 * 60 * 60) else 0) + " " + tr("mapobject.laststate.kmsperhour")),
        "lastmessagetime" -> {
          if (!repData.isEmpty) datetimeformat.format(repData.last.time)
          else ""
        },
        "lastpostiton" -> {
          if (!repData.isEmpty) repData.last.placeName
          else ""
        }
      )
      stepTime = d
      map
    }).filter(p => p("messagescount") != 0)
    res
  }
}

@ExtDirectService
class ObjectDayPathService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var tariffEDS: TariffEDS = null

  val model = Model("num", "name", "uid", "lat", "lon", "satelliteNum", "speed",
    "course", "regeo", "time", "insertTime", "devdata")

  val name = "ObjectDayPathService"
  val idProperty = "_id"
  val lazyLoad = false

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest) = {
    val (uid, from, to) = (request.getParams.get("uid").asInstanceOf[String], request.getParams.get("from").asInstanceOf[Long], request.getParams.get("to").asInstanceOf[Long])

    val (fromDate, toDate) = tariffEDS.correctReportWorkingDates(new Date(from), new Date(to))

    //    val ls = new LimitsSortsFilters(
    //      Option(request.getStart()).getOrElse(Integer.valueOf(0)).intValue(),
    //      Option(request.getLimit()).getOrElse(Integer.valueOf(200)).intValue()
    //    )
    val history = packStore.getHistoryFor(uid, fromDate, toDate /*,ls*/) //.toStream
    val objects = mdbm.getDatabase()("objects")

    //EDSJsonResponse(history.total,
    history.zipWithIndex.map({
      case (g, i) => {
        Map(
          "num" -> (i + 1),
          "name" -> objects.findOne(MongoDBObject("uid" -> g.uid), MongoDBObject("name" -> 1)).map(_.get("name").asInstanceOf[String]).getOrElse(g.uid),
          ("uid" -> g.uid),
          ("lat" -> g.lat),
          ("lon" -> g.lon),
          ("satelliteNum" -> g.satelliteNum),
          ("speed" -> g.speed.toString),
          ("course" -> g.course.toString),
          //                  ("regeo" -> positions(i)),
          ("regeo" -> Option(g.placeName).getOrElse("")),
          ("time" -> ReportsTransitionalUtils.ISODateTime(g.time)),
          ("insertTime" -> Option(g.insertTime).map(ReportsTransitionalUtils.ISODateTime).getOrElse("")),
          "devdata" -> Option(g.data).map(_.map(kv => kv._1 + " = " + kv._2).toSeq.sorted.mkString(", ")).getOrElse("")
        )
      }
    })
    //)
  }
}
