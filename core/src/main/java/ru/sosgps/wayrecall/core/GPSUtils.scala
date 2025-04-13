package ru.sosgps.wayrecall.core

import java.util.Date

import ru.sosgps.wayrecall.utils.{tryNumerics, tryParseInt}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
 * Created by nickl on 17.04.14.
 */
object GPSUtils {

  val ignitionParamNames = Stream("avl_inputs",
    "Digital Input 4", "Digital_Input_4",
    "Digital Input Status 1", "Digital_Input_Status_1", "ignition"
  )

  val canDistanceParams = Set("can_distance", "CANBUS_Distance")

  val canFuelUsedParams = Set("can_fuel_used", "CANBUS_Fuel_used")

  val canParamsSet = canDistanceParams ++ canFuelUsedParams

  def detectCanDistance(g: GPSData) = canDistanceParams.iterator.map(p => g.data.get(p)).find(null !=).map(_.tryLong)

  def detectCanFuelUsed(g: GPSData) = canFuelUsedParams.iterator.map(p => g.data.get(p)).find(null !=).map(_.tryLong)

  def detectIgnition(g: GPSData): Option[String] = {
    ignitionParamNames.map(ipn =>
      Option(g.data.get(ipn))
    ).flatten.headOption.map(_.toString)
  }

  def detectIgnitionInt(g: GPSData): Option[Int] = {
    detectIgnition(g).flatMap(tryParseInt)
  }
  
  def detectPowerExt(g: GPSData): Option[Double] = {
    Stream("pwr_ext", "Power_Supply_Voltage", "External_Power_Voltage", "Ext Power", "power").map(
      par =>  Option(g.data.get(par))
    ).flatten.headOption.flatMap(
      _ match {
        case i: Integer => Some(i / 1000.0)
        case d: java.lang.Double => Some(d)
        case _ => None
      }
    )
  }
  
  def detectCANCoolantTemp(g: GPSData): Option[Int] = {
    Stream("can_coolant_temp", "CANBUS_coolant_temperature").map(
      par => Option(g.data.get(par))
    ).flatten.headOption.flatMap(
      _ match {
        case i: Integer => Some(i - 40)
        case _ => None
      }
    )
  }
  
  def detectCANRPM(g: GPSData): Option[Double] = {
    Stream("can_rpm", "CANBUS_RPM").map(
      par => Option(g.data.get(par))
    ).flatten.headOption.flatMap(
      _ match {
        case i: Integer => Some(i * 0.125)
        case _ => None
      }
    )
  }
  
  def detectCANAccPedal(g: GPSData): Option[Double] = {
    Stream("can_acc_pedal", "CANBUS_AccPedal_position").map(
      par => Option(g.data.get(par))
    ).flatten.headOption.flatMap(
      _ match {
        case i: Integer => Some(i * 0.4)
        case _ => None
      }
    )
  }
  
  def getStateLatency(g: GPSData): Option[Map[String, Any]] = {
    if (g.data.containsKey("isParking") && g.data.containsKey("currentInterval")) {
      Some(
        Map(
          "isParking" -> g.data.get("isParking"),
          "currentInterval" -> g.data.get("currentInterval")
        )
      )
    } else {
      None
    }
  }

  def filterBadLonLat(loaded: Iterator[GPSData]): Iterator[GPSData] = {
    if (loaded.isEmpty)
      Iterator.empty
    else {
      val nonValueMarker: GPSData = null

      val combined: Iterator[GPSData] = Iterator(nonValueMarker) ++ loaded ++ Iterator(nonValueMarker)

      combined.sliding(3).map((trip) => {
        val i = trip.iterator
        val (left, center, right) = (i.next(), i.next(), i.next())
        if (left == nonValueMarker || right == nonValueMarker) {
          center.goodlonlat = center.containsLonLat()
          center
        }
        else if (!center.containsLonLat()) {
          center.goodlonlat = false
          center
        }
        else if (!right.containsLonLat() || !left.containsLonLat()) {
          center.goodlonlat = !(GPSData.manhdistance(right, center) > 1 || GPSData.manhdistance(left, center) > 1)
          center
        }
        else {
          val mleftcenter: Double = GPSData.manhdistance(left, center)
          if (mleftcenter > 1 && (mleftcenter + GPSData.manhdistance(right, center) > GPSData.manhdistance(left, right) * 1000)) {
            center.goodlonlat = false
            //println(center + " " + center.time + " is bad")
          }
          center
        }
      })
    }
  }

  def takeWhileWithTail[T](s: Iterable[T], p: T => Boolean) = {
    val buf = new ArrayBuffer[T]
    var rest = s
    while(rest.nonEmpty && p(rest.head)) {
      buf.append(rest.head)
      rest = rest.tail
    }
    (buf, rest)
  }

  def splitByIntervalsImpl[T,Intr <: TimeInterval](history: Iterable[T], intervals: Iterable[Intr], getTime: T => Date): Stream[Iterable[T]] = {
    if(history.isEmpty || intervals.isEmpty)
      Stream.empty
    else {
      // x > b
      val time = getTime(history.head)
      if(time.after(intervals.head.lastTime))
        splitByIntervalsImpl(history,intervals.tail, getTime)
        // x < a
      else if(time.before(intervals.head.firstTime))
        splitByIntervalsImpl(history.tail, intervals,getTime)
        // !( x < a || x > b) <=> !(x < a) && !(x > b) <=> x >= a && x <= b
      else {
        val (part, rest) = takeWhileWithTail(history,(gps:T) => getTime(gps).before(intervals.head.lastTime) || getTime(gps).equals(intervals.head.lastTime))
        part #:: splitByIntervalsImpl(rest, intervals.tail,getTime)
      }
    }
  }

  def removeIntervalsImpl[T,Intr <: TimeInterval](history: Iterable[T], intervals: Iterable[Intr],getTime: T => Date): Stream[Iterable[T]] = {
    if(history.isEmpty)
      Stream.empty
    else if(intervals.isEmpty) {
      history #:: Stream.empty
    }
    else {
      // x > b
      val time = getTime(history.head)
      if(time.after(intervals.head.lastTime))
        removeIntervalsImpl(history,intervals.tail,getTime)
      // x >= a && x <= b
      else if(time.after(intervals.head.firstTime) || time.equals(intervals.head.firstTime))
        removeIntervalsImpl(history.tail, intervals,getTime)
      //x < a
      else {
        val (part, rest) = takeWhileWithTail(history,(gps:T) => getTime(gps).before(intervals.head.firstTime))
        part #:: removeIntervalsImpl(rest, intervals,getTime)
      }
    }
  }

  def splitByIntervals[Intr <: TimeInterval](history: Iterable[GPSData], intervals: Iterable[Intr]) =
    splitByIntervalsImpl(history, intervals, (gps: GPSData) => gps.time)
  def removeIntervals[Intr <: TimeInterval](history: Iterable[GPSData], intervals: Iterable[Intr]) =
    removeIntervalsImpl(history,intervals,(gps: GPSData) => gps.time)



  def valid(gpsdata: GPSData): Boolean = {
    gpsdata.time != null
  }

}

object DistanceUtils extends grizzled.slf4j.Logging {

  // earth radius km
  val R = 6371.0;

  /**
   *
   * @param path points to calculate length
   * @return path length in kilometers
   */
  def sumdistance(path: TraversableOnce[GPSData]): Double = {
    path.toIterator.filter(g => g.goodlonlat && g.speed > 0)
      //.sliding(2).withPartial(false).filter(s => {kmsBetween(s(0), s(1)) > 0.08}).map(_(0))
      .sliding(2).withPartial(false)
      .flatMap(s => {
      val kms = kmsBetween(s(0), s(1))
      if (!(kms >= 0))
        debug("errkms=" + kms + " " + s)
      lazy val d = kms / (s(1).time.getTime - s(0).time.getTime) * 1000 * 60 * 60
      if (!(kms < 0.05) && d > 300 || d.isNaN || d.isInfinite)
        None
      else
        Some(kms)
    }).sum
  }

  def kmsBetween(a: GPSData, b: GPSData) = {
    kmsbetween(a.lon, a.lat, b.lon, b.lat)
  }

  def kmsBetween(lon1: Double, lat1: Double, b: GPSData) = {
    kmsbetween(lon1, lat1, b.lon, b.lat)
  }

  def kmsbetween(lon1: Double, lat1: Double, lon2: Double, lat2: Double): Double = {
    if (lon1 == lon2 && lat1 == lat2)
      0.0
    else
      math.acos(math.sin(lat1.toRadians) * math.sin(lat2.toRadians) +
        math.cos(lat1.toRadians) * math.cos(lat2.toRadians) *
          math.cos((lon2 - lon1).toRadians)) * R
  };

  def virtualSpeedBetween(prev: GPSData, cur: GPSData): Double = {
    val distance = DistanceUtils.kmsBetween(prev, cur)
    val hoursBeetween = (cur.time.getTime - prev.time.getTime).toDouble / (60 * 60 * 1000)
    val virtualSpeed = distance / hoursBeetween
    virtualSpeed
  }

}
