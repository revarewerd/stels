package ru.sosgps.wayrecall.utils


import _root_.io.netty.channel.{ChannelFuture, ChannelFutureListener, ChannelHandlerContext}
import ru.sosgps.wayrecall.utils

import scala.concurrent.{Future, Promise}

/**
  * Created by nickl-mac on 14.08.16.
  */
object ScalaNettyNetworkUtils extends grizzled.slf4j.Logging {

  implicit def channelFutureAsFuture(channelFuture: ChannelFuture): Future[Unit] = {
    val p = Promise[Unit]
    channelFuture.addListener(new ChannelFutureListener {
      override def operationComplete(future: ChannelFuture): Unit = {
        if (future.isSuccess)
          p.success()
        else
          p.failure(future.cause())
      }
    })
    p.future
  }

  implicit class channelHandlerContextOps(val ctx: ChannelHandlerContext) extends AnyVal {

    def inReadThread(f: => Any): Unit = try {
      val executor = ctx.executor()
      if (executor.inEventLoop())
        f
      else
        executor.execute(utils.runnable {
          try
          f
          catch {
            case e: Exception => error("inReadThread executor.execute", e)
          }
        })
    } catch {
      case e: Exception => error("inReadThread", e)
    }

  }

}

