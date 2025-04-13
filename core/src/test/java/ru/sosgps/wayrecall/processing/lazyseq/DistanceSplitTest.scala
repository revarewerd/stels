

package ru.sosgps.wayrecall.processing.lazyseq

import java.util.zip.GZIPInputStream

import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test
import ru.sosgps.wayrecall.core.{GPSData, DistanceUtils, GPSDataConversions, GPSUtils}
import ru.sosgps.wayrecall.utils.io.DboReader

/**
 * Created by nickl on 23.03.15.
 */
trait DistanceSplitTest extends grizzled.slf4j.Logging {

  val precesion = 0.005
  
  def createSplitter(): GpsIntervalSplitter

  val gpsDatasSorted = GPSUtils.filterBadLonLat(new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/objPacks.o83971426548810109.bson.gz")))
    .iterator.map(GPSDataConversions.fromDbo).toIndexedSeq.sortBy(_.time).toIterator).toIndexedSeq

  @Test
  def testIntervals(): Unit = {
    val sequence = createSplitter()

    sequence.add(gpsDatasSorted(0))
    sequence.add(gpsDatasSorted(3))

    var interval = sequence.getInterval(gpsDatasSorted(1).time).get

    Assert.assertEquals(gpsDatasSorted(0), interval.start)
    Assert.assertEquals(gpsDatasSorted(3), interval.end)

    sequence.add(gpsDatasSorted(2))

    interval = sequence.getInterval(gpsDatasSorted(1).time).get

    Assert.assertEquals(gpsDatasSorted(0), interval.start)
    Assert.assertEquals(gpsDatasSorted(2), interval.end)

    interval = sequence.getInterval(gpsDatasSorted(2).time).get

    Assert.assertEquals(gpsDatasSorted(0), interval.start)
    Assert.assertEquals(gpsDatasSorted(2), interval.end)

  }

  @Test
  def testSplits(): Unit = {
    val sequence = createSplitter()

    Assert.assertEquals(NoInterval, sequence.addAndGetSplit(gpsDatasSorted(0)))
    Assert.assertEquals(NewInterval(Interval(gpsDatasSorted(0), gpsDatasSorted(3))), sequence.addAndGetSplit(gpsDatasSorted(3)))

    val prevInterval = sequence.getInterval(gpsDatasSorted(2).time).get

    val split = sequence.addAndGetSplit(gpsDatasSorted(2)).asInstanceOf[Split]

    Assert.assertEquals(prevInterval, split.prev)
    Assert.assertEquals(Interval(gpsDatasSorted(0), gpsDatasSorted(2)), split.first)
    Assert.assertEquals(Interval(gpsDatasSorted(2), gpsDatasSorted(3)), split.second)

  }

  @Test
  def testSplits2(): Unit = {
    val sequence = createSplitter()

    Assert.assertEquals(NoInterval, sequence.addAndGetSplit(gpsDatasSorted(50)))
    Assert.assertEquals(NewInterval(Interval(gpsDatasSorted(20), gpsDatasSorted(50))), sequence.addAndGetSplit(gpsDatasSorted(20)))

    val prevInterval = sequence.getInterval(gpsDatasSorted(30).time).get

    val split = sequence.addAndGetSplit(gpsDatasSorted(30)).asInstanceOf[Split]

    Assert.assertEquals(prevInterval, split.prev)
    Assert.assertEquals(Interval(gpsDatasSorted(20), gpsDatasSorted(30)), split.first)
    Assert.assertEquals(Interval(gpsDatasSorted(30), gpsDatasSorted(50)), split.second)

  }

  @Test
  def testDistance(): Unit = {

    forIndexes(IndexedSeq(0, 130, 50, 20, 1, 140, 25, 80))
    forIndexes(IndexedSeq(153, 18, 8, 44, 128, 75, 171, 179
      , 116, 88, 84, 66, 130, 97, 145, 140
      , 92, 121, 57, 94, 67, 103, 136, 149, 117, 123, 157, 81, 2, 26, 24, 69, 71, 112
      , 96, 32, 49, 114, 178, 39, 154, 37, 43, 168, 177, 6, 85, 105, 79, 51, 87, 162, 62, 1, 65, 52, 9, 132, 93, 3
      , 113, 47, 77, 131, 138, 83, 4, 172, 118, 11, 169, 137, 158, 115, 14, 110, 100, 41, 104, 16, 45, 31, 59, 111
    ))

    forIndexes(IndexedSeq(132, 143, 3, 18, 79, 87, 150, 127, 43, 93, 120, 66, 80, 153, 16, 176, 88, 70, 165, 89, 131,
      106, 15, 118, 107, 10, 29, 147, 38, 31, 34, 62, 113, 141, 142, 61, 174, 84, 42, 69, 67, 65, 137, 47, 139, 40,
      138, 68, 134, 112, 48, 175, 51, 170, 152, 157, 54, 128, 130, 49, 169, 110, 136, 95, 149, 144, 178, 164, 167,
      102, 12, 75, 50, 92, 17, 83, 63, 58, 26, 52, 21, 117, 158, 13, 100, 45, 37, 30, 105, 122, 27, 129, 73, 156,
      101, 20, 19, 32, 56, 154, 7, 123, 146, 76, 108, 145, 94, 91, 85, 126, 103, 99, 24, 74, 140, 86, 151, 71,
      57, 36, 115, 46, 53, 1, 25, 121, 39, 97, 22, 5, 168, 90, 41, 55, 14, 148, 35, 166, 64, 23, 109, 2, 163,
      160, 177, 72, 114, 124, 78, 119, 104, 77, 44, 60, 179, 173, 96, 81, 125, 59, 9, 111, 159, 8, 4, 161,
      172, 0, 155, 162, 82, 116, 6, 28, 11, 98, 33, 135, 133, 171))
    //forIndexes(Random.shuffle(gpsDatasSorted.indices.toIndexedSeq))
  }

  @Test
  def testDistanceAll(): Unit = {

    val shuffled = gpsDatasSorted.drop(5)

    val sumdistance = DistanceUtils.sumdistance(shuffled.sortBy(_.time).iterator)

    debug(s"sumdistance=$sumdistance")

    val sequence = createSplitter()

    val distanceAggregator = new DistanceAggregator(0.0, sequence)

    for ((g, i) <- shuffled.zipWithIndex) {
      debug("adding " + (i) + " " + g)
      distanceAggregator.add(g)
      debug("")
    }

    debug(s"sum=${distanceAggregator.sum}")

    Assert.assertEquals(sumdistance, distanceAggregator.sum, precesion)
  }

  @Test
  def testDistanceWithDuplicates(): Unit = {

    forIndexes(IndexedSeq(0, 130, 50, 130 /*, 20, 1, 140, 25, 1, 80*/))
    forIndexes(IndexedSeq(153, 18, 8, 44, 128, 75, 171, 179
      , 116, 88, 84, 66, 130, 97, 145, 140, 3
      , 92, 121, 57, 94, 67, 103, 136, 149, 117, 123, 157, 81, 2, 26, 24, 69, 71, 112, 100
      , 96, 32, 49, 114, 178, 39, 154, 37, 43, 168, 177, 6, 85, 105, 79, 51, 87, 162, 62, 1, 65, 52, 9, 132, 93, 3, 14
      , 113, 47, 77, 131, 138, 83, 4, 172, 118, 11, 169, 137, 158, 115, 14, 110, 100, 41, 104, 16, 45, 31, 59, 111
    ))

    //forIndexes(Random.shuffle(gpsDatasSorted.indices.toIndexedSeq))
  }

  private def forIndexes(indexes: IndexedSeq[Int]): Unit = {

    debug("indexes=" + indexes.mkString("(", ", ", ")"))

    val shuffled = indexes.map(gpsDatasSorted).map(_.clone()) //Random.shuffle(gpsDatasSorted)

    minutifyInsertionTime(shuffled)

    val sumdistance = DistanceUtils.sumdistance(shuffled.sortBy(_.time).iterator)

    debug(s"sumdistance=$sumdistance")

    val sequence = createSplitter()

    val distanceAggregator = new DistanceAggregator(0.0, sequence)

    for ((g, i) <- shuffled.zipWithIndex) {
      debug("adding " + indexes(i) + " " + g)
      distanceAggregator.add(g)
      debug("")
    }

    debug(s"sum=${distanceAggregator.sum}")

    Assert.assertEquals(sumdistance, distanceAggregator.sum, precesion)

  }




}



