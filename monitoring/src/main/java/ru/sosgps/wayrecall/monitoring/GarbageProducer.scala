package ru.sosgps.wayrecall.monitoring

import scala.collection.mutable.ListBuffer
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.events.EventsStore
import ru.sosgps.wayrecall.data.UserMessage

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 04.05.13
 * Time: 2:05
 * To change this template use File | Settings | File Templates.
 */
class GarbageProducer extends grizzled.slf4j.Logging {

  @Autowired
  var es: EventsStore = null

  def fillMem(count: Int, interval: Int) = {

    def freebytes = Runtime.getRuntime.freeMemory()

    debug("fill start freebytes = " + freebytes)
    debug("count = " + count)
    val bytes = new ListBuffer[Array[Byte]]

    //while (freebytes > 1024*1024*64)
    for (i <- 0 to count) {
      Thread.sleep(Option(interval).map(_.longValue()).getOrElse(100L))
      bytes += Array.ofDim[Byte](1024 * 1024 * 1)
      //debug("freebytes = " + freebytes)
    }

    debug("fill finish freebytes = " + freebytes)

  }

  def publishNotification(user: String, text: String): Unit = publishNotification(user: String, text: String, null: String)

  def publishNotification(user: String, text: String, obj: String): Unit = {
    es.publish(new UserMessage(user, text, "Уведомление", Option(obj)))
  }

}
