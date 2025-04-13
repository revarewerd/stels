package ru.sosgps.wayrecall.packreceiver

import io.netty.channel.nio.NioEventLoopGroup
import org.scalatest._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.scalatestutils.ConnectedFixture
import ru.sosgps.wayrecall.utils.stubs.CollectionPacketWriter

import scala.collection.mutable.ListBuffer

trait Netty4ReceiverServerFixture extends ReceiverServerFixture[Netty4Server] {
  this: Suite =>
  protected def useServer: Netty4Server

  setupServer {
    server.bossPool = new NioEventLoopGroup()
    server.workerPool = server.bossPool
  }
}

trait Netty3ReceiverServerFixture[T <: Netty3Server] extends ReceiverServerFixture[T] {
  this: Suite =>
  protected def useServer: T

  override protected def setupServer(): Unit = {
    server.timer = new org.jboss.netty.util.HashedWheelTimer()
    server.bossPool = new NioEventLoopGroup()
    server.workerPool = server.bossPool
  }
}

trait ReceiverServerFixture[T <: ProtocolServer] extends BeforeAndAfterAll with ConnectedFixture {
  this: Suite =>

  protected def useServer: T

  var server: T = _

  val packWriter = new CollectionPacketWriter(new ListBuffer[GPSData])

  def stored = packWriter.collection

  private val serverStups = ListBuffer.empty[() => Any]

  protected final def setupServer(f: => Any): Unit = serverStups.+=(() => f)

  protected def setupServer(): Unit = {}

  override protected def beforeAll(): Unit = {
    server = useServer
    //server.timer = new HashedWheelTimer()
    server.setStore(new TrimmedPackProcessor(packWriter, true))
    //server.commands = commands
    setupServer()
    serverStups.foreach(_ ())
    server.setPort(serverPort)
    server.start()
    super.beforeAll()
  }

  override def afterEach() {
    try {
      super.afterEach() // To be stackable, must call super.afterEach
    }
    finally {
      packWriter.collection.clear()
    }
  }

  override protected def afterAll(): Unit = {
    server.stop()
    super.afterAll()
  }

}
