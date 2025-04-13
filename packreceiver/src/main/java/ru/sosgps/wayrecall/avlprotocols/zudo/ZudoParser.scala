package ru.sosgps.wayrecall.avlprotocols.zudo

import java.nio.ByteBuffer
import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import io.netty.buffer.{ByteBuf, Unpooled}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.{byteBitReads, iff, intBitReads}

import scala.collection.mutable.ArrayBuffer


/**
  * Created by nmitropo on 6.10.2016.
  */

case class Package(ID_PAC: Byte, PAC_CNT: Byte, gpss: List[GPSData])

object ZudoParser extends grizzled.slf4j.Logging {

  val ZUDO_START_TIME = ZonedDateTime.ofInstant(java.time.Instant.EPOCH, ZoneId.systemDefault()).
    plusYears(10).plusDays(5)// экспериментально выясненный сдыиг

  def parse(in: ByteBuf): Package = {

    debug("bb:" + in + ":" + in.toHexString)

    val startIndex = in.readerIndex()

    val ID_PAC = in.readByte()
    debug(s"ID_PAC = $ID_PAC")

    val OFFS_DATA = in.readByte()
    debug(s"OFFS_DATA  = $OFFS_DATA ")

    val PAC_CNT = in.readByte()
    debug(s"PAC_CNT  = $PAC_CNT ")

    in.skipBytes(1) // reserv

    val PAR = in.readByte()
    debug(f"PAR  = $PAR%2d $PAR%2x ${PAR.bitString}%s ")

    val extended = ((PAR >> 7) & 0x1) == 1
    debug(s"extended  = $extended ")

    in.readerIndex(startIndex + OFFS_DATA)

    val accLat = new AccumulatingCFloat
    val accLon = new AccumulatingCFloat
    val accTime = new AccumulatingCFloat
    var week = 0

    def readNext(): Iterator[GPSData] = try {

      debug("remaiming1:" + in.readableBytes() + ":" + in.toHexString)

      val mFD = in.readByte()
      debug(f"mFD  = $mFD%2x: ${mFD.bitString}%s ")

      val events = readEvents(in)
      debug(s"events  = $events ")

      //require(mFD == 0xFF, "expect MTA6r format")

      val lat = accLat.readNext(in).toDouble.toDegrees

      val lon = accLon.readNext(in).toDouble.toDegrees
      debug(s"lonlat = $lon $lat")

      val gpsTime = accTime.readNext(in)
      debug(s"gpsTime = $gpsTime")

      if (accTime.lastLength == 0)
        week = in.readUnsignedShort()

      debug("week=" + week)

      val status = iff(mFD.bit(0)) {
        in.readByte()
      }

      debug(s"status = $status")

      val height = iff(mFD.bit(1))(in.readShort())
      debug(s"height = $height")
      val speed_angle = iff(mFD.bit(2)) {
        (in.readUnsignedShort() & 0x3FF, in.readByte())
      }
      debug(s"speed_angle = $speed_angle")

      val mks = iff(mFD.bit(3))(in.readUnsignedShort())

      debug(s"mks = $mks")

      debug("remaiming2:" + in.readableBytes())

      if (mFD.bit(4))
        in.skipBytes(12) // maybe parse later

      if (mFD.bit(5))
        in.skipBytes(8)

      if (mFD.bit(6))
        in.skipBytes(5)

      if (mFD.bit(7))
        in.skipBytes(4)

      val time = ZUDO_START_TIME.
        plusWeeks(week).
        plusSeconds(gpsTime.toLong)

      debug("time = " + time)

      val cur = new GPSData(null, null, lon.toDouble, lat.toDouble, Date.from(time.toInstant), speed_angle.map(_._1.toShort).getOrElse(0), speed_angle.map(_._2.toShort).getOrElse(0), -1)
      cur.data.put("protocol", "Zudo")
      cur.data.put("gpsTime", gpsTime.asInstanceOf[AnyRef])
      cur.data.put("week", week.asInstanceOf[AnyRef])

      if (in.readableBytes() > 0)
        Iterator(cur) ++ readNext()
      else
        Iterator(cur)

    } catch {
      case e: Exception => error("error:", e); Iterator.empty
    }


    Package(ID_PAC, PAC_CNT, readNext().toList)

  }

  def readEvents(in: ByteBuf): Any = {

    val b = in.readByte()
    debug(s"readEvents byte=${b.bitString}")
    val Ext = (b >> 7) & 0x01
    val NoE = (b >> 6) & 0x01
    if (Ext == 0 && NoE == 1)
      Seq(b)
    else if (Ext == 1 && NoE == 1) {
      val bytes = in.readBytes(8)
      debug(s"event butes = ${bytes.toHexString}")
      b +: bytes.toIteratorReadable.toIndexedSeq
    }
    else if (NoE == 0) {
      var _Ext = Ext
      val arrayBuffer = new ArrayBuffer[Byte](9)
      arrayBuffer += 9
      if (_Ext == 1)
        do {
          val b = in.readByte()
          debug("event byte:" + b.bitString)
          _Ext = b >> 7 & 0x01
          debug(s"_Ext = ${_Ext}")
          arrayBuffer += b
        } while (_Ext == 1)
      arrayBuffer.toSeq
    } else ???


  }


  class AccumulatingCFloat {
    var prev: Option[Int] = None

    var lastLength = 0

    def readNext(in: ByteBuf) = {
      val i = in.getUnsignedByte(in.readerIndex())
      val bits = i >> 6
      debug(s"AccumulatingCFloat bits = $bits")
      if (bits == 0) {
        val int = in.readInt() << 2
        prev = Some(int)
        Unpooled.buffer(4).writeInt(int).readFloat()
      } else {

        val slice = in.getInt(in.readerIndex())
        debug(s"in: ${slice.toHexString} ${slice.bitString}")
        val newInt = bits match {
          case 1 => prev.get & 0xFFFFFFF0 | in.readUnsignedByte()
          case 2 => prev.get & (0xFFFFFF00 << 2) | in.readUnsignedShort()
          case 3 => prev.get & 0xFFFFF000 | in.readUnsignedMedium() << 2
        }
        /*
        10 10010110001110

        00111111011110011001000000000100
        00111111011110111001011000111000

        00111111011110011010010110001110
         */

        debug(s"prev.get/newInt = ${prev.get.bitString}/${newInt.bitString} ${prev.get.toHexString}/${newInt.toHexString}")
        prev = Some(newInt)
        lastLength = bits
        Unpooled.buffer(4).writeInt(newInt).readFloat()
      }

    }
  }

}
