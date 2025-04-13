package ru.sosgps.wayrecall.avlprotocols.egts

import java.nio.ByteOrder

import _root_.io.netty.buffer.{Unpooled, ByteBuf}
import com.google.common.base.Charsets
import ru.sosgps.wayrecall.avlprotocols.navtelecom.FlexParser
import ru.sosgps.wayrecall.utils._
import ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.io.{CRC16CCITT, Utils}

/**
 * Created by nickl on 25.01.15.
 */
class TransportPackage extends grizzled.slf4j.Logging with Package {

  var prv: Byte = 0;
  var skid: Byte = 0;
  var prf: Byte = 0;
  var headerEncoding: Byte = 0;
  var packetIdentifier: Int = 0;
  var packetType: Byte = 0;

  var peerAddress: Option[Int] = None
  var recipientAddress: Option[Int] = None
  var ttl: Option[Byte] = None

  var frame: Frame = null

  override def read(src0: ByteBuf) = {
    val src = src0.order(ByteOrder.LITTLE_ENDIAN)
    debug("readableBytes = " + src.readableBytes())
    val headerStart = src.readerIndex()
    prv = src.readByte()
    debug(s"prv = $prv")
    skid = src.readByte()
    debug(s"skid = $skid")
    prf = src.readByte()
    debug(s"prf = $prf")
    val headerLength = src.readByte()
    debug(s"headerLength = $headerLength")
    headerEncoding = src.readByte()
    debug(s"headerEncoding = $headerEncoding")
    val frameDataLength = src.readUnsignedShort()
    debug(s"frameDataLength = $frameDataLength")
    packetIdentifier = src.readUnsignedShort()
    debug(s"packetIdentifier = $packetIdentifier")
    packetType = src.readByte()
    debug(s"packetType = $packetType")

    def hasData = {
      val remains = headerLength - (src.readerIndex() - headerStart)
      debug(s"remains=$remains")
      remains > 1
    }

    peerAddress = iff(hasData)(src.readUnsignedShort())

    debug(s"peerAddress = $peerAddress")

    recipientAddress = iff(hasData)(src.readUnsignedShort())

    debug(s"recipientAddress = $recipientAddress")

    ttl = iff(hasData)(src.readByte())

    debug(s"ttl = $ttl")

    val headerChecksum = src.readByte()
    val headerEnd = src.readerIndex()
    debug(s"headerChecksum = $headerChecksum")
    debug("calked = " + FlexParser.crc8(src.toIterator(headerStart, headerLength - 1)).toByte)

    val frameData = src.readSlice(frameDataLength)
    debug(s"frameData = ${Utils.toHexString(frameData," ")}")
    val calkedframeCheckSum = CRC16CCITT.calc(frameData.toIteratorReadable.toArray)

    frame = (packetType match {
      case 1 => new AppDataFrame()
      case 0 => new ResponseFrame()
    }).read(frameData)

    debug("body=" + Utils.toHexString(frameData, " "))
    debug("bodyString=" + frameData.toString(Charsets.UTF_8))

    debug("calkedframeCheckSum = " + calkedframeCheckSum)

    val frameCheckSum = src.readUnsignedShort()
    debug(s"frameCheckSum = $frameCheckSum")

    debug("readableBytes = " + src.readableBytes())
    this
  }

  override def write(buffer1: ByteBuf): ByteBuf = {
    val out = buffer1.order(ByteOrder.LITTLE_ENDIAN)
    val headerStart = out.writerIndex()
    out.writeByte(prv)
    out.writeByte(skid)
    out.writeByte(prf)
    val headerLength = 11 +
      peerAddress.map(_ => 2).getOrElse(0) +
      recipientAddress.map(_ => 2).getOrElse(0) +
      ttl.map(_ => 1).getOrElse(0)

    out.writeByte(headerLength)
    out.writeByte(headerEncoding)
    val frameSizeField = out.writerIndex()
    out.writeShort(-1) // reserve
    out.writeShort(packetIdentifier)
    out.writeByte(packetType)

    peerAddress.foreach(out.writeShort)
    recipientAddress.foreach(out.writeShort)
    ttl.foreach(i => out.writeByte(i))


    val crcField = out.writerIndex()
    out.writeByte(-1) // reserve
    val frameDataStart = out.writerIndex()
    frame.write(out)
    val frameLength = out.writerIndex() - frameDataStart
    out.setShort(frameSizeField, frameLength)

    val crc = FlexParser.crc8(out.toIterator(headerStart, headerLength - 1)).toByte
    debug("calked = " + crc)
    out.setByte(crcField, crc)

    val calkedframeCheckSum = CRC16CCITT.calc(out.toIterator(frameDataStart,frameLength).toArray)
    //out.writeBytes(frameData)

    debug("calkedframeCheckSum = " + calkedframeCheckSum)
    out.writeShort(calkedframeCheckSum)

    out
  }


}
