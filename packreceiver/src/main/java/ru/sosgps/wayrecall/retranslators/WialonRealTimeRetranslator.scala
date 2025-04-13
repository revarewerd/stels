package ru.sosgps.wayrecall.retranslators

import java.net.InetSocketAddress
import java.util.concurrent.{Executors, TimeUnit}

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.{NioWorkerPool, NioClientSocketChannelFactory}
import org.jboss.netty.handler.codec.frame.FixedLengthFrameDecoder
import org.jboss.netty.handler.timeout.ReadTimeoutHandler
import org.jboss.netty.util.{ThreadNameDeterminer, HashedWheelTimer}
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions}
import ru.sosgps.wayrecall.data.{DummyPackDataConverter, PackDataConverter}
import ru.sosgps.wayrecall.initialization.{MultiDbManager, MultiserverConfig}
import ru.sosgps.wayrecall.packreceiver.PackProcessor
import ru.sosgps.wayrecall.utils.errors.NotMoreThanIn
import ru.sosgps.wayrecall.wialonparser.{WialonPackage, WialonPackager}

import scala.beans.BeanProperty
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, promise}
import scala.util.{Failure, Success}


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 08.10.13
 * Time: 16:24
 * To change this template use File | Settings | File Templates.
 */
class WialonRealTimeRetranslator extends (GPSData => Unit) with grizzled.slf4j.Logging with ConfigurableRetranslator {

  //  def this(jlist: java.util.List[InetSocketAddress]) = {
  //    this()
  //    this.configure(scala.collection.JavaConversions.asScalaBuffer(jlist).map(addr => (addr, (_: GPSData) => true)))
  //  }

  def this(configurer: RetranslationConfigurator) = {
    this()
    configurer.configure(this);
  }

  @Autowired
  var timer: HashedWheelTimer = null //new HashedWheelTimer

  @Autowired
  @BeanProperty
  var packConv: PackDataConverter = new DummyPackDataConverter

  //TODO: возможно стоит сделать общий bootstrap
  protected[this] lazy val bootstrap = {
    val bootstrap = new ClientBootstrap(
      new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(), 1,
        new NioWorkerPool(Executors.newCachedThreadPool(), 1, new ThreadNameDeterminer {
          override def determineThreadName(currentThreadName: String, proposedThreadName: String): String = proposedThreadName + " wialonClient"
        })));

    // Set up the pipeline factory.
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      def getPipeline: ChannelPipeline = Channels.pipeline(
        new ReadTimeoutHandler(timer, 30, TimeUnit.MINUTES),
        new FixedLengthFrameDecoder(1),
        new WialonNettyClientHandler
      )
    });
    bootstrap
  }

  @volatile
  private[this] var allResend: Seq[RealTimeRetranslationTask] = Seq.empty

  override def configure(rules: Seq[(InetSocketAddress, (GPSData) => Boolean)]) {
    info("setting retranslation to " + rules)
    allResend = rules.map {
      case (sockaddr, accepts) => new RealTimeRetranslationTask(sockaddr, accepts)
    }
  }

  class RealTimeRetranslationTask(val address: InetSocketAddress, accepts: (GPSData) => Boolean) extends (GPSData => Unit) {

    val notEveryTime = new NotMoreThanIn(5.minutes)

    def apply(gps: GPSData) {
      if (accepts(gps)) {
        trace("resending " + gps + " to " + address)
        val converted = packConv.convertData(gps)
        val wln: WialonPackage = GPSDataConversions.toWialonPackage(converted)
        val binary = WialonPackager.wlnPackToBinary(wln)

        getChannel().onComplete {
          case Success(activeChannel) => activeChannel.write(ChannelBuffers.wrappedBuffer(binary))
          case Failure(e) => notEveryTime {error("retranslator connection error " + address, e)}
        }
      }
    }

    @volatile
    private[this] var channelFuture: Future[Channel] = Future.failed(new IllegalStateException("not connected yet"))

    private[this] def connect(): Future[ChannelFuture] = {
      val channelFuture = bootstrap.connect(address)

      val p = promise[ChannelFuture]()

      channelFuture.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          //debug("operationComplete " + future)
          p.success(future)
        }
      })
      p.future
    }

    private[this] def alreadyFailed(channelFuture: Future[Channel]): Boolean = {
      channelFuture.value.exists(_.isFailure) || channelFuture.value.flatMap(_.toOption).exists(!_.isOpen)
    }

    private[this] def getChannel(): Future[Channel] = {
      if (!alreadyFailed(channelFuture))
        channelFuture
      else synchronized {
        if (!alreadyFailed(channelFuture))
          channelFuture
        else {
          channelFuture = connect().map(channelFuture => {
            if (channelFuture.isSuccess) {
              channelFuture.getChannel
            }
            else
              throw new IllegalStateException("unsuccesseful connection", channelFuture.getCause)
          })
          channelFuture
        }
      }
    }


  }


  def apply(gps: GPSData): Unit = {
    allResend.foreach(_(gps))
  }


}

class WialonNettyClientHandler extends SimpleChannelUpstreamHandler with grizzled.slf4j.Logging {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) = {
    val buffer = e.getMessage.asInstanceOf[ChannelBuffer]
    val byte = buffer.getByte(0)
    trace("WialonNettyClientHandler byte=" + byte + " " + ctx.getChannel.getRemoteAddress)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) = {
    e.getCause match {
      case e: java.net.ConnectException =>
      case e: Exception => debug("connecion closed by exception " + e + " addr:" + ctx.getChannel.getRemoteAddress)
    }
    ctx.getChannel.close()
  }
}







