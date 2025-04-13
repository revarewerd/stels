package ru.sosgps.wayrecall.core

import java.io.Serializable
import java.util
import org.bson.types.ObjectId
import ru.sosgps.wayrecall.core.axon.EventsViewConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import  scala.collection.JavaConverters.mapAsJavaMapConverter
class EquipmentCreatedEvent(val oid: ObjectId, val data: Map[String, Serializable]) extends WayrecallAxonEvent
{

  override def getInitialName: Option[String] = Option(EventsViewConverter.eqNameFields.flatMap(data.get).map(_.toString).mkString(" "))

  def toHRString():String={
    if (this.data==null) return ""
    this.data.map(kv => WayrecallAggregateTranslate.EquipmentTranslate.getOrElse(kv._1,kv._1)+":\'"+kv._2+"\'").mkString("; ")
  }

  override def toHRTable: util.Map[String, AnyRef] = {
    if(this.data == null)
      new util.HashMap[String,AnyRef]()
    else
     this.data.map{case(k,v) => (WayrecallAggregateTranslate.EquipmentTranslate.getOrElse(k,k), v.asInstanceOf[AnyRef])}.asJava
  }

  def this(oid: ObjectId,
           changedAttributes: java.util.Map[String, Serializable]) = {
    this(oid, changedAttributes.asScala.toMap)
    }
  override def toString = "EquipmentCreatedEvent(oid = " + oid + ", data = " + data + ")"
}

class EquipmentDataChangedEvent(val oid: ObjectId, val changedAttributes: Map[String, Serializable]) extends WayrecallAxonEvent{
  def toHRString:String={
    if (this.changedAttributes==null) return ""
    this.changedAttributes.map(kv => WayrecallAggregateTranslate.EquipmentTranslate.getOrElse(kv._1,kv._1)+":\'"+kv._2+"\'").mkString("; ")
  }
  override def toHRTable: util.Map[String, AnyRef] = {
    if(this.changedAttributes == null)
      new util.HashMap[String,AnyRef]()
    else
       this.changedAttributes.map{case(k,v) => (WayrecallAggregateTranslate.EquipmentTranslate.getOrElse(k,k), v.asInstanceOf[AnyRef])}.asJava
  }

  def this(oid: ObjectId,
           changedAttributes: java.util.Map[String, Serializable]) = {
    this(oid, changedAttributes.asScala.toMap)
  }
  override def toString = "EquipmentDataChangedEvent(oid = " + oid + ", changedAttributes = " + changedAttributes + ")"
}

class EquipmentDeletedEvent(val oid: ObjectId, val data: scala.collection.Map[String, Serializable] with Serializable) extends WayrecallAxonEvent {
  def toHRString()={
    ""
  }

  override def toString = "EquipmentDeletedEvent(oid = " + oid + ", " + data + ")"
}

class EquipmentRemovedEvent(val oid: ObjectId, val data: scala.collection.Map[String, Serializable] with Serializable) extends WayrecallAxonEvent {
  def toHRString()={
    ""
  }
  override def toString = "EquipmentDeletedEvent(oid = " + oid + ", " + data + ")"
}

class EquipmentRestoredEvent(val oid: ObjectId, val data: scala.collection.Map[String, Serializable] with Serializable) extends WayrecallAxonEvent {
  def toHRString()={
    ""
  }
  override def toString = "EquipmentDeletedEvent(oid = " + oid + ", " + data + ")"
}


class EquipmentObjectChangedEvent(val oid: ObjectId, val prevObject: Option[String], val newObject: Option[String])extends WayrecallAxonEvent{
  def toHRString()={
    Seq(
      Option(this.prevObject).map(r => "предыдущий объект:"+r.mkString("[",",","]")),
      Option(this.newObject).map(r => "новый объект:"+r.mkString("[",",","]"))
    ).flatten.mkString(";")
  }

  override def toHRTable: util.Map[String, AnyRef] = {
    Map[String,AnyRef](
      "Предыдущий объект" -> prevObject.getOrElse("-"),
      "Новый объект" -> newObject.getOrElse("-")).asJava
  }

  override def toString = "EquipmentObjectChangedEvent(oid = " + oid + ", prevObject = " + prevObject + ", newObject = " + newObject + ")"
}

class EquipmentAccountChangedEvent(val oid: ObjectId, val prevAccount: Option[ObjectId], val newAccount: Option[ObjectId])extends WayrecallAxonEvent with AccountChangedEventTrait{
  def toHRString()={
    Seq(
      Option(this.prevAccount).map(r => "предыдущий аккаунт:"+r.mkString("[",",","]")),
      Option(this.newAccount).map(r => "новый аккаунт:"+r.mkString("[",",","]"))
    ).flatten.mkString(";")

  }

  override def toHRTable: util.Map[String, AnyRef] = {
    Map[String,AnyRef](
      "Предыдущий аккаунт" -> prevAccount.getOrElse("-"),
      "Новый аккаунт" -> newAccount.getOrElse("-")).asJava
  }

  override def toString = "EquipmentAccountChangedEvent(oid = " + oid + ", prevObject = " + prevAccount + ", newObject = " + newAccount + ")"
}
