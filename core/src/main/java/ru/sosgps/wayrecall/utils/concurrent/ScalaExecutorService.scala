package ru.sosgps.wayrecall.utils.concurrent

import java.util.concurrent.{Future => JFuture, _}
import java.util

import ru.sosgps.wayrecall.utils.errors.safeFuture

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future, promise}
import com.google.common.util.concurrent.MoreExecutors
import kamon.util.executors.ExecutorServiceMetrics
import ru.sosgps.wayrecall.utils.impldotChain

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 25.10.13
 * Time: 8:27
 * To change this template use File | Settings | File Templates.
 */
class ScalaExecutorService(val wrapped: ExecutorService) extends ExecutorService with grizzled.slf4j.Logging{

  private val entity = Option(try {
    ExecutorServiceMetrics.register(
      wrapped.asInstanceOpt[ThreadPoolExecutor].flatMap(_.getThreadFactory.asInstanceOpt[NamedThreadFactory]).map(_.namePrefix)
        .getOrElse(s"ScalaExecutorService-${wrapped.getClass.getSimpleName}-${System.identityHashCode(this)}"),
      wrapped
    )
  } catch {
    case e: Exception => error(s"ExecutorServiceMetrics exception for $wrapped", /*new IllegalArgumentException(*/e/*)*/)
      null
  })


  def this(corePoolSize: Int,
           maximumPoolSize: Int,
           keepAliveTime: Long,
           unit: TimeUnit,
           workQueue: BlockingQueue[Runnable],
           threadFactory: ThreadFactory /*= Executors.defaultThreadFactory()*/,
           handler: RejectedExecutionHandler/* = new ThreadPoolExecutor.AbortPolicy*/) = {
    this(new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler))
  }

  def this(poolName: String,
           corePoolSize: Int,
           maximumPoolSize: Int,
           allowCoreThreadTimeOut: Boolean,
           keepAliveTime: Long,
           unit: TimeUnit,
           workQueue: BlockingQueue[Runnable],
           handler: RejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy) = {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new NamedThreadFactory(poolName), handler)

    this.wrapped.asInstanceOf[ThreadPoolExecutor].allowCoreThreadTimeOut(allowCoreThreadTimeOut)

  }

  def future[T](f: => T): Future[T] = safeFuture{
    val prom = promise[T]();

    this.execute(new Runnable {
      def run() = try {
        val t: T = f;
        prom.success(t)
      } catch {
        case t: Throwable => prom.failure(t)
      }
    })

    prom.future
  }

  lazy val executionContext = ExecutionContext.fromExecutorService(this)

  def getActiveQueueSize: Int = {
    wrapped match {
      case tp: ThreadPoolExecutor => tp.getQueue.size();
      case _ => -1
    }
  }

  def execute(command: Runnable): Unit = wrapped.execute(command: Runnable)

  def execute(f: => Any): Unit = {
    this.execute(new Runnable {
      def run() = try {
        f
      } catch {
        case t: Throwable => t.printStackTrace()
      }
    })
  }

  def shutdown(): Unit = {
    wrapped.shutdown()
    entity.foreach(ExecutorServiceMetrics.remove)
  }

  def shutdownNow(): util.List[Runnable] = {
    val runnables = wrapped.shutdownNow()
    entity.foreach(ExecutorServiceMetrics.remove)
    runnables
  }

  def isShutdown: Boolean = wrapped.isShutdown

  def isTerminated: Boolean = wrapped.isTerminated

  def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = wrapped.awaitTermination(timeout: Long, unit: TimeUnit)

  def submit[T](task: Callable[T]): JFuture[T] = wrapped.submit[T](task: Callable[T])

  def submit[T](task: Runnable, result: T): JFuture[T] = wrapped.submit[T](task: Runnable, result: T)

  def submit(task: Runnable): JFuture[_] = wrapped.submit(task: Runnable)

  def invokeAll[T](tasks: util.Collection[_ <: Callable[T]]): util.List[JFuture[T]] = wrapped.invokeAll[T](tasks: util.Collection[_ <: Callable[T]])

  def invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): util.List[JFuture[T]] = wrapped.invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit)

  def invokeAny[T](tasks: util.Collection[_ <: Callable[T]]): T = wrapped.invokeAny[T](tasks: util.Collection[_ <: Callable[T]])

  def invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): T = wrapped.invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit)
}


class OrderedScalaExecutorService[K](wrapped: ExecutorService) extends ScalaExecutorService(wrapped) {

  def this(corePoolSize: Int,
           maximumPoolSize: Int,
           keepAliveTime: Long,
           unit: TimeUnit,
           workQueue: BlockingQueue[Runnable],
           threadFactory: ThreadFactory/* = Executors.defaultThreadFactory()*/,
           handler: RejectedExecutionHandler/* = new ThreadPoolExecutor.AbortPolicy*/) = {
    this(new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler))
  }

  def this(poolName: String,
           corePoolSize: Int,
           maximumPoolSize: Int,
           allowCoreThreadTimeOut: Boolean,
           keepAliveTime: Long,
           unit: TimeUnit,
           workQueue: BlockingQueue[Runnable],
           handler: RejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy) = {
    this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new NamedThreadFactory(poolName), handler)

    this.wrapped.asInstanceOf[ThreadPoolExecutor].allowCoreThreadTimeOut(allowCoreThreadTimeOut)

  }

  private val ordered = new OrderedExecutor[K](wrapped)

  def future[T](key: K)(f: => T): Future[T] = safeFuture {
    val prom = promise[T]();

    ordered.submit(key, new Runnable {
      def run() = try {
        val t: T = f;
        prom.success(t)
      } catch {
        case t: Throwable => prom.failure(t)
      }
    })

    prom.future
  }

  def execute(key: K)(f: => Any): Unit = {
    ordered.submit(key, new Runnable {
      def run() = try {
        f
      } catch {
        case t: Throwable => t.printStackTrace()
      }
    })
  }

  def contextBoundToKey(key: K): ExecutionContextExecutor = {
    scala.concurrent.ExecutionContext.fromExecutor(
      new Executor {
        override def execute(command: Runnable): Unit = ordered.submit(key, command)
      }, (t) => error("contextBoundToKey "+ key +" context failure", t)
    )
  }

}

object ScalaExecutorService extends grizzled.slf4j.Logging {

  import scala.concurrent.ExecutionContextExecutor

//  implicit lazy val globalContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.fromExecutor(
//    globalService, (t) => error("global context failure", t)
//  )

  lazy val globalService = new ScalaExecutorService("ScalaExecutorServiceGlobal", availableProcessors * 2, availableProcessors * 8, true, 120, TimeUnit.MINUTES, new LinkedBlockingQueue[Runnable](100000))

  def availableProcessors: Int = Runtime.getRuntime.availableProcessors()

  val sameThreadExecutor = new SameThreadExecutorService

  implicit val  implicitSameThreadContext = scala.concurrent.ExecutionContext.fromExecutor(sameThreadExecutor, t => error("implicitSameThreadContext context failure", t))
}

class SameThreadExecutorService extends ScalaExecutorService(MoreExecutors.sameThreadExecutor())