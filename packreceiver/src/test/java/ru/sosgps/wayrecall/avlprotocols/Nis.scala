package ru.sosgps.wayrecall.avlprotocols

import java.io.DataInput
import java.net.InetSocketAddress
import java.util.concurrent.{TimeUnit, Executors}

import org.jboss.netty.bootstrap.{ServerBootstrap, ClientBootstrap}
import org.jboss.netty.buffer.{ChannelBufferInputStream, ChannelBuffers, ChannelBuffer}
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.{NioServerSocketChannelFactory, NioClientSocketChannelFactory}
import org.jboss.netty.handler.codec.frame.{LengthFieldBasedFrameDecoder, FixedLengthFrameDecoder}
import org.jboss.netty.handler.logging.LoggingHandler
import org.jboss.netty.handler.timeout.ReadTimeoutHandler
import org.jboss.netty.logging.InternalLogLevel
import org.jboss.netty.util.HashedWheelTimer
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.utils.io.RichDataInput

/**
 * Created by nickl on 01.10.14.
 */
object Nis {

  @Autowired
  var timer: HashedWheelTimer = new HashedWheelTimer

  //TODO: возможно стоит сделать общий bootstrap
  private[this] val bootstrap = new ClientBootstrap(
    new NioClientSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()));

  // Set up the pipeline factory.
  bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
    def getPipeline: ChannelPipeline = Channels.pipeline(
      new ReadTimeoutHandler(timer, 30, TimeUnit.MINUTES),
      new FixedLengthFrameDecoder(1),
      new NisHandler
    )
  });

  def main(args: Array[String]) {
    val  serverBootsrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), 1, Executors.newCachedThreadPool(), 3))


    serverBootsrap.setPipelineFactory(new ChannelPipelineFactory {
      def getPipeline: ChannelPipeline = {
        return Channels.pipeline(
          new LoggingHandler(Nis.getClass,InternalLogLevel.INFO),
          new ReadTimeoutHandler(timer, 30, TimeUnit.MINUTES),
         new SimpleChannelUpstreamHandler{
           override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent): Unit = {
              e.getMessage match {
                case cb: ChannelBuffer => {
                  val array = cb.array()
                  println(new String(array))
                  val is = new RichDataInput(new ChannelBufferInputStream(cb): DataInput)
                  val prv = is.readUnsignedByte()
                  println("prv="+prv)
                  val skid = is.readUnsignedByte()
                  println("skid="+skid)
                  val prfrteenacmppr = is.readUnsignedByte()
                  println("prfrteenacmppr="+prfrteenacmppr)
                  val hl = is.readUnsignedByte()
                  println("hl="+hl)
                  val he = is.readUnsignedByte()
                  println("he="+he)
                  val fdl = is.readUnsignedShort()
                  println("fdl="+fdl)
                  val pid = is.readUnsignedShort()
                  println("pid="+pid)
                  val pt = is.readUnsignedByte()
                  println("pt="+pt)

                }
              }
           }
         }
        )
      }
    })

    serverBootsrap.bind(new InetSocketAddress(6013));

//    bootstrap.connect(new InetSocketAddress("77.243.111.83", 20438)).addListener(new ChannelFutureListener {
//      override def operationComplete(future: ChannelFuture): Unit = {
//        future.getChannel.write(ChannelBuffers.wrappedBuffer)
//      }
//    })

  }

}

class NisHandler extends SimpleChannelUpstreamHandler with grizzled.slf4j.Logging {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) = {
    val buffer = e.getMessage.asInstanceOf[ChannelBuffer]
    val byte = buffer.getByte(0)
    trace("NisHandler byte=" + byte + " " + ctx.getChannel.getRemoteAddress)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) = {
    debug("connecion closed by exception " + e.getCause + " addr:" + ctx.getChannel.getRemoteAddress)
    ctx.getChannel.close()
  }
}