package ru.sosgps.wayrecall.packreceiver.netty

import java.nio.ByteOrder
import java.util
import java.util.Date

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelPipeline, SimpleChannelInboundHandler}
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.logging.LoggingHandler
import io.netty.util.CharsetUtil
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.{Netty4Server, PackProcessor}
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext
import ru.sosgps.wayrecall.utils.{bit, shortBitReads}

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.mutable


class GalileoskyServer extends Netty4Server with grizzled.slf4j.Logging {

  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[GalileoskyServer]),
      new GalileoskyMessageDecoder(),
      new GalileoskyAvlDataDecoder(store)
    )
  }

}

class GalileoskyMessageDecoder extends ByteToMessageDecoder with grizzled.slf4j.Logging {

  override def decode(ctx: ChannelHandlerContext, in0: ByteBuf, out: util.List[AnyRef]): Unit = {
    val in = in0.order(ByteOrder.LITTLE_ENDIAN)

    if (in.readableBytes() < 3)
      return

    val size = (in.getUnsignedShort(in.readerIndex() + 1) & 0x7fff) + 5

    if (in.readableBytes() < size) {
      debug(s"not enought data to read size = $size in.readableBytes() = ${in.readableBytes()} ")
      return
    }

    val body = in.readSlice(size).retain()
    debug(s"incoming body =${body.toHexString} size = $size")
    out.add(body)
  }
}


class GalileoskyAvlDataDecoder(store: PackProcessor) extends SimpleChannelInboundHandler[ByteBuf] with grizzled.slf4j.Logging {

  private var info: Option[String] = None

  override def channelRead0(ctx: ChannelHandlerContext, msg0: ByteBuf): Unit = {
    val msg = msg0.order(ByteOrder.LITTLE_ENDIAN)

    val startIndex = msg.readerIndex()

    debug(s"receiving: ${msg.toString(CharsetUtil.US_ASCII)}")

    val header = msg.readByte()
    debug(s"header = $header")

    msg.skipBytes(2) // skip size

    var date: Date = null
    var lat: java.lang.Float = null
    var lon: java.lang.Float = null
    var speed: java.lang.Integer = null
    var dir: java.lang.Integer = null
    var sat: java.lang.Byte = null
    var height: java.lang.Short = null
    val data = new mutable.HashMap[String, Any]()

    def storeGpsIfAny() {
      if (date != null) {
        val gPSData = new GPSData(null, info.get, lon.toDouble, lat.toDouble, date, speed.toShort, dir.toShort, sat)
        gPSData.data.put("protocol", "GalileoSky")
        gPSData.data.putAll(data.mapValues(_.asInstanceOf[AnyRef]).asJava)
        store.addGpsDataAsync(gPSData).onComplete(r => debug("save result: " + r))
        date = null
        data.clear()
      }
    }

    while (msg.readableBytes() > 2) {

      val tag = msg.readUnsignedByte()
      debug(s"tag = $tag")

      tag match {
        case 0x01 /*Device type*/ => msg.skipBytes(1)
        case 0x02 /*Firmware*/ => msg.skipBytes(1)
        case 0x03 /* IMEI */ =>
          info = Some(msg.readBytesAsString(15))
          debug("imeiInfo = " + info)
        case 0x04 /*device num*/ => msg.skipBytes(2)
        case 0x05 /*unknown*/ => msg.skipBytes(1)
        case 0x10 /*arhive num*/ => msg.skipBytes(2)
        case 0x20 /*datetime*/ =>
          storeGpsIfAny()
          date = new Date(msg.readUnsignedInt() * 1000)
        case 0x30 =>
          val firstByte = msg.readByte()
          val correctness = (firstByte & 0xF0) >> 4
          data.put("correctness", correctness)
          if (correctness == 0 || correctness == 2) {
            sat = (firstByte & 0x0F).toByte
            if (sat == 0) warn(s"undefining lon/lat in ${msg.toString(CharsetUtil.US_ASCII)}")
            lat = msg.readInt().toFloat / 1000000
            lon = msg.readInt().toFloat / 1000000
            if (sat == 0) {
              lat = Float.NaN
              lon = Float.NaN
            }
          } else {
            sat = 0.toByte
            lat = Float.NaN
            lon = Float.NaN
            msg.skipBytes(8)
          }

        case 0x33 =>
          speed = (msg.readUnsignedShort() * 0.1).round.toInt
          dir = (msg.readUnsignedShort() * 0.1).round.toInt
        case 0x34 =>
          height = msg.readShort()
        case 0x35 /* hdop 8*/ => msg.skipBytes(1)
        case 0x40 /* status*/ =>
          val status = msg.readShort()
          data.put("status", status.toBinaryString)
          data.put("ignition", bit(status.bit(9)))
        case 0x41 => data.put("power", msg.readUnsignedShort())
        case 0x42 => data.put("acc", msg.readUnsignedShort())
        case 0x43 => data.put("temp", msg.readByte())
        case 0x44 =>
          val acc4 = msg.readInt()
          //          val x = acc4 & 0xffc00000
          //          val y = acc4 & 0x003ff000
          //          val z = acc4 & 0x00000ffc
          val x = acc4 & 0x3ff
          val y = (acc4 & 0xffc00) >> 10
          val z = (acc4 & 0x3ff00000) >> 20
          val a = Math.sqrt(x * x + y * y + z * z)
          data.put("acceleration", a)
        case 0x45 => data.put("outputs", msg.readUnsignedShort().toBinaryString)
        case 0x46 => data.put("inputs", msg.readUnsignedShort().toBinaryString)
        case 0x50 => data.put("in0", msg.readUnsignedShort())
        case 0x51 => data.put("in1", msg.readUnsignedShort())
        case 0x52 => data.put("in2", msg.readUnsignedShort())
        case 0x53 => data.put("in3", msg.readUnsignedShort())
        case 0x58 /* rs232 0*/ => msg.skipBytes(2)
        case 0x59 /* rs232 1*/ => msg.skipBytes(2)
        case x if x >= 0x70 && x <= 0x77 => data.put("temp" + (x - 0x70), msg.readShort())
        case 0x90 /* ibutton*/ => msg.skipBytes(4)
        case x if x >= 0xC0 && x <= 0xC3 => data.put("can_a" + (x - 0xC0), msg.readInt())
        case x if x >= 0xC4 && x <= 0xD2 => data.put("can8bitr" + (x - 0xC4), msg.readByte())
        case 0xD3 /* ibutton 2 */ => msg.skipBytes(4)
        case 0xD4 /* total m */ => msg.skipBytes(4)
        case 0xD5 /* ibutton state */ => msg.skipBytes(1)
        case x if x >= 0xD6 && x <= 0xDA => data.put("can16bitr" + (x - 0xD6), msg.readShort())
        case x if x >= 0xDB && x <= 0xDF => data.put("can32bitr" + (x - 0xDB), msg.readInt())
        case x if x >= 0x54 && x <= 0x57 => data.put("in" + (x - 0x50), msg.readUnsignedShort())
        case x if x >= 0x80 && x <= 0x87 => data.put("DS1923_" + (x - 0x80), msg.readMedium())
        case x if x >= 0x60 && x <= 0x62 => data.put("fuel" + (x - 0x60), msg.readUnsignedShort())
        case x if x >= 0x63 && x <= 0x6F => data.put("fuel" + (x - 0x60), msg.readMedium())
        case x if x >= 0x88 && x <= 0xAF => data.put("rs232_" + (x - 0x88), msg.readByte())
        case x if x >= 0xB0 && x <= 0xB9 => msg.skipBytes(2)
        case x if x >= 0xF0 && x <= 0xF9 => msg.skipBytes(4)
        case 0x47 /* EcoDrive */ =>
          data.put("course_accel", msg.readUnsignedByte() / 100.0)
          data.put("braking_accel", msg.readUnsignedByte() / 100.0)
          data.put("turn_accel", msg.readUnsignedByte() / 100.0)
          data.put("vertical_accel", msg.readUnsignedByte() / 100.0)
        case 0x48 /* extended terminal status */ => msg.skipBytes(2)
        case x if x >= 0xE2 && x <= 0xE9 => data.put("userdata" + (x - 0xE2), msg.readInt())
        case other => warn(s"unknown command $other")

      }
    }

    storeGpsIfAny()

    //    val curIndex = msg.readerIndex()
    //    val remaining = size - (curIndex - startIndex)
    //    debug(s"bytes still unread = $remaining")
    //    msg.skipBytes(remaining)

    debug("msg left = " + msg.readableBytes())


    val checksum = msg.readShort()

    ctx.writeAndFlush(ctx.alloc().buffer(3).order(ByteOrder.LITTLE_ENDIAN)
      .writeByte(0x02)
      .writeShort(checksum)
    )


  }
}
