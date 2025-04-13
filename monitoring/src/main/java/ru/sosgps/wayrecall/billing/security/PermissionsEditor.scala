package ru.sosgps.wayrecall.billing.security

import ru.sosgps.wayrecall.billing.user.permission.commands._
import ru.sosgps.wayrecall.core.{SecureGateway, MongoDBManager}
import ru.sosgps.wayrecall.security.{PermissionValue, PermissionsManager}
import org.bson.types.ObjectId
import com.mongodb.casbah
import casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.jms.core.{MessageCreator, JmsTemplate}
import javax.jms.{Message, Session}
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.collection.JavaConversions.mapAsJavaMap

/**
 * Billing component for permissions editing
 */
//@Component
class PermissionsEditor extends PermissionsManager with grizzled.slf4j.Logging{

  @Autowired
  var commandGateway:SecureGateway = null

  @Autowired
  var mdbm: MongoDBManager = null

  private def collection = mdbm.getDatabase().apply("usersPermissions")

  @Autowired
  var jmsTemplate: JmsTemplate = null

  val permissionsSet = Set("view","sleepersView","control","block","getCoords","restartTerminal","paramsView","paramsEdit","fuelSettings","sensorsSettings")

  def  addPermissionRecord(userId: ObjectId, recordType: String, itemId: ObjectId, permissions: Map[String, Boolean]): String = {
    require(permissions != null, "permissions cant be null")
    checkRectype(recordType)


    val query1 = HashMap(
      "item_id" -> itemId,
      "userId" -> userId,
      "recordType" -> recordType
    )

    val mongoDBObject= new HashMap[String, AnyRef]()++ HashMap("permissions" -> new java.util.HashMap[String,Boolean](permissions.filterKeys(permissionsSet))) ++ query1
    val res: casbah.MongoCollection#T = collection.findOne(query1).getOrElse(null)
    var id: ObjectId = null
    var command : AnyRef = null;
    if(res == null)
      {
        id=new ObjectId()
        command = new PermissionCreateCommand(id, mongoDBObject)
      }
    else
      {
        id=res.remove("_id").get.asInstanceOf[ObjectId]
        command = new PermissionDataSetCommand(id, mongoDBObject)
      }
    try {
      commandGateway.sendAndWait(command,getUserAuthorities(),getUserName())
     }
     catch {
      case e: org.axonframework.commandhandling.CommandExecutionException => throw new RuntimeException("При сохранении данных произошла ошибка: " + Option(e.getCause()).map(_.getMessage()).getOrElse(e), e);
     }

    //    val res: casbah.MongoCollection#T = collection.findAndModify(
//      query = query1,
//      fields = MongoDBObject("_id" -> 1),
//      sort = MongoDBObject.empty,
//      remove = false,
//      update = mongoDBObject,
//      returnNew = true,
//      upsert = true
//    ).get

    //    try {
    //      //TODO: сделать из этого событие
    //      jmsTemplate.send("billing.permissionChange", new MessageCreator {
    //        def createMessage(session: Session): Message = {
    //          val map = new mutable.HashMap() ++= mongoDBObject
    //          val r = session.createObjectMessage(map)
    //          debug("sending message " + r)
    //          r
    //        }
    //      })
    //    }
    //    catch {
    //      case e: Exception => error("error in permissionChange publishing ", e)
    //    }

    id.toString
  }

  def provideObjectPermission(userId: ObjectId, itemId: ObjectId, permissions: Map[String,Boolean]): Unit = {

    lazy val accountId = getObjectAccount(itemId)
    lazy val accountRecord = getPermissionsRecord(userId, "account", accountId).getOrElse({
        addPermissionRecord(userId, "account", accountId, Map.empty)
      getPermissionsRecord(userId, "account", accountId).get
    }
    )

    val curPermissions = getPermissionsRecord(userId, "object", itemId).getOrElse(accountRecord).as[String]("permissions")

    if (curPermissions != permissions) {
      addPermissionRecord(userId, "object", itemId, permissions)
    }

  }

  def setPermissions(userObjectId: ObjectId, recType: String, item_id: ObjectId, permissions: Map[String,Boolean]): Unit = {
    checkRectype(recType)
    recType match {
      case "account" => addPermissionRecord(userObjectId, "account", item_id, permissions)
      case "object" =>  addPermissionRecord(userObjectId, "object", item_id, permissions)//provideObjectPermission(userObjectId, item_id, permissions)
    }
  }


  def removePermissionRecord(userId: ObjectId, recordType: String, itemId: ObjectId): Unit = {
    checkRectype(recordType)
    val query = MongoDBObject(
      "item_id" -> itemId,
      "userId" -> userId,
      "recordType" -> recordType
    )

//    if (recordType == "account")
//      getAccountObjects(itemId, MongoDBObject("_id" -> 1)).foreach(objDbo =>
//        removePermissionRecord(userId, "object", objDbo.as[ObjectId]("_id"))
//      )

    info(collection.getName + " removing " + query)
    val permissionRecord=collection.findOne(query).getOrElse(null)
    require(permissionRecord != null,"permission record not found")
    val id=permissionRecord.getAsOrElse[ObjectId]("_id",null)
    require(id != null,"permission record without id")
    try {
      commandGateway.send(new PermissionDeleteCommand(id, query.toMap.asInstanceOf[java.util.Map[String, AnyRef]]), getUserAuthorities(), getUserName())
    }
    catch {
      case e: org.axonframework.commandhandling.CommandExecutionException => throw new RuntimeException("При сохранении данных произошла ошибка: " + Option(e.getCause()).map(_.getMessage()).getOrElse(e), e);
    }
  }

  def removePermissionsForItem(itemType: String, itemId: ObjectId): Unit = {
    val query = MongoDBObject(
      "item_id" -> itemId,
      "recordType" -> itemType
    )
    val permissionRecords=collection.find(query)
    permissionRecords.foreach(rec =>
      {
        try {
          commandGateway.send(new PermissionDeleteCommand(rec.getAsOrElse[ObjectId]("_id", null),
            rec.toMap.asInstanceOf[java.util.Map[String, AnyRef]]), getUserAuthorities(), getUserName())
        }
        catch {
          case e: org.axonframework.commandhandling.CommandExecutionException => throw new RuntimeException("При сохранении данных произошла ошибка: " + Option(e.getCause()).map(_.getMessage()).getOrElse(e), e);
        }
      }
    )
  }

  def removeUserPermissions(userId: ObjectId): Unit = {
    val query = MongoDBObject(
      "userId" -> userId
    )
    val permissionRecords=collection.find(query)
    permissionRecords.foreach(rec =>
      {
        try {
          commandGateway.send(new PermissionDeleteCommand(rec.getAsOrElse[ObjectId]("_id", null),
            rec.toMap.asInstanceOf[java.util.Map[String, AnyRef]]), getUserAuthorities(), getUserName())
        }
        catch {
          case e: org.axonframework.commandhandling.CommandExecutionException => throw new RuntimeException("При сохранении данных произошла ошибка: " + Option(e.getCause()).map(_.getMessage()).getOrElse(e), e);
        }
      }
    )
  }

  private def getAccountObjects(accountId: ObjectId, fields: DBObject): MongoCollection#CursorType = {
    mdbm.getDatabase()("objects").find(
      MongoDBObject("account" -> accountId),
      fields
    )
  }

}
