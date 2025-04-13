package ru.sosgps.wayrecall.billing.retranslator
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.NISClient

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

/**
 * Created by nickl on 01.10.14.
 */
trait RetranslationTask {

  def name: String

  def host: String

  def port: Int

  def uids: Seq[String]

  def completedCount: Int

  def totalCount: Int

  def status: String

  def from: Date

  def to: Date

  def kill()

  def start()
}

class NISRetranslationTask(val name: String,
                           val host: String,
                           val port: Int,
                           val uids: Seq[String],
                           val from: Date, val to: Date,
                           val totalCount: Int,
                           data: Iterator[GPSData]
                            ) extends RetranslationTask with grizzled.slf4j.Logging {

  private var _completedCount = new AtomicInteger(0)

  @transient
  private var stopped = false

  @transient
  private var error = ""

  override def completedCount: Int = _completedCount.get()

  override def kill(): Unit = stopped = true

  override def status: String = if (error.nonEmpty) error else if (completedCount < totalCount) "в обработке" else "выполнен"

  override def start(): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = try {

        val nis = new NISClient
        for(gps <- data if gps.containsLonLat()){
          //Await.result(send(rule._1.getHostName, rule._1.getPort, v1), 30 seconds)
          val r = Await.result(nis.send(host, port, gps), 30 seconds)
          if (r._1 != 200) {
            warn("error answer " + r._1 + " for " + gps + " :" + r._2)
            throw new IllegalStateException("answer " + r._1)
          }
          _completedCount.incrementAndGet()
        }

        debug("completed")

      } catch {
        case e: Throwable => {
          warn("error in retranslation", e)
          error = Option(e.getMessage).getOrElse(e.toString)
        }
      }
    }, "NISRetranslationTask").start()
  }
}
