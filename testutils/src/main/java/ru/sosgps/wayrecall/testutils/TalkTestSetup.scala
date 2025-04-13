package ru.sosgps.wayrecall.testutils

import java.net.Socket

import org.junit.{After, Before}

/**
 * Created by nickl on 21.11.14.
 */
trait TalkTestSetup {

  var socket: Socket = null
  val talk: Talk = new Talk

  @Before
  def connect(): Unit = {
    socket = defaultSocket()
    talk.set(socket)
  }

  @After
  def disconnect(): Unit = {
    socket.close();
  }

  val serverPort: Int

  val sotimeout = 1000

  protected def defaultSocket(): Socket = {
    val socket = new Socket("localhost", serverPort)
    socket.setSoTimeout(sotimeout)
    socket
  }
}
