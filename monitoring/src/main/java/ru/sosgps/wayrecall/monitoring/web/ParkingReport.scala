package ru.sosgps.wayrecall.monitoring.web

import java.text.SimpleDateFormat
import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.tariff.TariffEDS
import ru.sosgps.wayrecall.core.{MongoDBManager, Translations}
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.monitoring.processing.parkings.{MovingStatesExtractor, ObjectTripSettings, Parking}
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConverters.{asJavaCollectionConverter, mapAsJavaMapConverter}
import scala.collection.immutable.ListMap

//import ru.sosgps.wayrecall.data.sleepers.Moving

@ExtDirectService
class ParkingReport extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("num", "datetime", "interval", "intervalRaw", "pointsCount", "lon", "lat", "regeo", "isSmall")
  val name = "ParkingReport"
  val idProperty = "datetime"
  val lazyLoad = false

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var mdbm: MongoDBManager = _

  @Autowired
  var translations: Translations = null

  @Autowired
  var tariffEDS: TariffEDS = null

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest): Iterator[Map[String, Any]] = {

    val (from, to) = tariffEDS.correctReportWorkingDates(ReportsTransitionalUtils.getWorkingDates(request.getParams))
    val selectedUid: String = ReportsTransitionalUtils.getSelectedId(request.getParams)

    debug("building ParkingReport for " + selectedUid + " " + (from, to))

    val parkings = getParkings(selectedUid, from, to)

    parkings.zipWithIndex.map({
      case (p, i) => ListMap(
        "num" -> (i + 1),
        "datetime" -> ReportsTransitionalUtils.ISODateTime(p.first.time),
        "interval" -> ReportsTransitionalUtils.formatPeriod(p.intervalMills),
        "intervalRaw" -> p.intervalMills,
        "pointsCount" -> p.pointsCount,
        "lon" -> p.first.lon.toString,
        "lat" -> p.first.lat.toString,
        "regeo" -> Option(p.first.placeName).getOrElse(""),
        "isSmall" -> p.small
      )
    }).toIterator
  }


  def getParkings(selectedUid: String, from: Date, to: Date) = {
    val history = packStore.getHistoryFor(selectedUid, from, to)
    new MovingStatesExtractor().detectStates(history.iterator, new ObjectTripSettings(selectedUid, mdbm))
      .collect { case e: Parking => e }
  }

  def genJRDataSource(selected: String, from: Date, to: Date) = {
    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val dateFormat = new SimpleDateFormat(tr("format.scala.datetime"))
    val parkings = getParkings(selected, from, to)
    parkings.zipWithIndex.map({
      case (p, i) => Map(
        "num" -> (i + 1),
        "type" -> (if (!p.small) tr("movinggroupgrid.parking") else tr("movinggroupgrid.smallparking")),
        "datetime" -> dateFormat.format(p.first.time),
        "interval" -> ReportsTransitionalUtils.formatPeriod(p.intervalMills),
        "regeo" -> Option(p.first.placeName).getOrElse("")
      ).asJava
    }).toSeq.asJavaCollection.asInstanceOf[java.util.Collection[java.util.Map[String, _]]]

  }


}
