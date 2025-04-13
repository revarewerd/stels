package ru.sosgps.wayrecall.events

trait Event extends Serializable {
  val targetType: TargetType
  val eventType: EventType
  val targetId: String
  val time: Long
  val persistent: Boolean
  val additionalData:Map[String,Any]

  def uuid: Long
}


@SerialVersionUID(-2316157322396190859L)
abstract class AbstractEvent(@transient eventTarget:EventTopic[_]) extends Event {
  val targetType: TargetType = eventTarget.targetType
  val eventType: EventType = eventTarget.eventType
  val time: Long = System.currentTimeMillis()
  val persistent: Boolean = false
  val additionalData:Map[String,Any] = Map.empty
  lazy val uuid = targetType.hashCode() * 11L + eventType.hashCode * 13L + time

  override def hashCode: Int = uuid.toInt
}

@SerialVersionUID(-2316157322396100859L)
class SimpleEvent(typ: EventTopic[_ >: SimpleEvent], val targetId: String)  extends AbstractEvent(typ) {
  def this(targetType: TargetType, targetId: String, eventType: EventType = "") = this(EventTopic[SimpleEvent](targetType,eventType), targetId)

  override def toString = this.getClass.getName + "(" + targetType + ")"
}

@SerialVersionUID(-2316157322196190859L)
class DataEvent[T <: java.io.Serializable](val data: T, typ: EventTopic[DataEvent[T]], val targetId: String) extends AbstractEvent(typ) {
  def this(data: T, targetType: TargetType, targetId: String, eventType: EventType = "") = this(data, EventTopic[DataEvent[T]](targetType, eventType), targetId)

  override def toString = this.getClass.getName + s"(targetType=$targetType, targetId=$targetId, eventType=$eventType, data=$data )"
}
