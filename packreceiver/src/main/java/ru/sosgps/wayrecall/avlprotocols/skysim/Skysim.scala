package ru.sosgps.wayrecall.avlprotocols.skysim

import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util
import java.util.Date

import com.google.common.base.Charsets
import io.netty.buffer.ByteBuf
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils.{ScalaNettyUtils, typingMutableMap}
import ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.io.Utils
import ru.sosgps.wayrecall.utils.typingMutableMap

import scala.collection.JavaConverters._
import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created by nickl on 16.04.15.
 */
object Skysim extends grizzled.slf4j.Logging {

  val PACKET_PING: Byte = 0
  val PACKET_DATA: Byte = 1
  val PACKET_TEXT: Byte = 3
  val PACKET_FILE: Byte = 4
  val VERSION_HEADER: Byte = 0x22
  val VERSION_HEADER2: Byte = 0x23
  val PACKAGE_END: Byte = 0x5d
  val PACKAGE_START: Byte = 0x5b
  val TAG_PACKET_SIZE = 5


  def parseHeader(message0: ByteBuf): Header = {
    require(message0.readableBytes() >= 10)
    val message = message0.order(ByteOrder.LITTLE_ENDIAN)

    val headerId = message.readByte()
    debug(s"headerId=${Integer.toHexString(headerId)}")
    val protocolVersion = message.readByte()
    debug(s"protocolVersion=${Integer.toHexString(protocolVersion)}")
    val bytes = message.toIterator(message.readerIndex(), 8).toArray.reverse
    //debug(s"imeiBytes=${Utils.toHexString(bytes,"")}")
    val imeiLong = message.readLong()
    debug(s"imei=$imeiLong")
    //val imei = message.toString(message.readerIndex(), 8, Charsets.UTF_8)
    Header(headerId, protocolVersion, imeiLong.toString)
  }

  def parsePackage(message0: ByteBuf): (Int, Seq[Any]) = {
    val message = message0.order(ByteOrder.LITTLE_ENDIAN)
    val startByte = message.readByte()
    assert(startByte == PACKAGE_START, "invalid start byte:" + startByte)
    val num = message.readByte()

    val packets = new ListBuffer[Any]()
    while (message.getByte(message.readerIndex()) != PACKAGE_END) {
      packets += parsePacket(message)
    }

    assert(message.readByte() == PACKAGE_END)
    debug("packets=" + packets)
    (num, packets)
  }

  def parsePacket(message0: ByteBuf): scala.collection.Map[String, Any] = {
    val message = message0.order(ByteOrder.LITTLE_ENDIAN)
    val packetType = message.readByte()
    debug(s"packetType=$packetType")
    val len = message.readUnsignedShort()
    debug(s"len=$len")
    val checkStart = message.readerIndex()
    val time = message.readUnsignedInt() * 1000L

    val tagAttributes = parseTags(message, len) + ("time" -> new Date(time))

    val controlSumCalked = checkSum(message, checkStart, len + 4)
    val controlSum = message.getUnsignedByte(checkStart + 4 + len)
    //assert(controlSum == controlSumCalked, "control sum failed: expected " + controlSum + " but calcked " + controlSumCalked)
    if(controlSum != controlSumCalked)
      warn(s"controlSum not matched: $controlSum $controlSumCalked")
    message.readerIndex(checkStart + len + 5)
    tagAttributes
  }

  def checkSum(message: ByteBuf, checkStart: Int, length: Int): Int = {
    message.toIterator(checkStart, length).foldLeft(0x0)(_ + _) & 0xFF
  }

  def parseTags(message: ByteBuf, len: Int): scala.collection.Map[String, Any] = {
    val tagAttributes = (0 until len / TAG_PACKET_SIZE).map(_ => parseTag(message)).reduce(_ ++ _)

    //debug(s"tagAttributes = ${tagAttributes.toSeq.sortBy(_._1).mkString("[\n", ",\n", "]")}")

    debug(s"remaining = ${message.readableBytes()}")
    tagAttributes
  }

  private def parseTag(message: ByteBuf): scala.collection.Map[String, Any] = {
    val id = message.readUnsignedByte()
    tagReaders.get(id).map(_(message)).getOrElse(Map("key" + id -> message.readInt()))
  }

  val tagReaders = Map(
    1 -> ((m: ByteBuf) => Map("vol1" -> m.readUnsignedShort(), "vol2" -> m.readUnsignedShort())),
    2 -> ((m: ByteBuf) => Map("ID" -> m.readInt())),
    3 -> ((m: ByteBuf) => Map("lat" -> m.readFloat())),
    4 -> ((m: ByteBuf) => Map("lon" -> m.readFloat())),
    5 -> ((m: ByteBuf) => Map("speed" -> m.readUnsignedByte(), "sattelits" -> sumSatellits(m), "height" -> m.readUnsignedByte(), "course" -> m.readUnsignedByte())),
    8 -> ((m: ByteBuf) => Map("gsmlevel" -> m.readUnsignedByte(), "MCC" -> m.readUnsignedShort(), "MNC" -> m.readUnsignedByte())),
    9 -> ((m: ByteBuf) => readStatuses(m)),
    250 -> ((m: ByteBuf) => Map("info" -> m.readInt()))
  )

  private def sumSatellits(m: ByteBuf): Byte = {
    val byte = m.readByte()
    val gps = byte & 0x0F;
    val glonass = (byte >> 4) & 0x0F
    (gps + glonass).toByte
  }

  private def readStatuses(bb: ByteBuf): scala.collection.Map[String, Any] = {

    val bits = bb.readInt()

    val data = new mutable.HashMap[String, Any]()

    object dataNotNull {
      def update(key: String, v: Int) = {
        if (v != 0)
          data(key) = v
      }
    }

    for (i <- 0 to 7) {
      dataNotNull("IN" + i) = (bits >> i) & 0x01
    }
    for (i <- 8 to 11) {
      dataNotNull("OUT" + (i - 8)) = (bits >> i) & 0x01
    }
    data("gsm_st") = (bits >> 12) & 0x03
    data("nav_st") = (bits >> 14) & 0x03
    data("mw") = (bits >> 16) & 0x01
    data("sim_t") = (bits >> 17) & 0x01
    data("sim_in") = (bits >> 18) & 0x01
    data("st0") = (bits >> 19) & 0x01
    data("st1") = (bits >> 20) & 0x01
    data("st2") = (bits >> 20) & 0x01
    data("pwr_in") = (bits >> 22) & 0x03
    data("pwr_ext") = (bits >> 24) & 0xFF
    data
  }

  def tagsToGPSData(tags0: Map[String, Any], header: Header): GPSData = {

    val tags = new mutable.HashMap[String, Any]() ++ tags0

    val gps = new GPSData(null, header.imei,
      tags.removeAs[Float]("lon").map(_.toDouble).getOrElse(Double.NaN),
      tags.removeAs[Float]("lat").map(_.toDouble).getOrElse(Double.NaN),
      tags.removeAs[Date]("time").get,
      tags.removeAs[Short]("speed").getOrElse(0),
      tags.removeAs[Short]("course").getOrElse(0),
      tags.removeAs[Byte]("sattelits").getOrElse(0)
    )
    gps.data = new util.HashMap(tags.mapValues(_.asInstanceOf[AnyRef]).asJava)
    gps.data.put("protocol", "Skysim")
    gps.data.put("protocolversion", header.version.asInstanceOf[AnyRef])
    gps
  }

  def serverCom(messageNum: Int, data: ByteBuf, out0: ByteBuf): ByteBuf = {
    val out = out0.order(ByteOrder.LITTLE_ENDIAN)
    out.writeByte(0x7B);
    out.writeByte(data.readableBytes());
    out.writeByte(messageNum);
    if (data.readableBytes() > 0) {
      out.writeByte(checkSum(data, data.readerIndex(), data.readableBytes()));
      out.writeBytes(data)
    }
    data.release()
    out.writeByte(0x7D)
  }

}

case class Header(id: Int, version: Int, imei: String)