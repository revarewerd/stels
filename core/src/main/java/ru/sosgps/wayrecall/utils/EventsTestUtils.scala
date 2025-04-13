package ru.sosgps.wayrecall.utils

import java.util.concurrent.{BlockingQueue, ArrayBlockingQueue}

import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.events.{Event, EventTopic, EventsStore}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.{DurationInt, Duration}

/**
 * Created by nickl on 09.02.14.
  * @see [[ru.sosgps.wayrecall.testutils]]
 */
object EventsTestUtils {
  @deprecated("use collectEventsQueue")
  def collectEvents[T <: Event](es1: EventsStore, topic: EventTopic[T]) = {
    val aggregatedEvents = new ArrayBuffer[T]()
    es1.subscribe(topic, (um: T) => aggregatedEvents += um)
    aggregatedEvents
  }

  def collectEventsQueue[T <: Event](es1: EventsStore, topic: EventTopic[T], filter: (T) => Boolean = (e: T) => true) = {
    val aggregatedEvents = new ArrayBlockingQueue[T](100, true)
    es1.subscribe(topic, (um: T) => if (filter(um)) aggregatedEvents.offer(um))
    aggregatedEvents
  }

  implicit class queueUtlis[T](aggregatedEvents: BlockingQueue[T]){

    def queueToStream(d: Duration): Stream[T] = queueToStream(d,d)

    def queueToStream(d: Duration, others:Duration): Stream[T] = {
      val poll = aggregatedEvents.poll(d.length, d.unit)
      if(poll == null) Stream.empty else poll #:: queueToStream(others)
    }
  }

  def waitUntilDuration(cond: => Boolean, limit: Duration, interval: Duration = 30.milli) : Unit
  = waitUntil(cond, limit.toMillis, interval.toMillis)

  def waitUntil(cond: => Boolean, limit: Long, interval: Long = 30L): Unit = {
    val startTime = System.currentTimeMillis()

    while (!cond) {
      if (System.currentTimeMillis() - startTime > limit)
        throw new IllegalStateException("timeout")
      Thread.sleep(interval)
    }
  }

}
