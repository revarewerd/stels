package ru.sosgps.wayrecall.utils.concurrent

import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.{Timer, TimerTask}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.Duration
import scala.util.{Success, Try}


object FuturesUtils extends grizzled.slf4j.Logging {


  implicit class extensions[T](val fut: Future[T]) extends AnyVal {
    def withTimeout(after: Duration)(implicit ec: ExecutionContext = ScalaExecutorService.implicitSameThreadContext) = FuturesUtils.withTimeout(fut, after)

    def andFinally(fin: Try[T] => Unit)(implicit ec: ExecutionContext = ScalaExecutorService.implicitSameThreadContext) = {
      fut.onComplete(fin)
      fut
    }
  }

  implicit class promiseExtension[T](val promise: Promise[T]) extends AnyVal {

    def delegateBy[F](conversion: (F) => T)(implicit ec: ExecutionContext): Promise[F] = {
      val fpromise = Promise[F]
      promise.completeWith(fpromise.future.map(conversion))
      fpromise
    }


  }

  private val timer = new Timer("FuturesUtilsTimer");

  private val cancelledCount = new AtomicInteger(0)

  def scheduleTimeout(promise: Promise[_], after: Duration) = {
    val timerTask = new TimerTask() {
      def run() {
        promise.failure(new TimeoutException("Operation timed out after " + after.toMillis + " millis"))
      }
    }
    timer.schedule(timerTask, after.toMillis)
    timerTask
  }

  var TASKS_BEFORE_PURGE: Int = 1000


  private def checkAndPurge(): Unit = {
    val lastKnown = cancelledCount.get()
    if (lastKnown > TASKS_BEFORE_PURGE) {
      if (cancelledCount.compareAndSet(lastKnown, 0)) {
        debug("purging timer")
        timer.purge()
      } else checkAndPurge()
    }

  }

  def withTimeout[T](fut: Future[T], after: Duration)(implicit ec: ExecutionContext) = {
    val prom = Promise[T]()
    val timeout = scheduleTimeout(prom, after)
    val combinedFut = Future.firstCompletedOf(List(fut, prom.future))
    fut onComplete { case result => {
      //debug("cancelling timer:"+timeout)
      timeout.cancel()
      if (cancelledCount.incrementAndGet() > TASKS_BEFORE_PURGE) {
        checkAndPurge()
      }
    }
    }
    combinedFut
  }

  def runSafe[T](f: => Future[T]) = {
    try {
      f
    }
    catch {
      case e: Exception => Future.failed(e)
    }
  }

}


