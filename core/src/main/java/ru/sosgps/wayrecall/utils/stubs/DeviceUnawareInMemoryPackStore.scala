package ru.sosgps.wayrecall.utils.stubs

import java.util
import java.util.Date

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.{IllegalImeiException, DBPacketsWriter, MovementHistory, PackagesStore}
import ru.sosgps.wayrecall.processing.lazyseq.TreeGPSHistoryWalker
import ru.sosgps.wayrecall.utils.LimitsSortsFilters

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.mapAsScalaMap

/**
 * Created by nickl on 02.04.15.
 */
class DeviceUnawareInMemoryPackStore(seq: TraversableOnce[GPSData]) extends PackagesStore with DBPacketsWriter with grizzled.slf4j.Logging{

  def this() = this(Seq.empty)

  val innerTree = new TreeGPSHistoryWalker(seq)

  override def getLatestFor(uids: Iterable[String]): Iterable[GPSData] = Option(innerTree.tree.lastEntry()).map(_.getValue)
    .map(last => Iterable(last)).getOrElse(Iterable.empty)

  override def getPositionsCount(uid: String, from: Date, cur: Date): Int = submap(from, cur).size()

  private def submap(from: Date, cur: Date): util.NavigableMap[Date, GPSData] = {
    innerTree.tree.subMap(from, true, cur, true)
  }

  override def getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters): MovementHistory = {
    val submap1 = submap(from, to)
    new MovementHistory(submap1.size(), 0, submap1.size(), submap1.values().toIterator)
  }

  override def refresh(uid: String): Unit = {}

  override def removePositionData(uid: String, from: Date, to: Date): Unit = {
    val toRemove = innerTree.tree.filter(kv => /*kv._2.uid == uid &&*/ kv._2.time.after(from) && kv._2.time.before(to))
    toRemove.foreach(kv => innerTree.tree.remove(kv._1, kv._2) )
  }

  override def getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double): Option[GPSData] = ???

  override def at(uid: String, date: Date): Option[GPSData] = innerTree.at(date)

  override def prev(uid: String, date: Date): Option[GPSData] = {
    debug("loading prev for "+date.getTime+" having "+Option(innerTree.tree.firstEntry()).map(_.getKey.getTime).orNull)
    innerTree.prev(date)
  }

  override def next(uid: String, date: Date): Option[GPSData] = innerTree.next(date)

  def put(gps: GPSData) = innerTree.put(gps)

  @throws(classOf[IllegalImeiException])
  override def addToDb(gpsdata: GPSData): Boolean = {put(gpsdata); true }

  override def updateGeogata(gpsdata: GPSData, placeName: String): Unit = {}
}