package ru.sosgps.wayrecall

import java.util.Date
import java.util.concurrent.TimeUnit

import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import ru.sosgps.wayrecall.core.{GPSData, MongoDBManager, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.data._
import ru.sosgps.wayrecall.utils.{EventsTestUtils, durationAsJavaDuration}
import ru.sosgps.wayrecall.packreceiver.BufferedMongoDBPacketsWriter
import ru.sosgps.wayrecall.scalatestutils.CleanableMdbm
import ru.sosgps.wayrecall.sms.IOSwitchDeviceCommand
import ru.sosgps.wayrecall.testutils.ObjectsFixture

import scala.concurrent.duration.DurationInt

/**
  * Created by nickl-mac on 10.06.16.
  */
@ContextConfiguration(classes = Array(classOf[PackagesWritingConfig]))
class PackagesWritingTest extends FunSpec with ObjectsFixture with CleanableMdbm with grizzled.slf4j.Logging {


  @Autowired
  var writer: BufferedMongoDBPacketsWriter = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var reader: PackagesStore = null

  new TestContextManager(this.getClass).prepareTestInstance(this)


  describe("Storage Writing and reading process") {

    it("one objects received by DBPacketsWriter should be readable by PackagesStore") {
      inClearDb {
        ensureExists("0", "0")

        //val gps = new GPSData("0", "0", 20.0, 20.0, new Date(), 0, 0, 11)

        val gpses = new Posgenerator("0").movingsWithTimeStep(1 second).take(1000).toList

        gpses.foreach(writer.addToDb)
        Thread.sleep(300)
        val loaded = reader.getHistoryFor("0", gpses.head.time, gpses.last.time).toList.distinct

        debug("loaded=" + loaded)
        debug("expected=" + gpses)

        loaded should equal(gpses)

      }
    }

    it("multiple objects data received by DBPacketsWriter should be readable by PackagesStore") {
      inClearDb {

        val uids = Iterator.from(0).map(_.toString).take(20).toVector

        for (uid <- uids) {
          ensureExists(uid, uid)
        }


        //val gps = new GPSData("0", "0", 20.0, 20.0, new Date(), 0, 0, 11)

        val gpses = uids.flatMap(uid =>
          new Posgenerator(uid).movingsWithTimeStep(1 second).take((uid.toInt + 1) * 10).toVector).sortBy(_.time)

        gpses.foreach(writer.addToDb)
        //Thread.sleep(1000)

        def getGpses() = uids.flatMap(uid => reader.getHistoryFor(uid, gpses.head.time, gpses.last.time).toStream.distinct.toVector).sortBy(_.time)


        EventsTestUtils.waitUntil({
          gpses == getGpses()
        }, 20000)


      }
    }


  }


}


class PackagesWritingConfig {

  @Bean
  def mongodbManager: MongoDBManager = {
    val manager = new MongoDBManager()
    manager.databaseName = this.getClass.getName.replace(".", "-") + "-test"
    manager
  }

  @Bean
  def objectsRepositoryReader = new ObjectsRepositoryReader

  @Bean
  def packDataConverter = new MapProtocolPackConverter

  @Bean
  def reader: PackagesStore = new MongoPackagesStore


  @Bean
  def writer: DBPacketsWriter = {
    val packetsWriter = new BufferedMongoDBPacketsWriter
    packetsWriter.bufferSize = 50
    packetsWriter.perUidBufferLimit = 20
    packetsWriter.drainSleepTime = 1500
    packetsWriter
  }


}