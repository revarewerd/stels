package ru.sosgps.wayrecall.utils.concurrent

import java.util.concurrent.LinkedBlockingDeque
import javax.annotation.{PostConstruct, PreDestroy}

import kamon.Kamon
import ru.sosgps.wayrecall.utils

import scala.collection.mutable

/**
 * Created by nickl on 19.01.15.
 */
trait BlockingActor[T] extends grizzled.slf4j.Logging{
  import scala.collection.JavaConversions.bufferAsJavaList
  private val dboqueue = new LinkedBlockingDeque[T](10000)

  private val gauge =
    Kamon.metrics.gauge(s"blockingActor-${this.getClass.getSimpleName}",
      Map("identity" -> System.identityHashCode(this).toString)
    )(
      dboqueue.size().toLong
    )



  protected def actorThreadName: String

  def accept(dbo: T): Boolean = {
    dboqueue.offer(dbo)
  }

  protected def drainTo(buffer: mutable.Buffer[_ >: T]): Int = {
    dboqueue.drainTo(bufferAsJavaList(buffer))
  }

  protected def processMessage(value: T): Unit

  private[this] lazy val writingThread = new Thread(utils.runnable{

    debug("writingThread started")

    try {
      while (true) {
        val head = dboqueue.take()
        processMessage(head)
      }
    }
    catch {
      case e: InterruptedException => info(actorThreadName + " interrupted")
      case e: Exception => warn(actorThreadName+" Exception", e)
    }


  },actorThreadName)

  @PostConstruct
  def start(): Unit ={
    writingThread.start()
  }

  @PreDestroy
  def stop(): Unit = {
    writingThread.interrupt()
    writingThread.join(2000)
  }

  def qsize = dboqueue.size()


}