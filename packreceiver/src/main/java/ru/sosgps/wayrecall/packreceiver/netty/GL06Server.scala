package ru.sosgps.wayrecall.packreceiver.netty

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util
import java.util.Date

import io.netty.buffer.{ByteBuf, ByteBufAllocator, Unpooled}
import io.netty.channel.{ChannelHandlerContext, ChannelPipeline, SimpleChannelInboundHandler}
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.logging.LoggingHandler
import io.netty.util.CharsetUtil
import org.traccar.helper.Checksum
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.{Netty4Server, PackProcessor}
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext
import ru.sosgps.wayrecall.utils.io.Utils

import scala.collection.JavaConverters.mapAsJavaMapConverter


class GL06Server extends Netty4Server with grizzled.slf4j.Logging {

  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[GL06Server]),
      new GL06MessageDecoder(),
      new GL06AvlDataDecoder(store)
    )
  }

}

class GL06MessageDecoder extends ByteToMessageDecoder with grizzled.slf4j.Logging {

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (in.readableBytes() < 5)
      return

    val initialIndex = in.readerIndex()
    val firstByte = in.getByte(in.readerIndex())
    require(in.readByte() == 0x78, s"first byte should be 0x78 not ${in.getByte(in.readerIndex() - 1)}")
    require(in.readByte() == 0x78, s"first byte should be 0x78 not ${in.getByte(in.readerIndex() - 1)}")
    val lenght = in.readUnsignedByte()

    if (in.readableBytes() < lenght + 2) {
      debug(s"not enought data to read lenght = $lenght in.readableBytes() = ${in.readableBytes()} ")
      in.readerIndex(initialIndex)
      return
    }

    val body = in.readSlice(lenght).retain()
    debug("incoming body =" + body.toHexString)
    out.add(body)
    require(in.readByte() == 0x0D, s"stop bit should be 0x0D not ${in.getByte(in.readerIndex() - 1)}")
    require(in.readByte() == 0x0A, s"stop bit should be 0x0A not ${in.getByte(in.readerIndex() - 1)}")
  }
}


class GL06AvlDataDecoder(store: PackProcessor) extends SimpleChannelInboundHandler[ByteBuf] with grizzled.slf4j.Logging {

  val dateFormat = DateTimeFormatter.ofPattern("yyMMdd")
  val timeFormat = DateTimeFormatter.ofPattern("HHmmss")

  private var imei: Option[String] = None

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {

    debug(s"receiving: ${msg.toString(CharsetUtil.US_ASCII)}")

    val protocolNum = msg.readByte()
    debug(s"protocolNum = $protocolNum")

    protocolNum match {
      case 0x01 /* Login */ =>
        imei = Some(Utils.toHexString(msg.readBytesToArray(8), "").dropWhile(_ == '0'))
        ctx.writeAndFlush(response(ctx.alloc(), 0x01, 0x01))
      case 0x12 /* Location Data Packet */ => {
        val year = msg.readUnsignedByte() + 2000
        val month = msg.readUnsignedByte()
        val day = msg.readUnsignedByte()
        val hour = msg.readUnsignedByte()
        val minute = msg.readUnsignedByte()
        val second = msg.readUnsignedByte()
        val dateTime = ZonedDateTime.of(year, month, day, hour, minute, second, 0, ZoneId.of("UTC"))
        val satelites = msg.readUnsignedByte()
        val lat = msg.readUnsignedInt() / 30000.0 / 60
        val lon = msg.readUnsignedInt() / 30000.0 / 60
        val speed = msg.readUnsignedByte()
        val course = msg.readUnsignedShort() & 0x03FF
        val LBS = Map[String, AnyRef](
          "MCC" -> msg.readUnsignedShort().asInstanceOf[AnyRef],
          "MNC" -> msg.readUnsignedByte().asInstanceOf[AnyRef],
          "LAC" -> msg.readUnsignedShort().asInstanceOf[AnyRef],
          "CID" -> msg.readUnsignedMedium().asInstanceOf[AnyRef]
        ).asJava
        val serial = msg.readShort()
        val data = new GPSData(null, imei.get, lon, lat, Date.from(dateTime.toInstant), speed, course.toShort, satelites.toByte)
        data.data.putAll(LBS)
        store.addGpsDataAsync(data).onComplete(r => debug("save result: " + r))
      }
      case 0x13 /* status */ => {
        msg.skipBytes(5) // status info
        val serial = msg.readShort()
        ctx.writeAndFlush(response(ctx.alloc(), 0x13, serial))
      }

      case other => warn(s"unknown command $other")

    }


  }

  private def response(allocator: ByteBufAllocator, protocol: Int, serial: Int) = {
    val body = Unpooled.buffer(4)
      .writeByte(0x05) // lenght
      .writeByte(protocol) // protocolNum
      .writeShort(serial) // information serial

    val checksum = Checksum.crc16(Checksum.CRC16_X25, body.nioBuffer())

    allocator.buffer(10)
      .writeByte(0x78)
      .writeByte(0x78)
      .writeAndRelease(body)
      .writeShort(checksum)
      .writeByte(0x0D) // stop bit
      .writeByte(0x0A) // stop bit
  }

  private def str(string: String): ByteBuf = Unpooled.wrappedBuffer(string.getBytes(CharsetUtil.US_ASCII))

}
