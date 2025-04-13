package ru.sosgps.wayrecall.avlprotocols.skysim

import java.nio.ByteOrder
import java.util.Date

import io.netty.buffer.{ByteBuf, Unpooled}
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions}
import ru.sosgps.wayrecall.testutils.DataHelpers
import ru.sosgps.wayrecall.utils.io.Utils

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
 * Created by nickl on 16.04.15.
 */
class ParserTest extends grizzled.slf4j.Logging {


  @Test
  def testParseHeader(): Unit = {
    val headerData = Unpooled.wrappedBuffer(
      Utils.asBytesHex("FF 22 F3 0C 45 F5 C9 0F 03 00")
    ).order(ByteOrder.LITTLE_ENDIAN)
    val header = Skysim.parseHeader(headerData)

    Assert.assertEquals(Header(-1, Skysim.VERSION_HEADER, "861785007918323"), header)
  }

  @Test
  def testParsePackage(): Unit = {
    val headerData = Unpooled.wrappedBuffer(
      //Utils.asBytesHex("5B 01 01 55 00 C5 CF C2 51 03 4D 8B 5E 42 04 18 D6 14 42 05 05 16 0A 00 09 02 E0 CC 64 15 F5 01 00 00 20 00 00 00 00 24 00 00 00 00 2A 3A CD 00 00 2C 71 CD 00 00 2D 2E CD 00 00 2E 6C CD 00 00 2F 3F CD 00 00 30 4D CD 00 00 31 46 CD 00 00 FA F8 01 00 00 FA F8 01 00 00 FA 90 01 00 00 62 01 50 00 5B D0 C2 51 03 4D 8B 5E 42 04 18 D6 14 42 05 05 16 0A 00 09 02 E0 CC 64 15 F5 01 00 00 20 00 00 00 00 24 00 00 00 00 2A 3A CD 00 00 2C 71 CD 00 00 2D 2E CD 00 00 2E 6C CD 00 00 2F 3F CD 00 00 30 4D CD 00 00 31 46 CD 00 00 FA F8 01 00 00 FA F8 01 00 00 6E 5D")
      Utils.asBytesHex(
        """
          |5B     // начало PACKAGE                
          |01  // номер посылки от 0x01 до 0xFB                                
          |01  // тип PACKET (0x00-PING, 0x01-DATA, 0x03 - TEXT, 0x04 - FILE)                                                        
          |55 00  // длина пакета данных (длина DATA)                                
          |C5 CF C2 51  // unixtime                                                       
          |03 4D 8B 5E 42  // TAG 0x03
          |04 18 D6 14 42 
          |05 05 16 0A 00 
          |09 02 E0 CC 64 
          |15 F5 01 00 00 
          |20 00 00 00 00 
          |24 00 00 00 00 
          |2A 3A CD 00 00 
          |2C 71 CD 00 00 
          |2D 2E CD 00 00 
          |2E 6C CD 00 00 
          |2F 3F CD 00 00 
          |30 4D CD 00 00 
          |31 46 CD 00 00 
          |FA F8 01 00 00 
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
          |15 F5 01 00 00
          |20 00 00 00 00
          |24 00 00 00 00
          |2A 3A CD 00 00
          |2C 71 CD 00 00
          |2D 2E CD 00 00
          |2E 6C CD 00 00
          |2F 3F CD 00 00
          |30 4D CD 00 00
          |31 46 CD 00 00
          |FA F8 01 00 00
          |FA F8 01 00 00
          |6E // CRC
          |5D  // конец PACKAGE
        """.stripMargin.lines.map(_.replaceAll("//.*", "")).mkString)).order(ByteOrder.LITTLE_ENDIAN)

    val result = Skysim.parsePackage(headerData)._2.collect({
      case m: Map[String, Any] => Skysim.tagsToGPSData(m, Header(1, 1, null))
    })

    //Parser.parsePackage(headerData)
    debug(result.map(DataHelpers.toScalaCode(_, false)))

    Assert.assertEquals(ListBuffer(new GPSData(
      null: String,
      null: String,
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
        "protocolversion" -> 1,
        "pwr_ext" -> 100,
        "pwr_in" -> 3,
        "sim_in" -> 1,
        "sim_t" -> 0,
        "st0" -> 1,
        "st1" -> 0,
        "st2" -> 0).mapValues(_.asInstanceOf[AnyRef]).asJava
    ), new GPSData(
      null: String,
      null: String,
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
        "protocolversion" -> 1,
        "pwr_ext" -> 100,
        "pwr_in" -> 3,
        "sim_in" -> 1,
        "sim_t" -> 0,
        "st0" -> 1,
        "st1" -> 0,
        "st2" -> 0).mapValues(_.asInstanceOf[AnyRef]).asJava
    )), result)
  }

  @Test
  def serverCom(): Unit = {

    Assert.assertEquals(
      "7b 00 00 7d ",
      Utils.toHexString(Skysim.serverCom(0, Unpooled.buffer(0), Unpooled.buffer(4)), " ")
    )
    //debug(Unpooled.wrappedBuffer(Utils.asBytesHex("DE 95 DB 52")).order(ByteOrder.LITTLE_ENDIAN).readUnsignedInt())
    Assert.assertEquals(
      "7B 04 00 A0 DE 95 DB 52 7D ".toLowerCase,
      Utils.toHexString(Skysim.serverCom(0, Unpooled.buffer(4).order(ByteOrder.LITTLE_ENDIAN).writeInt(1390122462), Unpooled.buffer(9)), " ")
    )

  }

}
