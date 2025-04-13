package ru.sosgps.wayrecall.avlprotocols.ruptela

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.{Netty3ReceiverServerFixture, Netty4ReceiverServerFixture}
import ru.sosgps.wayrecall.packreceiver.netty.{ArnaviServer, RuptelaNettyServer}
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPSsOrdered

import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
  * Created by nickl on 15.01.17.
  */
class RuptelaiServerTest extends FunSpec with Netty3ReceiverServerFixture[RuptelaNettyServer] {
  override protected def useServer = new RuptelaNettyServer


  describe("Ruptela server") {
    it("should answer messages and store data") {

      send("message") as "00 53 00 03 10 f5 61 49 6f 76 01 00 02 5a 5b 70 " +
        "8a 00 00 16 73 18 82 21 4c f1 8c 06 6b 4c 72 07 " +
        "00 22 0b 08 03 05 01 1b 12 20 2a 01 1d 36 be 00 " +
        "00 5a 5b 70 90 00 00 16 73 10 1c 21 4c df f8 06 " +
        "6c 4c 68 07 00 19 0b 08 03 05 01 1b 12 20 2a 01 " +
        "1d 36 a7 00 00 58 1b "
      expect("confirmation") as "00 02 64 01 13 bc "

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "863071015366518",
          37.6639516,
          55.86862,
          Date.from(ZonedDateTime.of(2018, 1, 14, 18, 0, 32, 0, ZoneId.of("Europe/Moscow")).toInstant),
          25,
          195,
          7,
          null: String,
          null: Date,
          Map("27" -> 18,
            "29" -> 13991,
            "32" -> 42,
            "5" -> 1,
            "alt" -> 164.4,
            "protocol" -> "Ruptela",
            "pwr_ext" -> 13.991).mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "863071015366518",
          37.6641666,
          55.86907,
          Date.from(ZonedDateTime.of(2018, 1, 14, 18, 0, 26, 0, ZoneId.of("Europe/Moscow")).toInstant),
          34,
          195,
          7,
          null: String,
          null: Date,
          Map("27" -> 18,
            "29" -> 14014,
            "32" -> 42,
            "5" -> 1,
            "alt" -> 164.3,
            "protocol" -> "Ruptela",
            "pwr_ext" -> 14.014).mapValues(_.asInstanceOf[AnyRef]).asJava)))

    }

    it("should answer and not store bad messages") {

      send("message") as "01 64 00 03 11 6e 74 32 fa ad 01 00 01 5a 5b 70 " +
        "8e 00 00 1c 4a f9 4d 21 79 43 a4 03 1c 47 c2 0b " +
        "00 29 07 08 04 06 39 05 01 1b 10 cf 57 02 1d 6e " +
        "93 c5 24 42 02 d0 00 09 2f 3d 72 0d 24 1b eb 00 " +
        "5d 71 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 " +
        "00 00 00 00 00 00 00 00"
      expect("confirmation") as "00 02 64 01 13 bc"

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "863591023704749",
          47.4675533,
          56.15953,
          Date.from(ZonedDateTime.of(2018, 1, 14, 18, 0, 30, 0, ZoneId.of("Europe/Moscow")).toInstant),
          41,
          183,
          11,
          null: String,
          null: Date,
          Map("114" -> 220470251,
            "197" -> 9282,
            "207" -> 87,
            "208" -> 601917,
            "27" -> 16,
            "29" -> 28307,
            "5" -> 1,
            "6" -> 57,
            "alt" -> 79.6,
            "protocol" -> "Ruptela",
            "pwr_ext" -> 28.307).mapValues(_.asInstanceOf[AnyRef]).asJava)))

    }

    it("should answer and not store messages with zeros") {
      send("message") as "00 61 00 03 13 61 2d 76 1d 59 01 00 02 5a 5b 90 " +
        "6c 00 00 ff ff fe f6 ff ff f6 3c 00 00 02 76 00 " +
        "00 00 00 07 05 05 00 1b 13 20 1a 88 00 87 00 02 " +
        "1d 32 39 8b 00 00 00 00 5a 5b 92 c3 00 00 ff ff " +
        "fe f6 ff ff f6 3c 00 00 02 76 00 00 00 00 07 05 " +
        "05 00 1b 12 20 1a 88 00 87 00 02 1d 32 2b 8b 00 " +
        "00 00 00 da 21"
      expect("confirmation") as "00 02 64 01 13 bc"

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "865733025602905",
          Double.NaN,
          Double.NaN,
          Date.from(ZonedDateTime.of(2018, 1, 14, 20, 16, 28, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          6,
          0,
          null: String,
          null: Date,
          Map("135" -> 0,
            "136" -> 0,
            "139" -> 0,
            "27" -> 19,
            "29" -> 12857,
            "32" -> 26,
            "5" -> 0,
            "alt" -> 0.0,
            "protocol" -> "Ruptela",
            "pwr_ext" -> 12.857).mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "865733025602905",
          Double.NaN,
          Double.NaN,
          Date.from(ZonedDateTime.of(2018, 1, 14, 20, 26, 27, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          6,
          0,
          null: String,
          null: Date,
          Map("135" -> 0,
            "136" -> 0,
            "139" -> 0,
            "27" -> 18,
            "29" -> 12843,
            "32" -> 26,
            "5" -> 0,
            "alt" -> 0.0,
            "protocol" -> "Ruptela",
            "pwr_ext" -> 12.843).mapValues(_.asInstanceOf[AnyRef]).asJava)))

    }

  }

  override protected def setupServer(): Unit = {
    super.setupServer()
    server.ruptelaStore.configsPath = "." //dummy value
  }
}



