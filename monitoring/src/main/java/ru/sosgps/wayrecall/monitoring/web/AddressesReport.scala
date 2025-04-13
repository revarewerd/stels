package ru.sosgps.wayrecall.monitoring.web

import java.text.SimpleDateFormat
import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.tariff.TariffEDS
import ru.sosgps.wayrecall.data.{MovementHistory, PackagesStore}
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

import scala.Predef._
import scala.collection.JavaConversions.mapAsScalaMap
import scala.util.Random

@ExtDirectService
class AddressesReport extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("num", "date", "time", "address", "lon", "lat")
  val name = "AddressesReport"
  val idProperty = "num"
  val lazyLoad = false

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var tariffEDS: TariffEDS = null

  def fakeLoadByUid(uid: String, from: Date, to: Date): Seq[Map[String, Any]] = {
    val history: MovementHistory = packStore.getHistoryFor(uid, from, to)

    val format1 = new SimpleDateFormat("dd.MM.yyyy")
    val format2 = new SimpleDateFormat("HH:mm:ss")
    var curdate: String = null
    var curplace: String = null
    var counter = 0

    val locations = Array("улица Сталеваров, Ивановское, Москва", "16, Свободный проспект, Новогиреево, Москва", "8/1, улица Борисовские Пруды, Зябликово", "33А, Свободный проспект, Новогиреево, Москва")
    history.map(gps => {

      val placeName = locations(Random.nextInt(locations.length))

      val day = format1.format(gps.time)
      if (curdate != day)
        curdate = day
      if (curplace != placeName) {
        curplace = placeName
        counter += 1
        Map(
          "num" -> counter,
          "date" -> curdate,
          "time" -> format2.format(gps.time),
          "address" -> placeName,
          "lon" -> gps.lon,
          "lat" -> gps.lat
        )
      } else Map.empty[String, Any]
    }).toSeq.filter(_.nonEmpty)
  }

  def loadByUid(uid: String, from: Date, to: Date): Seq[Map[String, Any]] = {
    val history: MovementHistory = packStore.getHistoryFor(uid, from, to)

    val format1 = new SimpleDateFormat("dd.MM.yyyy")
    val format2 = new SimpleDateFormat("HH:mm:ss")
    var curdate: String = null
    var curplace: String = null
    var counter = 0

    history.filter(gps => {
      gps.goodlonlat && gps.placeName != null
    }).map(gps => {
      val day = format1.format(gps.time)
      if (curdate != day)
        curdate = day
      if (curplace != gps.placeName) {
        curplace = gps.placeName
        counter += 1
        Map(
          "num" -> counter,
          "date" -> curdate,
          "time" -> format2.format(gps.time),
          "address" -> gps.placeName,
          "lon" -> gps.lon,
          "lat" -> gps.lat
        )
      } else Map.empty[String, Any]
    }).toSeq.filter(_.nonEmpty)
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest): Seq[Map[String, Any]] = {
    val (from, to) = tariffEDS.correctReportWorkingDates(ReportsTransitionalUtils.getWorkingDates(request.getParams))
    val selectedUid: String = ReportsTransitionalUtils.getSelectedId(request.getParams)

    loadByUid(selectedUid, from, to)
  }

}
