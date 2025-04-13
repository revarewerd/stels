package ru.sosgps.wayrecall.monitoring

import java.util.Objects
import java.util.Objects.requireNonNull
import java.util.concurrent.TimeUnit

import com.mongodb.casbah.commons.MongoDBObject
import org.junit._
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.intercept.RunAsUserToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.events.{Event, EventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.monitoring.web.{MockSmsGate, ObjectsCommander}
import ru.sosgps.wayrecall.sms.MockGprsCommandProcessor
import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.EventsTestUtils.collectEventsQueue

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class ObjectCommanderTest {

  @Autowired
  var es: EventsStore = null

  @Autowired
  var obj: ObjectsCommander = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var mockGPRS: MockGprsCommandProcessor = null

  val user = "1234"

  @Autowired
  var gate: MockSmsGate = null

  @Before
  def cleanup(): Unit = {
    mdbm.getDatabase()("smsconversation").remove(MongoDBObject.empty)
  }

  @After
  def after(): Unit = {
    EventsTestUtils.waitUntil(gate.scheduled == 0, 5000, 50)
    //    if( gate.scheduled.get() > 0)
    //      Thread.sleep(200)
    Assert.assertEquals(0, gate.scheduled)
  }

  @Test
  def test(): Unit = {

    val testUid = "o83971426548810109"

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage, (_: Event).targetId == user)

    SecurityContextHolder.getContext.setAuthentication(new RunAsUserToken(user, user, null, null, null))
    obj.sendBlockCommand(testUid, true, "21s21")

    val e = aggregatedEvents.poll(1, TimeUnit.SECONDS)

    Assert.assertEquals(user, e.targetId)
    Assert.assertEquals("objectscommander.objectblocked", e.message)

  }

  @Test
  def testSignalSms(): Unit = {

    val testUid = "o1493975117851847450"

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage, (_: Event).targetId == user)

    mockGPRS.answerTimeOut = -1;

    SecurityContextHolder.getContext.setAuthentication(new RunAsUserToken(user, user, null, null, null))
    obj.sendBlockCommand(testUid, true, "does not matter")
    //obj.sendBlockCommand(testUid,true,"21s21")

    //Thread.sleep(1200)

    //println("aggregatedEvents=" + aggregatedEvents)
    val e = requireNonNull(aggregatedEvents.poll(3, TimeUnit.SECONDS), "event not registred")
    Assert.assertEquals(user, e.targetId)
    Assert.assertEquals("objectscommander.objectblocked", e.message)

  }

  @Test
  @Ignore
  def testSignalGPRS(): Unit = {

    val testUid = "o1493975117851847450"

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)
    mockGPRS.answerTimeOut = 20;
    SecurityContextHolder.getContext.setAuthentication(new RunAsUserToken(user, user, null, null, null))
    mockGPRS.sentCount.set(0)
    obj.sendBlockCommand(testUid, true, "21s21")

    println("aggregatedEvents=" + aggregatedEvents)
    val e = aggregatedEvents.poll(1, TimeUnit.SECONDS)

    Assert.assertTrue("command should have been sent over GPRS", 1 == mockGPRS.sentCount.get())
    Assert.assertEquals(user, e.targetId)
    Assert.assertEquals("objectscommander.objectblocked", e.message)

  }


}
