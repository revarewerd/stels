package ru.sosgps.wayrecall.scalatestutils

import java.net.Socket

import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatest.Matchers._
import org.scalatest.Matchers
import BinaryMatchers._
import ru.sosgps.wayrecall.testutils.{Talk, TalkFramework}



trait ConnectedFixture extends BeforeAndAfterEach with grizzled.slf4j.Logging{
  this: Suite =>

  var socket: Socket = null
  private val talk: TalkFramework = new TalkFramework {

    override protected def assertArrayEquals(message: String, expected: Array[Byte], actual: Array[Byte]): Unit = {
      withClue(message)  {actual should matchInHex(expected)}
    }

    override protected def assertTrue(message: String, actual: Boolean): Unit = {
      Matchers.assert(actual, message)
    }
  }

  val sotimeout = 1000

  override def beforeEach() {
    debug("before each")
    socket = defaultSocket()
    talk.set(socket)
    super.beforeEach()
  }

  val serverPort = ru.sosgps.wayrecall.testutils.getFreePort

  protected def defaultSocket(): Socket = {
    val socket = new Socket("localhost", serverPort)
    socket.setSoTimeout(sotimeout)
    socket
  }

  override def afterEach() {
    try {
      super.afterEach() // To be stackable, must call super.afterEach
      socket.close()
      socket = null
    }
    finally {

      debug("after ")
    }
  }

  def send(description: String) = new {
    def as(data: String) = talk.send(data)

    def as(data: Array[Byte]) = talk.send(data)
  }

  def expect(description: String) = new {

    def as(data: String) = talk.expect(data)

    def as(data: Array[Byte]) = talk.expect(data)
  }
}