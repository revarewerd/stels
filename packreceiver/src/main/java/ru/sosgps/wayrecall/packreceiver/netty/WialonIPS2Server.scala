package ru.sosgps.wayrecall.packreceiver.netty

import com.google.common.base.Charsets
import io.netty.buffer.{ByteBuf, ByteBufInputStream}
import io.netty.channel.{ChannelFutureListener, ChannelHandlerAdapter, ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelPipeline, SimpleChannelInboundHandler}
import io.netty.handler.codec.compression.{JZlibDecoder, JdkZlibDecoder, ZlibDecoder}
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import io.netty.handler.codec.{ByteToMessageDecoder, LineBasedFrameDecoder}
import io.netty.handler.logging.LoggingHandler
import org.apache.commons.compress.archivers.sevenz.Coders
import org.apache.commons.compress.compressors.gzip.{GzipCompressorInputStream, GzipUtils}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.{Netty4Server, PackProcessor}

import java.time.{LocalDate, LocalTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Date
import scala.util.{Failure, Success}
import scala.util.matching.Regex
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext
import ru.sosgps.wayrecall.utils.{tryNumerics, tryParseDouble}

import java.nio.ByteOrder
import java.util
import java.util.zip.{Deflater, Inflater}
import scala.concurrent.Future

class WialonIPS2Server extends Netty4Server with grizzled.slf4j.Logging {
  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[WialonIPS2Server]),
      new WialonIPSDeflateAwareDecoder(),
      new StringDecoder(Charsets.US_ASCII),
      new WialonIPS2Decoder(store)
    )
  }
}

private class WialonIPSDeflateAwareDecoder extends ChannelInboundHandlerAdapter {

  private val lineBasedFrameDecoder = new LineBasedFrameDecoder(10240)
  private val zlibDecoder = new JdkZlibDecoder()

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    if (!msg.isInstanceOf[ByteBuf]) {
      lineBasedFrameDecoder.channelRead(ctx, msg)
      return
    }
    val in = msg.asInstanceOf[ByteBuf]
    if (in.readableBytes() < 2) return
    if (in.getByte(in.readerIndex()) != -1) {
      lineBasedFrameDecoder.channelRead(ctx, msg)
      return
    }

    val size = in.order(ByteOrder.LITTLE_ENDIAN).getShort(in.readerIndex() + 1)
    if (in.readableBytes() < size + 3) return
    zlibDecoder.channelRead(ctx, in.slice(3, size))
  }
}

class WialonIPS2Decoder(store: PackProcessor) extends SimpleChannelInboundHandler[String] with grizzled.slf4j.Logging {

  var imei: Option[String] = None
  var protocolVersion: Option[String] = None

  import WialonIPS2Decoder._

  override def channelRead0(ctx: ChannelHandlerContext, msg: String): Unit = {
    val Some(List(key, body)) = WialonIPS2Decoder.messageRexexp.unapplySeq(msg)
    val bodyParts = new BodyParts(body.split(";"))
    key match {
      case "L" /* Login */ => {
        if (bodyParts.length > 2) {
          protocolVersion = Some(bodyParts(0))
          imei = Some(bodyParts(1))
          answer(ctx, "AL", "1")
        } else {
          imei = Some(bodyParts(0))
          answer(ctx, "AL", "1")
        }
      }
      case "P" /* Ping */ => answer(ctx, "AP", "")
      case "SD" /* Short Data package */ =>
        val gpsdata: GPSData = readShortData(bodyParts)
        store.addGpsDataAsync(gpsdata) onComplete {
          case Success(v) =>
            answer(ctx, "ASD", "1")
          case Failure(e) =>
            answer(ctx, "ASD", "-1").addListener(ChannelFutureListener.CLOSE)
        }

      case "D" /* Data package */ =>
        val gpsdata = readShortData(bodyParts)
        readRemainingToFull(bodyParts, gpsdata)
        store.addGpsDataAsync(gpsdata) onComplete {
          case Success(v) =>
            answer(ctx, "AD", "1")
          case Failure(e) =>
            answer(ctx, "AD", "-1").addListener(ChannelFutureListener.CLOSE)
        }

      case "B" /* Blackbox package */ =>

        val bbSeparated = body.split("\\|").toIndexedSeq
          .map(part => new BodyParts(part.split(";")))

        val gpsDatas = bbSeparated.filter(_.length > 1)
          .map(bodyParts => {
            val gpsdata = readShortData(bodyParts)
            if (bodyParts.length > 10) {
              readRemainingToFull(bodyParts, gpsdata)
            }
            gpsdata
          })

        Future.sequence(gpsDatas.map(store.addGpsDataAsync)) onComplete {
          case Success(v) =>
            answer(ctx, "AB", v.size.toString)
          case Failure(e) =>
            answer(ctx, "AB", "").addListener(ChannelFutureListener.CLOSE)
        }

    }
  }

  private def readRemainingToFull(bodyParts: BodyParts, gpsdata: GPSData): Unit = {
    bodyParts.vat(10).map(_.toFloat).foreach(v => gpsdata.data.put("hdop", v.asInstanceOf[AnyRef]))
    bodyParts.vat(11).map(_.toInt).foreach(v => gpsdata.data.put("inputs", v.asInstanceOf[AnyRef]))
    bodyParts.vat(12).map(_.toInt).foreach(v => gpsdata.data.put("outputs", v.asInstanceOf[AnyRef]))
    bodyParts.vat(13).foreach(gpsdata.data.put("adc", _))
    bodyParts.vat(14).foreach(gpsdata.data.put("lbutton", _))
    bodyParts.vat(15).foreach(allParams => {
      allParams.split(",").foreach(pair => {
        val splitAt = pair.lastIndexOf(":")
        if (splitAt > -1) {
          val (key, value) = pair.splitAt(splitAt + 1)
          gpsdata.data.put(key.stripSuffix(":"), tryParseDouble(value).map(_.asInstanceOf[AnyRef]).getOrElse(value))
        }
        else {
          gpsdata.data.put(pair, "")
        }
      })
    })
  }

  private def readShortData(bodyParts: BodyParts) = {
    val datetime = (for (date <- bodyParts.vat(0).map(LocalDate.parse(_, dateFormat).atStartOfDay(ZoneId.of("UTC")));
                         time <- bodyParts.vat(1).map(LocalTime.parse(_, timeFormat)))
      yield date.`with`(time)).getOrElse(ZonedDateTime.now())

    val lat = parsePost(bodyParts, 2, 2, "S").getOrElse(Double.NaN)
    val lon = parsePost(bodyParts, 4, 3, "W").getOrElse(Double.NaN)

    val speed = bodyParts.vat(6).map(_.toInt)
    val course = bodyParts.vat(7).map(_.toInt)
    val height = bodyParts.vat(8).map(_.toInt)
    val sats = bodyParts.vat(9).map(_.toInt)

    val gpsdata = new GPSData(null,
      imei.get,
      lon, lat, Date.from(datetime.toInstant),
      speed.getOrElse(0).toShort, course.getOrElse(0).toShort, sats.getOrElse(0).toByte)

    height.foreach(v => gpsdata.data.put("height", v.asInstanceOf[AnyRef]))

    gpsdata.data.put("protocol", "WialonIPS")
    gpsdata.data.put("protocolversion", protocolVersion.getOrElse(1).toString)
    gpsdata
  }

  private def parsePost(bodyParts: BodyParts, offset: Int, grad: Int, reverseLetter: String) = {
    for (lat1 <- bodyParts.vat(offset);
         lat2 <- bodyParts.vat(offset + 1))
      yield {
        val (g, m) = lat1.splitAt(grad)
        val r = g.toDouble + m.toDouble / 60
        if (lat2 == reverseLetter) -r else r
      }
  }



  private def answer(ctx: ChannelHandlerContext, kode: String, body: String) = {
    val codeBytes = kode.getBytes(Charsets.US_ASCII)
    val bodyBytes = body.getBytes(Charsets.US_ASCII)
    val answerBuf = ctx.alloc().buffer(codeBytes.length + bodyBytes.length + 2)
    answerBuf.writeByte(0x23)
    answerBuf.writeBytes(codeBytes)
    answerBuf.writeByte(0x23)
    answerBuf.writeBytes(bodyBytes)
    answerBuf.writeByte(0x0d)
    answerBuf.writeByte(0x0a)
    ctx.writeAndFlush(answerBuf)
  }
}

private class BodyParts(val bodyPartsArray: Array[String]) {
  def apply(i: Int): String = bodyPartsArray(i)

  def length: Int = bodyPartsArray.length

  def vat(i: Int): Option[String] = Some(bodyPartsArray(i)).filter(_ != "NA")
}

object WialonIPS2Decoder {

  val messageRexexp: Regex = "#(\\w+)#(.*)$".r

  val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyy")
  val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss")

}