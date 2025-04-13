package ru.sosgps.wayrecall.packreceiver.netty

import java.time.{LocalDate, LocalTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.Date

import com.google.common.base.Charsets
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelHandlerContext, ChannelPipeline, SimpleChannelInboundHandler}
import io.netty.handler.codec.{DelimiterBasedFrameDecoder, Delimiters}
import io.netty.handler.logging.LoggingHandler
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.{Netty4Server, PackProcessor}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

/**
  * Created by nickl on 18.02.17.
  */
class GTLT3MT1Server extends Netty4Server with grizzled.slf4j.Logging {

  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[GTLT3MT1Server]),
      new DelimiterBasedFrameDecoder(Short.MaxValue.toInt, Unpooled.wrappedBuffer(Array[Byte]('#'))),
      new GTLT3MT1Decoder(store)
    )
  }

}

class GTLT3MT1Decoder(store: PackProcessor) extends SimpleChannelInboundHandler[ByteBuf] with grizzled.slf4j.Logging {

  val dateFormat = DateTimeFormatter.ofPattern("ddMMyy")
  val timeFormat = DateTimeFormatter.ofPattern("HHmmss")

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {


    val message = msg.toString(Charsets.US_ASCII)
    debug("received:" + message)
    if (message.isEmpty)
      return

    val elems = message.split(",").map(_.trim).toSeq

    debug("elems = " + elems.zipWithIndex.map(e => e._2 + " -> " + e._1).mkString("\n"))

    val serial = elems(1)
    val command = elems(2)
    val time = LocalTime.parse(elems(3), timeFormat)
    val validity = elems(4)
    val latStr = elems(5)
    val northSouth = elems(6)
    val lonStr = elems(7)
    val eastWest = elems(8)
    val speed = elems(9)
    val direction = elems(10)
    val date = LocalDate.parse(elems(11), dateFormat).atStartOfDay(ZoneId.of("UTC"))
    val status = elems(12)

    val datetime = date.`with`(time)

    val lat = {
      val deg = latStr.take(2).toDouble
      val min = latStr.drop(2).toDouble
      deg + min / 60
    }

    val lon = {
      val deg = lonStr.take(3).toDouble
      val min = lonStr.drop(3).toDouble
      deg + min / 60
    }

    val gpsdata = new GPSData(null,
      serial,
      lon, lat, Date.from(datetime.toInstant),
      speed.toDouble.toShort, direction.toDouble.toShort, 0.toByte)

    gpsdata.data.put("protocol", "GTLT3MT1")


    store.addGpsDataAsync(gpsdata) onComplete {
      case Success(v) => ctx.writeAndFlush(ctx.alloc().buffer()
        .writeBytes((elems.take(4).mkString(",") + "#").getBytes(Charsets.US_ASCII)))
      case Failure(e) =>
        warn(s"exception processimg data $elems ", e)
        ctx.close()
    }


  }

}