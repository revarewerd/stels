package ru.sosgps.wayrecall.core

import java.util.Date

import org.junit.Test

/**
  * Created by ivan on 17.07.17.
  */
class GPSUtilsTest {

  @Test
    def intervalSplitTest(): Unit = {
      val gpsData = Iterable(
        new Date(117,5,1,13,0),
        new Date(117,5,1,13,5),
        new Date(117,5,1,13,10),
        new Date(117,5,1,13,15),
        new Date(117,5,1,13,20),
        new Date(117,5,1,13,25),
        new Date(117,5,1,13,30),
        new Date(117,5,1,13,35),
        new Date(117,5,1,13,40)).map(date => new GPSData(null, null, date))

      val intervals = Iterable(
        SimpleTimeInterval(new Date(117,5,1,12,50),new Date(117,5,1,12,55)),
        SimpleTimeInterval(new Date(117,5,1,13,6),new Date(117,5,1,13,8)),
        SimpleTimeInterval(new Date(117,5,1,13,12),new Date(117,5,1,13,26)),
        SimpleTimeInterval(new Date(117,5,1,13,31),new Date(117,5,1,13,33)),
        SimpleTimeInterval(new Date(117,5,1,13,50),new Date(117,5,1,13,55))
      )

      GPSUtils.removeIntervals(gpsData, intervals).map(_.map(_.time)).foreach(println)
    }



}
