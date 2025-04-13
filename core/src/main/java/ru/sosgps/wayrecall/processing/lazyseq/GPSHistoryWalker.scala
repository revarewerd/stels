package ru.sosgps.wayrecall.processing.lazyseq

import java.util
import java.util.{Collections, Date}

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.PackagesStore

import scala.None

/**
 * Created by nickl on 02.04.15.
 */
trait GPSHistoryWalker {

  def prev(date: Date): Option[GPSData]

  def at(date: Date): Option[GPSData]

  def atOrNext(date: Date): Option[GPSData]

  def next(date: Date): Option[GPSData]

  def put(data: GPSData): Unit

}

class TreeGPSHistoryWalker extends GPSHistoryWalker {

  def this(data: TraversableOnce[GPSData]) = {
    this()
    data.foreach(put)
  }

  val tree = Collections.synchronizedNavigableMap(new util.TreeMap[Date, GPSData]())

  override def prev(date: Date) = Option(tree.lowerEntry(date)).map(_.getValue)

  override def atOrNext(date: Date) = Option(tree.ceilingEntry(date)).map(_.getValue)

  override def next(date: Date) = Option(tree.higherEntry(date)).map(_.getValue)

  override def put(data: GPSData): Unit = tree.put(data.time, data)

  override def at(date: Date) = Option(tree.get(date))
}

class FiltredGPSWalker(src: GPSHistoryWalker, acceptor: (GPSData) => Boolean) extends GPSHistoryWalker with grizzled.slf4j.Logging{
  override def prev(date: Date): Option[GPSData] = filtred(src.prev, prev, date)

  private def filtred(loader: (Date) => Option[GPSData], rec: (Date) => Option[GPSData],  date: Date): Option[GPSData] = {
    loader(date) match {
      case Some(gps) if acceptor(gps) => Some(gps)
      case Some(gps) => loader(date)
      case None => None
    }
  }

  override def next(date: Date): Option[GPSData] = filtred(src.next, next, date)

  override def atOrNext(date: Date): Option[GPSData] = filtred(src.atOrNext, atOrNext, date)

  override def put(data: GPSData): Unit = src.put(data)

  override def at(date: Date): Option[GPSData] = src.at(date)
}

class PackageStoreGPSWalker(uid: String, packageStore: PackagesStore, putter: (GPSData) => Unit) extends GPSHistoryWalker {
  override def prev(date: Date): Option[GPSData] = packageStore.prev(uid, date)

  override def next(date: Date): Option[GPSData] = packageStore.next(uid, date)

  override def atOrNext(date: Date): Option[GPSData] = at(date).orElse(next(date))

  override def put(data: GPSData): Unit = putter(data)

  override def at(date: Date): Option[GPSData] = packageStore.at(uid, date)
}

class DelegatingGPSHistoryWalker(delegate: => GPSHistoryWalker) extends GPSHistoryWalker{
  override def prev(date: Date): Option[GPSData] = delegate.prev(date)

  override def next(date: Date): Option[GPSData] = delegate.next(date)

  override def atOrNext(date: Date): Option[GPSData] = delegate.atOrNext(date)

  override def put(data: GPSData): Unit = delegate.put(data)

  override def at(date: Date): Option[GPSData] = delegate.at(date)
}
