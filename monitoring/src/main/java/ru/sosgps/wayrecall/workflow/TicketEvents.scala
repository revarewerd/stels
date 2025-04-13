package ru.sosgps.wayrecall.workflow

import java.io.Serializable
import java.util

import org.bson.types.ObjectId
import ru.sosgps.wayrecall.core.{WayrecallAggregateTranslate, WayrecallAxonEvent}
import scala.collection.JavaConverters._
/**
 * Created by IVAN on 15.01.2015.
 */

class TicketCreatedEvent(val id: ObjectId, val data: Map[String, Serializable]) extends WayrecallAxonEvent
{
  def toHRString():String={
    if (this.data==null) return ""
    this.data.map(kv => WayrecallAggregateTranslate.TicketTranslate.getOrElse(kv._1,kv._1)+":\'"+kv._2+"\'").mkString("; ")
  }
  def this(id: ObjectId,
           changedAttributes: java.util.Map[String, Serializable]) = {
    this(id, changedAttributes.asScala.toMap)
  }
  override def toHRTable: util.Map[String, AnyRef] = {
    if(this.data == null)
      new util.HashMap[String,AnyRef]()
    else
      this.data.map{case(k,v) => (WayrecallAggregateTranslate.EquipmentTranslate.getOrElse(k,k), v.asInstanceOf[AnyRef])}.asJava
  }
  override def toString = "TicketCreatedEvent(oid = " + id + ", data = " + data + ")"
}

class TicketDataChangedEvent(val id: ObjectId, val changedAttributes: Map[String, Serializable]) extends WayrecallAxonEvent{
  def toHRString:String={
    if (this.changedAttributes==null) return ""
    this.changedAttributes.map(kv => WayrecallAggregateTranslate.TicketTranslate.getOrElse(kv._1,kv._1)+":\'"+kv._2+"\'").mkString("; ")
  }
  override def toHRTable: util.Map[String, AnyRef] = {
    if(this.changedAttributes == null)
      new util.HashMap[String,AnyRef]()
    else
      this.changedAttributes.map{case(k,v) => (WayrecallAggregateTranslate.EquipmentTranslate.getOrElse(k,k), v.asInstanceOf[AnyRef])}.asJava
  }
  def this(id: ObjectId,
           changedAttributes: java.util.Map[String, Serializable]) = {
    this(id, changedAttributes.asScala.toMap)
  }
  override def toString = "TicketDataChangedEvent(id = " + id + ", changedAttributes = " + changedAttributes + ")"
}

class TicketDeletedEvent(val id: ObjectId, val data: scala.collection.Map[String, Serializable] with Serializable) extends WayrecallAxonEvent {
  def toHRString()={
    ""
  }

  override def toString = "TicketDeletedEvent(id = " + id + ", " + data + ")"
}