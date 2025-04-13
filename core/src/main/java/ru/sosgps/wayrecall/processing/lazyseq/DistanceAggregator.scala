package ru.sosgps.wayrecall.processing.lazyseq

import ru.sosgps.wayrecall.core.{GPSUtils, DistanceUtils, GPSData}
import ru.sosgps.wayrecall.data.PackDataConverter

/**
 * Created by nickl on 02.04.15.
 */
class DistanceAggregator(var sum: Double, val sequence: GpsIntervalSplitter) extends Aggregator[Double] {

  def good(g: GPSData) = g.goodlonlat && g.speed > 0


  protected def kmsIn(interval: Interval): Option[Double] = {
    val kmsBetween = DistanceUtils.kmsBetween(interval.start, interval.end)
    val vs = DistanceUtils.virtualSpeedBetween(interval.start, interval.end)
    if (vs < 0 || kmsBetween > 0.2 && vs > 300) {
      warn(s"strange virtual speed $vs in $interval")
      return None
      //throw new IllegalArgumentException(s"strange virtual speed $vs in $interval")
    }

    Some(kmsBetween)
  }

  override def intinalValue: Double = 0.0

  override def numeric: Numeric[Double] = implicitly[Numeric[Double]]
}

/**
 * @see [[ ru.sosgps.wayrecall.packreceiver.MotoAccumulator ]]
 */
class MotohoursAggregator(var sum: Long, val sequence: GpsIntervalSplitter, cnv: PackDataConverter) extends Aggregator[Long] {

  def good(g: GPSData) = g.goodlonlat /* && g.speed > 0   */

  protected def canApply(prev: GPSData, cur: GPSData): Boolean = {
    detectIgnition(cur).exists(_ > 0) && detectIgnition(prev).getOrElse(0) > 0
  }

  private def detectIgnition(cur: GPSData): Option[Int] = {
    val data = new GPSData(cur.uid, cur.imei, cur.time)
    data.data = cur.data
    convertData(data)
    GPSUtils.detectIgnitionInt(data)
  }

  protected def convertData(data: GPSData): Unit = {
    cnv.convertData(data)
  }

  protected def distance(prev: GPSData, cur: GPSData): Option[Long] = {
    if (canApply(prev, cur)) Some(cur.time.getTime - prev.time.getTime) else None
  }

  protected def kmsIn(interval: Interval): Option[Long] = {
    distance(interval.start, interval.end)
  }

  override def intinalValue: Long = 0L

  override def numeric: Numeric[Long] = implicitly[Numeric[Long]]
}

trait Aggregator[T] extends grizzled.slf4j.Logging {

  def intinalValue: T

  var sum: T

  def sequence: GpsIntervalSplitter

  def good(g: GPSData): Boolean

  protected implicit def numeric: Numeric[T]

  def add(g: GPSData): SplitResult = synchronized {
    val split1 = correctByInsertionTime(g, sequence.addAndGetSplit(g))
    //val split1 = sequence.splitVirtually(g)
    val r = split1 match {
      case split: Split if Stream(g, split.prev.start).exists(good) =>
        trace(s"split=$split")
        val prevDist = kmsIn(split.prev)
        //val newDist = kmsIn(split.first).flatMap(f => kmsIn(split.second).map(s => f + s))
        val newDist = for (f <- kmsIn(split.first); s <- kmsIn(split.second)) yield numeric.plus(f, s)
        for (prevDist <- prevDist; newDist <- newDist) {
          trace(s"prevDist=$prevDist newDist=$newDist")
          assert(numeric.toDouble(prevDist) < numeric.toDouble(newDist) + 0.00001, s"must be $prevDist < $newDist")
          //assert(prevDist < newDist + 0.00001, s"must be $prevDist < $newDist")
          updateSumSafe(numeric.plus(numeric.minus(sum, prevDist), newDist))
          trace(s"sum=$sum")
        }
        split1
      case n: NewInterval if g.goodlonlat && Stream(g, n.interval.start).exists(good) =>
        trace(s"newInterval=$n")
        updateSumSafe(numeric.plus(sum, kmsIn(n.interval).getOrElse(intinalValue)))
        trace(s"sum=$sum")
        split1
      case _ =>
        trace(s"NoInterval=$split1")
        trace(s"sum=$sum")
        NoInterval
    }

    /* } else
       NoInterval  */
    //if(good(g)) {
    //sequence.add(g)
    r
    //    }
    //    else
    //      NoInterval
  }

  protected def kmsIn(interval: Interval): Option[T]

  def updateSumSafe(newSum: T): Unit = {
    if (!java.lang.Double.isNaN(numeric.toDouble(newSum)) && !java.lang.Double.isInfinite(numeric.toDouble(newSum))) {
      sum = newSum
    }
  }

  //protected def kmsIn(interval: Interval): Double

  def correctByInsertionTime(g: GPSData, split2: SplitResult): SplitResult = {
    // split2
    trace(s"beforecorrection:=$split2")
    split2 match {
      case split: Split if split.second.end.insertTime.getTime > g.insertTime.getTime => NewInterval(split.first, split.dublicate)
      case other => other
    }
  }


}