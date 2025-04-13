package ru.sosgps.wayrecall.monitoring.web

import java.util.concurrent.TimeUnit
import javax.annotation.{PostConstruct, Resource}

import com.google.common.cache.{CacheBuilder, LoadingCache}
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.{ExtDirectService, funcLoadingCache}
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{GPSData, MongoDBManager, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.events._
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import java.text.SimpleDateFormat
import java.util.Date

import org.joda.time.DateTime

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConversions.asScalaBuffer
import ru.sosgps.wayrecall.data.{PackagesStore, UserMessage}
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.events.OnTargets
import ru.sosgps.wayrecall.events.OnTarget
import ru.sosgps.wayrecall.utils.io.{serialize, tryDeserialize}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 22.08.13
 * Time: 19:20
 * To change this template use File | Settings | File Templates.
 */
@ExtDirectService
class EventsMessages extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("num", "eid", "time", "uid", "type", "name", "text", "user", "lon", "lat", "readStatus", "targetId")

  val name = "EventsMessages"
  override val autoSync = false
  val idProperty = "eid"
  val lazyLoad = false

  @Autowired
  var objCommander: ObjectsCommander = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var es: EventsStore = null

  @Autowired
  var permissionsChecker: ObjectsPermissionsChecker = null

  @Autowired
  var packStore: PackagesStore = null

  @PostConstruct
  def init() {
    es.subscribe(PredefinedEvents.userMessage, onNewUserMessage)
  }

  def onNewUserMessage(ue: UserMessage) {
    debug("onNewUserMessage:" + ue.message)
    unreadUserMessagesCache.invalidate(ue.targetId)
  }

  private[this] def eventsCollection = mdbm.getDatabase()("events")

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

  private[this] val unreadUserMessagesCache: LoadingCache[String, Integer] = CacheBuilder.newBuilder()
    .expireAfterWrite(30, TimeUnit.HOURS)
    .buildWithFunction((username: String) => {
      eventsCollection.find(MongoDBObject("eventType" -> "notification", "targetType" -> "user", "targetId" -> username, "additionalData.readStatus" -> false)).count()
    })

  @ExtDirectMethod
  def getUnreadUserMessagesCount(): Int = {
    unreadUserMessagesCache(permissionsChecker.username)
  }

  @ExtDirectMethod
  def updateEventsReadStatus(events: java.util.List[Map[String, Any]], readStatus: Boolean): Unit = {
    events.map(eventdata => eventdata + ("readStatus" -> readStatus)).foreach(eventdata => updateEventReadStatus(eventdata))
    unreadUserMessagesCache.invalidate(permissionsChecker.username)
  }

  @ExtDirectMethod
  def updateEventReadStatus(eventdata: Map[String, Any]) {
    val message = eventdata("text")
    val time = eventdata("time")
    val targetId = eventdata("targetId")
    val readStatus = eventdata("readStatus")
    val uid = eventdata.get("uid")
    debug("updateEventReadStatus eventdata.eid=" + eventdata("eid") + " new readStatus=" + readStatus)
    val searchCriteria = MongoDBObject("eventType" -> "notification", "targetType" -> "user", "time" -> time, "targetId" -> targetId)
    val userMessagesIterator = eventsCollection.find(searchCriteria).flatMap(dbo =>
      tryDeserialize[UserMessage](dbo.as[Array[Byte]]("event")) match {
        case Right(d) => Some(dbo._id.get, d)
        case Left(t) => warn("deserializationError:", t); None
      })

    val (umId, userMessage) = userMessagesIterator.find({
      case (id, um) => {
        um.message == message && um.subjObject == uid
      }
      case _ => false
    }).get

    debug("umId=" + umId)
    debug("userMessage=" + userMessage)

    val newAdditionalData = userMessage.additionalData ++ Map("readStatus" -> readStatus)
    val newUserMessage = new UserMessage(userMessage.targetId, userMessage.message, userMessage.`type`,
      userMessage.subjObject, userMessage.fromUser, userMessage.time, newAdditionalData)
    eventsCollection.update(searchCriteria ++ ("_id" -> umId), $set("event" -> serialize(newUserMessage.asInstanceOf[Event]), "additionalData" -> newAdditionalData), false, false, WriteConcern.Safe)
    unreadUserMessagesCache.invalidate(targetId)
  }

  @ExtDirectMethod
  def getLastEvent(uid: String) = {
    require(uid != null, "uid must be defined")
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    val dateFrom = new DateTime().minusDays(7)
    val from = utils.parseDate(dateFormat.format(dateFrom.withTimeAtStartOfDay.toDate))
    debug("Date interval = [" + from + ", " + /*to +*/ "]")
    debug("Messages for  uid = " + uid)
    val objectEvents = getUserEvents(from).filter(e => uid == e.subjObject.getOrElse("")) //.iterator

    val lastMessage = objectEvents.head
    val result = Map(
      "eid" -> lastMessage.hashCode(),
      "uid" -> lastMessage.subjObject.orNull,
      "name" -> lastMessage.subjObject.map(or.getUserObjectName).getOrElse(""),
      "text" -> lastMessage.message,
      "time" -> lastMessage.time,
      "type" -> lastMessage.`type`,
      "user" -> Option(lastMessage.fromUser).flatten.getOrElse(""),
      "targetId" -> lastMessage.targetId
    ) ++ lastMessage.additionalData ++ detectPosition(lastMessage)
    result
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadObjects(request: ExtDirectStoreReadRequest) = {
    val uids = (request.getParams.get("uids") match {
      case u: java.util.ArrayList[String] => {
        u.toArray.toSeq
      }
      case _ => {
        Seq.empty
      }
    }).asInstanceOf[Seq[String]]
    debug("Messages for selected uids = " + uids + ", Is EMPTY = " + uids.isEmpty)

    def filteredByTime() = {
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
      val today = new DateTime()
      val from = utils.parseDate(request.getParams.getOrElse("from", dateFormat.format(today.withTimeAtStartOfDay.toDate)).asInstanceOf[String])
      val to = utils.parseDate(request.getParams.getOrElse("to", dateFormat.format(today.toDate)).asInstanceOf[String])
      debug("Date interval = [" + from + ", " + to + "]")
      val objectEvents = if (uids.isEmpty) getUserEvents(from).iterator else getUserEvents(from).filter(e => uids.contains(e.subjObject.getOrElse(""))).iterator
      val filteredEvents = objectEvents.dropWhile(a => {
        a.time > to.getTime
      })
      getObjectEventsMap(filteredEvents)
    }

    def getObjectEventsMap(events: Iterator[UserMessage]) = {
      events.collect({
        case event: UserMessage => Map(
          "eid" -> event.hashCode,
          "uid" -> event.subjObject.orNull,
          "name" -> event.subjObject.map(or.getUserObjectName).getOrElse(""),
          "text" -> event.message,
          "time" -> event.time,
          "type" -> event.`type`,
          "user" -> Option(event.fromUser).flatten.getOrElse(""),
          "targetId" -> event.targetId
        ) ++ event.additionalData ++ detectPosition(event)
      }).zipWithIndex.map { case (map, ind) => map + ("num" -> ind) }
    }

    val filterType = request.getParams.getOrElse("filterType", "period").asInstanceOf[String]
    debug("filterType=" + filterType)

    filterType match {
      case ("period") =>
        filteredByTime
      case ("unread") =>
        val events =
          if (uids.isEmpty) getFilteredEvents(Map("readStatus" -> false)).iterator
          else getFilteredEvents(Map("readStatus" -> false)).filter(e => uids.contains(e.subjObject.getOrElse(""))).iterator
        getObjectEventsMap(events)
    }
  }

  @ExtDirectMethod
  def getMessageHash(text: String = "text", msgType: String = "type", time: Long = 1L) = (text.hashCode * 33L + msgType.hashCode * 13L + time).toString

  def detectPosition(event: UserMessage): Map[_ <: String, Double] = {
    if (event.subjObject.isDefined && !event.additionalData.contains("lon")) getCorrespondingGPS(event.time, event.subjObject.get)
      .map(gps => Map("lat" -> gps.lat, "lon" -> gps.lon))
      .getOrElse(Map.empty)
    else Map.empty
  }

  def getObjectPhonesEventsAfter(date: Date): QueryResult[Event] = {
    val phones = permissionsChecker.getAvailableObjectsPermissions().keys.flatMap(uid => {
      val phone = objCommander.phonesCache(uid)
      //if (phone.isEmpty) warn("uid: " + uid + " has not defined phone number")
      phone
    }).toSet

    //debug("phones=" + phones)
    es.getEventsAfter(new OnTargets[Event]("objectPhone", phones), date.getTime)
  }

  def getUserEvents(date: Date) = {
    require(date != null, "date must not be null")
    es.getEventsAfter(OnTarget(PredefinedEvents.userMessage, permissionsChecker.username), date.getTime)
  }

  def getFilteredEvents(filter: Map[String, Any]) = {
    require(!filter.isEmpty, "filter must not be null")
    es.getEventsFiltered(OnTarget(PredefinedEvents.userMessage, permissionsChecker.username), filter)
  }

  @ExtDirectMethod
  def getUpdatedAfter(date: Date): Map[String, Any] = {
    val userMessages = this.getUserEvents(date)
    if (userMessages.nonEmpty)
      debug(s"getUpdatedAfter($date) userMessages=" + userMessages.mkString("[", ",", "]"))
    val objectListChanges = es.getEventsAfter(OnTarget(PredefinedEvents.userObjectsListChanged, permissionsChecker.username), date.getTime)
    Map(
      "newTime" -> (Seq(objectListChanges.lastAddedEventTime, userMessages.lastAddedEventTime).max + 1),
      "data" -> userMessages.collect({
        case event: UserMessage => Map(
          "uid" -> event.subjObject,
          "name" -> event.subjObject.map(or.getUserObjectName).getOrElse(""),
          "text" -> event.message,
          "time" -> event.time,
          "type" -> event.`type`,
          "user" -> Option(event.fromUser).flatten.getOrElse(""),
          "targetId" -> event.targetId
        ) ++ event.additionalData ++ detectPosition(event)
      }),
      "reload" -> objectListChanges.nonEmpty
    )
  }


}
