
package ru.sosgps.wayrecall.monitoring.notifications

import java.util.concurrent.{DelayQueue, Delayed, TimeUnit}
import java.util.zip.GZIPInputStream
import javax.annotation.PostConstruct

import org.joda.time.{DateTime, DateTimeZone}
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.`object`.ObjectsRepositoryWriter
import ru.sosgps.wayrecall.events.{EventTopic, EventsStore, PredefinedEvents}
import org.junit.{Assert, Before, Ignore, Test}
import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData, GPSDataConversions, MongoDBManager}
import java.util.Date

import ru.sosgps.wayrecall.data.{GPSEvent, UserMessage}
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.monitoring.processing.DistanceAggregationService
import ru.sosgps.wayrecall.processing.lazyseq.{DistanceAggregator, GpsIntervalSplitter, TreeGPSHistoryWalker}
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.io.DboReader
import ru.sosgps.wayrecall.utils.stubs.DeviceUnawareInMemoryPackStore
import ru.sosgps.wayrecall.utils.web.ScalaJson

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import EventsTestUtils._
import ru.sosgps.wayrecall.testutils.ObjectsFixture

import scala.concurrent.duration.{Duration, DurationInt}


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class DistanceNotificationTest extends grizzled.slf4j.Logging with ObjectsFixture {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var nt: GpsNotificationDetector = null

  @Autowired
  var distanceService: DistanceAggregationService = null

  @Autowired
  var es: EventsStore = null

  @PostConstruct
  def ensureObjects(): Unit = {
    ensureExists(testUid)
    ensureExists("o1603556978227868060")
    mdbm.getDatabase()("users").update(MongoDBObject("user" -> "1234"), ScalaJson.parse[Map[String, Any]](
      """{
        |	"additionalpass" : "",
        |	"blockcause" : "",
        |	"canchangepass" : true,
        |	"commandpass" : "",
        |	"comment" : "",
        |	"creator" : "",
        |	"email" : "",
        |	"enabled" : true,
        |	"hascommandpass" : false,
        |	"name" : "1234",
        |	"password" : "1234",
        |	"phone" : "",
        |	"showbalance" : false,
        |	"showfeedetails" : "",
        |	"uid" : "1234",
        |	"userType" : "user"
        |}""".stripMargin('|')
    ), upsert = true)
  }

  val testUid = "o1603556978227868060"


  val rule = Map(
    "_id" -> new ObjectId(),
    "name" -> "Тест прохождения дистанции",
    "email" -> "",
    "params" -> Map(
      "ntfDistMax" -> "0.2",
      "ntfDistInterval" -> "day"
    ),
    "showmessage" -> true,
    "objects" -> List(
      testUid
    ),
    "type" -> "ntfDist",
    "user" -> "1234",
    "messagemask" -> "Объект превысил максимальное расстояние в день" //,
    //"phone" -> "+791604108305"
  )

  val path = Seq((56.141615, 41.9804916),
    (56.14165, 41.9804583),
    (56.141905, 41.9803),
    (56.1421216, 41.9802483),
    (56.142355, 41.9802366),
    (56.1426233, 41.980225),
    (56.1428216, 41.980185),
    (56.1428683, 41.980145),
    (56.1428833, 41.980115),
    (56.1428916, 41.98008),
    (56.1428933, 41.9800433),
    (56.1428983, 41.9798866),
    (56.1428833, 41.97947),
    (56.1428633, 41.979065),
    (56.1428683, 41.9788183),
    (56.1428783, 41.978755),
    (56.1428866, 41.9787316),
    (56.1428983, 41.9787133),
    (56.1429133, 41.9787016),
    (56.14293, 41.978695),
    (56.1430933, 41.9785633),
    (56.1431116, 41.978535),
    (56.1431333, 41.97847),
    (56.1431383, 41.978435),
    (56.1431383, 41.978345),
    (56.1431366, 41.9779216),
    (56.1431216, 41.9774633),
    (56.14311, 41.9770416),
    (56.1431166, 41.9766116))


  @Test
  def testNow() {
    collection.insert(rule)
    val store = new DeviceUnawareInMemoryPackStore()
    distanceService.store = store
    var prev = new DateTime(2012, 10, 1, 1, 1, 0, 0)
    val gpss = path.map(gpsData(_, {prev = prev.plusMinutes(1); prev.toDate}))

    debug("distance=" + path.size + "  " + distance(gpss))

    val aggregatedEvents = collectEvents(es, PredefinedEvents.userMessage)

    val n = 10
    for (gps <- gpss.take(n)) {
      store.put(gps)
      es.publish(new GPSEvent(gps))
    }

    Assert.assertEquals(0, aggregatedEvents.count(_.targetId == "1234"))

    for (gps <- gpss.drop(n)) {
      store.put(gps)
      es.publish(new GPSEvent(gps))
    }

    debug("aggregatedEvents=" + aggregatedEvents)
    EventsTestUtils.waitUntil(aggregatedEvents.size > 0, 14000)

    Assert.assertEquals(1, aggregatedEvents.count(_.targetId == "1234"))
    Assert.assertEquals(rule("messagemask"), aggregatedEvents(0).message)

  }

  def collection = mdbm.getDatabase()("notificationRules")

  @Before
  def clearDb() {
    nt.invalidateCache()
    mdbm.getDatabase()("notificationRulesStates").remove(MongoDBObject.empty)
    collection.remove(MongoDBObject.empty)
  }

  @Test
  @Ignore
  def testAccumulatedDistance() {
    //nt.windowMills = 0
    collection.insert(rule)
    val store = new DeviceUnawareInMemoryPackStore()
    distanceService.store = store


    val path = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/o3849010864663209562.bson.gz"))).iterator

    val limit = new DateTime(2015, 4, 15, 19, 42, 0, 0, DateTimeZone.UTC).getMillis
    val gpss = path.map(dbo => {val gps = GPSDataConversions.fromDbo(dbo); gps.uid = testUid; gps})
      .toIndexedSeq.sortBy(_.insertTime).takeWhile(_.insertTime.getTime < limit)

    val sumdistance = DistanceUtils.sumdistance(gpss.sortBy(_.time))

    val aggregator = new DistanceAggregator(0.0, new GpsIntervalSplitter(new TreeGPSHistoryWalker()))
    gpss.foreach(aggregator.add)
    Assert.assertEquals(sumdistance, aggregator.sum, 0.3)

    val aggregatedEvents = collectEvents(es, PredefinedEvents.userMessage)

    val n = 10
    for (gps <- gpss.take(n)) {
      store.put(gps)
      es.publish(new GPSEvent(gps))
    }

    Assert.assertEquals(0, aggregatedEvents.count(_.targetId == "1234"))

    for (gps <- gpss.drop(n)) {
      store.put(gps)
      es.publish(new GPSEvent(gps))
    }

    //Thread.sleep(2000)

    val notificationTime = new DateTime(2015, 4, 15, 19, 36, 38, DateTimeZone.UTC).getMillis

    def notificationState = mdbm.getDatabase()("notificationRulesStates").findOne(MongoDBObject("uid" -> testUid)).get

    EventsTestUtils.waitUntil(notificationState.as[Date]("time").getTime >= notificationTime, 5000, 100)

    debug("state = " + notificationState)

    Assert.assertEquals(sumdistance, notificationState.as[Double]("accomulated"), 0.01)
    Assert.assertEquals(sumdistance, notificationState.as[Double]("accomulatedB"), 0.01)
    //    debug("aggregatedEvents="+aggregatedEvents)
    //    EventsTestUtils.waitUntil(aggregatedEvents.size > 0, 14000)
    //
    //    Assert.assertEquals(1, aggregatedEvents.count(_.targetId == "1234"))
    //    Assert.assertEquals(rule("messagemask"), aggregatedEvents(0).message)

  }

  @Test
  def testDay() {
    collection.insert(rule)

    val n = 10

    val dates = Seq.tabulate(n)(i => new Date(114, 0, 12, 15, i)) ++ Seq.tabulate(n)(i => new Date(114, 0, 13, 0, i))

    val gpss = (path zip dates).map(a => gpsData(a._1, a._2))

    val aggregatedEvents = collectEvents(es, PredefinedEvents.userMessage)

    debug("aggregatedEvents=" + aggregatedEvents)

    for (gps <- gpss) {
      es.publish(new GPSEvent(gps))
    }

    Assert.assertEquals(0, aggregatedEvents.count(_.targetId == "1234"))

  }


  private[this] def gpsData(p: (Double, Double), date: Date): GPSData = {
    val gps = new GPSData(testUid, "352848025774023", p._2, p._1, date
      , 10.toShort, 0, 14)
    gps.insertTime = date
    gps.data.put("pwr_ext", 20.asInstanceOf[AnyRef])
    gps.goodlonlat = true
    gps
  }

  private[this] def distance(gpss: Seq[GPSData]): Double = {
    gpss.sliding(2).map(s => DistanceUtils.kmsBetween(s(0), s(1))).sum
  }
}

