package ru.sosgps.wayrecall.packreceiver.netty

import java.io.DataInput
import javax.annotation.{PreDestroy, PostConstruct}

import com.google.common.base.Charsets
import com.google.common.util.concurrent.MoreExecutors
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.{Unpooled, ByteBuf, ByteBufInputStream}
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler, ChannelOption}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.handler.logging.LoggingHandler
import org.joda.time.DateTime
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.PackProcessor
import ru.sosgps.wayrecall.utils.io.{Utils, RichDataInput}

import scala.beans.BeanProperty
import scala.concurrent.ExecutionContext

/**
 * Created by nickl on 07.10.14.
 */
class SkyPatrolNettyServer {

  @BeanProperty
  var store: PackProcessor = null

  @BeanProperty
  var port = 9090

  val group = new NioEventLoopGroup();
  val b = new Bootstrap();
  @PostConstruct
  def start(): Unit ={
      b.group(group)
        .channel(classOf[NioDatagramChannel])
      .option[java.lang.Boolean](ChannelOption.SO_BROADCAST, true)
        //.handler(new LoggingHandler(this.getClass))
        .handler(new SkyPatrolMessageReceiver(store));

    b.bind(port)//.sync().channel().closeFuture().await();

  }

  @PreDestroy
  def stop(): Unit = {
    //b.
    group.shutdownGracefully();
  }

}

class SkyPatrolMessageReceiver(store: PackProcessor) extends SimpleChannelInboundHandler[DatagramPacket] with grizzled.slf4j.Logging {

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  override def channelRead0(ctx: ChannelHandlerContext, msg: DatagramPacket): Unit = {
    val buf = msg.content
    debug("buf=" + Utils.toHexString(buf, ""))
    try {
      val in = new RichDataInput(new ByteBufInputStream(buf): DataInput)

      val gps = parseData(in)

      store.addGpsDataAsync(gps).onFailure{
        case e => {
          error("error processing " + buf, e)
          ctx.fireExceptionCaught(e)
        }
      }

    } catch {
      case e: Exception =>
        error("error processing " + buf, e)
        throw e
    }


  }

  val POSITION_REPORT = 1

  def parseData(in: RichDataInput):GPSData = {
    val api = in.readShort()
    debug("api = " + api)
    val commandType = in.readByte()
    debug(s"commandType = $commandType")
    val messageType = in.readByte()
    debug(s"messageType = $messageType")
    val messageTypeI = messageType >> 4
    debug(s"messageTypeI = $messageTypeI")
    val ack = messageType & 0xF
    debug(s"ack = $ack")
    val apiHeaderSize = in.readUnsignedByte()
    debug(s"apiHeaderSize = $apiHeaderSize")
    val apiHeader = in.readNumberOfBytes(apiHeaderSize)
    debug(s"apiHeader = ${Utils.toHexString(apiHeader, " ")}")

    val header = Unpooled.wrappedBuffer(apiHeader).readInt()

    def hbit(i: Int):Boolean = (header >> i & 0x1) == 1

    if (messageTypeI == POSITION_REPORT) {
      val ascii = !hbit(1)
      readInBinary(in, hbit)
    } else
      throw new UnsupportedOperationException("messageTypeI = "+messageTypeI)

  }

  private def readInBinary(in: RichDataInput, mask: (Int) => Boolean): GPSData = {

    def maskopt[T](i: Int)(f: => T) = if (mask(i)) Some(f) else None

    val param1Status = maskopt(1)(in.readInt()) // 2
    val deviseId = new String(in.readNumberOfBytes(if (mask(23)) 8 else 22), Charsets.US_ASCII) // 3
    val iodata = maskopt(3)(in.readShort()) // 4
    val ai1 = maskopt(4)(in.readUnsignedShort()) // 5
    val ai2 = maskopt(5)(in.readUnsignedShort()) // 6
    val functionCategory = maskopt(7)(in.readByte()) // 7

    // 8

    val day = maskopt(8) {in.readByte()}
    val month = maskopt(8) {in.readByte()}
    val year = maskopt(8) {in.readUnsignedByte()}

    val gpsstatus = maskopt(9) {in.readByte()} // 9
    val lat = maskopt(10) {convertll(in.readInt())} // 10
    val lon = maskopt(11) {convertll(in.readInt())} // 11
    val gpsVel = maskopt(12) {in.readUnsignedShort() / 10.0} // 12
    val heading = maskopt(13) {in.readUnsignedShort() / 10.0} // 13

    // 14
    val hours = maskopt(14) {in.readByte()}
    val mins = maskopt(14) {in.readByte()}
    val secs = maskopt(14) {in.readByte()}

    val alt = maskopt(15) {in.readByte() << 16 | in.readByte() << 8 | in.readByte()};
    // 15
    val satl = maskopt(16) {in.readUnsignedByte()} // 16
    val batpers = maskopt(17) {in.readUnsignedShort()} // 17
    val trimodometr = maskopt(20) {in.readInt()} // 18
    val odometr = maskopt(21) {in.readInt()} // 19

    //20
    val rtcyear = maskopt(22) {in.readUnsignedByte()}
    val rtcmonth = maskopt(22) {in.readByte()}
    val rtcday = maskopt(22) {in.readByte()}
    val rtchour = maskopt(22) {in.readByte()}
    val rtcmin = maskopt(22) {in.readByte()}
    val rtcsec = maskopt(22) {in.readByte()}
    val rtsdate = for (y <- year;
                       m <- rtcmonth;
                       d <- rtcday;
                       h <- rtchour;
                       min <- rtcmin;
                       s <- rtcsec
    ) yield new DateTime(y, m, d, h, min, s).toDate
    val batteryVoltage = maskopt(24) {in.readShort()} // 21
    val gpsOverspeed = maskopt(25) {in.readNumberOfBytes(18)} // 22
    val cellInf = maskopt(26) {in.readNumberOfBytes(54)} // 23
    debug(s"cellInf = ${cellInf.map(Utils.toHexString(_, " "))}")
    val seqNum = maskopt(28) {in.readUnsignedShort()} // 24
    val d = for (date <- rtsdate) yield {
        (for (lon <- lon; lat <- lat) yield
          new GPSData(null, deviseId, lon, lat,
            date,
            gpsVel.getOrElse(0.0).toShort, heading.getOrElse(0.0).toShort, satl.getOrElse(0).toByte
          )).getOrElse(new GPSData(null, deviseId,
          date
        ))
      }
    d.get
  }

  def convertll(intLat: Int): Double ={
//      if(intLat > 0){
        val mins = (intLat % 1000000) / 10000.0
        val deg = intLat / 1000000
        deg + mins / 60
//      }
//    else{
//
//      }
  }

}
