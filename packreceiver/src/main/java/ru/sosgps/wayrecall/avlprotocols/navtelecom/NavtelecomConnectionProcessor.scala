package ru.sosgps.wayrecall.avlprotocols.navtelecom

import io.netty.buffer.{Unpooled, ByteBuf}
import ru.sosgps.wayrecall.avlprotocols.common.{StoredDeviceCommand, StoredDeviceCommandsQueue}
import ru.sosgps.wayrecall.avlprotocols.navtelecom.NavtelecomParser._
import ru.sosgps.wayrecall.avlprotocols.navtelecom.FlexParser
import ru.sosgps.wayrecall.utils.ScalaNettyUtils
import ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.io.{Utils, RichDataInput}
import com.google.common.base.Charsets
import java.io.{DataInput, InputStream}
import java.nio.ByteOrder
import ru.sosgps.wayrecall.packreceiver.PackProcessor
import scala.collection.immutable.{SortedSet, TreeSet}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import ru.sosgps.wayrecall.core.{GPSUtils, GPSData}

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by nickl on 03.06.14.
 */
class NavtelecomConnectionProcessor(val store: PackProcessor
                                     )(implicit executor: ExecutionContext) extends StoredDeviceCommandsQueue[NavtelecomCommand] with grizzled.slf4j.Logging {

  var imei: Option[String] = None

  var flex = false

  var flexBits: SortedSet[Int] = SortedSet.empty

  var flexSize = 0

  var lastIDhost: Option[Int] = None
  var lastIDdevice: Option[Int] = None

  private val emptyBuffer: ByteBuf = Unpooled.unreleasableBuffer(Unpooled.unmodifiableBuffer(
    Unpooled.buffer()
  ))

  private def isFlex(channelBuffer: ByteBuf): Boolean = {
    channelBuffer.getByte(channelBuffer.readerIndex) == '~'
  }

  def processPackage(channelBuffer: ByteBuf): Future[ByteBuf] = {
    val processed = if (isFlex(channelBuffer)) {
      processFlex(channelBuffer)
    } else {
      val incomingPackage = parsePrefix(channelBuffer)
      lastIDhost = Some(incomingPackage.IDr)
      lastIDdevice = Some(incomingPackage.IDs)
      processNCTB(incomingPackage)
    }


    //    val command: Option[Future[ByteBuf]] = getCommandsToSend
    //
    //    command match {
    //      case Some(command) => for (processed <- processed; command <- command) yield Unpooled.wrappedBuffer(processed, command)
    //      case None => processed
    //    }

    val sendingCommandsButeBuffs = dequeSendingCommadsAsBinary()

    if (sendingCommandsButeBuffs.isEmpty) {
      processed
    }
    else {
      processed.map(processedData => {
        val buffer = Unpooled.wrappedBuffer(sendingCommandsButeBuffs: _*)
        Unpooled.wrappedBuffer(processedData, buffer)
      })
    }

  }

  def dequeSendingCommadsAsBinary(): Seq[ByteBuf] = {
    sendingCommands().map(m => makeCommand("@NTC", lastIDdevice.getOrElse(0), lastIDhost.getOrElse(1), m))
  }

  //  protected def getCommandsToSend: Option[Future[ByteBuf]] = {
  //    imei.map(i => commands.getSendingCommands(i)).map(
  //      commandsSeqFuture => {
  //        commandsSeqFuture.map(
  //          commands => Unpooled.wrappedBuffer(
  //            commands.map(m => makeCommand("@NTC", lastIDr.getOrElse(0), lastIDs.getOrElse(0), m)): _*
  //          )
  //        )
  //    })
  //  }

  private def processNCTB(pack: NavtelecomPackage): Future[ByteBuf] = {

    if (pack.body.length == 0) {
      return Future.successful(emptyBuffer)
    }

    processBody(pack.body)
      .map(body =>
      if (body.readableBytes() > 0)
        makePackage("@NTC", pack.IDs, pack.IDr, body)
      else
        body
      )
  }

  private def processBody(body: Array[Byte]): Future[ByteBuf] = {

    Unpooled.wrappedBuffer(body).doWith(bb => {
      //      val prefixBytes = Array.ofDim[Byte](3)
      //      bb.getBytes(bb.readerIndex, prefixBytes)
      //      val prefix = new String(prefixBytes, Charsets.US_ASCII)

      val retrievedGPSses = new ListBuffer[GPSData]

      val resp: ByteBuf = Seq(
        processNTCBTelemetry(body, retrievedGPSses),
        processCommandAnswers(body, retrievedGPSses)
      ).reduce(_ orElse _)(bb)

      debug("processBody resp:" + Utils.toHexString(resp, " "))

      Future.sequence(retrievedGPSses.map(processGPSData)).map(_ => resp)
    })
  }

  protected def processGPSData(g: GPSData): Future[GPSData] = {
    val storeFuture = store.addGpsDataAsync(g)

    //debug("processGPSData:"+commandsToAnswer.size+" " + g)

    notifyCommands(g)

    storeFuture
  }

  private def processNTCBTelemetry(body: Array[Byte], retrievedGPSses: mutable.Buffer[GPSData]): PartialFunction[ByteBuf, ByteBuf] =
    processNTCBTelemetry(body: Array[Byte], new RichDataInput(body.drop(3)), retrievedGPSses: mutable.Buffer[GPSData])


  case class Prefix(str: String) {
    def unapply(b: String) = str.equals(b)

    def unapply(bb: ByteBuf) =
      bb.readableBytes() >= str.size && str.zipWithIndex.forall(e => bb.getByte(bb.readerIndex() + e._2) == e._1)

  }

  val TELEMETRIC_ARRAY = Prefix("*>A")
  val HANDSNAKE = Prefix("*>S")
  val ALARM = Prefix("*>T")
  val TMKEY = Prefix("*>TMKEY")
  val FLEX = Prefix("*>FLEX")

  private def processNTCBTelemetry(body: Array[Byte], bodystream: RichDataInput, retrievedGPSses: mutable.Buffer[GPSData]): PartialFunction[ByteBuf, ByteBuf] = {
    case TELEMETRIC_ARRAY() => {
      val arrsize = bodystream.readUnsignedByte()
      debug("arrsize=" + arrsize)

      for (i <- 0 until arrsize) {
        retrievedGPSses += NavtelecomParser.parseTelemetry(imei.get, bodystream.getWrapped.asInstanceOf[InputStream])._2
      }

      Unpooled.wrappedBuffer(("*<A".getBytes(Charsets.US_ASCII) ++ Array(arrsize.toByte)))
    }
    case HANDSNAKE() => {
      imei = Some(new String(bodystream.readNumberOfBytes(body.size - 3).drop(1), Charsets.US_ASCII))
      Unpooled.wrappedBuffer("*<S".getBytes(Charsets.US_ASCII))
    }
    case TMKEY() => {
      val telemetryBody = bodystream.readNumberOfBytes(body.size - 3)
      debug("TMKEY:=" + Utils.toHexString(telemetryBody, ""))
      Unpooled.wrappedBuffer("*<TMKEY".getBytes(Charsets.US_ASCII))
    }
    case ALARM() => {
      val telemetryBody = bodystream.readNumberOfBytes(body.size - 3)
      debug("tetemetricdata:=" + Utils.toHexString(telemetryBody, ""))
      val buffer = Unpooled.buffer(8).order(ByteOrder.LITTLE_ENDIAN)
      buffer.writeBytes("*<T".getBytes(Charsets.US_ASCII))
      val (i, gps) = NavtelecomParser.parseTelemetry(imei.get, telemetryBody)
      retrievedGPSses += gps
      buffer.writeInt(i)
      //buffer.writeShort(parseTelemetry(telemetryBody))
      buffer
    }
    case FLEX() => {
      flex = true
      val prefix2 = new String(bodystream.readNumberOfBytes(3), Charsets.US_ASCII)
      val protocol = bodystream.readUnsignedByte()
      require(protocol == 0xB0, "protocol must be NTCB_FLEX not " + protocol)
      val protocolVersion = bodystream.readUnsignedByte()
      val structVersion = bodystream.readUnsignedByte()
      debug(s"protocolVersion=$protocolVersion structVersion=$structVersion")
      val datasize = bodystream.readUnsignedByte()
      debug("datasize:" + datasize)
      val bitfield: Array[Byte] = bodystream.readNumberOfBytes(datasize / 8 + 1)
      debug("bitfield:" + Utils.toHexString(bitfield, ""))
      updateFlexPacks(bitfield)
      makeFlexPackage(0xB0, 0x0A, 0x0A)
    }
  }

  val WRONG_PASSWORD = new Prefix("*?P") // *?PASS
  val RIGHT_PASSWORD = new Prefix("*!P") // *!PASS
  val COMMAND_ACCEPTED = new Prefix("*@C") // *!PASS

  def processCommandAnswers(body: Array[Byte], retrievedGPSses: mutable.Buffer[GPSData]): PartialFunction[ByteBuf, ByteBuf] = {

    case WRONG_PASSWORD() | RIGHT_PASSWORD() => notifyCommands(body); emptyBuffer
    case COMMAND_ACCEPTED() => {
      assert(retrievedGPSses.isEmpty)
      val fakeFlex = Unpooled.wrappedBuffer(body).order(ByteOrder.LITTLE_ENDIAN).skipBytes(1)
      fakeFlex.setByte(fakeFlex.readerIndex(), '~')
      val resp = processFlexBody(
        if (flexBits.nonEmpty) flexBits else TreeSet(1, 2, 3, 7, 8, 9, 10, 11, 12, 13, 14, 19),
        fakeFlex,
        retrievedGPSses
      )
      notifyCommands(retrievedGPSses)
      resp
    }

  }

  private def processFlex(pack: ByteBuf): Future[ByteBuf] = {
    val retrievedGPSses = new ListBuffer[GPSData]
    val resp = processFlexBody(pack, retrievedGPSses)
    Future.sequence(retrievedGPSses.map(processGPSData)).map(_ => resp)
  }

  private def processFlexBody(pack: ByteBuf, retrievedGPSses: mutable.Buffer[GPSData]): ByteBuf =
    processFlexBody(flexBits, pack, retrievedGPSses)

  private def processFlexBody(flexBits: SortedSet[Int], pack: ByteBuf, retrievedGPSses: mutable.Buffer[GPSData]): ByteBuf = {
    require(pack.readByte() == '~', " not a flex pac")
    val coommand = pack.readByte()

    def addRetrievedIfValid(gpses: GPSData*): Any = {
      gpses.foreach(gPSData =>
        if (GPSUtils.valid(gPSData))
          retrievedGPSses += gPSData
        else
          warn("invalid gps parsed:" + gPSData)
      )
    }
    coommand match {
      case 'C' => {
        debug("Flex pack:" + Utils.toHexString(pack, "") + " " + pack.order())
        val gPSData = FlexParser.parseBody(imei.orNull, pack.order(ByteOrder.LITTLE_ENDIAN), flexBits)
        val resp = Unpooled.buffer(3).order(ByteOrder.LITTLE_ENDIAN)
        resp.writeByte('~')
        resp.writeByte('C')
        resp.writeByte(FlexParser.crc8readable(resp))
        addRetrievedIfValid(gPSData)
        resp
      }

      case 'T' => {
        val eventIndex = pack.readUnsignedInt()
        val gPSData = FlexParser.parseBody(imei.orNull, pack, flexBits)
        val resp = Unpooled.buffer(3).order(ByteOrder.LITTLE_ENDIAN)
        resp.writeByte('~')
        resp.writeByte('T')
        resp.writeInt(eventIndex.toInt)
        resp.writeByte(FlexParser.crc8readable(resp))
        addRetrievedIfValid(gPSData)
        resp
      }
      case 'A' => {
        val size = pack.readUnsignedByte()
        val gpses = (0 until size).map(i => FlexParser.parseBody(imei.orNull, pack, flexBits))
        debug("gpses = " + gpses)
        val resp = Unpooled.buffer(3).order(ByteOrder.LITTLE_ENDIAN)
        resp.writeByte('~')
        resp.writeByte('A')
        resp.writeByte(size)
        resp.writeByte(FlexParser.crc8readable(resp))
        addRetrievedIfValid(gpses: _*)
        resp
      }

    }
  }

  private def updateFlexPacks(bitfield: Array[Byte]) {
    debug("bitfieldbin:" + bitfield.map(_.toInt.toBinaryString.drop(8 * 3)).mkString(" "))
    flexBits = getDefinedBits(bitfield)
    debug("definedBits=" + flexBits)
    //debug("sizes=" + FlexParser.sizes.toSeq.sortBy(_._1))
    val sum: Int = flexBits.toIterator.map(i => FlexParser.sizes(i)).sum
    debug("definedsize=" + sum)
    flexSize = sum
  }

  def getDefinedBits(bf: Array[Byte]): SortedSet[Int] = {
    (for ((b, i) <- bf.zipWithIndex;
          j <- 1 to 8 if (b >> (8 - j) & 0x1) == 1
    ) yield i * 8 + j).to[TreeSet]
  }

}


trait NavtelecomCommand extends StoredDeviceCommand


class IOSwitchCommand(var text: String) extends NavtelecomCommand with grizzled.slf4j.Logging {

  def this(ionum: Int, stateToSet: Boolean) = this("!" + ionum + (stateToSet match {
    case true => "Y"
    case false => "N"
  }))

  override def answerAccepted: Boolean = _answerAccepted

  private var _answerAccepted = false

  override def accept(gpsData: GPSData): Unit = {
    debug("IOSwitchCommand accepting:" + gpsData)
    _answerAccepted = true
  }

  override def accept(b: Array[Byte]): Unit = {
    debug("IOSwitchCommand accepting:" + Utils.toHexString(b, " "))
  }
}

class PasswordCommand(passw: String) extends NavtelecomCommand with grizzled.slf4j.Logging {
  override def text: String = ">PASS:" + passw

  override def answerAccepted: Boolean = _answerAccepted

  private var _answerAccepted = false

  override def accept(gpsData: GPSData): Unit = {
    debug("PasswordCommand accepting:" + gpsData)
  }

  override def accept(b: Array[Byte]): Unit = {
    val tt = new String(b, Charsets.US_ASCII)
    debug("PasswordCommand accepting:" + Utils.toHexString(b, " ") + " " + tt)
    if (tt.endsWith("PASS"))
      _answerAccepted = true
  }
}
