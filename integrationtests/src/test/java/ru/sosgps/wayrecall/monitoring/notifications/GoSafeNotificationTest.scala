package ru.sosgps.wayrecall.monitoring.notifications

import javax.annotation.PostConstruct

import org.junit.{Assert, Before, Test}
import java.util.zip.GZIPInputStream

import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.io.{DboReader, Utils}
import ru.sosgps.wayrecall.core.{GPSDataConversions, MongoDBManager}
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.events.{EventsStore, PredefinedEvents}
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import ru.sosgps.wayrecall.data.{GPSEvent, UserMessage}
import EventsTestUtils._
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import java.util.Date

import ru.sosgps.wayrecall.testutils.ObjectsFixture

import scala.collection.JavaConversions.iterableAsScalaIterable

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class GoSafePowerExtNotificationTest extends ObjectsFixture{

  @Autowired
  var mdbm: MongoDBManager = null

  @PostConstruct
  def ensureObjects(): Unit ={
    ensureExists(testUid)
  }
  
  @Autowired
  var nd: GpsNotificationDetector = null


  @Autowired
  var es: EventsStore = null

  val testUid = "o2238954995807954548"

  val title = "notifications.gosafesleeperscontrol"

  val powerMess = "Отключение внешнего питания"

  val user = "1234"

  val rule = Map(
    "_id" -> new ObjectId(),
    "name" -> "Тест потери питания спящих госейв",
    "email" -> "",
    "params" -> Map(
      "ntfSlprExtPower" -> true
    ),
    "showmessage" -> true,
    "objects" -> List(
      testUid
    ),
    "type" -> "ntfSlpr",
    "user" -> user,
    "messagemask" -> "Отключение внешнего питания",
    "phone" -> "+791604108305"
  )

  @Test
  def test1(){

    val gpsDatas = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o2238954995807954548.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq

    mdbm.getDatabase()("notificationRulesStates").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRules").remove(MongoDBObject.empty)
    nd.invalidateCache
    mdbm.getDatabase()("notificationRules").insert(rule)

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)
    
    Assert.assertEquals(0, aggregatedEvents.size);

    for (data <- gpsDatas.sortBy(_.time)) {
//      println(data.time + " GoSafe PowerExt Message = " + data.data)
      es.publish(new GPSEvent(data))
//      println("aggregatedEvents="+aggregatedEvents)
      
    }

    EventsTestUtils.waitUntil(aggregatedEvents.size >= 4, 5000)

    val aggregatedEventsAfter = aggregatedEvents.toIndexedSeq
    nd.invalidateCache()
    for (userMessage <- aggregatedEventsAfter) {
      println(new Date(userMessage.time) + " " + userMessage)
    }
    
    Assert.assertEquals(new UserMessage(
      user,
      powerMess,
      title,
      Some(testUid),
      None,
      1391428552000L, Map("readStatus" -> false, "lon" -> 37.776495, "lat" -> 55.748712)), aggregatedEventsAfter(0))

    Assert.assertEquals(new UserMessage(
      user,
      powerMess,
      title,
      Some(testUid),
      None,
      1391432092000L, Map("readStatus" -> false, "lon" -> 37.775331, "lat" -> 55.748631)), aggregatedEventsAfter(1))

    Assert.assertEquals(4, aggregatedEventsAfter.size)


  }

}


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class GoSafeMovementNotificationTest extends ObjectsFixture{

  @Autowired
  var mdbm: MongoDBManager = null

  @PostConstruct
  def ensureObjects(): Unit ={
    ensureExists(testUid)
    ensureExists("o2238954995807954548")
  }

  @Autowired
  var nd: GpsNotificationDetector = null

  @Autowired
  var es: EventsStore = null

  val testUid = "o2238954995807954548"

  val title = "notifications.gosafesleeperscontrol"

  val powerMess = "Зарегистрировано перемещение"

  val user = "1234"

  val rule = Map(
    "_id" -> new ObjectId(),
    "name" -> "Тест перемещения спящих госейв",
    "email" -> "",
    "params" -> Map(
      "ntfSlprMovement" -> true
    ),
    "showmessage" -> true,
    "objects" -> List(
      testUid
    ),
    "type" -> "ntfSlpr",
    "user" -> user,
    "messagemask" -> "Зарегистрировано перемещение",
    "phone" -> "+791604108305"
  )

  @Test
  def test1(){

    val gpsDatas = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o2238954995807954548.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq

    mdbm.getDatabase()("notificationRulesStates").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRules").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRules").insert(rule)

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)

    for (data <- gpsDatas.sortBy(_.time)) {
//      println(data.time + " GoSafe Movement Message = " + data.data)
      es.publish(new GPSEvent(data))
    }

    EventsTestUtils.waitUntil(aggregatedEvents.size >= 13, 5000)

    val aggregatedEventsAfter = aggregatedEvents.toIndexedSeq
    nd.invalidateCache()
    for (userMessage <- aggregatedEventsAfter) {
      println(new Date(userMessage.time) + " " + userMessage)
    }


    Assert.assertEquals(new UserMessage(
      user,
      powerMess,
      title,
      Some(testUid),
      None,
      1391427397000L, Map("readStatus" -> false, "lon" -> 37.775883, "lat" -> 55.74809)), aggregatedEventsAfter(0))

    Assert.assertEquals(new UserMessage(
      user,
      powerMess,
      title,
      Some(testUid),
      None,
      1391428811000L, Map("readStatus" -> false, "lon" -> 37.779602, "lat" -> 55.748286)), aggregatedEventsAfter(1))

    Assert.assertEquals(13, aggregatedEventsAfter.size)


  }

}