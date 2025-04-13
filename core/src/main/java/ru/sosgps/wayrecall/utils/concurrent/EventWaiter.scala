package ru.sosgps.wayrecall.utils.concurrent

import com.google.common.cache._
import java.util.concurrent.{TimeoutException, TimeUnit}
import scala.concurrent.{Await, Future, Promise, promise}
import ru.sosgps.wayrecall.utils.funcLoadingCache
import scala.concurrent.duration._

/**
 * Created by nickl on 06.05.14.
 */
class EventWaiter[T <: AnyRef, V <: AnyRef] {

  private[this] val cache: Cache[T, V] = CacheBuilder.newBuilder()
    .expireAfterAccess(10, TimeUnit.SECONDS)
    .build[T, V]()

  private[this] val listeners = CacheBuilder.newBuilder()
    .expireAfterAccess(10, TimeUnit.SECONDS)
    .removalListener(new RemovalListener[T, Promise[V]] {
    def onRemoval(notification: RemovalNotification[T, Promise[V]]): Unit = {
      val p = notification.getValue
      p.tryFailure(new TimeoutException("cant wait anymore for " + notification.getKey))
    }
  })
    .buildWithFunction[T, Promise[V]](t => {promise[V]()})

  def publish(event: T, v: V): Unit = synchronized {
    cache.put(event, v)
    val p = listeners.getIfPresent(event)
    if (p != null) {
      p.success(v)
      listeners.invalidate(event)
    }
  }

  def awaiter(event: T): Future[V] = synchronized {
    val ifPresent = cache.getIfPresent(event)
    if (ifPresent != null)
      return Future.successful(ifPresent)

    return listeners.get(event).future
  }

  def await(key: T, mills: Int): V = {
    Await.result(awaiter(key), mills millis)
  }


}
