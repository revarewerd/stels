package ru.sosgps.wayrecall.concurrent

import java.util.{Date, Timer}

import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.Posgenerator
import ru.sosgps.wayrecall.utils.durationAsJavaDuration
import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.concurrent.GPSDataWindow2

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt

/**
 * Created by nickl on 03.02.15.
 */
class GPSDataWindowTest {

  private val timer = new Timer()

  @Test
  def testNumber(): Unit ={

    val pg = new Posgenerator("test", 1000000000000L)
    val datas = pg.genMoving(50 seconds,10).toIndexedSeq

    moveTime(datas(2).time, 7000)
    moveTime(datas(7).time, -23000)

    println(datas.map(_.time.getTime).mkString(", "))

    val dequed = new ListBuffer[GPSData]

    def onDeque(gps: GPSData): Unit ={
      dequed += gps
    }

    val window = new GPSDataWindow2(4, 400.millis, timer, onDeque)

    datas.foreach(window.enqueue)
    Assert.assertEquals(datas.size - window.limit, dequed.size)

    println(dequed.map(_.time.getTime).mkString(", "))
    EventsTestUtils.waitUntil(dequed.size == datas.size, 1000)
    val longs = dequed.map(_.time.getTime)
    println(longs.mkString(", "))
    Assert.assertEquals(
      List(
        1000000005000L, 1000000010000L, 1000000020000L, 1000000022000L, 1000000017000L, 1000000025000L, 1000000030000L, 1000000035000L, 1000000045000L, 1000000050000L
      ), longs)



  }

  def moveTime(time: Date, i: Int): Unit = {
    time.setTime(time.getTime + i)
  }



}
