package ru.sosgps.wayrecall.billing.security

import javax.annotation.PostConstruct
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.{ExtDirectStoreReadRequest, SortDirection}
import com.mongodb.casbah.Imports._
import com.mongodb.{WriteConcern, casbah}
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{MongoDBManager, UserRolesChecker}
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.errors.NotPermitted
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._
import ru.sosgps.wayrecall.utils.web.extjsstore.{EDSStoreServiceDescriptor, StoreAPI}

/**
 * Created by IVAN on 03.06.2014.
 */
@ExtDirectService
class RolesService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id", "name", "authorities")

  val updatebleFields = Set("name", "authorities")

  var allAuthorities = Set(
    "AccountView","AccountCreate","AccountDataSet","AccountDelete",
    "TariffView","TariffPlanCreate","TariffPlanDataSet","TariffPlanDelete",
    "EquipmentView","EquipmentCreate","EquipmentDataSet", "EquipmentDelete",
    "ObjectView","ObjectCreate","ObjectDataSet","ObjectDelete",
    "ObjectRestore", "ObjectRemove", "EquipmentRestore", "EquipmentRemove",
    "EquipmentTypesView","EquipmentTypesCreate","EquipmentTypesDataSet","EquipmentTypesDelete",
    "UserView","UserCreate","UserDataSet","UserDelete",
    "ChangeRoles",
    "ChangePermissions",
    "ChangeBalance",
    "DealerBackdoor"
  )

  val name = "RolesService"
  val idProperty = "_id"

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  val dbName = "billingRoles"

  @PostConstruct
  def init() {
    val collection = mdbm.getDatabase().apply(dbName)
    info("ensuring index MongoDBObject(\"name\" -> 1)")
    collection.ensureIndex(MongoDBObject("name" -> 1), MongoDBObject("unique" -> true))
  }

  @ExtDirectMethod
  def checkAdminRole(): Boolean = {
    roleChecker.checkAdminAuthority()
  }

  @ExtDirectMethod
  def checkChangeRoleAuthority(): Boolean = {
    val roles: Array[String] =roleChecker.getUserAuthorities()
    roles.contains("ChangeRoles") && roles.contains("admin")
  }

  @ExtDirectMethod
  def getUserAuthorities(): Array[String] = {
    roleChecker.getUserAuthorities()
  }

  @ExtDirectMethod
  def getAvailableUserTypes(userName: String) = {
    val roles: Array[String] = roleChecker.getUserAuthorities()
    debug("roles" + roles.toList)
    if (roles.contains("admin"))
      Seq(
        Map("userType" -> "admin", "typeName" -> "Админ"),
        Map("userType" -> "superuser", "typeName" -> "Суперпользователь"),
        Map("userType" -> "user", "typeName" -> "Пользователь"),
        Map("userType" -> "servicer", "typeName" -> "Обслуживающий"))
    else if (roles.contains("superuser")) {
      if (userName == roleChecker.getUserName())
        Seq(Map("userType" -> "superuser", "typeName" -> "Суперпользователь"), Map("userType" -> "user", "typeName" -> "Пользователь"))
      else Seq(Map("userType" -> "user", "typeName" -> "Пользователь"))
    }
    else if (roles.contains("servicer")) {
      Seq(Map("userType" -> "servicer", "typeName" -> "Обслуживающий"))
    }
    else Seq.empty
  }

  //TODO: Доделать
  def getAvailableAuthorities(){
    val roles: Array[String] =roleChecker.getUserAuthorities()
    if(roles.contains("admin") && roles.contains("ChangeRoles")){
            allAuthorities
    }
    else  allAuthorities.filter(item=>roles.contains(item))
//      allRoles.map(item=> (item,roles.contains(item))).toMap
  }

  @ExtDirectMethod
  def updateUserRole(data: scala.collection.mutable.Map[String, Object]) = {
    debug("data=" + data)
    val roles: Array[String] = roleChecker.getUserAuthorities()
    if (roles.contains("admin") && roles.contains("ChangeRoles")) {
      if (!data.isEmpty) {
        val userId = data.remove("userId").map(a => new ObjectId(a.asInstanceOf[String]))
        data.put("userId", userId)
        debug("userId=" + userId)
        val user = mdbm.getDatabase().apply("billingPermissions").findOne(MongoDBObject("userId" -> userId)).getOrElse(MongoDBObject("_id" -> new ObjectId()))
        mdbm.getDatabase().apply("billingPermissions").update(MongoDBObject("_id" -> user.get("_id")), data, true, false, WriteConcern.SAFE)
      }
      data
    }
    else throw new NotPermitted("Недостаточно прав для изменения роли пользователя ")
  }
  @ExtDirectMethod
  def getUserRole(userId : String): Map[String, AnyRef] ={
    debug("userId="+userId)
    val permissionRec: Option[casbah.MongoCollection#T] =mdbm.getDatabase().apply("billingPermissions").findOne(MongoDBObject("userId" -> new ObjectId(userId)))
    if(permissionRec.isDefined){
      val templateId=permissionRec.get.getAsOrElse[String]("templateId","-1")
      val permissionRecMap: Map[String, AnyRef] = (permissionRec.get: MongoDBObject).toMap[String, AnyRef]
      if(!templateId.equals("-1")){
        val rec=mdbm.getDatabase().apply(dbName).findOne(MongoDBObject("_id"-> new ObjectId(templateId)))
        if(rec.isDefined){
          permissionRecMap ++ Map("authorities"->rec.get.getOrElse("authorities",Array.empty))
        }
        else permissionRecMap
      }
      else permissionRecMap
    }
    else {
      debug("permissions record undefined")
      Map("authorities"->Array.empty,"templateId"->"-1")
    }
  }
    //(value = ExtDirectMethodType.STORE_MODIFY)
  //@StoreAPI("update")
  @ExtDirectMethod
  def update(data: scala.collection.mutable.Map[String, Object]) = {
    debug("data="+data)
    val roles: Array[String] = roleChecker.getUserAuthorities()
    if (roles.contains("admin") && roles.contains("ChangeRoles")) {
      if (!data.isEmpty) {
        val id = data.remove("_id").map(a => new ObjectId(a.asInstanceOf[String])).getOrElse(new ObjectId())
        debug("data=" + data)
        mdbm.getDatabase().apply(dbName).update(MongoDBObject("_id" -> id), data,
          true, false, WriteConcern.SAFE)
      }
    }
    else throw new NotPermitted("Недостаточно прав для изменения роли пользователя ")
  }

//  @ExtDirectMethod(value = ExtDirectMethodType.STORE_MODIFY)
//  @StoreAPI("create")
//  def create(map0: Map[String, Object]) = {
//
//  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_MODIFY)
  @StoreAPI("destroy")
  def remove(request: HttpServletRequest, response: HttpServletResponse, maps: Seq[Map[String, Any]]) = {
    val roles: Array[String] =roleChecker.getUserAuthorities()
    if(roles.contains("admin") && roles.contains("ChangeRoles")) {
       maps.foreach(map => {
        val templateId=map("_id").toString
        info("removing userRole template " + templateId)
        val authorities=mdbm.getDatabase().apply(dbName).findOne(MongoDBObject("_id" -> new ObjectId(templateId))).get("authorities")
        val usersWithRemovedRole=mdbm.getDatabase().apply("billingPermissions").find(MongoDBObject("templateId" -> templateId))
        usersWithRemovedRole.foreach(userPerm=>{
          mdbm.getDatabase().apply("billingPermissions").update(MongoDBObject("_id" -> userPerm.get("_id")), $set("templateId"->"-1","authorities"->authorities),
            false, false, WriteConcern.SAFE)
        })
        mdbm.getDatabase().apply(dbName).remove(MongoDBObject("_id" -> new ObjectId(templateId)))
       })
    }
    else throw new NotPermitted("Недостаточно прав для удаления роли пользователя ")
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def readAll(request: ExtDirectStoreReadRequest) = {
    setDefaultSorterIfEmpty(request,"name", SortDirection.ASCENDING)
    val roles: Array[String] =roleChecker.getUserAuthorities()
    val userRoles = mdbm.getDatabase().apply(dbName).find(searchAndFilters(request), model.fieldsNames.map(_ -> 1).toMap)
    if(roles.contains("admin") && roles.contains("ChangeRoles")) {
      EDSJsonResponse(userRoles)
    }
    else {
      val userId = request.getParams.get("userId")
      debug("userId=" + userId)
      if (userId != null) {
        val userRole = getUserRole(userId.asInstanceOf[String]) //.asInstanceOf[Map[String,AnyRef]]
        if (!userRole.isEmpty) {
          debug("userRole=" + userRole)
          EDSJsonResponse(userRoles.filter(item => {
            item.get("_id") == userRole.get("templateId").getOrElse("-1")}
          ))
        }
        else EDSJsonResponse(Map.empty)
      }
      else {
        EDSJsonResponse(Map.empty)
      }
    }
  }
}
