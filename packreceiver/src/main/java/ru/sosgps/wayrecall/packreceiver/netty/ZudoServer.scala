package ru.sosgps.wayrecall.packreceiver.netty


import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, ChannelPipeline, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
import io.netty.handler.logging.LoggingHandler
import io.netty.util.CharsetUtil
import ru.sosgps.wayrecall.avlprotocols.zudo.ZudoParser
import ru.sosgps.wayrecall.packreceiver.{Netty4Server, PackProcessor}
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.io.asyncs.NettyFutureHandler
import ru.sosgps.wayrecall.avlprotocols.zudo.multipart.{Attribute, ZudoPostRequestDecoder}

import scala.collection.JavaConversions.iterableAsScalaIterable
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by nickl-mac on 01.10.16.
  */
class ZudoServer extends Netty4Server with grizzled.slf4j.Logging {

  import ZudoServer.HttpContextOps

  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[ZudoServer])
    )
    pipeline.addLast("httpCodec", new HttpServerCodec())
    pipeline.addLast("httpAggregator", new HttpObjectAggregator(64 * 1024))
    pipeline.addLast("businessHandler", new HttpServerHandler(store))
  }


  class HttpServerHandler(store: PackProcessor) extends SimpleChannelInboundHandler[FullHttpRequest] {
    override def channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit = {

      debug(s"msg: ${request.getUri} ${request.content().toString(CharsetUtil.UTF_8)}")

      val postDecoder = new ZudoPostRequestDecoder(request)

      debug("postDecoder:" + postDecoder.getBodyHttpDatas.collect(
        { case i: Attribute => i.getName + " -> " + i.content().toHexString }
      ).mkString("[", ", ", "]"))

      val imei = postDecoder.getBodyHttpData("id").asInstanceOf[Attribute].getValue

      val p = ZudoParser.parse(postDecoder.getBodyHttpData("bin").asInstanceOf[Attribute].content())

      Future.sequence(p.gpss.map(gps => {
        gps.imei = imei
        store.addGpsDataAsync(gps)
      })).onComplete {
        case Success(gps) =>
          ctx.sendHttpResponse(HttpResponseStatus.OK, request, ctx.alloc().buffer(8)
            .writeBytes(Unpooled.copiedBuffer("#ACK#", CharsetUtil.UTF_8))
            .writeByte(p.ID_PAC)
            .writeByte(p.PAC_CNT)
            .writeByte(0x00))
        case Failure(e) =>
          error("error saving gps", e)
          ctx.sendHttpResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, request, Unpooled.EMPTY_BUFFER)
          ctx.close()
      }



    }
  }


}

object ZudoServer {

  implicit class HttpContextOps(val ctx: ChannelHandlerContext) extends AnyVal {


    def sendHttpResponse(status: HttpResponseStatus, request: FullHttpRequest, responseContent: ByteBuf): Unit = {
      val responseHeaders = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)


      val keepAlive = HttpHeaders.isKeepAlive(request)
      if (keepAlive) {
        responseHeaders.headers().set(HttpHeaders.Names.CONTENT_LENGTH, responseContent.readableBytes())
        responseHeaders.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
      }

      ctx.write(responseHeaders)
      ctx.write(responseContent)

      val future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)

      // Decide whether to close the connection or not.
      if (!keepAlive) {
        future.addListener(ChannelFutureListener.CLOSE)
      }
    }
  }

}

