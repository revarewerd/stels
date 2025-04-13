package ru.sosgps.wayrecall.avlprotocols.skysim

import java.util.Date

import io.netty.channel.nio.NioEventLoopGroup
import org.junit._
import ru.sosgps.wayrecall.core.{GPSDataConversions, GPSData}
import ru.sosgps.wayrecall.packreceiver.{Netty4Server, TrimmedPackProcessor, DummyPackSaver}
import ru.sosgps.wayrecall.packreceiver.netty.{SkysimNettyServer, NavTelecomNettyServer}
import ru.sosgps.wayrecall.testutils.TalkTestSetup
import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.io.Utils
import ru.sosgps.wayrecall.utils.stubs.{CollectionPacketWriter, InMemoryPacketsWriter}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
 * Created by nickl on 17.04.15.
 */
class ServerTest extends grizzled.slf4j.Logging with TalkTestSetup {

  val serverPort: Int = ServerTest.SKYSIM_TEST_PORT

  import talk._


  @Before
  def clearGPSData(): Unit = {
    ServerTest.packWriter.synchronized(ServerTest.packWriter.collection.clear())
  }

  @Test
  def testMessage() {

    send("FF 22 F3 0C 45 F5 C9 0F 03 00")

    expect("7B 00 00 7D")

    send(asBytes( """|5B     // начало PACKAGE
                    |01  // номер посылки от 0x01 до 0xFB
                    |01  // тип PACKET (0x00-PING, 0x01-DATA, 0x03 - TEXT, 0x04 - FILE)
                    |55 00  // длина пакета данных (длина DATA)
                    |C5 CF C2 51  // unixtime
                    |03 4D 8B 5E 42  // TAG 0x03
                    |04 18 D6 14 42
                    |05 05 16 0A 00
                    |09 02 E0 CC 64
                    |15 F5 01 00 00"""))
    send(asBytes( """|20 00 00 00 00
                    |24 00 00 00 00
                    |2A 3A CD 00 00
                    |2C 71 CD 00 00
                    |2D 2E CD 00 00
                    |2E 6C CD 00 00
                    |2F 3F CD 00 00
                    |30 4D CD 00 00
                    |31 46 CD 00 00"""))
    send(asBytes( """|FA F8 01 00 00
                    |FA F8 01 00 00
                    |FA 90 01 00 00  // TAG 0xFA
                    |62  // CRC, далее идет следующий PACKET
                    |01 // тип PACKET (0x00-PING, 0x01-DATA, 0x03 - TEXT, 0x04 - FILE)
                    |50 00  // длина пакета данных (длина DATA)
                    |5B D0 C2 51 // unixtime
                    |03 4D 8B 5E 42
                    |04 18 D6 14 42
                    |05 05 16 0A 00
                    |09 02 E0 CC 64
                    |15 F5 01 00 00"""))
    send(asBytes( """|20 00 00 00 00
                    |24 00 00 00 00
                    |2A 3A CD 00 00
                    |2C 71 CD 00 00
                    |2D 2E CD 00 00
                    |2E 6C CD 00 00
                    |2F 3F CD 00 00
                    |30 4D CD 00 00"""))
    send(asBytes( """|31 46 CD 00 00
                    |FA F8 01 00 00
                    |FA F8 01 00 00
                    |6E // CRC
                    |5D  // конец PACKAGE
                  """))

    expect("7B 00 01 7D")
    //debug(ServerTest.packWriter.collection.sortBy(_.time).map(GPSDataConversions.toScalaCode).mkString("ListBuffer(",",\n",")"))
    Assert.assertEquals(ListBuffer(new GPSData(
      null: String,
      "861785007918323",
      37.209075927734375,
      55.63603591918945,
      new Date(1371721669000L),
      5,
      0,
      7,
      null: String,
      null: Date,
      Map(
        "IN1" -> 1,
        "gsm_st" -> 2,
        "height" -> 10.toShort,
        "info" -> 400,
        "key21" -> 501,
        "key32" -> 0,
        "key36" -> 0,
        "key42" -> 52538,
        "key44" -> 52593,
        "key45" -> 52526,
        "key46" -> 52588,
        "key47" -> 52543,
        "key48" -> 52557,
        "key49" -> 52550,
        "mw" -> 0,
        "nav_st" -> 3,
        "protocol" -> "Skysim",
        "protocolversion" -> 34,
        "pwr_ext" -> 100,
        "pwr_in" -> 3,
        "sim_in" -> 1,
        "sim_t" -> 0,
        "st0" -> 1,
        "st1" -> 0,
        "st2" -> 0).mapValues(_.asInstanceOf[AnyRef]).asJava
    ),
      new GPSData(
        null: String,
        "861785007918323",
        37.209075927734375,
        55.63603591918945,
        new Date(1371721819000L),
        5,
        0,
        7,
        null: String,
        null: Date,
        Map(
          "IN1" -> 1,
          "gsm_st" -> 2,
          "height" -> 10.toShort,
          "info" -> 504,
          "key21" -> 501,
          "key32" -> 0,
          "key36" -> 0,
          "key42" -> 52538,
          "key44" -> 52593,
          "key45" -> 52526,
          "key46" -> 52588,
          "key47" -> 52543,
          "key48" -> 52557,
          "key49" -> 52550,
          "mw" -> 0,
          "nav_st" -> 3,
          "protocol" -> "Skysim",
          "protocolversion" -> 34,
          "pwr_ext" -> 100,
          "pwr_in" -> 3,
          "sim_in" -> 1,
          "sim_t" -> 0,
          "st0" -> 1,
          "st1" -> 0,
          "st2" -> 0).mapValues(_.asInstanceOf[AnyRef]).asJava
      )),
      ServerTest.packWriter.collection.sortBy(_.time))

  }

  @Test
  def testTalk() {

    send("ff 22 ab 51 37 61 f5 10 03 00")
    expect("7b 00 00 7d")
    send("5b 16 01 23 00 5b 05 35 55 03 11 fd 5e 42 04 2f " +
      " 13 17 42 05 00 10 66 00 09 00 d0 c4 5d 08 01 fa " +
      " 00 14 01 88 10 ae 35 fa f4 01 00 00 31 5d ")
    expect("7b 00 16 7d")
    send("5b 17 01 1e 00 06 3e 1d 55 03 d3 fc 5e 42 04 8f " +
      " 13 17 42 05 00 14 0c 00 09 00 d0 c4 5f 01 df 0f " +
      " 7b 37 fa 2c 01 00 00 10 01 14 00 06 3e 1d 55 fc " +
      "7f 5e 00 00 fd 8f 90 c8 29 fe 96 24 f4 31 ff ed " +
      "0d 00 00 72 01 23 00 7e 3e 1d 55 03 d3 fc 5e 42 " +
      "04 8f 13 17 42 05 00 13 0b 00 09 00 d0 c4 5f 08 " +
      "01 fa 00 13 01 4b 11 da 37 fa 1e 00 00 00 5a 01 " +
      "23 00 f6 3e 1d 55 03 d3 fc 5e 42 04 8f 13 17 42 " +
      "05 00 12 0b 00 09 00 d0 c4 5e 08 01 fa 00 11 01 " +
      "27 10 77 36 fa 90 01 00 00 b8 01 1e 00 6e 3f 1d " +
      "55 03 d3 fc 5e 42 04 8f 13 17 42 05 00 13 0b 00 " +
      "09 00 d0 c4 5e 01 c6 0f b6 37 fa 46 00 00 00 b1 " +
      "01 23 00 e6 3f 1d 55 03 d3 fc 5e 42 04 8f 13 17 " +
      "42 05 00 13 0c 00 09 00 d0 c4 5e 08 01 fa 00 11 " +
      " 01 3c 11 9f 37 fa 1f 00 00 00 78 5d")
    expect("7b 00 17 7d")

    EventsTestUtils.waitUntil(ServerTest.packWriter.collection.size >= 7, 2000)

    //debug(ServerTest.packWriter.collection.sortBy(_.time).map(GPSDataConversions.toScalaCode).mkString("ListBuffer(",",\n",")"))

    Assert.assertEquals(ListBuffer(new GPSData(
      null: String,
      "863071014179243",
      37.769100189208984,
      55.74689865112305,
      new Date(1427979782000L),
      0,
      0,
      5,
      null: String,
      null: Date,
      Map(
        "gsm_st" -> 1,
        "height" -> 12.toShort,
        "info" -> 300,
        "mw" -> 0,
        "nav_st" -> 3,
        "protocol" -> "Skysim",
        "protocolversion" -> 34,
        "pwr_ext" -> 95,
        "pwr_in" -> 3,
        "sim_in" -> 1,
        "sim_t" -> 0,
        "st0" -> 0,
        "st1" -> 0,
        "st2" -> 0,
        "vol1" -> 4063,
        "vol2" -> 14203).mapValues(_.asInstanceOf[AnyRef]).asJava
    ),
      new GPSData(
        null: String,
        "863071014179243",
        Double.NaN,
        Double.NaN,
        new Date(1427979782000L),
        0,
        0,
        0,
        null: String,
        null: Date,
        Map("key252" -> 24191,
          "key253" -> 701010063,
          "key254" -> 838083734,
          "key255" -> 3565,
          "protocol" -> "Skysim",
          "protocolversion" -> 34).mapValues(_.asInstanceOf[AnyRef]).asJava
      ),
      new GPSData(
        null: String,
        "863071014179243",
        37.769100189208984,
        55.74689865112305,
        new Date(1427979902000L),
        0,
        0,
        4,
        null: String,
        null: Date,
        Map(
          "MCC" -> 250,
          "MNC" -> 19.toShort,
          "gsm_st" -> 1,
          "gsmlevel" -> 1.toShort,
          "height" -> 11.toShort,
          "info" -> 30,
          "mw" -> 0,
          "nav_st" -> 3,
          "protocol" -> "Skysim",
          "protocolversion" -> 34,
          "pwr_ext" -> 95,
          "pwr_in" -> 3,
          "sim_in" -> 1,
          "sim_t" -> 0,
          "st0" -> 0,
          "st1" -> 0,
          "st2" -> 0,
          "vol1" -> 4427,
          "vol2" -> 14298).mapValues(_.asInstanceOf[AnyRef]).asJava
      ),
      new GPSData(
        null: String,
        "863071014179243",
        37.769100189208984,
        55.74689865112305,
        new Date(1427980022000L),
        0,
        0,
        3,
        null: String,
        null: Date,
        Map(
          "MCC" -> 250,
          "MNC" -> 17.toShort,
          "gsm_st" -> 1,
          "gsmlevel" -> 1.toShort,
          "height" -> 11.toShort,
          "info" -> 400,
          "mw" -> 0,
          "nav_st" -> 3,
          "protocol" -> "Skysim",
          "protocolversion" -> 34,
          "pwr_ext" -> 94,
          "pwr_in" -> 3,
          "sim_in" -> 1,
          "sim_t" -> 0,
          "st0" -> 0,
          "st1" -> 0,
          "st2" -> 0,
          "vol1" -> 4135,
          "vol2" -> 13943).mapValues(_.asInstanceOf[AnyRef]).asJava
      ),
      new GPSData(
        null: String,
        "863071014179243",
        37.769100189208984,
        55.74689865112305,
        new Date(1427980142000L),
        0,
        0,
        4,
        null: String,
        null: Date,
        Map(
          "gsm_st" -> 1,
          "height" -> 11.toShort,
          "info" -> 70,
          "mw" -> 0,
          "nav_st" -> 3,
          "protocol" -> "Skysim",
          "protocolversion" -> 34,
          "pwr_ext" -> 94,
          "pwr_in" -> 3,
          "sim_in" -> 1,
          "sim_t" -> 0,
          "st0" -> 0,
          "st1" -> 0,
          "st2" -> 0,
          "vol1" -> 4038,
          "vol2" -> 14262).mapValues(_.asInstanceOf[AnyRef]).asJava
      ),
      new GPSData(
        null: String,
        "863071014179243",
        37.769100189208984,
        55.74689865112305,
        new Date(1427980262000L),
        0,
        0,
        4,
        null: String,
        null: Date,
        Map(
          "MCC" -> 250,
          "MNC" -> 17.toShort,
          "gsm_st" -> 1,
          "gsmlevel" -> 1.toShort,
          "height" -> 12.toShort,
          "info" -> 31,
          "mw" -> 0,
          "nav_st" -> 3,
          "protocol" -> "Skysim",
          "protocolversion" -> 34,
          "pwr_ext" -> 94,
          "pwr_in" -> 3,
          "sim_in" -> 1,
          "sim_t" -> 0,
          "st0" -> 0,
          "st1" -> 0,
          "st2" -> 0,
          "vol1" -> 4412,
          "vol2" -> 14239).mapValues(_.asInstanceOf[AnyRef]).asJava
      ),
      new GPSData(
        null: String,
        "863071014179243",
        37.768733978271484,
        55.747135162353516,
        new Date(1429538139000L),
        0,
        0,
        1,
        null: String,
        null: Date,
        Map(
          "MCC" -> 250,
          "MNC" -> 20.toShort,
          "gsm_st" -> 1,
          "gsmlevel" -> 1.toShort,
          "height" -> 102.toShort,
          "info" -> 500,
          "mw" -> 0,
          "nav_st" -> 3,
          "protocol" -> "Skysim",
          "protocolversion" -> 34,
          "pwr_ext" -> 93,
          "pwr_in" -> 3,
          "sim_in" -> 1,
          "sim_t" -> 0,
          "st0" -> 0,
          "st1" -> 0,
          "st2" -> 0,
          "vol1" -> 4232,
          "vol2" -> 13742).mapValues(_.asInstanceOf[AnyRef]).asJava
      )),ServerTest.packWriter.collection.sortBy(gps => (gps.time, gps.lat)))

  }

  @Test
  def testTalk2() {

    send("ff 22 ab 51 37 61 f5 10 03 00 ")
    expect("7b 00 00 7d ")
    send("5b 5f 01 1e 00 22 48 1d 55 03 d3 fc 5e 42 04 8f 13 17 42 05 00 11 0c 00 09 00 d0 c4 5f 00 01 0a 00 12 00 08 00 00 00 32 01 14 00 1a 4c 1d 55 00 53 5c 00 00 04 8f 10 00 00 04 00 00 04 00 09 00 00 00 00 08 5d ")
    expect("7b 00 5f 7d ")


    EventsTestUtils.waitUntil(ServerTest.packWriter.collection.size >= 2, 2000)
  }


    private def asBytes(text: String):
  Array[Byte] = {
    Utils.asBytesHex(
      text.stripMargin.lines.map(_.
        replaceAll("//.*", "")).mkString)
  }
}

object ServerTest extends grizzled.slf4j.Logging {

  val SKYSIM_TEST_PORT = ru.sosgps.wayrecall.testutils.getFreePort

  var server: SkysimNettyServer = null

  val packWriter = new CollectionPacketWriter(new ListBuffer[GPSData])

  @BeforeClass
  def initserver() {
    server = new SkysimNettyServer
    //server.timer = new HashedWheelTimer()
    server.setStore(new TrimmedPackProcessor(packWriter, true))
    //server.commands = commands
    server.bossPool = new NioEventLoopGroup()
    server.workerPool = server.bossPool


    server.setPort(SKYSIM_TEST_PORT)
    server.start()
  }

  @AfterClass
  def closeServer(): Unit = {
    server.stop()
  }

}