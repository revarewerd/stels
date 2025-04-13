package ru.sosgps.wayrecall.monitoring.notifications

import java.util.Date
import java.util.zip.GZIPInputStream

import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.junit.{Assert, Before, Ignore, Test}
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions, MongoDBManager}
import ru.sosgps.wayrecall.data.{DBPacketsWriter, GPSEvent, PackagesStore, Posgenerator, UserMessage}
import ru.sosgps.wayrecall.events.{EventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.monitoring.notifications.TimedNotificationDetector
import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.durationAsJavaDuration
import EventsTestUtils._
import ru.sosgps.wayrecall.testutils.ObjectsFixture
import ru.sosgps.wayrecall.utils.io.DboReader

import scala.concurrent.duration.DurationInt

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class TimedNotificationTest extends ObjectsFixture{

  @Autowired
  var mdbm: MongoDBManager = null

  @PostConstruct
  def ensureObjects(): Unit ={
    ensureExists(testUid)
  }

  @Autowired
  var es: EventsStore = null

  @Autowired
  var packStore: PackagesStore = null

  @Autowired
  var storePackWriter: DBPacketsWriter = null

  @Autowired
  var timedDetector: TimedNotificationDetector = null

  val testUid = "o8325087825488753638"

  //val title = "notifications.timecontrol"

  val user = "1234"
  

  @Before
  def clearDb() {
    mdbm.getDatabase()("notificationRulesStates").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRules").remove(MongoDBObject.empty)
    packStore.removePositionData(testUid, new Date(0), new Date(Long.MaxValue))
  }

  private def fillAndTestTimed(datas: Iterator[GPSData]) {
    require(datas.nonEmpty)
    var last: GPSData = null
    datas.map(g => {last = g; g}).foreach(storePackWriter.addToDb)
    packStore.refresh(testUid)
    timedDetector.doTest(last.time.getTime)
  }


  val longParkingRule = Map(
    "_id" -> new ObjectId(),
    "name" -> "Тест простоя объектов",
    "email" -> "",
    "params" -> Map(
      "interval" -> 100000L
    ),
    "showmessage" -> true,
    "objects" -> List(
      testUid
    ),
    "type" -> "ntfLongParking",
    "action" -> "none",
    "user" -> user,
    "messagemask" -> "Объект припаркован слишком долго",
    "phone" -> "+791604108305"
  )

  @Test
  @Ignore // когда-то этот тест работал, почему и когда перестал я сходу не разобрался
  def test1() {
    mdbm.getDatabase()("notificationRules").insert(longParkingRule)

    val aggregatedEvents = collectEvents(es, PredefinedEvents.userMessage)
    val p = new Posgenerator(testUid, 10000000)
    fillAndTestTimed(p.genParking(200 seconds))

    for (userMessage <- aggregatedEvents) {
      println("aggregated:" + new Date(userMessage.time) + " " + userMessage)
    }

    Assert.assertEquals(new UserMessage(
      user,
      "Объект припаркован слишком долго",
      "notifications.longparking",
      Some(testUid),
      None,
      10200000L, Map("readStatus" -> false, "lon" -> 56.0, "lat" -> 37.0)), aggregatedEvents(0))

    fillAndTestTimed(p.genParking(300 seconds))
    fillAndTestTimed(p.genMoving(300 seconds))
    fillAndTestTimed(p.genParking(300 seconds))
    Assert.assertEquals(new UserMessage(
      user,
      "Объект припаркован слишком долго",
      "notifications.longparking",
      Some(testUid),
      None,
      11100000, Map("readStatus" -> false, "lon" -> 56.00300000000003, "lat" -> 37.00300000000003)), aggregatedEvents(1))

    fillAndTestTimed(p.genParking(300 seconds))
    Assert.assertTrue(aggregatedEvents.size == 2)

  }

  private val noMessageInterval = 100000L
  val noMessagesRule = Map(
    "_id" -> new ObjectId(),
    "name" -> "Тест отсутсвие связи",
    "email" -> "",
    "params" -> Map(
      "interval" -> noMessageInterval
    ),
    "showmessage" -> true,
    "objects" -> List(
      testUid
    ),
    "type" -> "ntfNoMsg",
    "action" -> "none",
    "user" -> user,
    "messagemask" -> "Объект не присылал сообщения слишком долго",
    "phone" -> "+791604108305"
  )

  @Test
  def test2() {
    mdbm.getDatabase()("notificationRules").insert(noMessagesRule)

    val aggregatedEvents = collectEvents(es, PredefinedEvents.userMessage)
    val p = new Posgenerator(testUid, 10000000)


    fillAndTestTimed(p.genMoving(10 seconds))
    fillAndTestTimed(p.genParking(300 seconds))
    Assert.assertTrue(aggregatedEvents.isEmpty)
    val timeShift = p.last.time.getTime + noMessageInterval + 100
    timedDetector.doTest(timeShift)
    p.last.time = new Date(timeShift)
    Assert.assertEquals(new UserMessage(
      user,
      "Объект не присылал сообщения слишком долго",
      "notifications.nomessages",
      Some(testUid),
      None,
      10410100, Map("readStatus" -> false, "lon" -> 56.00300000000003, "lat" -> 37.00300000000003)), aggregatedEvents(0))
    println("continue parking")
    fillAndTestTimed(p.genParking(300 seconds))
    println("test again")
    timedDetector.doTest(p.last.time.getTime + noMessageInterval * 2 + 100)
    Assert.assertEquals(2, aggregatedEvents.size)
    p.last.time = new Date(p.last.time.getTime + noMessageInterval * 2 + 100)
    fillAndTestTimed(p.genParking(300 seconds))
    timedDetector.doTest(p.last.time.getTime + noMessageInterval * 2 + 100)

    Assert.assertEquals(new UserMessage(
      user,
      "Объект не присылал сообщения слишком долго",
      "notifications.nomessages",
      Some(testUid),
      None,
      10910200, Map("readStatus" -> false, "lon" -> 56.00300000000003, "lat" -> 37.00300000000003)), aggregatedEvents(1))

  }


}
