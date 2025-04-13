package ru.sosgps.wayrecall

import java.time.LocalDateTime
import java.util.zip.GZIPInputStream
import org.scalatest.FunSpec
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, TestContextManager}
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions, MongoDBManager}
import ru.sosgps.wayrecall.data.{MovementHistory, PackDataConverter, PackagesStore}
import ru.sosgps.wayrecall.packreceiver.BufferedMongoDBPacketsWriter
import ru.sosgps.wayrecall.processing.AccumulatedParamsReaggregator
import ru.sosgps.wayrecall.scalatestutils.CleanableMdbm
import ru.sosgps.wayrecall.testutils.ObjectsFixture
import ru.sosgps.wayrecall.utils.io.DboReader
import ru.sosgps.wayrecall.utils.{EventsTestUtils, java8TLocalDateTimeOps, typingMapJava}

@ContextConfiguration(classes = Array(classOf[PackagesWritingConfig]))
class ReaggregatorTest extends FunSpec with ObjectsFixture with CleanableMdbm with grizzled.slf4j.Logging {

  @Autowired
  var writer: BufferedMongoDBPacketsWriter = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var cnv: PackDataConverter = null

  @Autowired
  var reader: PackagesStore = null


  new TestContextManager(this.getClass).prepareTestInstance(this)

  describe("Repair aggregated params for an object") {

    it("one objects received by DBPacketsWriter should be readable by PackagesStore") {
      inClearDb {
        val uid = "o3164232486696863778"
        ensureExists(uid, "12207007378851")
        ensureExists(uid, "868324028659485")

        val gpses = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/" + uid + ".bson.gz")))
          .iterator.map(GPSDataConversions.fromDbo)
        gpses.foreach(writer.addToDb)
        val from = LocalDateTime.of(2017, 10, 1, 0, 0).toMSKDate

        def history: MovementHistory = reader.getHistoryFor(uid,
          from,
          LocalDateTime.of(2017, 12, 31, 23, 59).toMSKDate
        )

        EventsTestUtils.waitUntil(history.total >= 28093, 10000)

        def mhDiff = {
          val loaded = history.toList
          mh(loaded.last) - mh(loaded.head)
        }

        new AccumulatedParamsReaggregator(reader, writer, cnv).update(uid, from)
        mhDiff shouldBe 8528000
      }

    }

  }

  def mh(gps: GPSData) = gps.privateData.as[Long]("mh")

}

