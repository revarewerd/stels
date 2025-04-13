package ru.sosgps.wayrecall.avlprotocols.dtm

import java.nio.ByteOrder
import java.time.{LocalDateTime, Instant}
import java.util.Date

import io.netty.buffer.ByteBuf
import ru.sosgps.wayrecall.avlprotocols.autophonemayak.AutophoneMayak
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.{java8TLocalDateTimeOps, tryNumerics}

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable
import scala.collection.JavaConversions.mapAsJavaMap

/**
  * Created by nmitropo on 9.1.2016.
  */
object DTM extends grizzled.slf4j.Logging {

  def writeResponse1(out: ByteBuf, count: Int): ByteBuf = {
    out
      .writeByte(0x7B)
      .writeByte(0x00)
      .writeByte(count)
      .writeByte(0x7D)
  }

  def writeCommand(out: ByteBuf, command: ByteBuf): ByteBuf = {


    out
      .writeByte(0x7B)
      .writeByte(command.readableBytes())
      .writeByte(0xFF)
      .writeByte(checksum(command))
      .writeAndRelease(command)
      .writeByte(0x7D)
  }

  def parseHeader(in0: ByteBuf): String = {

    val in = in0.order(ByteOrder.LITTLE_ENDIAN)

    val headerId = in.readUnsignedByte()
    debug("headerId = " + headerId)

    val version = in.readUnsignedByte()
    debug("version = " + version)

    //val imei = in.readBytesAsString(8)
    val imei = BigInt(1, in.readBytesToArray(8).reverse).toString()
    debug("imei = " + imei)

    imei
  }

  def readPacket(in: ByteBuf): Option[GPSData] = {

    val packetType = in.readByte()
    debug("packetType = " + packetType)

    val packetSize = in.readUnsignedShort()
    debug("packetSize = " + packetSize)

    val crcareastart = in.readerIndex()

    val unixTime = Instant.ofEpochSecond(in.readUnsignedInt())

    val localDateTime = Date.from(unixTime)
    debug("unixTime = " + unixTime + " " + localDateTime)

    val body = in.readSlice(packetSize)
    val crcareaend = in.readerIndex()
    val checksum = in.readByte()
    val checksumCalc = this.checksum(in.slice(crcareastart, crcareaend - crcareastart)).toByte

    if(checksum != checksumCalc)
      warn(s"crc in package $checksum is not equal to calculated $checksumCalc")

    if (packetType == 0x01 /*DATA*/ ) {
      val tags = new mutable.HashMap() ++ Iterator.continually().takeWhile(_ => body.isReadable).flatMap(_ => readTag(body.readSlice(5)))
      val gps = new GPSData(null, null,
        tags.remove("lon").map(_.tryDouble).getOrElse(Double.NaN),
        tags.remove("lat").map(_.tryDouble).getOrElse(Double.NaN),
        localDateTime,
        tags.remove("speed").map(_.tryInt).getOrElse(0).toShort,
        tags.remove("course").map(_.tryInt).getOrElse(0).toShort,
        tags.remove("sattelits").map(_.tryInt).getOrElse(0).toByte
      )
      gps.data.putAll(tags)
      gps.data.put("protocol", "DTM")
      Option(gps)
    } else
      None


  }

  def checksum(bb: ByteBuf): Int = {
    bb.toIteratorReadable.foldLeft(0x0)(_ + _) & 0xFF
  }

  def readPackage(in0: ByteBuf): (Int, IndexedSeq[GPSData]) = {
    val in = in0.order(ByteOrder.LITTLE_ENDIAN)
    val packageSing = in.readByte()
    require(packageSing == 0x5B, s"packageSing $packageSing expected to be 0x5B")

    val parcelNumber = in.readUnsignedByte()
    debug("parcelNumber = " + parcelNumber)

    val packets = Iterator.continually().takeWhile(_ => in.getByte(in.readerIndex()) != 0x5D).map(_ => readPacket(in)).toIndexedSeq
    in.readByte()
    (packets.size, packets.flatten)
  }

  private def readTag(b: ByteBuf): Iterable[(String, AnyRef)] = {
    val code = b.readUnsignedByte()
    (code match {
      case 1 => Iterable(
        "ext" -> b.readShort(),
        "int" -> b.readShort()
      )
      case 2 => Iterable("IDKey" -> b.readInt())
      case 3 => Iterable("lat" -> b.readFloat())
      case 4 => Iterable("lon" -> b.readFloat())
      case 9 => parseDeviceStatus(b.readInt())
      case 5 => {
        val course = b.readUnsignedByte() / 2
        val height = b.readUnsignedByte() * 10
        val satByte = b.readUnsignedByte()
        val sattelits = (satByte & 0x0F) + (satByte >> 4 & 0x0F)
        val speed = b.readUnsignedByte() * 1.852
        Iterable(
          "course" -> course,
          "height" -> height,
          "sattelits" -> sattelits,
          "speed" -> speed
        )
      }
      case e => Iterable(e.toString -> b.readInt())
    }).map(kv => (kv._1, kv._2.asInstanceOf[AnyRef]))


  }

  private def parseDeviceStatus(i: Int): Iterable[(String, Any)] = {
    debug(s"parseDeviceStatus as $i")
    Iterable(
      "in1" -> (i >> 0 & 0x1),
      "in2" -> (i >> 1 & 0x1),
      "in3" -> (i >> 2 & 0x1),
      "in4" -> (i >> 3 & 0x1),
      "in5" -> (i >> 4 & 0x1),
      "in6" -> (i >> 5 & 0x1),
      "in7" -> (i >> 6 & 0x1),
      "in8" -> (i >> 7 & 0x1),
      "out1" -> (i >> 8 & 0x1),
      "out2" -> (i >> 9 & 0x1),
      "out3" -> (i >> 10 & 0x1),
      "out4" -> (i >> 11 & 0x1),
      /*
      12	the state of the GSM modem (messages Wialon gsm_st)	0 - off, 1 - Registration in network 2 - comp. 3 server - replies from the server (standard)
  13
  14	the state of GPS / Glonass module (messages Wialon nav_st)	0 - off 1 - search for satellites, 2 - <8 satellites, 3 - more than 8 satellites (normal)
  15
       */
      "gsm_st" -> (i >> 12 & 0x3),
      "nav_st" -> (i >> 14 & 0x3),
      /*
  16	Motion Sensor (messages Wialon mw)	1 - motion is 0 - no movement
  17	SIM card / SIM-chip (messages Wialon sim_t)	1 - sim-chip 0 - sim-card
  18	the presence of a SIM card / SIM-chip (messages Wialon sim_in)	1 - ie, 0 - no
  19	st0 - mode service	mode (0 - monitoring, 1 - protection)
  20	st1 - SOS mode monitor / alarm mode protection	status of the alarm button (SOS) / alarm in the armed mode
  21	st2 - Status of plugs	ignition or ignition of the CAN virtual
  22	Backup battery status (in messages Wialon pwr_in)	00 - less than 3V or not connected, 01 - from 3V to 3.8 V, 10 - from 3.8V to 4.1V, 11 - more than 4.1V (normal)
  23	*/
      "mw" -> (i >> 16 & 0x1),
      "sim_t" -> (i >> 17 & 0x1),
      "sim_t" -> (i >> 18 & 0x1),
      "st0" -> (i >> 19 & 0x1),
      "st1" -> (i >> 20 & 0x1),
      "st2" -> (i >> 21 & 0x1),
      "ignition" -> (i >> 21 & 0x1), // синоним
      "pwr_in" -> ((i >> 22 & 0x3) match {
        case 0 => 0.0
        case 1 => 3.0
        case 2 => 3.8
        case 3 => 4.1
      }), // 00 - less than 3V or not connected, 01 - from 3V to 3.8 V, 10 - from 3.8V to 4.1V, 11 - more than 4.1V (normal)
      "pwr_ext" -> (i >> 24 & 0xFF) * 150
      /*
24	external voltage (messages Wialon pwr_ext)	External voltage divided by 150

example: 0x54 = 84 * 150 = 12600 mV

max. 38400 mV
25
26
27
28
29
thirty
31
     */

    )

  }

}
