package ru.sosgps.wayrecall.monitoring.notifications

import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

import org.joda.time.DateTime
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
import scala.concurrent.duration.Duration

/**
  * Created by maxim on 07.03.16.
  */
@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class BadFuelingTest extends ObjectsFixture with grizzled.slf4j.Logging {

	@Autowired
	var mdbm: MongoDBManager = null

	@Autowired
	var es: EventsStore = null

	@Autowired
	var nt: GpsNotificationDetector = null

	val testUids = IndexedSeq(
		"o2375087825488753634",
//		"o2375087825488753635",
//		"o4101211322152391053",
//		"o1179166601455194467",
//		"o4490271822845429693"

		"o592042996114412889",
		"o1733479494293617784"
	)

	@PostConstruct
	def ensureObjects(): Unit = {
		testUids.foreach(ensureExists)
	}

	val title = "notifications.badfuelcontrol"
	val time = new DateTime(2016, 3, 7, 23, 15, 20).toDate
	val user = "1234"

	val rule = Map(
		"_id" -> new ObjectId(),
		"name" -> "Какой-то тест, хз о чем вообще",
		"email" -> "",
		"params" -> Map(
			"ntfBadFuelCount" -> "2"
		),
		"showmessage" -> true,
		"objects" -> testUids,
		"type" -> "ntfBadFuel",
		"action" -> "none",
		"user" -> user,
		"messagemask" -> "Датчик топлива шлет ненормальные значения (0 или 65к)"
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
//		val aggregatedEventsAfter = gpsToEvents("/objPacks.o2375087825488753634.bson.gz")
//		val aggregatedEventsAfter = gpsToEvents("/fuel/o592042996114412889.bson.gz")
		val aggregatedEventsAfter = gpsToEvents1("/fuel/o1733479494293617784.bson.gz")
		debug("test1():")
		debug(aggregatedEventsAfter.toList.mkString("\n"))
		debug(aggregatedEventsAfter.toList.size)

		Assert.assertEquals(2, aggregatedEventsAfter.size)
	}

	private def gpsToEvents2(file: String, first: Duration = 5 second, others: Duration = 1 second): Stream[UserMessage] = {
		val gpsDatas = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream(file)))
			.iterator.map(GPSDataConversions.fromDbo).toIndexedSeq
		val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)
		for (data <- gpsDatas.sortBy(_.insertTime)) {
			println(data)
			es.publish(new GPSEvent(data))
		}
		val aggregatedEventsAfter = aggregatedEvents.queueToStream(first, others)
//    for (userMessage <- aggregatedEventsAfter) {
//      println(new Date(userMessage.time) + " " + userMessage)
//    }
		aggregatedEventsAfter
	}

	private def gpsToEvents1(file: String, first: Duration = 5 second, others: Duration = 1 second): Stream[UserMessage] = {
		val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)

		// to fire
		for (i <- 0 to 2) {
			val myTime = new DateTime(2016, 3, 7, 23, 15, 20 + i).toDate
			val gpsData = new GPSData("o2375087825488753634", "BAD", 0.0, 0.0, myTime, 90, 0, 11)
			gpsData.data.put("fuel_lvl", new Integer(65535))
			es.publish(new GPSEvent(gpsData))
		}

		// to unfire
		for (i <- 0 to 2) {
			val myTime = new DateTime(2016, 3, 7, 23, 15, 25 + i).toDate
			val gpsData = new GPSData("o2375087825488753634", "GOOD", 0.0, 0.0, myTime, 90, 0, 11)
			es.publish(new GPSEvent(gpsData))
		}

		// to fire
		for (i <- 0 to 5) {
			val myTime = new DateTime(2016, 3, 7, 23, 15, 30 + i).toDate
			val gpsData = new GPSData("o2375087825488753634", "BAD", 0.0, 0.0, myTime, 90, 0, 11)
			gpsData.data.put("fuel_lvl", new Integer(65535))
			es.publish(new GPSEvent(gpsData))
		}


		val aggregatedEventsAfter = aggregatedEvents.queueToStream(first, others)
		aggregatedEventsAfter
	}
}