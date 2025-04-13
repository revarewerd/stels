package ru.sosgps.wayrecall.avlprotocols.egts

import java.nio.ByteOrder

import _root_.io.netty.buffer.ByteBuf
import com.google.common.base.Charsets
import ru.sosgps.wayrecall.utils._
import ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils._

import scala.collection.mutable

/**
 * Created by nickl on 26.01.15.
 */
trait Frame extends Package

class ResponseFrame extends Frame with grizzled.slf4j.Logging {
  var responsePacketId: Int = 0
  var processingResult: Byte = 0

  override def read(frameData: ByteBuf) = {
    responsePacketId = frameData.readUnsignedShort()
    debug(s"responsePacketId = $responsePacketId")

    processingResult = frameData.readByte()
    debug(s"processingResult = $processingResult")
    this
  }

  override def write(buffer1: ByteBuf): ByteBuf = {
    buffer1.writeShort(responsePacketId)
    buffer1.writeByte(processingResult)
  }
}



