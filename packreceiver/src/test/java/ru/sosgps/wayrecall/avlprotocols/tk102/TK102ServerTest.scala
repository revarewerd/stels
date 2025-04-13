package ru.sosgps.wayrecall.avlprotocols.tk102

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.Netty4ReceiverServerFixture
import ru.sosgps.wayrecall.packreceiver.netty.{ArnaviServer, TK102Server}
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPSsOrdered
import ru.sosgps.wayrecall.utils.io.ByteArrayOps
import ru.sosgps.wayrecall.utils.EventsTestUtils.waitUntil

import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
  * Created by nickl on 15.01.17.
  */
class TK102ServerTest extends FunSpec with Netty4ReceiverServerFixture {
  override protected def useServer = new TK102Server()



  describe("TK102") {

    it("should answer messages and store data1") {

      send("login") as "28 30 38 37 30 37 33 37 39 34 38 34 38 42 50 30 30 48 53 4f 29 "
      expect("confirmation") as "28 30 38 37 30 37 33 37 39 34 38 34 38 41 50 30 31 48 53 4f 29"


      send("data") as "28 30 38 37 30 37 33 37 39 34 38 34 38 42 52 30 30 31 37 30 33 31 36 41 35 35 34 35 2e 39 36 33  39 4e 30 33 37 34 39 2e 39 34 30 38 45 30 30 31 2e 36 31 34 30 30 33 37 30 30 30 2e 30 30 2c 30 30 30 30 30 30 30 30 4c 30 30 30 30 30 30 30 30 29 "

      expect("confirmation") as "28 30 38 37 30 37 33 37 39 34 38 34 38 41 52 30  33 29"

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
          "352887073794848",
          37.832346666666666,
          55.766065,
          Date.from(ZonedDateTime.of(2017, 3, 16, 17, 0, 37, 0, ZoneId.of("Europe/Moscow")).toInstant),
          1,
          0,
          9,
          null: String,
          null: Date,
          Map("protocol" -> "TK102").mapValues(_.asInstanceOf[AnyRef]).asJava),
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
          Map("protocol" -> "TK102").mapValues(_.asInstanceOf[AnyRef]).asJava)))

    }




  }


}



