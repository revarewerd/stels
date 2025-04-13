package ru.sosgps.wayrecall.billing

import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData}
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.monitoring.web.ReportsTransitionalUtils
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

import collection.JavaConversions.mapAsScalaMap


/**
  * Created by ivan on 02.07.17.
  */

@ExtDirectService
class TerminalMessagesGaps extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  override val model: Model = Model("period",
    "firstCoordinates", "secondCoordinates",
    "firstTime", "secondTime",
    "distance",
    "firstInsertionTime", "secondInsertionTime",
    "firstSpeed", "secondSpeed",
    "firstCourse", "secondCourse",
    "firstRegeo", "secondRegeo",
    "firstData", "secondData",
    "_firstRow")

  override val lazyLoad: Boolean = false
  override val name: String = "TerminalMessagesGaps"
  override val idProperty: String = "_firstRow"

  @Autowired
  var packagesStore: PackagesStore = _

  case class Gap(period: String,
                 firstCoordinates: String, secondCoordinates: String,
                 firstTime: String, secondTime: String,
                 distance: Double,
                 firstInsertionTime: String, secondInsertionTime: String,
                 firstRegeo: String, secondRegeo: String,
                 firstData: String, secondData: String,
                 _firstRow: Int
                 )

  def isGap(first: GPSData, second: GPSData, gapMinutes: Int) = {
    second.time.getTime - first.time.getTime > gapMinutes * 60 * 1000
  }

  def coordsToString(gps: GPSData) = {
    gps.lat + " " + gps.lon + " (" + gps.satelliteNum + ")"
  }

  def regeo(gps: GPSData) = Option(gps.placeName).getOrElse("")

  def data(gps: GPSData) = Option(gps.data).map(_.map(kv => kv._1 + " = " + kv._2).toSeq.sorted.mkString(", ")).getOrElse("")
  def toGap( first: GPSData, second: GPSData, firstRow: Int) = {
    Gap(ReportsTransitionalUtils.formatPeriod(second.time.getTime - first.time.getTime),
      coordsToString(first), coordsToString(second),
      utils.ISODateTime(first.time) , utils.ISODateTime(second.time),
      DistanceUtils.kmsBetween(first,second),
      utils.ISODateTime(first.insertTime), utils.ISODateTime(second.insertTime),
      first.placeName,
      second.placeName,
      data(first),
      data(second),
      firstRow)
  }

  def toGap(first: GPSData, limit: Date, firstRow: Int) = {
    Gap( "> " + ReportsTransitionalUtils.formatPeriod(limit.getTime - first.time.getTime),
      coordsToString(first), coordsToString(first), utils.ISODateTime(first.time) , utils.ISODateTime(limit),
      0.0,
      utils.ISODateTime(first.insertTime) , utils.ISODateTime(limit),
      first.placeName, "-", data(first), "-", firstRow
    )
  }


  def findGaps(uid: String, from: Date, to: Date, gapMinutes:Int) = {
    val history = packagesStore.getHistoryFor(uid, from, to).toIterator.zipWithIndex

    def gaps(iter: Iterator[(GPSData, Int)], prev: (GPSData, Int)): Stream[Gap] = {
      val (g0, firstRow) = prev
      if(iter.hasNext) {
        val h = iter.next()
        val g1 = h._1
        if(isGap(g0,g1, gapMinutes)) {
          toGap(g0,g1, firstRow) #:: gaps(iter,h)
        }
        else
          gaps(iter,h)
      }
      else {
        val now = new Date
        val limit = if(now.before(to)) now else to
        if(limit.getTime - g0.time.getTime > gapMinutes * 60 * 1000)
          toGap(g0, limit, firstRow) #:: Stream.empty
        else
          Stream.empty
      }
    }

    if(history.hasNext) {
      val first = history.next()
      gaps(history,first)
    }
    else
      Iterable.empty
  }


  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {
    val params = request.getParams
    val uid = params.get("uid").asInstanceOf[String]
    val isReport = Option(params.get("report")).contains(java.lang.Boolean.TRUE)
    val(dateFrom,dateTo) = if(isReport) {
      ReportsTransitionalUtils.getWorkingDates(params)
    }
    else {
      (Option(params.get("dateFrom")).map(x => utils.parseDate(x.asInstanceOf[String])).getOrElse(new Date(111, 0, 1)),
        Option(params.get("dateTo")).map(x => utils.parseDate(x.asInstanceOf[String])).getOrElse(new Date()))
    }

    val gap = Option(params.get("gap")).map(_.asInstanceOf[Int]).getOrElse(20)

    findGaps(uid, dateFrom, dateTo, gap)
  }
}
