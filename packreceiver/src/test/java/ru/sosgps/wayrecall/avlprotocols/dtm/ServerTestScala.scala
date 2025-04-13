package ru.sosgps.wayrecall.avlprotocols.dtm

import java.net.Socket

import io.netty.channel.nio.NioEventLoopGroup
import org.junit.{AfterClass, BeforeClass}
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.{Netty4ReceiverServerFixture, Netty4Server, TrimmedPackProcessor}
import ru.sosgps.wayrecall.packreceiver.netty.{SkysimNettyServer, DTMServer}
import ru.sosgps.wayrecall.testutils.Talk
import ru.sosgps.wayrecall.utils.stubs.CollectionPacketWriter

import scala.collection.mutable.ListBuffer

/**
  * Created by nmitropo on 10.1.2016.
  */

@RunWith(classOf[JUnitRunner])
class ServerTestScala extends FunSpec with Netty4ReceiverServerFixture {

  override val useServer = new DTMServer

  describe("DTMServer") {
    it("should authenticate and receive gps data") {
      send("header with imei, part1") as "FF 22 00 03 0F "
      send("header with imei, part2") as "               C9 F5 45 0C F3"
      expect("confirmation") as "7B 00 00 7D"

      send("set of packages with data") as ("5B " + // beginning PACKAGE" +
        "   01 " + // parcel number from 0x01 to 0xFB" +
        "      01 " + // type PACKET (0x00-PING, 0x01-DATA, 0x03 - TEXT, 0x04 - FILE)" +
        "          55 00 " + // Length of the data packet (DATA length)" +
        "                 C5 CF C2 51 " + // unixtime" +
        "                    03 4D 8B 5E 42 " + // TAG 0x03" +
        "                    04 18 D6 14 42" +
        "                    05 04 16 0A 00" +
        "                    02 09 E0 CC 64" +
        "                    15 F5 01 00 00" +
        "                    20 00 00 00 00" +
        "                    24 00 00 00 00" +
        "                    2A 3A 00 00 CD" +
        "                    CD 00 2C 71 00" +
        "                    2D 2E 00 00 CD" +
        "                    2E 6C CD 00 00" +
        "                    2F 3F 00 00 CD" +
        "                    30 4D 00 00 CD" +
        "                    CD 31 46 00 00" +
        "                    FA F8 01 00 00" +
        "                    FA F8 01 00 00" +
        "                    FA 90 01 00 00 " + // TAG 0xFA" +
        "                    62 " + // CRC, followed by the next PACKET" +
        "      01 " + // type PACKET (0x00-PING, 0x01-DATA, 0x03 - TEXT, 0x04 - FILE)" +
        "          50 00 " + // Length of the data packet (DATA length)" +
        "                5B D0 C2 51 " + // unixtime" +
        "                    03 4D 8B 5E 42" +
        "                    04 18 D6 14 42" +
        "                    05 05 16 0A 00" +
        "                    02 09 E0 CC 64" +
        "                    15 F5 01 00 00" +
        "                    20 00 00 00 00" +
        "                    24 00 00 00 00" +
        "                    2A 3A 00 00 CD" +
        "                    CD 00 2C 71 00" +
        "                    2D 2E 00 00 CD" +
        "                    2E 6C CD 00 00" +
        "                    2F 3F 00 00 CD" +
        "                    30 4D 00 00 CD" +
        "                    CD 31 46 00 00" +
        "                    FA F8 01 00 00" +
        "                    FA F8 01 00 00" +
        "                    6E " + // CRC" +
        " 5D " + // end PACKAGE" +
        ""
        )

      expect("confirmation") as "7B 00 02 7D"
    }

    it("should authenticate and receive gps data (sample from real)") {
      send("header with imei") as "FF 22 00 03 0F C9 F5 45 0C F3"
      expect("confirmation") as "7B 00 00 7D"

      send("gps data") as "5b 01 01 32 00 95 bd ca 56 03 eb bc 2c 42 04 b7 " +
        "92 33 42 05 00 41 66 00 07 16 5f 10 ef 08 63 fa " +
        " 00 0e 09 00 f0 c4 51 15 00 00 00 00 21 07 00 00 " +
        " 00 5b 00 00 00 00 5c 00 00 00 00 e8 5d"

      debug("stored = " + this.stored)

      expect("confirmation") as "7B 00 01 7D"

    }

    it("should authenticate and receive gps data (sample from real2)") {
      send("header with imei") as "FF 22 00 03 0F C9 F5 45 0C F3"
      expect("confirmation") as "7B 00 00 7D"

      send("gps data") as "5b 01 01 32 00 14 ab cc 56 03 e2 a3 2c 42 04 22 " +
        " 3c 33 42 05 00 3b 66 00 07 5b 61 10 ef 08 63 fa " +
        " 00 03 09 00 f0 c4 54 15 00 00 00 00 21 0a 00 00 " +
        " 00 5b 00 00 00 00 5c 00 00 00 00 86 5d "

      debug("stored = " + this.stored)

      expect("confirmation") as "7B 00 01 7D"

    }

    it("should send commands to connected device if any") {
      send("header with imei") as "FF 22 00 03 0F C9 F5 45 0C F3"
      expect("confirmation") as "7B 00 00 7D"

      useServer.activeConnections("17513449972879524608").command(0x09, 0x00)
      expect("command to device") as "7b 02 ff 09 09 00 7d"

    }

  }


}



