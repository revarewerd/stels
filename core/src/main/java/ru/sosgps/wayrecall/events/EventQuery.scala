package ru.sosgps.wayrecall.events

sealed abstract class EventQuery[T <: Event]

case class NoCriteria[T <: Event]() extends EventQuery[T]

//case class OnTargetType(eventTarget: EventTarget) extends EventQuery {
//  def this(targetType: TargetType) = this(targetType, "")
//  def this(targetType: TargetType, eventType: EventType) = this(EventTarget(targetType,eventType))
//}

case class OnTarget[T <: Event](target: EventTopic[T], id: String) extends EventQuery[T] {
  def this(typ: TargetType, id: String) = this(EventTopic[T](typ: TargetType, ""), id)

  def this(typ: TargetType, id: String, eventType: EventType) = this(EventTopic[T](typ: TargetType, eventType), id)
}

case class OnTargets[T <: Event](targetsIds: Set[String], eventTarget: EventTopic[T]) extends EventQuery[T] {
  def this(typ: TargetType, ids: Set[String], eventType: EventType = "") = this(ids, EventTopic[T](typ, eventType))
}

class QueryResult[T <: Event](val inner: Seq[T], val lastAddedEventTime: Long) extends Seq[T] {

  def length = inner.length

  def apply(idx: Int) = inner(idx)

  def iterator = inner.iterator
}

case class EventTopic[T <: Event](targetType: TargetType, eventType: EventType) extends EventQuery[T] {
  def this(targetType: TargetType) = this(targetType, "")
}


