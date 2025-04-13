package ru.sosgps.wayrecall.events

import scala.collection.mutable


trait EventSubscriber extends grizzled.slf4j.Logging {

  def fireListeners(e: Event) {

    for (listeners <- typeListeners.get(e.targetType);
         listener <- listeners) {
      try {
        listener match {
          case l: (Event => Any) => l(e)
        }
      }
      catch {
        case e: Exception => warn("error in listener ", e)
      }

    }

  }

  private[this] val typeListeners: AutoAddingMap[TargetType, mutable.Set[(_ <: Event) => Any]] = new AutoAddingMap[TargetType, mutable.Set[(_ <: Event) => Any]](
    _ => new mutable.HashSet[(_ <: Event) => Any]
  )

  private[this] val idtypeListeners: AutoAddingMap[TargetType, ListenerRetranslator] = new AutoAddingMap[TargetType, ListenerRetranslator](typ => {
    val retranslatorListener = new ListenerRetranslator(_.targetId)
    subscribe(typ, retranslatorListener)
    retranslatorListener
  }
  )

  private[this] val typetypeListeners: AutoAddingMap[TargetType, ListenerRetranslator] = new AutoAddingMap[TargetType, ListenerRetranslator](typ => {
    val retranslatorListener = new ListenerRetranslator(_.eventType)
    subscribe(typ, retranslatorListener)
    retranslatorListener
  }
  )

  private[this] val typeidtypeListeners: AutoAddingMap[(TargetType, String), ListenerRetranslator] = new AutoAddingMap[(TargetType, String), ListenerRetranslator](target => {
    val retranslatorListener = new ListenerRetranslator(_.eventType)
    idtypeListeners(target._1).listeners(target._2).add(retranslatorListener)
    retranslatorListener
  }
  )


  def subscribe[T <: Event](targetType: TargetType, listener: (Event) => Any): Unit = synchronized {
    typeListeners(targetType).add(listener)
  }

  def unsubscribe(targetType: TargetType, listener: (Event) => Any) = synchronized {
    typeListeners.get(targetType).foreach(s => s.remove(listener))
  }

  def subscribe[T <: Event](target: EventTopic[T], listener: (T) => Any): Unit = synchronized {
    typetypeListeners(target.targetType).listeners(target.eventType).add(listener)
  }

  def unsubscribe[T <: Event](eventTarget: EventTopic[T], listener: (T) => Any) = synchronized {
    typetypeListeners.get(eventTarget.targetType).foreach(_.listeners.valuesIterator.foreach(_.remove(listener)))
  }


  def subscribe[T <: Event](e: EventQuery[T], listener: (T) => Any): Unit = synchronized {

    e match {
      case EventTopic(targetType, eventType) => if (eventType.isEmpty)
        typeListeners(targetType).add(listener)
      else
        typetypeListeners(targetType).listeners(eventType).add(listener)

      case OnTarget(target, id) => if (target.eventType.isEmpty)
        idtypeListeners(target.targetType).listeners(id).add(listener)
      else
        typeidtypeListeners((target.targetType, id)).listeners(target.eventType).add(listener)
      case OnTargets(targets, eventType) => throw new UnsupportedOperationException("not implemented yet")
    }

  }

  def subscribeTyped[T <: Event : Manifest](eventTarget: EventQuery[T], listener: T => Any): Unit = {

    val typedWrapper = new PartialFunction[T, Any] {

      override def hashCode() = listener.hashCode()

      override def equals(obj: Any) = listener.equals(obj)

      def apply(v1: T) = if (isDefinedAt(v1)) listener(v1.asInstanceOf[T])

      val erasure = manifest[T].erasure

      def isDefinedAt(x: T) = {
        erasure.isAssignableFrom(x.getClass)
      }
    }

    subscribe[T](eventTarget, typedWrapper)

  }

  def unsubscribeTyped[T <: Event](eventTarget: EventQuery[T], listener: T => Any): Unit = {

    unsubscribe(eventTarget, listener.asInstanceOf[(Event) => Any])

  }

  def unsubscribe[T <: Event](e: EventQuery[T], listener: (T) => Any): Unit = synchronized {
    e match {
      case EventTopic(targetType, eventType) => if (eventType.isEmpty)
        typeListeners(targetType).remove(listener)
      else
        typetypeListeners(targetType).listeners(eventType).remove(listener)

      case OnTarget(target, id) => if (target.eventType.isEmpty)
        idtypeListeners(target.targetType).listeners(id).remove(listener)
      else
        typeidtypeListeners((target.targetType, id)).listeners(target.eventType).remove(listener)
      case OnTargets(targets, eventType) => throw new UnsupportedOperationException("not implemented yet")
    }
  }


  private[this] class AutoAddingMap[A, B](defaultUpdate: (A) => B) extends mutable.HashMap[A, B] {
    override def default(key: A) = {
      val r = defaultUpdate(key);
      put(key, r)
      r
    }
  }

  private[this] class ListenerRetranslator(criteria: (Event) => String) extends (Event => Any) {

    val listeners = new mutable.HashMap[String, mutable.Set[(_ <: Event) => Any]]() {

      override def default(criteriaVal: TargetType): mutable.Set[(_ <: Event) => Any] = {
        {
          val r = new mutable.HashSet[(_ <: Event) => Any]()
          put(criteriaVal, r)
          r
        }
      }

    }

    def apply(e: Event) = listeners.get(criteria(e)).foreach(_.foreach(f => {
      try {
        f match {
          case l: (Event => Any) => l(e)
        }
      }
      catch {
        case e: Exception => warn("error in listener ", e)
      }
    }))
  }

}
