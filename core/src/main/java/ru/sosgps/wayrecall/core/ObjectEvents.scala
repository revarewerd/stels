package ru.sosgps.wayrecall.core

import java.io.Serializable
import java.util
import org.bson.types.ObjectId
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import  scala.collection.JavaConverters.mapAsJavaMapConverter
class ObjectDataChangedEvent(val uid: String, val changedAttributes: Map[String, Serializable]) extends WayrecallAxonEvent{
  def toHRString():String={
    if (this.changedAttributes==null) return ""
    this.changedAttributes.map(kv => WayrecallAggregateTranslate.ObjectTranslate.getOrElse(kv._1,kv._1)+":\'"+kv._2+"\'").mkString("; ")
  }

  override def toHRTable: util.Map[String, AnyRef] = {
    if(this.changedAttributes == null)
      new util.HashMap[String,AnyRef]()
    else
      this.changedAttributes.map{case(k,v) => (WayrecallAggregateTranslate.ObjectTranslate.getOrElse(k,k), v.asInstanceOf[AnyRef])}.asJava
  }

  def this(uid: String,
           changedAttributes: java.util.Map[String, Serializable])= {
    this(uid, changedAttributes.asScala.toMap)
  }
  override def toString = "ObjectDataChangedEvent(uid = " + uid + ", changedAttributes = " + changedAttributes + ")"
}

class ObjectEquipmentChangedEvent(val uid: String, val removed: Set[ObjectId], val added: Set[ObjectId]) extends WayrecallAxonEvent{
  def toHRString()={
    Seq(
      Option(this.removed).map(r => "удаленное оборудование:"+r.mkString("[",",","]")),
      Option(this.added).map(r => "добавленное оборудование:"+r.mkString("[",",","]"))
    ).flatten.mkString(";")
  }


  override def toHRTable: util.Map[String, AnyRef] = {
    Map[String,AnyRef](
      "Удаленное оборудование" -> this.removed.mkString("[",",","]"),
      "Добавленное оборудование" -> this.added.mkString("[",",","]")
    ).asJava
  }

  def this(uid: String,
           removed: java.util.List[ObjectId],
           added: java.util.List[ObjectId]) = {
    this(uid, removed.asScala.toSet, added.asScala.toSet)}
}

class ObjectCreatedEvent(val uid: String, val objectData: Map[String, Serializable],val equipmentData: List[Map[String, Serializable]]) extends WayrecallAxonEvent{

  override def getInitialName: Option[String] = objectData.get("name").map(_.toString)

  def toHRString():String={
      if (this.objectData==null) return ""
      this.objectData.map(kv => WayrecallAggregateTranslate.ObjectTranslate.getOrElse(kv._1,kv._1)+":\'"+kv._2+"\'").mkString("; ")
    }

  override def toHRTable: util.Map[String, AnyRef] = {
    if(this.objectData == null)
      new util.HashMap[String,AnyRef]()
    else
      this.objectData.map{case(k,v) => (WayrecallAggregateTranslate.ObjectTranslate.getOrElse(k,k), v.asInstanceOf[AnyRef])}.asJava
  }

    def this(uid: String,
           objectData: java.util.Map[String, Serializable],
           equipmentData: java.util.List[java.util.Map[String, Serializable]]) = {
    this(uid, objectData.asScala.toMap, equipmentData.asScala.toList.map(_.asScala.toMap))
  }
  override def toString = "ObjectCreatedEvent(uid = " + uid + ", objectData = " + objectData + ")"
}

class ObjectDeletedEvent(val uid: String) extends WayrecallAxonEvent{
  def toHRString={
    ""
  }
  override def toString = "ObjectDeletedEvent(uid = " + uid +  ")"
}

class ObjectRemovedEvent(val uid: String) extends WayrecallAxonEvent {
  override def toHRString: String = ""
  override def toString = "ObjectRemovedEvent(uid = " + uid +  ")"
}

class ObjectRestoredEvent(val uid: String) extends WayrecallAxonEvent {
  override def toHRString: String = ""
  override def toString = "ObjectRestoredEvent(uid = " + uid +  ")"
}


class ObjectAccountChangedEvent(val uid: String, val prevAccount: Option[ObjectId], val newAccount: Option[ObjectId]) extends WayrecallAxonEvent with AccountChangedEventTrait {
  def toHRString = {
    Seq(
      Option(this.prevAccount).map(r => "предыдущий аккаунт:" + r.mkString("[", ",", "]")),
      Option(this.newAccount).map(r => "новый аккаунт:" + r.mkString("[", ",", "]"))
    ).flatten.mkString(";")

  }
  override def toHRTable: util.Map[String, AnyRef] = {
    Map[String,AnyRef](
      "Предыдущий аккаунт" -> prevAccount.getOrElse("-"),
      "Новый аккаунт" -> newAccount.getOrElse("-")).asJava
  }

  override def toString = "ObjectAccountChangedEvent(uid = " + uid + ", prevObject = " + prevAccount + ", newObject = " + newAccount + ")"
}

class ObjectEnabledChanged(val uid: String, val disabled: Boolean) extends WayrecallAxonEvent {
  def toHRString() = "Объект " + (if (disabled) "отключен" else "включен")

  override def toHRTable: util.Map[String, AnyRef] = Map[String,AnyRef]("Включен" -> (if (disabled) "Да" else "Нет")).asJava
  override def toString = "ObjectEnabledChanged(uid = " + uid + ", disabled = " + disabled + ")"
}

trait AccountChangedEventTrait {

  def prevAccount: Option[ObjectId]

  def newAccount: Option[ObjectId]

}
