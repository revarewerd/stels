package ru.sosgps.wayrecall.packreceiver.netty

import java.nio.ByteOrder
import java.util

import io.netty.buffer.{Unpooled, ByteBuf}
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler, ChannelPipeline}
import io.netty.handler.codec.ReplayingDecoder
import io.netty.handler.logging.LoggingHandler
import ru.sosgps.wayrecall.avlprotocols.autophonemayak.AutophoneMayak
import ru.sosgps.wayrecall.avlprotocols.common._
import ru.sosgps.wayrecall.avlprotocols.dtm.DTM
import ru.sosgps.wayrecall.avlprotocols.navtelecom.NavtelecomCommand
import ru.sosgps.wayrecall.avlprotocols.navtelecom.NavtelecomParser._
import ru.sosgps.wayrecall.avlprotocols.skysim.{Skysim, Header}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.{PackProcessor, Netty4Server}
import ru.sosgps.wayrecall.sms.{IOSwitchDeviceCommand, DeviceCommand}
import ru.sosgps.wayrecall.utils.concurrent.FuturesUtils
import ru.sosgps.wayrecall.utils.concurrent.FuturesUtils.runSafe
import ru.sosgps.wayrecall.utils.errors
import ru.sosgps.wayrecall.utils.io.Utils
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

import scala.beans.BeanProperty
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by nmitropo on 10.1.2016.
  */
class DTMServer extends Netty4Server with DeviceCommander {


  val activeConnections = new ConnectedDevicesWatcher[DTMAvlDataDecoder](_.imei)

  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    val decoder = new DTMAvlDataDecoder(store)
    pipeline.addLast(
      new LoggingHandler(classOf[DTMServer]),
      activeConnections.connectionWatcherFor(decoder),
      decoder
    )
  }

  override def connectedTo(imei: String): Boolean = activeConnections.devices.isDefinedAt(imei)

  override def receiveCommand(d: DeviceCommand): Future[DeviceCommand] = runSafe {

    val storedCommand = new ListenableStoredCommand(new DTMStoredCommand(d.asInstanceOf[IOSwitchDeviceCommand]))
    activeConnections.apply(d.target).sendCommands(
      storedCommand
    )

    storedCommand.flatMap{
      case true => Future.successful(d)
      case false => Future.failed(new Exception("returned false"))
    }


  }
}

case class DTMStoredCommand(command: Byte, value: Byte) extends StoredDeviceCommand {

  def this(iostch: IOSwitchDeviceCommand) = this((iostch.num.getOrElse(0) + 0x09).toByte, iostch.state match {
    case true => 0x01.toByte
    case false => 0x00.toByte
  })

  override def accept(binary: Array[Byte]): Unit = {}

  override def text: String = ""

  override def answerAccepted: Boolean = true

  override def accept(gpsData: GPSData): Unit = {}
}

class DTMAvlDataDecoder(store: PackProcessor) extends ReplayingDecoder[Unit]
  with grizzled.slf4j.Logging
  with PackProcessorWriterHandler
  with StoredDeviceCommandsQueue[StoredDeviceCommand]
{

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  private var header: Option[String] = None

  def imei = header

  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {

    header match {
      case None => {
        val parsedHeader = DTM.parseHeader(in)
        header = Some(parsedHeader)

        ctx.writeAndFlush(DTM.writeResponse1(ctx.alloc().buffer(4), 0))

      }
      case Some(header) => {
        val (num, gpsData) = DTM.readPackage(in)

        gpsData.foreach(_.imei = header)
        notifyCommands(gpsData)

        Future.sequence(gpsData.map(store.addGpsDataAsync)).onComplete {
          case Success(ss) =>
            debug("stored " + ss)
            ctx.writeAndFlush(DTM.writeResponse1(ctx.alloc().buffer(4), num))
          case Failure(e) =>
            debug("rejected " + gpsData)
            ctx.close()
        }

      }
    }
  }

  var ctx: ChannelHandlerContext = null

  override def channelRegistered(ctx: ChannelHandlerContext): Unit = {
    super.channelRegistered(ctx)
    this.ctx = ctx
  }

  def command(comm: Byte, value: Byte): Unit = {
    debug(s"sending command $comm $value")
    ctx.writeAndFlush(makeCommand(comm, value))
  }

  def makeCommand(comm: Byte, value: Byte): ByteBuf = {
    DTM.writeCommand(
      ctx.alloc().buffer(8),
      ctx.alloc().buffer(2)
        .writeByte(comm).writeByte(value))
  }

  def sendCommands(commands: StoredDeviceCommand*): Unit ={
    this.enqueueCommands(commands:_*)
    ctx.writeAndFlush(Unpooled.wrappedBuffer(this.dequeSendingCommadsAsBinary():_*))
  }

  def dequeSendingCommadsAsBinary(): Seq[ByteBuf] = {
    sendingCommands().map(m => {
      val storedCommand = m.unwrap[DTMStoredCommand]
      makeCommand(storedCommand.command, storedCommand.value)
    })
  }


}
