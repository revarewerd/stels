package ru.sosgps.wayrecall.packreceiver.blocking

import java.net.{ServerSocket, Socket}
import resource._
import java.io._
import java.text.{SimpleDateFormat, ParsePosition, FieldPosition, DateFormat}
import java.util.Date
import java.util.zip.GZIPOutputStream
import scala.collection.mutable.ListBuffer
import ru.sosgps.wayrecall.utils.io.Utils
import scala.annotation.tailrec

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 01.06.13
 * Time: 19:20
 * To change this template use File | Settings | File Templates.
 */
class Proxy(incomingConnection: Socket, outConn: Socket) extends Closeable {

  private[this] val file = Iterator.from(0).map(i => new File("bilod" + i + ".log")).filter(!_.exists()).next()

  private[this] val bilogger = new BiLogger(
    //new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)))
    new PrintWriter(file)
  )

  class StreamCopy(in: InputStream, out: OutputStream, key: String) extends Runnable {
    def run() {
      val buff = Array.ofDim[Byte](10240)

      @tailrec
      def wrwr() {
        val count = in.read(buff)
        if (count > 0) {
          bilogger.write(key, buff, 0, count)
          bilogger.flush()
          out.write(buff, 0, count)
          out.flush()
          wrwr()
        }
        else {
          bilogger.write(key + "EOF", 0)
          bilogger.flush()
          in.close()
          out.close()
        }
      }
      wrwr()


    }
  }

  val inThread = new Thread(new StreamCopy(incomingConnection.getInputStream, outConn.getOutputStream, "->")).start()
  val outThread = new Thread(new StreamCopy(outConn.getInputStream, incomingConnection.getOutputStream, "<-")).start()

  def close() {
    bilogger.close()
    incomingConnection.close()
    outConn.close()
  }
}


class BiLogger(out: PrintWriter) extends Closeable with Flushable {
  private[this] var key: String = null
  private[this] var writtenlen = 0;

  private[this] val df = new SimpleDateFormat("HH:mm:ss:SSS")


  def write(key: String, b: Int) = writeF(key, {
    out.printf("%02x ", (b & 0x000000ff).asInstanceOf[AnyRef])
    writtenlen = writtenlen + 1
  })

  def write(key: String, bytes: Array[Byte], start: Int, len: Int) = writeF(key, {
    for (i <- start until (start + len)) {
      out.printf("%02x ", (bytes(i) & 0x000000ff).asInstanceOf[AnyRef])
    }
    writtenlen = writtenlen + len
  })

  def writeF(key: String, f: => Unit) = synchronized {

    if (key == this.key) {
      f
      if (writtenlen >= 64) {
        out.println()
        writtenlen = 0
        out.flush();
      }
    }
    else {
      out.println();
      out.print(df.format(new Date()) + " " + key + " ")
      f
      this.key = key;
      writtenlen = 0
      out.flush();
    }

  }


  def close() {
    out.close()
  }

  def flush() {
    out.flush()
  }
}

object Proxy {
  def main(args: Array[String]) {

    val ss = new ServerSocket(args(0).toInt)

    val connections = new ListBuffer[Closeable]

    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      def run() {
        connections.foreach(_.close())
        ss.close()
      }
    }))

    while (true) {
      val accept = ss.accept()
      connections += new Proxy(accept, new Socket(args(1), args(2).toInt))
    }

  }
}
