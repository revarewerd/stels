package ru.sosgps.wayrecall.odsmosru

import java.io.{BufferedOutputStream, Closeable, File, OutputStream}
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong

import kamon.Kamon
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions}
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.concurrent.Semaphore
import ru.sosgps.wayrecall.utils.io.{DboReader, DboWriter}

import scala.beans.BeanProperty
import scala.collection.AbstractIterator
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.control

/**
  * Created by nickl on 07.10.14.
  */
class TemporaryStorage(dir: Path) extends grizzled.slf4j.Logging with Closeable {
  require(Files.isDirectory(dir), dir + " must be a directory")

  @volatile
  private var id = 0L

  @volatile
  private var currentOut: Option[(Path, DboWriter)] = None

  private def getWriter: DboWriter = currentOut match {
    case Some(co) => co._2
    case None => synchronized {
      currentOut match {
        case Some(co) => co._2
        case None =>
          initCurrentOut()
          currentOut.get._2
      }
    }
  }

  def initCurrentOut() {
    val prevOut = currentOut
    var i = id;
    id = Iterator.continually({
      i += 1; i
    }).find(i => !Files.exists(storePath(i))).get
    currentOut = Some(storePath(id), new DboWriter(new BufferedOutputStream(Files.newOutputStream(storePath(id)))))
    prevOut.foreach(_._2.close())
  }

  private def storePath(i: Long): Path = {
    dir.resolve(i + ".bson")
  }

  def storeAll(all: TraversableOnce[GPSData]) = all.foreach(store)

  def store(gps: GPSData): Unit = synchronized {
    debug("storing " + gps)
    getWriter.write(GPSDataConversions.toMongoDbObject(gps))
  }

  def retrieveBunch: Iterator[GPSData] = {
    val lastWrited = closePrevious().map(_._1)

    info("requested bunch for " + lastWrited)

    lastWrited match {
      case None => Iterator.empty
      case Some(path) => {
        val dboi = new DboReader(Files.newInputStream(path)).iterator.map(GPSDataConversions.fromDbo)
        new Iterator[GPSData] {
          override def hasNext: Boolean = {
            val hn = dboi.hasNext
            if (!hn) {
              info("deleting " + path)
              Files.delete(path)
            }
            hn
          }

          override def next(): GPSData = dboi.next()
        }
      }
    }
  }

  private def closePrevious(): Option[(Path, DboWriter)] = synchronized {
    val prevout = currentOut
    currentOut = None
    prevout.foreach(_._2.close())
    prevout
  }

  override def close(): Unit = {
    closePrevious()
  }
}

class RepeatedlyResender(
                          tempStorage: TemporaryStorage,
                          sendMethod: (GPSData) => Unit,
                          resendingTask: Executor,
                          sendingExecutor: Executor,
                          parallelSending: Int = 5
                        ) extends grizzled.slf4j.Logging {

  @volatile
  var lastResendTime = 0L

  @BeanProperty
  var waitSec = 60 * 10

  private val checkForResendLock = new Semaphore(1)

  private val parallelSendingLimit = new Semaphore(parallelSending)

  def safeSend(gpsdata: GPSData): Unit = {
    try {
      sendMethod(gpsdata)
      checkForResend()
    }
    catch {
      case e: Exception =>
        Kamon.metrics.counter("odsmosru-resender-exception").increment()
        tempStorage.store(gpsdata)
        throw e
    }
  }

  def checkForResend(): Unit = {
    if (checkForResendLock.tryAcquire(1)) {
      val now = System.currentTimeMillis()
      val interval = now - lastResendTime
      val minInterval = 1000 * waitSec
      debug(s"now = $now, lastResendTime=$lastResendTime, interval = $interval, minInterval = $minInterval")

      if (interval > minInterval) {
        lastResendTime = now
        try {
          resendingTask.execute(new Runnable {
            override def run(): Unit = try {
              debug("resending stared")
              tempStorage.retrieveBunch.foreach(gps => {

                parallelSendingLimit.acquire()

                sendingExecutor.execute(utils.runnable(try {
                  debug("resending safeSend: " + gps)
                  safeSend(gps)
                  debug("resending safeSend succeed " + gps)
                } catch {
                  case e: Exception => error("exception in resending exception", e)
                } finally parallelSendingLimit.release()))

                //                control.Exception.ignoring(classOf[Exception]) {
                //                                  debug("resending safeSend: " + gps)
                //                                  safeSend(gps)
                //                                  debug("resending safeSend succeed " + gps)
                //                }
              }
              )

            }
            catch {
              case e: Exception => error("tempStorage.retrieveBunch exception", e)
            }
            finally {
              //if (checkForResendLock.availablePermits() <= 0) {
              debug(s"releasing lock, availablePermits=${checkForResendLock.availablePermits()}")
              checkForResendLock.release()
              //}
            }

            debug("resending completed")
          })
          debug("resending scheduled")
        } catch {
          case e: Exception => {
            error("error:", e)
            debug("releasing lock because of exception")
            checkForResendLock.release()
          }
        }
      } else checkForResendLock.release()
    }


  }


}


