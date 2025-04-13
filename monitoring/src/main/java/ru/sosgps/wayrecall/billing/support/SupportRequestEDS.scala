package ru.sosgps.wayrecall.billing.support


import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.axonframework.domain.GenericEventMessage
import org.axonframework.eventhandling.EventBus
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.errors.{ImpossibleActionException, NotPermitted}
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import com.novus.salat._
import ru.sosgps.wayrecall.utils.salat_context._

@ExtDirectService
class SupportRequestEDS extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id", "question", "description", "dateTime", "userId", "category", "status", "userName", "userPhone", "dialog", "userRead", "supportRead")

  val name = "SupportRequestEDS"
  val idProperty = "_id"
  val lazyLoad = false

  //val fieldsNames: Set[String] = model.fieldsNames - "userName" - "userPhone"

  val messageTarget = TicketReader.USER
  val currentReader = TicketReader.SUPPORT

  @Autowired
  var supportRequestDA0: SupportRequestDAO = null

  @Autowired
  val roleChecker: UserRolesChecker = null

  @Autowired
  val eventBus: EventBus = null

  @Autowired
  val sens: SupportEmailNotificationEDS = null

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadAll(request: ExtDirectStoreReadRequest): Iterator[SupportRequest] =
    if (roleChecker.checkAdminAuthority()) supportRequestDA0.load(EDSMongoUtils.searchAndFilters(request))
    else throw new NotPermitted("У пользователя нет прав на загрузку списка заявок")

  protected def updateUnreadTicketsCount(ticketId: String, target: String, readStatus: Boolean): Unit = {
    target match {
      case TicketReader.USER => eventBus.publish(GenericEventMessage.asEventMessage(new TicketUserReadStatusChangedEvent(new ObjectId(ticketId), readStatus)))
      case TicketReader.SUPPORT => eventBus.publish(GenericEventMessage.asEventMessage(new TicketSupportReadStatusChangedEvent(new ObjectId(ticketId), readStatus)))
    }
  }

  protected def updateUnreadTicketsCount(ticketId: String, changes: Map[String, Boolean]): Unit =
    changes.foreach(t => updateUnreadTicketsCount(ticketId, t._1, t._2))

  protected def updateTicketReadStatus(ticketId: String, changes: Map[String, Boolean]): Unit = {
    changes.foreach(t => {
      supportRequestDA0.updateTicketReadStatus(ticketId, t._1, t._2)
      updateUnreadTicketsCount(ticketId, t._1, t._2)
    })
  }

  @ExtDirectMethod
  def getUnreadTicketsCount: Int = supportRequestDA0.calculateCount(currentReader)

  @ExtDirectMethod
  def loadOne(ticketId: String): SupportRequest = {
    checkUserPermissions(ticketId)
    supportRequestDA0.loadById(ticketId) match {
      case Some(sr: SupportRequest) => sr
      case _ => throw new ImpossibleActionException("Заявка c ticketId= " + ticketId + " не найдена")
    }
  }

  protected def checkUserPermissions(ticketId: String) {
    if (!roleChecker.checkAdminAuthority() && supportRequestDA0.loadByIdAndUserId(ticketId, roleChecker.getCurrentUserId().toString).isEmpty)
      throw new NotPermitted("У пользователя " + roleChecker.getUserName() + " нет прав на работу с заявкой ticketId=" + ticketId)
  }

  @ExtDirectMethod
  def updateTicketDialog(data: Map[String, Any]): Unit = {
    debug("data=" + data)
    if (data.as[String]("text").nonEmpty) {
      val ticketId = data.get("ticketId") match {
        case Some(id: String) => id
        case _ => throw new ImpossibleActionException("Заявка не существует")
      }
      checkUserPermissions(ticketId)
      val status: String = supportRequestDA0.loadById(ticketId).map(ticket => ticket.status).getOrElse(TicketStatus.NONE)
      val dateTime = ISODateTime(new Date(data("dateTime").asInstanceOf[Long]))
      val newMessage = data - "ticketId" + ("dateTime" -> dateTime, "userName" -> roleChecker.getUserName())
      status match {
        case TicketStatus.NEW =>
          supportRequestDA0.updateTicketDialog(ticketId, newMessage)
          supportRequestDA0.updateTicketStatus(ticketId, TicketStatus.OPEN)
          sens.notifyOnEmail("updateTicketDialog", messageTarget, newMessage + ("_id" -> ticketId))
          updateTicketReadStatus(ticketId, Map(currentReader -> true, messageTarget -> false))
        case TicketStatus.OPEN =>
          supportRequestDA0.updateTicketDialog(ticketId, newMessage)
          sens.notifyOnEmail("updateTicketDialog", messageTarget, newMessage + ("_id" -> ticketId))
          updateTicketReadStatus(ticketId, Map(currentReader -> true, messageTarget -> false))
        case TicketStatus.CLOSE => throw new ImpossibleActionException("Заявка закрыта")
        case TicketStatus.NONE => throw new ImpossibleActionException("Заявка не существует")
      }
    }
  }

  @ExtDirectMethod
  def updateTicketStatus(ticketId: String, status: String): Unit = {
    checkUserPermissions(ticketId)
    val userName = roleChecker.getUserName()

    def updateAndNotify() = {
      supportRequestDA0.updateTicketStatus(ticketId, status)
      sens.notifyOnEmail("updateTicketStatus", messageTarget, Map("_id" -> ticketId, "status" -> status, "userName" -> userName, "dateTime" -> ISODateTime(new Date())))
      updateTicketReadStatus(ticketId, Map(messageTarget -> false, currentReader -> true))
    }

    status match {
      case TicketStatus.CLOSE =>
        updateAndNotify()
      case TicketStatus.NEW | TicketStatus.OPEN =>
        if (!roleChecker.checkAdminAuthority()) throw new ImpossibleActionException("Статус заявки может изменить только пользователь с правами администратора")
        updateAndNotify()
      case _ => throw new ImpossibleActionException("Невозможно изменить статус заявки: указан некорректный статус")
    }
  }

  @ExtDirectMethod
  def changeTicketReadStatus(ticketId: String, read: Boolean): Boolean = {
    checkUserPermissions(ticketId)
    updateTicketReadStatus(ticketId, Map(currentReader -> read))
    read
  }

  @ExtDirectMethod
  def remove(maps: Seq[String]): Unit = {
    maps.foreach(id => {
      checkUserPermissions(id)
      supportRequestDA0.remove(id)
      eventBus.publish(GenericEventMessage.asEventMessage(new SupportTicketDeletedEvent(new ObjectId(id))))
    })
  }
}

@ExtDirectService
class UserSupportRequestEDS extends SupportRequestEDS {

  override val name = "UserSupportRequestEDS"

  override val messageTarget = TicketReader.SUPPORT
  override val currentReader = TicketReader.USER

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  override def loadAll(request: ExtDirectStoreReadRequest): Iterator[SupportRequest] = {
    supportRequestDA0.loadByUserId(roleChecker.getCurrentUserId().toString)
  }

  @ExtDirectMethod
  override def getUnreadTicketsCount: Int =
    supportRequestDA0.calculateCount(TicketReader.USER, roleChecker.getCurrentUserId().toString)

  @ExtDirectMethod
  def createTicket(map0: Map[String, Any]): Unit = {
    val ticketId = new ObjectId()
    val dateTime = ISODateTime(new Date(map0("dateTime").asInstanceOf[Long]))
    val supportRequest = SupportRequest(ticketId, map0.as[String]("question"), map0.as[String]("description"), dateTime,
      roleChecker.getCurrentUserId(), map0.as[String]("category"), TicketStatus.NEW, roleChecker.getUserName(),
      dialog = List.empty, userRead = true, supportRead = false)
    supportRequestDA0.insert(supportRequest)
    sens.notifyOnEmail("createTicket", TicketReader.SUPPORT, grater[SupportRequest].toMap(supportRequest))
    updateUnreadTicketsCount(ticketId.toString, Map(TicketReader.USER -> true, TicketReader.SUPPORT -> false))
  }
}

object TicketStatus {
  val NEW: String = "new"
  val OPEN: String = "open"
  val CLOSE: String = "close"
  val NONE: String = "none"
}

object TicketReader {
  val USER: String = "userRead"
  val SUPPORT: String = "supportRead"
}
