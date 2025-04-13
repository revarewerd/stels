package ru.sosgps.wayrecall.avlprotocols.tk103

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import com.google.common.base.Charsets
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.Netty4ReceiverServerFixture
import ru.sosgps.wayrecall.packreceiver.netty.{TK102Server, TK103Server}
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPSsOrdered
import ru.sosgps.wayrecall.utils.EventsTestUtils.waitUntil

import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
  * Created by nickl on 15.01.17.
  */
class TK103ServerTest extends FunSpec with Netty4ReceiverServerFixture {
  override protected def useServer = new TK103Server()

  private def text(str: String) = str.getBytes(Charsets.US_ASCII)

  describe("TK103") {

    it("should answer messages and store data1") {

      send("login") as "28 30 38 37 30 37 33 37 39 34 38 34 38 42 50 30 30 48 53 4f 29 "
      expect("confirmation") as "28 30 38 37 30 37 33 37 39 34 38 34 38 41 50 30 31 48 53 4f 29"


      send("data") as text("(013632782450BR00080612A2232.9828N11404.9297E000.0022828000.0000000000L000230ED)")

      expect("confirmation") as "28 30 31 33 36 33 32 37 38 32 34 35 30 41 52 30 33 29"

      send("data") as "28 30 38 37 30 37 33 37 39 34 38 34 38 42 52 30 " +
        " 30 31 37 30 33 31 36 56 35 35 34 34 2e 38 32 38 " +
        " 38 4e 30 33 37 34 36 2e 31 36 32 38 45 30 31 34 " +
        " 2e 31 31 35 30 32 30 38 32 33 30 2e 36 34 2c 30 " +
        " 30 30 30 30 30 30 30 4c 30 30 30 30 30 30 30 30 " +
        " 29 "

      expect("confirmation") as "28 30 38 37 30 37 33 37 39 34 38 34 38 41 52 30  33 29"

      waitUntil(stored.size >= 2, 300)

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "352813632782450",
          114.08216166666666,
          22.549713333333333,
          Date.from(ZonedDateTime.of(2008, 6, 12, 6, 28, 28, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          9,
          null: String,
          null: Date,
          Map("protocol" -> "TK103").mapValues(_.asInstanceOf[AnyRef]).asJava),
        new GPSData(null: String,
          "352887073794848",
          37.76938,
          55.747146666666666,
          Date.from(ZonedDateTime.of(2017, 3, 16, 18, 2, 8, 0, ZoneId.of("Europe/Moscow")).toInstant),
          14,
          230,
          9,
          null: String,
          null: Date,
          Map("protocol" -> "TK103").mapValues(_.asInstanceOf[AnyRef]).asJava)))

    }




  }


}



