package ru.sosgps.wayrecall.utils.io

import java.io.{FileOutputStream, BufferedOutputStream, File, OutputStream}
import java.nio.file.Files
import java.util.zip.GZIPOutputStream

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream

/**
 * Created by nickl on 19.01.15.
 */
class RollingFileOutputStream(name: String) extends OutputStream {

  private var written = 0

  private val sizeLimit = 1024 * 1024 * 10;

  private var curFile = newFile(name)

  private var curOut: OutputStream = new BufferedOutputStream(new FileOutputStream(curFile))

  override def write(b: Int): Unit = synchronized {
    curOut.write(b)
    written = written + 1
    rollIfNedded()
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = synchronized {
    curOut.write(b, off, len)
    written = written + len
    rollIfNedded()
  }

  private def rollIfNedded(): Unit = {
    if (written > sizeLimit) {
      curOut.close()
      val zippingFile = newFile(curFile.getAbsolutePath + "{n}.gz")
      val gzipCompressorOutputStream = new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(zippingFile)))
      Files.copy(curFile.toPath, gzipCompressorOutputStream)
      gzipCompressorOutputStream.close()
      curFile.delete();
      curFile = newFile(name)
      curOut = new BufferedOutputStream(new FileOutputStream(curFile))
      written = 0
    }
  }

  override def flush(): Unit = curOut.flush()

  override def close(): Unit = curOut.close()
}
