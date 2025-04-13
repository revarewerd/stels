package ru.sosgps.wayrecall.packreceiver.netty

import java.nio.ByteOrder
import java.time.format.DateTimeFormatter
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
import ru.sosgps.wayrecall.utils.byteBitReads
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext


class ADM1_07Server extends Netty4Server with grizzled.slf4j.Logging {

  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[ADM1_07Server]),
      new ADM1_07MessageDecoder(),
      new ADM1_07AvlDataDecoder(store)
    )
  }

}

class ADM1_07MessageDecoder extends ByteToMessageDecoder with grizzled.slf4j.Logging {

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes() < 3)
      return

    val size = in.getUnsignedByte(in.readerIndex() + 2)

    if (in.readableBytes() < size) {
      debug(s"not enought data to read size = $size in.readableBytes() = ${in.readableBytes()} ")
      return
    }

    val body = in.readSlice(size).retain()
    debug("incoming body =" + body.toHexString)
    out.add(body)
  }
}


class ADM1_07AvlDataDecoder(store: PackProcessor) extends SimpleChannelInboundHandler[ByteBuf] with grizzled.slf4j.Logging {

  val dateFormat = DateTimeFormatter.ofPattern("yyMMdd")
  val timeFormat = DateTimeFormatter.ofPattern("HHmmss")

  case class Info(imei: String, hwType: Byte, replyEnabled: Byte)

  private var info: Option[Info] = None

  override def channelRead0(ctx: ChannelHandlerContext, msg0: ByteBuf): Unit = {
    val msg = msg0.order(ByteOrder.LITTLE_ENDIAN)

    debug(s"receiving: ${msg.toString(CharsetUtil.US_ASCII)}")

    val startIndex = msg.readerIndex()

    val deviceId = msg.readUnsignedShort()
    debug(s"deviceId = $deviceId")
    val size = msg.readUnsignedByte()
    debug(s"size = $size")
    val typ = msg.readByte()
    debug(s"typ = $typ")

    val typeBits = typ & 0x03

    debug(s"typeBits = $typeBits")

    typeBits match {
      case 0x03 /* IMEI */ =>
        info = Some(Info(
          imei = msg.readBytesAsString(15),
          hwType = msg.readByte(),
          replyEnabled = msg.readByte()
        ))
        debug("imeiInfo = " + info)
      case 0x01 | 0x00 /* пакет с данными */ => {
        val soft = msg.readUnsignedByte()
        debug(s"soft = $soft")
        val gpsPntr = msg.readUnsignedShort()
        debug(s"gpsPntr = $gpsPntr")
        val status = msg.readUnsignedShort()
        debug(s"status = $status")
        debug("latbutes =" + msg.toHexString)
        def zeroAsNan(float: Float) = if (float == 0.0f) Float.NaN else float
        val lat = zeroAsNan(msg.order(ByteOrder.LITTLE_ENDIAN).readFloat())
        debug(s"lat = $lat")
        val lon = zeroAsNan(msg.order(ByteOrder.LITTLE_ENDIAN).readFloat())
        debug(s"lon = $lon")
        val course = (msg.readUnsignedShort() * 0.1).round
        debug(s"course = $course")
        val speed = (msg.readUnsignedShort() * 0.1).round
        debug(s"speed = $speed")
        val acc = msg.readUnsignedByte()
        debug(s"acc = $acc")
        val height = msg.readUnsignedShort()
        debug(s"height = $height")
        val hdop = msg.readUnsignedByte()
        debug(s"hdop = $hdop")
        val sattelits = msg.readUnsignedByte()
        debug(s"sattelits = $sattelits")
        val seconds = msg.order(ByteOrder.LITTLE_ENDIAN).readUnsignedInt()
        debug(s"seconds = $seconds")
        val power = msg.readUnsignedShort()
        debug(s"power = $power")
        val battery = msg.readUnsignedShort()
        debug(s"battery = $battery")

        if (typ.bit(2)) msg.skipBytes(4) //акселерометр, выходы, события по входам
        if (typ.bit(3)) msg.skipBytes(12) //аналоговые входы
        if (typ.bit(4)) msg.skipBytes(8) //импульсные/дискретные входы
        if (typ.bit(5)) msg.skipBytes(9) //датчики уровня топлива и температуры

        val data = new GPSData(null, info.get.imei, lon.toDouble, lat.toDouble, new Date(seconds * 1000), speed.toShort, course.toShort, sattelits.toByte)
        data.data.put("protocol", "ADM1.07")
        store.addGpsDataAsync(data).onComplete(r => debug("save result: " + r))
      }

      case other => warn(s"unknown command $other")

    }

    val curIndex = msg.readerIndex()
    val remaining = size - (curIndex - startIndex)
    debug(s"bytes still unread = $remaining")
    msg.skipBytes(remaining)

    debug("msg left = " + msg.readableBytes())

    if (info.get.replyEnabled == 0x02 /* should reply */ ) {
      ctx.writeAndFlush(ctx.alloc().buffer(132)
        .writeShort(deviceId)
        .writeByte(0x84)
        .writeBytes("***1*".getBytes(CharsetUtil.US_ASCII))
        .writeZero(124))

    }
  }
}
