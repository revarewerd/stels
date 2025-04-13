package ru.sosgps.wayrecall.monitoring.web

import java.text.{DecimalFormat, SimpleDateFormat}
import java.util.{Date, SimpleTimeZone}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.BasicDBList
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.monitoring.processing.fuelings.{Fueling, FuelingReportService, Refueling}
import ru.sosgps.wayrecall.monitoring.processing.parkings._
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.tariff.TariffEDS
import ru.sosgps.wayrecall.core._

import scala.collection.JavaConversions.{mapAsJavaMap, mapAsScalaMap}
import scala.collection.JavaConverters.{asJavaCollectionConverter, mapAsJavaMapConverter}
import ru.sosgps.wayrecall.data.{MovementHistory, PackagesStore}
import ru.sosgps.wayrecall.monitoring.processing.fuelings.{Fueling, FuelingReportService}
import ru.sosgps.wayrecall.monitoring.processing.parkings.{Moving, MovingStatesExtractor, Parking}
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionValue}
import ru.sosgps.wayrecall.utils.{ExtDirectService, tryLong}
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

import scala.Predef._
import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConverters.{asJavaCollectionConverter, mapAsJavaMapConverter}
import scala.collection.immutable.IndexedSeq

@ExtDirectService
class GroupMovementStatsReport extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  import DistanceUtils._

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var translations: Translations = null

  //@Autowired
  //var fuelings: FuelingReportService = _

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var permissions: ObjectsPermissionsChecker = null

  @Autowired
  var tariffEDS: TariffEDS = null

  val model = Model("name", "value")
  val name = "GroupMovementStats"
  val idProperty = "name"
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
      .get.filter(permissions.hasPermission(_, PermissionValue.VIEW))

    prepareReport(groupName, groupedObjects, from, to, timezone)
  }

  private[this] def prepareReport(groupName: String, groupedObjects: Seq[String], from: Date, to: Date, timezone: Int = 0) = {

    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale(tr("language"), tr("country")))
    val df = nf.asInstanceOf[DecimalFormat]
    df.applyPattern("#,##0.##")
    val dateformat = new SimpleDateFormat(tr("format.scala.datetime"))
    dateformat.setTimeZone(new SimpleTimeZone(timezone * 60 * 1000, ""))

    val res1 = Seq(
      Map(
        "name" -> tr("basereport.group"),
        "value" -> groupName
      ),
      Map(
        "name" -> tr("statistics.intervalstart"),
        "value" -> dateformat.format(from)
      ),
      Map(
        "name" -> tr("statistics.intervalfinish"),
        "value" -> dateformat.format(to)
      )
    )

    val objectsData = groupedObjects.map(selectedUid => {
      val history: MovementHistory = packStore.getHistoryFor(selectedUid, from, to)
      val distance = sumdistance(history.iterator)
      val abc = Map(
        "objectfield" -> or.getUserObjectName(selectedUid),
        "messagescount" -> history.total,
        "distance" -> distance,
        "maxspeed" -> (if (history.total > 0) history.iterator.map(_.speed).max else 0)
      )
      debug("abc" + abc)
      abc
    })

    val res2 = Seq(
      Map(
        "name" -> tr("statistics.objects"),
        "value" -> {
          objectsData.flatMap(item => item.get("objectfield"))
        }
      ),
      Map(
        "name" -> tr("statistics.messagescount"),
        "value" -> {
          objectsData.map(item => item.get("messagescount").get.asInstanceOf[Int]).sum
        }
      ),
      Map(
        "name" -> tr("statistics.distance"),
        "value" -> (df.format(objectsData.map(item => item.get("distance").get.asInstanceOf[Double]).sum) + " " + tr("movinggroupgrid.km"))
      ),
      Map(
        "name" -> tr("statistics.maxspeed"),
        "value" -> (df.format(objectsData.map(item => item.get("maxspeed").get.asInstanceOf[Int]).max) + " " + tr("mapobject.laststate.kmsperhour"))
      )
    )
    res1 ++ res2
  }
}

@ExtDirectService
class MovementStatsReport extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  import DistanceUtils._

  val model = Model("name", "value")
  val name = "MovementStats"
  val idProperty = "name"
  val lazyLoad = false

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var fuelings: FuelingReportService = _

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var translations: Translations = null

  @Autowired
  var tariffEDS: TariffEDS = null

  @Autowired
  var mdbm: MongoDBManager = _
  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest): Seq[Map[String, Any]] = {

    val (from, to) = tariffEDS.correctReportWorkingDates(ReportsTransitionalUtils.getWorkingDates(request.getParams))

    val timezone = tryLong(request.getParams.get("timezone")).toInt
    val selectedUid: String = ReportsTransitionalUtils.getSelectedId(request.getParams)
    debug("timezone=" + timezone);

    prepareReport(selectedUid, from, to, timezone)

  }

  def genJRDataSource(selected: String, from: Date, to: Date) = {
    val repData = prepareReport(selected, from, to)
    repData.map(_.asJava).toSeq.asJavaCollection.asInstanceOf[java.util.Collection[java.util.Map[String, _]]]
  }

  def prepareReport(selectedUid: String, from: Date, to: Date, timezone: Int = 0) = {

    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val history: MovementHistory = packStore.getHistoryFor(selectedUid, from, to)

    val movingStates = new MovingStatesExtractor().detectStates(history.iterator, new ObjectTripSettings(selectedUid, mdbm)).toSeq
    val parks: IndexedSeq[Parking] = (movingStates.collect { case e: Parking => e }).toIndexedSeq
    val movings = movingStates.collect { case e: Moving => e }


    val cleanTime = parks.iterator.sliding(2).withPartial(false).map(s => s(1).firstTime.getTime - s(0).lastTime.getTime).sum + 1
    val distanceInTrips = GPSUtils.splitByIntervals(history,movings).map(sumdistance).sum
    val distance = sumdistance(history.iterator)

    //TODO: каждый раз вызывается итератор с сохранением ссылки на history, это не позволяет экономить память за счет
    //ленивых вычислений

    val nf = java.text.NumberFormat.getNumberInstance(new java.util.Locale(tr("language"), tr("country")))
    val df = nf.asInstanceOf[DecimalFormat]
    df.applyPattern("#,##0.##")
    val dateformat = new SimpleDateFormat(tr("format.scala.datetime"))
    dateformat.setTimeZone(new SimpleTimeZone(timezone * 60 * 1000, ""))

    val canParamsSet = GPSUtils.canParamsSet
    val canHistory = history.filter(p => {
      val t = canParamsSet.intersect(p.data.keys.toSet)
      t.nonEmpty && t.filter(p.data.getOrElse(_, 0) != 0).nonEmpty
    })
    val canData = if (canHistory.nonEmpty) {
      //(!history.isEmpty && ((history.head.data.contains("can_distance") || history.head.data.contains("CANBUS_Distance")) && (history.last.data.contains("can_distance") || history.last.data.contains("CANBUS_Distance")))) {


      val first = canHistory.head
      val last = canHistory.last
      val fDist: Long = GPSUtils.detectCanDistance(first).getOrElse(0L) // if (fData.contains("can_distance")) fData.get("can_distance") else if (fData.contains("CANBUS_Distance")) fData.get("CANBUS_Distance") else 0
      val lDist = GPSUtils.detectCanDistance(last).getOrElse(0L) // if (lData.contains("can_distance")) lData.get("can_distance") else if (lData.contains("CANBUS_Distance")) lData.get("CANBUS_Distance") else 0
      val fFuel = GPSUtils.detectCanFuelUsed(first).getOrElse(0L) // if (fData.contains("can_fuel_used")) fData.get("can_fuel_used") else if (fData.contains("CANBUS_Fuel_used")) fData.get("CANBUS_Fuel_used") else 0
      val lFuel = GPSUtils.detectCanFuelUsed(last).getOrElse(0L) //if (lData.contains("can_fuel_used")) lData.get("can_fuel_used") else if (lData.contains("CANBUS_Fuel_used")) lData.get("CANBUS_Fuel_used") else 0

      val dist: Double = (lDist - fDist) * 5 / 1000
      val fuel: Double = lFuel / 2 - fFuel / 2

      Seq(
        Map(
          "name" -> tr("statistics.candistance"),
          "value" -> (df.format(dist) + " " + tr("movinggroupgrid.km"))
        ),
        Map(
          "name" -> tr("statistics.canfuelspent"),
          "value" -> (df.format(fuel) + " " + tr("fuelgraphreport.units"))
        ),
        Map(
          "name" -> tr("statistics.canfuelconsumption"),
          "value" -> (df.format((fuel * 100.0 / dist).toDouble) + " " + tr("fuelgraphreport.units") + " / 100 " + tr("movinggroupgrid.km"))
        )
      )
    } else {
      Seq.empty
    }

    val fuelSensors = fuelings.fuelSensorsNames(selectedUid)
    val fuelData = if (history.nonEmpty && fuelSensors.nonEmpty) {
      val fuelStates = fuelings.getFuelings(selectedUid, from, to)
        val refuelings = fuelStates.newStates.collect {case f: Refueling => f}
      val fVal = if (fuelStates.fuelDataRecs.nonEmpty) fuelStates.fuelDataRecs.head.value else 0
      val lVal = if (fuelStates.fuelDataRecs.nonEmpty) fuelStates.fuelDataRecs.last.value else 0

        val total = refuelings.map(_.volume).sum
      val drained = fuelStates.states.filterNot(_.isFueling).map(_.volumeCheck).sum
        val min = if (refuelings.nonEmpty) Seq(refuelings.map(_.firstPoint.value).min, fVal, lVal).min else Seq(fVal, lVal).min
        val max = if (refuelings.nonEmpty) Seq(refuelings.map(f => f.lastPoint.value).max, fVal, lVal).max else Seq(fVal, lVal).max

        val consumedByNorms = fuelStates.consumptionByNorms(fuelStates.fuelDataRecs)
        val consumedByEstimations = fuelStates.consumptionByEstimation(fuelStates.fuelDataRecs)

      Seq(
        Map(
          "name" -> tr("statistics.flsfuelingscount"),
          "value" -> refuelings.length
        ),
        Map(
          "name" -> tr("statistics.flstotalfueled"),
          "value" -> (df.format(total) + " " + tr("fuelgraphreport.units"))
        ),
        Map(
          "name" -> tr("statistics.flsstartfuellevel"),
          "value" -> (df.format(fVal) + " " + tr("fuelgraphreport.units"))
        ),
        Map(
          "name" -> tr("statistics.flsfinalfuellevel"),
          "value" -> (df.format(lVal) + " " + tr("fuelgraphreport.units"))
        ),
        Map(
          "name" -> tr("statistics.flsminfuellevel"),
          "value" -> (df.format(min) + " " + tr("fuelgraphreport.units"))
        ),
        Map(
          "name" -> tr("statistics.flsmaxfuellevel"),
          "value" -> (df.format(max) + " " + tr("fuelgraphreport.units"))
        ),
        Map(
          "name" -> tr("statistics.flsfuelspent"),
          "value" -> (df.format(fVal + total - lVal) + " " + tr("fuelgraphreport.units"))
        ),
        Map(
          "name" -> tr("statistics.flsfuelconsumption"),
          "value" -> (df.format(((fVal + total - drained - lVal) * 100.0 / distance).toDouble) + " " + tr("fuelgraphreport.units") + " / 100 " + tr("movinggroupgrid.km"))
          ),
          Map(
            "name" -> tr("statistics.consumedbynorms"),
             "value" -> (df.format(consumedByNorms.total) + " " + tr("fuelgraphreport.units"))
          ),
          Map(
            "name" -> tr("statistics.consumedbynormsavgdist"),
            "value" -> (df.format(consumedByNorms.avgByDistance) + " "+tr("fuelgraphreport.units")+" / 100 "+tr("movinggroupgrid.km"))
          ),
          Map(
            "name" -> tr("statistics.consumedbyestimations"),
            "value" -> (df.format(consumedByEstimations.total) + " " + tr("fuelgraphreport.units"))
          ),
          Map(
            "name" -> tr("statistics.consumedbyestimationsavgdist"),
            "value" -> (df.format(consumedByEstimations.avgByDistance) + " "+tr("fuelgraphreport.units")+" / 100 "+tr("movinggroupgrid.km"))
        )
      )
    } else {
      Seq.empty
    }


    val motorhours = history.lastOption.flatMap(lo => Option(lo.privateData.get("mh"))).map(tryLong)
      .map(_ - history.headOption.flatMap(ho => Option(ho.privateData.get("mh"))).map(tryLong).getOrElse(0L)).map(mh => Map(
      "name" -> tr("statistics.motorhours"),
      "value" -> (df.format(mh / (1000.0 * 60 * 60)) + " " + tr("units.hour")))
    ).toSeq

    Seq(
      Map(
        "name" -> tr("basereport.objectfield"),
        "value" -> or.getUserObjectName(selectedUid)
      ),
      Map(
        "name" -> tr("statistics.intervalstart"),
        "value" -> dateformat.format(from)
      ),
      Map(
        "name" -> tr("statistics.intervalfinish"),
        "value" -> dateformat.format(to)
      ),
      Map(
        "name" -> tr("statistics.messagescount"),
        "value" -> history.total
      ),
      Map(
        "name" -> tr("statistics.distance"),
        "value" -> (df.format(distance) + " " + tr("movinggroupgrid.km"))
      ),
      Map(
        "name" -> tr("statistics.distanceInTrips"),
        "value" -> (df.format(distanceInTrips) + " " + tr("movinggroupgrid.km"))
      ),
      Map(
        "name" -> tr("statistics.maxspeed"),
        "value" -> (df.format(if (history.total > 0) history.iterator.map(_.speed).max else 0) + " " + tr("mapobject.laststate.kmsperhour"))
      ),
      Map(
        "name" -> tr("statistics.avrspeed"),
        "value" -> (df.format(if (movings.nonEmpty) (movings.map(_.distance).sum / movings.map(_.intervalMills).sum * 1000 * 60 * 60) else 0) + " " + tr("mapobject.laststate.kmsperhour"))
      ),
      Map(
        "name" -> tr("statistics.parkscount"),
        "value" -> parks.filter(!_.small).length
      ),
      Map(
        "name" -> tr("statistics.stopscount"),
        "value" -> parks.filter(_.small).length
      ),
      Map(
        "name" -> tr("statistics.stoplength"),
        "value" -> ReportsTransitionalUtils.formatPeriod(parks.map(_.intervalMills).sum)
      ),
      Map(
        "name" -> tr("statistics.lastmessagetime"),
        "value" -> {
          if (!history.isEmpty) dateformat.format(history.last.time)
          else ""
        }
      ),
      Map(
        "name" -> tr("statistics.lastpostiton"),
        "value" -> {
          if (!history.isEmpty) history.last.placeName
          else ""
        }
      )
    ) ++ canData ++ fuelData ++ motorhours
  }

}



