package ru.sosgps.wayrecall.monitoring.notifications.rules

import org.springframework.context.ApplicationContext
import ru.sosgps.wayrecall.data.UserMessage
import java.util.Date
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.OptMap
import ru.sosgps.wayrecall.core.GPSData
import com.mongodb.casbah.Imports

case class Notification[T](ruleEvent: NotificationRule[T], userMessage: UserMessage, uid: String, mesString: String, data: T, eventTime: Date) extends Serializable

class NotificationRule[T](
                           val applicationContext: ApplicationContext,
                           val notificationId: ObjectId,
                           val name: String,
                           val uids: Set[String],
                           val user: String,
                           val notifyInSystem: Boolean,
                           val email: Set[String],
                           val phone: Set[String],
                           val messageMask: Option[String],
                           val action: String = "none",
                           val blockpasw: String = ""
                           ) {

  def this(orig: NotificationRule[T]) = this(orig.applicationContext, orig.notificationId, orig.name, orig.uids, orig.user, orig.notifyInSystem, orig.email, orig.phone, orig.messageMask, orig.action, orig.blockpasw)

  def this(dbo: DBObject, applicationContext: ApplicationContext) = this(NotificationRule.fromDbo[T](dbo, applicationContext: ApplicationContext))

  override def toString = this.getClass.getName + s"($notificationId, $name)"

  def process(data: T, state: scala.collection.Map[String, AnyRef]): NotificationStateChange[T] = ???

}


abstract class NotificationStateChange[T](
                                           val notificationId: ObjectId,
                                           val data: T,
                                           val state: scala.collection.Map[String, AnyRef],
                                           val eventTime: Option[Date]
                                           ) {

  override def toString = this.getClass.getName + s"(notificationId = $notificationId, data = $data, state = $state, eventTime = $eventTime, fired = $fired, firingNow = $firingNow)"

  import ru.sosgps.wayrecall.utils.typingMap

  def fired: Boolean

  def unfired: Boolean = !fired

  def notifications: Seq[Notification[T]]

  def updatedState: scala.collection.Map[String, AnyRef] = {
    state ++ OptMap(
      "fired" -> (fired || (wasFired && !unfired)).asInstanceOf[AnyRef],
      "time" -> (if (!state.get("fired").exists(fired !=))
        eventTime
      else None)
    )
  }

  def firingNow = !wasFired && fired

  def unfiringNow = wasFired && unfired

  def wasFired: Boolean = state.getAs[java.lang.Boolean]("fired").map(_.booleanValue()).getOrElse(false)

}

abstract class GpsNotificationStateChange(notificationId: ObjectId,
                                          data: GPSData,
                                          state: scala.collection.Map[String, AnyRef]) extends NotificationStateChange[GPSData](notificationId, data, state, Option(data.time))

object NotificationRule {

  def fromDbo[T](dbo: Imports.DBObject, ctxt: ApplicationContext): NotificationRule[T] = {
    val base = new NotificationRule[T](ctxt,
      notificationId = dbo.as[ObjectId]("_id"),
      name = dbo.as[String]("name"),
      uids = dbo.getAs[MongoDBList]("objects").getOrElse(MongoDBList.empty).map(_.asInstanceOf[String]).toSet,
      user = dbo.as[String]("user"),
      notifyInSystem = dbo.getAs[Boolean]("showmessage").getOrElse(true),
      email = dbo.getAs[String]("email").filter(_.nonEmpty).map(_.split(",").map(_.trim).toSet).getOrElse(Set.empty),
      phone = dbo.getAs[String]("phone").filter(_.nonEmpty).map(_.split(",").map(_.trim).toSet).getOrElse(Set.empty)
        .map(str => str.replaceAll( """[+\-\(\)\s]""", "")),
      messageMask = dbo.getAs[String]("messagemask").filter(_.nonEmpty),
      action = dbo.getAsOrElse[String]("action", "none"),
      blockpasw = dbo.getAsOrElse[String]("blockpasw", "")
    )
    base
  }

}

