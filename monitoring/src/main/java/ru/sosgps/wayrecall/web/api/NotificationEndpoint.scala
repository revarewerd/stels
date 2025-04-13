package ru.sosgps.wayrecall.web.api

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, RestController}
import ru.sosgps.wayrecall.core.{GPSData, MongoDBManager, ObjectsRepositoryReader, UserRolesChecker}
import ru.sosgps.wayrecall.data.{PackagesStore, UserMessage}
import ru.sosgps.wayrecall.events.{EventsStore, OnTarget, PredefinedEvents, QueryResult}
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker
import ru.sosgps.wayrecall.utils.Memo

import java.util.Date

@RestController
class NotificationEndpoint(@Autowired es: EventsStore,
                           @Autowired packStore: PackagesStore,
                           @Autowired permissionsChecker: ObjectsPermissionsChecker,
                           @Autowired or: ObjectsRepositoryReader)
  extends grizzled.slf4j.Logging {

  @RequestMapping(value=Array("/getNotifications"))
  def getNotifications( @RequestParam("dateFrom") dateFrom: Date,
                        @RequestParam("dateTo") dateTo: Date
                        ): Iterable[Map[String, Any]] = {
    val events = getUserEvents(dateFrom).dropWhile(_.time > dateTo.getTime)
    debug( s"From $dateFrom, to $dateTo")
    getObjectEventsMap(events)
  }

  private def getObjectEventsMap(events: Iterable[UserMessage]): Iterable[Map[String, Any]] = {
    val objectByUid = Memo(or.getObjectByUid)
    events.collect({
      case event: UserMessage =>
        Map(
          "eid" -> event.hashCode,
          "uid" -> event.subjObject.orNull,
          "objectInfo" -> event.subjObject.map(objectByUid).map(d => Map(
            "name" -> d.get("name"),
            "customName" -> d.get("customName"),
            "gosnumber" -> d.get("gosnumber")
          )).orNull,
        "text" -> event.message,
        "time" -> event.time,
        "type" -> event.`type`,
//        "user" -> Option(event.fromUser).flatten.getOrElse(""),
        "targetId" -> event.targetId
      ) ++ event.additionalData ++ detectPosition(event)
    })
  }


  def getUserEvents(date: Date): QueryResult[UserMessage] = {
    require(date != null, "date must not be null")
    es.getEventsAfter(OnTarget(PredefinedEvents.userMessage, permissionsChecker.username), date.getTime)
  }

  private def getCorrespondingGPS(time: Long, uid: String): Option[GPSData] = {
    try {
      val t = new DateTime(time)
      packStore.getHistoryFor(uid, t.minusMinutes(15).toDate, t.plusMinutes(15).toDate).reduceOption(
        (g1, g2) => if (Math.abs(t.getMillis - g1.time.getTime) < Math.abs(t.getMillis - g2.time.getTime)) g1 else g2
      )
    }
    catch {
      case e: Exception => warn("getCorrespondingGPS error", e); None
    }
  }

  // TODO This duplication is evil
  def detectPosition(event: UserMessage): Map[_ <: String, Double] = {
    if (event.subjObject.isDefined && !event.additionalData.contains("lon")) getCorrespondingGPS(event.time, event.subjObject.get)
      .map(gps => Map("lat" -> gps.lat, "lon" -> gps.lon))
      .getOrElse(Map.empty)
    else Map.empty
  }
}
