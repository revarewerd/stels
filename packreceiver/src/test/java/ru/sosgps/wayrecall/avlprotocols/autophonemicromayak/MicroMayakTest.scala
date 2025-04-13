package ru.sosgps.wayrecall.avlprotocols.autophonemicromayak

import java.io.DataInputStream
import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import org.scalatest.FunSpec
import ru.sosgps.wayrecall.packreceiver.netty.MicroMayakServer
import ru.sosgps.wayrecall.packreceiver.{Netty4ReceiverServerFixture, ProtocolServer, ReceiverServerFixture}
import ru.sosgps.wayrecall.utils.io.Utils
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.sleepers.{LBS, LBSConverter, LBSLonLat}
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPS

import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
  * Created by nmitropo on 12.8.2016.
  */
class MicroMayakTest extends FunSpec with Netty4ReceiverServerFixture {
  override protected def useServer = new MicroMayakServer(
    LBSConverter(Map(LBS(MCC = 250, MNC = 1, LAC = 0x232, CID = 0xd1b) -> LBSLonLat(37.770144, 55.748677, 1)))) {
    override protected def nowTime(): Long = ZonedDateTime.of(2010, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")).toInstant.toEpochMilli
  }


  describe("MikroMayakServer") {
    it("should authenticate and receive stations info and then block states") {
      send("auth query") as "24 02 00 03 58 94 50 40 45 50 85 65 0d"
      expect("authentication confirmation") as "24 04 00 4b 3d 3b 00 00 39 0d"

      send("block states") as "24 08 16 47 a1 b5 57 04 00 00 d0 12 1a 2a 11 80 15 1e 0d"
      expect("confirmation") as "24 00 58 08 a0 0d"

      send("states query") as "24 03 07 fa 10 00 32 02 1b 0d 00 00 00 90 0d"
      expect("confirmation") as "24 00 1d 03 e0 0d"

      send("stations info") as "24 05 11 fa 10 00 32 02 1b 0d 00 00 00 13 00 71 0d"
      expect("confirmation") as "24 00 44 05 b7 0d"

      stored.should(have).length(2)
      stored(0) should matchGPS(new GPSData(null: String,
        "358945040455085",
        37.770144,
        55.748677,
        new Date(1471521095000L) /* Thu Aug 18 14:51:35 EEST 2016 */ ,
        0,
        0,
        0,
        null: String,
        null: Date,
        Map("lbs" -> "LBShex(MCC=250, MNC=1, LAC=0x232, CID=0xd1b)",
          "protocol" -> "MicroMayak LBS").mapValues(_.asInstanceOf[AnyRef]).asJava))

      stored(1) should matchGPS(new GPSData(null: String,
        "358945040455085",
        37.770144,
        55.748677,
        new Date(1471521095000L) /* Thu Aug 18 14:51:35 EEST 2016 */ ,
        0,
        0,
        0,
        null: String,
        null: Date,
        Map("lbs" -> "LBShex(MCC=250, MNC=1, LAC=0x232, CID=0xd1b)",
          "protocol" -> "MicroMayak LBS").mapValues(_.asInstanceOf[AnyRef]).asJava))

    }

    it("should authenticate and block states") {
      send("auth query") as "24 02 00 03 58 94 50 40 45 50 85 65 0d"
      expect("authentication confirmation") as "24 04 00 4b 3d 3b 00 00 39 0d"

      send("block states") as "24 08 0f 1c 6b b2 57 04 00 00 da 32 1a 25 11 80 15 64 0d"
      expect("confirmation") as "24 00 3c 08 bc 0d"

      send("description") as "24 0c 04 00 03 10 01 00 dc 0d"
      expect("confirmation") as "24 00 10 0c e4 0d "

      send("block states") as "24 fc 00 00 00 00 00 04 0d"
      expect("confirmation") as "24 00 00 fc 04 0d"
    }

    it("should authenticate and accept GPRS") {
      send("auth query") as "24 02 00 03 58 94 50 40 45 50 85 65 0d"
      expect("authentication confirmation") as "24 04 00 4b 3d 3b 00 00 39 0d"

      //      send("gprs data") as "24 09 10 48 9e c5 57 ff 60 fe 61 9e 9c 15 00 90 40 00 08 0d "
      //      expect("confirmation") as "24 00 3c 08 bc 0d"

      send("gprs data") as "24 09 1c c2 9e c5 57 ff 60 fe 61 9e 9c 15 00 90 00 00 c2 0d"
      expect("confirmation") as "24 00 70 09 87 0d"

      debug("stored:" + stored)

      stored.should(have).length(1)
      stored.head should matchGPS(new GPSData(null: String,
        "358945040455085",
        37.76934333333333,
        55.746985,
        new Date(1472569026000L) /* Tue Aug 30 17:57:06 EEST 2016 */ ,
        0,
        0,
        0,
        null: String,
        null: Date,
        Map("protocol" -> "MicroMayak").mapValues(_.asInstanceOf[AnyRef]).asJava))


    }

    it("should fail with unknown code") {
      send("auth query") as "24 02 00 03 58 94 50 40 45 50 85 65 0d"
      expect("authentication confirmation") as "24 04 00 4b 3d 3b 00 00 39 0d"

      send("broken message") as "24 29 1c c2 9e c5 57 ff 60 fe 61 9e 9c 15 00 90 00 00 c2 0d"
      expect("disconnected") as "FF"

      assert(stored.isEmpty)

    }

  }


}


