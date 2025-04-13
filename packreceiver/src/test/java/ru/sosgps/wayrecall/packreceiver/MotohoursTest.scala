package ru.sosgps.wayrecall.packreceiver

import java.io.FileInputStream
import java.lang.reflect.{Method, InvocationHandler}
import java.util.concurrent.TimeUnit
import java.util.{Timer, Date}
import java.util.zip.GZIPInputStream

import com.google.common.cache.CacheBuilder
import jdk.nashorn.internal.ir.annotations.Ignore
import org.junit.runner.RunWith
import org.junit.{Assert, Test}
import org.powermock.api.support.membermodification.MemberMatcher.method
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.data._
import ru.sosgps.wayrecall.initialization.MultiDbManager
import ru.sosgps.wayrecall.packreceiver.TestUlits._
import ru.sosgps.wayrecall.processing.LazySeqAccumilatedParamsProcessor
import ru.sosgps.wayrecall.utils.stubs.DeviceUnawareInMemoryPackStore
import ru.sosgps.wayrecall.utils.{funcLoadingCache, tryNumerics, LimitsSortsFilters}
import ru.sosgps.wayrecall.utils.io.DboReader

import scala.collection.immutable
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.DurationDouble
import scala.util.Random

/**
 * Created by nickl on 27.11.14.
 */

trait TDistanceSamples extends grizzled.slf4j.Logging {

  @Test
  def testTdist(): Unit = {
    val gpsDatas = GPSUtils.filterBadLonLat(new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o83971426548810109.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq.sortBy(_.time).toIterator).toIndexedSeq

    //gpsDatas.foreach(g => debug(GPSUtils.detectIgnition(g)))

    val prevGps = runAggregator(gpsDatas, history = true)
    prevGps.data.putAll(prevGps.privateData)
    debug("tdist=" + prevGps.data.get("tdist"))
    debug("distutils=" + DistanceUtils.sumdistance(gpsDatas.iterator))


    Assert.assertEquals(16.1509, prevGps.data.get("tdist").tryDouble, 0.001)

  }

  @Test
  def testTdist2(): Unit = {

    //    val gpsDatas = GPSUtils.filterBadLonLat(new DboReader(new GZIPInputStream(
    // new FileInputStream("/home/nickl/Загрузки/o2521726042381181224.bson.gz")))
    //      .iterator.map(GPSDataConversions.fromPerObjectDbo)
    //      .map(g => {g.privateData.clear(); g.data.remove("mh"); g.data.remove("tdist"); g})
    //      .toIndexedSeq.sortBy(_.time).take(3000).toIterator).toIndexedSeq

    val gpsDatas = GPSUtils.filterBadLonLat(new DboReader(new GZIPInputStream(
      this.getClass.getResourceAsStream("/o2521726042381181224.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq.sortBy(_.time)
      .drop(0).take(1000)
      .toIterator).toIndexedSeq

    //gpsDatas.foreach(g => debug(GPSUtils.detectIgnition(g)))

    doTestDistance(gpsDatas)
  }



  implicit class randomize[T](seq: Seq[T]) {
    def shuffle(size: Int) = seq.grouped(size).map(s => Random.shuffle(s)).flatten.toSeq
  }

  @Test
  def testWithPauses(): Unit = {
    val gpsDatas = new DboReader(
      new GZIPInputStream(
        this.getClass.getResourceAsStream("/o1804826124483519280.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo)
      .map(clearAccData).toIndexedSeq.sortBy(_.insertTime).take(500)

    var count = 0

    doTestDistance(gpsDatas.iterator.map(g => {
      if (count > 0 && count % 200 == 0) {
        debug(s"count = $count, so waiting")
        Thread.sleep(300)
      }
      count = count + 1
      g
    }).toStream)
  }

  protected def clearAccData(g: GPSData): GPSData = {
    g.privateData.clear();
    g.data.remove("mh");
    g.data.remove("tdist");
    g
  }

  protected def doTestDistance(gpsDatas: immutable.Seq[GPSData], permittedRelativeMistake: Double = 0.02): Unit = {
    val prevGps = runAggregator(gpsDatas, history = true)
    prevGps.data.putAll(prevGps.privateData)
    debug("tdist = " + prevGps.data.get("tdist"))
    val distulis = DistanceUtils.sumdistance(gpsDatas.iterator)
    debug("distutils = " + distulis)

    assertEquals(distulis, prevGps.data.get("tdist").tryDouble, permittedRelativeMistake)
  }

  @Test
  def testWoHistooryTdist(): Unit = {
    val gpsDatas = GPSUtils.filterBadLonLat(new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o83971426548810109.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq.sortBy(_.time).toIterator).toIndexedSeq

    //gpsDatas.foreach(g => debug(GPSUtils.detectIgnition(g)))

    val prevGps = runAggregator(gpsDatas, history = false)

    debug("tdist=" + prevGps.data.get("tdist"))
    val sumdistance = DistanceUtils.sumdistance(gpsDatas.iterator.drop(5))
    debug("distutils=" + sumdistance)

    prevGps.data.putAll(prevGps.privateData)
    Assert.assertEquals(woHistooryExpected, prevGps.data.get("tdist").tryDouble, 0.005)

  }

  val woHistooryExpected = 15.895642157067899

  def runAggregator(gpsDatas: Seq[GPSData], history: Boolean): GPSData

}

trait MotohoursSamples {
  @Test
  def test1(): Unit = {
    val gpsDatas = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o83971426548810109.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq.sortBy(_.time)

    //gpsDatas.foreach(g => debug(GPSUtils.detectIgnition(g)))

    doTestTotal(3336000L, gpsDatas)
  }

  @Test
  def test2(): Unit = {
    val gpsDatas = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o2375087825488753634.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq.sortBy(_.time)
    doTestTotal(2439000L, gpsDatas)
  }

  def doTestTotal(total: Long, gpsDatas: IndexedSeq[GPSData]) = {
    val prevGps = runAggregator(gpsDatas)
    prevGps.data.putAll(prevGps.privateData)
    assertEquals(total, prevGps.data.get("mh"), 0.01)
  }

  def runAggregator(gpsDatas: Seq[GPSData], history: Boolean = true): GPSData

}

//@RunWith(classOf[PowerMockRunner])
//@PrepareForTest(Array(classOf[AccumulatedParamsProcessor]))
abstract class AccumulatedParamsProcessorAggregatorSetup extends grizzled.slf4j.Logging {

  def runAggregator(gpsDatas: Seq[GPSData], history: Boolean = true): GPSData = {
    val (inMemoryPackStore, aggregator) = setupAggregator(gpsDatas.iterator, history)

    var prevGps: GPSData = gpsDatas(5)

    for (gps <- gpsDatas.drop(5)) {
      val ign = GPSUtils.detectIgnitionInt(gps)
      val r = Await.result(aggregator.apply(gps), 1.second)
      inMemoryPackStore.put(r)
      if (notDuplicate(gps) && (ignitionOff(gps) || ignitionOff(prevGps))) {
        debug("cheching no ignition")
        Assert.assertEquals(prevGps.data.get("mh"), r.data.get("mh"))
      }

      //debug("r=" + r)
      if (notDuplicate(gps))
        prevGps = r
    }
    prevGps
  }

  private def notDuplicate(gps: GPSData): Boolean = {
    !java.lang.Boolean.TRUE.equals(gps.data.get("duplicate"))
  }

  private def setupAggregator(gpsDatas: Iterator[GPSData], history: Boolean = true) = {
    val stored = gpsDatas.take(5).toSeq
    val inMemoryPackStore = new DeviceUnawareInMemoryPackStore(if (history) stored else Seq.empty)

    val imei = stored.head.imei
    val uid = stored.head.uid

    val reader = new ClusteredPacketsReader {
      override def getStoreByImei(imei: String): PackagesStore = inMemoryPackStore

      override def uidByImei(imei: String) = Some(uid)
    }

    (inMemoryPackStore, createParamsProcessor(reader))
  }


  protected def createParamsProcessor(reader: ClusteredPacketsReader): (GPSData) => Future[GPSData]

  val cnv = new MapProtocolPackConverter

  def ignitionOff(g: GPSData) = detectIgnition(g).getOrElse(0) <= 0

  private def detectIgnition(cur: GPSData): Option[Int] = {
    val data = new GPSData(cur.uid, cur.imei, cur.time)
    data.data = cur.data
    cnv.convertData(data)
    GPSUtils.detectIgnitionInt(data)
  }

}


class LazySeqAccumulatorTest extends AccumulatedParamsProcessorAggregatorSetup with TDistanceSamples with MotohoursSamples {

  //
  // override val woHistooryExpected = 15.891820866777778
  //

  override protected def createParamsProcessor(reader: ClusteredPacketsReader): (GPSData) => Future[GPSData] = {
    val processor = new LazySeqAccumilatedParamsProcessor(reader, cnv, 100)
    processor.evictingTime = 1000
    processor
  }

  @Test
  def testWithPausesInverse(): Unit = {
    val gpsDatas = new DboReader(
      new GZIPInputStream(
        this.getClass.getResourceAsStream("/o1804826124483519280.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo)
      .map(clearAccData).toIndexedSeq.sortBy(_.insertTime).take(1000).reverse

    var count = 0

    doTestDistance(gpsDatas.iterator.map(g => {
      if (count > 0 && count % 200 == 0) {
        debug(s"count = $count, so waiting")
        Thread.sleep(300)
      }
      count = count + 1
      g
    }).toStream)
  }

  @Test
  def testSmallIntervals(): Unit = {
    val gpsDatas = new DboReader(
      new GZIPInputStream(
        this.getClass.getResourceAsStream("/o1189014804117835622.bson.gz")))
      .iterator.map(GPSDataConversions.fromDbo)
      .map(clearAccData).toIndexedSeq.sortBy(_.insertTime).slice(2000, 7000)

    doTestDistance(gpsDatas, 0.03)
  }

}

