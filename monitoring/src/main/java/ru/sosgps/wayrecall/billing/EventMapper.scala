package ru.sosgps.wayrecall.billing

import org.springframework.stereotype.Component
import com.mongodb.BasicDBObject
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.events.{PredefinedEvents, Event, DataEvent, EventsStore}
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct
import org.springframework.jms.core.{MessageCreator, JmsTemplate}
import javax.jms.{Message, Session}
import scala.collection.mutable


class EventMapper extends grizzled.slf4j.Logging {

  @Autowired
  var jmsTemplate: JmsTemplate = null

  @PostConstruct
  def init {
    es.subscribeTyped(PredefinedEvents.objectChange, sendAsJMS)
    es.subscribeTyped(PredefinedEvents.accountChange, sendAsJMS)
  }

  private[this] def sendAsJMS(e: Event) {
      jmsTemplate.send("eventbus.event", new MessageCreator {
        def createMessage(session: Session): Message = {
          val r = session.createObjectMessage(e)
          debug("sending message " + r)
          r
        }
      })
    }

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var es: EventsStore = null

}
