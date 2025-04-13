package ru.sosgps.wayrecall.utils.concurrent

import java.util.concurrent.{TimeUnit, Semaphore => JSemaphore}

import ru.sosgps.wayrecall.utils.concurrent.FuturesUtils.extensions

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.01.13
 * Time: 16:15
 * To change this template use File | Settings | File Templates.
 */
class Semaphore(val permits: Int) extends JSemaphore(permits) {

  def acquired[T](f: => T): T = {
    acquire()
    try {
      f
    }
    finally {
      release()
    }
  }

  def tryAcquired[T](f: => T): Option[T] = {
    if (tryAcquire()) {
      try {
        Some(f)
      }
      finally {
        release()
      }
    }
    else
      None
  }

  def tryAcquiredMills[T](time:Long)(f: => T): Option[T] = {
    if (tryAcquire(time, TimeUnit.MILLISECONDS)) {
      try {
        Some(f)
      }
      finally {
        release()
      }
    }
    else
      None
  }

  def acquiriedFuture[T](f: => Future[T])(implicit ec: ExecutionContext = ScalaExecutorService.implicitSameThreadContext) : Future[T] = {

    acquire()
    try {
      f.andFinally(_ => release())
    } catch {
      case e: Throwable =>
        release()
        throw e
    }

  }

}


class LimitedSimultaneous(val maxLimit: Int) {

  @volatile
  private[this] var passed = 0;

  def processIfCan[T](f: => T): Option[T] = {
    if (passed < maxLimit) {
      synchronized {
        passed = passed + 1
      }
      try {
        Some(f)
      }
      finally {
        synchronized {
          passed = passed - 1
        }
      }
    }
    else {
      None
    }
  }


}
