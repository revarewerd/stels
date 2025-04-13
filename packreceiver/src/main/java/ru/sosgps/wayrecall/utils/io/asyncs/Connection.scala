package ru.sosgps.wayrecall.utils.io.asyncs

import java.nio.ByteOrder
import java.nio.charset.Charset

import com.google.common.base.Charsets
import io.netty.buffer.{ByteBuf, ByteBufAllocator, Unpooled}
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps

import scala.concurrent.Future

/**
  * Created by nmitropo on 15.8.2016.
  */
trait Connection {

  def allocator: ByteBufAllocator

  def in: ByteBufAsyncIn

  def out: ByteBufAsyncOut

  def close(): Unit

}

abstract class ByteBufAsyncIn {

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  //def readByteBufSlice(size: Int): Future[ByteBuf]

  def readTransformAndRelease[T](size: Int, t: (ByteBuf) => T): Future[T]

  /*= {
     readByteBufSlice(size).map(bb => {
       val t1 = t(bb)
       //bb.release()
       t1
     })
   }*/

  def readInt: Future[Int] = readTransformAndRelease(4, _.readInt())

  def readLong: Future[Long] = readTransformAndRelease(8, _.readLong())

  def readBoolean = readTransformAndRelease(1, _.readBoolean())

  def readByte = readTransformAndRelease(1, _.readByte())

  def readChar = readTransformAndRelease(1, _.readChar())

  def readDouble = readTransformAndRelease(8, _.readDouble())

  def readFloat = readTransformAndRelease(4, _.readFloat())

  def readMedium = readTransformAndRelease(3, _.readMedium())

  def readShort = readTransformAndRelease(2, _.readShort())

  def readUnsignedByte = readTransformAndRelease(1, _.readUnsignedByte())

  def readUnsignedInt = readTransformAndRelease(4, _.readUnsignedInt())

  def readUnsignedMedium = readTransformAndRelease(3, _.readUnsignedMedium())

  def readUnsignedShort = readTransformAndRelease(2, _.readUnsignedShort())

  def readByteBuf(length: Int) = readTransformAndRelease(length, _.readBytes(length))

  def readByteArray(length: Int) = readTransformAndRelease(length, _.readBytesToArray(length))

  def readBytesAsString(length: Int, charset: Charset = Charsets.US_ASCII) = readTransformAndRelease(length, _.readBytesAsString(length, charset))

  def order(byteOrder: ByteOrder) = {
    val self = this
    new ByteBufAsyncIn {
      //override def readByteBufSlice(size: Int): Future[ByteBuf] = self.readByteBufSlice(size).map(_.order(byteOrder))
      override def readTransformAndRelease[T](size: Int, t: (ByteBuf) => T): Future[T] =
      self.readTransformAndRelease(size, (bb) => t(bb.order(byteOrder)))
    }
  }

}

class RecordingByteBufAsyncIn(wrapped: ByteBufAsyncIn) extends ByteBufAsyncIn {

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  val recorded = Unpooled.buffer()

  //  override def readByteBufSlice(size: Int): Future[ByteBuf] = wrapped.readByteBufSlice(size).map(bb => {
  //    recorded.writeBytes(bb, bb.readerIndex(), bb.readableBytes())
  //    bb
  //  })
  override def readTransformAndRelease[T](size: Int, t: (ByteBuf) => T): Future[T] = {
    wrapped.readTransformAndRelease(size, (bb) => {
      recorded.writeBytes(bb, bb.readerIndex(), bb.readableBytes())
      t(bb)
    })
  }
}

abstract class ByteBufAsyncOut {

  def alloc(size: Int): ByteBuf

  def write(bb: ByteBuf): Future[Unit]

  def writeInt(i: Int) = write(alloc(4).writeInt(i))

  def writeByte(byte: Int) = write(alloc(1).writeByte(byte))

  def writeShort(i: Short) = write(alloc(2).writeShort(i))

  def writeMedium(i: Int) = write(alloc(3).writeMedium(i))

  def writeLong(i: Long) = write(alloc(8).writeLong(i))

  def writeBytes(array: Array[Byte]) = write(alloc(array.length).writeBytes(array))

  def order(byteOrder: ByteOrder) = {
    val self = this
    new ByteBufAsyncOut {
      override def alloc(size: Int): ByteBuf = self.alloc(size).order(byteOrder)

      override def write(bb: ByteBuf): Future[Unit] = self.write(bb)
    }
  }


}
