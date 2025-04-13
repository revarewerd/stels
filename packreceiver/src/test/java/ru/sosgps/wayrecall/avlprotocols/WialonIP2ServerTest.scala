package ru.sosgps.wayrecall.avlprotocols

import com.google.common.base.Charsets
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.Netty4ReceiverServerFixture
import ru.sosgps.wayrecall.packreceiver.netty.{GalileoskyServer, WialonIPS2Server}
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPSsOrdered
import ru.sosgps.wayrecall.utils.EventsTestUtils.waitUntil

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date
import scala.collection.JavaConverters.mapAsJavaMapConverter


class WialonIP2ServerTest extends FunSpec with Netty4ReceiverServerFixture {
  override protected def useServer = new WialonIPS2Server

  describe("WialongServer") {

    it("should support compressed login") {

      send("compressed login") as "FF1B00780153F65136D233B0CECC4DCDB4F673B476B4343602002FF404E6"

      expect("auth confirmation") as "23 41 4c 23 31 0d 0a"

    }

    it("should answer messages and store short data package") {

      send("init name and passoword") as "23 4c 23 31 32 33 34 35 36 3b 31 32 33 34 35 36 0d 0a "

      expect("auth confirmation") as "23 41 4c 23 31 0d 0a"

      send("ping") as "23 50 23 0d 0a "

      expect("ping response") as "23 41 50 23 0d 0a"

      send("short data package") as "23 53 44 23 31 37 30 31 32 31 3b 31 38 33 35 32 " +
        " 38 3b 35 33 31 30 2e 39 32 30 30 30 3b 4e 3b 30 " +
        " 32 37 33 34 2e 30 36 32 30 30 3b 45 3b 31 32 3b " +
        " 31 32 3b 33 30 30 3b 37 0d 0a"

      expect("confirmation") as "23 41 53 44 23 31 0d 0a "

      waitUntil(stored.size >= 1, 300)
      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "123456",
          27.5677,
          53.182,
          Date.from(ZonedDateTime.of(2021, 1, 17, 21, 35, 28, 0, ZoneId.of("Europe/Moscow")).toInstant),
          12,
          12,
          7,
          null: String,
          null: Date,
          Map("height" -> 300,
            "protocol" -> "WialonIPS",
            "protocolversion" -> "1").mapValues(_.asInstanceOf[AnyRef]).asJava))      )
    }

    it("should answer messages and store full data package") {

      send("init name and passoword") as "23 4c 23 31 32 33 34 35 36 3b 31 32 33 34 35 36 0d 0a "

      expect("auth confirmation") as "23 41 4c 23 31 0d 0a "

      send("full data package") as "23 44 23 32 33 30 31 32 31 3b 31 34 32 30 30 31 " +
        " 3b 35 33 31 30 2e 39 32 30 30 30 3b 4e 3b 30 32 " +
        " 37 33 34 2e 30 36 32 30 30 3b 45 3b 31 32 3b 31 " +
        " 32 3b 33 30 30 3b 37 3b 31 32 3b 33 33 34 34 3b " +
        " 31 36 33 39 30 34 3b 3b 34 34 3b 61 62 63 3a 31 " +
        " 3a 31 32 2c 64 66 61 3a 32 3a 31 2e 32 2c 53 4f " +
        " 53 3a 31 3a 31 0d 0a"

      expect("confirmation") as "23 41 44 23 31 0d 0a"

      waitUntil(stored.size >= 1, 300)
      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "123456",
          27.5677,
          53.182,
          Date.from(ZonedDateTime.of(2021, 1, 23, 17, 20, 1, 0, ZoneId.of("Europe/Moscow")).toInstant),
          12,
          12,
          7,
          null: String,
          null: Date,
          Map("SOS:1" -> 1.0,
            "abc:1" -> 12.0,
            "adc" -> "",
            "dfa:2" -> 1.2,
            "hdop" -> 12.0f,
            "height" -> 300,
            "inputs" -> 3344,
            "lbutton" -> "44",
            "outputs" -> 163904,
            "protocol" -> "WialonIPS",
            "protocolversion" -> "1").mapValues(_.asInstanceOf[AnyRef]).asJava)))
    }

    it("should answer store from black box") {

      send("init name and passoword") as "23 4c 23 31 32 33 34 35 36 3b 31 32 33 34 35 36 0d 0a "

      expect("auth confirmation") as "23 41 4c 23 31 0d 0a "

      send("blackbox package") as "23 42 23 32 33 30 31 32 31 3b 31 39 35 30 31 38" +
        "3b 35 33 31 30 2e 39 32 30 30 30 3b 4e 3b 30 32" +
        "37 33 34 2e 30 36 32 30 30 3b 45 3b 31 32 3b 31" +
        "32 3b 33 30 30 3b 37 7c 32 33 30 31 32 31 3b 31" +
        "39 35 30 32 38 3b 35 33 31 30 2e 39 32 30 30 30" +
        "3b 4e 3b 30 32 37 33 34 2e 30 36 32 30 30 3b 45" +
        "3b 31 32 3b 31 32 3b 33 30 30 3b 37 7c 32 33 30" +
        "31 32 31 3b 31 39 35 30 34 30 3b 35 33 31 30 2e" +
        "39 32 30 30 30 3b 4e 3b 30 32 37 33 34 2e 30 36" +
        "32 30 30 3b 45 3b 31 32 3b 31 32 3b 33 30 30 3b" +
        "37 7c 37 0d 0a"

      expect("confirmation") as "23 41 42 23 33 0d 0a "

      waitUntil(stored.size >= 1, 300)
      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "123456",
          27.5677,
          53.182,
          Date.from(ZonedDateTime.of(2021, 1, 23, 22, 50, 40, 0, ZoneId.of("Europe/Moscow")).toInstant),
          12,
          12,
          7,
          null: String,
          null: Date,
          Map("height" -> 300,
            "protocol" -> "WialonIPS",
            "protocolversion" -> "1").mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "123456",
          27.5677,
          53.182,
          Date.from(ZonedDateTime.of(2021, 1, 23, 22, 50, 18, 0, ZoneId.of("Europe/Moscow")).toInstant),
          12,
          12,
          7,
          null: String,
          null: Date,
          Map("height" -> 300,
            "protocol" -> "WialonIPS",
            "protocolversion" -> "1").mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "123456",
          27.5677,
          53.182,
          Date.from(ZonedDateTime.of(2021, 1, 23, 22, 50, 28, 0, ZoneId.of("Europe/Moscow")).toInstant),
          12,
          12,
          7,
          null: String,
          null: Date,
          Map("height" -> 300,
            "protocol" -> "WialonIPS",
            "protocolversion" -> "1").mapValues(_.asInstanceOf[AnyRef]).asJava)))
    }

    it("receive big from prod") {

      send("init name and passoword") as "23 4c 23 38 36 32 35 33 31 30 34 33 34 39 30 39 32 30 3b 4e 41 0d 0a"

      expect("auth confirmation") as "23 41 4c 23 31 0d 0a "

      send("large data package") as "23 42 23 31 39 30 33 32 31 3b 30 32 31 32 35 32 " +
        " 3b 35 35 33 35 2e 36 37 34 33 3b 4e 3b 30 33 37 " +
        " 33 36 2e 31 34 31 31 3b 45 3b 30 3b 30 3b 31 37 " +
        " 35 3b 31 37 3b 30 2e 36 33 3b 33 3b 30 3b 3b 4e " +
        " 41 3b 73 74 61 74 75 73 3a 31 3a 35 32 34 33 38 " +
        " 38 2c 73 61 74 73 5f 67 70 73 3a 31 3a 31 30 2c " +
        " 73 61 74 73 5f 67 6c 6f 6e 61 73 73 3a 31 3a 37 " +
        " 2c 70 77 72 5f 65 78 74 3a 32 3a 31 33 2e 38 37 " +
        " 32 2c 61 63 63 5f 78 3a 31 3a 2d 32 38 38 2c 61 " +
        " 63 63 5f 79 3a 31 3a 32 35 36 2c 61 63 63 5f 7a " +
        " 3a 31 3a 39 30 38 2c 72 73 73 69 3a 31 3a 2d 31 " +
        " 31 33 2c 6f 64 6f 6d 65 74 65 72 3a 31 3a 34 32 " +
        " 38 39 31 38 36 2c 62 6f 6f 74 63 6f 75 6e 74 3a " +
        " 31 3a 32 39 7c 31 39 30 33 32 31 3b 30 34 35 37 " +
        " 31 39 3b 35 35 33 36 2e 30 30 34 33 3b 4e 3b 30 " +
        " 33 37 33 36 2e 36 34 39 36 3b 45 3b 30 3b 31 35 " +
        " 38 3b 31 38 32 3b 31 37 3b 30 2e 36 33 3b 30 3b " +
        " 30 3b 3b 4e 41 3b 73 74 61 74 75 73 3a 31 3a 31 " +
        " 39 32 2c 73 61 74 73 5f 67 70 73 3a 31 3a 39 2c " +
        " 73 61 74 73 5f 67 6c 6f 6e 61 73 73 3a 31 3a 38 " +
        " 2c 70 77 72 5f 65 78 74 3a 32 3a 30 2e 31 31 32 " +
        " 2c 61 63 63 5f 78 3a 31 3a 2d 33 30 38 2c 61 63 " +
        " 63 5f 79 3a 31 3a 32 34 38 2c 61 63 63 5f 7a 3a " +
        " 31 3a 39 33 36 2c 72 73 73 69 3a 31 3a 2d 35 31 " +
        " 2c 6d 63 63 3a 31 3a 32 35 30 2c 6d 6e 63 3a 31 " +
        " 3a 31 2c 6c 61 63 3a 31 3a 36 37 30 2c 63 65 6c " +
        " 6c 5f 69 64 3a 31 3a 38 34 39 35 2c 6f 64 6f 6d " +
        " 65 74 65 72 3a 31 3a 34 32 39 30 32 31 33 2c 62 " + 
        " 6f 6f 74 63 6f 75 6e 74 3a 31 3a 32 39 7c 30 37 " +
        " 30 33 32 31 3b 31 35 33 36 35 30 3b 35 35 33 35 " +
        " 2e 34 34 36 35 3b 4e 3b 30 33 37 33 36 2e 31 31 " +
        " 35 34 3b 45 3b 37 3b 32 33 35 3b 31 39 34 3b 34 " +
        " 3b 32 2e 31 38 3b 30 3b 30 3b 3b 4e 41 3b 73 74 " +
        " 61 74 75 73 3a 31 3a 31 32 38 2c 73 61 74 73 5f " +
        " 67 70 73 3a 31 3a 33 2c 73 61 74 73 5f 67 6c 6f " +
        " 6e 61 73 73 3a 31 3a 31 2c 70 77 72 5f 65 78 74 " +
        " 3a 32 3a 30 2e 31 31 32 2c 61 63 63 5f 78 3a 31 " +
        " 3a 2d 33 33 32 2c 61 63 63 5f 79 3a 31 3a 32 38 " +
        " 30 2c 61 63 63 5f 7a 3a 31 3a 39 32 38 2c 72 73 " +
        " 73 69 3a 31 3a 2d 36 31 2c 6d 63 63 3a 31 3a 32 " +
        " 35 30 2c 6d 6e 63 3a 31 3a 31 2c 6c 61 63 3a 31 " +
        " 3a 36 37 30 2c 63 65 6c 6c 5f 69 64 3a 31 3a 33 " +
        " 33 35 35 31 2c 6f 64 6f 6d 65 74 65 72 3a 31 3a " +
        " 33 36 33 37 31 31 33 2c 62 6f 6f 74 63 6f 75 6e " +
        " 74 3a 31 3a 31 35 7c 30 37 30 33 32 31 3b 31 35 " +
        " 33 37 30 39 3b 35 35 33 35 2e 34 34 38 39 3b 4e " +
        " 3b 30 33 37 33 36 2e 31 31 35 36 3b 45 3b 30 3b " +
        " 32 33 35 3b 32 30 30 3b 35 3b 32 2e 36 38 3b 30 " +
        " 3b 30 3b 3b 4e 41 3b 73 74 61 74 75 73 3a 31 3a " +
        " 31 32 38 2c 73 61 74 73 5f 67 70 73 3a 31 3a 34 " +
        " 2c 73 61 74 73 5f 67 6c 6f 6e 61 73 73 3a 31 3a " +
        " 31 2c 70 77 72 5f 65 78 74 3a 32 3a 30 2e 31 31 " +
        " 32 2c 61 63 63 5f 78 3a 31 3a 2d 33 33 32 2c 61 " +
        " 63 63 5f 79 3a 31 3a 32 37 36 2c 61 63 63 5f 7a " +
        " 3a 31 3a 39 32 30 2c 72 73 73 69 3a 31 3a 2d 36 " +
        " 33 2c 6d 63 63 3a 31 3a 32 35 30 2c 6d 6e 63 3a " +
        " 31 3a 31 2c 6c 61 63 3a 31 3a 36 37 30 2c 63 65 " +
        " 6c 6c 5f 69 64 3a 31 3a 33 33 35 35 31 2c 6f 64 " +
        " 6f 6d 65 74 65 72 3a 31 3a 33 36 33 37 31 32 31 " +
        " 2c 62 6f 6f 74 63 6f 75 6e 74 3a 31 3a 31 35 7c " +
        " 30 37 30 33 32 31 3b 31 35 33 37 31 32 3b 35 35 " +
        " 33 35 2e 34 34 39 32 3b 4e 3b 30 33 37 33 36 2e " +
        " 31 31 31 38 3b 45 3b 34 3b 32 33 35 3b 32 30 30 " +
        " 3b 34 3b 32 2e 31 36 3b 30 3b 30 3b 3b 4e 41 3b"
      
      send("additionally") as "73 74 61 74 75 73 3a 31 3a 31 32 38 2c 73 61 74 " +
        " 73 5f 67 70 73 3a 31 3a 33 2c 73 61 74 73 5f 67 " +
        " 6c 6f 6e 61 73 73 3a 31 3a 31 2c 70 77 72 5f 65 " +
        " 78 74 3a 32 3a 30 2e 31 31 32 2c 61 63 63 5f 78 " +
        " 3a 31 3a 2d 33 32 38 2c 61 63 63 5f 79 3a 31 3a " +
        " 32 36 38 2c 61 63 63 5f 7a 3a 31 3a 39 32 38 2c " +
        " 72 73 73 69 3a 31 3a 2d 36 33 2c 6d 63 63 3a 31 " +
        " 3a 32 35 30 2c 6d 6e 63 3a 31 3a 31 2c 6c 61 63 " +
        " 3a 31 3a 36 37 30 2c 63 65 6c 6c 5f 69 64 3a 31 " +
        " 3a 33 33 35 35 31 2c 6f 64 6f 6d 65 74 65 72 3a " +
        " 31 3a 33 36 33 37 31 32 35 2c 62 6f 6f 74 63 6f " +
        " 75 6e 74 3a 31 3a 31 35 0d 0a"

      expect("confirmation") as "23 41 42 23 35 0d 0a "

      waitUntil(stored.size >= 1, 300)
      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "862531043490920",
          37.602351666666664,
          55.59457166666667,
          Date.from(ZonedDateTime.of(2021, 3, 19, 5, 12, 52, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          17,
          null: String,
          null: Date,
          Map("acc_x:1" -> -288.0,
            "acc_y:1" -> 256.0,
            "acc_z:1" -> 908.0,
            "adc" -> "",
            "bootcount:1" -> 29.0,
            "hdop" -> 0.63f,
            "height" -> 175,
            "inputs" -> 3,
            "odometer:1" -> 4289186.0,
            "outputs" -> 0,
            "protocol" -> "WialonIPS",
            "protocolversion" -> "1",
            "pwr_ext:2" -> 13.872,
            "rssi:1" -> -113.0,
            "sats_glonass:1" -> 7.0,
            "sats_gps:1" -> 10.0,
            "status:1" -> 524388.0).mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "862531043490920",
          37.61082666666667,
          55.600071666666665,
          Date.from(ZonedDateTime.of(2021, 3, 19, 7, 57, 19, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          158,
          17,
          null: String,
          null: Date,
          Map("acc_x:1" -> -308.0,
            "acc_y:1" -> 248.0,
            "acc_z:1" -> 936.0,
            "adc" -> "",
            "bootcount:1" -> 29.0,
            "cell_id:1" -> 8495.0,
            "hdop" -> 0.63f,
            "height" -> 182,
            "inputs" -> 0,
            "lac:1" -> 670.0,
            "mcc:1" -> 250.0,
            "mnc:1" -> 1.0,
            "odometer:1" -> 4290213.0,
            "outputs" -> 0,
            "protocol" -> "WialonIPS",
            "protocolversion" -> "1",
            "pwr_ext:2" -> 0.112,
            "rssi:1" -> -51.0,
            "sats_glonass:1" -> 8.0,
            "sats_gps:1" -> 9.0,
            "status:1" -> 192.0).mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "862531043490920",
          37.60192333333333,
          55.590775,
          Date.from(ZonedDateTime.of(2021, 3, 7, 18, 36, 50, 0, ZoneId.of("Europe/Moscow")).toInstant),
          7,
          235,
          4,
          null: String,
          null: Date,
          Map("acc_x:1" -> -332.0,
            "acc_y:1" -> 280.0,
            "acc_z:1" -> 928.0,
            "adc" -> "",
            "bootcount:1" -> 15.0,
            "cell_id:1" -> 33551.0,
            "hdop" -> 2.18f,
            "height" -> 194,
            "inputs" -> 0,
            "lac:1" -> 670.0,
            "mcc:1" -> 250.0,
            "mnc:1" -> 1.0,
            "odometer:1" -> 3637113.0,
            "outputs" -> 0,
            "protocol" -> "WialonIPS",
            "protocolversion" -> "1",
            "pwr_ext:2" -> 0.112,
            "rssi:1" -> -61.0,
            "sats_glonass:1" -> 1.0,
            "sats_gps:1" -> 3.0,
            "status:1" -> 128.0).mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "862531043490920",
          37.601926666666664,
          55.590815,
          Date.from(ZonedDateTime.of(2021, 3, 7, 18, 37, 9, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          235,
          5,
          null: String,
          null: Date,
          Map("acc_x:1" -> -332.0,
            "acc_y:1" -> 276.0,
            "acc_z:1" -> 920.0,
            "adc" -> "",
            "bootcount:1" -> 15.0,
            "cell_id:1" -> 33551.0,
            "hdop" -> 2.68f,
            "height" -> 200,
            "inputs" -> 0,
            "lac:1" -> 670.0,
            "mcc:1" -> 250.0,
            "mnc:1" -> 1.0,
            "odometer:1" -> 3637121.0,
            "outputs" -> 0,
            "protocol" -> "WialonIPS",
            "protocolversion" -> "1",
            "pwr_ext:2" -> 0.112,
            "rssi:1" -> -63.0,
            "sats_glonass:1" -> 1.0,
            "sats_gps:1" -> 4.0,
            "status:1" -> 128.0).mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "862531043490920",
          37.601863333333334,
          55.59082,
          Date.from(ZonedDateTime.of(2021, 3, 7, 18, 37, 12, 0, ZoneId.of("Europe/Moscow")).toInstant),
          4,
          235,
          4,
          null: String,
          null: Date,
          Map("acc_x:1" -> -328.0,
            "acc_y:1" -> 268.0,
            "acc_z:1" -> 928.0,
            "adc" -> "",
            "bootcount:1" -> 15.0,
            "cell_id:1" -> 33551.0,
            "hdop" -> 2.16f,
            "height" -> 200,
            "inputs" -> 0,
            "lac:1" -> 670.0,
            "mcc:1" -> 250.0,
            "mnc:1" -> 1.0,
            "odometer:1" -> 3637125.0,
            "outputs" -> 0,
            "protocol" -> "WialonIPS",
            "protocolversion" -> "1",
            "pwr_ext:2" -> 0.112,
            "rssi:1" -> -63.0,
            "sats_glonass:1" -> 1.0,
            "sats_gps:1" -> 3.0,
            "status:1" -> 128.0).mapValues(_.asInstanceOf[AnyRef]).asJava)))
    }


  }


}



