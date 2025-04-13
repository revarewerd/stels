package ru.sosgps.wayrecall.security

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.utils.errors.NotPermitted

import scala.collection.JavaConversions.iterableAsScalaIterable

/**
 * class provides methods to work with Permissions Records.
 * This class just contains methods to avoid duplicating code so could be treated as mixin and has no other semantic
 */
abstract class PermissionsManager extends grizzled.slf4j.Logging {

  var mdbm: MongoDBManager

  val supportedRecordTypes = Set("account", "object")

  def getUserAuthorities() : Array[String] = {
    val auth = SecurityContextHolder.getContext().getAuthentication().getAuthorities
    auth.map(_.getAuthority).toArray
  }

  def getUserName() : String = {
    val userName = SecurityContextHolder.getContext().getAuthentication().getName
    userName
  }

  def getCurrentUserId():ObjectId = {
    getUserIdByName(getUserName())
  }

  def checkAdminAuthority(): Boolean = {
    val roles: Array[String] =getUserAuthorities()
    roles.contains("admin")
  }

  def hasPermissions(userName: String, recType: String, recId: Any) : Boolean = {
    val permitted = recType match {
      case "account" => {
        val permittedItems = getInferredPermissionRecordIds(userName, "account")
        permittedItems.contains(recId.asInstanceOf[ObjectId])
      }
      case "object" => {
        val permittedItems = getInferredPermissionRecordIds(userName, "object")
        val objectId = mdbm.getDatabase().apply("objects").findOne(new BasicDBObject("uid", recId.asInstanceOf[String])).flatMap(item => item.getAs[ObjectId]("_id"))
        if (!objectId.isDefined) throw new IllegalArgumentException("cant find object with _id= " + objectId)
        permittedItems.contains(objectId.get)
      }
      case "equipment" => {
        val eqRec = mdbm.getDatabase().apply("equipments").findOne(new BasicDBObject("_id", recId.asInstanceOf[ObjectId]))
        if (!eqRec.isDefined) throw new IllegalArgumentException("cant find equipment with _id= " + recId)
        val accountId = eqRec.get.getAs[ObjectId]("accountId")
        val uid = eqRec.get.getAs[String]("uid")
        uid.isDefined && hasPermissions(userName, "object", uid.get) || accountId.isDefined && hasPermissions(userName, "account", accountId.get)
      }
      case "user" => {
        val user = mdbm.getDatabase().apply("users").findOne(new BasicDBObject("_id", recId.asInstanceOf[ObjectId]))
        if (!user.isDefined) throw new IllegalArgumentException("cant find user with _id= " + recId)
        val creator = user.get.getOrElse("creator", null)
        debug("creator="+creator)
        val isCreator = creator match {
          case null | "" => {
            false
          }
          case creatorName: String => {
            creatorName == userName
          }
          case creatorId: ObjectId => {
            creatorId == getUserIdByName(userName)
          }
        }
        isCreator || (getUserIdByName(userName) == user.get.getAs[ObjectId]("_id").get)
      }
    }
    permitted
  }

  /**
   * loads all explicit permissions records of specified type from database
   * @param userObjectId objectId for user for whom permission is requested
   * @param recType type of record. actually "object" or "account"
   * @return all explicit permissions records of specified type from database
   */
  def getInferredPermissionRecordIds(userName: String, recType: String): Seq[ObjectId] = {
    checkRectype(recType)
    recType match {
      case "account" => getPermissionsRecords(userName, recType).map(_.getAs[ObjectId]("item_id").get)
      case "object" => {
        val permittedRecords = getPermissionsRecords(userName, recType).map(_.getAs[ObjectId]("item_id").get)
        val accountIds = getPermissionsRecords(userName, "account").map(_.getAs[ObjectId]("item_id").get)
        var objects=accountIds.flatMap(itemId => {
          //debug("itemId "+itemId)
          mdbm.getDatabase().apply("objects").find(MongoDBObject("account"->itemId)).map(_.getAs[ObjectId]("_id").get)
        })
        permittedRecords++objects
      }
    }
  }

  def getPermissionsRecords(recType: String): Seq[DBObject] = {
    val userName=this.getUserName();
    getPermissionsRecords(userName, recType);
  }

  def getPermissionsRecords(userName: String, recType: String): Seq[DBObject] = {
    val userInfo = mdbm.getDatabase().apply("users").findOne(MongoDBObject("name"->userName)).getOrElse(null);
    if(userInfo!=null) {
      getPermissionsRecords(userInfo("_id").asInstanceOf[ObjectId], recType)
    }
    else throw new NotPermitted(s"cant find user with name = '$userName'")
  }

  def getPermissionsRecords(userObjectId: ObjectId, recType: String): Seq[DBObject] = {
    checkRectype(recType)

    mdbm.getDatabase()("usersPermissions").find(
      MongoDBObject(
        "userId" -> userObjectId,
        "recordType" -> recType
      ),
      MongoDBObject("item_id" -> 1, "permissions" -> 1, "customName" -> 1)
    ).toSeq
  }

  @inline
  final protected def checkRectype(recType: String) {
    require(supportedRecordTypes.contains(recType), "recordType must be in " + supportedRecordTypes)
  }

  /**
   * returns permissions record for requested item as stored in database
   * @param userObjectId objectId for user for whom permission is requested
   * @param recType type of record. actually "object" or "account"
   * @param item_id objectId of object or account requested
   * @return permission record from database if this record is explicitly stored, None otherwise
   * @see [[ru.sosgps.wayrecall.security.PermissionsManager.getInferredPermissionRecord( )]] to get inferred permissions
   */
  def getPermissionsRecord(userObjectId: ObjectId, recType: String, item_id: ObjectId): Option[DBObject] = {
    checkRectype(recType)

    mdbm.getDatabase()("usersPermissions").findOne(
      MongoDBObject(
        "userId" -> userObjectId,
        "recordType" -> recType,
        "item_id" -> item_id
      ),
      MongoDBObject("item_id" -> 1, "permissions" -> 1, "customName" -> 1)
    )
  }

  /**
   * returns seq of users who has access for specified object
   * @param recType type of record. actually "object" or "account"
   * @param item_id objectId of object or account requested
   * @return seq of users who has access for specified object
   */
  def getPermittedUsers(recType: String, item_id: ObjectId): Seq[ObjectId] = {
    checkRectype(recType)

    def accountPermissions(accid: ObjectId) = {
      mdbm.getDatabase()("usersPermissions").find(
        MongoDBObject(
          "recordType" -> "account",
          "item_id" -> accid
        ),
        MongoDBObject("userId" -> 1, "permissions" -> 1, "customName" -> 1)
      )
    }

    recType match {
      case "account" => accountPermissions(item_id) /*.filter(isAllowingPermissionRecord)*/ .map(dbo => dbo.as[ObjectId]("userId")).toSeq

      case "object" => {
        val explicitObjectPermissions = mdbm.getDatabase()("usersPermissions").find(
          MongoDBObject(
            "recordType" -> "object",
            "item_id" -> item_id
          ),
          MongoDBObject("userId" -> 1, "permissions" -> 1, "customName" -> 1)
        ).map(dbo => (dbo.as[ObjectId]("userId"), dbo)).toMap

        debug(explicitObjectPermissions)

        (accountPermissions(getObjectAccount(item_id))
//        .filter(
//          accsRecord => {
//            val option = explicitObjectPermissions.get(accsRecord.as[ObjectId]("userId"))
//
//            debug("explicitObjectPermissions=" + option)
//
//            option.
//              map(isAllowingPermissionRecord).
//              getOrElse(isAllowingPermissionRecord(accsRecord))
//          }
//        )
           .map(_.as[ObjectId]("userId")).toSet ++ explicitObjectPermissions.keySet).toSeq
      }

    }

  }

  def hasPermission(userObjectId: ObjectId, uid: String, requestedAuthority: String): Boolean = {

    val r: Boolean = {
      val permissionObject = getObjectPermission(userObjectId, uid)
      try {
        permissionObject.getAs[Boolean](requestedAuthority).getOrElse(false)
      }
      catch {
        case e: Exception =>
          warn("error processing permission " + permissionObject + " userId=" + userObjectId + " uid=" + uid, e); false
      }
    }

    debug("checkedAuthority:" + userObjectId +
      " uid=" + uid +
      " requestedAuthority=" + requestedAuthority +
      " r=" + r);
    r
  }

  /**
   * returns DBObject representation of inferred permissions for this object
   * @param userObjectId bjectId for user for whom permission is requested
   * @param uid object `uid`
   * @return  string representation of inferred permissions for this object
   */
  def getObjectPermission(userObjectId: ObjectId, uid: String): DBObject = {
    getObjectPermissionRecord(userObjectId, uid).map(_.as[DBObject]("permissions")).getOrElse(DBObject.empty)
  }

  /**
   * returns inferred permission record for this object.
   * actually returned record could be assigned to account, not really requested object
   * @param userObjectId bjectId for user for whom permission is requested
   * @param uid object `uid`
   * @return inferred permission record for this object.
   */
  def getObjectPermissionRecord(userObjectId: Imports.ObjectId, uid: String): Option[Imports.DBObject] = {
    getInferredPermissionRecord(userObjectId: ObjectId, "object",
      mdbm.getDatabase()("objects").findOne(
        MongoDBObject("uid" -> uid),
        MongoDBObject("_id" -> 1)
      ).get.as[ObjectId]("_id"))
  }

  /**
   * returns permissions record for requested item inferring permission if it was not explicitly stored
   * @param userObjectId objectId for user for whom permission is requested
   * @param recType type of record. actually "object" or "account"
   * @param item_id objectId of object or account requested
   * @return inferred permission record, None if no permission on this target is specified anywhere
   * @see [[ru.sosgps.wayrecall.security.PermissionsManager.getPermissionsRecord( )]] to get explicit permissions
   */
  def getInferredPermissionRecord(userObjectId: ObjectId, recType: String, item_id: ObjectId): Option[DBObject] = {
    checkRectype(recType)
    recType match {
      case "account" => getPermissionsRecord(userObjectId, recType, item_id)
      case "object" => getPermissionsRecord(userObjectId, recType, item_id)
        .orElse(getPermissionsRecord(userObjectId, "account", getObjectAccount(item_id)))
    }
  }

  protected def getObjectAccount(objectid: ObjectId): ObjectId = {
    mdbm.getDatabase()("objects").findOne(
      MongoDBObject("_id" -> objectid),
      MongoDBObject("account" -> 1)
    ).getOrElse(throw new IllegalArgumentException("cant find object with _id= " + objectid))
      .getAsOrElse[ObjectId]("account", throw new IllegalArgumentException("account for object " + objectid + " is not set"))
  }

  def isAllowingPermissionRecord(dbo: DBObject): Boolean = isAllowingPermission(dbo.as[DBObject]("permissions"))

  def isAllowingPermission(perm: DBObject): Boolean = perm.getAs[Any]("view").filter(None !=) match {
    case Some(b: Boolean) => b
    case Some("") => false
    case None => false
  }

    def getUserIdByName(userName : String) : ObjectId = {
      val userInfo = mdbm.getDatabase().apply("users").findOne(MongoDBObject("name"->userName));
      if(!userInfo.isEmpty) {
        userInfo.get.getAsOrElse[ObjectId]("_id",null)
      }
      else null//throw new IllegalArgumentException("cant find user with _name= " + userName)
    }

  //Billing Authorities
  @annotation.varargs
  def hasRequiredStringAuthorities(requiredAuth: String*): Boolean = {
    val currentAuthorities = getUserAuthorities().toSet
    requiredAuth.forall(currentAuthorities contains)
  }

}
