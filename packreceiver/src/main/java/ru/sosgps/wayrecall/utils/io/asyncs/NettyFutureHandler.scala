package ru.sosgps.wayrecall.utils.io.asyncs

import java.util
import java.util.concurrent.ConcurrentLinkedQueue

import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.netty.channel.{ChannelFuture, ChannelFutureListener, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.ByteToMessageDecoder
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.ScalaNettyNetworkUtils._
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * Created by nickl-mac on 14.08.16.
  */
abstract class NettyFutureHandler extends ByteToMessageDecoder with grizzled.slf4j.Logging {

  protected def connectionHandler(connection: Connection): Unit

  protected val readRequestQueue: util.Queue[(Int, Promise[ByteBuf])] = new util.LinkedList[(Int, Promise[ByteBuf])]()

  protected var ctx: ChannelHandlerContext = null

  val connection = new Connection {

    override def allocator: ByteBufAllocator = ctx.alloc()

    val in = new ByteBufAsyncIn {
      override def readTransformAndRelease[T](size: Int, t: (ByteBuf) => T): Future[T] = {

        //debug(s"readByteBufSlice $size $actualReadableBytes()")
        //val requestTrace = new Exception(s"stacktrace $size")

        if (ctx.executor().inEventLoop() && actualReadableBytes() >= size) {
          Future.successful(t(internalBuffer().readSlice(size)))
        } else {
          val bbPromise = Promise[ByteBuf]
          ctx.inReadThread {
            readRequestQueue.add((size, bbPromise))
            tryCompleteTheQueue(internalBuffer())
          }
          bbPromise.future.map(bb => {
            //debug(s"reading ${bb.toHexString} for", requestTrace)
            t(bb)
          })(ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext)
        }
      }
    }
    val out = new ByteBufAsyncOut {
      override def alloc(size: Int): ByteBuf = ctx.alloc().buffer(size)

      override def write(bb: ByteBuf): Future[Unit] = ctx.writeAndFlush(bb)
    }

    def close(): Unit = ctx.close()
  }


  override def channelRegistered(ctx: ChannelHandlerContext): Unit = {
    this.ctx = ctx
    super.channelRegistered(ctx)
    connectionHandler(connection)
  }


  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    if (readRequestQueue.isEmpty) {
      if(this.actualReadableBytes() > 10 * 1024 * 1024){
        throw new IllegalStateException(s"internal buffer overflow: ${actualReadableBytes()} seemes that no one reads us")
      }
      return
    }

    tryCompleteTheQueue(in)
  }

  private def tryCompleteTheQueue(in: ByteBuf): Unit = try {
    val peek = readRequestQueue.peek()
    if (peek == null)
      return
    val requredToRead = peek._1
    if (requredToRead <= in.readableBytes()) {
      //val bytes = in.readBytes(requredToRead)
      val bytes = in.readSlice(requredToRead)
      readRequestQueue.poll()._2.success(bytes)
      tryCompleteTheQueue(in)
    }
  } catch {
    case e: Exception => error("tryCompleteTheQueue,", e); throw e
  }
}


object NettyFutureHandler {

  def cycled(handler: (Connection) => Future[Any]) = new NettyFutureHandler {
    override protected def connectionHandler(conn: Connection): Unit = {

      import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

      def recHandler(): Unit = handler(conn).onComplete {
        case Success(_) => recHandler()
        case Failure(e) =>
          //e.printStackTrace()
          //grizzled.slf4j.Logger(classOf[NettyFutureHandler]).error("error in rechandler", e)
          ctx.fireExceptionCaught(e)
          recHandler()
          //conn.close()
          //throw new RuntimeException("CycledFuture failure", e)
      }

      recHandler()
    }
  }

  def apply(handler: (Connection) => Future[Any]) = new NettyFutureHandler {
    override protected def connectionHandler(conn: Connection): Unit = {

      import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

      def recHandler(): Unit = handler(conn).onComplete {
        case Success(_) => recHandler()
        case Failure(e) =>
          //e.printStackTrace()
          //grizzled.slf4j.Logger(classOf[NettyFutureHandler]).error("error in rechandler", e)
          ctx.fireExceptionCaught(e)
      }

      recHandler()
    }
  }
}





