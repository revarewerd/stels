package ru.sosgps.wayrecall.utils.stubs

import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiFunction

import com.google.common.cache.CacheBuilder
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data._

import ru.sosgps.wayrecall.utils.{funcLoadingCache, LimitsSortsFilters}

import scala.collection.generic.Growable
import scala.concurrent.Future

/**
 * Created by nickl on 08.04.15.
 */
class InMemoryPacketsWriter extends DBPacketsWriter with ClusteredPacketsReader with grizzled.slf4j.Logging {

  //val latestPerImei = new ConcurrentHashMap[String, Entry]()

  val totalWritten = new AtomicInteger(0)

  //  implicit def j8l[T, U, R](f: (T, U) => R): BiFunction[T, U, R] = new BiFunction[T, U, R]() {
  //    override def apply(t: T, u: U): R = f(t, u)
  //  }

  val stores = CacheBuilder.newBuilder().buildWithFunction((uid: String) => new DeviceUnawareInMemoryPackStore())

  @throws(classOf[IllegalImeiException])
  override def addToDb(gpsdata: GPSData): Boolean = {

    gpsdata.uid = uidByImei(gpsdata.imei).get
    require(gpsdata.uid != null)
    if (!gpsdata.unchained()) {
      debug("addToDb " + gpsdata)
      stores(gpsdata.uid).put(gpsdata)
      //latestPerImei.compute(gpsdata.imei, j8l((imei, entry) => Option(entry).getOrElse(new Entry).put(gpsdata)))
      totalWritten.incrementAndGet()
    }
    //latestPerImei.put(gpsdata.imei, gpsdata);
    true
  }

  override def updateGeogata(gpsdata: GPSData, placeName: String): Unit = {}

  //  class Entry {
  //    var last: GPSData = null
  //    var count = new AtomicInteger(0)
  //
  //    def put(gps: GPSData): Entry = {
  //      last = gps
  //      count.incrementAndGet()
  //      this
  //    }
  //
  //    override def toString = s"Entry($count, $last)"
  //  }

  override def getStoreByImei(imei: String): PackagesStore = stores(uidByImei(imei).get)

  override def uidByImei(imei: String): Option[String] = Some(imei)

  //  object latestPackagesStore extends PackagesStore {
  //    override def getLatestFor(uids: Iterable[String]): Iterable[GPSData] = uids.flatMap(imei => latest(imei))
  //
  //    override def next(uid: String, date: Date): Option[GPSData] = latest(uid).filter(_.time.getTime > date.getTime)
  //
  //    override def prev(uid: String, date: Date): Option[GPSData] = latest(uid).filter(_.time.getTime < date.getTime)
  //
  //    override def getPositionsCount(uid: String, from: Date, cur: Date): Int = ???
  //
  //    override def getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters): MovementHistory =
  //      latest(uid).filter(gps => gps.time.getTime >= from.getTime && gps.time.getTime <= to.getTime) match {
  //        case Some(gps) => new MovementHistory(1, 0, 1, Iterator(gps))
  //        case None => new MovementHistory(0, 0, 0, Iterator.empty)
  //      }
  //
  //    override def refresh(uid: String): Unit = ???
  //
  //    override def at(uid: String, date: Date): Option[GPSData] = latest(uid).filter(_.time.getTime == date.getTime)
  //
  //    override def removePositionData(uid: String, from: Date, to: Date): Unit = ???
  //
  //    override def getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double): Option[GPSData] = ???
  //  }

  //  private def latest(uid: String): Option[GPSData] = {
  //    stores(uid).getLatestFor(Iterable(uid)).headOption
  //    //Option(latestPerImei.get(uid)).map(_.last)
  //  }
}

class CollectionPacketWriter[T <: Growable[GPSData]](val collection: T) extends DBPacketsWriter {
  @throws(classOf[IllegalImeiException])
  override def addToDb(gpsdata: GPSData): Boolean = synchronized {
    if (!gpsdata.unchained())
      collection += gpsdata;
    true
  }

  override def updateGeogata(gpsdata: GPSData, placeName: String): Unit = {}
}