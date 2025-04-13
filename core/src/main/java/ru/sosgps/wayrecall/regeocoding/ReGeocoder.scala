package ru.sosgps.wayrecall.regeocoding

import scala.concurrent.Future

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.08.13
 * Time: 12:54
 * To change this template use File | Settings | File Templates.
 */
trait ReGeocoder {

  var enabled:Boolean

  /**
   *
   * @param lon
   * @param lat
   * @return String representation of corresponding place, or Exception if one occurred.
   *         [[java.util.NoSuchElementException]] if there are no regeocoding data for this place
   */
  def getPosition(lon: Double, lat: Double): Future[Either[java.lang.Throwable, String]]

  def setTimeout(timeout:Int)

}
