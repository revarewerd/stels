package ru.sosgps.wayrecall.avlprotocols.dtm

import java.util.Date

import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.avlprotocols.autophonemayak.AutophoneMayak7
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.testutils.Matchers._
import ru.sosgps.wayrecall.utils.io.Utils
import org.hamcrest.CoreMatchers._
import org.junit.Assert._
import ru.sosgps.wayrecall.utils.io.Utils._

import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
  * Created by nmitropo on 9.1.2016.
  */
class DTMTest {

  @Test
  def headerParsing1(): Unit = {
    val imei = DTM.parseHeader(Utils.asByteBuffer("FF 22 F3 0C 45 F5 C9 0F 03 00"))
    assertThat(imei, is("861785007918323"))
  }

  @Test
  def headerParsingR(): Unit = {
    val imei = DTM.parseHeader(Utils.asByteBuffer("ff 23 ea fb 61 ac bc 15 03 00"))
    assertThat(imei, is("868325020269546"))
  }


  @Test
  def testPackFromDoc(): Unit = {
    val packet = asByteBuffer("" +
      "5B " + // beginning PACKAGE" +
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
    val (_, gPSData) = DTM.readPackage(packet)
    assertThat(packet.readableBytes(), is(0))
    assertThat(gPSData(0), containsDataOf(new GPSData(null: String,
      null: String,
      37.209075927734375,
      55.63603591918945,
      new Date(1371721669000L) /* Thu Jun 20 13:47:49 MSK 2013 */ ,
      0,
      2,
      10,
      null: String,
      null: Date,
      Map("IDKey" -> 1691148297,
        "height" -> 220,
        "protocol" -> "DTM").mapValues(_.asInstanceOf[AnyRef]).asJava))
    )
    assertThat(gPSData(1), containsDataOf(new GPSData(null: String,
      null: String,
      37.209075927734375,
      55.63603591918945,
      new Date(1371721819000L) /* Thu Jun 20 13:50:19 MSK 2013 */ ,
      0,
      2,
      10,
      null: String,
      null: Date,
      Map("IDKey" -> 1691148297,
        "height" -> 220,
        "protocol" -> "DTM").mapValues(_.asInstanceOf[AnyRef]).asJava))
    )
  }

  @Test
  def testPack1(): Unit = {
    val packet = asByteBuffer("5b 01 01 1e 00 d8 3f 96 56 03 6f d3 5e 42" +
      " 04 42 d9 15 42 05 aa 12 77 00 09 00 f0 c4 83 5b 00 00  00 00 5c 00 00 00 00 8d 5d"
    )
    val (_, gPSData) = DTM.readPackage(packet)
    assertThat(packet.readableBytes(), is(0))
    assertThat(gPSData(0), containsDataOf(new GPSData(null: String,
      null: String,
      37.46216583251953,
      55.706478118896484,
      new Date(1452687320000L) /* Wed Jan 13 15:15:20 MSK 2016 */,
      0,
      85,
      14,
      null: String,
      null: Date,
      Map("91" -> 0,
        "92" -> 0,
        "gsm_st" -> 3,
        "height" -> 180,
        "ignition" -> 0,
        "in1" -> 0,
        "in2" -> 0,
        "in3" -> 0,
        "in4" -> 0,
        "in5" -> 0,
        "in6" -> 0,
        "in7" -> 0,
        "in8" -> 0,
        "mw" -> 0,
        "nav_st" -> 3,
        "out1" -> 0,
        "out2" -> 0,
        "out3" -> 0,
        "out4" -> 0,
        "protocol" -> "DTM",
        "pwr_ext" -> 19650,
        "pwr_in" -> 4.1,
        "sim_t" -> 1,
        "st0" -> 0,
        "st1" -> 0,
        "st2" -> 0).mapValues(_.asInstanceOf[AnyRef]).asJava)
    ))
  }

}
