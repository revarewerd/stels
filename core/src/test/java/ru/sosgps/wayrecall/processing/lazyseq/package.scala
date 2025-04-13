package ru.sosgps.wayrecall.processing

import org.joda.time.DateTime
import ru.sosgps.wayrecall.core.GPSData

/**
 * Created by nickl on 14.04.15.
 */
package object lazyseq {

  def minutifyInsertionTime(shuffled: Seq[GPSData]): Unit ={
    var h = new DateTime(shuffled.head.time)
    shuffled.foreach(gps => {gps.insertTime = h.toDate; h = h.plusMinutes(1)})
  }

}
