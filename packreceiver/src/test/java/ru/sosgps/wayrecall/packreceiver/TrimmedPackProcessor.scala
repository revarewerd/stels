package ru.sosgps.wayrecall.packreceiver

import java.io.Serializable
import java.util.Date

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.DBPacketsWriter

import scala.concurrent.Future

/**
 * Created by nickl on 17.04.15.
 */
class TrimmedPackProcessor(_packetsWriter: DBPacketsWriter, nullInsertTime: Boolean = false) extends PackProcessor {
  override protected val incomingLogger = new IncomingGPSLogger() {
    override def apply(v1: GPSData): Future[GPSData] = Future.successful(v1)
  }

  override protected def instanceName(gpsdata: GPSData): String = ""

  override protected[this] def publishJMS(topic: String, gpsdata: Serializable): Unit = {}

  this.packetsWriter = _packetsWriter

  override protected def calcInsertTime(): Date = if (nullInsertTime) null else super.calcInsertTime()
}
