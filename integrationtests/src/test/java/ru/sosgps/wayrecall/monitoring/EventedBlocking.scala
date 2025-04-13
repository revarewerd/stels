package ru.sosgps.wayrecall.monitoring

import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.junit.{Assert, Before, Test}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.intercept.RunAsUserToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.sosgps.wayrecall.core.{GPSDataConversions, GPSData, MongoDBManager}
import ru.sosgps.wayrecall.data.GPSEvent
import ru.sosgps.wayrecall.events.{Event, EventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.monitoring.notifications.GpsNotificationDetector
import ru.sosgps.wayrecall.monitoring.web.EventedObjectCommander
import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.EventsTestUtils.collectEventsQueue
import ru.sosgps.wayrecall.utils.io.DboReader


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class EventedBlocking extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null


  @Autowired
  var es: EventsStore = null

  @Autowired
  var eventedCommander: EventedObjectCommander = null

  @Autowired
  var nt: GpsNotificationDetector = null

  val testUid = "o83971426548810109"

  val user = "1234"

  @Test
  def testByTime() {

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage, (_: Event).targetId == user)

    SecurityContextHolder.getContext.setAuthentication(new RunAsUserToken(user, user, null, null, null))
    //TODO: fixme: could fail when sms returned in 20 mills
    eventedCommander.sendBlockCommandAtDate(testUid, true, "fff", new DateTime().plusMillis(300).getMillis)

    println("aggregatedEvents=" + aggregatedEvents)
    val e = aggregatedEvents.poll(5, TimeUnit.SECONDS)

    Assert.assertEquals(user, e.targetId)
    Assert.assertEquals("objectscommander.objectblocked", e.message)

  }

  @Before
  def clearConversations {
    nt.invalidateCache()
    mdbm.getDatabase()("smsconversation").remove(MongoDBObject.empty)
  }

  private def shiftTime(gps: GPSData, mills: Long) = {
    gps.time = new Date(gps.time.getTime + mills)
    gps.insertTime = new Date(gps.insertTime.getTime + mills)
    gps
  }

  @Test
  def testBySpeed() {

    val now = System.currentTimeMillis()

    val gpsDatas = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o83971426548810109.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq

    val tshift = gpsDatas.head.insertTime.getTime - now

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage, (_: Event).targetId == user)

    Assert.assertEquals(0, aggregatedEvents.size());

    SecurityContextHolder.getContext.setAuthentication(new RunAsUserToken(user, user, null, null, null))
    eventedCommander.sendBlockAfterStop(testUid, true, "fff")
    for (data <- gpsDatas.sortBy(_.time)) {
      es.publish(new GPSEvent(shiftTime(data, tshift)))
    }


    //println("aggregatedEvents=" + aggregatedEvents)
    val e = aggregatedEvents.poll(30, TimeUnit.SECONDS)

    Assert.assertEquals(user, e.targetId)
    Assert.assertEquals("objectscommander.objectblocked", e.message)
    debug("aggregatedEvents:" + aggregatedEvents)
    Assert.assertEquals(0, aggregatedEvents.size)

  }

  @Test
  def testByIgnition() {

    val now = System.currentTimeMillis()

    val gpsDatas = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o83971426548810109.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq

    val tshift = gpsDatas.head.insertTime.getTime - now

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)

    Assert.assertEquals(0, aggregatedEvents.size());

    SecurityContextHolder.getContext.setAuthentication(new RunAsUserToken(user, user, null, null, null))
    eventedCommander.sendBlockAfterIgnition(testUid, true, "fff")
    for (data <- gpsDatas.sortBy(_.time)) {
      es.publish(new GPSEvent(shiftTime(data, tshift)))
    }

    //println("aggregatedEvents=" + aggregatedEvents)
    val e = aggregatedEvents.poll(1, TimeUnit.SECONDS)

    Assert.assertEquals(user, e.targetId)
    Assert.assertEquals("objectscommander.objectblocked", e.message)
    Assert.assertTrue(aggregatedEvents.isEmpty)

  }


}
