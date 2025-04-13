package ru.sosgps.wayrecall.processing.lazyseq

import java.util.{TimerTask, Timer}

import org.joda.time.DateTimeUtils

import scala.ref.WeakReference

/**
 * Created by nickl on 02.04.15.
 */
class TimeoutCachingGPSHistoryWalker(src: GPSHistoryWalker, timer: Timer, val evictingTimeOut: Long) extends CachingGPSHistoryWalker(src) {


  private def timerTask: TimerTask =
    new TimeoutCachingGPSHistoryWalkerTimerTask(this, timer)

  timer.schedule(timerTask, evictingTimeOut)

}

private class TimeoutCachingGPSHistoryWalkerTimerTask(
                                                       parent: WeakReference[TimeoutCachingGPSHistoryWalker],
                                                       timer: Timer,
                                                       evictingTimeOut: Long
                                                       ) extends TimerTask with grizzled.slf4j.Logging {

  def this(parent: TimeoutCachingGPSHistoryWalker, timer: Timer) =
    this(new WeakReference[TimeoutCachingGPSHistoryWalker](parent), timer, parent.evictingTimeOut)


  override def run(): Unit = {
    try {

      parent.get match {
        case Some(walker) =>

          walker.synchronized {

            val now = DateTimeUtils.currentTimeMillis()

            val splitLine = now - evictingTimeOut

            val latestAccess = walker.nodes.toList.flatMap(node => {
              val lastAccess = node.lastAccess
              if (lastAccess <= splitLine) {
                trace("timeout evicting " + node)
                node.evict();
                None;
              }
              else
                Some(lastAccess)
            }).reduceOption((l, l2) => math.min(l, l2)).getOrElse(now)

            timer.schedule(new TimeoutCachingGPSHistoryWalkerTimerTask(parent, timer, evictingTimeOut), (latestAccess - now) + evictingTimeOut)
          }
        case None =>
      }
    } catch {
      case e: Throwable => {
        error("throwable1:", e)
        try {
          timer.schedule(new TimeoutCachingGPSHistoryWalkerTimerTask(parent, timer, evictingTimeOut), evictingTimeOut)
        } catch {
          case e: Throwable =>
            error("throwable2:", e)
        }
      }
    }
  }


}
