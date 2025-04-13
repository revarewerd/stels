package ru.sosgps.wayrecall.packreceiver

import java.io.{File, Serializable}
import java.util
import java.util.{Timer, Date}
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{TimeUnit, Executors, ConcurrentHashMap}
import java.util.function.BiFunction

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions}
import ru.sosgps.wayrecall.data._
import ru.sosgps.wayrecall.packreceiver.TestUlits.assertEquals
import ru.sosgps.wayrecall.processing.LazySeqAccumilatedParamsProcessor
import ru.sosgps.wayrecall.utils.stubs.InMemoryPacketsWriter
import ru.sosgps.wayrecall.utils.web.{ScalaCollectionJson, ScalaJson}
import ru.sosgps.wayrecall.utils.{tryNumerics, LimitsSortsFilters, EventsTestUtils}
import ru.sosgps.wayrecall.utils.concurrent.{Semaphore, ScalaExecutorService}
import ru.sosgps.wayrecall.utils.io.DboReader

import scala.collection.mutable.ListBuffer
import scala.collection.{SortedMap, immutable}
import scala.compat.java8.JFunction
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConverters.mapAsScalaConcurrentMapConverter
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

class PackReceiverLevelAggregationTest extends grizzled.slf4j.Logging {

  val cnv = new MapProtocolPackConverter

  val failLimit = 20


  @Test(expected = classOf[IllegalImeiException])
  def testUnexistingObject(): Unit = {
    val packetsWriter = new InMemoryPacketsWriter() {
      @throws(classOf[IllegalImeiException]) override
      def addToDb(gpsdata: GPSData): Boolean = {
        throw new IllegalImeiException("all imeis are illegal")
      }

      override def uidByImei(imei: String): Option[String] = None

      override def getStoreByImei(imei: String): PackagesStore = throw new IllegalImeiException("no store for imei " + imei)
    }
    val processor: PackProcessor = packProcessorFixture(packetsWriter)
    val future = processor.addGpsDataAsync(new GPSData(null, "1234", 0, 0, new Date(100, 9, 9, 9, 9, 9), 0, 0, 0))
    Await.result(future, 1.second)
  }

  @Test
  def testMultipleThreadMultipleObject() = {

    val inclogName = "inclog2015-02-09-21-25-00-660"
    val asStream = this.getClass.getResourceAsStream("/" + inclogName + ".xz")
    require(asStream != null)
    val dbos = new DboReader(
      new XZCompressorInputStream(asStream)
    ).iterator.map(GPSDataConversions.fromDbo).take(10000) //.filter(_.imei == "863071015387688")


    //val expectations =  mapper.readValue()[Map[String, Map[String, Double]]](this.getClass.getResourceAsStream("/" + inclogName + "aggr.json"))
    val expectations = ScalaCollectionJson.parse[Map[String, Map[String, Any]]](this.getClass.getResourceAsStream("/" + inclogName + "aggr.json"))
    //.filterKeys("863071015387688" ==)


    val packetsWriter = new InMemoryPacketsWriter()

    val processor: PackProcessor = packProcessorFixture(packetsWriter)

    val parallel = new ScalaExecutorService(Executors.newFixedThreadPool(8))

    val semaphore = new Semaphore(10)

    var processingError: Option[Throwable] = None

    val ftrs = dbos.foreach(gps => {
      semaphore.acquire()
      parallel.execute {
        //debug("processing "+gps.time+" - "+gps.time.getTime)
        val async = processor.addGpsDataAsync(gps)
        async.onComplete(_ => semaphore.release())
        async.onFailure {
          case e: Exception => {
            processingError = Some(e);
            warn("exception", e)
          }
        }
        async
      }
    })

    EventsTestUtils.waitUntil(processingError.isDefined || {
      val written = packetsWriter.totalWritten.get()
      debug("written:" + written)
      written >= 9038
    } && parallel.getActiveQueueSize <= 0 && processor.inserterPool.getActiveQueueSize <= 0, 30000, 1000)

    parallel.shutdownNow()
    parallel.awaitTermination(2, TimeUnit.SECONDS)
    processor.inserterPool.shutdownNow()
    processor.inserterPool.awaitTermination(2, TimeUnit.SECONDS)

    processingError.foreach(throw _)

    val results = SortedMap[String, Map[String, Any]]() ++ packetsWriter.stores.asMap().values().filter(_.innerTree.tree.size() > 50)
      .map(e => {
      val last = e.innerTree.tree.lastEntry().getValue
      last.data.putAll(last.privateData)
      last.imei -> Map(
        "count" -> e.innerTree.tree.size(),
        "tdist" -> last.data.get("tdist"),
        "mh" -> last.data.get("mh")
      )
    })

    ScalaCollectionJson.generate(results, new File(inclogName + "_" + this.getClass.getSimpleName + "aggr.json"))

    val faledassertions = new ListBuffer[Throwable]

    try {
      expectations.foreach {
        case (imei, d) => try {
          assertEquals("tdist for", d("tdist").tryDouble, Option(results(imei)("tdist")).map(_.tryDouble).getOrElse(0.0), 0.01)
          assertEquals("mh for", d("mh").tryDouble, Option(results(imei)("mh")).map(_.tryDouble).getOrElse(0.0), 0.01)
        } catch {
          //case aseertion: java.lang.AssertionError  if faledassertions.length < 10 => faledassertions +=  aseertion
          case aseertion@(_: AssertionError | _: Exception) if faledassertions.length < failLimit => faledassertions += aseertion
        }
      }
    }
    finally {
      for (assertion <- faledassertions) {
        error("keept failed assertion:", assertion)
        assertion.printStackTrace(System.err)
      }
    }

  }


  private def packProcessorFixture(packetsWriter: InMemoryPacketsWriter): PackProcessor = {
    val processor: PackProcessor = setupPackProcessor(packetsWriter)

    val elems = accumulator(packetsWriter)
    processor.gpsdataPreprocessors = immutable.Seq[processor.Preprocessor](elems);
    processor
  }

  protected def accumulator(latestPacketsWriter: InMemoryPacketsWriter): (GPSData) => Future[GPSData] = {
    val processor = new LazySeqAccumilatedParamsProcessor(latestPacketsWriter, cnv)
    processor.evictingTime = 30000
    processor
  }

  def setupPackProcessor(latestPacketsWriter: DBPacketsWriter): PackProcessor = {
    new TrimmedPackProcessor(latestPacketsWriter)
  }

}






