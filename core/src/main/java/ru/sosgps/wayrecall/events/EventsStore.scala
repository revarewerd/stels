

package ru.sosgps.wayrecall {

package object events {

  type TargetType = String

  type EventType = String
}

}

package ru.sosgps.wayrecall.events {

//case class TargetType(str:String){
//  override def toString = str
//}
//
//case class EventType(str:String){
//  override def toString = str
//}


trait EventsStore {

  def publish(e: Event): Unit

  def subscribe[T <: Event](eventTarget: EventQuery[T], listener: T => Any): Unit

  def subscribeTyped[T <: Event : Manifest](eventTarget: EventQuery[T], listener: T => Any): Unit

  def unsubscribe[T <: Event](eventTarget: EventQuery[T], listener: T => Any): Unit

  def unsubscribeTyped[T <: Event](eventTarget: EventQuery[T], listener: T => Any): Unit

  def getEventsAfter[T <: Event](time: Long): QueryResult[T]

  def getEventsAfter[T <: Event](e: EventQuery[T], time: Long): QueryResult[T]

  def getEventsFiltered[T <: Event](filterParams: Map[String,Any]): QueryResult[T]

  def getEventsFiltered[T <: Event](e: EventQuery[T],  filterParams: Map[String,Any]): QueryResult[T]

}

}






