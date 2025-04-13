package ru.sosgps.wayrecall.monitoring.notifications

import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions, MongoDBManager}
import ru.sosgps.wayrecall.events.{EventsStore, PredefinedEvents}
import org.bson.types.ObjectId
import org.junit.{Assert, Before, Test}
import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.io.DboReader
import ru.sosgps.wayrecall.utils.durationAsJavaDuration
import java.util.zip.GZIPInputStream

import com.mongodb.casbah.Imports._

import scala.Some
import EventsTestUtils._

import scala.Some
import ru.sosgps.wayrecall.data.{GPSEvent, Posgenerator, UserMessage}
import java.util.Date

import ru.sosgps.wayrecall.testutils.ObjectsFixture

import scala.collection.immutable
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class SpeedNotificationTest extends ObjectsFixture {
  @Autowired
  var mdbm: MongoDBManager = null


  @Autowired
  var es: EventsStore = null

  @Autowired
  var nt: GpsNotificationDetector = null

  val testUids = IndexedSeq(
    "o2375087825488753634",
    "o2375087825488753635",
    "o4101211322152391053",
    "o1179166601455194467",
    "o4490271822845429693"
  )


  @PostConstruct
  def ensureObjects(): Unit = {
    testUids.foreach(ensureExists)
  }

  val title = "notifications.speedcontrol"

  val user = "1234"

  val rule = Map(
    "_id" -> new ObjectId(),
    "name" -> "Тест перемещения спящих госейв",
    "email" -> "",
    "params" -> Map(
      "ntfVeloMax" -> "115"
    ),
    "showmessage" -> true,
    "objects" -> testUids,
    "type" -> "ntfVelo",
    "action" -> "none",
    "user" -> user,
    "messagemask" -> "Объект превысил максимальную скорость, движется со скоростью %VALUE% км/ч."//,
    //"phone" -> "+791604108305"
  )

  @Before
  def prepareDb(): Unit = {
    nt.invalidateCache()
    mdbm.getDatabase()("notificationRulesStates").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRules").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRules").insert(rule)
  }

  @Test
  def test1() {

    val aggregatedEventsAfter = gpsToEvents("/objPacks.o2375087825488753634.bson.gz")

    Assert.assertEquals(new UserMessage(
      user,
      "Объект превысил максимальную скорость, движется со скоростью 116 км/ч.",
      title,
      Some(testUids(0)),
      None,
      1396946363000L, Map("readStatus" -> false, "lon" -> 37.8486183, "lat" -> 55.48892)), aggregatedEventsAfter(0))

    Assert.assertEquals(new UserMessage(
      user,
      "Объект превысил максимальную скорость, движется со скоростью 116 км/ч.",
      title,
      Some(testUids(0)),
      None,
      1396946742000L, Map("readStatus" -> false, "lon" -> 37.7642966, "lat" -> 55.571185)), aggregatedEventsAfter(1))

    Assert.assertEquals(2, aggregatedEventsAfter.size)


  }

  private def gpsToEvents(file: String, first: Duration = 1 second, others: Duration = 1 second): Stream[UserMessage] = {
    val gpsDatas = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream(file)))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)

    for (data <- gpsDatas.sortBy(_.insertTime)) {
      //println(data)
      es.publish(new GPSEvent(data))
    }



    val aggregatedEventsAfter = aggregatedEvents.queueToStream(first, others)

//    for (userMessage <- aggregatedEventsAfter) {
//      println(new Date(userMessage.time) + " " + userMessage)
//    }
    aggregatedEventsAfter
  }

  @Test
  def testSyntetic() {

    val pg = new Posgenerator(testUids(1), 1000000L)
    val gpsDatas = pg.genMoving(10 seconds) ++
      Iterator(
        pg.upd(i => {
          val clone = i.clone()
          clone.speed = 127;
          clone
        }) /*,
        pg.upd(i => {i.speed = 60;i})*/
      ) ++ pg.genMoving(10 seconds) ++ Iterator.from(0).map(i => pg.upd(g => new GPSData(
      g.uid,
      g.imei,
      g.lat + 0.0003,
      g.lon + 0.0003,
      new Date(g.time.getTime + 1000),
      (20 + i * 10).toShort,
      90,
      3
    ))).take(15)

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)


    for (data <- gpsDatas) {
      es.publish(new GPSEvent(data))
    }

    val userMessage = aggregatedEvents.poll(10, TimeUnit.SECONDS)

    Assert.assertEquals(new UserMessage(
      user,
      "Объект превысил максимальную скорость, движется со скоростью 120 км/ч.",
      title,
      Some(testUids(1)),
      None,
      1031000L, Map("readStatus" -> false, "lon" -> 37.00930000000009, "lat" -> 56.00930000000009)), userMessage)

  }


  @Test
  def test2() {
    val aggregatedEventsAfter = gpsToEvents("/speedTestsData/o4101211322152391053-speedMistake.bson.gz", 300 millis)
    Assert.assertEquals(0, aggregatedEventsAfter.size)
  }

  @Test
  def test4() {
    val aggregatedEventsAfter = gpsToEvents("/speedTestsData/o4490271822845429693-1-time-over-120-mistake.bson.gz", 300 millis)
    Assert.assertEquals(0, aggregatedEventsAfter.size)
  }


  @Test
  def test3() {

    val aggregatedEventsAfter = gpsToEvents("/speedTestsData/o1179166601455194467-3-times-over-120-in-15-minutes.bson.gz", 10 seconds)

    Assert.assertEquals(new UserMessage(
      user,
      "Объект превысил максимальную скорость, движется со скоростью 119 км/ч.",
      title,
      Some(testUids(3)),
      None,
      1424642658621L, Map("readStatus" -> false, "lon" -> 37.3694336, "lat" -> 55.7485248)), aggregatedEventsAfter(0))

    Assert.assertEquals(new UserMessage(
      user,
      "Объект превысил максимальную скорость, движется со скоростью 121 км/ч.",
      title,
      Some(testUids(3)),
      None,
      1424643246461L, Map("readStatus" -> false, "lon" -> 37.3950912, "lat" -> 55.8303552)), aggregatedEventsAfter(1))

    Assert.assertEquals(2, aggregatedEventsAfter.size)


  }

}
