package ru.sosgps.wayrecall.avlprotocols.egts

import java.net.Socket

import io.netty.channel.nio.NioEventLoopGroup
import org.junit.{Ignore, Test, BeforeClass}
import ru.sosgps.wayrecall.packreceiver.DummyPackSaver
import ru.sosgps.wayrecall.packreceiver.netty.{EgtsProxyServer, NavTelecomNettyServer}
import ru.sosgps.wayrecall.testutils.TalkTestSetup

/**
 * Created by nickl on 26.01.15.
 */
class ServerTest extends grizzled.slf4j.Logging with TalkTestSetup {
  override val serverPort: Int = ServerTest.EGTS_TEST_PORT



  import talk._

  override val sotimeout: Int = 1000

  @Test
  @Ignore
  def testReject() {

    send("01 00 00 0b 00 1e 00 00 00 01 ef 17 00 00 00 00 01 01 01 14 00 00 00 00 00 02 33 35 34 33 33 30 30 33 30 39 39 32 39 34 38 20 68")
    //Thread.sleep(2*60*1000)
    expect("01 00 00 0b 00 03 00 01 00 00 16 00 00 99 0c de")

  }


//  override protected def defaultSocket(): Socket = {
//    val socket = new Socket("77.243.111.83", 20629)
//    socket.setSoTimeout(sotimeout)
//    socket
//  }

  @Test
  @Ignore
  def testAccept() {

    //talk.set()

    //send("01 00 00 0b 00 1e 00 00 00 01 ef 17 00 00 00 00 01 01 01 14 00 00 00 00 00 02 33 35 34 33 33 30 30 33 30 39 39 32 39 34 38 20 68")
    send("01 00 00 0b 00 1e 00 00 00 01 ef 17 00 00 00 00 " +
      " 01 01 01 14 00 00 00 00 00 02 31 32 31 32 31 32 " +
      " 31 32 31 32 31 32 31 32 31 4d 2f")
    //Thread.sleep(2*60*1000)
    expect("01 00 00 0b 00 03 00 01 00 00 16 00 00 00 9c cc")

    send("01 00 00 0b 00 24 00 01 00 01 84 1d 00 00 00 00 02 02 00 1a 00 57 4b 89 09 7c 25 7a 9e 4a 1f e5 35 81 00 80 22 00 00 00 00 00 00 00 00 00 00 1c cf")

    expect("01 00 00 0b 00 03 00 02 00 00 dc 01 00 80 24 6a")

//    send(" 01 00 00 0b 00 ba 01 01 00 01 66 b3 01 00 00 00 " +
//      " 02 02 10 1a 00 c3 0c 8a 09 b8 22 4a 9e c5 c7 ae " +
//      " 35 91 6e 80 04 00 00 00 00 00 00 00 00 00 00 10 " +
//      " 1a 00 83 0c 8a 09 d0 44 49 9e 5e b7 ae 35 91 fa " +
//      " 00 3e 00 00 00 00 00 00 00 00 00 00 10 1a 00 9e " +
//      " 0c 8a 09 16 06 4a 9e 0d dc ae 35 91 46 80 4e 00 " +
//      " 00 00 00 00 00 00 00 00 00 10 1a 00 8f 0c 8a 09 " +
//      " 65 8c 49 9e 67 f5 ae 35 91 fa 80 64 00 00 00 00 " +
//      " 00 00 00 00 00 00 10 1a 00 8a 0c 8a 09 69 67 49 " +
//      " 9e c1 ec ae 35 91 aa 00 31 00 00 00 00 00 00 00 " +
//      " 00 00 00 10 1a 00 80 0c 8a 09 fc 26 49 9e 7d a2 " +
//      " ae 35 91 c8 80 66 00 00 00 00 00 00 00 00 00 00 " +
//      " 10 1a 00 8d 0c 8a 09 88 74 49 9e b4 f5 ae 35 91 " +
//      " b4 00 22 00 00 00 00 00 00 00 00 00 00 10 1a 00 " +
//      " 78 0d 8a 09 67 14 4a 9e c8 3c ae 35 81 00 80 01 " +
//      " 00 00 00 00 00 00 00 00 00 00 10 1a 00 79 0c 8a " +
//      " 09 fb e2 48 9e bc ab ae 35 91 8c 80 5c 00 00 00 " +
//      " 00 00 00 00 00 00 00 10 1a 00 98 0c 8a 09 74 e9 " +
//      " 49 9e 00 e5 ae 35 91 a0 80 5a 00 00 00 00 00 00 " +
//      " 00 00 00 00 10 1a 00 8e 0c 8a 09 45 7f 49 9e e5 " +
//      " f6 ae 35 91 d2 00 0b 00 00 00 00 00 00 00 00 00 " +
//      " 00 10 1a 00 82 0c 8a 09 d9 3e 49 9e 04 af ae 35 " +
//      " 91 e6 00 2f 00 00 00 00 00 00 00 00 00 00 10 1a " +
//      " 00 72 0c 8a 09 ce bc 48 9e ce b0 ae 35 91 6e 80 " +
//      " 66 00 00 00 00 00 00 00 00 00 00 10 1a 00 c2 0c " +
//      " 8a 09 e9 23 4a 9e a6 cb ae 35 91 3c 80 31 00 00 " +
//      " 00 00 00 00 00 00 00 00 10 1a 00 81 0c 8a 09 56 " +
//      " 2f 49 9e 47 a4 ae 35 91 c8 00 0a 00 00 00 00 00 " +
//      " 00 00 00 00 00 bc 34")
//
//    expect("01 00 00 0b 00 03 00 02 00 00 dc 01 00 80 24 6a")
  }
  
}

object ServerTest extends grizzled.slf4j.Logging {

  val EGTS_TEST_PORT = ru.sosgps.wayrecall.testutils.getFreePort

  var server: EgtsProxyServer = null

  @BeforeClass
  def initserver() {
    server = new EgtsProxyServer
    //server.timer = new HashedWheelTimer()
    //server.store = new DummyPackSaver
    //server.commands = commands
    server.bossPool = new NioEventLoopGroup()
    server.workerPool = server.bossPool


    server.setPort(EGTS_TEST_PORT)
    server.start()
  }

}