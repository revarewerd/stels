package ru.sosgps.wayrecall.data.sleepers

case class DeviceLocation(lon:Double, lat:Double, precision: Double)

trait LBSLocator {
  def getLocation(lBS: LBS): DeviceLocation
}



//class YandexLBSLocator extends LBSLocator {
//  override def getLocation(lBS: LBS): DeviceLocation = {
//
//  }
//}