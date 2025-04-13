package ru.sosgps.wayrecall.packreceiver.netty

import java.net.InetSocketAddress
import java.nio.ByteOrder

import io.netty.buffer.ByteBuf
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.{SimpleChannelInboundHandler, ChannelHandlerContext, ChannelPipeline}
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.logging.LoggingHandler
import ru.sosgps.wayrecall.avlprotocols.egts.{TermIdentity, AppDataFrame, TransportPackage}
import ru.sosgps.wayrecall.packreceiver.Netty4Server

/**
 * Created by nickl on 26.01.15.
 */
class EgtsProxyServer extends Netty4Server{
  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[EgtsProxyServer]),
      new EGTSFrameDecoder,
      new IMEIRecoder,
      //new ProxiedHandler(new InetSocketAddress("91.230.215.12", 7001), workerPool)
      new ProxiedHandler(new InetSocketAddress("77.243.111.83", 20629), workerPool)


    )
  }
}

class EGTSFrameDecoder extends LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, 65536, 5, 2, -5, 0, true) with grizzled.slf4j.Logging{
  override def getUnadjustedFrameLength(buf: ByteBuf, offset: Int, length: Int, order: ByteOrder): Long = {
   val headerLength = super.getUnadjustedFrameLength(buf, 3, 1, order)
   debug(s"headerLength=$headerLength")
   val frameLength = super.getUnadjustedFrameLength(buf, offset, length, order)
   debug(s"frameLength=$frameLength")
   headerLength + frameLength
  }

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf): AnyRef = {
    val src = super.decode(ctx, in).asInstanceOf[ByteBuf]
    if(src != null)
    new TransportPackage().read(src)
    else
      null
  }

  protected override def extractFrame(ctx: ChannelHandlerContext, buffer: ByteBuf, index: Int, length: Int): ByteBuf = {
    return buffer.slice(index, length)
  }

}

class IMEIRecoder extends SimpleChannelInboundHandler[TransportPackage]  {
  override def channelRead0(ctx: ChannelHandlerContext, msg: TransportPackage): Unit = {
    msg.frame match {
      case appdata: AppDataFrame =>
        appdata.subrecords.collect({
          case id: TermIdentity => id.imei = Some("121212121212121")
        })
      case _ =>
    }

    ctx.fireChannelRead(msg.write(ctx.alloc().buffer()))
  }


}

object EgtsProxyServer{

  def main(args: Array[String]) {
    val server = new EgtsProxyServer
    //server.timer = new HashedWheelTimer()
    //server.store = new DummyPackSaver
    //server.commands = commands
    server.bossPool = new NioEventLoopGroup()
    server.workerPool = server.bossPool

    server.setPort(7001)
    server.start()
  }

}

