package ru.sosgps.wayrecall.utils.concurrent

import java.util.{Timer, TimerTask}

import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils.CollectionUtils.additionalMethods
import ru.sosgps.wayrecall.utils.WeakTracer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration.Duration

/**
 * Created by nickl on 03.02.15.
 */
class GPSDataWindow2(val limit: Int, duration: Duration, timer: Timer) extends grizzled.slf4j.Logging {

  var onDequeued: (GPSData) => Unit = a => {}

  def this(limit: Int, duration: Duration, timer: Timer, onDequeued: (GPSData) => Unit) = {
    this(limit: Int, duration: Duration, timer: Timer)
    this.onDequeued = onDequeued
  }

  private val queue: mutable.PriorityQueue[Item] = new mutable.PriorityQueue[Item]()(Ordering.by((item: Item) => item.gps.time).reverse)

  private def task: TimerTask = new TimerTask {
    override def run(): Unit = GPSDataWindow2.this.synchronized {
      try {

        val splitline = System.currentTimeMillis() - duration.toMillis
        val items = dequeWhile(_.queueTime <= splitline)
        //debug("dequeued:"+items.map(i => (i.gps.imei, i.gps.time.getTime,i.gps.insertTime.getTime, i.queueTime)))
        items.foreach(i => {
          WeakTracer.trace(i.gps, "dequeuing by time")
          processDequeued(i)
        })

        val testLine = System.currentTimeMillis() - duration.toMillis * 5
        val lastOption = queue.toStream.filter(_.queueTime < testLine).maxByOpt(_.gps.time)

        lastOption.foreach(i => {
          debug("dequeuing up to " + i)
          dequeUpTo(_ eq i).foreach(i => {
            WeakTracer.trace(i.gps, "dequeuing by popping")
            debug("dequeuing by popping" + i)
            processDequeued(i)
          })
        })

        //        if (queue.exists(i => i.queueTime < testLine))
        //          debug(s"strange queue size=${queue.size} :" + queue.toIndexedSeq.sortBy(_.gps.time).mkString("\n    ", "\n    ", "\n"))

        val nextTime = queue.headOption.map(head => head.queueTime - splitline).filter(_ >= 0).getOrElse(duration.toMillis)
        //debug("nextTime = "+nextTime)
        timer.schedule(task, nextTime)
      } catch {
        case e: Exception => {
          warn("exception", e)
          timer.schedule(task, duration.toMillis)
        }
      }
    }
  }

  timer.schedule(task, duration.toMillis)

  private def processDequeued(i: Item): Unit = {
    WeakTracer.trace(i.gps, "processDequeue")
    i.promisedData.success(i.gps);
    onDequeued(i.gps)
    WeakTracer.trace(i.gps, "dequeued")
  }

  def enqueue(gps: GPSData): Future[GPSData] = synchronized {
    val promisedData = scala.concurrent.promise[GPSData]()
    queue.enqueue(Item(gps, System.currentTimeMillis(), promisedData))
    WeakTracer.trace(gps, "enqueued")
    val toTake = queue.size - limit

    if (toTake > 0) {
      debug("evicting " + toTake + " for " + gps.imei)
      (0 to toTake).foreach(_ => processDequeued(queue.dequeue()))
    }


    promisedData.future
  }

  def dequeueAll() = synchronized {
    dequeWhile(_ => true).foreach(i => processDequeued(i))
  }

  private def dequeUpTo(matches: (Item => Boolean)): List[Item] = {
    var triggered = false;
    def cond(i: Item): Boolean = if (triggered) false
    else {
      if (matches(i)) {
        triggered = true
      }
      true
    }
    dequeWhile(cond)
  }

  private def dequeWhile(pred: (Item => Boolean)): List[Item] = synchronized {
    if (queue.isEmpty)
      return List.empty
    val result = new ListBuffer[Item]
    while (!queue.isEmpty && pred(queue.head)) {
      result += queue.dequeue()
    }
    result.toList
  }

  case class Item(gps: GPSData, queueTime: Long, promisedData: Promise[GPSData]) {
    override def toString: String = s"Item(queueTime = $queueTime, gps = $gps)"
  }


}
