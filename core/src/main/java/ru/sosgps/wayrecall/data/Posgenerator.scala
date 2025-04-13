package ru.sosgps.wayrecall.data

import java.time._
import java.util.Date

import ru.sosgps.wayrecall.core.GPSData



class Posgenerator(uid: String, imei: String,
                   start: Instant = ZonedDateTime.of(2010, 1,1, 12,0,0, 0 , ZoneId.of("Europe/Moscow")).toInstant) {


  private val log = grizzled.slf4j.Logger(classOf[Posgenerator])
  import log._

  @deprecated
  def this(uid: String, start: Long)= this(uid, uid, Instant.ofEpochMilli(start))

  def this(uid: String) = this(uid, uid)

  var last = new GPSData(uid, imei, 56, 37, Date.from(start), 0, 0, 3)

  def upd(f: (GPSData => GPSData)): GPSData = upd(f(last))

  def upd(gpsData: GPSData): GPSData = {
    last = gpsData
    gpsData
  }

  var defaultCount = 10

  def genParking(duration: Duration, count: Int = defaultCount): Iterator[GPSData] = {
    val step = duration.toMillis  / count
    Iterator.from(0).map(i => upd(new GPSData(
      last.uid,
      last.imei,
      last.lat,
      last.lon,
      new Date(last.time.getTime + step),
      0,
      0,
      3
    ))).take(count)
  }


  def genMoving(duration: Duration, count: Int = defaultCount, increment: Double = 0.0003): Iterator[GPSData] = {

    val step = duration.dividedBy(count)
    movingsWithTimeStep(step, increment).take(count)
  }

  def movingsWithTimeStep(step: Duration, increment: Double = 0.0003): Iterator[GPSData] = {
    Iterator.from(0).map(i => upd(new GPSData(
      last.uid,
      last.imei,
      last.lon + increment,
      last.lat + increment,
      new Date(last.time.getTime + step.toMillis),
      20,
      90,
      3
    )))
  }


}