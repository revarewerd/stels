package ru.sosgps.wayrecall.data

import java.lang
import java.util.Date
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import com.google.common.cache.{CacheBuilder, CacheLoader}
import org.axonframework.eventhandling.annotation.EventHandler
import ru.sosgps.wayrecall.security
import ru.sosgps.wayrecall.utils.{impldotChain, PartialIterable, LimitsSortsFilters}
import ru.sosgps.wayrecall.utils.web.{ScalaServletConverters, springCurrentRequest, springCurrentRequestOption}
import javax.servlet.http.HttpSession
import scala.beans.BeanProperty
import scala.ref.{ReferenceWrapper, SoftReference, WeakReference}
import org.springframework.web.context.request.{ServletRequestAttributes, RequestContextHolder}
import grizzled.slf4j.{Logger, Logging}
import ru.sosgps.wayrecall.core._
import scala.collection.mutable
import org.springframework.beans.factory.annotation.Autowired
import ScalaServletConverters.sessionToMap
import ru.sosgps.wayrecall.security.{requirePermission, ObjectsPermissionsChecker}
import javax.jms.{Message, ObjectMessage}
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.events.{PredefinedEvents, EventsStore}
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 30.10.12
 * Time: 0:16
 * To change this template use File | Settings | File Templates.
 */
class CachedPackageStore extends PackagesStore with Logging {

  @Autowired
  protected var mongoDbManager: MongoDBManager = null;

  @Autowired
  var or: ObjectsRepositoryReader = null

  @BeanProperty
  var wrapped: PackagesStore = null;

  @Autowired
  var es: EventsStore = null

  @Autowired
  var pconv: PackDataConverter = null;

  private[this] val latest = new {

    //def apply(uid: String): GPSData = latestCache.getOrElseUpdate(uid, getLatestFromDatabase(uid).get)

    private[this] val latestCache: mutable.Map[String, Option[GPSData]] = mutable.HashMap()

    def get(uid: String): Option[GPSData] = {
      val fromCache: Option[Option[GPSData]] = latestCache.get(uid)
      fromCache.getOrElse({
        val lfd: Option[GPSData] = getLatestFromDatabase(uid)
        latestCache.synchronized {
          latestCache.put(uid, lfd.map(pconv.convertData).map(l => markParkings(l, l)))
        }
        lfd
      })
    }

    def getOnlyCached(uid: String): Option[GPSData] = latestCache.get(uid).flatMap(p => p)

    private[this] def getLatestFromDatabase(uid: String): Option[GPSData] = {
      debug("loading getLatestFor " + uid + " from database")
      wrapped.getLatestFor(Iterable(uid)).headOption
        .map(gps => {if (gps.insertTime == null) gps.insertTime = new Date(); gps})
    }

    def put(uid: String, gps: GPSData) = latestCache.synchronized {
      latestCache.put(uid, Option(pconv.convertData(gps)))
    }

    def remove(uid: String) = latestCache.remove(uid)

  }

  @PostConstruct
  def init() {
    es.subscribeTyped(PredefinedEvents.objectGpsEvent, onGpsEvent)


    if (!System.getProperty("wayrecall.skipCachePreload", "false").toBoolean) {
      val thread = new Thread(new Runnable {
        def run() = try {
          debug("start lastMessageLoader")
          val uids = mongoDbManager.getDatabase()("objects").find(MongoDBObject(), MongoDBObject("uid" -> 1)).flatMap(_.getAs[String]("uid"))
          getLatestFor(uids.toStream).foreach(gps => {
            info("lastMessageLoader preloaded: " + gps.imei)
          })
          debug("finished lastMessageLoader")
        } catch {
          case e: Throwable => {
            error("lastMessageLoader error", e)
          }
        }

      }, "lastMessageLoader")
      thread.setPriority(Thread.MIN_PRIORITY)
      thread.start()
    }

  }

  def getLatestFor(uids: Iterable[String]): Iterable[GPSData] = uids.flatMap(latest.get)

  //def getLatestFor(uids: Iterable[String]) = wrapped.getLatestFor(uids)

  def getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double) = wrapped.getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double)

  def getPositionsCount(uid: String, from: Date, cur: Date) = wrapped.getPositionsCount(uid: String, from: Date, cur: Date)

  def getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters) = {
    val session = springCurrentRequestOption.map(_.getSession(true))

    getHistory(session, uid, from, to, limitsAndSorts: LimitsSortsFilters)
  }

  case class SessionEntry(selectedUid: String, from: Date, to: Date, history: MovementHistory, limitsAndSort: LimitsSortsFilters)

  private def getHistory(sessionOpt: Option[HttpSession], selectedUid: String, from: Date, to: Date, limitsAndSort: LimitsSortsFilters): MovementHistory = {

    debug("is about to get data for " + selectedUid + " " + from + "-" + to)
    val history: MovementHistory = sessionOpt match {
      case Some(session) => session.synchronized {
        Option(session.getAttribute("history")).
          flatMap({ case wr: ReferenceWrapper[SessionEntry] => wr.get }).
          flatMap(
            entry =>
              if (selectedUid == entry.selectedUid && entry.limitsAndSort.accepts(limitsAndSort) && entry.from == from && entry.to == to) {
              //  debug("getting from cache " + selectedUid + " " + from + "-" + to + " cached entry reslen=" + entry.history.total)
                Some(entry)
              }
              else {
                debug("cache is not actual for " + selectedUid + " " + from + "-" + to)
                None
              }
          ).getOrElse {
          debug("no cache for " + selectedUid + " " + from + "-" + to + " loading from db")
          val st = System.currentTimeMillis()
          val hm = wrapped.getHistoryFor(selectedUid, from, to, limitsAndSort)
          debug("wrapped.getHistoryFor took " + (System.currentTimeMillis() - st))
          val entry = SessionEntry(selectedUid, from, to, hm, new LimitsSortsFilters(hm.start, if (hm.limit < hm.total) Some(hm.limit) else None, limitsAndSort.sortName, limitsAndSort.order))
          session.setAttribute("history", new SoftReference(entry))
          entry
        }.history
      }
      case None => {
        val st = System.currentTimeMillis()
        val hm = wrapped.getHistoryFor(selectedUid, from, to, limitsAndSort)
        debug("wrapped.getHistoryFor took " + (System.currentTimeMillis() - st))
        hm
      }
    }

    //debug("got head of data  " + selectedUid + " " + from + "-" + to)
    if (history.total > 1000000)
      throw new IllegalArgumentException("too many data: " + history.total + "")

    val res = history.getPart(limitsAndSort.start, limitsAndSort.limit.getOrElse(history.total)): MovementHistory
   // debug("returning got data for " + selectedUid + " " + from + "-" + to)
    res
  }

  def onGpsEvent(e: GPSEvent) {
    val gpsdata = e.gpsData
    trace(mongoDbManager.getDatabaseName + " received gpsdata " + gpsdata)
    if (validTime(gpsdata) && !or.isRemoved(gpsdata.uid)) {
      latest.getOnlyCached(gpsdata.uid) match {
        case Some(prevcoords) =>
          if (
            gpsdata.containsLonLat() &&
              prevcoords.time.before(gpsdata.time)
          ) {
            putToLatest(gpsdata)
          }
        case None =>
          putToLatest(gpsdata)
      }
    }
  }





  val newDataListeners: mutable.Buffer[(GPSData) => Any] = new ArrayBuffer[(GPSData) => Any]() with mutable.SynchronizedBuffer[(GPSData) => Any]

  private[this] def putToLatest(gpsdata: GPSData) {
    //debug("Latest GPS data = " + gpsdata.data)
    val last = latest.get(gpsdata.uid).getOrElse(gpsdata)
    markParkings(gpsdata, last)
  }

  private def markParkings(gpsdata: GPSData, last: GPSData): GPSData = {
    if (last.data.containsKey("currentInterval") && last.data.containsKey("isParking")) {
     // debug(s"Parking interval information exists for ${gpsdata.uid}")
      if ((gpsdata.speed < 5) == last.data.get("isParking").asInstanceOf[Boolean]) {
        val interval: lang.Long = last.data.get("currentInterval").asInstanceOf[Long] + (gpsdata.time.getTime - last.time.getTime)
        gpsdata.data.put("isParking", last.data.get("isParking").asInstanceOf[lang.Boolean])
        gpsdata.data.put("currentInterval", interval)
      } else {
        gpsdata.data.put("isParking", (!last.data.get("isParking").asInstanceOf[Boolean]).asInstanceOf[lang.Boolean])
        gpsdata.data.put("currentInterval", 0L.asInstanceOf[lang.Long])
      }
    } else {
   //   debug(s"Parking interval information does not present for ${gpsdata.uid}")
      val curParking = (gpsdata.speed < 5)
      //val buffer = mongoDbManager.getDatabase()("objPacks.buffer").find(MongoDBObject("uid" -> gpsdata.uid), MongoDBObject("time" -> 1, "speed" -> 1)).sort(MongoDBObject("time" -> -1))
      val buffer = unwrap[MongoPackagesStore].map(_.biggestBuffer.find(MongoDBObject("uid" -> gpsdata.uid), MongoDBObject("time" -> 1, "speed" -> 1)).sort(MongoDBObject("time" -> -1)))
    //  debug(s"Parking interval information buffer size: ${buffer.map(_.length)}")
      val interval = {
        var firstTime = gpsdata.time
        buffer.foreach(_.foreach(r => {
          if ((r.getAsOrElse[Short]("speed", 0) < 5) == curParking) {
            firstTime = r.getAsOrElse[java.util.Date]("time", firstTime)
          } else {
            return gpsdata
          }
        }))

        if (firstTime != gpsdata.time) {
          gpsdata.time.getTime - firstTime.getTime
        } else {
          0L
        }
      }

      gpsdata.data.put("isParking", curParking.asInstanceOf[lang.Boolean])
      gpsdata.data.put("currentInterval", interval.asInstanceOf[lang.Long])
    }
    gpsdata.insertTime = new Date()
    latest.put(gpsdata.uid, gpsdata)
    newDataListeners.foreach(_ (gpsdata))
    gpsdata
  }

  private[this] def validTime(gpsdata: GPSData): Boolean = {
    gpsdata.time.getTime < (System.currentTimeMillis() + 1000 * 60 * 15)
  }

  def removePositionData(uid: String, from: Date, to: Date) = {
    latest.getOnlyCached(uid) match {
      case Some(gpsdata) =>
        if (gpsdata.time == from || gpsdata.time == to || gpsdata.time.after(from) && gpsdata.time.before(to))
          latest.remove(uid)
      case None =>
    }
    wrapped.removePositionData(uid: String, from: Date, to: Date)
    refresh(uid)
  }

  override def refresh(uid: String): Unit = {
    latest.remove(uid)
    wrapped.refresh(uid)
    debug("refresh:" + uid)
    for (
      session <- Option(springCurrentRequest.getSession(false));
      history <- session.get("history").flatMap({ case e: ReferenceWrapper[SessionEntry] => e.get })
    ) {
      debug("session:" + history)
      if (history.selectedUid == uid) {
        debug("removing history")
        session.removeAttribute("history")
      }
    }
  }

  override def at(uid: String, date: Date): Option[GPSData] = wrapped.at(uid, date)

  override def prev(uid: String, date: Date): Option[GPSData] = wrapped.prev(uid, date)

  override def next(uid: String, date: Date): Option[GPSData] = wrapped.next(uid, date)

  override def unwrap[T <: PackagesStore : ClassTag]: Option[T] = super.unwrap.orElse(wrapped.unwrap[T])
}

class PermissionChekedPackageStore(wrapped: PackagesStore) extends PackagesStore with Logging {

  @Autowired
  var permissionsChecker: ObjectsPermissionsChecker = _

  def getLatestFor(uids: Iterable[String]): Iterable[GPSData] = if (uids == null)
    permissionsChecker.getAvailableObjectsPermissions().keys.flatMap(getLatest)
  else springCurrentRequestOption.map(_.getSession(true).getTyped[Object]("getLatestForLock", new Object)).getOrElse(new Object)
    .synchronized(uids.filter(permissionsChecker.checkIfObjectIsAvailable).flatMap(getLatest))


  private[this] def getLatest: (String) => Iterable[GPSData] = {
    p => wrapped.getLatestFor(Iterable(p)).headOption
  }

  override def getPositionsCount(uid: String, from: Date, cur: Date): Int = {
    checkPermissions(uid)

    wrapped.getPositionsCount(uid: String, from: Date, cur: Date)
  }


  override def getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters): MovementHistory = {
    checkPermissions(uid)
    wrapped.getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters)
  }

  override def removePositionData(uid: String, from: Date, to: Date): Unit = {
    checkPermissions(uid)
    wrapped.removePositionData(uid: String, from: Date, to: Date)
  }

  override def getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double): Option[GPSData] = {
    checkPermissions(uid)
    wrapped.getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double)
  }

  private def checkPermissions(uid: String): Unit = {
    requirePermission(permissionsChecker.checkIfObjectIsAvailable(uid), "user is not permitted to work with object " + uid)
  }

  override def refresh(uid: String): Unit = { checkPermissions(uid); wrapped.refresh(uid) }

  override def at(uid: String, date: Date): Option[GPSData] = { checkPermissions(uid); wrapped.at(uid, date) }

  override def prev(uid: String, date: Date): Option[GPSData] = { checkPermissions(uid); wrapped.prev(uid, date) }

  override def next(uid: String, date: Date): Option[GPSData] = { checkPermissions(uid); wrapped.next(uid, date) }

  override def unwrap[T <: PackagesStore : ClassTag]: Option[T] = super.unwrap.orElse(wrapped.unwrap[T])

}


