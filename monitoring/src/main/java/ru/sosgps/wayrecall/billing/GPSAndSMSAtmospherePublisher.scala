package ru.sosgps.wayrecall.billing

import java.util.{Objects, Timer}

import javax.annotation.{PostConstruct, Resource}
import org.atmosphere.cpr.{Broadcaster, BroadcasterFactory}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.WebApplicationContext
import ru.sosgps.wayrecall.core.{GPSData, GPSUtils}
import ru.sosgps.wayrecall.data.CachedPackageStore
import ru.sosgps.wayrecall.data.sleepers.SleeperData
import ru.sosgps.wayrecall.events.{DataEvent, EventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.utils.funcToTimerTask
import ru.sosgps.wayrecall.utils.web.ScalaJson

import scala.collection.JavaConversions.{collectionAsScalaIterable, setAsJavaSet}
import scala.collection.concurrent.TrieMap
import scala.collection.mutable

/**
 * Created by nickl on 16.06.14.
 */
class GPSAndSMSAtmospherePublisher extends grizzled.slf4j.Logging {

  @Autowired
  var es: EventsStore = null

  @Autowired
  var timer: Timer = null

  @Resource(name = "cachedPackageStore")
  var cachedPackageStore: CachedPackageStore = null

  @Autowired
  var allOS: AllObjectsService = null

  @Autowired private[billing] var context: WebApplicationContext = null


  def getBroadcaster(): Broadcaster = {
    val broadcasterFactory: BroadcasterFactory =
      Objects.requireNonNull(
        context.getServletContext.getAttribute(classOf[BroadcasterFactory].getName).asInstanceOf[BroadcasterFactory], "BroadcasterFactory"
      )
    val broadCaster: Broadcaster = broadcasterFactory.lookup("servermes", true)
    debug("getting broadcaster:" + System.identityHashCode(broadCaster) + " :" + broadCaster);
    broadCaster
  }

  @PostConstruct
  def init() {
    cachedPackageStore.newDataListeners += onGpsEvent
    es.subscribe(PredefinedEvents.objectNewSleeperData, onNewSleeperData)
    timer.schedule(broadcastCurrent(), 5000, 5000)
  }

  private def broadcastCurrent() = try {
    val toResendGPS = lastGPSSendCache
    //toResendGPS.clear()
    lastGPSSendCache = new TrieMap[String, GPSData]()
    val toResendSleeper = lastSleeperSendCache
    lastSleeperSendCache = new TrieMap[String, SleeperData]()
    val gpsValues = toResendGPS.values
    val sleeperValues = toResendSleeper.values
//    for(resource <- broadcaster.getAtmosphereResources){
//      debug("resource:"+resource.getRequest.getUserPrincipal)
//    }
    if (toResendGPS.nonEmpty || toResendSleeper.nonEmpty) {
      val broadcaster = this.getBroadcaster()
      debug("broadcusting :" + gpsValues.size + " " + sleeperValues.size)
      val enabled = broadcaster.getAtmosphereResources.filter(
        _.getRequest.getCookies.exists(c => c.getName == "refreshGPS" && c.getValue.toLowerCase == "true")
      ).toSet

      debug("enabled :" + enabled)

      broadcaster.broadcast(ScalaJson.generate(Map(
        "eventType" -> "aggregateEventBatch",
        "events" -> (gpsValues.map(gpsToMap) ++ sleeperValues.map(sleeperToMap))
      )), enabled)
    }

  } catch {
    case e: Throwable => warn("broadcastCurrent() exception:", e); throw e
  }

  @volatile
  private var lastGPSSendCache: mutable.Map[String, GPSData] = new TrieMap[String, GPSData]()

  def onGpsEvent(gpsdata: GPSData) {
    //debug("onGpsEvent:" + e)

    val lastGPSSendCacheCur = lastGPSSendCache

    lastGPSSendCacheCur.get(gpsdata.uid) match {
      case Some(prevcoords) =>
        if (
          gpsdata.containsLonLat() &&
            prevcoords.time.before(gpsdata.time)
        ) {
          lastGPSSendCacheCur(gpsdata.uid) = gpsdata
        }
      case None =>
        lastGPSSendCacheCur(gpsdata.uid) = gpsdata
    }

  }


  private[this] def gpsToMap(e: GPSData): Map[String, Any] = {
    val latest = Some(e)

    Map(
      "eventType" -> "objectsUpdate",
      "aggregate" -> "object",
      "action" -> "update",
      "itemId" -> e.uid,
      "data" -> Map(
        "latestmsg" -> latest.map(_.time).orNull,
        "ignition" -> latest.map(g => GPSUtils.detectIgnition(g).getOrElse(-1)).orNull,
        "latestmsgprotocol" -> latest.map(g => Option(g.data.get("protocol")).getOrElse("Wialon")).orNull,
        //"sms" -> hasSmses(d),
        "speed" -> latest.map(_.speed).getOrElse(0),
        "satelliteNum" -> latest.map(_.satelliteNum).getOrElse(0),
        "placeName" -> latest.map(_.placeName).filter(null !=).orNull
        //      "sleeper" -> sleeperLatestOpt.map(s => sleeperData(s)).orNull,
        //      "sleepertime" -> (sleeperLatestOpt match {
        //        case Some(sd) => sd.time.map(_.getTime).getOrElse(-1L)
        //        case None => -2L
        //      })
      )
    )
  }

  @volatile
  private var lastSleeperSendCache: mutable.Map[String, SleeperData] = new TrieMap[String, SleeperData]()

  def onNewSleeperData(de: DataEvent[SleeperData]) {
    lastSleeperSendCache(de.data.uid) = de.data
  }

  private[this] def sleeperToMap(e: SleeperData): Map[String, Any] = {

    Map(
      "eventType" -> "objectsUpdate",
      "aggregate" -> "object",
      "action" -> "update",
      "itemId" -> e.uid,
      "data" -> Map(
              "sleeper" -> allOS.sleeperData(e),
              "sleepertime" -> e.time.map(_.getTime).getOrElse(-1L)
              )
      )
  }

}
