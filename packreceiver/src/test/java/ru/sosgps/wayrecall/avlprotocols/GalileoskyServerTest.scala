package ru.sosgps.wayrecall.avlprotocols

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import com.google.common.base.Charsets
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.Netty4ReceiverServerFixture
import ru.sosgps.wayrecall.packreceiver.netty.GalileoskyServer
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPSsOrdered
import ru.sosgps.wayrecall.utils.EventsTestUtils.waitUntil

import scala.collection.JavaConverters.mapAsJavaMapConverter


class GalileoskyServerTest extends FunSpec with Netty4ReceiverServerFixture {
  override protected def useServer = new GalileoskyServer()

  private def text(str: String) = str.getBytes(Charsets.US_ASCII)

  describe("GalileoskyServer") {

    it("should answer messages and store data") {

      send("init with imei") as "01 17 80 01 11 02 DF 03 38 36 38 32 30 34 30 30" +
        "35 36 34 37 38 33 38 04 32 00 86 9C"

      expect("confirmation") as "02 86 9c"

      send("data") as " 01 3b 00 10 52 3b 20 79 56 22 5d 30 0c ea 00 53 " +
        " 03 62 82 41 02 33 00 00 00 00 34 a7 00 40 00 38 " +
        " 41 2a 2d 42 ee 0e 43 1a 45 0d 00 46 00 00 47 00 " +
        " 00 00 00 50 00 00 51 00 00 c1 00 8b 00 00 b9 e9 "

      expect("confirmation") as "02 b9 e9 "

      send("data") as "01 3b 00 10 55 3b 20 95 56 22 5d 30 0c e1 00 53 " +
        " 03 63 82 41 02 33 00 00 00 00 34 a6 00 40 00 38 " +
        " 41 39 2d 42 ee 0e 43 1a 45 0d 00 46 01 00 47 00 " +
        " 00 00 00 50 4b 29 51 00 00 c1 00 8b 00 00 d1 ac"

      expect("confirmation") as "02 d1 ac "

      waitUntil(stored.size >= 2, 300)
      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "868204005647838",
          37.84764862060547,
          55.771366119384766,
          Date.from(ZonedDateTime.of(2019, 7, 7, 23, 30, 49, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          12,
          null: String,
          null: Date,
          Map("acc" -> 3822,
            "can_a1" -> 35584,
            "in0" -> 0,
            "in1" -> 0,
            "inputs" -> "0",
            "outputs" -> "1101",
            "power" -> 11562,
            "protocol" -> "GalileoSky",
            "temp" -> 26.toByte).mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "868204005647838",
          37.847652435302734,
          55.771358489990234,
          Date.from(ZonedDateTime.of(2019, 7, 7, 23, 31, 17, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          12,
          null: String,
          null: Date,
          Map("acc" -> 3822,
            "can_a1" -> 35584,
            "in0" -> 10571,
            "in1" -> 0,
            "inputs" -> "1",
            "outputs" -> "1101",
            "power" -> 11577,
            "protocol" -> "GalileoSky",
            "temp" -> 26.toByte).mapValues(_.asInstanceOf[AnyRef]).asJava))      )
    }


    it("should answer messages and store big set of data") {

      send("data") as "01 4c 83 01 82 02 15 03 38 36 37 34 35 39 30 34 " +
        " 34 33 36 37 34 31 35 04 32 00 05 00 10 3e 3c 20 " +
        " 61 5d 22 5d 30 0c 38 01 53 03 60 82 41 02 33 00 " +
        " 00 00 00 34 a0 00 35 07 40 00 38 41 1a 2d 42 ee " +
        " 0e 43 1a 45 0d 00 46 00 00 47 00 00 00 00 48 03 " +
        " 00 50 00 00 51 00 00 52 00 00 53 00 00 54 00 00 " +
        " 55 00 00 60 00 00 61 00 00 62 00 00 63 00 00 00 " +
        " 64 00 00 00 65 00 00 00 66 00 00 00 67 00 00 00 " +
        " 68 00 00 00 69 00 00 00 6a 00 00 00 6b 00 00 00 " +
        " 6c 00 00 00 6d 00 00 00 6e 00 00 00 6f 00 00 00 " +
        " 8a 00 8b 00 8c 00 90 00 00 00 00 a0 00 a1 00 a2 " +
        " 00 a3 00 a4 00 a5 00 a6 00 a7 00 a8 00 a9 00 aa " +
        " 00 ab 00 ac 00 ad 00 ae 00 af 00 b0 00 00 b1 00 " +
        " 00 b2 00 00 b3 00 00 b4 00 00 b5 00 00 b6 00 00 " +
        " b7 00 00 b8 00 00 b9 00 00 c0 00 00 00 00 c1 00 " +
        " 8b 00 00 c2 00 00 00 00 c3 00 00 00 00 c4 00 c5 " +
        " 00 c6 00 c7 00 c8 00 c9 00 ca 00 cb 00 cc 00 cd " +
        " 00 ce 00 cf 00 d0 00 d1 00 d2 00 d3 00 00 00 00 " +
        " d4 84 75 00 00 d5 00 d6 00 00 d7 00 00 d8 00 00 " +
        " d9 00 00 da 00 00 db 00 00 00 00 dc 00 00 00 00 " +
        " dd 00 00 00 00 de 00 00 00 00 df 00 00 00 00 e2 " +
        " 00 00 00 00 e3 00 00 00 00 e4 00 00 00 00 e5 00 " +
        " 00 00 00 e6 00 00 00 00 e7 00 00 00 00 e8 00 00 " +
        " 00 00 e9 00 00 00 00 f0 00 00 00 00 f1 00 00 00 " +
        " 00 f2 00 00 00 00 f3 00 00 00 00 f4 00 00 00 00 " +
        " f5 00 00 00 00 f6 00 00 00 00 f7 00 00 00 00 f8 " +
        " 00 00 00 00 f9 00 00 00 00 01 82 02 15 03 38 36 " +
        " 37 34 35 39 30 34 34 33 36 37 34 31 35 04 32 00 " +
        " 05 00 10 3d 3c 20 60 5d 22 5d 30 0c 38 01 53 03 " +
        " 60 82 41 02 33 00 00 00 00 34 a0 00 35 07 40 00 " +
        " 38 41 2f 2d 42 ee 0e 43 1a 45 0d 00 46 01 00 47 " +
        " 00 00 00 00 48 02 00 50 40 29 51 00 00 52 00 00 " +
        " 53 00 00 54 00 00 55 00 00 60 00 00 61 00 00 62 " +
        " 00 00 63 00 00 00 64 00 00 00 65 00 00 00 66 00 " +
        " 00 00 67 00 00 00 68 00 00 00 69 00 00 00 6a 00 " +
        " 00 00 6b 00 00 00 6c 00 00 00 6d 00 00 00 6e 00 " +
        " 00 00 6f 00 00 00 8a 00 8b 00 8c 00 90 00 00 00 " +
        " 00 a0 00 a1 00 a2 00 a3 00 a4 00 a5 00 a6 00 a7 " +
        " 00 a8 00 a9 00 aa 00 ab 00 ac 00 ad 00 ae 00 af " +
        " 00 b0 00 00 b1 00 00 b2 00 00 b3 00 00 b4 00 00 " +
        " b5 00 00 b6 00 00 b7 00 00 b8 00 00 b9 00 00 c0 " +
        " 00 00 00 00 c1 00 8b 00 00 c2 00 00 00 00 c3 00 " +
        " 00 00 00 c4 00 c5 00 c6 00 c7 00 c8 00 c9 00 ca " +
        " 00 cb 00 cc 00 cd 00 ce 00 cf 00 d0 00 d1 00 d2 " +
        " 00 d3 00 00 00 00 d4 84 75 00 00 d5 00 d6 00 00 " +
        " d7 00 00 d8 00 00 d9 00 00 da 00 00 db 00 00 00 " +
        " 00 dc 00 00 00 00 dd 00 00 00 00 de 00 00 00 00" +
        " df 00 00 00 00 e2 00 00 00 00 e3 00 00 00 00 e4 " +
        " 00 00 00 00 e5 00 00 00 00 e6 00 00 00 00 e7 00 " +
        " 00 00 00 e8 00 00 00 00 e9 00 00 00 00 f0 00 00 " +
        " 00 00 f1 00 00 00 00 f2 00 00 00 00 f3 00 00 00 " +
        " 00 f4 00 00 00 00 f5 00 00 00 00 f6 00 00 00 00 " +
        " f7 00 00 00 00 f8 00 00 00 00 f9 00 00 00 00 19 " +
        " 01"

      expect("confirmation") as "02 19 01 "


      waitUntil(stored.size >= 2, 300)
      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "867459044367415",
          37.84764862060547,
          55.771446228027344,
          Date.from(ZonedDateTime.of(2019, 7, 8, 0, 0, 17, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          12,
          null: String,
          null: Date,
          Map("acc" -> 3822,
            "braking_accel" -> 0.0,
            "can16bitr0" -> 0.toShort,
            "can16bitr1" -> 0.toShort,
            "can16bitr2" -> 0.toShort,
            "can16bitr3" -> 0.toShort,
            "can16bitr4" -> 0.toShort,
            "can32bitr0" -> 0,
            "can32bitr1" -> 0,
            "can32bitr2" -> 0,
            "can32bitr3" -> 0,
            "can32bitr4" -> 0,
            "can8bitr0" -> 0.toByte,
            "can8bitr1" -> 0.toByte,
            "can8bitr10" -> 0.toByte,
            "can8bitr11" -> 0.toByte,
            "can8bitr12" -> 0.toByte,
            "can8bitr13" -> 0.toByte,
            "can8bitr14" -> 0.toByte,
            "can8bitr2" -> 0.toByte,
            "can8bitr3" -> 0.toByte,
            "can8bitr4" -> 0.toByte,
            "can8bitr5" -> 0.toByte,
            "can8bitr6" -> 0.toByte,
            "can8bitr7" -> 0.toByte,
            "can8bitr8" -> 0.toByte,
            "can8bitr9" -> 0.toByte,
            "can_a0" -> 0,
            "can_a1" -> 35584,
            "can_a2" -> 0,
            "can_a3" -> 0,
            "correctness" -> 0,
            "course_accel" -> 0.0,
            "fuel0" -> 0,
            "fuel1" -> 0,
            "fuel10" -> 0,
            "fuel11" -> 0,
            "fuel12" -> 0,
            "fuel13" -> 0,
            "fuel14" -> 0,
            "fuel15" -> 0,
            "fuel2" -> 0,
            "fuel3" -> 0,
            "fuel4" -> 0,
            "fuel5" -> 0,
            "fuel6" -> 0,
            "fuel7" -> 0,
            "fuel8" -> 0,
            "fuel9" -> 0,
            "ignition" -> 0,
            "in0" -> 0,
            "in1" -> 0,
            "in2" -> 0,
            "in3" -> 0,
            "in4" -> 0,
            "in5" -> 0,
            "inputs" -> "0",
            "outputs" -> "1101",
            "power" -> 11546,
            "protocol" -> "GalileoSky",
            "rs232_2" -> 0.toByte,
            "rs232_24" -> 0.toByte,
            "rs232_25" -> 0.toByte,
            "rs232_26" -> 0.toByte,
            "rs232_27" -> 0.toByte,
            "rs232_28" -> 0.toByte,
            "rs232_29" -> 0.toByte,
            "rs232_3" -> 0.toByte,
            "rs232_30" -> 0.toByte,
            "rs232_31" -> 0.toByte,
            "rs232_32" -> 0.toByte,
            "rs232_33" -> 0.toByte,
            "rs232_34" -> 0.toByte,
            "rs232_35" -> 0.toByte,
            "rs232_36" -> 0.toByte,
            "rs232_37" -> 0.toByte,
            "rs232_38" -> 0.toByte,
            "rs232_39" -> 0.toByte,
            "rs232_4" -> 0.toByte,
            "status" -> "11100000000000",
            "temp" -> 26.toByte,
            "turn_accel" -> 0.0,
            "userdata0" -> 0,
            "userdata1" -> 0,
            "userdata2" -> 0,
            "userdata3" -> 0,
            "userdata4" -> 0,
            "userdata5" -> 0,
            "userdata6" -> 0,
            "userdata7" -> 0,
            "vertical_accel" -> 0.0).mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "867459044367415",
          37.84764862060547,
          55.771446228027344,
          Date.from(ZonedDateTime.of(2019, 7, 8, 0, 0, 16, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          12,
          null: String,
          null: Date,
          Map("acc" -> 3822,
            "braking_accel" -> 0.0,
            "can16bitr0" -> 0.toShort,
            "can16bitr1" -> 0.toShort,
            "can16bitr2" -> 0.toShort,
            "can16bitr3" -> 0.toShort,
            "can16bitr4" -> 0.toShort,
            "can32bitr0" -> 0,
            "can32bitr1" -> 0,
            "can32bitr2" -> 0,
            "can32bitr3" -> 0,
            "can32bitr4" -> 0,
            "can8bitr0" -> 0.toByte,
            "can8bitr1" -> 0.toByte,
            "can8bitr10" -> 0.toByte,
            "can8bitr11" -> 0.toByte,
            "can8bitr12" -> 0.toByte,
            "can8bitr13" -> 0.toByte,
            "can8bitr14" -> 0.toByte,
            "can8bitr2" -> 0.toByte,
            "can8bitr3" -> 0.toByte,
            "can8bitr4" -> 0.toByte,
            "can8bitr5" -> 0.toByte,
            "can8bitr6" -> 0.toByte,
            "can8bitr7" -> 0.toByte,
            "can8bitr8" -> 0.toByte,
            "can8bitr9" -> 0.toByte,
            "can_a0" -> 0,
            "can_a1" -> 35584,
            "can_a2" -> 0,
            "can_a3" -> 0,
            "correctness" -> 0,
            "course_accel" -> 0.0,
            "fuel0" -> 0,
            "fuel1" -> 0,
            "fuel10" -> 0,
            "fuel11" -> 0,
            "fuel12" -> 0,
            "fuel13" -> 0,
            "fuel14" -> 0,
            "fuel15" -> 0,
            "fuel2" -> 0,
            "fuel3" -> 0,
            "fuel4" -> 0,
            "fuel5" -> 0,
            "fuel6" -> 0,
            "fuel7" -> 0,
            "fuel8" -> 0,
            "fuel9" -> 0,
            "ignition" -> 0,
            "in0" -> 10560,
            "in1" -> 0,
            "in2" -> 0,
            "in3" -> 0,
            "in4" -> 0,
            "in5" -> 0,
            "inputs" -> "1",
            "outputs" -> "1101",
            "power" -> 11567,
            "protocol" -> "GalileoSky",
            "rs232_2" -> 0.toByte,
            "rs232_24" -> 0.toByte,
            "rs232_25" -> 0.toByte,
            "rs232_26" -> 0.toByte,
            "rs232_27" -> 0.toByte,
            "rs232_28" -> 0.toByte,
            "rs232_29" -> 0.toByte,
            "rs232_3" -> 0.toByte,
            "rs232_30" -> 0.toByte,
            "rs232_31" -> 0.toByte,
            "rs232_32" -> 0.toByte,
            "rs232_33" -> 0.toByte,
            "rs232_34" -> 0.toByte,
            "rs232_35" -> 0.toByte,
            "rs232_36" -> 0.toByte,
            "rs232_37" -> 0.toByte,
            "rs232_38" -> 0.toByte,
            "rs232_39" -> 0.toByte,
            "rs232_4" -> 0.toByte,
            "status" -> "11100000000000",
            "temp" -> 26.toByte,
            "turn_accel" -> 0.0,
            "userdata0" -> 0,
            "userdata1" -> 0,
            "userdata2" -> 0,
            "userdata3" -> 0,
            "userdata4" -> 0,
            "userdata5" -> 0,
            "userdata6" -> 0,
            "userdata7" -> 0,
            "vertical_accel" -> 0.0).mapValues(_.asInstanceOf[AnyRef]).asJava))     )
    }
    

  }


}



