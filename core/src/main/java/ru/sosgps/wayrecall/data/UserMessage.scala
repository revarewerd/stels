package ru.sosgps.wayrecall.data

import java.util.Date

import ru.sosgps.wayrecall.events.{PredefinedEvents, AbstractEvent}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.08.13
 * Time: 20:16
 * To change this template use File | Settings | File Templates.
 */

@SerialVersionUID(2311157312396190159L)
class UserMessage(
                   val targetId: String,
                   val message: String,
                   val `type`: String,
                   val subjObject: Option[String],
                   val fromUser: Option[String] = None,
                   override val time: Long = System.currentTimeMillis(),
                   override val additionalData: Map[String, Any] = Map("readStatus"->false)
                   ) extends AbstractEvent(PredefinedEvents.userMessage) {
  override val persistent = true

  override def toString = s"UserMessage($targetId, $message, ${this.`type`} , $subjObject, $fromUser, $time (${new Date(time)}), $additionalData)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[UserMessage]

  override def equals(other: Any): Boolean = other match {
    case that: UserMessage =>
      (that canEqual this) &&
        persistent == that.persistent &&
        targetId == that.targetId &&
        message == that.message &&
        `type` == that.`type` &&
        subjObject == that.subjObject &&
        fromUser == that.fromUser &&
        time == that.time &&
        additionalData == that.additionalData
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(super.hashCode(), persistent, targetId, message, `type`, subjObject, fromUser, time)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
