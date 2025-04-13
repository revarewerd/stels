package ru.sosgps.wayrecall.avlprotocols.gl06

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import com.google.common.base.Charsets
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.Netty4ReceiverServerFixture
import ru.sosgps.wayrecall.packreceiver.netty.GL06Server
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPSsOrdered
import ru.sosgps.wayrecall.utils.EventsTestUtils.waitUntil

import scala.collection.JavaConverters.mapAsJavaMapConverter


class GL06ServerTest extends FunSpec with Netty4ReceiverServerFixture {
  override protected def useServer = new GL06Server()

  private def text(str: String) = str.getBytes(Charsets.US_ASCII)

  describe("GL06") {

    it("should answer messages and store data1") {

      send("login") as "78 78 0d 01 06 90 21 80 22 80 46 63 00 01 7c ad 0d 0a "
      expect("confirmation") as "78 78 05 01 00 01 D9 DC 0D 0A"


      //      send("data") as "78 78 1F 12 0B 08 1D 11 2E 10 CC 02 7A C7 EB 0C 46 58 49 00 14 8F 01 CC 00 28 7D 00 1F B8 00 03 80 81 0D 0A"
      send("data") as "78 78 21 12 12 04 10 15 1b 2e c8 05 fa 88 10 04" +
        " 04 b1 8e 00 14 00 00 fa 01 03 35 00 0c 22 00 02 " +
        " 00 08 f2 9b 0d 0a 78 78 0b 13 44 64 04 02 00 00 " +
        " 00 09 74 92 0d 0a"
      expect("confirmation") as "78 78 05 13 00 00 f8 78 0d 0a"
      send("headbeat") as "78 78 0A 13 4B 04 03 00 01 00 11 06 1F 0D 0A"

      expect("confirmation") as "78 78 05 13 00 11 F9 70 0D 0A"

      waitUntil(stored.size >= 1, 300)

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "690218022804663",
          37.45359,
          55.72495111111111,
          Date.from(ZonedDateTime.of(2018, 4, 17, 0, 27, 46, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          -56,
          null: String,
          null: Date,
          Map("CID" -> 3106,
            "LAC" -> 821,
            "MCC" -> 250,
            "MNC" -> 1.toShort).mapValues(_.asInstanceOf[AnyRef]).asJava)))

    }


  }


}



