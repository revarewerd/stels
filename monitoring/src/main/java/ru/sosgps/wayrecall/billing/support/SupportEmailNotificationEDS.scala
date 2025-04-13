package ru.sosgps.wayrecall.billing.support


import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.bson.types.ObjectId
import org.joda.time.format.ISODateTimeFormat
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.UserRolesChecker
import ru.sosgps.wayrecall.utils.errors.{ImpossibleActionException, NotPermitted}
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils
import ru.sosgps.wayrecall.utils.{ExtDirectService, MailSender}
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.typingMap

/**
  * Created by User on 02.02.2017.
  */

@ExtDirectService
class SupportEmailNotificationEDS extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  @Autowired
  val roleChecker: UserRolesChecker = null

  @Autowired
  var mailSender: MailSender = null

  @Autowired
  var supportRequestDA0: SupportRequestDAO = null

  @Autowired
  var supportEmailNotificationDA0: SupportEmailNotificationDAO = null

  val model = Model("_id", "name", "email", "equipment", "program", "finance")
  val lazyLoad: Boolean = false
  val name = "SupportEmailNotificationEDS"
  val idProperty = "_id"

  @ExtDirectMethod
  def addEmail(map: Map[String, Any]) {
    adminRequired("Зарегистрировать e-mail для оповещений может только администратор")
    val id = map.get("_id") match {
      case Some(id: String) => new ObjectId(id)
      case _ => new ObjectId()
    }
    val supportNotificationEmail = SupportNotificationEmail(id, map.as[String]("name"), map.as[String]("email"),
      map.as[Boolean]("equipment"), map.as[Boolean]("program"), map.as[Boolean]("finance"))
    supportEmailNotificationDA0.updateOrCreate(supportNotificationEmail)
  }

  @ExtDirectMethod
  def removeEmails(list: Seq[String]) {
    adminRequired("Удалить e-mail для оповещений может только администратор")
    supportEmailNotificationDA0.removeMany(list)
  }

  @ExtDirectMethod
  def loadOne(id: String) = {
    adminRequired("Просматривать e-mail для оповещений может только администратор")
    supportEmailNotificationDA0.loadOne(id) match {
      case Some(sne: SupportNotificationEmail) => sne
      case _ => throw new ImpossibleActionException("Уведомление c _id= " + id + " не обнаружено")
    }
  }

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadAll(request: ExtDirectStoreReadRequest): Iterator[SupportNotificationEmail] = {
    adminRequired("Просматривать список e-mail для оповещений может только администратор")
    supportEmailNotificationDA0.loadAll(EDSMongoUtils.searchAndFilters(request))
  }

  private def adminRequired(message: String) = {
    if (!roleChecker.checkAdminAuthority)
      throw new NotPermitted(message)
  }

  def prepareTicketData(ticketAction: String, messageTarget: String, data: Map[String, Any]) = {
    //TODO: Желательно сделать языковую локализацию и приводить dateTime в зависимости от часового пояса клиента
    val isoDateFormatter = ISODateTimeFormat.dateTime();
    val ticketId = data("_id").toString
    val dateTime = isoDateFormatter.parseLocalDateTime(data.as[String]("dateTime")).toString("yyyy.MM.dd - HH:mm:ss")
    val userName = data("userName")
    val sender = messageTarget match {
      case TicketReader.USER => "Сотрудник техподдержки"
      case TicketReader.SUPPORT => "Пользователь"
    }

    def makeHtmlString(title: String, body: String): String =
      "<html style=\"border:0;margin:0;padding:0;\">" +
        s"<title>$title</title>" +
        s"""<body style="border:0;margin:0;padding:0;">$body</body></html>"""

    ticketAction match {
      case "createTicket" => {
        val category = data("category") match {
          case "equipment" => "Вопросы по оборудованию"
          case "finance" => "Финансовые вопросы"
          case "program" => "Вопросы по программе"
        }
        SupportRequestTicketData(
          "Зарегистрирована новая заявка",
          makeHtmlString(
            "Зарегистрирована новая заявка",
            "<h1>Новая заявка</h1><br>" +
              s"<p><b>Пользователь:</b> $userName</p>" +
              s"<p><b>Код заявки:</b>  $ticketId</p>" +
              s"<p><b>Дата:</b> $dateTime</p>" +
              s"<p><b>Категория:</b> $category</p>" +
              s"<p><b>Вопрос:</b>  ${data("question")}</p>" +
              s"<p><b>Текст заявки:</b> ${data("description")}</p>"
          )
        )
      }
      case "updateTicketDialog" => {
        SupportRequestTicketData(
          "Новое сообщение в заявке",
          makeHtmlString(
            "Новое сообщение в заявке",
            "<h1>Новое сообщение</h1><br>" +
              s"<p><b>$sender: </b> $userName</p>" +
              s"<p><b>Код заявки:</b> $ticketId</p>" +
              s"<p><b>Дата:</b> $dateTime</p>" +
              s"<p><b>Сообщение:</b>${data("text")}</p>"
          )
        )
      }
      case "updateTicketStatus" => {
        val status = data("status") match {
          case TicketStatus.CLOSE => "заявка закрыта"
          case TicketStatus.NEW | TicketStatus.OPEN => "заявка открыта повторно"
        }
        SupportRequestTicketData(
          "Изменен статус заявки",
          makeHtmlString(
            "Изменен статус заявки",
            "<h1>Статус заявки изменен</h1><br>" +
              s"<p><b>$sender:</b> $userName</p>" +
              s"<p><b>Код заявки:</b> $ticketId</p>" +
              s"<p><b>Дата:</b> $dateTime</p>" +
              s"<p><b>Новый статус:</b> $status</p>"
          )
        )
      }
    }
  }

  private def getTicketCategory(ticketId: String): String = {
    supportRequestDA0.loadById(ticketId).get.category
  }

  private def getTicketOwnerId(ticketId: String): ObjectId = {
    supportRequestDA0.loadById(ticketId).get.userId
  }

  private def loadByCategory(category: String) = {
    supportEmailNotificationDA0.loadByCategory(category)
  }

  def notifyOnEmail(ticketAction: String, messageTarget: String, messageData: Map[String, Any]) {
    val category = messageData.getOrElse("category", getTicketCategory(messageData.as[String]("_id"))).asInstanceOf[String]
    val srtd = prepareTicketData(ticketAction, messageTarget, messageData)
    messageTarget match {
      case TicketReader.SUPPORT => {
        val emails = loadByCategory(category)
        emails.foreach(sne => mailSender.sendEmail(sne.email, srtd.subject, srtd.data, srtd.format))
      }
      case TicketReader.USER => {
        val ownerId = getTicketOwnerId(messageData.as[String]("_id"))
        if (supportEmailNotificationDA0.isUserSubscribed(ownerId)) {
          supportEmailNotificationDA0.getUserEmail(ownerId).foreach(email =>
            mailSender.sendEmail(email, srtd.subject, srtd.data, srtd.format)
          )
        }
      }
    }
  }
}

case class SupportRequestTicketData(subject: String, data: String, format: String = "text/html; charset=UTF-8")