package ru.sosgps.wayrecall.billing.support

import java.util.Date

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import com.novus.salat._
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.utils.ISODateTime
import ru.sosgps.wayrecall.utils.errors.WrcLogicException
import ru.sosgps.wayrecall.utils.salat_context._

@Repository
class SupportRequestDAO @Autowired()(mdbm: MongoDBManager) extends grizzled.slf4j.Logging {

  private val supportRequestCollection: MongoCollection = mdbm.getDatabase()("supportRequest")
  supportRequestCollection.createIndex(MongoDBObject("userId" -> 1))

  private val usersCollection: MongoCollection = mdbm.getDatabase().apply("users")

  private def dBObjectToSupportRequest(dBObject: DBObject): SupportRequest = {
    dBObject.getAs[Date]("dateTime").foreach(date => dBObject.put("dateTime", ISODateTime(date)))
    if (dBObject.getAs[List[Any]]("dialog").isEmpty) dBObject.put("dialog", List.empty)
    grater[SupportRequest].asObject(dBObject ++ (getUserNameAndPhone(dBObject.as[ObjectId]("userId")).toSeq: _*))
  }

  private def supportRequestToDBObject(supportRequest: SupportRequest): DBObject = {
    grater[SupportRequest].asDBObject(supportRequest) - "userName" - "userPhone"
  }

  def loadOne(ref: DBObject = MongoDBObject.empty): Option[SupportRequest] =
    supportRequestCollection.findOne(ref).map(dBObjectToSupportRequest)

  def loadById(id: String): Option[SupportRequest] =
    supportRequestCollection.findOne(MongoDBObject("_id" -> new ObjectId(id))).map(dBObjectToSupportRequest)

  def loadByIdAndUserId(id: String, userId: String): Option[SupportRequest] =
    supportRequestCollection.findOne(MongoDBObject("_id" -> new ObjectId(id), "userId" -> new ObjectId(userId)))
      .map(dBObjectToSupportRequest)

  def load(request: DBObject = MongoDBObject.empty): Iterator[SupportRequest] =
    supportRequestCollection.find(request).sort(MongoDBObject("supportRead" -> 1, "dateTime" -> -1)).map(dBObjectToSupportRequest)

  def loadByUserId(userId: String): Iterator[SupportRequest] =
    supportRequestCollection.find(MongoDBObject("userId" -> new ObjectId(userId))).sort(MongoDBObject("userRead" -> 1, "dateTime" -> -1)).map(dBObjectToSupportRequest)

  def update(supportRequest: SupportRequest): Unit = {
    val supportRequestDBO = supportRequestToDBObject(supportRequest) - "id"
    supportRequestCollection.update(MongoDBObject("_id" -> supportRequest._id), $set(supportRequestDBO.toSeq: _*),
      concern = WriteConcern.Acknowledged)
  }

  def updateTicketReadStatus(ticketId: String, target: String, readStatus: Boolean): Unit = {
    if (target != "userRead" && target != "supportRead") throw new WrcLogicException("Parameter \"target\" must be \"userRead\" or \"supportRead\"")
    supportRequestCollection.update(MongoDBObject("_id" -> new ObjectId(ticketId)), $set(target -> readStatus),
      concern = WriteConcern.Acknowledged)
  }

  def updateTicketStatus(ticketId: String, status: String): Unit = {
    supportRequestCollection.update(MongoDBObject("_id" -> new ObjectId(ticketId)), $set("status" -> status),
      concern = WriteConcern.Acknowledged)
  }

  def updateTicketDialog(ticketId: String, message: Map[String, Any]): Unit = {
    supportRequestCollection.update(MongoDBObject("_id" -> new ObjectId(ticketId)), $push("dialog" -> message),
      concern = WriteConcern.Acknowledged)
  }

  def insert(supportRequest: SupportRequest): Unit = {
    supportRequestCollection.insert(supportRequestToDBObject(supportRequest))
  }

  def remove(ticketId: String): Unit = {
    supportRequestCollection.remove(MongoDBObject("_id" -> new ObjectId(ticketId)), concern = WriteConcern.Acknowledged)
  }

  def calculateCount(userType: String, userId: String = ""): Int = {
    val dbo = if (userId.isEmpty)
      MongoDBObject(userType -> false)
    else
      MongoDBObject(userType -> false, "userId" -> new ObjectId(userId))
    supportRequestCollection.find(dbo).count()
  }

  //TODO:  Использовать usersDAO при его наличии
  def getUserNameAndPhone(uid: ObjectId) = {
    val user = usersCollection.findOne(MongoDBObject("_id" -> uid), MongoDBObject("name" -> 1, "phone" -> 1))
      .getOrElse(MongoDBObject("name" -> "", "phone" -> ""))
    Map("userName" -> user.getAsOrElse[String]("name", ""), "userPhone" -> user.getAsOrElse[String]("phone", ""))
  }

  //TODO:  Использовать usersDAO при его наличии
  private def getUserName(uid: ObjectId) = {
    val user = usersCollection.findOne(MongoDBObject("_id" -> uid), MongoDBObject("name" -> 1))
    if (user.isDefined)
      user.get.getAsOrElse[String]("name", "")
    else ""
  }
}

case class SupportRequest(_id: ObjectId, question: String, description: String, dateTime: String,
                          userId: ObjectId, category: String, status: String, userName: String = "", userPhone: String = "",
                          dialog: List[Map[String, Any]], userRead: Boolean, supportRead: Boolean)
