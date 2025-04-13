package ru.sosgps.wayrecall.utils.io.asyncs

import java.io.{DataInputStream, DataOutputStream}
import java.net.{InetSocketAddress, Socket}
import java.util.concurrent.TimeUnit

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.{NioEventLoop, NioEventLoopGroup}
import io.netty.channel.{ChannelInitializer, ChannelOption, ChannelPipeline}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.timeout.ReadTimeoutHandler
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers._

import scala.concurrent.Future

/**
  * Created by nickl-mac on 14.08.16.
  */
class NettyFutureHandlerTest extends FunSpec with BeforeAndAfterAll {

  val serverPort = ru.sosgps.wayrecall.testutils.getFreePort
  val bootstrap = new ServerBootstrap
  bootstrap.group(new NioEventLoopGroup()).channel(classOf[NioServerSocketChannel]).childHandler(new ChannelInitializer[SocketChannel]() {
    @throws[Exception]
    def initChannel(ch: SocketChannel) {
      val pipeline = ch.pipeline
      pipeline.addLast(new ReadTimeoutHandler(300, TimeUnit.SECONDS))
      pipeline.addLast(NettyFutureHandler.cycled(handler))
    }
  })

  val serverChannel = bootstrap.bind(new InetSocketAddress(serverPort)).sync.channel


  def handler(conn: Connection): Future[Any] = {

    import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

    for (
      i <- conn.in.readInt;
      r <- conn.out.writeInt(i) ;
      i <- conn.in.readLong;
      r <- conn.out.writeLong(i)
    ) yield r
  }


  describe("NettyFutureHandler many reads") {

    val socket = new Socket("localhost", serverPort)
    val in = new DataInputStream(socket.getInputStream)
    val out = new DataOutputStream(socket.getOutputStream)

    it("should receive and answerInt"){
      out.write(0x00)
      out.write(0x00)
      out.flush()
      in.available() shouldBe 0
      out.write(0x12)
      out.write(0xA3)
      out.flush()
      in.readInt() shouldBe 0x12A3
    }

    it("then should receive and answerLong"){
      out.write(0x00)
      out.write(0x00)
      out.write(0x00)
      out.write(0x00)
      out.flush()
      in.available() shouldBe 0
      out.write(0x12)
      out.write(0xA3)
      out.write(0xA3)
      out.write(0xA3)
      out.flush()
      in.readLong() shouldBe 0x12A3A3A3
    }

    it("and repeat it again"){
      out.write(0x00)
      out.write(0x00)
      out.flush()
      in.available() shouldBe 0
      out.write(0x12)
      out.write(0xA3)
      out.flush()
      in.readInt() shouldBe 0x12A3

      out.write(0x00)
      out.write(0x00)
      out.write(0x00)
      out.write(0x00)
      out.flush()
      in.available() shouldBe 0
      out.write(0x12)
      out.write(0xA3)
      out.write(0xA3)
      out.write(0xA3)
      out.flush()
      in.readLong() shouldBe 0x12A3A3A3
    }

  }

  describe("NettyFutureHandler buffered read") {
    it("should process data received in batch") {

      val socket = new Socket("localhost", serverPort)
      val in = new DataInputStream(socket.getInputStream)
      val out = new DataOutputStream(socket.getOutputStream)

      out.writeInt(12)
      out.writeLong(67)
      out.writeInt(120)
      out.writeLong(99)
      out.flush()

      in.readInt() shouldBe 12
      in.readLong() shouldBe 67
      in.readInt() shouldBe 120
      in.readLong() shouldBe 99


    }
  }

  override protected def afterAll(): Unit = {
    serverChannel.close()
    super.afterAll()
  }
}
