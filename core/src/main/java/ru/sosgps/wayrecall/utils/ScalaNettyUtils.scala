package ru.sosgps.wayrecall.utils

import java.nio.charset.Charset

import _root_.io.netty.buffer.{ByteBuf, ByteBufProcessor, CompositeByteBuf}
import com.google.common.base.Charsets
import ru.sosgps.wayrecall.utils.io.Utils

/**
  * Created by nickl on 04.11.14.
  */
object ScalaNettyUtils {


  implicit class FuncToByteBuffProc(f: (Byte) => Any) extends ByteBufProcessor {
    override def process(value: Byte): Boolean = {
      f;
      true
    }
  }

  implicit class ByteBufOps(val bb: ByteBuf) extends AnyVal {

    def readBytesAsString(length: Int, charset: Charset = Charsets.US_ASCII): String = {
      val bytes: Array[Byte] = readBytesToArray(length)
      new String(bytes, charset)
    }

    def readBytesToArray(length: Int): Array[Byte] = {
      val bytes = Array.ofDim[Byte](length)
      bb.readBytes(bytes)
      bytes
    }

    def foreach(index: Int, length: Int, f: (Byte) => Any) = {
      bb.forEachByte(index, length, f)
    }

    def toIterator(index: Int, length: Int): Iterator[Byte] =
      Iterator.from(index).take(length).map(bb.getByte)

    def toIteratorReadable =
      toIterator(bb.readerIndex(), bb.readableBytes())

    def findByteInReadable(byte: Int):Int = {
      for (i <- bb.readerIndex() until bb.readerIndex() + bb.readableBytes()) {
        if(bb.getByte(i) == byte)
          return i
      }
      return -1
    }

    def fold[@specialized(Long, Int, Short, Byte) T](index: Int, length: Int)(start: T)(f: (T, Byte) => T): T = {
      var r = start
      for (i <- index until index + length) {
        r = f(r, bb.getByte(i))
      }
      r
    }

    def toHexString: String = Utils.toHexString(toIteratorReadable.toArray, " ")

    def writeAndRelease(data: ByteBuf): ByteBuf = {
      bb.writeBytes(data)
      data.release()
      bb
    }

    def doWith[T](f: (ByteBuf) => T) = {
      try {
        f(bb)
      }
      finally bb.release()
    }

  }

  implicit class CompositeByteBufOps(val bb: CompositeByteBuf) extends AnyVal {

    def writeComponent(component: ByteBuf): CompositeByteBuf = {
      bb.addComponent(component).writerIndex(bb.writerIndex() + component.readableBytes())
    }

  }


}
