package ru.sosgps.wayrecall.billing.user


import java.util

import com.mongodb.casbah.Imports
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import ru.sosgps.wayrecall.core.{MongoDBManager, UserRolesChecker}
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.{ExtDirectStoreReadRequest, ExtDirectStoreResult}
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.utils.ScalaConverters._

import collection.JavaConversions.{collectionAsScalaIterable, mapAsJavaMap, mapAsScalaMap, seqAsJavaList}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.web.bind.annotation.RequestParam
import ru.sosgps.wayrecall.utils.web.extjsstore.{EDSStoreServiceDescriptor, StoreAPI}
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import com.mongodb.{DBCursor, DBObject, casbah}

import scala.Predef._
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.typingMap
import ru.sosgps.wayrecall.utils.impObjectId
import ru.sosgps.wayrecall.utils.DBQueryUtils._
import ru.sosgps.wayrecall.billing.security._

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable


@ExtDirectService
class UsersPermissionsService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id", "item_id", "name", "view", "sleepersView", "control","block","getCoords","restartTerminal", "paramsView", "paramsEdit","fuelSettings","sensorsSettings",
    /*"customName",*/ "uid", /*"permissions",*/ "recordType", "userId")
  val name = "UserPermissionsService"
  val idProperty = "_id"
  val lazyLoad = false

  //override val storeType = "Ext.data.TreeStore"

  val objectIdFields = Set("userId", "item_id")

  val updatebleFields = model.fieldsNames - "_id"

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var permissions: PermissionsEditor = null

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_MODIFY)
  @StoreAPI("update")
  def update(list: Seq[Map[String, Any]]) = {
    list map updateOne
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_MODIFY)
  @StoreAPI("create")
  def create(list: Seq[Map[String, Any]]): Seq[Map[String, Any]] = {
    debug("UsersPermissionsService create=" + list.mkString("[\n", ",\n", "\n]"))
    //println(Thread.currentThread().getStackTrace().mkString("\n"))
    list map updateOne
  }

  def updateOne(map0: Map[String, Any]): Map[String, Any] = {

    debug("updateOne with arg:" + map0)

    val map = map0
    val pid = permissions.addPermissionRecord(
      map.as[String]("userId"),
      map.as[String]("recordType"),
      map.as[String]("item_id"),
      map.filterKeys(permissions.permissionsSet).mapValues(v => v match {
        case b: Boolean => b
        case "" => false
        case s: String => s.toBoolean
      })
    )

    assert(pid != null)

    map + ("_id" -> pid)

  }

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadAll(request: ExtDirectStoreReadRequest) = {

    debug("request = " + request)
    val params = request.getParams.toMap
    val userId = params("userId").asInstanceOf[String]

    debug("userId = " + userId)
    val userOid = new ObjectId(userId)

    val accountsPermissionsRecords = permissions.getPermissionsRecords(userOid, "account")

    val explicitObjects = permissions.getPermissionsRecords(userOid, "object") //.map(dbo => (dbo.as[ObjectId]("item_id"), dbo)).toMap


    val accounts = accountsPermissionsRecords.flatMap(accPermission => {
      val targetAccount = mdbm.getDatabase()("accounts").findOne(
        notRemoved ++
        MongoDBObject("_id" -> accPermission("item_id")),
        model.fieldsNames.map(_ -> 1).toMap
      )
      targetAccount.map(_ ++ ((accPermission - "permissions") ++ accPermission.as[DBObject]("permissions")) ++
        MongoDBObject(
          "userId" -> userId,
          "recordType" -> "account"
        ))
    })

    val objects = explicitObjects.flatMap(objPermission => {
      val targetObject = mdbm.getDatabase()("objects").findOne(
        notRemoved ++
        MongoDBObject("_id" -> objPermission("item_id")),
        model.fieldsNames.map(_ -> 1).toMap
      )
      targetObject match {
        case Some(objdata) =>
          Some(objdata  ++ ((objPermission - "permissions") ++ objPermission.as[DBObject]("permissions")) ++
            MongoDBObject(
              "userId" -> userId,
              "recordType" -> "object"
            ))
        case None => warn("objPermission" + objPermission + " does not point to real item"); None
      }
    })
    val result = accounts ++ objects
    EDSJsonResponse(result)

  }


  private def getAccountObjects(accountId: ObjectId, fields: DBObject = model.fieldsNames.map(_ -> 1).toMap): MongoCollection#CursorType = {
    mdbm.getDatabase()("objects").find(
      MongoDBObject("account" -> accountId),
      fields
    )
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_MODIFY)
  @StoreAPI("destroy")
  def remove(request: HttpServletRequest, response: HttpServletResponse, /* map: Map[String, Any]*/ list: Seq[Map[String, Any]]) {
    list map removeOne
  }

  def removeOne(map0: Map[String, Any]) = {
    val map = map0
    debug("UsersPermissionsService destroy=" + map)
    val pid = permissions.removePermissionRecord(
      map.as[String]("userId"),
      map.as[String]("recordType"),
      map.as[String]("item_id")
    )
    assert(pid != null)

    map + ("_id" -> pid)
  }

  @ExtDirectMethod
  def getPermittedUsersCount(id: String, recType: String): Integer = {
    require(id!=null)
    val currentUserPermUserIds=roleChecker.getPermForCurrentUser().map(_.as[ObjectId]("_id")).toSet
    val objectIds= permissions.getPermittedUsers(recType, new ObjectId(id)).filter(userId=>currentUserPermUserIds.contains(userId))
    objectIds.size
  }

  @ExtDirectMethod
  def providePermissions(toUpdate: util.ArrayList[util.HashMap[String, Any]],toRemove: util.ArrayList[util.HashMap[String, Any]],recType: String,recId: String){
    val recordId= new ObjectId(recId)
    if (toUpdate.isEmpty) debug("nothing to update")
    else {
      toUpdate.foreach(rec=>{
        val userId=new ObjectId(rec.get("_id").asInstanceOf[String])

        val permissionMap =rec.filterKeys(permissions.permissionsSet).mapValues(v => v match {
          case b: Boolean => b
          case "" => false
          case s: String => s.toBoolean
        }).toMap
        permissions.addPermissionRecord(userId, recType, recordId, permissionMap)
      })
    }
    if (toRemove.isEmpty) debug("nothing to remove")
    else {
      toRemove.foreach(rec=>{
        val userId=new ObjectId(rec.get("_id").asInstanceOf[String])
        permissions.removePermissionRecord(userId, recType, recordId)
      })
    }
  }

}


import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._

@ExtDirectService
class UserPermissionSelectionService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  //val model = Model("_id", "name", "recordType")
  val model = Model("_id", "item_id", "name", "view", "sleepersView", "control","block","getCoords","restartTerminal", "paramsView", "paramsEdit","fuelSettings","sensorsSettings",
    "uid", /*"customName",*/ /*"permissions",*/ "recordType", "userId")
  val name = "UserPermissionSelectionService"
  val idProperty = "_id"
  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var roleChecker: UserRolesChecker = null


  val permissionsDbName: String = "usersPermissions"


  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadAll(request: ExtDirectStoreReadRequest): ExtDirectStoreResult[scala.collection.Map[String, AnyRef]] = {

    debug("request = " + request)

    val params = request.getParams.toMap
    val ItemType = params.get("ItemType").flatMap(Option.apply)

    val userId = params("userId").asInstanceOf[String]

    val collection: casbah.MongoCollection = mdbm.getDatabase().apply(permissionsDbName)

    // Множество аккаунтов или объектов пользователя
    val settedItems = collection.find(
      MongoDBObject(
        "userId" -> new ObjectId(userId),
        "recordType" -> (ItemType match {
          case None => "account"
          case Some(_) => "object"
        })
      ),
      MongoDBObject(
        "item_id" -> 1
      )
    ).map(_.as[ObjectId]("item_id")).toSet

    debug("settedItems = " + settedItems)

    // Объекты и аккаунты, на которые пользователь биллинга может дать права другому пользователю
    ItemType match {
      case None => {
        val accFilter = userTypePermittedItemsFilter("account")
        val accounts = mdbm.getDatabase()("accounts").find(notRemoved ++ searchAndFilters(request),
          model.fieldsNames.map(_ -> 1).toMap
        ).toIterator.filter(dbo => !settedItems.contains(dbo.as[ObjectId]("_id")) && accFilter(dbo.as[ObjectId]("_id")))
        val result = accounts.map(_ + ("recordType" -> "account"))
        EDSJsonResponse(result)
      }

      case Some("object") => {
        val objectsFilter = userTypePermittedItemsFilter("object")
        val objects = mdbm.getDatabase()("objects").find(notRemoved ++ searchAndFilters(request),
          model.fieldsNames.map(_ -> 1).toMap
        ).toIterator
          .filter(dbo => !settedItems.contains(dbo.as[ObjectId]("_id")) && objectsFilter(dbo.as[ObjectId]("_id")))
          .map(_ + ("recordType" -> "object"))
        EDSJsonResponse(objects)
      }
    }
  }

  def userTypePermittedItemsFilter(itemType: String): ObjectId => Boolean = {
    val roles = roleChecker.getUserAuthorities()
    if (roles.contains("admin")) {
      _ => true
    }
    else if (roles.contains("superuser")) {
      itemType match {
        case "account" => {
          val permAccs = roleChecker.getInferredPermissionRecordIds(roleChecker.getUserName(), "account")
          objectId => permAccs.contains(objectId)
        }
        case "object" => {
          val permObjs = roleChecker.getInferredPermissionRecordIds(roleChecker.getUserName(), "object")
          objectId => permObjs.contains(objectId)
        }
        case _ => _ => false
      }
    }
    else _ => false
  }
}

@ExtDirectService
class PermittedItemsService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id", "item_id", "name", "view", "sleepersView","control","block","getCoords","restartTerminal", "paramsView", "paramsEdit","fuelSettings","sensorsSettings",
    "comment", "inherited",  "uid", /*"customName",*/ /*"permissions",*/ "recordType", "userId")
  val name = "PermittedItemsService"
  val idProperty = "_id"
  val lazyLoad = false

  val objectIdFields = Set("userId", "item_id")

  val updatebleFields = model.fieldsNames - "_id"

  @Autowired
  var permEd: PermissionsEditor = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadAll(request: ExtDirectStoreReadRequest) /*: ExtDirectStoreResponse  [scala.collection.Map[String, AnyRef]]  */ = {
    debug("request = " + request)
    val params = request.getParams.toMap
    val permitted = params("permitted").asInstanceOf[Boolean]
    val ItemType = params /*.get*/ ("type").asInstanceOf[String] //.flatMap(Option.apply)
    val oid = params("oid").asInstanceOf[String]
    debug("oid=" + oid)
    debug("ItemType=" + ItemType)
    val users = if (!permitted) {
      getNotPermittedUsers(oid, ItemType)
    } else getPermittedUsersDetails(oid, ItemType)

    users
  }
//  def getPermForCurrentUser() = {
//    var allObjects : Iterator[DBObject] = Iterator.empty
//    val userName = roleChecker.getUserName()
//    if(roleChecker.hasRequiredAuthorities(Seq(new SimpleGrantedAuthority("admin"),new SimpleGrantedAuthority("UserView")))) {
//      allObjects = mdbm.getDatabase().apply("users").find(MongoDBObject(), model.fieldsNames.map(_ -> 1).toMap)
//    }
//    else if(roleChecker.hasRequiredAuthorities(Seq(new SimpleGrantedAuthority("superuser"),new SimpleGrantedAuthority("UserView")))) {
//      val creatorId=roleChecker.getUserIdByName(userName)
//      val creator=if(creatorId==null) userName else creatorId
//      allObjects = mdbm.getDatabase().apply("users").find($or("name"->userName,"creator"->creator),model.fieldsNames.map(_ -> 1).toMap)
//    }
//    allObjects
//  }

  def getPermittedUsersDetails(oid: String, recordType: String)={
    val itemId=new ObjectId(oid)
    val permUserIds= permEd.getPermittedUsers(recordType, itemId)
    val currentUserPermUserIds=roleChecker.getPermForCurrentUser().map(_.as[ObjectId]("_id")).toSet
    debug("currentUserPermUserIds="+currentUserPermUserIds)
    val permUsersDetails=permUserIds.filter(userId=>(getUser(userId)!=null && currentUserPermUserIds.contains(userId))).map(userId=> {
      val userInfo = getUser(userId)
      val permissions: BasicDBObject=permEd.getInferredPermissionRecord(userId, recordType, itemId)
        .map(rec=>rec.getAsOrElse[BasicDBObject]("permissions",new BasicDBObject())).get
      val inherited=(recordType=="object" && !permEd.getPermissionsRecord(userId, "object", itemId).isDefined)
      Map("inherited"->inherited)++(userInfo++permissions)
      }
    )
    permUsersDetails
  }

  def getNotPermittedUsers(oid: String, recordType: String)={
    val allUsers= roleChecker.getPermForCurrentUser()
    val permUserIds= permEd.getPermittedUsers(recordType, new ObjectId(oid))
    allUsers.filter(user=>{!permUserIds.contains(user.get("_id"))})
  }

  private def getUser(objectId: ObjectId): DBObject = {
    mdbm.getDatabase.underlying.getCollection("users").findOne(MongoDBObject("_id" -> objectId),MongoDBObject("_id"->1,"name"->1,"comment"->1))
  }

  //private def getAllUsers: Iterator[DBObject] = {
  //  mdbm.getDatabase.apply("users").find(MongoDBObject(),MongoDBObject("_id"->1,"name"->1,"comment"->1))
  //}
}