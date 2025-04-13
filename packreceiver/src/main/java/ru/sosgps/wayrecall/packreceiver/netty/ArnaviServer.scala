package ru.sosgps.wayrecall.packreceiver.netty

import java.nio.charset.Charset
import java.time.{LocalDate, LocalTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Date

import com.google.common.base.Charsets
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelPipeline, SimpleChannelInboundHandler}
import io.netty.handler.codec.{DelimiterBasedFrameDecoder, Delimiters}
import io.netty.handler.logging.LoggingHandler
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.{Netty4Server, PackProcessor}
import ru.sosgps.wayrecall.utils.io.asyncs.NettyFutureHandler
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by nickl on 16.01.17.
  */
class ArnaviServer extends Netty4Server with grizzled.slf4j.Logging {

  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[ArnaviServer]),
      new DelimiterBasedFrameDecoder(Short.MaxValue.toInt, Delimiters.lineDelimiter() ++ Delimiters.nulDelimiter(): _*),
      new ArnaviAvlDataDecoder(store)
    )
  }

}

class ArnaviAvlDataDecoder(store: PackProcessor) extends SimpleChannelInboundHandler[ByteBuf] with grizzled.slf4j.Logging {

  val resp = "RCPTOK\r\n".getBytes

  val dateFormat = DateTimeFormatter.ofPattern("ddMMyy")
  val timeFormat = DateTimeFormatter.ofPattern("HHmmss")

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {


    val message = msg.toString(Charsets.US_ASCII)
    debug("received:" + message)


    val elems = message.split(",").map(_.trim).toSeq

    debug("elems = " + elems.zipWithIndex.map(e => (e._2) + " -> " + e._1).mkString("\n"))

    require(elems.head == "$AV", s"elems.head should be $$AV but found $elems")


    (elems(1) match {
      case "V2" => store.addGpsDataAsync(parseV2(elems))
      case "V3" => store.addGpsDataAsync(parseV3(elems))
      case other =>
        warn("unknown message: " + elems)
        Future.successful()
    }) onComplete {
      case Success(v) => ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(resp))
      case Failure(e) =>
        warn(s"exception processimg data $elems ", e)
        ctx.close()
    }


  }

  private def parseV2(elems: Seq[String]) = {
    val trackerID = elems(2)
    val Serial = elems(3)
    val VIN = elems(4)
    val VBAT = elems(5)
    val FSDATA = elems(6)
    val ISSTOP = elems(7)
    val ISEGNITION = elems(8)
    val D_STATE = elems(9)
    val FREQ1 = elems(10)
    val COUNT1 = elems(11)
    val FIX_TYPE = elems(12)
    val SAT_COUNNT = elems(13)
    val TIME = elems(14)
    val XCOORD = elems(15)
    val YCOORD = elems(16)
    val SPEED = elems(17)
    val COURSE = elems(18)
    val DATE = elems(19)
    val chechsum = elems(20)

    val lon = coord(XCOORD)
    val lat = coord(YCOORD)

    val date = LocalDate.parse(DATE, dateFormat).atStartOfDay(ZoneId.of("UTC"))
    val time = LocalTime.parse(TIME, timeFormat)

    val datetime = date.`with`(time)

    val gpsdata = new GPSData(null,
      trackerID + "-" + Serial,
      lon, lat, Date.from(datetime.toInstant),
      SPEED.toDouble.toShort, COURSE.toDouble.toShort, SAT_COUNNT.toByte)

    gpsdata.data.put("protocol", "Arnavi")
    gpsdata
  }

  private def parseV3(elems: Seq[String]) = {

    val trackerID = elems(2)
    val Serial = elems(3)
    val VIN = elems(4)
    val VBAT = elems(5)
    val FSDATA = elems(6)
    val ISSTOP = elems(7)
    val ISEGNITION = elems(8)
    val D_STATE = elems(9)

    val FREQ1 = elems(10)
    val COUNT1 = elems(11)
    val FREQ2 = elems(12)
    val COUNT2 = elems(13)
    val FIX_TYPE = elems(14)
    val SAT_COUNNT = elems(15)
    val TIME = elems(16)
    val XCOORD = elems(17)

    val YCOORD = elems(18)
    val SPEED = elems(19)
    val COURSE = elems(20)
    val DATE = elems(21)
    val ADC1 = elems(22)
    val COUNTER3 = elems(23)
    val TS_TEMP = elems(24)
    val chechsum = elems(25)

    val lon = coord(XCOORD)
    val lat = coord(YCOORD)

    val date = LocalDate.parse(DATE, dateFormat).atStartOfDay(ZoneId.of("UTC"))
    val time = LocalTime.parse(TIME, timeFormat)

    val datetime = date.`with`(time)

    val gpsdata = new GPSData(null,
      trackerID + "-" + Serial,
      lon, lat, Date.from(datetime.toInstant),
      SPEED.toDouble.toShort, COURSE.toDouble.toShort, SAT_COUNNT.toByte)

    gpsdata.data.put("protocol", "Arnavi")
    gpsdata
  }

  private def coord(value: String): Double = {
    val d = value.replaceAll("[EN]$", "").toDouble
    val deg = d.toInt / 100
    val min = d - deg * 100
    deg + min / 60
  }
}