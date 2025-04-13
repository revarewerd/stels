package ru.sosgps.wayrecall.monitoring

import ru.sosgps.wayrecall.core.DistanceUtils._
import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData}
import ru.sosgps.wayrecall.utils.tryNumerics

/**
  * Created by ivan on 12.06.17.
  */
package object processing {
  def distanceBetweenPackets(f0: GPSData, f1: GPSData) = {
    kmsBetween(f0,f1)
  }
}
