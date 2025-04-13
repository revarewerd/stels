package ru.sosgps.wayrecall.avlprotocols.navtelecom

import java.nio.ByteOrder
import java.util.Date

import com.google.common.base.Charsets
import com.google.common.util.concurrent.MoreExecutors
import io.netty.buffer.Unpooled
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions}
import ru.sosgps.wayrecall.utils.ScalaNettyUtils
import ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.packreceiver.{DummyPackSaver, TrimmedPackProcessor}
import ru.sosgps.wayrecall.testutils.DataHelpers
import ru.sosgps.wayrecall.utils.io.{RichDataInput, Utils}
import ru.sosgps.wayrecall.utils.stubs.CollectionPacketWriter

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.immutable.{TreeMap, TreeSet}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContext, Future}
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext
import scala.collection.JavaConverters.mapAsJavaMapConverter

import scala.concurrent.duration.DurationInt

/**
 * Created by nickl on 02.06.14.
 */
class ParseTest {


  @Test
  def test1 {
    val telem = Utils.asBytesHex("0674110000c1110f391015030ecc046300803700760e02000a000a00000000000000000000000000ffffff00faff00faff00faff00faff00faff00faff00faff15808080ffffffffffff010f231715030ea323fe01819b56010000000000000000bd00a444ec420000000003000000")

    NavtelecomParser.parseTelemetry(null, telem)

  }

  @Test
  def test2 {

    val received = new ListBuffer[GPSData]

    val connectionProcessor = new NavtelecomConnectionProcessor(
      new TrimmedPackProcessor(new CollectionPacketWriter(received))
    )

    def receiveBytes(telem: Array[Byte]) {
      val buffer = Unpooled.wrappedBuffer(telem)
      val result = connectionProcessor.processPackage(buffer)
      buffer.release()
      println("result=" + result.map(Utils.toHexString(_, "")))
    }

    receiveBytes(Utils.asBytesHex("404e5443010000005031270313004c022a3e533a383638323034303034323131323633"))
    receiveBytes(Utils.asBytesHex("404e544301000000503127030000005d"))
    receiveBytes(Utils.asBytesHex("404e544301000000503127030000005d"))

    val telem = Utils.asBytesHex("404e544301000000503127037300cce22a3e410106ad9400000b170c281219080ecc2d1000001231261003000a000a00000000000000000000000000ffffff00faff00faff00faff00faff00faff00faff00faff14808080ffffffffffff3b0c281219080ee951fe0113c357018e05000000000000ef00eb4b48440000000078007800")
    receiveBytes(telem)
    for (gps <- received) {
      println(gps)
      println(TreeMap(gps.data.toSeq: _ *))
    }

  }

  @Test
  def testXor(): Unit = {

    Assert.assertEquals(0x6d, FlexParser.crc8(Array(
      0x7e, 0x54, 0x03, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x11, 0x8e, 0x59, 0x5a, 0x54,
      0x01, 0x8e, 0x59, 0x5a, 0x54, 0xf1, 0xc4, 0x7d, 0x4c, 0x03, 0xc5, 0x9e, 0x71, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00
    ).map(_.toByte)))

    Assert.assertEquals(0x8d, FlexParser.crc8(Array(
      0x7e, 0x54, 0x02, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x21, 0x11, 0x8b, 0x71, 0x5a, 0x54,
      0x11, 0x8b, 0x71, 0x5a, 0x54, 0xf0, 0x4c, 0x7c, 0x4c, 0x4c, 0x31, 0x9e, 0x71, 0x00, 0x00, 0xe0,
      0x40, 0x02, 0x00
    ).map(_.toByte)))

  }

  @Test
  def parseReal(): Unit = {
    val buffer = Unpooled.wrappedBuffer(
      Utils.asBytesHex("7e4104b00400000317b85162546323b85162542f61fe0131c9590158080000000000001b009128b10400000015b85162546300b85162542f61fe0131c9590158080000000000001b007f29b40400000317d75162546317d7516254f166fe0163cb590138090000000000001b004428b60400000015df5162546300df516254b964fe01e6ca5901c9080000000000001b005929fe")
    ).order(ByteOrder.LITTLE_ENDIAN)
    val definedBits = TreeSet(1, 2, 3, 7, 8, 9, 10, 11, 12, 13, 14, 19)

    buffer.skipBytes(2)
    val size = buffer.readByte()
    println("size=" + size)
    for (i <- 0 until size)
      FlexParser.parseBody(null, buffer, definedBits)
  }

  @Test
  def doCommand(): Unit = {
    //val pack = NavtelecomParser.makePackage("@NTC", 52900176, -1, Unpooled.wrappedBuffer("*?V".getBytes(Charsets.US_ASCII)))
    //val pack = NavtelecomParser.makePackage("@NTC", 52900176, 0, Unpooled.wrappedBuffer("*>PASS:21s21".getBytes(Charsets.US_ASCII)))
    val pack = NavtelecomParser.makePackage("@NTC", 52900176, 0, Unpooled.wrappedBuffer("*!2Y".getBytes(Charsets.US_ASCII)))
    println("pack=" + new String(pack.toIteratorReadable.toArray, Charsets.US_ASCII))
    println("pack=" + Utils.toHexString(pack, " "))

  }


  @Test
  def parseCommandResponse(): Unit = {

    val resp = Utils.asByteBuffer("40 4e 54 43 01 00 00 00 00 00 00 00 35 00 7e 53 " +
      " 2a 40 43 3f c4 09 00 20 a1 d4 38 7e 57 00 39 0a " +
      " 27 d4 38 7e 57 b2 7c fe 01 68 82 58 01 b3 06 00 " +
      " 00 00 00 00 00 eb 00 4c 64 1c 47 d4 31 33 10 00 " +
      " 02 4f 5d 49 00").order(ByteOrder.LITTLE_ENDIAN)

    val prefix = NavtelecomParser.parsePrefix(resp.copy())
    println("prefix=" + prefix)

    val fakeFlex = Unpooled.wrappedBuffer(prefix.body).order(ByteOrder.LITTLE_ENDIAN).skipBytes(3)
    //val fakeFlex = resp.skipBytes(19)

    val flexBits = TreeSet(1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 19, 20, 29, 31, 37)

    val gPSData = FlexParser.parseBody("0", fakeFlex,
      //TreeSet(1, 2, 3, 7, 8, 9, 10, 11, 12, 13, 14, 19)
      flexBits
      //TreeSet(32)
    )

    //println("gPSData=" + DataHelpers.toScalaCode(gPSData))

    val encodedData = new GPSData(null: String,
      "0",
      37.62961333333333,
      55.75880333333333,
      new Date(1467889876000L) /* Thu Jul 07 14:11:16 MSK 2016 */ ,
      0,
      235,
      9,
      null: String,
      null: Date,
      Map("clockSync" -> 1,
        "distance" -> 40036.297f,
        "dsensors1" -> 0.toShort,
        "event" -> 41248,
        "gsm" -> 1,
        "gsmlevels" -> 10.toShort,
        "height" -> 1715,
        "ignition" -> 0,
        "index" -> 640063L,
        "motohours" -> 4808015L,
        "network" -> 1,
        "out1" -> 2.toShort,
        "protocol" -> "Navtelecom-flex",
        "reserv" -> 0,
        "roaming" -> 0,
        "sim" -> 1,
        "status" -> 0.toShort,
        "usb" -> 0,
        "voltage" -> 12756,
        "voltage_r" -> 4147).mapValues(_.asInstanceOf[AnyRef]).asJava)

    Assert.assertEquals(encodedData, gPSData)

    val received = new ListBuffer[GPSData]

    val connectionProcessor = new NavtelecomConnectionProcessor(
      new TrimmedPackProcessor(new CollectionPacketWriter(received))
    )
    connectionProcessor.imei = Some("0")
    connectionProcessor.flexBits = flexBits

    Await.result(connectionProcessor.processPackage(resp.copy().order(ByteOrder.LITTLE_ENDIAN)), 1 second)
    received.head.insertTime = null
    Assert.assertEquals(encodedData, received.head)

  }

}
