package ru.sosgps.wayrecall.monitoring

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.{MessageCreator, JmsTemplate}
import javax.annotation.PostConstruct
import ru.sosgps.wayrecall.events._
import javax.jms.{ObjectMessage, Message}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 23.05.13
 * Time: 16:46
 * To change this template use File | Settings | File Templates.
 */
class EventMapper extends grizzled.slf4j.Logging with javax.jms.MessageListener {

  @Autowired
  var es: EventsStore = null

  def onMessage(p1: Message) {

    p1 match {
      case p1: ObjectMessage => p1.getObject match {
        case event: Event => {
          es.publish(event);
        }
        case o: Any => warn("unmatched object " + o)
      }
      case p1: Any => warn("unmatched message " + p1)
    }

  }
}



