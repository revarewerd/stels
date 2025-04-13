package ru.sosgps.wayrecall.avlprotocols.common

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import ru.sosgps.wayrecall.sms.{DeviceCommand, DeviceCommandException}

import scala.concurrent.{Future, blocking}

/**
  * Created by nickl-mac on 27.02.16.
  */
trait DeviceCommanderJMS extends grizzled.slf4j.Logging {

  @Autowired
  var jmsTemplate: JmsTemplate = null

  protected def notifyFailure(t: DeviceCommandException) = {
    debug("notify failure command:", t)
    blocking {
      jmsTemplate.convertAndSend("receiver.networkcommand.result", t)
    }
  }

  protected def notifySuccess(d: DeviceCommand) = {
    debug("notify successful command:" + d, new RuntimeException("staktrace"))
    blocking {
      jmsTemplate.convertAndSend("receiver.networkcommand.result", d)
    }
    debug(s"jms sent for $d")
  }



}

trait DeviceCommander{
  def receiveCommand(d: DeviceCommand): Future[DeviceCommand]

  def connectedTo(imei: String): Boolean
}