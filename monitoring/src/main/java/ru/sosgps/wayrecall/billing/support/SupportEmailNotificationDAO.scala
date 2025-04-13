package ru.sosgps.wayrecall.billing.support

import com.mongodb.{DBObject, DuplicateKeyException}
import com.mongodb.casbah.MongoCollection
import com.novus.salat._
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.utils.salat_context._
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.errors.ImpossibleActionException

/**
  * Created by IVAN on 30.01.2017.
  */

@Repository
class SupportEmailNotificationDAO @Autowired()(mdbm: MongoDBManager) extends grizzled.slf4j.Logging {

  private val supportNotificationEmailsCollection: MongoCollection = mdbm.getDatabase().apply("supportNotificationEmails")
  supportNotificationEmailsCollection.createIndex(MongoDBObject("email" -> 1), MongoDBObject("unique" -> true))

  // TODO: Использовать usersDAO при его наличии
  private val users: MongoCollection = mdbm.getDatabase().apply("users")

  private def dBObjectToSupportNotificationEmail(dBObject: DBObject): SupportNotificationEmail = {
    grater[SupportNotificationEmail].asObject(dBObject)
  }

  def updateOrCreate(supportNotificationEmail: SupportNotificationEmail): WriteResult = {
    val supportNotificationDBO = grater[SupportNotificationEmail].asDBObject(supportNotificationEmail) - "_id"
    try {
      supportNotificationEmailsCollection.update(MongoDBObject("_id" -> supportNotificationEmail._id), $set(supportNotificationDBO.toSeq: _*),
        upsert = true, multi = false, WriteConcern.Acknowledged)
    }
    catch {
      case _: DuplicateKeyException => throw new ImpossibleActionException("Такой e-mail уже зарегистрирован")
    }
  }

  def removeMany(list: Seq[String]): Unit = {
    list.foreach(removeOne)
  }

  def removeOne(id: String): Unit = {
    supportNotificationEmailsCollection.remove(MongoDBObject("_id" -> new ObjectId(id)))
  }

  def loadOne(id: String): Option[SupportNotificationEmail] = {
    supportNotificationEmailsCollection.findOne(MongoDBObject("_id" -> new ObjectId(id))).map(dBObjectToSupportNotificationEmail)
  }

  def loadAll(request: DBObject = MongoDBObject.empty): Iterator[SupportNotificationEmail] = {
    supportNotificationEmailsCollection.find(request).map(dBObjectToSupportNotificationEmail)
  }

  def loadByCategory(category: String): Iterator[SupportNotificationEmail] = {
    supportNotificationEmailsCollection.find(MongoDBObject(category -> true)).map(dBObjectToSupportNotificationEmail)
  }

  def isUserSubscribed(userId: ObjectId): Boolean = {
    users.findOne(MongoDBObject("_id" -> userId, "supportNotificationsByEmail" -> true)).isDefined
  }

  def getUserEmail(userId: ObjectId): Option[String] = {
    users.findOne(MongoDBObject("_id" -> userId)) map (_.get("email").asInstanceOf[String])
  }
}

case class SupportNotificationEmail(_id: ObjectId, name: String, email: String, equipment: Boolean = false,
                                    program: Boolean = false, finance: Boolean = false)