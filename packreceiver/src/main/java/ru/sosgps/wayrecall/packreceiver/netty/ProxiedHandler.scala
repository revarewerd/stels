package ru.sosgps.wayrecall.packreceiver.netty

import java.net.SocketAddress

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.ReferenceCountUtil
import io.netty.util.concurrent.{Future, GenericFutureListener}
import ru.sosgps.wayrecall.utils.io.Utils

/**
 * Created by nickl on 26.01.15.
 */
class ProxiedHandler(another: SocketAddress, workerGroup:EventLoopGroup ) extends ChannelDuplexHandler with grizzled.slf4j.Logging {


  private var serverCxt: ChannelHandlerContext = null

  private var clientConnectionFuture: ChannelFuture = null

  private def clientConnection = {
     if(clientConnectionFuture == null)
       clientConnect
    clientConnectionFuture
  }

  override def channelRegistered(ctx: ChannelHandlerContext): Unit = {
    this.serverCxt = ctx
    super.channelRegistered(ctx)
  }

  override def connect(ctx: ChannelHandlerContext, remoteAddress: SocketAddress, localAddress: SocketAddress, future: ChannelPromise): Unit = {
    debug("connect")
    super.connect(ctx, remoteAddress, localAddress, future)

  }

  private def clientConnect {
    val b = new Bootstrap(); // (1)
    b.group(workerGroup); // (2)
    b.channel(classOf[NioSocketChannel])
    b.option(ChannelOption.SO_KEEPALIVE, true.asInstanceOf[java.lang.Boolean]); // (4)
//    b.option(ChannelOption.TCP_NODELAY, true.asInstanceOf[java.lang.Boolean])
    b.handler(new ChannelInitializer[SocketChannel]() {
      override def initChannel(ch: SocketChannel): Unit = {
           ch.pipeline().addLast(
             new LoggingHandler(classOf[ProxiedHandler]),
             new ClientHandler
           )
      }
    })

    // Start the client.
    clientConnectionFuture = b.connect(another)
  }

  class ClientHandler extends ChannelInboundHandlerAdapter {

    override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
      msg match {
        case bbuff: ByteBuf => {
          //debug("client received:"+Utils.toHexString(bbuff," "))
          ProxiedHandler.this.serverCxt.writeAndFlush(bbuff)
        }
        case _ => ReferenceCountUtil.release(msg)
      }
      //super.channelRead(ctx, msg)
    }

    override def channelInactive(ctx: ChannelHandlerContext): Unit = {
      debug("channelInactive")
      serverCxt.close()
      super.channelInactive(ctx)
    }

  }


  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    clientConnection.channel().close()
    super.channelInactive(ctx)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    try {
      msg match {
        case bbuff: ByteBuf =>  clientConnection.addListener(new GenericFutureListener[Future[Void]] {
          override def operationComplete(future: Future[Void]): Unit = clientConnection.channel().writeAndFlush(bbuff)
        })
        case _ => ReferenceCountUtil.release(msg)
      }
    }
    finally {

    }
    //super.channelRead(ctx, msg)
  }



}
