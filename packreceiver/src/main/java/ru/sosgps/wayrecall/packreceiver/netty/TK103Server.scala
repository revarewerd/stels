package ru.sosgps.wayrecall.packreceiver.netty

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalTime, ZoneId}
import java.util
import java.util.Date

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelHandlerContext, ChannelPipeline, SimpleChannelInboundHandler}
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.logging.LoggingHandler
import io.netty.util.CharsetUtil
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.{Netty4Server, PackProcessor}
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

import scala.util.{Failure, Success}


/**
  * Created by nickl on 08.03.17.
  */
class TK103Server extends Netty4Server with grizzled.slf4j.Logging {

  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[TK103Server]),
      new TK103MessageDecoder(),
      new TK103rAvlDataDecoder(store)
    )
  }

}

class TK103MessageDecoder extends ByteToMessageDecoder  with grizzled.slf4j.Logging {

  val START_MARKER = 0x28
  val END_MARKER = 0x29

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if(in.readableBytes() <= 0)
      return

    val firstByte = in.getByte(in.readerIndex())
    require(firstByte == START_MARKER, s"first byte should be $START_MARKER not $firstByte")
    val end = in.findByteInReadable(END_MARKER)
    if(end != -1){
      out.add(in.readSlice(end - in.readerIndex() + 1).retain())
    }
  }
}


class TK103rAvlDataDecoder(store: PackProcessor) extends  SimpleChannelInboundHandler[ByteBuf] with grizzled.slf4j.Logging {

  val dateFormat = DateTimeFormatter.ofPattern("yyMMdd")
  val timeFormat = DateTimeFormatter.ofPattern("HHmmss")

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {

    debug(s"receiving: ${msg.toString(CharsetUtil.US_ASCII)}")

    msg.skipBytes(1)
    val serial = msg.readBytesAsString(12)
    debug(s"serial = $serial")
    val command = msg.readBytesAsString(4)
    debug(s"command = $command")
    val body = msg.readBytes(msg.readableBytes() - 1)
    debug(s"body = ${body.toString(CharsetUtil.US_ASCII)}")

    val imei = "3528" + serial.dropWhile(_ == '0')

    command match {
      case "BP00" => ctx.writeAndFlush(str("(" + serial + "AP01" + "HSO"+ ")"))
      case "BP05" => ctx.writeAndFlush(str("(" + serial + "AP05" + "HSO"+ ")"))
      case "BR00" =>
        val date = LocalDate.parse(body.readBytesAsString(6), dateFormat).atStartOfDay(ZoneId.of("UTC"))
        val avaliability = body.readBytesAsString(1)
        val latstr = body.readBytesAsString(9)

        val lat = {
          val deg = latstr.take(2).toDouble
          val min = latstr.drop(2).toDouble
          deg + min / 60
        }
        val latLetter = body.readBytesAsString(1)

        val lonstr = body.readBytesAsString(10)

        val lon = {
          val deg = lonstr.take(3).toDouble
          val min = lonstr.drop(3).toDouble
          deg + min / 60
        }
        val lonLetter = body.readBytesAsString(1)

        val speed = body.readBytesAsString(5).toFloat.toShort

        val time = LocalTime.parse(body.readBytesAsString(6), timeFormat)
        val direction = body.readBytesAsString(6).toFloat

        val datetime = date.`with`(time)

        val gpsdata = new GPSData(null,
          imei,
          lon, lat, Date.from(datetime.toInstant),
          speed.toDouble.toShort, direction.toShort, 9.toByte)

        gpsdata.data.put("protocol", "TK103")

        store.addGpsDataAsync(gpsdata) onComplete {
          case Success(v) =>
            ctx.writeAndFlush(str("(" + serial + "AR03" + ")"))
          case Failure(e) =>
            warn(s"exception processimg data $gpsdata ", e)
            ctx.close()
        }



      case other =>  warn(s"unknown command $other")
    }




  }

  private def str(string: String):ByteBuf = Unpooled.wrappedBuffer(string.getBytes(CharsetUtil.US_ASCII))

}
