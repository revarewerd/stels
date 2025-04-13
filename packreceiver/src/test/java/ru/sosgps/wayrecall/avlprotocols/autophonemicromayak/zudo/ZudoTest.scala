package ru.sosgps.wayrecall.avlprotocols.autophonemicromayak.zudo

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import org.scalatest.FunSpec
import org.scalatest.Matchers.have
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.Netty4ReceiverServerFixture
import ru.sosgps.wayrecall.packreceiver.netty.{MicroMayakServer, ZudoServer}
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPS
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData

import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPS
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPSsOrdered

import scala.collection.JavaConverters.mapAsJavaMapConverter
/**
  * Created by nickl-mac on 01.10.16.
  */
class ZudoTest extends FunSpec with Netty4ReceiverServerFixture {
  override protected def useServer = new ZudoServer


  describe("ZudoServer") {
    it("should accept some http data") {
      send("part of http") as "50 4f 53 54 20 6d 6f 6e 2f 67 70 72 73 2e 64 6c " +
        " 6c 3f 64 61 74 61 20 48 54 54 50 2f 31 2e 31 0d " +
        " 0a 48 6f 73 74 3a 20 39 31 2e 32 33 30 2e 32 31 " +
        " 35 2e 31 32 0d 0a 43 6f 6e 74 65 6e 74 2d 54 79 " +
        " 70 65 3a 20 61 70 70 6c 69 63 61 74 69 6f 6e 2f " +
        " 62 69 6e 61 72 79 0d 0a 43 6f 6e 74 65 6e 74 2d " +
        " 4c 65 6e 67 74 68 3a 20 38 37 0d 0a 0d 0a"

      send("continue") as "69 64 3d 38 39 37 30 31 30 31 30 30 38 35 32 34 " +
        " 39 31 39 33 32 36 35 26 62 69 6e 3d 32 05 49 00 " +
        " 81 ff 08 0f de 2a 67 0f c9 fe 8b 12 43 28 04 07 " +
        " 7c 02 00 cb 08 00 e2 2c 92 00 00 00 00 02 b2 a8 " +
        " 74 1a 87 00 00 00 00 00 00 00 00 00 00 cf 0f fe " +
        " 0f fe a4 85 2a 49 00"


      expect("OK") as "48 54 54 50 2f 31 2e 31 20 32 30 30 20 4f 4b 0d 0a 43 6f 6e 74 65 6e 74 2d 4c 65 6e 67 74 68 3a 20 38 0d 0a 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 6b 65 65 70 2d 61 6c 69 76 65 0d 0a 0d 0a 23 41 43 4b 23 32 49 00 "


      stored.should(have).length(1)
      stored(0) should matchGPS(new GPSData(null: String,
        "89701010085249193265",
        37.59525998433944,
        55.65356981017948,
        Date.from(ZonedDateTime.of(2016, 10, 1, 19, 0, 1, 0, ZoneId.of("Europe/Moscow")).toInstant),
        0,
        -30,
        -1,
        null: String,
        null: Date,
        Map("gpsTime" -> 576001.0f,
          "protocol" -> "Zudo",
          "week" -> 1916).mapValues(_.asInstanceOf[AnyRef]).asJava))


    }

    it("should accept some http data2") {
      send("part of http") as "50 4f 53 54 20 6d 6f 6e 2f 67 70 72 73 2e 64 6c " +
        " 6c 3f 64 61 74 61 20 48 54 54 50 2f 31 2e 31 0d " +
        " 0a 43 6f 6e 74 65 6e 74 2d 4c 65 6e 67 74 68 3a " +
        " 20 31 36 39 0d 0a 0d 0a"

      send("continue") as "69 64 3d 38 39 37 30 31 30 31 30 30 38 35 32 34 " +
        " 39 31 39 33 33 35 36 26 62 69 6e 3d 32 05 01 00 " +
        " 80 ff 09 0f de 64 01 0f ca 33 21 12 00 fe a0 07 " +
        " 7d 00 00 99 04 6d e2 3e 34 00 00 00 00 01 97 d1 " +
        " aa 14 f0 00 00 00 00 00 00 00 00 00 00 cf 0f df " +
        " 0f df a4 9d 36 4a ff 09 a5 8e b0 1f bc d0 00 00 " +
        " 98 04 6e e2 3e 34 00 00 00 00 01 97 d1 ab 14 f0 " +
        " 00 00 00 00 00 00 00 00 00 00 cf 0f df 0f df a4 " +
        " 9d 36 3c ff 09 a7 25 ad 06 ba f0 00 00 8f 04 6b " +
        " e2 3e 33 00 00 00 "

      send("continue") as "00 01 97 d1 ab 14 f0 00 00 00 00 00 00 00 00 00 00 cf 00"


      expect("OK") as "48 54 54 50 2f 31 2e 31 20 32 30 30 20 4f 4b 0d 0a 43 6f 6e 74 65 6e 74 2d 4c 65 6e 67 74 68 3a 20 38 0d 0a 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 6b 65 65 70 2d 61 6c 69 76 65 0d 0a 0d 0a 23 41 43 4b 23 32 01 00 "

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "89701010085249193356",
          37.77915599755287,
          55.855005749181494,
          Date.from(ZonedDateTime.of(2016, 10, 3, 16, 32, 26, 0, ZoneId.of("Europe/Moscow")).toInstant),
          109,
          -30,
          -1,
          null: String,
          null: Date,
          Map("gpsTime" -> 135146.0f,
            "protocol" -> "Zudo",
            "week" -> 1917).mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "89701010085249193356",
          37.82077575526034,
          55.88782480814341,
          Date.from(ZonedDateTime.of(2016, 10, 3, 16, 32, 35, 0, ZoneId.of("Europe/Moscow")).toInstant),
          110,
          -30,
          -1,
          null: String,
          null: Date,
          Map("gpsTime" -> 135155.25f,
            "protocol" -> "Zudo",
            "week" -> 1917).mapValues(_.asInstanceOf[AnyRef]).asJava)))
    }

    it("should accept some http data3") {
      send("part of http") as "50 4f 53 54 20 6d 6f 6e 2f 67 70 72 73 2e 64 6c " +
        " 6c 3f 64 61 74 61 20 48 54 54 50 2f 31 2e 31 0d " +
        " 0a 43 6f 6e 74 65 6e 74 2d 4c 65 6e 67 74 68 3a " +
        "20 31 36 39 0d 0a 0d 0a"

      send("continue") as "69 64 3d 38 39 37 30 31 30 31 30 30 38 35 32 34 " +
        " 39 31 39 33 33 35 36 26 62 69 6e 3d 32 05 00 00 " +
        " 80 ff 09 0f de 63 b1 0f ca 14 6c 12 00 d9 a0 07 " +
        " 7d 00 00 95 04 44 bb 3e 29 00 00 00 00 06 44 0c " +
        " 52 14 ef 00 00 00 00 00 00 00 00 00 00 cf 0f df " +
        " 0f df a4 9c 35 49 ff 09 a4 9b 94 c3 97 c0 00 00 " +
        " 94 04 00 b9 3e 29 00 00 00 00 06 42 76 1e 14 ef " +
        " 00 00 00 00 00 00 00 00 00 00 cf 0f df 0f df a4 " +
        " 9c 35 49 ff 09 5b 44 95 e0 00 00 93 04 00 b9 3e " +
        " 29 00 00 00 00 00 00 00 00 14 ef 00 00 00 00 00 " +
        " 00 00 00 00 00 cf 0f df 00 "


      expect("OK") as "48 54 54 50 2f 31 2e 31 20 32 30 30 20 4f 4b 0d 0a 43 6f 6e 74 65 6e 74 2d 4c 65 6e 67 74 68 3a 20 38 0d 0a 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 6b 65 65 70 2d 61 6c 69 76 65 0d 0a 0d 0a 23 41 43 4b 23 32 00 00 "

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "89701010085249193356",
          37.67177176342193,
          55.85391291891429,
          Date.from(ZonedDateTime.of(2016, 10, 3, 16, 22, 34, 0, ZoneId.of("Europe/Moscow")).toInstant),
          68,
          -69,
          -1,
          null: String,
          null: Date,
          Map("gpsTime" -> 134554.0f,
            "protocol" -> "Zudo",
            "week" -> 1917).mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "89701010085249193356",
          37.786365262221814,
          55.880000826449155,
          Date.from(ZonedDateTime.of(2016, 10, 3, 16, 32, 15, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          -71,
          -1,
          null: String,
          null: Date,
          Map("gpsTime" -> 135135.0f,
            "protocol" -> "Zudo",
            "week" -> 1917).mapValues(_.asInstanceOf[AnyRef]).asJava))
      )



    }

    it("should accept some http with non http-compatible binary data") {
      send("part of http") as "50 4f 53 54 20 6d 6f 6e 2f 67 70 72 73 2e 64 6c " +
        " 6c 3f 64 61 74 61 20 48 54 54 50 2f 31 2e 31 0d " +
        " 0a 48 6f 73 74 3a 20 39 31 2e 32 33 30 2e 32 31" +
        " 35 2e 31 32 0d 0a 43 6f 6e 74 65 6e 74 2d 54 79 " +
        " 70 65 3a 20 61 70 70 6c 69 63 61 74 69 6f 6e 2f " +
        " 62 69 6e 61 72 79 0d 0a 43 6f 6e 74 65 6e 74 2d " +
        " 4c 65 6e 67 74 68 3a 20 38 37 0d 0a 0d 0a"

      send("continue") as "69 64 3d 38 39 37 30 31 30 31 30 30 36 35 30 34 " +
        " 35 38 38 35 37 32 38 26 62 69 6e 3d 32 05 e9 00 " +
        " 81 ff 09 0f de 45 0d 0f ca 2f 2f 12 3b 91 18 07 " +
        " 86 00 00 0d 08 03 ba 2c 91 00 00 00 00 02 32 41 " +
        " 22 1a 92 00 00 00 00 00 00 00 00 00 00 cf 0f fe " +
        " 0f fe a4 b8 29 33 00 "


      expect("OK") as "48 54 54 50 2f 31 2e 31 20 32 30 30 20 4f 4b 0d 0a 43 6f 6e 74 65 6e 74 2d 4c 65 6e 67 74 68 3a 20 38 0d 0a 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 6b 65 65 70 2d 61 6c 69 76 65 0d 0a 0d 0a 23 41 43 4b 23 32 e9 00 "

      stored.should(have).length(1)
      stored(0) should matchGPS(new GPSData(null: String,
        "89701010065045885728",
        37.76535901542944,
        55.746760911215134,
        Date.from(ZonedDateTime.of(2016, 12, 9, 18, 32, 51, 0, ZoneId.of("Europe/Moscow")).toInstant),
        3,
        -70,
        -1,
        null: String,
        null: Date,
        Map("gpsTime" -> 487971.0f,
          "protocol" -> "Zudo",
          "week" -> 1926).mapValues(_.asInstanceOf[AnyRef]).asJava))

    }

    it("should accept some http data5") {
      send("part of http") as "50 4f 53 54 20 6d 6f 6e 2f 67 70 72 73 2e 64 6c " +
        " 6c 3f 64 61 74 61 20 48 54 54 50 2f 31 2e 31 0d " +
        " 0a 48 6f 73 74 3a 20 39 31 2e 32 33 30 2e 32 31 " +
        " 35 2e 31 32 0d 0a 43 6f 6e 74 65 6e 74 2d 54 79 " +
        " 70 65 3a 20 61 70 70 6c 69 63 61 74 69 6f 6e 2f " +
        " 62 69 6e 61 72 79 0d 0a 43 6f 6e 74 65 6e 74 2d " +
        " 4c 65 6e 67 74 68 3a 20 38 37 0d 0a 0d 0a"

      send("continue") as "69 64 3d 38 39 37 30 31 30 31 30 30 36 35 30 34 " +
        " 35 38 38 35 37 32 38 26 62 69 6e 3d 32 05 0e 00 " +
        " 81 ff 08 0f de 45 12 0f ca 30 4b 11 ff 47 e0 07 " +
        " 87 02 00 ff 08 00 00 2c 98 00 00 00 00 02 32 41 " +
        " 22 1a 92 00 00 00 00 00 00 00 00 00 00 cf 0f fe " +
        " 0f fe a4 b6 2b 30 00 "


      expect("OK") as "48 54 54 50 2f 31 2e 31 20 32 30 30 20 4f 4b 0d 0a 43 6f 6e 74 65 6e 74 2d 4c 65 6e 67 74 68 3a 20 38 0d 0a 43 6f 6e 6e 65 63 74 69 6f 6e 3a 20 6b 65 65 70 2d 61 6c 69 76 65 0d 0a 0d 0a 23 41 43 4b 23 32 0e 00 "

      stored.should(have).length(1)
      stored(0) should matchGPS(new GPSData(null: String,
        "89701010065045885728",
        37.76923856287801,
        55.74682921310683,
        Date.from(ZonedDateTime.of(2016, 12, 12, 14, 59, 59, 0, ZoneId.of("Europe/Moscow")).toInstant),
        0,
        0,
        -1,
        null: String,
        null: Date,
        Map("gpsTime" -> 129599.0f,
          "protocol" -> "Zudo",
          "week" -> 1927).mapValues(_.asInstanceOf[AnyRef]).asJava))
    }

  }

}