package ru.sosgps.wayrecall.data

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.events._
import ru.sosgps.wayrecall.events.EventTopic
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import javax.jms.{ObjectMessage, Message}


@SerialVersionUID(1L)
class GPSEvent(val gpsData: GPSData) extends AbstractEvent(PredefinedEvents.objectGpsEvent) {
  val targetId = gpsData.uid
}

class NewGpsEventMapper extends grizzled.slf4j.Logging with javax.jms.MessageListener {

  @Autowired
  var es: EventsStore = null

  def onMessage(p1: Message) {
    p1 match {
      case m: ObjectMessage => {
        m.getObject match {
          case gpsdata: GPSData => {
            trace("received gpsdata " + gpsdata)
            es.publish(new GPSEvent(gpsdata))
          }
          case e: Any => warn("received unrecognized ObjectMessage with object" + e)
        }

      }
      case _ => warn("received unrecognized message")
    }
  }
}
