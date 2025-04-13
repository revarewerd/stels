package ru.sosgps.wayrecall.monitoring.web

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import ru.sosgps.wayrecall.core.{FuelConversions, GPSData, MongoDBManager, ObjectDataConversions, ObjectFuelSettings, ObjectsRepositoryReader, Translations}
import java.text.SimpleDateFormat
import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.tariff.{TariffEDS, TariffService}
import ru.sosgps.wayrecall.core.{MongoDBManager, ObjectsRepositoryReader, Translations}
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ru.sosgps.wayrecall.monitoring.processing.fuelings.{FuelingReportService, Refueling}

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConverters.{asJavaCollectionConverter, mapAsJavaMapConverter}

@ExtDirectService
class FuelingReport extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("num", "isFueling", "datetime", "interval", "intervalRaw", "pointsCount", "startVal", "endVal", "volumeRaw", "volume", "volumeCheck", "regeo", "lon", "lat")
  val name = "FuelingReport"
  val idProperty = "datetime"
  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var translations: Translations = null

  @Autowired
  var frs: FuelingReportService = null

  @Autowired
  var tariffEDS: TariffService = null

  val minTime = 1.0 * 1000 * 60

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest): Iterator[Map[String, Any]] = {

    val (from, to) = tariffEDS.correctReportWorkingDates(ReportsTransitionalUtils.getWorkingDates(request.getParams))
    val selectedUid: String = ReportsTransitionalUtils.getSelectedId(request.getParams)

    debug("building FuelingReport for " + selectedUid + " " + (from, to))

    val fuelings = frs.getFuelings(selectedUid, from, to).newStates

    fuelings.zipWithIndex.map({
      case (p, i) => Map(
        "num" -> (i + 1),
        "isFueling" -> p.isInstanceOf[Refueling],
        "datetime" -> ReportsTransitionalUtils.ISODateTime(p.firstPoint.point.time),
        "interval" -> ReportsTransitionalUtils.formatPeriod(p.lastPoint.point.time.getTime - p.firstPoint.point.time.getTime),
        "intervalRaw" -> (p.lastPoint.point.time.getTime - p.firstPoint.point.time.getTime),
        "pointsCount" -> 0,
        "startVal" -> p.firstPoint.value,
        "endVal" -> p.lastPoint.value,
        "volume" -> p.volume,
        "volumeRaw" -> p.volume,
        "volumeCheck" -> (p.lastPoint.value - p.firstPoint.value),
        "regeo" -> Option(p.midPoint.placeName).getOrElse(""),
        "lon" -> p.midPoint.lon,
        "lat" -> p.midPoint.lat
      )
    }).toIterator
  }


  def genJRDataSource(selected: String, from: Date, to: Date) = {
    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val dateFormat = new SimpleDateFormat(tr("format.scala.datetime"))
    val fuelings = frs.getFuelings(selected, from, to).newStates

    fuelings.zipWithIndex.map({
      case (p, i) => Map(
        "num" -> (i + 1),
        "type" ->  (if(p.isInstanceOf[Refueling])  tr("fuelinggrid.fueling") else tr("fuelinggrid.draining")) ,
        "datetime" -> ReportsTransitionalUtils.ISODateTime(p.firstPoint.point.time),
        "interval" -> ReportsTransitionalUtils.formatPeriod(p.lastPoint.point.time.getTime - p.firstPoint.point.time.getTime),
        "pointsCount" -> 0,
        "startVal" -> p.firstPoint.value,
        "endVal" -> p.lastPoint.value,
        "volume" -> p.volume,
        "regeo" -> Option(p.midPoint.placeName).getOrElse("")
      ).asJava
    }).toSeq.asJavaCollection.asInstanceOf[java.util.Collection[java.util.Map[String, _]]]
  }

}

