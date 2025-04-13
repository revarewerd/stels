package ru.sosgps.wayrecall.packreceiver.netty

import java.net.InetSocketAddress
import java.nio.ByteOrder
import java.util

import io.netty.buffer.ByteBuf
import io.netty.channel.{SimpleChannelInboundHandler, ChannelHandlerContext, ChannelPipeline}
import io.netty.handler.codec.{LengthFieldBasedFrameDecoder, ByteToMessageDecoder}
import io.netty.handler.logging.LoggingHandler
import ru.sosgps.wayrecall.avlprotocols.autophonemayak.AutophoneMayak
import ru.sosgps.wayrecall.avlprotocols.autophonemayak.AutophoneMayak.AuthorizationPack
import ru.sosgps.wayrecall.packreceiver.{PackProcessor, Netty4Server}

import scala.beans.BeanProperty
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by nickl-mac on 20.12.15.
  */
class AutophoneMayakServer extends Netty4Server {


  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[AutophoneMayakServer]),
      new AutophoneMayakFrameDecoder,
      new AutophoneMayakAvlDataDecoder(store)
    )
  }
}

class AutophoneMayakFrameDecoder extends LengthFieldBasedFrameDecoder(512, 0, 0) {
  override def getUnadjustedFrameLength(buf: ByteBuf, offset: Int, length: Int, order: ByteOrder): Long = {
    buf.getByte(0) match {
      case AutophoneMayak.AUTH_PACK_CODE => 12
      case AutophoneMayak.WORKING_PACK_CODE => 78
      case AutophoneMayak.BLACKBOX_PACK_CODE => 257
    }
  }
}

class AutophoneMayakAvlDataDecoder(store: PackProcessor) extends SimpleChannelInboundHandler[ByteBuf]
with PackProcessorWriterHandler with grizzled.slf4j.Logging{

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  var imei: Option[String] = None

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {


    msg match {
      case AutophoneMayak.AuthorizationPack(receivedimei, crc) =>
        this.imei = Some(receivedimei)
        ctx.writeAndFlush(AutophoneMayak.answerCRC(crc))
      case AutophoneMayak.WorkingPack(receiveFor) =>
        val (gps, crc) = receiveFor(imei.get)
        store.addGpsDataAsync(gps) onComplete {
          case Success(_) => ctx.writeAndFlush(AutophoneMayak.answerCRC(crc))
          case Failure(e) =>
            debug("rejected" + gps)
            ctx.close()
        }
      case AutophoneMayak.BlackBoxPack(receiveFor) =>
        val (gpss, crc) = receiveFor(imei.get)
        Future.sequence(gpss.map(store.addGpsDataAsync)) onComplete {
          case Success(_) => ctx.writeAndFlush(AutophoneMayak.answerCRC(crc))
          case Failure(e) =>
            debug("rejected" + gpss)
            ctx.close()
        }

    }

  }
}

