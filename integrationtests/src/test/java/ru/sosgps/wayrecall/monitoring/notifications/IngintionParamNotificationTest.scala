package ru.sosgps.wayrecall.monitoring.notifications

import java.util.zip.GZIPInputStream

import com.mongodb.casbah.Imports._
import org.junit.runner.RunWith
import org.junit.{Assert, Test}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.sosgps.wayrecall.core.{GPSDataConversions, MongoDBManager}
import ru.sosgps.wayrecall.data.{GPSEvent, UserMessage}
import ru.sosgps.wayrecall.events.{EventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.testutils.ObjectsFixture
import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.EventsTestUtils._
import ru.sosgps.wayrecall.utils.io.DboReader
import ru.sosgps.wayrecall.utils.web.{ScalaCollectionJson, ScalaJson}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class IngintionParamNotificationTest extends grizzled.slf4j.Logging with ObjectsFixture {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var es: EventsStore = null

  @Autowired
  var nd: GpsNotificationDetector = null

  val notificationId = new ObjectId("5642e574cd1a8b7edf22c99e")

  val rule = ScalaCollectionJson.parse[Map[String, Any]]( """{
 |	"name" : "Зажигание",
 |	"email" : "",
 |	"params" : {
 |		"ntfDataSensor" : "ignition",
 |		"ntfDataMin" : 0,
 |		"ntfDataMax" : 0,
 |		"ntfDataOperate" : true
 |	},
 |	"showmessage" : true,
 |	"objects" : [
 |		"o4160068739884382479"
 |	],
 |	"type" : "ntfData",
 |	"action" : "none",
 |	"user" : "Р823НМ77",
 |	"messagemask" : "У объекта сработал %SENSOR% со значением %VALUE%. Зажигание.",
 |	"phone" : ""
 |}""".stripMargin)

  @Test
  def test1() {

    ensureExists("o4160068739884382479")

    val gpsDatas = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/o4160068739884382479.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq

    mdbm.getDatabase()("notificationRulesStates").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRules").remove(MongoDBObject.empty)

    mdbm.getDatabase()("notificationRules").insert(rule + ("_id" -> notificationId))

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)

    Assert.assertEquals(0, aggregatedEvents.size);

    for (data <- gpsDatas.sortBy(_.time)) {
      es.publish(new GPSEvent(data))
    }

    val aggregatedEventsAfter = aggregatedEvents

    EventsTestUtils.waitUntil(aggregatedEventsAfter.size >= 5, 5000)
    nd.invalidateCache()


    Assert.assertEquals(
      new UserMessage(
        "Р823НМ77",
        "У объекта сработал ignition со значением 1,00. Зажигание.",
        "notifications.sensorscontrol",
        Some("o4160068739884382479"),
        None, 1447315038000L,
        Map("readStatus" -> false, "lon" -> 37.841593333333336, "lat" -> 55.76869666666666))
    , aggregatedEventsAfter.take())



  }


}
