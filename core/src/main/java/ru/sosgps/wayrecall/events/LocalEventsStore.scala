package ru.sosgps.wayrecall.events

import com.googlecode.cqengine.CQEngine
import com.googlecode.cqengine.index.hash.HashIndex
import com.googlecode.cqengine.attribute.SimpleAttribute
import com.googlecode.cqengine.index.navigable.NavigableIndex
import com.googlecode.cqengine.query.QueryFactory._
import com.mongodb.casbah.Imports

import scala.beans.BeanProperty
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.setAsJavaSet
import scala.collection.JavaConversions.asJavaCollection
import java.util.concurrent.locks.ReentrantLock
import java.util.Collections
import scala.collection.mutable
import java.util.concurrent.ConcurrentHashMap
import collection.mutable.ArrayBuffer
import ru.sosgps.wayrecall.events.Event
import java.util
import java.util.concurrent.atomic.AtomicLong
import ru.sosgps.wayrecall.events.OnTargets
import ru.sosgps.wayrecall.events.OnTarget
import ru.sosgps.wayrecall.events.EventTopic

import scala.concurrent._

class WrappedEvent(val e: Event, val time: Long = System.currentTimeMillis()) {
  override def toString = "WrappedEvent(" + time + "," + e + ")"
}

class SA[O: Manifest, T: Manifest](name: String, f: O => T) extends SimpleAttribute[O, T](manifest[O].erasure.asInstanceOf[Class[O]], manifest[T].erasure.asInstanceOf[Class[T]], name) {
  def getValue(o: O) = f(o)
}

class LocalEventsStore extends EventsStore with EventSubscriber {

  private def sa[T: Manifest](name: String, f: WrappedEvent => T) = new SA[WrappedEvent, T](name, f)

  private val store = CQEngine.newInstance[WrappedEvent]();

  import java.lang.Long.{valueOf => lv}

  val timeAttr = sa[java.lang.Long]("time", e => lv(e.time))
  val targetIdAttr = sa("target", _.e.targetId)
  val typeAttr = sa("type", _.e.targetType)

  val timeIndex = NavigableIndex.onAttribute(timeAttr)

  store.addIndex(timeIndex)
  store.addIndex(HashIndex.onAttribute(targetIdAttr))
  store.addIndex(HashIndex.onAttribute(typeAttr))

  @BeanProperty
  var maxxise = 50000

  @BeanProperty
  var buffersize = 20000

  private val lock = new ReentrantLock()

  private[this] val _eldest: AtomicLong = new AtomicLong(Long.MaxValue)

  def eldestStored: Long = _eldest.get()

  def publish(e: Event) {
    trace("publishing event:" + e)
    if (lock.tryLock()) try {
      if (store.size() >= maxxise + buffersize) {
        val take = store.retrieve(greaterThan(timeAttr, lv(0))).take(buffersize)
        store.removeAll(take)
        val remainsHead = store.retrieve(greaterThan(timeAttr, lv(0))).take(1).head
        _eldest.set(remainsHead.time)
      }
    }
    finally lock.unlock()

    val we = new WrappedEvent(e)
    store.add(we)

    while (we.time < _eldest.get()) {
      _eldest.set(we.time)
    }

    blocking {fireListeners(e)}
  }

  private[this] def toQueryResult[T <: Event](wrapped: Iterator[WrappedEvent], startTime: Long): QueryResult[T] = {
    val lb = List.newBuilder[T]
    var maxtime = startTime;
    for (wrappedEvent <- wrapped) {
      lb += wrappedEvent.e.asInstanceOf[T]
      if (wrappedEvent.time > maxtime)
        maxtime = wrappedEvent.time
    }

    new QueryResult(lb.result(), maxtime)
  }

  def getEventsAfter[T <: Event](time: Long) = toQueryResult[T](store.retrieve(greaterThanOrEqualTo(timeAttr, lv(time))).iterator(), time)

  def getEventsAfter[T <: Event](id: String, targetType: TargetType, time: Long) = store.retrieve(
    and(
      greaterThanOrEqualTo(timeAttr, lv(time)),
      equal(targetIdAttr, id),
      equal(typeAttr, targetType)
    )
  ).iterator()

  def getEventsAfter(targetType: TargetType, time: Long) = store.retrieve(
    and(
      greaterThanOrEqualTo(timeAttr, lv(time)),
      equal(typeAttr, targetType)
    )
  ).iterator()

  def getEventsAfter(targets0: Set[String], targetType: TargetType, time: Long): util.Iterator[WrappedEvent] = {
    val targets: java.util.Collection[String] = targets0
    if (targets.size() == 0)
      return Collections.emptyIterator()
    store.retrieve(
      and(
        greaterThanOrEqualTo(timeAttr, lv(time)),
        if (targets.size() > 1) in(targetIdAttr, targets) else equal(targetIdAttr, targets.head)
      )
    ).iterator()
  }

  private[this] def filterEventType[T <: Event](eventType: EventType, events: Iterator[WrappedEvent], time: Long): QueryResult[T] = {
    toQueryResult[T](if (eventType.isEmpty) events else events.filter(_.e.eventType == eventType), time)
  }

  def getEventsAfter[T <: Event](e: EventQuery[T], time: Long) = e match {
    case NoCriteria() => getEventsAfter(time)
    case EventTopic(targetType, eventType) => filterEventType(eventType, this.getEventsAfter(targetType, time), time)
    case OnTarget(target, id) => filterEventType(target.eventType, this.getEventsAfter(id, target.targetType, time), time)
    case OnTargets(targets, eventType) => filterEventType(eventType.eventType, this.getEventsAfter(targets, eventType.targetType, time), time)
  }

  //Return empty stream
  @deprecated
  def getEventsFiltered[T <: Event](filterParams: Map[String,Any]) = getEventsFiltered[T](NoCriteria[T], filterParams)

  //Return empty stream
  @deprecated
  def getEventsFiltered[T <: Event](e: EventQuery[T], filterParams: Map[String,Any]) = {
    new QueryResult[T](Stream.empty,1L)
  }
}

