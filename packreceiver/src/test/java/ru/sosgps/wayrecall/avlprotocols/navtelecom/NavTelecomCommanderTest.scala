package ru.sosgps.wayrecall.avlprotocols.navtelecom

import java.util.Date
import java.util.concurrent.{TimeUnit, LinkedBlockingQueue}
import javax.jms.{Message, Session}

import io.netty.channel.nio.NioEventLoopGroup
import org.junit.runner.RunWith
import org.junit.{Ignore, Assert, Test, BeforeClass}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.{MessageCreator, JmsTemplate}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.sosgps.wayrecall.packreceiver.DummyPackSaver
import ru.sosgps.wayrecall.packreceiver.netty.NavTelecomNettyServer
import ru.sosgps.wayrecall.sms.{IOSwitchDeviceCommand, DeviceCommandsBatch, DeviceCommandData}
import ru.sosgps.wayrecall.testutils.TalkTestSetup
import scala.collection.JavaConversions.asScalaIterator

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[CommanderTestConfig]))
class NavTelecomCommanderTest extends TalkTestSetup with grizzled.slf4j.Logging {

  val serverPort: Int = 50125

  @Autowired
  var jmsTemplate: JmsTemplate = null

  @Autowired
  var mr: MessageReceiver = null

  import talk._

  @Test
  def testJms(): Unit = {
    //    jmsTemplate.send(new MessageCreator {
    //      override def createMessage(session: Session): Message = session.createTextMessage("ttt")
    //    })
    send("40 4e 54 43 00 00 00 00 50 31 27 03 13 00 4c 03 2a 3e 53 3a 38 36 38 32 30 34 30 30 34 32 31 31 32 36 33")
    expect("40 4e 54 43 50 31 27 03 00 00 00 00 03 00 45 1a 2a 3c 53 ")

    val commandData = DeviceCommandData("868204004211263", new Date(), "", "21s21", "!2Y")
    jmsTemplate.convertAndSend(commandData)
    debug("message sent")
    Assert.assertTrue(mr.isEmpty)
//    expect("40 4e 54 43 50 31 27 03 00 00 00 00 0c 00 4c 1c 2a 3e 50 41 53 53 3a 32 31 73 32 31")
//    send("40 4e 54 43 00 00 00 00 50 31 27 03 06 00 1a 40 2a 21 50 41 53 53")


    expect("40 4e 54 43 50 31 27 03 00 00 00 00 04 00 60 38 2a 21 32 59")
    send("40 4e 54 43 00 00 00 00 50 31 27 03 27 00 5c 27 2a 40 43 80 10 00 00 20 a1 ee f2 64 54 26 61 fe 01 7c c9 59 01 33 06 00 00 00 f2 64 54 0e 43 ee 00 00 00 1b 00 7b 29 ")
    val answers = mr.poll(5, TimeUnit.SECONDS)
    debug("responsequeue:" + mr.toStringQueue)
    debug("answers:" + answers)
    Assert.assertEquals(commandData,answers)
    expect("40 4e 54 43 50 31 27 03 00 00 00 00 03 00 84 db 7e 43 b9")

  }

  @Test
  def testJms2(): Unit = {
    //    jmsTemplate.send(new MessageCreator {
    //      override def createMessage(session: Session): Message = session.createTextMessage("ttt")
    //    })
    send("40 4e 54 43 00 00 00 00 50 31 27 03 13 00 4c 03 2a 3e 53 3a 38 36 38 32 30 34 30 30 34 32 31 31 32 36 33")
    expect("40 4e 54 43 50 31 27 03 00 00 00 00 03 00 45 1a 2a 3c 53 ")

    val commandData = DeviceCommandsBatch("868204004211263", (1 to 2).map(i => IOSwitchDeviceCommand("868204004211263", new Date(), "", "21s21", Some(i), true)).toList)
    jmsTemplate.convertAndSend(commandData)
    debug("message sent")
    Assert.assertTrue(mr.isEmpty)
//    expect("40 4e 54 43 50 31 27 03 00 00 00 00 0c 00 4c 1c 2a 3e 50 41 53 53 3a 32 31 73 32 31")
//    send("40 4e 54 43 00 00 00 00 50 31 27 03 06 00 1a 40 2a 21 50 41 53 53")


    expect("40 4e 54 43 50 31 27 03 00 00 00 00 04 00 63 3b 2a 21 31 59")
   // expect("40 4e 54 43 50 31 27 03 00 00 00 00 04 00 60 38 2a 21 32 59")
    send("40 4e 54 43 00 00 00 00 50 31 27 03 27 00 5c 27 2a 40 43 80 10 00 00 20 a1 ee f2 64 54 26 61 fe 01 7c c9 59 01 33 06 00 00 00 f2 64 54 0e 43 ee 00 00 00 1b 00 7b 29 ")
    val answers = mr.poll(5, TimeUnit.SECONDS)
    debug("responsequeue:" + mr.toStringQueue)
    debug("answers:" + answers)
    Assert.assertEquals(commandData,answers)
    expectAnyOrderS("40 4e 54 43 50 31 27 03 00 00 00 00 04 00 60 38 2a 21 32 59",
      " 40 4e 54 43 50 31 27 03 00 00 00 00 03 00 84 db 7e 43 b9")

  }



}

class MessageReceiver extends grizzled.slf4j.Logging{
  private val responsequeue = new LinkedBlockingQueue[Any]()

  def poll(long: Long, timeUnit: TimeUnit) = {
    debug("polling")
    val r = responsequeue.poll(long, timeUnit)
    debug(s"polling got result $r")
    r
  }

  def receive(a: Any): Unit = {
    debug("received:" + a)
    responsequeue.put(a)
    debug("responsequeue:" + toStringQueue)
    debug("responsequeue:" + toStringQueue)
  }

  def isEmpty = responsequeue.isEmpty

  def toStringQueue: String = {
    responsequeue.iterator().mkString("[", ",", "]")
  }
}
