package ru.sosgps.wayrecall.monitoring.processing.parkings

import java.util.Date

import grizzled.slf4j.Logging
import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData, GPSUtils}

import scala.collection.mutable.ArrayBuffer

/**
 * Created by nickl on 27.01.14.
 */
class MovingStatesExtractor extends Logging {


  var maxDist: Double = 0.002;
  var minTime: Double = 1.0 * 1000 * 60

  def getDiff(point: GPSData, parkCandidate: MovingState) = {
    val distDiff = GPSData.manhdistance(point, parkCandidate.first)
    val timeDiff = point.time.getTime - parkCandidate.first.time.getTime
    require(timeDiff >= 0, "timediff must be positive (" + timeDiff + ")\n" +
      "parkCandidate.startpoint = " + parkCandidate.first + "\n" +
      "point=" + point
    )
    (distDiff, timeDiff)
  }

  def canExtendParking(point: GPSData, timeDiff: Long): Boolean = {
    timeDiff >= minTime && point.speed <= 3
  }

  def extendIfPossible(parking: Parking, point: GPSData): Boolean = {
    if (parking.contains(point))
      return true
    else {
      val (distDiff, timeDiff) = getDiff(point, parking)
      if (canExtendParking(point, timeDiff)) {
        parking.add(point)
        return true
      } else
        return false
    }
  }

  @deprecated
  def extractMovingStates(history: Iterator[GPSData]): Iterator[MovingState] = {

    var prevState: MovingState = Undefined
    var currentState: MovingState = Undefined

    def returnAndResetPrevState: Iterable[MovingState] = {
     // println(s"returnAndResetPrevState=${prevState.getClass.getSimpleName},${prevState.approved},${prevState.lastPoint.time} current=${currentState.getClass.getSimpleName},${currentState.approved},${currentState.lastPoint.time}")
      val r = if (prevState != Undefined && prevState != currentState && currentState.approved) {
        //Seq(currentState.startPoint, currentState.lastPoint).distinct.foreach(prevState.add)
        val r = prevState
        prevState = Undefined
        Iterable(r)
      } else
        Iterable.empty
      //println(s"r=${r}")
      r
    }

    history.filter(_.goodlonlat).flatMap(point => {
 //     println(s"point=${point},${prevState.approved}")
 //     println(s"prevStateState=${prevState.getClass.getSimpleName},${prevState.approved},${prevState.lastPoint.time}")
 //     println(s"currentState=${currentState.getClass.getSimpleName},${currentState.approved},${currentState.lastPoint.time}")
      currentState match {
        case parkCandidate: Parking =>
          val (distDiff, timeDiff) = getDiff(point, parkCandidate)
          if (distDiff > maxDist && point.speed > 0) {
            if (parkCandidate.approved) {
              currentState = new Moving(parkCandidate.last, point)
              currentState.pointsCount = 1
              Iterable(prevState, parkCandidate).filter(Undefined !=)
            }
            else {
              prevState match {
                case m: Moving =>
                  m.pointsCount = m.pointsCount + currentState.pointsCount
                  m.add(point)
                  currentState = m
                case _ => currentState = new Moving(parkCandidate, point)
              }
              None
            }
          }
          else if (canExtendParking(point, timeDiff)) {
            parkCandidate.add(point)
            returnAndResetPrevState
          }
          else {
            parkCandidate.add(point, canApprove = false)
            None
          }

        case movingCandidate: Moving =>
          //val (distDiff, timeDiff) = getDiff(point, movingCandidate)
          if (point.speed > 0) {
            movingCandidate.add(point)
            returnAndResetPrevState
          }
          else if (point.speed == 0) {
            prevState = currentState
            currentState = new Parking(movingCandidate.last, point)
            currentState.pointsCount = 1
            None
          }
          else
            None

        case Undefined =>
          if (point.speed == 0) {
            currentState = new Parking(point)
          }
          else
            currentState = new Moving(point)
          None
      }
    }) ++ Seq(prevState, currentState).distinct.filter(Undefined !=)
  }

  def isMoving(gps: GPSData, settings: ObjectTripSettings) = {
    gps.speed >= settings.minMovementSpeed &&
      (!settings.useIgnitionToDetectMovement || GPSUtils.detectIgnitionInt(gps).getOrElse(0) > 0)
  }


  def isValid(t: Moving, settings: ObjectTripSettings) = {
    t.distance >= settings.minTripDistance / 1000.0 &&  t.intervalMills >= settings.minTripTime * 1000
  }

  def isValid(p: Parking, settings: ObjectTripSettings) = {
    p.intervalMills >= settings.minParkingTime * 1000
  }

  def detectStates(history: Iterator[GPSData], settings: ObjectTripSettings): Iterable[MovingState] = {
    var currentState: MovingState = null
    var pendingTrip: Moving = null
    var pendingParking: Parking = null

    def splitByGaps(messages: Iterable[GPSData]): Stream[Iterable[GPSData]] = {
      if(messages.nonEmpty) {
        var prev = messages.head
        var rest = messages.tail
        val buf = new ArrayBuffer[GPSData]
        buf.append(prev)
        while(rest.nonEmpty) {
          val gps = rest.head
          val dist = DistanceUtils.kmsBetween(prev,gps)
          if(dist * 1000 <= settings.maxDistanceBetweenMessages) {
            buf.append(gps)
            rest = rest.tail
            prev = gps
          }
          else
            return buf #:: splitByGaps(rest)
        }
        Stream(buf)
      }
      else
        Stream.empty
    }


    def detect(messages: Iterator[GPSData], prev: GPSData): Stream[MovingState]= {
      // Стоянка - от первого сообщения без движения до последнего, аналогично остановка
      // Поездка - от сообщения, предшествующему сообщению со скоростью, до сообщения без скорости
      // нормативный расход
      // filterBadLonLat в отчетах по топливу // Ok
      // разрывы в мониторинге, добавить их отображение как путей. При двойном клике переход на pathgrid Ok
      //  Настройки GUI  (в том числе биллинг?) Ok
      // Базовая статистика по поездкам (пробег Ok)
      // ObjectTripsSettings N Ok
      // Суммарный перерасход топлива
      // регеокодинг
      // групповые отчеты
      // уведомления по длинным стоянкам Ok
      // статистика с учетом новой градации
      // отрефакторить movementStats
      if(messages.hasNext){
        val gps = messages.next()
        currentState match {
          case moving: Moving =>
            if (isMoving(gps, settings)) {
              moving.append(gps)
            }
            else {
              require(pendingTrip == null)
              moving.append(gps)
              pendingTrip = moving
              currentState = new Parking(gps)
              if(isValid(moving, settings)) {
                if(pendingParking != null) {
                  val park = pendingParking
                  pendingParking = null
                    //  if(isValid(park,settings))
                    return park #:: detect(messages,gps)
                }
              }

            }
          case parking: Parking =>
            if (!isMoving(gps, settings)) {
              parking.append(gps)
            }
            else {
                if(isValid(parking, settings)) {
                  currentState = new Moving(prev)
                  if(pendingParking != null) { // pendingParking должен быть сброшен если pendingTrip валиден
                    require(pendingTrip != null && !isValid(pendingTrip, settings))
                    pendingParking.absorb(pendingTrip)
                    pendingParking.absorb(parking)
                    pendingTrip = null
                  }
                  else {
                    pendingParking = parking
                    if(pendingTrip != null) {
                      val trip = pendingTrip
                      pendingTrip = null
                      if(isValid(trip, settings)) {
                        return trip #:: detect(messages, gps)
                      }
                    }
                  }
                }
                else {
                  if(pendingTrip != null) {
                    pendingTrip.absorb(parking)
                    currentState = pendingTrip
                    pendingTrip = null
                    return parking #:: detect(messages, gps)
                  }
                  else {
                    require(pendingParking == null)
                    pendingParking = parking
                    currentState = new Moving(prev)
                  }
                }
            }
        }
        detect(messages, gps)
      }
      else {

        def valid(st: MovingState) = if(st.isParking) true else isValid(st.asInstanceOf[Moving], settings)
        currentState match {
          case moving: Moving => Stream(pendingParking,moving).filter(_ != null).filter(valid)
          case parking: Parking  if isValid(parking,settings) =>
            if(pendingParking != null) {
              // pendingParking должен быть сброшен если pendingTrip валиден
              require(pendingTrip != null && !isValid(pendingTrip, settings))
              pendingParking.absorb(pendingTrip)
              pendingParking.absorb(parking)
              Stream(pendingParking)
            }
            else {
              if(pendingTrip != null && isValid(pendingTrip,settings))
                Stream(pendingTrip, parking)
              else
                Stream(parking)
            }
          case parking: Parking =>
            if(pendingTrip != null)
              pendingTrip.absorb(parking)
            Stream(pendingParking, pendingTrip, parking).filter(_ != null).filter(valid)
          }
        }
      }



    splitByGaps(
      history
      .filter(_.goodlonlat)
        .filter(gps => gps.satelliteNum < 0 || gps.satelliteNum >= settings.minSatelliteNum)
        .toIterable
    ).flatMap(part => {
      pendingTrip = null
      pendingParking = null
      val first = part.head
      if(isMoving(first,settings))
        currentState = new Moving(first)
      else
        currentState = new Parking(first)
      detect(part.tail.toIterator,first)
    }).map{
      case p: Parking if !isValid(p,settings) =>
        p.small = true
        p
      case x => x
    }.sortBy(_.firstTime)
  }
}




