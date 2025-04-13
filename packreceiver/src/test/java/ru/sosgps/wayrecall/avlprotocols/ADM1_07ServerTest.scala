package ru.sosgps.wayrecall.avlprotocols

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import com.google.common.base.Charsets
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.Netty4ReceiverServerFixture
import ru.sosgps.wayrecall.packreceiver.netty.ADM1_07Server
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPSsOrdered
import ru.sosgps.wayrecall.utils.EventsTestUtils.waitUntil

import scala.collection.JavaConverters.mapAsJavaMapConverter


class ADM1_07ServerTest extends FunSpec with Netty4ReceiverServerFixture {
  override protected def useServer = new ADM1_07Server()

  private def text(str: String) = str.getBytes(Charsets.US_ASCII)

  describe("ADM1_07Server") {

    it("should answer messages and store data1") {

      send("init with imei") as "01 00 42 03 38 36 31 33 35 39 30 33 31 30 32 35 " +
        "39 36 36 21 01 00 00 00 00 00 00 00 00 00 00 00" +
        " 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        " 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        " 00 5c"

      send("data") as "01 00 2e 08 0d e0 02 24 40 00 00 c0 7f 00 00 c0 " +
        " 7f 00 00 00 00 00 00 00 00 00 bb 0b 22 5b 00 00 " +
        " 00 00 a0 2c 57 00 00 00 00 00 00 00 00 00 01 00 " +
        " 2e 08 0d df 02 24 40 00 00 c0 7f 00 00 c0 7f 00 " +
        " 00 00 00 00 00 00 00 00 ba 0b 22 5b 00 00 00 00 " +
        " b5 2c 57 00 00 00 00 00 00 00 00 00 01 00 2e 08 " +
        " 0d de 02 25 40 00 00 c0 7f 00 00 c0 7f 00 00 00 " +
        " 00 00 00 00 00 00 b9 0b 22 5b 00 00 00 00 b5 2c " +
        " 57 00 00 00 00 00 00 00 00 00"


      //      expect("confirmation") as "78 78 05 01 00 01 D9 DC 0D 0A"

      //
      waitUntil(stored.size >= 3, 300)

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "861359031025966",
          Double.NaN,
          Double.NaN,
          Date.from(ZonedDateTime.of(2018, 6, 14, 9, 31, 23, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          0,
          null: String,
          null: Date,
          Map("protocol" -> "ADM1.07").mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "861359031025966",
          Double.NaN,
          Double.NaN,
          Date.from(ZonedDateTime.of(2018, 6, 14, 9, 31, 22, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          0,
          null: String,
          null: Date,
          Map("protocol" -> "ADM1.07").mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "861359031025966",
          Double.NaN,
          Double.NaN,
          Date.from(ZonedDateTime.of(2018, 6, 14, 9, 31, 21, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          0,
          null: String,
          null: Date,
          Map("protocol" -> "ADM1.07").mapValues(_.asInstanceOf[AnyRef]).asJava)))

    }

    it("should not store zero messages") {
      send("init with imei") as "01 00 42 03 38 36 31 33 35 39 30 33 31 30 32 35 " +
        "39 36 36 21 01 00 00 00 00 00 00 00 00 00 00 00" +
        " 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        " 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        " 00 5c"

      send("data") as "01 00 2e 08 20 fe 7b 20 40 00 00 00 00 00 00 00 " +
        "00 cc 06 00 00 00 00 00 00 00 ec f2 ca 60 00 00 " +
        " 00 00 dd 8b 33 00 00 00 29 00 fa 00 01 00"
      
      waitUntil(stored.nonEmpty, 300)

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "861359031025966",
          Double.NaN,
          Double.NaN,
          Date.from(ZonedDateTime.of(2021, 6, 17, 9, 59, 56, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          174,
          0,
          null: String,
          null: Date,
          Map("protocol" -> "ADM1.07").mapValues(_.asInstanceOf[AnyRef]).asJava)))

    }


  }


}



