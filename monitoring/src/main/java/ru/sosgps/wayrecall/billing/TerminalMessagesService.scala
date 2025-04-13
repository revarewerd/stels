package ru.sosgps.wayrecall.billing

import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.processing.AccumulatedParamsReaggregator
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.errors.ImpossibleActionException
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.{ExtDirectService, ISODateTime, LimitsSortsFilters, typingMap}

import scala.collection.JavaConversions.mapAsScalaMap;

@ExtDirectService
class TerminalMessagesService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model(/*"lon", "lat", "satelliteNum",*/ "regeo", "speed", "course", Field("time", "date"), "timemils",Field("insertTime", "date"), "devdata", "coordinates")
  val name = "TerminalMessagesService"
  val idProperty = "_id"
  val lazyLoad = true

  val savableFields = model.fieldsNames //- "cost" - "objectsCount" - "usersCount"

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var pathStore: PackagesStore = null

  @Autowired(required = false)
  var reaggregator: AccumulatedParamsReaggregator = null

  @ExtDirectMethod
  def remove(uid: String, maps: Seq[Map[String, Any]]) = {
    val dates = maps.map(e => new Date(e.as[Long]("timemils")))
    debug("remove:" + uid + " " + dates)

    for (date <- dates) {
      pathStore.removePositionData(uid, date, date)
    }

  }

  @ExtDirectMethod
  def removeInInterval(uid: String, from: Date, to: Date) = {
    debug("remove:" + uid + " " + from + " " + to)
    pathStore.removePositionData(uid, from, to)
  }

  @ExtDirectMethod
  def reaggregate(uid: String, from: Date) = {
    if (reaggregator == null)
      throw new ImpossibleActionException("сервис обновления показателей не доступен в данной конфигурации")
    reaggregator.update(uid, from)
  }


  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {
    debug("AccountsStoreService read " + request)
    val params = request.getParams
    val uid = params.get("uid").asInstanceOf[String]
    debug("AccountsStoreService uid " + uid + " dateFrom " + params.get("dateFrom") + " dateTo " + params.get("dateTo"))
    val dateFrom = Option(params.get("dateFrom")).map(x => utils.parseDate(x.asInstanceOf[String])).getOrElse(new Date(111, 0, 1))
    val dateTo = Option(params.get("dateTo")).map(x => utils.parseDate(x.asInstanceOf[String])).getOrElse(new Date())
    debug("request.getStart() " + request.getStart())
    debug("request.getLimit() " + request.getLimit())
    val ls = new LimitsSortsFilters(
      Option(request.getStart()).getOrElse(Integer.valueOf(0)).intValue(),
      Option(request.getLimit()).getOrElse(Integer.valueOf(200)).intValue()
    )
    val data = pathStore.getHistoryFor(uid, dateFrom, dateTo, ls) //.take(100)
    EDSJsonResponse(data.total, data.map(gps => Map(
      //      "lon" -> gps.lon,
      //      "lat" -> gps.lat,
      //      "satelliteNum"->gps.satelliteNum,
      "coordinates" -> (gps.lat + " " + gps.lon + " (" + gps.satelliteNum + ")"),
      "time" ->  ISODateTime(gps.time),
      "timemils" ->  gps.time,
      "insertTime" -> ISODateTime(gps.insertTime),
      "speed" -> gps.speed,
      "course" -> gps.course,
      "regeo" -> Option(gps.placeName).getOrElse(""),
      "devdata" -> Option(gps.data).map(_.map(kv => kv._1 + " = " + kv._2).toSeq.sorted.mkString(", ")).getOrElse("")
    )))
  }
}
