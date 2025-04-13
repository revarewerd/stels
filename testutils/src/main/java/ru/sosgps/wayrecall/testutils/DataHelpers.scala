package ru.sosgps.wayrecall.testutils

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import ru.sosgps.wayrecall.core.GPSData

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions.mapAsScalaMap

/**
  * Created by nmitropo on 19.12.2015.
  */
object DataHelpers {

  def toScalaCode(gps: GPSData, woData: Boolean = false): String = {


    val attrs = ListBuffer(
      wrapString(gps.uid),
      wrapString(gps.imei),
      Option(gps.lon).filterNot(java.lang.Double.isNaN).getOrElse("Double.NaN"),
      Option(gps.lat).filterNot(java.lang.Double.isNaN).getOrElse("Double.NaN"),
      wrapDate(gps.time),
      gps.speed,
      gps.course,
      gps.satelliteNum,
      wrapString(gps.placeName),
      wrapDate(gps.insertTime)
    )

    if(!woData)
      attrs += toScalaCode(gps.data)


    attrs.mkString("new GPSData(", if(woData) ", " else ",\n", ")")
  }

  def toScalaCode(map: java.util.Map[String, Object]): String = {
    map.toList.sortBy(_._1).map(kv => "\"" + kv._1 + "\" -> " + {
      kv._2 match {
        case n: Integer => n
        case n: java.lang.Long => n + "L"
        case n: java.lang.Float => n + "f"
        case n: java.lang.Double => n
        case n: java.lang.Short => n + ".toShort"
        case n: java.lang.Byte => n + ".toByte"
        case s: String => wrapString(s)
      }

    }).mkString("Map(", ",\n", ").mapValues(_.asInstanceOf[AnyRef]).asJava")
  }

  private def wrapString(str: String) = Option(str).map("\"" + _ + "\"").getOrElse("null: String")

  private def wrapDate(date: Date) = Option(date).map(d => {
    val zonedDateTime = ZonedDateTime.ofInstant(d.toInstant, ZoneId.of("Europe/Moscow"))
    s"""Date.from(ZonedDateTime.of(${zonedDateTime.getYear}, ${zonedDateTime.getMonthValue}, ${zonedDateTime.getDayOfMonth}, ${zonedDateTime.getHour}, ${zonedDateTime.getMinute}, ${zonedDateTime.getSecond}, ${zonedDateTime.getNano}, ZoneId.of("Europe/Moscow")).toInstant)"""
  }
  ).getOrElse("null: Date")


}
