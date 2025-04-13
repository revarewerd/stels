package ru.sosgps.wayrecall.sms

import java.util.Date
import ru.sosgps.wayrecall.events.{PredefinedEvents, AbstractEvent, EventTopic, Event}


class SMS(
           val smsId: Long,
           val text: String,
           val fromObject: Boolean,
           val senderPhone: String,
           val targetPhone: String,
           val sendDate: Date,
           var deliveredDate: Date,
           var status: String
           ) extends Serializable {

  def this(smsId: Long, text: String, targetPhone: String) = this(smsId, text, false, "", targetPhone, new Date(), null, "Unknown")

  def objectPhone = if (fromObject) senderPhone else targetPhone

  override def toString =
    "smsId:" + smsId +
      " from:" + senderPhone +
      " to:" + targetPhone +
      " text:" + text +
      " sent:" + sendDate +
      " delivered:" + deliveredDate +
      " status:" + status
}

object SMS {

  val PENDING = "Pending"
  val SENDING = "Sending"
  val SENT = "Sent"
  val NOT_SENT = "NotSent"
  val DELIVERED = "Delivered"
  val NOT_DELIVERED = "NotDelivered"

  val statusTranslation = Map[String, String](
    SMS.PENDING -> "Ожидает отправки",
    SMS.SENDING -> "Отправляется",
    SMS.SENT -> "Отправлено",
    SMS.NOT_SENT -> "Не отправлено",
    SMS.DELIVERED -> "Доставлено",
    SMS.NOT_DELIVERED -> "Не доставлено"
  ).withDefaultValue("Неизвестно")

  import com.mongodb.casbah.Imports._

  def toMongoDbObject(sms: SMS) = {
    MongoDBObject(
      "smsId" -> sms.smsId,
      "text" -> sms.text,
      "fromObject" -> sms.fromObject,
      "senderPhone" -> sms.senderPhone,
      "targetPhone" -> sms.targetPhone,
      "sendDate" -> sms.sendDate,
      "deliveredDate" -> sms.deliveredDate,
      "status" -> sms.status
    )
  }

  def fromMongoDbObject(dbo: MongoDBObject): SMS = {
    new SMS(
      dbo.as[Long]("smsId"),
      dbo.as[String]("text"),
      dbo.as[Boolean]("fromObject"),
      dbo.as[String]("senderPhone"),
      dbo.as[String]("targetPhone"),
      dbo.as[Date]("sendDate"),
      dbo.getAs[Date]("deliveredDate").orNull,
      dbo.getAs[String]("status").getOrElse("Unknown")
    )
  }

}

class SMSEvent(val sms: SMS) extends AbstractEvent(PredefinedEvents.sms) {
  val targetId = sms.objectPhone
}

class SMSDeliveredEvent(phone: String, val messageId: Long, val deliveryDate: Date) extends AbstractEvent(PredefinedEvents.smsDelivered) {
  val targetId = phone
}