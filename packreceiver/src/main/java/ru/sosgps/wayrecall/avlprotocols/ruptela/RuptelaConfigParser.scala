package ru.sosgps.wayrecall.avlprotocols.ruptela

import resource._
import java.io._
import ru.sosgps.wayrecall.utils.io.{Utils, RichDataInput}
import ru.sosgps.wayrecall.utils.io.iterateStream
import com.google.common.io.ByteStreams
import scala.collection.mutable.ArrayBuffer
import com.google.common.base.Charsets

object RuptelaConfigParser {


  @deprecated("does not work as expected")
  def readParams(file: File): Stream[Array[Byte]] = {
    val stream = new BufferedInputStream(new FileInputStream(file))
    readParams(stream)
  }

  @deprecated("does not work as expected")
  def readParams(stream: InputStream): Stream[Array[Byte]] = {
    val in = new RichDataInput(stream)

    iterateStream(stream, (a: InputStream) => {
      val out = ByteStreams.newDataOutput(16)
      val length = in.readUnsignedShort()
      out.writeShort(length)
      val len = in.readUnsignedByte()
      out.write(len)
      val value = in.readNumberOfBytes(len)
      out.write(value)
      out.toByteArray
    })
  }


  def readParamsBlocks(file: File): Stream[Array[Byte]] = {
    val stream = new BufferedInputStream(new FileInputStream(file))
    readParamsBlocks(stream)
  }

  def readParamsBlocks(stream: InputStream): Stream[Array[Byte]] = {
    val in = new RichDataInput(stream)

    iterateStream(stream, (a: InputStream) => {
      val out = ByteStreams.newDataOutput(512)
      val length = in.readUnsignedShortLE()
      out.writeShort((length & 0x00ff) << 8 | (length & 0xff00) >> 8)
      val blockId = in.readUnsignedByte()
      out.write(blockId)
      val parameterCount = in.readUnsignedByte()
      out.write(parameterCount)
      val value = in.readNumberOfBytes(length - 4)
      out.write(value)
      out.toByteArray
    })
  }

  def groupWithLimit(limit: Int, params: Stream[Array[Byte]]): Stream[Seq[Array[Byte]]] = /*if(params.isEmpty) Stream.empty else */ {
    var bytes = 0
    val sum = new ArrayBuffer[Array[Byte]]
    var tail = params
    while (tail.nonEmpty) {
      bytes = bytes + tail.head.length
      if (bytes > limit)
        return Stream.cons(sum, groupWithLimit(limit, tail))
      else {
        sum += tail.head
        tail = tail.tail
      }
    }
    return Stream(sum)
  }

  def packParams(i: Int, params: Seq[Array[Byte]]): Array[Byte] = {

    val paramCount = params.length
    val totalLen = params.map(_.length).sum + 5
    printf("totalLen=%04x\n", totalLen)
    val out = ByteStreams.newDataOutput(totalLen)
    out.writeShort(totalLen)
    out.write(i)
    printf("paramCount=%04x\n", paramCount)
    out.writeShort(paramCount)
    params.foreach(out.write)
    out.toByteArray
  }


  def packConfig(msg: String, additionalBytes: Byte*) = {
    val packdata = 0x66.toByte +: (msg.getBytes(Charsets.US_ASCII) ++ additionalBytes)
    RuptelaParser.packBody(packdata)
  }

}
