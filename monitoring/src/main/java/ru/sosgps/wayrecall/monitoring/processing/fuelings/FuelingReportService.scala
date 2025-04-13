package ru.sosgps.wayrecall.monitoring.processing.fuelings

import java.util.Date

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.monitoring
import ru.sosgps.wayrecall.monitoring.processing
import ru.sosgps.wayrecall.monitoring.processing.parkings.MovingStatesExtractor
import ru.sosgps.wayrecall.monitoring.processing.urban.PointInUrbanArea
import ru.sosgps.wayrecall.regeocoding.DirectNominatimRegeocoder

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import ru.sosgps.wayrecall.utils.tryNumerics

/**
 * Created by nickl on 03.03.15.
 */
class FuelingReportService extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var packStore: PackagesStore = _

  @Autowired
  var regeo: DirectNominatimRegeocoder = _

  def fuelSensorsNames(uid: String) = {
    mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("sensors" -> 1)).
      flatMap(_.getAs[MongoDBList]("sensors")).getOrElse(new MongoDBList()).toSeq.map(_.asInstanceOf[BasicDBObject]).
      filter(s => {Set("sFuelL").contains(s.getAsOrElse[String]("type", ""))}).map(_.getAsOrElse[String]("paramName", ""))
  }

  def getFuelings(uid: String, from: Date, to: Date): FuelingReportData = {
    val history = packStore.getHistoryFor(uid, from, to).iterator

    getFuelings(uid, history.toSeq)
  }

  def getFuelings(uid: String, history: Seq[GPSData]): FuelingReportData = {


    val fuelSensors: Seq[String] = fuelSensorsNames(uid)
    debug("Fuel sensors = " + fuelSensors)
    val settings: ObjectFuelSettings = new ObjectFuelSettings(uid, mdbm)

    val converters: Seq[FuelConversions] = fuelSensors.map(s => {
      new FuelConversions(uid, s, mdbm)
    }
    )

    new FuelingReportData(history.filter(_.goodlonlat), settings, converters, regeo)
  }

}

class FuelDataRec(val point: GPSData, val value: Int, val rawValue: Int,
                  val src: Option[FuelDataRec] = None) {
  override def toString = s"FuelDataRec(pointTime = " + point.time + ", fuelValue = " + value + ", fuelRawValue = " + rawValue + ")"
}

abstract sealed class FuelBehavior(val firstPoint: FuelDataRec, val lastPoint: FuelDataRec, val volume: Double) {
  var approved = false
  var midPoint = firstPoint.point
  //def approved: Boolean
}

class Refueling(start: FuelDataRec, lastPoint: FuelDataRec, firstVolume: Double) extends  FuelBehavior(start, lastPoint, firstVolume) {
}

class FuelDraining(start: FuelDataRec, lastPoint: FuelDataRec, firstVolume: Double) extends  FuelBehavior(start, lastPoint, firstVolume) {

}


sealed trait FuelStandard
case object IdlingStandard extends  FuelStandard
case object NoConsumption extends FuelStandard
case object UrbanStandard extends  FuelStandard
case object ExUrbanStandard extends  FuelStandard

sealed trait FuelNorm
case object SummerNorm extends  FuelNorm
case object WinterNorm extends  FuelNorm
case object StableNorm extends  FuelNorm




class FuelingReportData(history: Seq[GPSData],
                        settings: ObjectFuelSettings,
                        converters: Seq[FuelConversions],
                        regeo: DirectNominatimRegeocoder
                         ) extends grizzled.slf4j.Logging {
  val fuelMaxValue = if (settings.fuelVolume > 0) {
    settings.fuelVolume
  } else if (converters.nonEmpty) {
    converters.map(c => {
      c.maxFuelValue
    }
    ).max
  } else {
    100
  }

  val fuelDataRecs: Seq[FuelDataRec] = {
    var prevVals: Option[(Double, Double)] = None
    history.flatMap(point => {
      val (realVal, rawVal) = converters.map(c => {
        c.getPointValues(point)
      }
      ).filter(_._1.toInt > 0).headOption.orElse(prevVals).getOrElse((0.0, 0.0))
      if (realVal.toInt > 0 && rawVal > 0)
        prevVals = Some((realVal, rawVal))
      //if (realVal.toInt > 0 && rawVal > 0)
      //debug(s"fuelDataRecs:$point\n$realVal, $rawVal prev = $prevVals ")
      Some(new FuelDataRec(point, realVal.toInt, rawVal.toInt))
      //    else
      //      None
    })
  }

  def slide(seq: Iterable[FuelDataRec], windowLength: Int) = {
    def result(prefix: Iterable[FuelDataRec], current: Iterable[FuelDataRec], wl: Int, dec: Boolean = false): Stream[Seq[FuelDataRec]] = {
      if(current.isEmpty)
        return Stream.empty
      val takeBefore = wl / 2
      val toTake = if(wl % 2 != 0) wl / 2 + 1 else wl / 2
      val after = current.take(toTake).toSeq
      if(after.size < toTake) {
        (prefix.slice(1, takeBefore) ++ after).toArray #:: result(prefix.drop(2),current.tail, wl - 2, true)
      }
      else if(dec) {
        (prefix.take(takeBefore) ++ after).toArray #:: result(prefix.drop(2), current.tail, wl - 2, true)
      }
      else if(wl < windowLength) {
        (prefix.take(takeBefore) ++ after).toArray #:: result(prefix, current.tail, math.min(windowLength,wl + 2))
      }
      else
        (prefix.take(takeBefore) ++ after).toArray #:: result(prefix.drop(1), current.tail, wl)

    }

    result(seq, seq, 1)
  }

  val smoothedPoints = if (settings.isNoFiltering || fuelDataRecs.isEmpty) {
    fuelDataRecs.map(selfParent)
  } else {


    val len = if(settings.filtering == 0) 3 else settings.filtering * 5

    slide(fuelDataRecs, len).map(rng => {
      val middle = rng.size / 2
      val mid = rng.apply(middle)
      def avr(data: Seq[Int]) = data.sorted.apply(middle)
      new FuelDataRec(mid.point, avr(rng.map(_.value)), avr(rng.map(_.rawValue)), Some(mid))
    })

  }


  lazy val isUrban = if(settings.urbanByCoordinates) {
    val pointInUrbanArea = new PointInUrbanArea(regeo)
    (gps: GPSData) => pointInUrbanArea.isUrban(gps.lon,gps.lat)
  }
  else
    (gps: GPSData) => gps.speed < settings.maxUrbanSpeed


  //TODO settings
  def isWinter(date: Date) = false

  def fuelNormFor(point: GPSData): FuelNorm = {
    if(point.speed <= 1)
      StableNorm
    else if(isWinter(point.time))
      WinterNorm
    else
      SummerNorm
  }

  case class ConsumptionStats(total: Double, avgByDistance: Double)

  def consumptionByNorms(seq: Iterable[FuelDataRec]): ConsumptionStats = {
    // Todo императивно
    if (seq.isEmpty || seq.tail.isEmpty)
      ConsumptionStats(0.0, 0.0)
    else {
      var total = 0.0
      var dist = 0.0
      for (pair <- seq.sliding(2)) {
        val f0 = pair.head
        val f1 = pair.tail.head
        val norm = fuelNormFor(f1.point)
        norm match {
          case StableNorm =>
          case WinterNorm => {
            total +=  expectedDelta100km(f0.point, f1.point, settings.winternorm)
            dist += processing.distanceBetweenPackets(f0.point, f1.point)
          }
          case SummerNorm => {
            total += expectedDelta100km(f0.point, f1.point, settings.summernorm)
            dist += processing.distanceBetweenPackets(f0.point, f1.point)
          }
        }
      }
      ConsumptionStats(total, total * 100 / dist )
    }
  }


  def consumptionByEstimation(seq: Iterable[FuelDataRec]): ConsumptionStats = {
    if(seq.isEmpty || seq.tail.isEmpty)
      ConsumptionStats(0.0, 0.0)
    else {
      var idlingTotal = 0.0
      var totalWithDist = 0.0
      var dist = 0.0
      for (pair <- seq.sliding(2)) {
        val f0 = pair.head
        val f1 = pair.tail.head
        val estimation = fuelStandardFor(f1.point)
        estimation match {
          case NoConsumption =>
          case IdlingStandard => idlingTotal += expectedDeltaHour(f0.point, f1.point, settings.idling)
          case UrbanStandard => {
            totalWithDist += expectedDelta100km(f0.point, f1.point, settings.urban)
            dist += processing.distanceBetweenPackets(f0.point, f1.point)
          }
          case ExUrbanStandard => {
            totalWithDist += expectedDelta100km(f0.point, f1.point, settings.extraurban)
            dist += processing.distanceBetweenPackets(f0.point, f1.point)
          }
        }
      }
      ConsumptionStats(idlingTotal + totalWithDist, (idlingTotal + totalWithDist) * 100 / dist)
    }
  }

  def expectedDelta100km(f0: GPSData, f1: GPSData, standard: Double) = {
    standard * processing.distanceBetweenPackets(f0,f1) / 100.0
  }

  def expectedDeltaHour(f0: GPSData, f1: GPSData, standard: Double) = {
    standard * (f1.time.getTime - f0.time.getTime) / 3600 / 1000
  }


  def fuelStandardFor(point: GPSData): FuelStandard = {
    // Начать с холостого хода (т.к. он не требует определения города/загорода). Использовать детектор поездок
    val idling = GPSUtils.detectIgnitionInt(point).exists(_ > 0) && point.speed <= 1
    if(idling)
      IdlingStandard
    else if(point.speed > 1)
      if(isUrban(point))
        UrbanStandard
      else
        ExUrbanStandard
    else
      NoConsumption
  }

  def removeIntervals(seq: Iterable[FuelDataRec], intervals: Iterable[(Date,Date)]): Stream[Iterable[FuelDataRec]] = {
      if(seq.isEmpty)
        Stream.empty
      else if(intervals.isEmpty)
        seq #:: Stream.empty
      else {
        val (from, to) = intervals.head
        def dateLE(rec: FuelDataRec, date: Date) = rec.point.time.compareTo(date) <= 0

        seq.takeWhile(_.point.time.before(from)) #:: removeIntervals(seq.dropWhile(dateLE(_, to)), intervals.tail)
      }
  }

  def  expectedFuelLevelByNorms(seq: Iterable[FuelDataRec], prevValue: Double, f0: FuelDataRec): Stream[(Date, Double)] = {
    if(seq.isEmpty)
      Stream.empty
    else {
      val f1 = seq.head

      val standard = fuelNormFor(f1.point)

      val newValue = standard match {
        case StableNorm => prevValue
        case SummerNorm => prevValue - expectedDelta100km(f0.point, f1.point, settings.summernorm)
        case WinterNorm => prevValue - expectedDelta100km(f0.point, f1.point, settings.winternorm)
      }

      (f1.point.time, newValue) #:: expectedFuelLevelByNorms(seq.tail, newValue, f1)
    }
  }

  def expectedFuelLevelStream(seq: Iterable[FuelDataRec], prevValue: Double, f0: FuelDataRec): Stream[(Date, Double)] = {
    if(seq.isEmpty)
      Stream.empty
    else {
      val f1 = seq.head

      val standard = fuelStandardFor(f1.point)

      val newValue = standard match {
        case IdlingStandard =>  prevValue - expectedDeltaHour(f0.point,f1.point, settings.idling)
        case NoConsumption => prevValue
        case UrbanStandard => prevValue - expectedDelta100km(f0.point, f1.point, settings.urban)
        case ExUrbanStandard => prevValue - expectedDelta100km(f0.point, f1.point, settings.extraurban)
      }

      (f1.point.time, newValue) #:: expectedFuelLevelStream(seq.tail, newValue, f1)
    }
  }


  def expectedFuelLevel(useCalcStandards: Boolean) = {
    val stream = if(useCalcStandards) expectedFuelLevelStream _ else expectedFuelLevelByNorms _
    if(smoothedPoints.isEmpty)
      Seq.empty
    else {
      if(settings.useRefuelingsInExpectedFuelLevel)
      {
        var refuelings = newStates.collect{case f: Refueling => f}
        removeIntervals(smoothedPoints, refuelings.map(rf => (rf.firstPoint.point.time,
          rf.lastPoint.point.time)))
          .flatMap{seq => {
            val refuelingOpt = refuelings.headOption
            val level = if(seq.isEmpty)
              Iterable.empty
            else
              (seq.head.point.time, seq.head.value.toDouble) #:: stream(seq.tail, seq.head.value, seq.head)

            if(refuelingOpt.isDefined) {
              val refueling = refuelingOpt.get
              refuelings = refuelings.tail
              level ++ Seq((refueling.firstPoint.point.time, refueling.firstPoint.value.toDouble),
                (refueling.lastPoint.point.time, refueling.lastPoint.value.toDouble))
            }
            else
              level
          }}
      }
      else
        (smoothedPoints.head.point.time, smoothedPoints.head.value.toDouble) #:: stream(smoothedPoints.tail, smoothedPoints.head.value, smoothedPoints.head)
    }

  }

  def standardToNumber(standard: FuelStandard) = {
    val off = 0
    val idling = 1
    val urban = 2
    val exurban = 3
    val value = standard match {
      case NoConsumption => off
      case IdlingStandard => idling
      case UrbanStandard => urban
      case ExUrbanStandard => exurban
    }
    value
  }


  def pointsAndNorms(points: Iterable[FuelDataRec]) = points.map(rec => (rec, fuelNormFor(rec.point)))


  def pointsAndStandards(points: Iterable[FuelDataRec]) = points.map(rec => (rec, fuelStandardFor(rec.point)))


  def isNotMoving(point: GPSData) = {
    val maxMovingSpeed = 1 // Todo совместить с перемещениями и стоянками, детектор поездок
    point.speed <= maxMovingSpeed
  }

  def expectedDiff(f0: FuelDataRec, f1: FuelDataRec, standard: FuelStandard) = {
    standard match {
      case NoConsumption => 0.0
      case IdlingStandard => settings.idling * ((f1.point.time.getTime - f0.point.time.getTime) / 1000.0 / 3600)
      case UrbanStandard => expectedDelta100km(f0.point, f1.point, settings.urban)
      case ExUrbanStandard => expectedDelta100km(f0.point, f1.point, settings.extraurban)
    }
  }

  def expectedDiff(f0: FuelDataRec, f1: FuelDataRec, norm: FuelNorm) = {
    norm match {
      case StableNorm => 0.0
      case SummerNorm => expectedDelta100km(f0.point, f1.point, settings.summernorm)
      case WinterNorm => expectedDelta100km(f0.point, f1.point, settings.winternorm)
    }
  }

  type PointPairs = Iterable[Iterable[FuelDataRec]]


  case class FuelInterval(first: FuelDataRec, upperBound: Option[FuelDataRec], condition: Boolean) {
    def lengthMs = upperBound.map(ub => ub.point.time.getTime - first.point.time.getTime)
  }

  def intervalSplit(seq: Iterable[FuelDataRec],condition: FuelDataRec => Boolean): Stream[FuelInterval] = {
    var s = seq
    if(s.nonEmpty) {
      val f = s.head
      val conditionResult = condition(f)
      s = s.tail
      while(s.nonEmpty && condition(s.head) == conditionResult) { s = s.tail}
      return FuelInterval(f,  s.headOption, conditionResult) #:: intervalSplit(s, condition)
    }
    Stream.empty
  }

  def filterMovementStarts(recs: Iterable[FuelDataRec]) = {
    def filter(seq: Iterable[FuelDataRec], prev: GPSData): Stream[FuelDataRec] = {
      if(seq.isEmpty)
        Stream.empty
      else if(isNotMoving(prev) && !isNotMoving(seq.head.point)) {
        var next = seq
        var prevHead = prev
        var base = prev

        while(next.nonEmpty && next.head.point.time.getTime - base.time.getTime <= settings.ignoreMessagesAfterMoving * 1000) {
          if(isNotMoving(prevHead) && !isNotMoving(next.head.point))
            base  = prevHead
          prevHead = next.head.point
          next = next.tail
        }
        if(next.nonEmpty)
          next.head #:: filter(next.tail,next.head.point)
        else
          Stream.empty
      }
      else
        seq.head #:: filter(seq.tail,seq.head.point)
    }
    if(settings.ignoreMessagesAfterMoving != 0 && recs.nonEmpty)
      filter(recs, recs.head.point)
    else
      recs
  }

  lazy val newStates: Seq[FuelBehavior] = {
    val fuelings = new ArrayBuffer[Refueling]
    toMonotonicSeq(filterMovementStarts(smoothedPoints),fuelings).foreach(x => {})
    fuelings
  }

  def isUp(f0: FuelDataRec, f1: FuelDataRec) = {
    f1.value > f0.value
  }

  def isDown(f0: FuelDataRec, f1: FuelDataRec) = {
    f1.value < f0.value
  }

  def isStable(f0: FuelDataRec, f1: FuelDataRec) = {
    f1.value == f0.value
  }

  trait FuelChange {
    def volume: Int
    def f0: FuelDataRec
    def f1: FuelDataRec
    def rest: PointPairs
  }

  case class FuelUp(f0: FuelDataRec, f1: FuelDataRec, rest: PointPairs) extends FuelChange {
    override def volume: Int = f1.value - f0.value
  }

  case class FuelDown(f0: FuelDataRec, f1: FuelDataRec, rest: PointPairs) extends FuelChange {
    override def volume: Int = f0.value - f1.value
  }

  def unpack(pair : Iterable[FuelDataRec]) = {
    (pair.head, pair.tail.head)
  }

  def isInInterval(p0: GPSData, p1: GPSData, secs: Long) = {
    p1.time.getTime - p0.time.getTime <= secs * 1000
  }

  def tails[T](iterable: Iterable[T]): Stream[Iterable[T]] = {
    if(iterable.isEmpty)
      Stream.empty
    else
      iterable #:: tails(iterable.tail)
  }

  def makeRefueling(iterable: Seq[FuelUp]) = {
    val result = iterable.foldLeft(
      new Refueling(iterable.head.f0, iterable.head.f1, iterable.head.volume)
    ) ((res, mf) => {
      new Refueling(res.firstPoint, mf.f1, mf.f1.value - res.firstPoint.value)
    })
    result.midPoint = iterable.apply(iterable.size / 2).f0.point
    result
  }


  def toMonotonicSeq(src: Iterable[FuelDataRec], fuelings: ArrayBuffer[Refueling] = null) = {
    val extensionTime = 600
    val approvalTime = 600

    def sumFueling(changes: Iterable[FuelChange]) = {
      changes.last.f1.value - changes.head.f0.value
    }

    def monotonicPoints(points: PointPairs): Stream[FuelDataRec] = {
     // debug("points type is " + points.getClass)
      if(points.isEmpty)
        return Stream.empty
      val (a,b) = unpack(points.head)
      if(isUp(a,b) || isDown(a,b)) {
        val changes = new ArrayBuffer[FuelChange]
        def recent = changes.last
        var cont = points.tail
        var extended = false
        if(isUp(a,b)) {
          changes.append(FuelUp(a,b, points.tail)) // Внести сюда стабильные интервалы?

          while(!extended) {
            if(!isNotMoving(recent.f1.point))
              extended = true
            val extensionCandidates = tails(cont).map(s => {
              val (f0,f1) = unpack(s.head)
              (f0, f1, s.tail)
            }).takeWhile{case (f0,f1,_) =>
              isInInterval(recent.f1.point, f1.point, extensionTime)
            }.filter{case (f0,f1,_) => isUp(f0,f1) && f1.value > recent.f1.value}
            if(extensionCandidates.isEmpty)
              extended = true
            else {
              val newUp = extensionCandidates.minBy{case(f0,f1,_) => f1.value - recent.f1.value}
              changes.append(FuelUp(newUp._1, newUp._2, newUp._3))
              cont = recent.rest
            }
          }

          // Discard anything less than setting.minFueling

          while(changes.nonEmpty && sumFueling(changes) >= settings.minFueling) {
            val rest = recent.rest.map(_.tail.head)
//            var base = last.f1


            def stabilises(value: Int, up: FuelUp) = {
              val volume = up.f1.value - up.f0.value
              if(volume > 5)
                value >= up.f0.value + 2 * volume / 3
              else
                value > up.f0.value
            }

            val sorted = rest.takeWhile(f => isInInterval(recent.f1.point, f.point, approvalTime)).toArray.sortBy(_.value)
            val approved = sorted.isEmpty || stabilises(sorted.apply(sorted.length / 2).value, recent.asInstanceOf[FuelUp])
            if(approved) {
              if(fuelings != null)
                fuelings.append(makeRefueling(changes.map(_.asInstanceOf[FuelUp])))
              return changes.map(_.f1).toStream ++ monotonicPoints(recent.rest)
            }

            changes.remove(changes.length - 1)
          }
          if(points.tail.nonEmpty) {
            val (_,c) = unpack(points.tail.head)
            monotonicPoints(Stream(Iterable(a,c)) ++ points.tail.tail)
          }
          else Stream.empty
        }
        else {
          changes.append(FuelDown(a,b, points.tail))

          while(!extended) {
            val extensionCandidates = tails(cont).map(s => {
              val (f0,f1) = unpack(s.head)
              (f0, f1, s.tail)
            }).takeWhile{case (f0,f1,_) =>
              isInInterval(recent.f1.point, f1.point, extensionTime)
            }.filter{case (f0,f1,_) => isDown(f0,f1) && f1.value < recent.f1.value}
            if(extensionCandidates.isEmpty)
              extended = true
            else {
              val newDown = extensionCandidates.minBy{case(f0,f1,_) => recent.f1.value - f1.value}
              changes.append(FuelDown(newDown._1, newDown._2, newDown._3))
              cont = recent.rest
            }
          }

          while(changes.nonEmpty) {
            val rest = recent.rest.map(_.tail.head)
            //            var base = last.f1


            val sorted = rest.takeWhile(f => isInInterval(recent.f1.point, f.point, approvalTime)).toArray.sortBy(_.value)
            val approved = sorted.isEmpty || sorted.apply(sorted.length / 2).value <= recent.f1.value
            if (approved)
              return changes.map(_.f1).toStream ++ monotonicPoints(recent.rest)
            changes.remove(changes.length - 1)
          }
          if(points.tail.nonEmpty) {
            val (_,c) = unpack(points.tail.head)
            monotonicPoints(Stream(Iterable(a,c)) ++ points.tail.tail)
          }
          else Stream.empty
        }
      }
      else {
         b #:: monotonicPoints(points.tail)
      }
    }

    if(src.isEmpty || src.tail.isEmpty)
      src
    else
      src.head #:: monotonicPoints(src.sliding(2).toIterable)
  }



  private def selfParent(r: FuelDataRec): FuelDataRec = new FuelDataRec(r.point, r.value, r.rawValue, Some(r))

  def states: Seq[FuelState] = {
    Seq.empty
  }

}



@deprecated("use FuelBehavior")
abstract sealed class FuelState(val startVal: Int = 0, var approved: Boolean = false) {

  def isFueling: Boolean

  var pointsCount = 1

  var volume = 0

  var volumeRaw = 0

  var volumeCheck = 0

  var leftEdge: Double = 0

  var rightEdge: Double = 0

  protected[this] var _last: GPSData = startPoint

  def startPoint: GPSData

  def lastPoint = _last

  def lastTime = _last.time

  def startTime: Date = startPoint.time

  def intervalMills: Long = lastTime.getTime - startTime.getTime

  def add(point: GPSData, curVal: Int, curRawVal: Int) {
    pointsCount = pointsCount + 1
    if (pointsCount > 2) {
      approved = true
    }

    _last = point

    if (startPoint.placeName == null && point.placeName != null) {
      startPoint.placeName = point.placeName
    }
  }

  def add(fRec: FuelDataRec) {
    add(fRec.point, fRec.value, fRec.rawValue)
  }

  def contains(point: GPSData) = {
    require(startPoint.uid == point.uid, "point.uid-s " + startPoint.uid + " and " + point.uid + " does not match")
    val time = point.time.getTime
    time >= startTime.getTime && time <= lastTime.getTime
  }

  protected[this] def pointStr(gps: GPSData) = gps.time + " (" + gps.lon + "," + gps.lat + ")"

  override def toString(): String = this.getClass.getName.split("\\.").last + "(points=" + pointsCount + " start=" + pointStr(startPoint) + " end=" + pointStr(lastPoint) + /*" interval=" + (intervalMills.toDouble / (1000 * 60)) +*/ " volume=" + volume + " volumeRaw=" + volumeRaw + " volumeCheck=" + volumeCheck + " leftEdge=" + leftEdge + " rightEdge=" + rightEdge + ")"


}

@deprecated("use FuelBehavior")
class Fueling(val startPoint: GPSData, override val startVal: Int, val startRawVal: Int) extends FuelState {

  def this(fRec: FuelDataRec) = { this(fRec.point, fRec.value, fRec.rawValue) }

  //  def this(points: GPSData*) = { this(points.head); points.tail.foreach(add) }

  override def add(point: GPSData, curVal: Int, curRawVal: Int) {
    super.add(point, curVal, curRawVal)

    volume = curVal - startVal
    volumeRaw = curRawVal - startRawVal
  }

  //
  //  override def add(fRec: FuelDataRec) {
  //    add(fRec.point, fRec.value, fRec.rawValue)
  //  }

  def isFueling = true
}

@deprecated("use FuelBehavior")
class Draining(val startPoint: GPSData, override val startVal: Int, val startRawVal: Int) extends FuelState {

  def this(fRec: FuelDataRec) = { this(fRec.point, fRec.value, fRec.rawValue) }

  //  def this(points: GPSData*) = { this(points.head); points.tail.foreach(add) }

  override def add(point: GPSData, curVal: Int, curRawVal: Int) {
    super.add(point, curVal, curRawVal)

    volume = startVal - curVal
    volumeRaw = startRawVal - curRawVal
  }

  //
  //  override def add(fRec: FuelDataRec) {
  //    add(fRec.point, fRec.value, fRec.rawValue)
  //  }

  def isFueling = false
}

object Stable extends FuelState {

  def startPoint = new GPSData(null, null, 0, 0, null, 0, 0, 0)

  def isFueling = ???
}

