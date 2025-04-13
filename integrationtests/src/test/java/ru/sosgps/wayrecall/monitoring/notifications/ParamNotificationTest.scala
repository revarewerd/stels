package ru.sosgps.wayrecall.monitoring.notifications

import java.util.Date
import java.util.zip.GZIPInputStream

import com.mongodb.casbah.Imports._
import org.junit.{Assert, Test}
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.sosgps.wayrecall.core.{GPSDataConversions, MongoDBManager}
import ru.sosgps.wayrecall.data.{GPSEvent, UserMessage}
import ru.sosgps.wayrecall.events.{EventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.utils.EventsTestUtils
import EventsTestUtils._
import ru.sosgps.wayrecall.testutils.ObjectsFixture
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.io.DboReader
import ru.sosgps.wayrecall.utils.web.ScalaJson

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class ParamNotificationTest extends grizzled.slf4j.Logging with ObjectsFixture {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var es: EventsStore = null

  @Autowired
  var nd: GpsNotificationDetector = null

  val notificationId = new ObjectId("53d0fea384ae4ffc4a2834d4")

  val rule = ScalaJson.parse[Map[String, Any]]( """{
                                                  |	"name" : "bug",
                                                  |	"email" : "",
                                                  |	"params" : {
                                                  |		"ntfDataSensor" : "pwr_ext",
                                                  |		"ntfDataMin" : "13",
                                                  |		"ntfDataMax" : "16",
                                                  |		"ntfDataOperate" : true
                                                  |	},
                                                  |	"showmessage" : true,
                                                  |	"allobjects" : true,
                                                  |	"type" : "ntfData",
                                                  |	"action" : "none",
                                                  |	"user" : "atlanta",
                                                  |	"messagemask" : "У объекта сработал %SENSOR% со значением %VALUE%.",
                                                  |	"phone" : ""
                                                  |}
                                                  | """.stripMargin)

  val uid: String = "o2645125656274874537"
  val imei: String = "353976013691070"

  @Test
  def test1() {

    val gpsDatas = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o2375087825488753634.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).map(g => {
      g.uid = uid;
      g.imei = imei;
      g
    }).toIndexedSeq


    mdbm.getDatabase()("notificationRulesStates").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRules").remove(MongoDBObject.empty)
    /*
    {
	"_id" : ObjectId("53d12efc0a8231dd3114b01d"),
	"notificationRule" : ObjectId("53d0fea384ae4ffc4a2834d4"),
	"uid" : "o2839765074353479687",
	"fired" : false,
	"time" : ISODate("2014-07-24T16:04:24Z")
}

     */
    //    mdbm.getDatabase()("notificationRulesStates").insert(
    //      MongoDBObject(
    //        "_id" -> new ObjectId("53d12efc0a8231dd3114b01d"),
    //        "notificationRule" -> new ObjectId("53d12efc0a8231dd3114b01c"),
    //        "uid" -> uid,
    //        "fired" -> false,
    //        "time" -> org.joda.time.format.ISODateTimeFormat.dateTime.parseDateTime("2014-06-24T16:04:24.000Z").toDate
    //      ))

    mdbm.getDatabase()("notificationRules").insert(rule + ("_id" -> notificationId))

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)

    Assert.assertEquals(0, aggregatedEvents.size);

    for (data <- gpsDatas.sortBy(_.time)) {
      //      println(data.time + " GoSafe PowerExt Message = " + data.data)
      es.publish(new GPSEvent(data))
      //      println("aggregatedEvents="+aggregatedEvents)

    }

    val aggregatedEventsAfter = aggregatedEvents

//    for (userMessage <- aggregatedEventsAfter) {
//      println(new Date(userMessage.time) + " " + userMessage)
//    }
    EventsTestUtils.waitUntil(aggregatedEventsAfter.size >= 2, 5000)
    nd.invalidateCache()


    //Assert.assertEquals(1, aggregatedEventsAfter.size)

    Assert.assertEquals(List(
      new UserMessage(
        "atlanta",
        "У объекта сработал Датчик напряжения со значением 12,80.",
        "notifications.sensorscontrol",
        Some("o2645125656274874537"),
        None, 1396944876000L,
        Map("readStatus" -> false, "lon" -> 37.8579683, "lat" -> 55.4535316)
      ),
      new UserMessage("atlanta",
        "У объекта сработал Датчик напряжения со значением 12,91.",
        "notifications.sensorscontrol",
        Some("o2645125656274874537"),
        None, 1396945613000L,
        Map("readStatus" -> false, "lon" -> 37.8575816, "lat" -> 55.453845))
    ), aggregatedEventsAfter.toArray.toList)

  }


}
