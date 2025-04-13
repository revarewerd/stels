package ru.sosgps.wayrecall.monitoring

import java.util.concurrent.TimeUnit

import com.mongodb.casbah.commons.MongoDBObject
import org.junit.{After, Assert, Before, Test}
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.events.{EventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.monitoring.web.{FMBDigioutCommand, MockSmsGate, TeltonikaDigioutCommand}
import ru.sosgps.wayrecall.sms.{SMSCommandProcessor, SmsConversation}
import ru.sosgps.wayrecall.utils.EventsTestUtils

import scala.collection.JavaConversions.asScalaIterator


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array("/spring-notification-test.xml"))
class SMSCommandProcessorTest extends grizzled.slf4j.Logging {

  @Autowired
  var es: EventsStore = null

  @Autowired
  var cm: SMSCommandProcessor = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var gate: MockSmsGate = null

  @Before
  def preclean(): Unit = {
    mdbm.getDatabase()("smsconversation").remove(MongoDBObject.empty)
    Assert.assertEquals(0, gate.scheduled)
  }

  @After
  def after(): Unit = {
    EventsTestUtils.waitUntil(gate.scheduled == 0, 5000, 50)
    //    if( gate.scheduled.get() > 0)
    //      Thread.sleep(200)
    Assert.assertEquals(0, gate.scheduled)
  }

  @Test
  def testSingle(): Unit = {
    val eventsQueue = EventsTestUtils.collectEventsQueue(es, PredefinedEvents.sms)
    cm.sendCommand("+79154885631", new TeltonikaDigioutCommand("u", "l", "p", true))
    gate.awaitCompletion()
    val events = (Iterator(eventsQueue.poll(1, TimeUnit.SECONDS)) ++ eventsQueue.iterator()).toList
    debug(events.map(_.sms.text).mkString("\n"))

    Assert.assertEquals("Digital Outputs are set to: 11", events.head.sms.text)
    Assert.assertFalse(events.size > 1)
    cm.withConversation("+79154885631", con => {
      Assert.assertTrue(con.allPending.isEmpty)
    })

  }


  @Test
  def testSingleFMB(): Unit = {
    val eventsQueue = EventsTestUtils.collectEventsQueue(es, PredefinedEvents.sms)
    cm.sendCommand("FMB-phone", new FMBDigioutCommand("u", "l", "p", true))
    gate.awaitCompletion()
    val events = (Iterator(eventsQueue.poll(1, TimeUnit.SECONDS)) ++ eventsQueue.iterator()).toList
    debug(events.map(_.sms.text).mkString("\n"))

    Assert.assertEquals("DOUT1:1 Timeout:INFINITY DOUT2:1 Timeout:INFINITY ", events.head.sms.text)
    Assert.assertFalse(events.size > 1)
    cm.withConversation("FMB-phone", con => {
      Assert.assertTrue(con.allPending.isEmpty)
    })
  }

  @Test
  def testBatch(): Unit = {
    val eventsQueue = EventsTestUtils.collectEventsQueue(es, PredefinedEvents.sms)

    cm.sendCommand(new SmsConversation("+79154885631", Seq(
      new TeltonikaDigioutCommand("u", "l", "p", true),
      new TeltonikaDigioutCommand("u", "l", "p", false),
      new TeltonikaDigioutCommand("u", "l", "p", true)
    )))

    gate.awaitCompletion()
    val events = (Iterator.fill(3)(eventsQueue.poll(3, TimeUnit.SECONDS)) ++ eventsQueue.iterator()).toList
    debug(events.map(_.sms.text).mkString("\n"))

    val event = events.iterator
    Assert.assertEquals("Digital Outputs are set to: 11", event.next().sms.text)
    Assert.assertEquals("Digital Outputs are set to: 00", event.next().sms.text)
    Assert.assertEquals("Digital Outputs are set to: 11", event.next().sms.text)
    Assert.assertFalse(event.hasNext)

  }

  @Test
  def testAlreadyExists(): Unit = {
    val eventsQueue = EventsTestUtils.collectEventsQueue(es, PredefinedEvents.sms)

    try {
      cm.sendCommand("+79154885631", new TeltonikaDigioutCommand("u", "l", "p", true))
      cm.sendCommand("+79154885631", new TeltonikaDigioutCommand("u", "l", "p", true))
      Assert.fail("dublicates sent")
    }
    catch {
      case e: ru.sosgps.wayrecall.sms.AlreadySentException =>
        debug("AlreadySentException caught")
    }

    cm.sendCommand("+79154885631", new TeltonikaDigioutCommand("u", "l", "p", false))
    gate.awaitCompletion()
    val events = (Iterator.fill(2)(eventsQueue.poll(3, TimeUnit.SECONDS)) ++ eventsQueue.iterator()).toList
    debug(events.map(_.sms.text).mkString("\n"))

    val event = events.iterator
    Assert.assertEquals("Digital Outputs are set to: 11", event.next().sms.text)
    Assert.assertEquals("Digital Outputs are set to: 00", event.next().sms.text)
    Assert.assertFalse(event.hasNext)
  }

}
