package ru.sosgps.wayrecall.tools.wialon.packaging

import java.net.{SocketAddress, InetSocketAddress, InetAddress}
import ru.sosgps.wayrecall.wialonparser.{WialonPackager, WialonPackage}
import org.springframework.beans.factory.annotation.Autowired
import org.jboss.netty.util.{Timeout, TimerTask, HashedWheelTimer}
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.{TimeUnit, Executors}
import org.jboss.netty.channel._
import org.jboss.netty.handler.timeout.ReadTimeoutHandler
import org.jboss.netty.handler.codec.frame.FixedLengthFrameDecoder
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import scala.concurrent._
import org.jboss.netty.channel.Channel
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.immutable.IndexedSeq
import scala.util.{Failure, Success}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by nickl on 15.03.14.
 */
class WlnNettyResender(
                        address: InetAddress,
                        port: Int,
                        packetsStream: Iterator[WialonPackage],
                        soTimeout: Int = 10000,
                        val maxReconnectionsCount: Int = 50000,
                        val reconnectionInterval: Int = 10000,
                        sendingLogEnabled: Boolean = false,
                        sleepBeetween: Int = 0,
                        useNowTime: Boolean = false
                        ) extends WlnResender(address,
  port, packetsStream, soTimeout, maxReconnectionsCount, reconnectionInterval, sendingLogEnabled, sleepBeetween, useNowTime)
with NettyReconnectable with grizzled.slf4j.Logging {


  @Autowired
  var timer: HashedWheelTimer = new HashedWheelTimer

  protected[this] val socketAddess = new InetSocketAddress(address, port)

  protected[this] val bootstrap: ClientBootstrap = new ClientBootstrap(
    new NioClientSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool(), 1, 1)
  );

  private val confirmationWaiter = new ConfirmationWaiter

  bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
    def getPipeline: ChannelPipeline = Channels.pipeline(
      new ReadTimeoutHandler(timer, soTimeout, TimeUnit.MILLISECONDS),
      new FixedLengthFrameDecoder(1),
      confirmationWaiter
    )
  });


  val blockSize = 100


  override def doSending: Unit = {

    val genPromise = promise[Unit]()

    def generalSend() {
      packsBlock = packetsStream.take(blockSize).toList
      blockSend().onComplete {
        case Success(_) =>
          trace("blockSend success:" + packsBlock.size + " " + packsBlock.head.time + " " + packsBlock.last.time)
          if (packetsStream.hasNext) {
            generalSend()
          } else {
            trace("genPromise completed")
            genPromise.success()
          }
        case Failure(t) =>
          warn("blocksend failed:", t)
          sendByOne() onComplete {
            case Success(_) =>
              if (packetsStream.hasNext) {
                generalSend()
              } else
                genPromise.success()
            case Failure(e) =>
              warn("sendByOne failed:", e)
              genPromise.failure(e)
          }
      }

    }

    val genFuture = genPromise.future
    genFuture.onComplete {
      case _ =>
        getChannel().map(_.close()).onComplete {case _ => bootstrap.shutdown()}
    }

    generalSend()

    Await.ready(genFuture, Duration.Inf)
    info("doSending completed")

  }


  private var packsBlock: List[WialonPackage] = List.empty

  private[this] def blockSend(): Future[Unit] = {
    getChannel().flatMap(c => {
      val confirmed = confirmationWaiter.confirmationFuture(packsBlock.size)

      packsBlock.foreach(wp =>
        c.write(ChannelBuffers.wrappedBuffer(WialonPackager.wlnPackToBinary(wp))
        ))

      confirmed
    }
    )
  }

  private[this] def sendByOne(): Future[Unit] = {
    def send(packs: List[WialonPackage]): Future[Unit] = {
      packs match {
        case Nil => Future.successful()
        case head :: tail =>
          getChannel().flatMap(c => {
            val confirmed = confirmationWaiter.confirmationFuture(1)
            c.write(ChannelBuffers.wrappedBuffer(WialonPackager.wlnPackToBinary(head)))
            confirmed
          }
          ).map(_ => send(tail))
      }
    }

    send(packsBlock)
  }

  class ConfirmationWaiter extends SimpleChannelUpstreamHandler with grizzled.slf4j.Logging {

    private val confirmedPacks = new AtomicInteger(0)

    @volatile
    private var conprom: Promise[Unit] = promise().failure(new IllegalStateException("dummy promise"))

    @volatile
    private var num: Int = 0

    def confirmationFuture(num: Int): Future[Unit] = synchronized {
      require(conprom.isCompleted, "concurent confirmation awaiting are not allowed")
      val p = promise[Unit]()
      trace("confirmation waiting for " + num + " confirmations " + System.identityHashCode(p))
      this.num = num
      confirmedPacks.set(0)



      val to = timer.newTimeout(new TimerTask {
        def run(timeout: Timeout): Unit = {
          debug("confirmation waiting timed out " + System.identityHashCode(p))
          p.failure(new TimeoutException("confirmation waiting timed out"))
        }
      }, 5, TimeUnit.SECONDS)

      val future = p.future
      conprom = p

      future.onComplete {
        case _ => {
          trace("confirmation timedout cancelled " + System.identityHashCode(p))
          to.cancel()
        }
      }

      future
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) = {
      val buffer = e.getMessage.asInstanceOf[ChannelBuffer]
      val byte = buffer.getByte(0)
      val cp = confirmedPacks.incrementAndGet()

      if (cp >= num) {
        conprom.success()
        trace("confirmationFuture success:" + cp + " >=" + num + " " + System.identityHashCode(conprom))
      }

      trace("cp = " + cp + " num = " + num)
      trace("WialonNettyClientHandler byte=" + byte + " " + ctx.getChannel.getRemoteAddress)
    }

    override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) = {
      debug("connecion closed by exception " + e.getCause + " addr:" + ctx.getChannel.getRemoteAddress)
      ctx.getChannel.close()
    }
  }

}

trait NettyReconnectable extends grizzled.slf4j.Logging {

  val connectionAttempts = new AtomicInteger(0)

  protected[this] def bootstrap: ClientBootstrap

  protected[this] def socketAddess: SocketAddress

  protected[this] def maxReconnectionsCount: Int

  protected[this] def reconnectionInterval: Int

  protected[this] def timer: HashedWheelTimer

  @volatile
  private[this] var channelFuture: Future[Channel] = Future.failed(new IllegalStateException("not connected yet"))

  private[this] def connect(): Future[ChannelFuture] = {
    val channelFuture = bootstrap.connect(socketAddess)

    val p = promise[ChannelFuture]()

    channelFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        debug("operationComplete " + future)
        p.success(future)
      }
    })
    p.future
  }

  private[this] def alreadyFailed(channelFuture: Future[Channel]): Boolean = {
    channelFuture.value.exists(_.isFailure) || channelFuture.value.flatMap(_.toOption).exists(!_.isOpen)
  }

  protected[this] def getChannel(): Future[Channel] = {
    if (!alreadyFailed(channelFuture))
      channelFuture
    else synchronized {
      if (!alreadyFailed(channelFuture))
        channelFuture
      else {
        val connAtt = connectionAttempts.getAndIncrement
        debug("trying to connect " + connAtt + " of " + maxReconnectionsCount)
        if (connAtt >= maxReconnectionsCount) {
          channelFuture = Future.failed(new IllegalStateException("number of atempts enhausted:" + connAtt))
          return channelFuture
        }

        channelFuture = connect().map(channelFuture => {
          if (channelFuture.isSuccess) {
            channelFuture.getChannel
          }
          else {
            warn("cant connect:", channelFuture.getCause)
            throw new IllegalStateException("unsuccesseful connection", channelFuture.getCause)
          }
        }).recoverWith({
          case _ => {
            reconnect()
          }
        })
        channelFuture
      }
    }
  }


  protected[this] def reconnect(): Future[Channel] = {
    val p = promise[Channel]()
    debug("schedule reconnection in " + reconnectionInterval + " " + TimeUnit.MILLISECONDS)
    timer.newTimeout(new TimerTask {
      def run(t: Timeout) = try {
        debug("reconnecting")
        channelFuture = Future.failed(new IllegalStateException("not connected yet"))
        p.completeWith(getChannel())
      } catch {
        case e: Throwable => p.failure(e)
      }
    }, reconnectionInterval, TimeUnit.MILLISECONDS)
    p.future
  }


}

