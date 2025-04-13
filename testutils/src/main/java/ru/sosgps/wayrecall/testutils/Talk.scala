package ru.sosgps.wayrecall.testutils

import java.io.{InputStream, OutputStream}
import java.net.Socket

import com.google.common.base.Charsets
import org.junit.Assert
import ru.sosgps.wayrecall.utils.io.Utils

/**
 * Created by nickl on 21.11.14.
 */
abstract class TalkFramework extends grizzled.slf4j.Logging {

  var in: InputStream = null
  var out: OutputStream = null

  def set(socket: Socket) = {
   in= socket.getInputStream
   out =socket.getOutputStream
  }

  def send(bytesHex: Array[Byte]): Unit = {
    out.write(bytesHex)
    out.flush()
  }

  def send(send: String): Unit = {
    val bytesHex = Utils.asBytesHex(send)
    this.send(bytesHex: Array[Byte])
  }

  def send(bytes: Byte*): Unit = {
    this.send(bytes.toArray)
  }

  def expect(expect: String)  {
    val hex = Utils.asBytesHex(expect)
    this.expect(hex)
  }

  def expectAnyOrderS(expect: String*)  {
    this.expectAnyOrderB(expect.map(Utils.asBytesHex):_*)
  }

  def expect(hex: Array[Byte]) {
    val r1 = waitAndReadAvaliable(in)
    debug("resparr=" + Utils.toHexString(r1, ""))
    debug("respstr=" + new String(r1, Charsets.US_ASCII))
    assertArrayEquals("",hex, r1)
  }

  def expectAnyOrderB(hex: Array[Byte]*) {

    def tryReceveRemaininPremuatations(prevData: Array[Byte], attempts: Int): (Array[Byte], Boolean) = {
      if(attempts <= 0)
        return (prevData, false)
      val r1 = try prevData ++ waitAndReadAvaliable(in) catch {
        case t: java.net.SocketTimeoutException =>
          warn("tryReceveRemaininPremuatations timeout", t)
          prevData
      }
      debug("resparr=" + Utils.toHexString(r1, ""))
      debug("respstr=" + new String(r1, Charsets.US_ASCII))
      val exists = hex.permutations.exists(bytes => {
        //debug("bytes="+bytes.map(_.mkString("[",",","]")).mkString(","))
        bytes.flatten.sameElements(r1)
      })
      if(exists)
        (r1, true)
      else
        tryReceveRemaininPremuatations(r1, attempts - 1)
    }

    val (data, matc) = tryReceveRemaininPremuatations(Array.empty, hex.size + 1)
    assertTrue(Utils.toHexString(data, "")+" does no match premutations",matc)
  }

  def expect(expect: Byte*) {
    this.expect(expect.toArray)
  }


  private def waitAndReadAvaliable(is: InputStream): Array[Byte] = {
    val fb = is.read()
    val tail = new Array[Byte](is.available())
    is.read(tail)
    val r = Array(fb.toByte) ++ tail
    r
  }

  protected def assertArrayEquals(message: String, expected: Array[Byte], actual: Array[Byte])
  protected def assertTrue(message: String, actual: Boolean)

}

class Talk extends TalkFramework{
  override protected def assertArrayEquals(message: String, expected: Array[Byte], actual: Array[Byte]): Unit = {
    Assert.assertArrayEquals(expected, actual)
  }

  override protected def assertTrue(message: String, actual: Boolean): Unit = {
    Assert.assertTrue(message, actual)
  }
}