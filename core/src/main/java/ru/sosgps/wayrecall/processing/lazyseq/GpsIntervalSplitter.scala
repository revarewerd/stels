package ru.sosgps.wayrecall.processing.lazyseq

import java.util
import java.util.{TimerTask, Timer, Date}
import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import org.joda.time.{DateTimeUtils, DateTime}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.{core, utils}
import ru.sosgps.wayrecall.utils.CollectionUtils.additionalMethods

import scala.ref.WeakReference


class GpsIntervalSplitter(store: GPSHistoryWalker) extends grizzled.slf4j.Logging {

  def add(data: GPSData): Unit = if (store.at(data.time).isEmpty) store.put(data)

  def getInterval(date: Date): Option[Interval] = for (prev <- store.prev(date); next <- store.atOrNext(date))
    yield Interval(prev, next)

  def getNextInterval(date: Date): Option[Interval] = store.next(date).flatMap(g => getInterval(g.time))


  def addAndGetSplit(data: GPSData): SplitResult = {
    val result: SplitResult = splitVirtually(data)
    add(data)
    result
  }

  def splitVirtually(data: GPSData): SplitResult = {
    val splitTime = data.time
    val prevInterval = getInterval(splitTime)
    trace(s"prevInterval=${prevInterval.orNull}")
    //    if (prevInterval.exists(_.end == data))
    //      return NoInterval
    //    //TODO: better return NewInterval(prevInterval.get)

    if (prevInterval.exists(_.end == data)) {
      val nextInterval = getNextInterval(splitTime)
      trace(s"nextInterval=${nextInterval.orNull}")
      return nextInterval match {
        case Some(next) =>
          Split(Interval(prevInterval.get.start, next.end),
            prevInterval.get, next, true)
        case None => NewInterval(prevInterval.get, true)
      }
    }


    val split = for (prevInterval <- prevInterval) yield {
      Split(prevInterval, Interval(prevInterval.start, data), Interval(data, prevInterval.end))
    }

    val result: SplitResult = split
      //.orElse(getInterval(splitTime).map(NewInterval))
      .orElse(store.prev(splitTime).map(prev => Interval(prev, data)).map(NewInterval(_, false)))
      // .orElse(store.next(new Date(splitTime.getTime)).flatMap(next => getInterval(next.time)).map(NewInterval))
      .orElse(store.next(new Date(splitTime.getTime)).map(next => Interval(data, next)).map(NewInterval(_, false)))
      .getOrElse(NoInterval)
    result
  }
}

case class Interval(start: GPSData, end: GPSData) {

  def ends = IndexedSeq(start, end)

  def intervalGPS(gps: GPSData) = s"${gps.time},(${gps.lon},${gps.lat})"

  override def toString = s"Interval(${intervalGPS(start)}-${intervalGPS(end)})"
}

sealed trait SplitResult

case class Split(prev: Interval, first: Interval, second: Interval, dublicate: Boolean = false) extends SplitResult

case class NewInterval(interval: Interval, dublicate: Boolean = false) extends SplitResult

case object NoInterval extends SplitResult






