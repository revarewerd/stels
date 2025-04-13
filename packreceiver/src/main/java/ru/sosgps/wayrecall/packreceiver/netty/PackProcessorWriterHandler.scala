package ru.sosgps.wayrecall.packreceiver.netty

import com.google.common.util.concurrent.MoreExecutors
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import org.jboss.netty.buffer.{ChannelBuffers => N3ChannelBuffers}
import org.jboss.netty.channel.{ChannelHandlerContext => N3ChannelHandlerContext}
import ru.sosgps.wayrecall.avlprotocols.ruptela.ProtocolExceptionWithResponse
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.IllegalImeiException

import scala.concurrent.ExecutionContext
import scala.util.{Try, Failure, Success}


trait PackProcessorWriterHandler extends grizzled.slf4j.Logging {

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

//  def confirmOrClose(ctx: ChannelHandlerContext, confirm: => Any): Try[GPSData] => Unit = confirmOrClose(ctx, (gps: GPSData) => confirm)
//
//  def confirmOrClose(ctx: ChannelHandlerContext, confirm: (GPSData) => Any): Try[GPSData] => Unit = {
//    case Success(gps) => {
//      confirm(gps)
//    }
//    case Failure(exc) => stdExceptionHandler(ctx)
//  }

  def stdExceptionHandler(ctx: ChannelHandlerContext): PartialFunction[Throwable, Unit] = {
    case e: IllegalImeiException => {
      warn(e.toString)
      ctx.close()
    }
    case e: ProtocolExceptionWithResponse => {
      warn(e.toString)
      //debug("ProtocolExceptionWithResponse response for " + incomingPackage.imei + " = " + Utils.toHexString(e.respData, ""))
      ctx.channel().writeAndFlush(Unpooled.wrappedBuffer(e.respData))
      ctx.channel().close()
    }
    case e: Throwable => {
      error(e.toString, e)
      ctx.channel().close()
    }
  }


}

trait PackProcessorN3WriterHandler extends grizzled.slf4j.Logging {

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  def confirmOrClose(ctx: N3ChannelHandlerContext, confirm: => Any): Try[GPSData] => Unit = confirmOrClose(ctx, (gps: GPSData) => confirm)

  def confirmOrClose(ctx: N3ChannelHandlerContext, confirm: (GPSData) => Any): Try[GPSData] => Unit = {
    case Success(gps) => {
      confirm(gps)
    }
    case Failure(exc) => stdExceptionHandler(ctx)
  }

  def stdExceptionHandler(ctx: N3ChannelHandlerContext): PartialFunction[Throwable, Unit] = {
    case e: IllegalImeiException => {
      warn(e.toString)
      ctx.getChannel.close()
    }
    case e: ProtocolExceptionWithResponse => {
      warn(e.toString)
      //debug("ProtocolExceptionWithResponse response for " + incomingPackage.imei + " = " + Utils.toHexString(e.respData, ""))
      ctx.getChannel.write(N3ChannelBuffers.wrappedBuffer(e.respData))
      ctx.getChannel.close()
    }
    case e: Throwable => {
      error(e.toString, e)
      ctx.getChannel.close()
    }
  }


}
