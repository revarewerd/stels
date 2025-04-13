package ru.sosgps.wayrecall.tools

import java.nio.file.{Files, StandardCopyOption}

import com.google.common.io.ByteStreams
import org.junit.{Assert, Test}
import resource.managed
import ru.sosgps.wayrecall.core.GPSData

import scala.collection.immutable.Stream.Empty

/**
  * Created by nickl-mac on 26.03.16.
  */
class BsonGpsSenderTest {


  @Test
  def testAnHour(): Unit = {

    val file = Files.createTempFile("hdump", ".zip")
    Files.delete(file)
    try {
      Files.copy(this.getClass.getClassLoader.getResourceAsStream("hourdump.zip"), file)

      var time = Long.MinValue
      var count = 0;
      var imei: String = null

      val allInOrder = new BsonGpsSender().getAllInOrder(Seq(file.toAbsolutePath.toString))
      try {
        for (e <- allInOrder) {

          //if (imei != e.imei) {
          //println(s"${e.imei} - ${e.time} ${e.time.getTime}")
          //imei = e.imei


          if (e.time.getTime < time)
            Assert.fail(s"failed on $e")

          time = e.time.getTime
          count += 1
          //}
        }
      } finally {
        allInOrder.close()
      }

      //println("count = " + count)
      Assert.assertEquals(82005, count)


    }

    finally {
      Files.deleteIfExists(file)
    }

  }

  //  def checkInOrder(accumulated: Int, nowTime: Long, items: Stream[GPSData]): Int = {
  //    items match {
  //      case Empty => 0
  //      case (head #:: tail) =>
  //    }
  //  }


}
