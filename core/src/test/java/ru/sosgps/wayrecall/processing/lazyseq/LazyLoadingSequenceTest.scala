package ru.sosgps.wayrecall.processing.lazyseq

import java.util.zip.GZIPInputStream
import java.util.{Date, Timer}


import org.joda.time.DateTimeUtils.currentTimeMillis
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData, GPSDataConversions, GPSUtils}
import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.io.DboReader

import scala.collection.mutable

/**
 * Created by nickl on 17.03.15.
 */


class LazyLoadingSequenceTest extends DistanceSplitTest {

  override def createSplitter(): GpsIntervalSplitter = {
    new GpsIntervalSplitter(new TreeGPSHistoryWalker)
  }

}

class LazyLoadingCachedSequenceTest extends DistanceSplitTest {

  override def createSplitter(): GpsIntervalSplitter = {
    new GpsIntervalSplitter(new CachingGPSHistoryWalker(new NotLoadingTwice(new TreeGPSHistoryWalker)))
  }

  @Test
  def testPut(): Unit = {
    val cache = new CachingGPSHistoryWalker(new TreeGPSHistoryWalker)

    cache.put(gpsDatasSorted(0))
    Assert.assertEquals(None, cache.getNode(cache.Before(gpsDatasSorted(0).time)).gpsOpt)
    Assert.assertEquals(None, cache.getNode(cache.After(gpsDatasSorted(0).time)).gpsOpt)
    cache.put(gpsDatasSorted(2))
    Assert.assertEquals(Some(gpsDatasSorted(0)), cache.getNode(cache.Before(gpsDatasSorted(2).time)).gpsOpt)
    cache.put(gpsDatasSorted(1))

    Assert.assertEquals(cache.prev(gpsDatasSorted(2).time), Some(gpsDatasSorted(1)))

    Assert.assertEquals(Some(gpsDatasSorted(2)), cache.getNode(cache.After(gpsDatasSorted(1).time)).gpsOpt)

  }

  @Test
  def testPutWithAlreadyStored(): Unit = {
    val innerStore = new NotLoadingTwice(new TreeGPSHistoryWalker)
    val cache = new CachingGPSHistoryWalker(innerStore)
    innerStore.put(gpsDatasSorted(0))
    innerStore.put(gpsDatasSorted(2))
    Assert.assertEquals(Some(gpsDatasSorted(0)), cache.getNode(cache.Before(gpsDatasSorted(2).time)).gpsOpt)
    cache.put(gpsDatasSorted(1))

    Assert.assertEquals(cache.prev(gpsDatasSorted(2).time), Some(gpsDatasSorted(1)))
    Assert.assertEquals(Some(gpsDatasSorted(2)), cache.getNode(cache.After(gpsDatasSorted(1).time)).gpsOpt)
  }

  class NotLoadingTwice(srs: GPSHistoryWalker) extends GPSHistoryWalker {
    override def prev(date: Date): Option[GPSData] = assertOnce(('prev, date))(srs.prev(date))

    override def next(date: Date): Option[GPSData] = assertOnce(('next, date))(srs.next(date))

    override def atOrNext(date: Date): Option[GPSData] = assertOnce(('nextthis, date))(srs.atOrNext(date))

    override def put(data: GPSData): Unit = srs.put(data)

    override def at(date: Date): Option[GPSData] = assertOnce(('at, date))(srs.at(date))
  }

  private val already = new mutable.HashMap[Any, List[Array[StackTraceElement]]]().withDefault(_ => List.empty)

  private def assertOnce[T](date: Any)(f: => T): T = {
    val r = f
    if (r != None) {
      val prev = already.put(date, Thread.currentThread().getStackTrace :: already(date))
      prev.foreach(st =>
        Assert.fail("must call for " + date + " only once, but already called within " + st.head.mkString("\n{\n", "\n\tat ", "\n}")))
    }
    r
  }


}

class EvictingTest extends grizzled.slf4j.Logging {

  val gpsDatasSorted = GPSUtils.filterBadLonLat(new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o83971426548810109.bson.gz")))
    .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq.sortBy(_.time).toIterator).toIndexedSeq

  @Test
  def testInnerLoading(): Unit = {
    val innerStore = new TreeGPSHistoryWalker
    val cache = new CachingGPSHistoryWalker(innerStore)
    innerStore.put(gpsDatasSorted(0))
    innerStore.put(gpsDatasSorted(2))
    Assert.assertEquals(Some(gpsDatasSorted(0)), cache.getNode(cache.Before(gpsDatasSorted(2).time)).gpsOpt)
    cache.put(gpsDatasSorted(1))

    Assert.assertEquals(Some(gpsDatasSorted(1)), cache.prev(gpsDatasSorted(2).time))
    Assert.assertEquals(Some(gpsDatasSorted(2)), cache.getNode(cache.After(gpsDatasSorted(1).time)).gpsOpt)
    cache.getNode(cache.After(gpsDatasSorted(1).time)).evict()
    Assert.assertEquals(Some(gpsDatasSorted(2)), cache.getNode(cache.After(gpsDatasSorted(1).time)).gpsOpt)
  }

  @Test
  def distanceWithManualEvicting(): Unit = {

    val indexes = IndexedSeq(153, 18, 8, 44, 128, 75, 171, 179
      , 116, 88, 84, 66, 130, 97, 145, 140
      , 92, 121, 57, 94, 67, 103, 136, 149, 117, 123, 157, 81, 2, 26, 24, 69, 71, 112
      , 96, 32, 49, 114, 178, 39, 154, 37, 43, 168, 177, 6, 85, 105, 79, 51, 87, 162, 62, 1, 65, 52, 9, 132, 93, 3
      , 113, 47, 77, 131, 138, 83, 4, 172, 118, 11, 169, 137, 158, 115, 14, 110, 100, 41, 104, 16, 45, 31, 59, 111
    )

    debug(s"indexes size=${indexes.size}")

    val innerStore = new TreeGPSHistoryWalker
    val cache = new CachingGPSHistoryWalker(innerStore)

    val shuffled = indexes.map(gpsDatasSorted) //Random.shuffle(gpsDatasSorted)

    minutifyInsertionTime(shuffled)

    val sumdistance = DistanceUtils.sumdistance(shuffled.sortBy(_.time).iterator)

    debug(s"sumdistance=$sumdistance")

    val sequence = new GpsIntervalSplitter(cache)

    val distanceAggregator = new DistanceAggregator(0.0, sequence)

    val (firstPart, others) = shuffled.splitAt(40)
    firstPart.foreach(distanceAggregator.add)

    Seq(0, 4, 39, 23, 5, 8, 13, 20, 29, 30, 37, 9, 10).map(firstPart).foreach(gps => cache.getNode(cache.At(gps.time)))

    others.foreach(distanceAggregator.add)

    debug(s"sum=${distanceAggregator.sum}")

    Assert.assertEquals(sumdistance, distanceAggregator.sum, 0.005)
  }

  @Test
  def timeoutEvicting(): Unit = {

    val innerStore = new TreeGPSHistoryWalker
    val evictingTime = 100
    val cache = new TimeoutCachingGPSHistoryWalker(innerStore, new Timer(), evictingTime)

    val prefillTime = currentTimeMillis()
    gpsDatasSorted.foreach(cache.put)
    Assert.assertTrue(cache.nodes.nonEmpty)
    EventsTestUtils.waitUntil(cache.nodes.size == 0, (currentTimeMillis() - prefillTime) * 5 + evictingTime * 5, 500)
    //Thread.sleep()
    cache.synchronized {
      debug("gpsDatasSorted.size=" + gpsDatasSorted.size)
      Assert.assertEquals(0, cache.nodes.size)
    }

  }


}

