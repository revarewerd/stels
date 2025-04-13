package ru.sosgps.wayrecall.avlprotocols.arnavi

import java.nio.ByteOrder
import java.util.Date
import java.io.DataInputStream
import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import org.scalatest.FunSpec
import ru.sosgps.wayrecall.packreceiver.netty.{ArnaviServer, MicroMayakServer}
import ru.sosgps.wayrecall.packreceiver.{Netty4ReceiverServerFixture, ProtocolServer, ReceiverServerFixture}
import ru.sosgps.wayrecall.utils.io.Utils
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.sleepers.{LBS, LBSConverter, LBSLonLat}
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.{matchGPS, matchGPSsOrdered}

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.concurrent.duration.DurationLong

/**
  * Created by nickl on 15.01.17.
  */
class ArnaviServerTest extends FunSpec with Netty4ReceiverServerFixture {
  override protected def useServer = new ArnaviServer

  private def line(str: String) = str.getBytes ++ Array[Byte](0x0d, 0x0a)

  describe("ArnaviServer") {
    it("should answer messages and store data") {

      send("message") as line("$AV,V2,32768,12487,2277, 203, -1,0,0,193,0,0,1,13,200741,5950.6773N,03029.1043E,0.0,0.0,121012,*6E")
      expect("confurmation") as line("RCPTOK")

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "32768-12487",
          59.84462166666667,
          30.485071666666666,
          Date.from(ZonedDateTime.of(2012, 10, 13, 0, 7, 41, 0, ZoneId.of("Europe/Moscow")).toInstant),
          0,
          0,
          13,
          null: String,
          null: Date,
          Map("protocol" -> "Arnavi").mapValues(_.asInstanceOf[AnyRef]).asJava)))

    }

    it("should answer messages and store data partially sent") {

      val (part1, part2) =
        line("$AV,V3,999999,12487,2277,203,65534,0,0,193,65535,65535,65535,65535,1,13,200741,5950.6773N,03029.1043E,300.0,360.0,121012,65535,65535,65535,SF*6E")
          .splitAt(50)
      send("message") as part1
      send("notthing") as ""
      send("message") as part2
      expect("confurmation") as line("RCPTOK")

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "999999-12487",
          59.84462166666667,
          30.485071666666666,
          Date.from(ZonedDateTime.of(2012, 10, 13, 0, 7, 41, 0, ZoneId.of("Europe/Moscow")).toInstant),
          300,
          360,
          13,
          null: String,
          null: Date,
          Map("protocol" -> "Arnavi").mapValues(_.asInstanceOf[AnyRef]).asJava)))
    }


  }


}



