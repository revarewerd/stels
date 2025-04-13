package ru.sosgps.wayrecall.avlprotocols.gtlt3mt1

import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.Netty4ReceiverServerFixture
import ru.sosgps.wayrecall.packreceiver.netty.{ArnaviServer, GTLT3MT1Server}
import ru.sosgps.wayrecall.scalatestutils.GPSMatcher.matchGPSsOrdered

import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
  * Created by nickl on 15.01.17.
  */
class GTLT3MT1ServerTest extends FunSpec with Netty4ReceiverServerFixture {
  override protected def useServer = new GTLT3MT1Server

  private def text(str: String) = str.getBytes

  describe("GTLT3MT1Server") {
    it("should answer messages and store data") {

      send("message") as text("*HQ,2020916012,V1,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#")
      expect("confirmation") as text("*HQ,2020916012,V1,050316#")

      stored should matchGPSsOrdered(Seq(
        new GPSData(null: String,
          "2020916012",
          113.77762333333334,
          22.214575,
          Date.from(ZonedDateTime.of(2002, 9, 22, 9, 3, 16, 0, ZoneId.of("Europe/Moscow")).toInstant),
          14,
          28,
          0,
          null: String,
          null: Date,
          Map("protocol" -> "GTLT3MT1").mapValues(_.asInstanceOf[AnyRef]).asJava)))

    }

  }


}



