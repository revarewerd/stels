package ru.sosgps.wayrecall.regeocoding

import java.util.concurrent.{TimeUnit, ThreadPoolExecutor, SynchronousQueue}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils.PerMinuteLogger

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class AsyncRegeocoder(regeoCoder: ReGeocoder) extends grizzled.slf4j.Logging {

  private[this] val regeocodingPool = {
    val blockingQueue = new SynchronousQueue[Runnable]();
    val rejectedExecutionHandler = new ThreadPoolExecutor.DiscardPolicy()
    new ThreadPoolExecutor(1, 10, 180L, TimeUnit.SECONDS, blockingQueue, rejectedExecutionHandler);
  }

  def regeocode(gpsdata: GPSData, callback: (Either[Throwable, String]) => Any) {
    if (regeoCoder.enabled) {
      regeocodingPool.execute(new Runnable {
        def run() {
          val name = loadPositionName(gpsdata)
          callback(name)
        }
      })
    }
  }


  def loadPositionName(gpsdata: GPSData): Either[java.lang.Throwable, String] = loadPositionName(gpsdata: GPSData, true)

  private[this] val cordsfrommongoFL = new PerMinuteLogger

  def loadPositionName(gpsdata: GPSData, update: Boolean): Either[java.lang.Throwable, String] = {

    if (gpsdata.placeName != null)
      Right(gpsdata.placeName)
    else if (!regeoCoder.enabled)
      Left(new UnsupportedOperationException("regeoCoder is disabled"))
    else {
      Await.result(regeoCoder.getPosition(gpsdata.lon, gpsdata.lat), Duration(20, TimeUnit.SECONDS))
    }

  }


}
