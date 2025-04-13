package ru.sosgps.wayrecall.billing

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.{SortDirection, ExtDirectStoreReadRequest}
import com.mongodb.BasicDBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.security.PermissionsEditor
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, UserRolesChecker, MongoDBManager}
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._
import ru.sosgps.wayrecall.utils.web.{EDSJsonResponse, EDSMongoUtils}
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.DBQueryUtils._
/**
 * Created by IVAN on 28.05.2015.
 */


@ExtDirectService
class GroupsOfObjects extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model( "_id","name", "uid","userName", "objects","objectsNames")

  val name = "GroupsOfObjects"
  val idProperty = "_id"
  val lazyLoad = false

  val fieldsNames = model.fieldsNames - "userName" - "objectsNames"

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def readAll(request: ExtDirectStoreReadRequest) = {
    EDSMongoUtils.searchFieldFromQuery(request)
    val collection = mdbm.getDatabase().apply("groupsOfObjects")
    var result: Iterator[DBObject] = Iterator.empty

    var permittedUserIds = roleChecker.getPermForCurrentUser().map(_.as[ObjectId]("_id")).toSet

    val allGroups = collection.find(
      EDSMongoUtils.searchAndFilters(request), //+=("uid" -> uid.asInstanceOf[ObjectId]),
      model.fieldsNames.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1)).toIterator
    result = allGroups.filter(item => permittedUserIds.contains(item.get("uid").asInstanceOf[ObjectId]))
      .map(item => item + ("objects" -> item.get("objects").asInstanceOf[BasicDBList].toIterable)) // Преобрзуется в скаловский Map
      .map(item => item + ("objects" -> excludeRemovedObjects(item("objects").asInstanceOf[Iterable[Any]])))
      .map(item =>
      item
        + ("userName" ->  getUserNameById(item("uid").asInstanceOf[ObjectId]))
        + ("objectsNames" -> getObjectsNames(item("objects").asInstanceOf[Iterable[Any]])))
    EDSJsonResponse(result)
  }

  @ExtDirectMethod
  def remove(maps: Seq[String]) = {
    val collection = mdbm.getDatabase().apply("groupsOfObjects")
    maps.foreach(_id => {
      val objId = new ObjectId(_id)
      debug("remove group of objects with id="+_id)
      collection.remove(MongoDBObject("_id"->objId))
    })
  }

  @ExtDirectMethod
  def update(map0: Map[String, Object]) = {
    debug("map="+map0)
    val collection = mdbm.getDatabase().apply("groupsOfObjects")
    val id=map0.get("_id")
    val userId=new ObjectId(map0("uid").asInstanceOf[String])
    val data=map0-"_id"+("uid"->userId)
    if(id.isDefined) {
      map0.get("_id").foreach(id => {
        collection.update(MongoDBObject("_id" -> new ObjectId(id.asInstanceOf[String])), $set(data.toSeq:_*),concern = WriteConcern.Safe)
      })
    }
    else
      collection.insert(data)
    data
  }



  protected def getUserNameById(uid: ObjectId) = {
    val user = mdbm.getDatabase().apply("users").findOne(MongoDBObject("_id" -> uid), MongoDBObject("name" -> 1))
    val userName =
      if (user.isDefined)
        user.get.getAsOrElse[String]("name", "")
    userName
  }

  protected def getObjectNameByUid(uid: String): Option[String] = {
    val carObject = mdbm.getDatabase().apply("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("name" -> 1))
    val objectName = carObject.flatMap(_.getAs[String]("name"))
    objectName
  }

  protected def getObjectsNames(objects: Iterable[Any]) = {
    debug("getObjectsNames objectList=" + objects)
    val objectNamesString = objects
      .flatMap(item => getObjectNameByUid(item.asInstanceOf[BasicDBObject].get("uid").asInstanceOf[String]))
      .mkString(" ; ")
   objectNamesString
  }



  protected def excludeRemovedObjects(objects: Iterable[Any]) = {
    objects.filterNot(item => or.isRemoved(item.asInstanceOf[DBObject].get("uid").asInstanceOf[String]))
  }
}

@ExtDirectService
class ObjectsGroupStore extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id",  "name",    "customName",    "comment",    "uid",
    "type",    "contract",    "cost",    "marka",    "model",    "gosnumber",    "VIN")

  val name = "ObjectsGroupStore"
  val idProperty = "_id"
  val lazyLoad = false

  val fieldsNames = model.fieldsNames

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadObjectsInGroup(request: ExtDirectStoreReadRequest) = {
    val groupId = request.getParams.getOrDefault("groupId", "").asInstanceOf[String];
    val objectsData =
      if (!groupId.isEmpty) {
        EDSMongoUtils.searchFieldFromQuery(request)
        val objectsIds = mdbm.getDatabase().apply("groupsOfObjects")
          .findOne(MongoDBObject("_id" -> new ObjectId(groupId)), MongoDBObject("objects" -> 1))
          .map(objList => objList.get("objects").asInstanceOf[BasicDBList]
                .map(obj => obj.asInstanceOf[BasicDBObject].get("uid").asInstanceOf[String])
          )
        mdbm.getDatabase().apply("objects").find(("uid" $in objectsIds.get) ++ notRemoved,fieldsNames.map( _ -> 1).toMap).sort(MongoDBObject("name" -> 1))
        //.find(MongoDBObject("uid"->MongoDBObject("$in"->objectsIds.get)))
      }
      else Iterator.empty
    EDSJsonResponse(objectsData)
  }

}

@ExtDirectService
class PermittedObjectsStore  extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id",  "name",    "customName",    "comment",    "uid",
    "type",    "contract",    "cost",    "marka",    "model",    "gosnumber",    "VIN")

  val name = "PermittedObjectsStore"
  val idProperty = "_id"
  val lazyLoad = false

  val fieldsNames = model.fieldsNames

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadAll(request: ExtDirectStoreReadRequest) = {
    debug("params"+request.getParams.toString)
    val uid=request.getParams.getOrDefault("uid","").asInstanceOf[String]
    val objects =
    if(!uid.isEmpty) {
      val permittedObjectsIds = roleChecker.getInferredPermissionRecordIds(getUserNameById(new ObjectId(uid)), "object")
      mdbm.getDatabase()("objects").find(("_id" $in permittedObjectsIds) ++ notRemoved, fieldsNames.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1))
    }
    else {
      val permittedObjectsIds = roleChecker.getInferredPermissionRecordIds(roleChecker.getUserName(), "object")
      mdbm.getDatabase()("objects").find(("_id" $in permittedObjectsIds) ++ notRemoved, fieldsNames.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1))
    }
    //else Iterator.empty
    objects
  }

  private def getUserNameById(uid: ObjectId) = {
    val user = mdbm.getDatabase().apply("users").findOne(MongoDBObject("_id" -> uid), MongoDBObject("name" -> 1))
    val userName =
      if (user.isDefined)
        user.get.getAsOrElse[String]("name", "")
      else ""
    userName
  }
}

@ExtDirectService
class UserGroupsOfObjects extends GroupsOfObjects with EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  override val name = "UserGroupsOfObjects"

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
    override def readAll(request: ExtDirectStoreReadRequest) = {
    EDSMongoUtils.searchFieldFromQuery(request)
    val collection = mdbm.getDatabase().apply("groupsOfObjects")
    var result: Iterator[DBObject] = Iterator.empty
    val userId = roleChecker.getUserIdByName(roleChecker.getUserName())
    val allGroups = collection.find(
      EDSMongoUtils.searchAndFilters(request), //+=("uid" -> uid.asInstanceOf[ObjectId]),
      model.fieldsNames.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1)).toIterator

    result = allGroups.filter(item => userId == item.get("uid").asInstanceOf[ObjectId])
      .map(item => item + ("objects" -> item.get("objects").asInstanceOf[BasicDBList].toIterable))
      .map(item => item + ("objects" -> excludeRemovedObjects(item("objects").asInstanceOf[Iterable[Any]])))
      .map(item =>
      item
        //  + ("userName" ->  getUserNameById(item.get("uid").asInstanceOf[ObjectId]))
        + ("objectsNames" -> getObjectsNames(item("objects").asInstanceOf[Iterable[Any]])))
    EDSJsonResponse(result)

  }

  @ExtDirectMethod
  override def update(map0: Map[String, Object]) = {
    debug("map="+map0)
    val collection = mdbm.getDatabase().apply("groupsOfObjects")
    val id=map0.get("_id")
    val userId = roleChecker.getUserIdByName(roleChecker.getUserName())
    //val userId=new ObjectId(map0("uid").asInstanceOf[String])
    val data=map0-"_id"+("uid"->userId)
    if(id.isDefined) {
      map0.get("_id").foreach(id => {
        collection.update(MongoDBObject("_id" -> new ObjectId(id.asInstanceOf[String])), $set(data.toSeq:_*),concern = WriteConcern.Safe)
      })
    }
    else
      collection.insert(data)
    data
  }
}
