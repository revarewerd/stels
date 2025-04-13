package ru.sosgps.wayrecall.packreceiver.netty

import java.nio.ByteOrder
import java.util

import io.netty.buffer.{Unpooled, ByteBuf}
import io.netty.channel.{ChannelHandlerContext, ChannelPipeline}
import io.netty.handler.codec.ReplayingDecoder
import io.netty.handler.logging.LoggingHandler
import ru.sosgps.wayrecall.avlprotocols.navtelecom.NavtelecomConnectionProcessor
import ru.sosgps.wayrecall.avlprotocols.skysim.{Header, Skysim}
import ru.sosgps.wayrecall.packreceiver.{PackProcessor, Netty4Server}

import scala.beans.BeanProperty
import scala.concurrent.Future
import scala.util.{Failure, Success}

import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

/**
 * Created by nickl on 17.04.15.
 */
class SkysimNettyServer extends Netty4Server {


  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[SkysimNettyServer]),
      new SkysimHander(store)
    )
  }
}

class SkysimHander(store: PackProcessor) extends ReplayingDecoder[Unit] with grizzled.slf4j.Logging with PackProcessorWriterHandler{

  private var header: Option[Header] = None

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {

    header match {
      case None => {
        val parsedHeader = Skysim.parseHeader(in)

        parsedHeader.version match {
          case Skysim.VERSION_HEADER =>
            ctx.writeAndFlush(Skysim.serverCom(0, Unpooled.buffer(0), ctx.alloc().buffer(4)))
          case Skysim.VERSION_HEADER2 =>
            val unixtime = (System.currentTimeMillis() / 1000).toInt
            ctx.writeAndFlush(
              Skysim.serverCom(
                0,
                Unpooled.buffer(4).order(ByteOrder.LITTLE_ENDIAN).writeInt(unixtime),
                ctx.alloc().buffer(9))
            )
        }


        header = Some(parsedHeader)
      }
      case Some(header) => {
        val (num, packets) = Skysim.parsePackage(in)
        val gpsData = packets.collect({
          case m: Map[String, Any] => Skysim.tagsToGPSData(m, header)
        })

        Future.sequence(gpsData.map(store.addGpsDataAsync)).onComplete {
          case Success(ss) =>
            debug("stored " + ss)
            ctx.writeAndFlush(Skysim.serverCom(num, Unpooled.buffer(0), ctx.alloc().buffer(4)))
          case Failure(e) =>
            debug("rejected " + gpsData)
            ctx.writeAndFlush(Skysim.serverCom(0xFE, Unpooled.buffer(0), ctx.alloc().buffer(4)))
        }

      }
    }
  }


}