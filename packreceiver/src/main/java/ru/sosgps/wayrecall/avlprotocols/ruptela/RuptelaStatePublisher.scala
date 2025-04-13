package ru.sosgps.wayrecall.avlprotocols.ruptela

import org.springframework.jms.core.{JmsTemplate, MessageCreator}
import javax.jms.{Message, Session}
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.avlprotocols.ruptela.StateMachine.StateEvent
import scala.collection.immutable.HashMap

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 03.11.13
 * Time: 15:00
 * To change this template use File | Settings | File Templates.
 */
class RuptelaStatePublisher extends StateMachine.Listener with grizzled.slf4j.Logging {

  @Autowired
  var jmsTemplate: JmsTemplate = null

  private[this] def publishJMS(data: java.io.Serializable) {
    try {
      jmsTemplate.send("packreceiver.ruptela",new MessageCreator {
        def createMessage(session: Session): Message = {
          val r = session.createObjectMessage(data)
          debug("sending message " + r)
          r
        }
      })
    }
    catch {
      case e: Throwable => error("error in RuptelaStatePublisher retranslation ", e)
    }
  }

  //def apply(v1: String, v2: String) = publishJMS((v1,v2))

  def apply(s: StateEvent) = publishJMS(
    HashMap(
      "name" -> s.name,
      "imei" -> s.imei,
      "time" -> s.time,
      "source" -> s.eventSourceName,
      "dataSourceName" -> s.dataSourceName
    )
  )
}
