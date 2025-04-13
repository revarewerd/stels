package ru.sosgps.wayrecall.packreceiver.netty

import ru.sosgps.wayrecall.data.IllegalImeiException
import ru.sosgps.wayrecall.packreceiver.{PackProcessor, Netty3Server}
import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.frame.{TooLongFrameException, FrameDecoder, LengthFieldBasedFrameDecoder}
import org.jboss.netty.handler.timeout.ReadTimeoutHandler
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}
import scala.beans.BeanProperty
import ru.sosgps.wayrecall.avlprotocols.ruptela.{ProtocolExceptionWithResponse, RuptelaParser, RuptelaPackProcessor}
import org.jboss.netty.buffer.{BigEndianHeapChannelBuffer, ChannelBuffers, ChannelBufferInputStream, ChannelBuffer}
import resource._
import ru.sosgps.wayrecall.utils.io.{Utils, RichDataInput}
import ru.sosgps.wayrecall.avlprotocols.gosafe.GosafeParser
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import ru.sosgps.wayrecall.data.sleepers.LBSConverter
import org.springframework.beans.factory.annotation.Autowired
import java.io.DataInput

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Created by nickl on 18.03.14.
 */
class GosafeNettyServer extends Netty3Server {
  protected def getPipelineFactory: ChannelPipelineFactory = {
    return new ChannelPipelineFactory {
      def getPipeline: ChannelPipeline = {
        return Channels.pipeline(
          new ReadTimeoutHandler(timer, 30, TimeUnit.MINUTES),
          new GosafeFrameDecoder,
          new GosafeAvlDataDecoder(store, lbsConverter)
        )
      }
    }
  }

  @Autowired
  @BeanProperty
  var lbsConverter: LBSConverter = null
}


class GosafeFrameDecoder extends FrameDecoder with grizzled.slf4j.Logging {


  val maxFrameLength = 10240

  private val bF8: Byte = 0xF8.asInstanceOf[Byte]

  override def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): AnyRef = {

    val frameLength = buffer.readableBytes()
    trace("frameLength = " + frameLength)
    trace("buffer = " + Utils.toHexString(buffer.toByteBuffer.array(), ""))
    if (frameLength > maxFrameLength)
      Channels.fireExceptionCaught(ctx.getChannel, new TooLongFrameException("frame length exceeds " + maxFrameLength + ": " + frameLength + " - discarded"))

    if (buffer.readableBytes() < 2)
      return null

    buffer.markReaderIndex()
    val fb = buffer.readByte()
    trace("fb=" + fb)
    if (fb != bF8 && fb != '*') {
      error("GosafeFrame first byte is " + fb + " not " + bF8)
      Channels.fireExceptionCaught(ctx.getChannel, new IllegalArgumentException("GosafeFrame first byte is " + fb + " not " + bF8 + "and not " + '*'.toByte))
    }


    trace("buffer.readerIndex()=" + buffer.readerIndex())
    trace("buffer.readableBytes()=" + buffer.readableBytes())
    val laspackIndex = if (fb == bF8)
      buffer.indexOf(buffer.readerIndex() + 1, buffer.readerIndex() + buffer.readableBytes() + 1, bF8)
    else
      buffer.indexOf(buffer.readerIndex() + 1, buffer.readerIndex() + buffer.readableBytes() + 1, '#'.toByte)

    trace("laspackIndex=" + laspackIndex)
    if (laspackIndex == -1) {
      buffer.resetReaderIndex()
      return null;
    }

    val length = laspackIndex - buffer.readerIndex() + 1

    trace("length=" + length)
    //buffer.resetReaderIndex()
    buffer.skipBytes(-1)
    val bytes = buffer.readBytes(length + 1)

    trace("bytes = " + Utils.toHexString(bytes.array(), ""))

    return bytes

  }
}

class GosafeAvlDataDecoder(val store: PackProcessor, val lbsConverter: LBSConverter) extends SimpleChannelUpstreamHandler with PackProcessorN3WriterHandler {

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  @BeanProperty
  var lbsClientPool = new ScalaExecutorService("gosafeLBSclient", 10, 30, true, 1L, TimeUnit.MINUTES, new ArrayBlockingQueue[Runnable](50))

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
        val input = new RichDataInput(is: DataInput)
        try {
          val parseResult = GosafeParser.processPackage(input)
          trace("parseResult " + parseResult)
          for (gps <- parseResult.gpsData) {

            GosafeParser.appendAdditionalData(parseResult, gps)
            trace("writing " + gps)
            store.addGpsDataAsync(gps).onComplete(confirmOrClose(ctx, sendConfirmation(ctx)))
          }

          if (parseResult.lbsData.nonEmpty) {
            lbsClientPool.future {
                val gpss = GosafeParser.convertLBSToGpsData(lbsConverter, parseResult)
                 for (gps <- gpss) {
                  GosafeParser.appendAdditionalData(parseResult, gps)
                  store.addGpsDataAsync(gps).onComplete(confirmOrClose(ctx, sendConfirmation(ctx)))
                }
              //Future.sequence(writing)
            }
            //sendConfirmation(ctx)
          }

        }
        catch {
          case e: ProtocolExceptionWithResponse => {
            warn(e.toString)
            //debug("ProtocolExceptionWithResponse response for " + incomingPackage.imei + " = " + Utils.toHexString(e.respData, ""))
            ctx.getChannel.write(ChannelBuffers.wrappedBuffer(e.respData))
            ctx.getChannel.close()
          }
          case e: IllegalImeiException => {
            warn(e.toString)
            ctx.getChannel.close()
          }
          case e: Exception => {
            error(e.toString, e)
            ctx.getChannel.close()
          }
        }

      }
    }
    else {
      super.messageReceived(ctx, e)
    }
  }


  private[this] def sendConfirmation(ctx: ChannelHandlerContext): ChannelFuture = {
    val buffer = ChannelBuffers.buffer(1)
    buffer.writeByte(0x01)
    ctx.getChannel.write(buffer)
  }
}
