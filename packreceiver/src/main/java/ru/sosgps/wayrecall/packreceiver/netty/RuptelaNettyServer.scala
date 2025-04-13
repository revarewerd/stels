package ru.sosgps.wayrecall.packreceiver.netty

import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.buffer.ChannelBufferInputStream
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.avlprotocols.ruptela.{ProtocolExceptionWithResponse, RuptelaIncomingPackage, RuptelaPackProcessor, RuptelaParser}
import ru.sosgps.wayrecall.avlprotocols.teltonika.TeltonikaParser
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.IllegalImeiException
import ru.sosgps.wayrecall.packreceiver.{Netty3Server, PackProcessor}
import ru.sosgps.wayrecall.utils.io.CRC16
import ru.sosgps.wayrecall.utils.io.RichDataInput
import ru.sosgps.wayrecall.utils.io.Utils
import java.io._
import java.util.Objects

import resource._

import scala.beans.BeanProperty
import org.jboss.netty.handler.timeout.ReadTimeoutHandler
import java.util.concurrent.TimeUnit

import org.jboss.netty.handler.logging.LoggingHandler
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 13.03.13
 * Time: 21:42
 * To change this template use File | Settings | File Templates.
 */
class RuptelaNettyServer extends Netty3Server {
  protected def getPipelineFactory: ChannelPipelineFactory = {
    return new ChannelPipelineFactory {
      def getPipeline: ChannelPipeline = {
        val firstStepDecoder = new LengthFieldBasedFrameDecoder(4096, 0, 2, 2, 0)
        val avlDataDecoder: RuptelaAvlDataDecoder = new RuptelaAvlDataDecoder(ruptelaStore)
        return Channels.pipeline(
          new LoggingHandler(classOf[RuptelaNettyServer]),
          new ReadTimeoutHandler(timer, 30, TimeUnit.MINUTES),
          firstStepDecoder,
          avlDataDecoder
        )
      }
    }
  }

  @BeanProperty
  var ruptelaStore: RuptelaPackProcessor = null

  override def setStore(store: PackProcessor): Unit = {
    super.setStore(store)
    ruptelaStore = new RuptelaPackProcessor(Objects.requireNonNull(store, "store"))(ScalaExecutorService.globalService.executionContext)
  }
}


class RuptelaAvlDataDecoder(val store: RuptelaPackProcessor) extends SimpleChannelUpstreamHandler with PackProcessorN3WriterHandler {

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    val cause: Throwable = e.getCause
    if (!cause.isInstanceOf[org.jboss.netty.handler.timeout.ReadTimeoutException])
      logger.debug("Unexpected exception from downstream.", cause)
    e.getChannel.close
  }

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    if (e.getMessage.isInstanceOf[ChannelBuffer]) {
      val channelBuffer: ChannelBuffer = e.getMessage.asInstanceOf[ChannelBuffer]

      for (is <- managed(new ChannelBufferInputStream(channelBuffer))) {
        val input = new RichDataInput(is:DataInput)
        val incomingPackage = RuptelaParser.parsePackage(input)
        try {
          val resultsF = store.process(incomingPackage)
          resultsF.onFailure(stdExceptionHandler(ctx))
          resultsF.onSuccess{
            case results =>
              val outBuffer: ChannelBuffer = ChannelBuffers.dynamicBuffer(8)
              results.filter(_.length != 0).foreach(outBuffer.writeBytes)
              //debug("response for " + incomingPackage.imei + " = " + Utils.toHexString(outBuffer.array(), ""))
              e.getChannel.write(outBuffer)
          }
        }
        catch stdExceptionHandler(ctx)

      }
    }
    else {
      super.messageReceived(ctx, e)
    }
  }

}