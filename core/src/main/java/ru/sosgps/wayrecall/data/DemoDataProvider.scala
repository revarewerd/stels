package ru.sosgps.wayrecall.data

import java.util
import java.util.{TimerTask, Timer, Date}
import ru.sosgps.wayrecall.utils.LimitsSortsFilters
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.core._
import com.mongodb.casbah.commons.MongoDBObject
import scala.Some
import ru.sosgps.wayrecall.events.{DataEvent, PredefinedEvents, EventsStore}
import ru.sosgps.wayrecall.utils.tryLong
import ru.sosgps.wayrecall.utils.funcToTimerTask
import ru.sosgps.wayrecall.utils
import org.axonframework.eventhandling.annotation.{AnnotationEventListenerAdapter, EventHandler}
import org.axonframework.eventhandling.EventBus

import scala.beans.BeanProperty
import scala.reflect.ClassTag

class DemoDataProvider(val wrapped: PackagesStore) extends PackagesStore with MultiObjectsPacketsMixin with grizzled.slf4j.Logging {

  @Autowired
  protected var mongoDbManager: MongoDBManager = null;

  @Autowired
  protected var or: ObjectsRepositoryReader = null;

  @Autowired
  protected var timer: Timer = null;

  @Autowired
  var es: EventsStore = null

  @Autowired
  var removalIntervals: RemovalIntervalsManager = null

  class Remapping(val origUid: String, val lateinterval: Long) {

    debug("Remapping lateinterval: " + lateinterval)

    def convertTime(time: Date): Date = if (time != null)
      new Date(time.getTime + lateinterval)
    else
      null

    def deconvertTime(time: Date): Date = if (time != null)
      new Date( if(Long.MinValue + lateinterval >= time.getTime) time.getTime - lateinterval else Long.MinValue)
    else
      null


    def canEqual(other: Any): Boolean = other.isInstanceOf[Remapping]

    override def equals(other: Any): Boolean = other match {
      case that: Remapping =>
        (that canEqual this) &&
          origUid == that.origUid &&
          lateinterval == that.lateinterval
      case _ => false
    }

    override def hashCode(): Int = {
      val state = Seq(origUid, lateinterval)
      state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
    }

    override def toString() = s"Remapping(orig=$origUid, late=$lateinterval)";
  }

  private[this] var demoMapping: Map[String, Remapping] = Map.empty
  private[this] var demoMappingRevese: Map[String, String] = Map.empty

  @BeanProperty
  var restartInterval = 1000 * 60 * 30

  val defaultDemoLate = 1000L * 60 * 60 * 24 * 80

  @PostConstruct
  def init() {
    es.subscribeTyped(PredefinedEvents.objectGpsEvent, onGpsEvent)
    es.subscribeTyped(PredefinedEvents.equipmentChange, onEqChange)

    try
      reinitDemoMapping(loadDemoMapping())
    catch {
      case e: Exception => error("error in initialization", e)
    }
  }

  private def onEqChange(e: AnyRef) = synchronized {
    debug("onObjectChange " + e)
    val nm = loadDemoMapping()
    debug("new Mapping " + nm)
    debug("old Mapping " + demoMapping)
    if (nm != demoMapping)
      reinitDemoMapping(nm)
  }


  private def onGpsEvent(e: GPSEvent) {
    val gpsdata = e.gpsData
    demoMappingRevese.get(gpsdata.uid).foreach(demouid => {
      val ngps: GPSData = convertGPS(this.demoMapping(demouid))(gpsdata)
      trace("received converted gpsdata " + ngps)
      if (ngps.time.getTime < System.currentTimeMillis()) {
        trace("publising")
        es.publish(new GPSEvent(ngps))
      }
    })
  }

  private[this] def reinitDemoMapping(newDemoMapping: Map[String, Remapping]) {
    val demoMappingDiff = newDemoMapping -- demoMapping.keys
    demoMapping = newDemoMapping
    demoMappingRevese = demoMapping.map(kv => (kv._2.origUid, kv._1)).toMap
    debug("demoMappingRevese:" + demoMappingRevese)
    for (uid <- demoMappingDiff) {
      startPublishing(uid)
    }
  }


  private[this] def loadDemoMapping(): Map[String, Remapping] = {
    val demoTargetted = mongoDbManager.getDatabase()("objects").find(
      MongoDBObject("demoTarget" -> MongoDBObject("$exists" -> true)),
      MongoDBObject("uid" -> 1, "demoTarget" -> 1, "demoLate" -> 1)
    ).map(dbo => (dbo.as[String]("uid"),
      new Remapping(dbo.as[String]("demoTarget"), dbo.getAs[Any]("demoLate").filter(None !=).map(tryLong).getOrElse(defaultDemoLate)))).toMap

    val eqTargetted = (for (
      eqDbo <- mongoDbManager.getDatabase()("equipments").find(
        MongoDBObject("eqtype" -> "Виртуальный терминал", "uid" -> MongoDBObject("$exists" -> true)),
        MongoDBObject("uid" -> 1, "eqIMEI" -> 1, "eqConfig" -> 1)
      );
      uid <- eqDbo.getAs[String]("uid");
      dbo <- mongoDbManager.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid))
    )
      yield {
        (dbo.as[String]("uid"),
          new Remapping(eqDbo.as[String]("eqIMEI").stripPrefix("rt"),
            utils.tryParseInt(eqDbo.as[String]("eqConfig").stripSuffix("late=")).getOrElse(0) * 60 * 1000))
      }).toMap
    demoTargetted ++ eqTargetted
  }

  private[this] def startPublishing(mappingEntry: (String, Remapping)) {
    val iterator = getHistoryFor(mappingEntry._1, new Date, mappingEntry._2.convertTime(new Date())).iterator
    publishByInterval(iterator, mappingEntry)
  }

  private[this] def publishByInterval(iterator: Iterator[GPSData], mappingEntry: (String, Remapping)) {
    if (!demoMapping.contains(mappingEntry._1))
      return
    if (iterator.hasNext) {
      val g = iterator.next()
      val interval = Math.max(g.time.getTime - System.currentTimeMillis(), 0L)
      trace("publishByInterval:" + interval + " data:" + g)
      timer.schedule(try {
        es.publish(new GPSEvent(g))
        publishByInterval(iterator, mappingEntry)
      } catch {
        case t: Throwable => {
          timer.schedule({
            startPublishing(mappingEntry)
          }, restartInterval)
          throw t;
        }
      }, interval)
    }
    else {
      warn("publishByInterval finished")
      timer.schedule({
        startPublishing(mappingEntry)
      }, restartInterval)
    }
  }

  //  @BeanProperty
  //  var lateinterval = 1000 * 60 * 60 * 24 * 8



  val MinDate = new Date(Long.MinValue)
  val MaxDate = new Date(Long.MaxValue)

  override def getLatestFor(uids: Iterable[String]): Iterable[GPSData] = {
    uids.flatMap(uid => demoMapping.get(uid) match {
      case Some(rm) => { // Щито поделать
        removalIntervals.getFilteredIntervals(uid, MinDate, MaxDate).reverse.toStream.map { case (minTime, maxTime) =>
          collCache.get(rm.origUid).find(MongoDBObject("lon" -> MongoDBObject("$exists" -> true)) ++ ("time" $lt rm.deconvertTime(new Date)  $gte rm.deconvertTime(minTime)))
            .sort(MongoDBObject("time" -> -1)).limit(1)
            .map(convertGps(rm))
            .toIterable
        }.find(_.nonEmpty).getOrElse(Iterable())
      }
      case None => wrapped.getLatestFor(Iterable(uid))
    })
  }

  //  override def getLatestFor(uids: Iterable[String]): Iterable[GPSData] = {
//    uids.flatMap(uid => demoMapping.get(uid) match {
//      case Some(rm) => {
//        collCache.get(rm.origUid).find(MongoDBObject("lon" -> MongoDBObject("$exists" -> true)) ++ ("time" $lt rm.deconvertTime(new Date())))
//          .sort(MongoDBObject("time" -> -1)).limit(1)
//          .map(convertGps(rm))
//          .toIterable
//      }
//      case None => wrapped.getLatestFor(Iterable(uid))
//    })
//  }


  private[this] def convertGps(rm: Remapping)(d: DBObject): GPSData = {
    val gps = GPSDataConversions.fromDbo(d)
    convertGPS(rm)(gps)
  }

  private[this] def convertGPS(rm: Remapping)(gps: GPSData): GPSData = {
    val r = new GPSData(Option(gps.uid).map(demoMappingRevese).orNull, gps.imei, gps.lon, gps.lat, rm.convertTime(gps.time), gps.speed, gps.course, gps.satelliteNum, gps.placeName, rm.convertTime(gps.insertTime))
    r.goodlonlat = gps.goodlonlat
    r.data = new util.HashMap[String, AnyRef](gps.data)
    r
  }

  def getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters): MovementHistory = demoMapping.get(uid) match {
    case Some(rm: Remapping) => {
      debug("demouid:" + rm.origUid)
      val origHistory = wrapped.getHistoryFor(rm.origUid, rm.deconvertTime(from), rm.deconvertTime(to), limitsAndSorts: LimitsSortsFilters)
      debug("origHistory:" + origHistory.take(5).toList)
      new MovementHistory(origHistory.total, origHistory.start, origHistory.limit, origHistory.iterator.map(convertGPS(rm)))
    }
    case None => wrapped.getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters)
  }

  def getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double): Option[GPSData] = demoMapping.get(uid) match {
    case Some(rm: Remapping) => {
      wrapped.getNearestPosition(rm.origUid, rm.deconvertTime(from), rm.deconvertTime(to), lon: Double, lat: Double, radius: Double).map(convertGPS(rm))
    }
    case None => wrapped.getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double)
  }

  def getPositionsCount(uid: String, from: Date, cur: Date): Int = demoMapping.get(uid) match {
    case Some(rm: Remapping) => {
      wrapped.getPositionsCount(rm.origUid, rm.deconvertTime(from), rm.deconvertTime(cur))
    }
    case None => wrapped.getPositionsCount(uid: String, from: Date, cur: Date)
  }

  def removePositionData(uid: String, from: Date, to: Date) = wrapped.removePositionData(uid: String, from: Date, to: Date)

  override def refresh(uid: String): Unit = wrapped.refresh(uid)

  override def at(uid: String, date: Date): Option[GPSData] = query(uid, date, wrapped.at)

  override def prev(uid: String, date: Date): Option[GPSData] = query(uid, date, wrapped.prev)

  override def next(uid: String, date: Date): Option[GPSData] = query(uid, date, wrapped.next)

  private def query(uid: String, date: Date, f: (String, Date) => Option[GPSData]) = demoMapping.get(uid) match {
    case Some(rm: Remapping) => {
      f(uid, rm.deconvertTime(date))
    }
    case None => f(uid, date)
  }

  override def unwrap[T <: PackagesStore : ClassTag]: Option[T] = super.unwrap.orElse(wrapped.unwrap[T])
}
