package ru.sosgps.wayrecall.billing

import java.util
import java.util.{Objects, Date}
import javax.annotation.PostConstruct
import ru.sosgps.wayrecall.billing.user.commands.UserDataSetCommand

import scala.collection.JavaConversions
import scala.reflect.{ClassTag, classTag}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{DBObject, BasicDBList, BasicDBObject}
import org.bson.types.ObjectId
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.sosgps.wayrecall.billing.`object`.ObjectsRepositoryWriter
import ru.sosgps.wayrecall.billing.account.{AccountEventHandler, AccountRepository}
import ru.sosgps.wayrecall.billing.equipment.EquipmentObjectChangeCommand
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectDataSetCommand
import ru.sosgps.wayrecall.billing.security.PermissionsEditor
import ru.sosgps.wayrecall.core._
import collection.JavaConversions
/**
 * Created by ivan on 14.09.15.
 */
@Component
class RemovalRestorationManager {
  private var log: Logger = org.slf4j.LoggerFactory.getLogger(classOf[RemovalRestorationManager])

  @Autowired
  private var mdbm : MongoDBManager = null;

  @Autowired
  private var or: ObjectsRepositoryReader = null

  @Autowired
  private var permEd: PermissionsEditor = null

  lazy val defaultAcc =  dbaccounts.findOne(MongoDBObject("default" -> true)).get
  private var dbobjects : MongoCollection = null
  private var dbaccounts: MongoCollection = null
  private var dbequipments: MongoCollection = null
  private var dbpermissions: MongoCollection = null
  private var dbpermissionsRemoved: MongoCollection = null
  private var dbusers: MongoCollection = null
  @PostConstruct private def init {
    dbequipments = mdbm.getDatabase()("equipments")
    dbaccounts = mdbm.getDatabase()("accounts")
    dbobjects = mdbm.getDatabase()("objects")
    dbusers = mdbm.getDatabase()("users")
    dbpermissions = mdbm.getDatabase()("usersPermissions")
    dbpermissionsRemoved = mdbm.getDatabase()("usersPermissions.removed")
  }

  @Autowired
  private var accRepo: AccountRepository = null
  @Autowired
  private var writer: ObjectsRepositoryWriter = null

  @Autowired
  private var commandGateway: SecureGateway = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  private def defaultAccId = defaultAcc("_id").asInstanceOf[ObjectId]
  private def defaultAccName = defaultAcc("name").asInstanceOf[String]


  def deleteEquipment(_id: ObjectId): Unit = {
    writer.equipmentsCollection.remove(new BasicDBObject("_id", _id))
  }

  def deleteObject(uid: String): Unit = {
    log.debug("removing account permissions")
    val obj: DBObject = Objects.requireNonNull(writer, "writer cant be null").getObjectByUid(uid)
    Objects.requireNonNull(permEd, "permEd cant be null").removePermissionsForItem("object", obj.get("_id").asInstanceOf[ObjectId])
    mdbm.getDatabase().underlying.getCollection("objects.removed").insert(obj)
    writer.objectsCollection.remove(new BasicDBObject("uid", uid))
  }


  def eraseUsersMainAcc(usr: DBObject) = {
    val data = new util.HashMap[String,AnyRef](JavaConversions.mapAsJavaMap(usr.map{case(k,v) => (k.asInstanceOf[String], v.asInstanceOf[AnyRef])}))
    val oid = data.get("_id").asInstanceOf[ObjectId]
    log.debug("oid is " + oid)
    data.put("mainAccId", null)
    log.debug("data is " + data)
    commandGateway.sendAndWait(new UserDataSetCommand(oid,data),Array("UserDataSet", "admin"), "System")
  }

  def removeAccount(id: ObjectId): Unit = {
    //Сохраняем права в резервную коллекцию
    val usersPermissions = dbpermissions.find(MongoDBObject
    ("item_id" -> id ,
      "recordType" -> "account"))

    usersPermissions.foreach(p => {
      dbpermissionsRemoved.insert(p - "_id")
    })

    permEd.removePermissionsForItem("account", id)


    val date = new Date()
    val removalMark = MongoDBObject("removed" -> true, "removalTime" -> date)
    dbaccounts.update(MongoDBObject("_id" -> id), MongoDBObject("$set" -> removalMark))

  }

  class PermissionRecord(val item_id: ObjectId, val userId: ObjectId, val permissions: Map[String,Boolean]) {} // Возможно стоит использовать tupled

  def restoreAccount(id: ObjectId): Unit = {
    // Находим удаленные права
    val removedPermissions = dbpermissionsRemoved.find(MongoDBObject
    ("item_id" -> id ,
      "recordType" -> "account")).toIterable


    // Добавляем права
    removedPermissions.map( dbo => {
      new PermissionRecord(id, dbo.as[ObjectId]("userId"),
        {
          val permissions = dbo.as[DBObject]("permissions").filter(_._2 != null).map{case (k,v) => (k, v.asInstanceOf[Boolean]) }
          permissions.toMap
        })
    }).foreach(p => permEd.addPermissionRecord(p.userId,"account", p.item_id, p.permissions))

    // Чистим свалку
    removedPermissions.foreach(p => dbpermissionsRemoved.remove(MongoDBObject("_id" -> p("_id"))))

    dbaccounts.update(MongoDBObject("_id" -> id),MongoDBObject("$unset" -> MongoDBObject("removed" -> 1, "removalTime" -> 1)))
  }

  def removeEquipment(id: ObjectId): Unit = {
    log.debug("Removing equipment " + id);
    val date = new Date
    val dbo = dbequipments.findOne(MongoDBObject("_id" -> id))
    if(dbo.isDefined) {
      val doc = dbo.get
      val uid = doc.getAs[String]("uid")
      val removalMark = MongoDBObject("removed" -> true, "removalTime" -> date) ++
        (if (uid.isDefined) MongoDBObject("prevUid" -> uid.get) else MongoDBObject())
      dbequipments.update(new BasicDBObject("_id", id), new BasicDBObject("$set", removalMark))

      commandGateway.sendAndWait(
        new EquipmentObjectChangeCommand(id, uid, None),Set("EquipmentObjectChange", "EquipmentDataSet", "admin").toArray, "System")
    }
    else
      throw new RuntimeException("Object does not exist")
  }

  def restoreEquipment(id: ObjectId): Unit = {
    log.debug("Restoring equipment " + id );
    val dbo = dbequipments.findOne(MongoDBObject("_id" -> id))
    if(dbo.isDefined) {
      val doc = dbo.get
      val uid = doc.getAs[String]("prevUid")

      dbequipments.update(
        MongoDBObject("_id" -> id), MongoDBObject("$unset" -> MongoDBObject("prevUid" -> 1,"removed" -> 1, "removalTime" -> 1)))

      if(uid.isDefined && or.getObjectByUid(uid.get) != null)
        commandGateway.sendAndWait(
          new EquipmentObjectChangeCommand(id,None,uid),Set("EquipmentObjectChange", "EquipmentDataSet", "admin").toArray, "System")
    }
    else
      throw new RuntimeException("Equipment does not exist")
  }

  def removeById(collectionName: String, id: ObjectId): Unit = {
    log.debug("Removing by id " + id + " within " + collectionName);
    val date = new Date
    val removalMark = MongoDBObject("removed" -> true, "removalTime" -> date)
    mdbm.getDatabase().getCollection(collectionName)
      .update(new BasicDBObject("_id", id), new BasicDBObject("$set", removalMark), false, false )
  }

  def restoreById(collectionName: String, id: ObjectId): Unit = {
    log.debug("Restoring by id " + id + " within " + collectionName);
    mdbm.getDatabase()(collectionName)
      .update(
        MongoDBObject("_id" -> id),
        MongoDBObject("$unset" -> MongoDBObject("removed" -> 1, "removalTime" -> 1)))
  }


  def removeObject(uid: String): Unit = {
    log.debug("Removing object uid = " + uid)
    val data = or.getObjectByUid(uid)
    val accountId = data.as[ObjectId]("account") // Аккаунт будет изменен на дефолтный чтобы у пользователя аккаунта не было прав на этот объект
    val oid = data.as[ObjectId]("_id")
    val usersPermissions = dbpermissions.find(MongoDBObject
      ("item_id" -> oid ,
        "recordType" -> "object"))
    log.info(s"Saving and removing usersPermissions for uid = $uid")
    usersPermissions.map(_.-("_id")).foreach(dbpermissionsRemoved.insert(_))

    permEd.removePermissionsForItem("object", oid)

    val date: Date = new Date
    val removalMark = MongoDBObject("removed" -> true, "removalTime" -> date, "prevAccount" -> accountId)
    commandGateway.sendAndWait(new ObjectDataSetCommand(uid,Map("account" -> defaultAccId),None), Set("admin", "ObjectDataSet").toArray, "System")
    dbobjects.update(new BasicDBObject("uid", uid), new BasicDBObject("$set", removalMark), false, false)

    or.setRemoved(uid, true);

    newRemovalPeriod(uid,date)
  }

  def isRemovedQuery: DBObject = MongoDBObject("removed" -> true)
  def isNotRemovedQuery: DBObject = MongoDBObject("removed" -> MongoDBObject("$exists" -> false))

  def accountExists(id: ObjectId) = {
    mdbm.getDatabase()("accounts").findOne(MongoDBObject("_id" -> id)).isDefined
  }


  def restoreObject(uid: String): Unit = {
    log.debug("Restoring object uid = " + uid)
    val data = or.getObjectByUid(uid)
    val accountId = data.as[ObjectId]("prevAccount")
    val oid = data.as[ObjectId]("_id")



    log.info(s"Restoring usersPermissions for uid = $uid")
    val removedPermissions = dbpermissionsRemoved.find(MongoDBObject
    ("item_id" -> oid ,
      "recordType" -> "object")).toIterable;

    removedPermissions.map( dbo => {
      new PermissionRecord(oid, dbo.as[ObjectId]("userId"),
        {
          val permissions = dbo.as[DBObject]("permissions").filter(_._2 != null).map{case (k,v) => (k, v.asInstanceOf[Boolean]) }
          permissions.toMap
        })
    }).foreach(p => permEd.addPermissionRecord(p.userId,"object", p.item_id, p.permissions))

    removedPermissions.foreach(p => dbpermissionsRemoved.remove(MongoDBObject("_id" -> p("_id"))))

    val date = new Date()
    dbobjects.update(
        MongoDBObject("uid" -> uid),
        MongoDBObject(
          "$unset" -> MongoDBObject("removed" -> 1, "removalTime" -> 1, "prevAccount" -> 1)))
    or.setRemoved(uid, false);

    if(accountExists(accountId))
      commandGateway.sendAndWait(new ObjectDataSetCommand(uid,Map("account" -> accountId),None),Set("admin", "ObjectDataSet").toArray, "System")
    closeRemovalPeriod(uid,date)
  }



  def newRemovalPeriod(uid: String, date: Date): Unit = {
    log.debug("Setting new removal period " + date)
    val coll = mdbm.getDatabase().apply("removalPeriods")
    val doc = coll.findOne(MongoDBObject("uid" -> uid))
    val dbo = new MongoDBObject(doc.getOrElse(MongoDBObject("uid" -> uid, "periods" -> new BasicDBList())))

    val periods = dbo.get("periods").get.asInstanceOf[BasicDBList]
    periods += MongoDBObject("begin" -> date, "end" -> new Date(java.lang.Long.MAX_VALUE))

    if(doc.isDefined) {
      coll.update(MongoDBObject("_id" -> dbo.get("_id").get),MongoDBObject("$set" -> MongoDBObject("periods" -> periods)))
    }
    else {
      coll.save(MongoDBObject("periods" -> periods, "uid" -> uid))
    }
  }


  def closeRemovalPeriod(uid: String, date: Date): Unit = {
    log.debug("Closing removal period " + date)
    val coll = mdbm.getDatabase().apply("removalPeriods")
    val doc = coll.findOne(MongoDBObject("uid" -> uid))
    if(doc.isDefined) {
      val dbo = new MongoDBObject(doc.get)
      val periods = dbo.get("periods").get.asInstanceOf[BasicDBList]
      if(periods.nonEmpty) {
        val lastPeriod = periods.last.asInstanceOf[BasicDBObject]
        lastPeriod.put("end", date);
        coll.update(MongoDBObject("uid" -> uid),MongoDBObject("$set" -> MongoDBObject("periods" -> periods)))
      }
      else throw new IllegalArgumentException("Некорректный документ (RRM)")
    }
    else {
      throw new IllegalArgumentException("Данный объект не имеет записанного периода удаления")
    }
  }

}
