
package ru.sosgps.wayrecall.monitoring.notifications

import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream
import javax.annotation.PostConstruct

import com.mongodb.casbah.Imports._
import org.joda.time.{DateTime, DateTimeZone}
import org.junit.runner.RunWith
import org.junit.{Assert, Before, Ignore, Test}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData, GPSDataConversions, MongoDBManager}
import ru.sosgps.wayrecall.data._
import ru.sosgps.wayrecall.events.{EventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.monitoring.web.MaintenanceService
import ru.sosgps.wayrecall.utils.{EventsTestUtils, tryNumerics}
import EventsTestUtils._
import ru.sosgps.wayrecall.testutils
import ru.sosgps.wayrecall.testutils.{DataHelpers, ObjectsFixture, expectException, runAsUser}
import ru.sosgps.wayrecall.utils.errors.ImpossibleActionException
import ru.sosgps.wayrecall.utils.io.DboReader

import scala.concurrent.duration.DurationInt


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class MaintenanceNotificationTest extends grizzled.slf4j.Logging with ObjectsFixture {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var nt: GpsNotificationDetector = null

  @Autowired
  var maintenanceService: MaintenanceService = null

  @Autowired
  var pw: DBPacketsWriter = null

  @Autowired
  var packStore: PackagesStore = null

  @Autowired
  var es: EventsStore = null

  @PostConstruct
  def ensureObjects(): Unit = {
    ensureExists(testUid)
    ensureExists("o1603556978227868060")
  }

  val testUid = "o1603556978227868060"


  val rule = Map(
    "_id" -> new ObjectId(),
    "name" -> "Тест прохождения дистанции",
    "email" -> "",
    "params" -> Map(
      "ntfDistMax" -> "2.5",
      "ntfMotoMax" -> "2.1"
    ),
    "showmessage" -> true,
    "objects" -> List(
      testUid
    ),
    "type" -> "ntfMntnc",
    "user" -> "1234",
    "messagemask" -> "Требуется техобслуживание",
    "phone" -> "+791604108305"
  )

  def notificationRules = mdbm.getDatabase()("notificationRules")

  @Before
  def clearDb() {
    nt.invalidateCache()
    mdbm.getDatabase()("notificationRulesStates").remove(MongoDBObject.empty)
    notificationRules.remove(MongoDBObject.empty)
    packStore.removePositionData(testUid, new Date(0), new Date(Long.MaxValue))
  }


  @Test
  def testDay() {
    notificationRules.insert(rule)

    val gpss = new DboReader(new GZIPInputStream(
      this.getClass.getResourceAsStream("/o4475460171288094111.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo)
    //.take(50)
    //.takeWhile(_.time.getTime < 1432264717650L)
    //.  takeWhile(_.time.getTime <= 1432264254571L)

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)

    val state0 = runAsUser("1234") {
      maintenanceService.getMaintenanceState(testUid)
    }
    debug("state0=" + state0)


    for ((gps, i) <- gpss.zipWithIndex /*.take(15)*/ ) {
      //debug("publishing:"+i)
      publish(gps)
    }


    val messages = aggregatedEvents.queueToStream(5.seconds, 10.milli).toList

    //debug(s"messages=$messages")

    Assert.assertEquals(
      List(
        new UserMessage("1234",
          "Требуется техобслуживание",
          "notifications.maintenance",
          Some("o1603556978227868060"),
          None,
          1432265034280L, Map("readStatus" -> false, "lon" -> 37.7401568, "lat" -> 55.4806208))
      ), messages)
    nt.invalidateCache()

    val state2 = runAsUser("1234") {
      maintenanceService.getMaintenanceState(testUid)
    }

    debug("maintenanceService state=" + state2)

    Assert.assertEquals(-52.895185660749945, state2("distanceUntil").asInstanceOf[Double], 0.001)
    Assert.assertEquals(1.0125827777777778, state2("motohoursUntil").asInstanceOf[Double], 0.001)
    Assert.assertEquals(-22.461094444444445, state2("hoursUntil").asInstanceOf[Double], 0.001)
  }

  @Test
  def testCustomUpdateDay() {
    notificationRules.insert(rule)

    val gpss = new DboReader(new GZIPInputStream(
      this.getClass.getResourceAsStream("/o4475460171288094111.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).slice(160, 180).toIndexedSeq.sortBy(_.time)

    val (preload, load) = gpss.splitAt(2)

    debug("distance=" + (load.last.privateData.get("tdist").tryDouble - load.head.privateData.get("tdist").tryDouble))

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)

    preload.foreach(publish)

    nt.invalidateCache()

    runAsUser("1234") {
      val state0 = maintenanceService.getMaintenanceState(testUid)

      debug("state0=" + state0)

      maintenanceService.saveSettings(testUid,
        Map(
          "distanceUntil" -> 1,
          "motohoursUntil" -> 1,
          "hoursUntil" -> 1
        ))
    }

    load.foreach(publish)
    nt.invalidateCache()
    val messages = aggregatedEvents.queueToStream(5.seconds, 10.milli).toList

    Assert.assertEquals(1, messages.size /*.count(_.targetId == "1234")*/)
    debug(s"messages=$messages")


    val state2 = runAsUser("1234") {
      maintenanceService.getMaintenanceState(testUid)
    }

    debug("maintenanceService state=" + state2)

    Assert.assertEquals(-5.265475292870178, state2("distanceUntil").asInstanceOf[Double], 0.001)
    Assert.assertEquals(0.9035599999999704, state2("motohoursUntil").asInstanceOf[Double], 0.001)
    Assert.assertEquals(0.9686527777776064, state2("hoursUntil").asInstanceOf[Double], 0.001)
  }

  @Test
  def testReset() {
    notificationRules.insert(rule)

    val gpss = new DboReader(new GZIPInputStream(
      this.getClass.getResourceAsStream("/o4475460171288094111.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).slice(160, 180).toIndexedSeq.sortBy(_.time)

    val (preload, load) = gpss.splitAt(2)

    preload.foreach(publish)


    runAsUser("1234") {
      maintenanceService.saveSettings(testUid,
        Map(
          "hoursInterval" -> 5000,
          "distanceInterval" -> 10000,
          "motohoursInterval" -> 1000,
          "distanceUntil" -> 3,
          "motohoursUntil" -> 3,
          "hoursUntil" -> 3
        ))
    }

    val state1 = runAsUser("1234") {
      maintenanceService.getMaintenanceState(testUid)
    }

    debug("state1=" + state1)

    Assert.assertEquals(10000.0, state1("distanceInterval").asInstanceOf[Double], 0.001)
    Assert.assertEquals(3.0, state1("distanceUntil").asInstanceOf[Double], 1.0)
    Assert.assertEquals(1000.0, state1("motohoursInterval").asInstanceOf[Double], 0.001)
    Assert.assertEquals(3.0, state1("motohoursUntil").asInstanceOf[Double], 0.001)


    runAsUser("1234") {
      maintenanceService.resetMaintenance(testUid)
    }


    val state2 = runAsUser("1234") {
      maintenanceService.getMaintenanceState(testUid)
    }

    debug("state2=" + state1)

    Assert.assertEquals(10000.0, state2("distanceInterval").asInstanceOf[Double], 0.001)
    Assert.assertEquals(10000.0, state2("distanceUntil").asInstanceOf[Double], 1.0)
    Assert.assertEquals(1000.0, state2("motohoursInterval").asInstanceOf[Double], 0.001)
    Assert.assertEquals(1000.0, state2("motohoursUntil").asInstanceOf[Double], 0.001)

  }



  @Test
  def testEnabled() {
    notificationRules.insert(Map(
      "_id" -> new ObjectId(),
      "name" -> "Тест прохождения дистанции",
      "email" -> "",
      "params" -> Map(
        "ntfDistMax" -> "2.5",
        "ntfDistEnabled" -> false,
        "ntfMotoMax" -> "2.1"
      ),
      "showmessage" -> true,
      "objects" -> List(
        testUid
      ),
      "type" -> "ntfMntnc",
      "user" -> "1234",
      "messagemask" -> "Требуется техобслуживание",
      "phone" -> "+791604108305"
    )
    )

    expectException[ImpossibleActionException] {
      runAsUser("1234") {
        maintenanceService.saveSettings(testUid,
          Map(
            "distanceEnabled" -> true
          ))
      }
    }

    val state2 = runAsUser("1234") {
      maintenanceService.getMaintenanceState(testUid)
    }

    debug("state2=" + state2)

    Assert.assertFalse(state2("distanceEnabled").asInstanceOf[Boolean])

  }


  def publish(gps: GPSData): Unit = {
    gps.uid = testUid
    pw.addToDb(gps)
    es.publish(new GPSEvent(gps))
  }
}
