package ru.sosgps.wayrecall.sms

import com.mongodb
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.events._
import javax.annotation.PostConstruct
import ru.sosgps.wayrecall.core.MongoDBManager
import com.mongodb.casbah.Imports._
import scala.beans.BeanProperty
import scala.collection.mutable.ListBuffer
import java.io.{ObjectInputStream, ByteArrayInputStream, ByteArrayOutputStream, ObjectOutputStream}
import grizzled.slf4j.Logger
import java.util.Date
import org.springframework.context.ApplicationContext
import org.springframework.security.core.context.SecurityContextHolder
import ru.sosgps.wayrecall.utils.io.{tryDeserialize, serialize}
import ru.sosgps.wayrecall.utils
import scala.Some


class SMSCommandProcessor extends grizzled.slf4j.Logging {

  @Autowired
  @BeanProperty
  var es: EventsStore = null

  @Autowired
  @BeanProperty
  var mdbm: MongoDBManager = null

  //  @Autowired
  @BeanProperty
  var smsReceiver: SmsGate = null

  @Autowired
  var context: ApplicationContext = null

  @PostConstruct
  def init() {
    require(smsReceiver != null)
    es.subscribe(PredefinedEvents.sms, processEvent)
    es.subscribe(PredefinedEvents.smsDelivered, processEvent)
  }

  def loadConversations(): Seq[SmsConversation] = {
    val conversations = mdbm.getDatabase()("smsconversation").find().
      map(_.as[Array[Byte]]("conversationData")).flatMap(tryDeserialize[SmsConversation](_) match {
      case Right(d) => Some(d)
      case Left(t) => warn("deserializationError:", t); None
    }).map(conversation => {
      conversation.context = context
      conversation.commandProcessor = this
      conversation
    })

    conversations.toSeq
  }


  private[this] def loadConversation(objectPhone: String): Option[SmsConversation] = {
    val conversationOption = mdbm.getDatabase()("smsconversation").findOne(MongoDBObject("phone" -> objectPhone)).
      map(_.as[Array[Byte]]("conversationData")).flatMap(tryDeserialize[SmsConversation](_) match {
      case Right(d) => Some(d)
      case Left(t) => warn("deserializationError:", t); None
    })

    for (conversation <- conversationOption) {
      conversation.context = context
      conversation.commandProcessor = this
    }

    conversationOption
  }

  private[this] def removeConversation(objectPhone: String) = {
    mdbm.getDatabase()("smsconversation").remove(MongoDBObject("phone" -> objectPhone))
  }

  private[this] def saveConversation(conversation: SmsConversation): Unit = {
    debug("saving conversation: " + conversation)
    mdbm.getDatabase()("smsconversation").update(
      MongoDBObject("phone" -> conversation.phone),
      MongoDBObject(
        "phone" -> conversation.phone,
        "conversationData" -> serialize(conversation),
        "lastModified" -> conversation.lastModified
      ),
      upsert = true,
      concern = mongodb.WriteConcern.SAFE
    )
  }

  def saveOrRemoveConversation(conversation: SmsConversation) {
    if (conversation.canBeDeleted)
      removeConversation(conversation.phone)
    else {
      saveConversation(conversation)
    }
  }

  def sendCommand(phone: String, command: SMSCommand): Unit = sendCommand(phone: String, Seq(command))

  def sendCommand(phone: String, command: Seq[SMSCommand]): Unit = {
    debug(s"sending single sms command: $command")
    val conversation = new SmsConversation(phone, command)
    sendCommand(conversation)
  }

  def sendCommand(conversation: SmsConversation): Unit = {
    conversation.context = context
    conversation.commandProcessor = this

    val prevConversation = loadConversation(conversation.phone)

    debug("prevConversation= " + prevConversation + " new conversation = " + conversation + " isDuplicate=" + prevConversation.exists(isDuplicate(conversation)))
    if (prevConversation.exists(isDuplicate(conversation))) {
      debug("conversation " + conversation + " is already sent " + prevConversation.get.lastModified.orNull)

      val sms = for (
        conv <- prevConversation;
        headSmsCommand <- conv.allPending.headOption;
        smsId <- headSmsCommand.sentSMS.map(_.smsId);
        dbo <- mdbm.getDatabase()("smses").findOne(MongoDBObject("smsId" -> smsId))
      ) yield {
          val smsFromConversation = headSmsCommand.sentSMS
          val smsFromDb = SMS.fromMongoDbObject(dbo)
          debug("prevconversation sms " + smsFromConversation + " smsFromDb=" + smsFromDb)
          smsFromDb
        }

      //val smsId = prevConversation.allPending.headOption.flatMap(_.sentSMS).map(_.smsId)
      //val sms = smsId.flatMap(smsId => mdbm.getDatabase()("smses").findOne(MongoDBObject("smsId" -> smsId)).map(e => SMS.fromMongoDbObject(e)))
      throw new AlreadySentException("command is already sent", sms)
    }
    else {
      if (prevConversation.exists(_.canBeDeleted))
        warn("conversation for phone " + conversation.phone + " already exists but was removed")
      conversation.start()
      saveConversation(conversation)
    }
  }


  private[this] def isDuplicate(conversation: SmsConversation)(prevConversation: SmsConversation): Boolean = {
    prevConversation.lastModified.map(lastmod => System.currentTimeMillis() - lastmod.getTime < 1000 * 60 * 60 * 6).getOrElse(true) &&
      prevConversation.contentEquals(conversation)
  }

  def sendSms(phone: String, text: String): SMS = {
    val sms = smsReceiver.sendSms(phone, text)

    //val user = Option(SecurityContextHolder.getContext().getAuthentication()).map(_.getName())
    //mdbm.getDatabase()("smses").insert(SMS.toMongoDbObject(sms) ++ ("user" -> user.orNull), WriteConcern.Normal)
    sms
  }

  def withConversation(phone: String, f: (SmsConversation) => Unit) = {
    loadConversation(phone) match {
      case Some(conversation) => {
        f(conversation)
        saveOrRemoveConversation(conversation)
      }
      case None => info("there are no conversation for " + phone)
    }
  }

  private[this] def processEvent(e: Event) = {
    e match {
      case e: SMSEvent =>
        debug("received:" + e.sms)
        val phone = e.sms.objectPhone
        withConversation(phone, (conversation) => {
          conversation.receive(e.sms)
        }
        )
      case e: SMSDeliveredEvent => {
        val phone = e.targetId
        withConversation(phone, (conversation) => {
          conversation.updateDeliveryStatus(e.messageId, e.deliveryDate)
        }
        )
      }

      case _ => warn("unknown event:" + e)

    }

  }

}

class AlreadySentException(message: String, val sms: Option[SMS]) extends IllegalStateException(message)

