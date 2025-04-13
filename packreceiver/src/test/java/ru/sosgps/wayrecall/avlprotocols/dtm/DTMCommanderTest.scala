package ru.sosgps.wayrecall.avlprotocols.dtm

import java.util.Date
import java.util.concurrent.TimeUnit

import org.scalatest.{Matchers, FunSpec}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.{TestContextManager, ContextConfiguration}
import org.springframework.test.context.support.AnnotationConfigContextLoader
import ru.sosgps.wayrecall.avlprotocols.navtelecom.{MessageReceiver, CommanderTestConfig}
import ru.sosgps.wayrecall.scalatestutils.ConnectedFixture
import ru.sosgps.wayrecall.sms.{IOSwitchDeviceCommand, DeviceCommandData}
import scala.collection.JavaConversions.asScalaIterator
import Matchers._
/**
  * Created by nmitropo on 10.3.2016.
  */
@ContextConfiguration(classes = Array(classOf[CommanderTestConfig]))
class DTMCommanderTest extends FunSpec with ConnectedFixture{

  override val serverPort: Int = 50126

  @Autowired
  var jmsTemplate: JmsTemplate = null

  @Autowired
  var mr: MessageReceiver = null

  new TestContextManager(this.getClass).prepareTestInstance(this)


  describe("DTMServerOnCommands"){
    it("should send commands to connected device if any") {
      send("header with imei") as "FF 22 00 03 0F C9 F5 45 0C F3"
      expect("confirmation") as "7B 00 00 7D"

      //useServer.activeConnections("17513449972879524608").command(0x09, 0x00)
      //val commandData = DeviceCommandData("17513449972879524608", new Date(), "", "", "0900")
      val commandData = IOSwitchDeviceCommand("17513449972879524608", new Date(), "", "", None, false)
      jmsTemplate.convertAndSend(commandData)
      expect("command to device") as "7b 02 ff 09 09 00 7d"

      send("gps data") as "5b 01 01 32 00 14 ab cc 56 03 e2 a3 2c 42 04 22 " +
        " 3c 33 42 05 00 3b 66 00 07 5b 61 10 ef 08 63 fa " +
        " 00 03 09 00 f0 c4 54 15 00 00 00 00 21 0a 00 00 " +
        " 00 5b 00 00 00 00 5c 00 00 00 00 86 5d "

      val answer = mr.poll(5, TimeUnit.SECONDS)

      answer should equal(commandData)

      expect("confirmation") as "7B 00 01 7D"

    }
  }



}
