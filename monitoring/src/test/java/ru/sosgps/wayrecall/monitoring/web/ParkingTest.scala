package ru.sosgps.wayrecall.monitoring.web

import java.util.Date

import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.data.Posgenerator
import ru.sosgps.wayrecall.monitoring.processing.parkings.{Moving, MovingState, MovingStatesExtractor}
import ru.sosgps.wayrecall.testutils.DataHelpers
import ru.sosgps.wayrecall.utils.io.DboReader
import java.util.zip.GZIPInputStream
import java.io.FileInputStream

import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData, GPSDataConversions}
import ru.sosgps.wayrecall.utils.typingMapJava
import ru.sosgps.wayrecall.utils.durationAsJavaDuration

import scala.collection.immutable.IndexedSeq
import scala.concurrent.duration.DurationInt

/**
 * Created by nickl on 27.01.14.
 */
class ParkingTest {


  @Test
  def test1() {
    testWithData("/report.bson.gz", 12, 25)
  }

  @Test
  def test2() {
    testWithData("/report2.bson.gz", 17, 35)

    val reader = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/report2.bson.gz")))

    shadowParkingFromTest2

  }

  @Test
  def shadowParkingFromTest2(): Unit = {
    val datas = IndexedSeq(
      new GPSData("o4263058740375270393", "356307040867151", 37.5014498, 55.8629638, new Date(1386878886000L) /* Fri Dec 13 00:08:06 MSK 2013 */ , 257, 12, 18, "1", new Date(1386878911754L) /* Fri Dec 13 00:08:31 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5013518, 55.8629521, new Date(1386878888000L) /* Fri Dec 13 00:08:08 MSK 2013 */ , 245, 7, 18, "2", new Date(1386878911755L) /* Fri Dec 13 00:08:31 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5012496, 55.8629013, new Date(1386878949000L) /* Fri Dec 13 00:09:09 MSK 2013 */ , 0, 0, 15, "3", new Date(1386879588016L) /* Fri Dec 13 00:19:48 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5012438, 55.8629031, new Date(1386879009000L) /* Fri Dec 13 00:10:09 MSK 2013 */ , 0, 0, 16, "4", new Date(1386879588014L) /* Fri Dec 13 00:19:48 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5012443, 55.8629003, new Date(1386879069000L) /* Fri Dec 13 00:11:09 MSK 2013 */ , 0, 0, 16, "5", new Date(1386879588013L) /* Fri Dec 13 00:19:48 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5012025, 55.8628851, new Date(1386879125000L) /* Fri Dec 13 00:12:05 MSK 2013 */ , 236, 11, 16, "6", new Date(1386879588015L) /* Fri Dec 13 00:19:48 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.501154, 55.8628718, new Date(1386879126000L) /* Fri Dec 13 00:12:06 MSK 2013 */ , 221, 12, 16, "7", new Date(1386879588014L) /* Fri Dec 13 00:19:48 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5010645, 55.8628315, new Date(1386879127000L) /* Fri Dec 13 00:12:07 MSK 2013 */ , 242, 12, 17, "8", new Date(1386879588015L) /* Fri Dec 13 00:19:48 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.500963, 55.8628013, new Date(1386879129000L) /* Fri Dec 13 00:12:09 MSK 2013 */ , 210, 7, 18, "9", new Date(1386879588014L) /* Fri Dec 13 00:19:48 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5009098, 55.8627715, new Date(1386879131000L) /* Fri Dec 13 00:12:11 MSK 2013 */ , 180, 7, 17, "10", new Date(1386879588013L) /* Fri Dec 13 00:19:48 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5008963, 55.8627541, new Date(1386879132000L) /* Fri Dec 13 00:12:12 MSK 2013 */ , 149, 6, 17, "11", new Date(1386879588012L) /* Fri Dec 13 00:19:48 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5008971, 55.862732, new Date(1386879149000L) /* Fri Dec 13 00:12:29 MSK 2013 */ , 78, 6, 16, "12", new Date(1386879581964L) /* Fri Dec 13 00:19:41 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5008971, 55.862732, new Date(1386879150000L) /* Fri Dec 13 00:12:30 MSK 2013 */ , 59, 9, 16, "13", new Date(1386879581964L) /* Fri Dec 13 00:19:41 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5014078, 55.862925, new Date(1386879163000L) /* Fri Dec 13 00:12:43 MSK 2013 */ , 73, 8, 16, "14", new Date(1386879581961L) /* Fri Dec 13 00:19:41 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5014455, 55.8629315, new Date(1386879164000L) /* Fri Dec 13 00:12:44 MSK 2013 */ , 86, 6, 16, "15", new Date(1386879581967L) /* Fri Dec 13 00:19:41 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5014755, 55.8629343, new Date(1386879165000L) /* Fri Dec 13 00:12:45 MSK 2013 */ , 97, 7, 16, "16", new Date(1386879581967L) /* Fri Dec 13 00:19:41 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5015348, 55.862928, new Date(1386879168000L) /* Fri Dec 13 00:12:48 MSK 2013 */ , 81, 8, 16, "17", new Date(1386879581963L) /* Fri Dec 13 00:19:41 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5015685, 55.8629273, new Date(1386879169000L) /* Fri Dec 13 00:12:49 MSK 2013 */ , 65, 9, 16, "18", new Date(1386879581961L) /* Fri Dec 13 00:19:41 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5016046, 55.8629348, new Date(1386879170000L) /* Fri Dec 13 00:12:50 MSK 2013 */ , 49, 11, 16, "19", new Date(1386879581960L) /* Fri Dec 13 00:19:41 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5017563, 55.8630283, new Date(1386879174000L) /* Fri Dec 13 00:12:54 MSK 2013 */ , 39, 15, 16, "20", new Date(1386879581966L) /* Fri Dec 13 00:19:41 MSK 2013 */),
      new GPSData("o4263058740375270393", "356307040867151", 37.5022823, 55.8633693, new Date(1386879186000L) /* Fri Dec 13 00:13:06 MSK 2013 */ , 42, 13, 16, "21", new Date(1386879581960L) /* Fri Dec 13 00:19:41 MSK 2013 */)
    )

    testWithData(datas.iterator, 1, 2)
  }

  @Test
  def test3() {
    testWithData("/report3.bson.gz", 2, 3)
  }

  private[this] def testWithData(s: String, parks: Int, total: Int) {
    val reader = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream(s)))

    val datas = reader.iterator.map(GPSDataConversions.fromDbo)
    testWithData(datas, parks, total)
  }

  private def testWithData(datas: Iterator[GPSData], parks: Int, total: Int) {
    val parkings = new MovingStatesExtractor().extractMovingStates(datas).toSeq

    for (parking: MovingState <- parkings) {
      println(parking)
    }

    Assert.assertEquals(parks, parkings.count(_.isParking))
    Assert.assertEquals(total, parkings.size)

    for (state <- parkings.sliding(2)) {
      Assert.assertTrue(state.exists(_.isParking))
      Assert.assertTrue(state.exists(!_.isParking))
      Assert.assertTrue("state " + state(0) + " last  must be first for" + state(1), state(0).last == state(1).first)
    }

    val movings = parkings.filterNot(_.isParking)

    for (moving <- movings) {
      Assert.assertTrue(moving + " distance must be >0", moving.distance > 0)
      Assert.assertTrue(moving + " maxSpeed must be >0", moving.maxSpeed > 0)
    }
  }

  @Test
  def testStillMoving() {
    val date = 1410000000000L
    val gen = new Posgenerator("otestuid", date)
    gen.defaultCount = 10

    val duration = 1000000
    val history = (Iterator(gen.last) ++
      gen.genParking(duration millis) ++
      gen.genMoving(duration millis, increment = 0.0015)).toIndexedSeq
    val res = new MovingStatesExtractor().extractMovingStates(
      history.iterator
    ).toIndexedSeq


    Assert.assertTrue(res(0).isParking)
    Assert.assertEquals(date, res(0).firstTime.getTime)
    Assert.assertEquals(date + duration, res(0).lastTime.getTime)
    Assert.assertFalse(res(1).isParking)
    Assert.assertEquals(date + duration, res(1).firstTime.getTime)
    Assert.assertEquals(date + 2 * duration, res(1).lastTime.getTime)
    Assert.assertEquals(history.size, res(0).pointsCount + res(1).pointsCount)
  }

  @Test
  def testMarriage() {

    val reader = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/o963268714566195949.bson.gz")))

    //val ldate = new Date(14, 9, 4, 11, 0, 0)
    val update = new Date(114, 9, 4, 13, 0, 0)

    val datas = reader.iterator.map(GPSDataConversions.fromDbo).toIndexedSeq.sortBy(_.time)
      // .dropWhile(_.time.before(ldate))
      .takeWhile(_.time.before(update))
    println("datas.size=" + datas.size)
    val parkings = new MovingStatesExtractor().extractMovingStates(datas.iterator).toSeq

    for (parking: MovingState <- parkings) {
      println(parking + " " + parking.first + " - " + parking.last)
    }

    Assert.assertEquals(3, parkings.count(_.isParking))
    Assert.assertEquals(5, parkings.size)

  }


  @Test
  def testPointCount1(): Unit = {
    val points = IndexedSeq(
      new GPSData("o4297310092419035023", "356307043981868", 37.8286944, 55.7655424, new Date(1442471164110L), 12, 201, 11, "7 к2 с2, Свободный проспект, Ивановское, Москва", new Date(1442471290082L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8286272, 55.765472, new Date(1442471166110L), 10, 188, 11, "7 к2 с2, Свободный проспект, Икое, Москва", new Date(1442471290081L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8286560, 55.7654208, new Date(1442471169100L), 7, 217, 11, "7 к2 с2, Свободный проспект, Ивановское, Москва", new Date(1442471290081L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8285984, 55.7653888, new Date(1442471171100L), 8, 250, 11, "7 к2 с2, Свободный проспект, Ивановское, Москва", new Date(1442471290081L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8285056, 55.7653824, new Date(1442471173100L), 7, 282, 10, "7 к2 с2, Свободный проспект, Ивановское, Москва", new Date(1442471290081L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8284096, 55.7653504, new Date(1442471294030L), 0, 332, 11, "7 к2 с2, Свободный проспект, Ивановское, Москва", new Date(1442471914993L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8284096, 55.7653504, new Date(1442471414030L), 0, 332, 12, "7 к2 с2, Свободный проспект, Ивановское, Москва", new Date(1442471914993L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8284096, 55.7653504, new Date(1442471534030L), 0, 332, 12, "7 к2 с2, Свободный проспект, Ивановское, Москва", new Date(1442471914993L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8284096, 55.7653504, new Date(1442471654030L), 0, 332, 12, "7 к2 с2, Свободный проспект, Ивановское, Москва", new Date(1442471914993L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8284096, 55.7653504, new Date(1442471774030L), 0, 332, 14, "7 к2 с2, Свободный проспект, Ивановское, Москва", new Date(1442471914993L))
    )
    val states = new MovingStatesExtractor().extractMovingStates(points.iterator).toSeq

    paintItnervals(points, states.iterator.buffered)

    Assert.assertEquals(points.size, states.map(_.pointsCount).sum)
  }

  @Test
  def testPointCount2(): Unit = {
    val points = Seq(new GPSData("o4297310092419035023", "356307043981868", 37.756656, 55.8002624, new Date(1442481395040L), 0, 0, 18, "1", new Date(1442481444111L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.7566304, 55.80016, new Date(1442481507040L), 6, 307, 18, "2", new Date(1442482178357L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.7565344, 55.800224, new Date(1442481511040L), 6, 353, 18, "3", new Date(1442482174741L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.7565184, 55.8002432, new Date(1442481512040L), 7, 3, 18, "4", new Date(1442482174741L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.75648, 55.8003968, new Date(1442481519040L), 6, 21, 18, "5", new Date(1442482174741L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.7565952, 55.8004608, new Date(1442481526030L), 8, 88, 18, "6", new Date(1442482174741L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.7567264, 55.8004736, new Date(1442481528030L), 11, 101, 17, "7", new Date(1442482174741L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.757568, 55.8003456, new Date(1442481541020L), 18, 98, 18, "8", new Date(1442482174741L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.7580544, 55.8003072, new Date(1442481547020L), 20, 86, 17, "9", new Date(1442482174740L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.758864, 55.800352, new Date(1442481555030L), 25, 86, 16, "10", new Date(1442482174740L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.7596672, 55.800384, new Date(1442481564030L), 18, 88, 14, "11", new Date(1442482174740L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.7605632, 55.8003456, new Date(1442481571030L), 31, 93, 14, "12", new Date(1442482174740L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.7613792, 55.8002944, new Date(1442481581030L), 8, 93, 16, "13", new Date(1442482174740L))
    )
    val states = new MovingStatesExtractor().extractMovingStates(points.iterator).toSeq
    Assert.assertEquals(points.size, states.map(_.pointsCount).sum)
  }

  @Test
  def testPointCount3(): Unit = {
    val points = IndexedSeq(
      new GPSData("o4297310092419035023", "356307043981868", 37.8336384, 55.7646528, new Date(1442485761150L), 7, 192, 14, "1", new Date(1442486101547L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8336576, 55.764512, new Date(1442485881150L), 0, 6, 13, "2", new Date(1442486101547L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8335488, 55.7660608, new Date(1442485969150L), 17, 6, 14, "3", new Date(1442486101547L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.833568, 55.7663872, new Date(1442485976150L), 14, 343, 14, "4", new Date(1442486101547L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.833568, 55.7664192, new Date(1442485977150L), 12, 354, 16, "5", new Date(1442486101547L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8335584, 55.7664576, new Date(1442485979150L), 8, 334, 15, "6", new Date(1442486101547L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8335584, 55.7664768, new Date(1442485980150L), 7, 349, 14, "7", new Date(1442486101547L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.833568, 55.7665152, new Date(1442485982150L), 8, 8, 14, "8", new Date(1442486101547L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.833568, 55.7666112, new Date(1442485986150L), 8, 338, 14, "9", new Date(1442486101547L)),
      new GPSData("o4297310092419035023", "356307043981868", 37.8336256, 55.7665856, new Date(1442486106150L), 0, 352, 17, "10", new Date(1442486112327L))
    )
    val states = new MovingStatesExtractor().extractMovingStates(points.iterator).toSeq
    Assert.assertEquals(points.size, states.map(_.pointsCount).sum)
  }

  @Test
  def testTicket644Mismatch(): Unit = {

    def reader = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/ticket644parkingDistanceMismatch.gz")))
    //def reader = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/parkingDistanceMismatchData.bson.gz")))

    val from = 0; val to = Int.MaxValue
    //val from = 1025;
    //val to = 1035
    val datas = reader.iterator.map(GPSDataConversions.fromDbo).toIndexedSeq.sortBy(_.time)
      .slice(from, to)
    //.slice(637, 863)
    //.slice(637, 650)

    printGPS(datas)

    val first = datas.head
    val last = datas.last
    println(first)
    println(last)
    println("datas.length = " + datas.length)
    println("sumdistance = " + DistanceUtils.sumdistance(datas))
    println("tdist = " + (last.data.as[Double]("tdist") - first.data.as[Double]("tdist")))
    println("priv tdist = " + (last.privateData.as[Double]("tdist") - first.privateData.as[Double]("tdist")))

    val states = new MovingStatesExtractor().extractMovingStates(datas.iterator).toList
    val movings = states.collect { case e: Moving => e }.toList

    println("states=\n  "+states.mkString("\n  "))

    paintItnervals(datas, states.iterator.buffered)

    println("datas.length = " + datas.length)
    println("sumdistance = " + DistanceUtils.sumdistance(datas))
    println("tdist = " + (last.data.as[Double]("tdist") - first.data.as[Double]("tdist")))
    println("priv tdist = " + (last.privateData.as[Double]("tdist") - first.privateData.as[Double]("tdist")))
    println("moving distances: =" + states.map(_.distance).sum)
    println("moving points: =" + states.map(_.pointsCount).sum + "(" + (datas.length - states.map(_.pointsCount).sum) + ")")
    println("statesCounr: =" + states.size)


    //paintItnervals(datas, new MovingStatesExtractor().extractMovingStates(datas.iterator).buffered)


  }

  def printGPS(datas: Iterable[GPSData]): Unit = {
    println(datas.zipWithIndex.map({ case (g, i) =>
      g.placeName = (i + 1).toString
      DataHelpers.toScalaCode(g, true)
    }).mkString("IndexedSeq(\n", ",\n", "\n)"))
  }

  def paintItnervals(datas: IndexedSeq[GPSData], statesIterator: BufferedIterator[MovingState], begin: Int = 0): Unit = {

    var sumByStates = 0;

    for ((point, i) <- datas.zip(Stream.from(begin))) {

      print(i)
      print("\t")
      print(point.time)

      def cur = statesIterator.head


      def checkCur(): Any = {
        print("\t")
        if (!cur.first.time.before(point.time)) {
          print(cur.getClass.getSimpleName + " Starts:")
          print(cur.first.time)
        } else
        if (!cur.last.time.after(point.time)) {
          print(cur.getClass.getSimpleName + " Ends:")
          print(cur.last.time)
          print(" ")
          sumByStates = sumByStates + cur.pointsCount
          print(sumByStates)
          val hasNext = statesIterator.hasNext
          if (hasNext) {
            statesIterator.next()
            if (statesIterator.hasNext)
              checkCur()
          }
        }
      }

      val hasNext = statesIterator.hasNext
      if (hasNext)
        checkCur()

      println()


    }
  }
}
