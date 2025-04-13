package ru.sosgps.wayrecall.monitoring.processing.parkings

import java.util.Date

import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData, GPSInterval, GPSUtils}

import scala.collection.mutable.ArrayBuffer


abstract sealed class MovingState(var approved: Boolean = false) extends GPSInterval {

  def isParking: Boolean

  var pointsCount = 1

  var distance = 0.0

  var maxSpeed = 0.0

  protected[this] var _last: GPSData = first

  override def first: GPSData

  override def last = _last

  override def lastTime = _last.time

  def intervalMills: Long = lastTime.getTime - firstTime.getTime

  override def firstTime: Date = first.time

  def append(point: GPSData): Unit = {
    distance += DistanceUtils.kmsBetween(last, point)
    maxSpeed = math.max(maxSpeed, point.speed)
    _last = point
    pointsCount += 1
  }

  def absorb(state: MovingState): Unit = {
    val cross = lastTime == state.firstTime
    pointsCount += state.pointsCount
    distance += state.distance
    maxSpeed = math.max(maxSpeed, state.maxSpeed)
    if(cross)
      pointsCount -= 1
    _last = state.last
  }

  def add(point: GPSData, canApprove: Boolean = true) = {
    if (canApprove && pointsCount > 2 )
      approved = true
    pointsCount = pointsCount + 1
    _last = point
    if (first.placeName == null && point.placeName != null) {
      first.placeName = point.placeName
    }
  }

  protected[this] def pointStr(gps: GPSData) = gps.time + " (" + gps.lon + "," + gps.lat + ")"

  override def toString(): String = this.getClass.getSimpleName + "(points=" + pointsCount + " start=" + pointStr(first) + " end=" + pointStr(last) + " interval=" + (intervalMills.toDouble / (1000 * 60)) + ")"


}


class Parking(val first: GPSData) extends MovingState {

  var small = false

  def this(points: GPSData*) = { this(points.head); points.tail.foreach(add(_)) }
  //  def this(prevState:MovingState, points: GPSData*) = { this(prevState.startPoint);
  //    add(prevState.lastPoint)
  //    pointsCount += prevState.pointsCount - 2
  //    points.foreach(add(_)) }

  def contains(point: GPSData) = {
    require(first.uid == point.uid, "point.uid-s " + first.uid + " and " + point.uid + " does not match")
    val time = point.time.getTime
    time >= firstTime.getTime && time <= lastTime.getTime
  }

  def isParking = true
}

class Moving(val first: GPSData) extends MovingState {
  def this(points: GPSData*) = { this(points.head); points.tail.foreach(add(_)) }
  def this(prevState:MovingState, points: GPSData*) = { this(prevState.first);
    add(prevState.last)
    pointsCount += prevState.pointsCount - 2
    points.foreach(add(_)) }

  maxSpeed = first.speed

  override def add(point: GPSData, canApprove: Boolean = true) = {
    distance = distance + DistanceUtils.kmsBetween(last, point)
    require(!distance.isNaN, "distance must no be Nan after " + last + " - " + point)
    maxSpeed = if (point.speed > maxSpeed) point.speed else maxSpeed
    super.add(point, canApprove)
  }

  override def toString() = super.toString() + " distance=" + distance


  def isParking = false
}

object Undefined extends MovingState {

  def first = new GPSData(null, null, 0, 0, null, 0, 0, 0)

  def isParking = ???
}