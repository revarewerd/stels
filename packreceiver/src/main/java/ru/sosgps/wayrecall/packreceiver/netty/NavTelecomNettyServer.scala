package ru.sosgps.wayrecall.packreceiver.netty


import java.util.{TimerTask, Timer}

import com.google.common.util.concurrent.MoreExecutors
import io.netty.buffer.{Unpooled, ByteBufInputStream, ByteBuf}
import io.netty.channel.{ChannelOutboundHandlerAdapter, ChannelPipeline, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.ReferenceCountUtil
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.avlprotocols.common.ConnectedDevicesWatcher
import ru.sosgps.wayrecall.data.IllegalImeiException
import ru.sosgps.wayrecall.packreceiver.{Netty4Server, PackProcessor, Netty3Server}
import scala.collection.JavaConversions
import scala.concurrent.{Future, ExecutionContext}
import scala.beans.BeanProperty
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import java.nio.ByteOrder
import ru.sosgps.wayrecall.avlprotocols.navtelecom._
import ru.sosgps.wayrecall.utils.io.{RichDataInput, Utils}
import java.io.DataInput
import ru.sosgps.wayrecall.avlprotocols.ruptela.ProtocolExceptionWithResponse
import resource._

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 13.03.13
 * Time: 21:42
 * To change this template use File | Settings | File Templates.
 */
class NavTelecomNettyServer extends Netty4Server {


  val activeConnections = new ConnectedDevicesWatcher[NavtelecomAvlDataDecoder](_.connproc.imei)

  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {

    val connproc = new NavtelecomConnectionProcessor(store)(ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext)

    val firstStepDecoder = new NavtelecomFirstStepDecoder(connproc)

    val avlDataDecoder: NavtelecomAvlDataDecoder = new NavtelecomAvlDataDecoder(connproc)
    pipeline.addLast(
      new LoggingHandler(classOf[NavTelecomNettyServer]),
      activeConnections.connectionWatcherFor(avlDataDecoder),
      firstStepDecoder,
      avlDataDecoder
    )

  }

  //def commandsAutorized(imei: String) = imeiConnectionsMap.get(imei).map(_.connproc.)

  def sendCommand(imei: String, command: NavtelecomCommand) = {
    activeConnections(imei).sendCommands(command)
  }

  def hasNoCommands = !activeConnections.devices.values.map(_.connproc.hasPendingCommands).fold(false)(_ || _)

}


class NavtelecomFirstStepDecoder(val conproc: NavtelecomConnectionProcessor)
  extends LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, 65536, 12, 2, 2, 0, true)
  with grizzled.slf4j.Logging {

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf): AnyRef = {
    val readerIndex: Int = in.readerIndex
    val flexSize = conproc.flexSize + 3
    if (in.readableBytes() > 0) {
      trace(s"in.getByte($readerIndex)=" + in.getByte(readerIndex).toInt.toHexString)
      if (in.getByte(readerIndex) == 0x7f) {
        in.readByte()
        return null;
      }
    }
    if (in.readableBytes() > 12 ) {
      trace(s"size=" + in.order(ByteOrder.LITTLE_ENDIAN).getUnsignedShort(readerIndex+12)+" / "+in.readableBytes())
    }

    if (in.readableBytes() > 0 && in.getByte(readerIndex) == '~') {
      trace("flexPackage flexSize="+flexSize)
      if (in.readableBytes() >= flexSize) {
        val packsize = in.getByte(readerIndex + 1) match {
          case 'C' => flexSize
          case 'A' =>
            val Asize = in.getUnsignedByte(readerIndex + 2)
            trace(s"Asize=$Asize")
            Asize * (flexSize - 3) + 4 // 12     // 2 - 8, 5 - 11, 7 - 17
          case 'T' => flexSize + 4
        }
        if (in.readableBytes() >= packsize) {
          val frame: ByteBuf = extractFrame(ctx, in, readerIndex, packsize)
          in.readerIndex(readerIndex + packsize)
          frame
        }
        else {
          trace("readableBytes: " + in.readableBytes() + " but nedded " + packsize)
          null
        }
      }
      else
        null
    }
    else super.decode(ctx: ChannelHandlerContext, in: ByteBuf)
  }

  override def extractFrame(ctx: ChannelHandlerContext, buffer: ByteBuf, index: Int, length: Int): ByteBuf =
    super.extractFrame(ctx, buffer, index, length).order(ByteOrder.LITTLE_ENDIAN)
}

class NavtelecomAvlDataDecoder(val connproc: NavtelecomConnectionProcessor) extends ChannelInboundHandlerAdapter with PackProcessorWriterHandler {

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {

    if (!cause.isInstanceOf[org.jboss.netty.handler.timeout.ReadTimeoutException])
      logger.debug("Unexpected exception from downstream.", cause)
    ctx.channel().close
  }

  var ctx: ChannelHandlerContext = null

  override def channelRegistered(ctx: ChannelHandlerContext): Unit = {
    super.channelRegistered(ctx)
    this.ctx = ctx
  }

  def sendCommands(commands: NavtelecomCommand*): Unit ={
    this.connproc.enqueueCommands(commands:_*)
    ctx.writeAndFlush(Unpooled.wrappedBuffer(this.connproc.dequeSendingCommadsAsBinary():_*))
  }

  //private val timer = new Timer()

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit =

    msg match {
      case channelBuffer: ByteBuf => try {
        debug("channelBuffer=" + Utils.toHexString(channelBuffer, ""))
            //21s21
        try {
          val results = connproc.processPackage(channelBuffer)

            results.onFailure(stdExceptionHandler(ctx))
            results.onSuccess {
              case arr =>
                debug("resp=" + Utils.toHexString(arr, ""))
                if (arr.readableBytes() > 0) {
                  ctx.channel().writeAndFlush(arr)
                }

//                debug("scheduling task")
//                timer.schedule(new TimerTask {
//                  override def run(): Unit = {
//                    debug("running scheduled task")
//                    connproc.getCommands.foreach(_.onSuccess { case bb =>
//                      if (bb.readableBytes() > 0) {
//                        debug("writing scheduled:" + Utils.toHexString(bb, " "))
//                        ctx.channel().writeAndFlush(bb)
//                      }
//                    })
//
//                  }
//                }, 5000)

            }
          }
          catch stdExceptionHandler(ctx)


      } finally {
        ReferenceCountUtil.release(msg);
      }

      case _ => super.channelRead(ctx, msg)
    }

}






