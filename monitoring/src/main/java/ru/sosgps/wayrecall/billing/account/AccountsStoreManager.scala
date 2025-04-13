/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.billing.account

import javax.annotation.PostConstruct

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.account.commands._
import ru.sosgps.wayrecall.billing.equipment.EquipmentDataSetCommand
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectDataSetCommand
import ru.sosgps.wayrecall.billing.security.PermissionsEditor
import ru.sosgps.wayrecall.billing.tariff.{TariffDAO, TariffEDS}
import ru.sosgps.wayrecall.billing.tariff.commands.TariffPlanCreateCommand
import ru.sosgps.wayrecall.core.finance.TariffPlans
import ru.sosgps.wayrecall.core.{MongoDBManager, ObjectsRepositoryReader, SecureGateway, UserRolesChecker}
import ru.sosgps.wayrecall.utils.DBQueryUtils._
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.io.Utils
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.web.{EDSJsonResponse, EDSMongoUtils}

import scala.Predef._
import scala.collection.JavaConversions.mapAsJavaMap

/**
  * Component for reading account data
  *
  * @see [[ru.sosgps.wayrecall.billing.account.AccountData]] for editing account data
  */
@ExtDirectService
class AccountsStoreService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("currency", "plan", Field("balance", "float"), "_id", "name", "comment", "cost", "objectsCount", "equipmentsCount", "usersCount", "status", "paymentWay")
  val name = "AccountsData"
  val idProperty = "_id"
  val lazyLoad = false

  val savableFields = model.fieldsNames - "cost" - "objectsCount" - "usersCount"

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var commandGateway: SecureGateway = null;

  @Autowired
  var tariffPlans: TariffPlans = null

  @Autowired
  var tariffEDS: TariffEDS = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  var defaultAccId: ObjectId = null

  var dbaccounts: MongoCollection = null
  var dbobjects: MongoCollection = null
  var dbequipments: MongoCollection = null
  var dbpermissions: MongoCollection = null

  @PostConstruct
  def init() {
    dbequipments = mdbm.getDatabase().apply("equipments")
    dbaccounts = mdbm.getDatabase().apply("accounts")
    dbobjects = mdbm.getDatabase().apply("objects")
    dbpermissions = mdbm.getDatabase().apply("usersPermissions")

    info("ensuring index MongoDBObject(\"name\" -> 1)")
    dbaccounts.ensureIndex(MongoDBObject("name" -> 1), MongoDBObject("unique" -> true))
    info("ensuring default account")
    defaultAccId = getDefaultAccountId()
  }



  def getDefaultAccountId() = {
    info("getDefaultAccountId")
    val defaultAcc = dbaccounts.findOne($or("name" -> "Без Аккаунта", "default" -> true))
    defaultAcc match {
      case Some(acc) => acc.get("_id").asInstanceOf[ObjectId]
      case None => {
        info("createDefaultAccount")
        val accountId = new ObjectId()
        commandGateway.sendAndWait(new AccountCreateCommand(accountId, Map("default" -> true.asInstanceOf[AnyRef], "name" -> "Без Аккаунта", "plan" -> tariffEDS.getDefaultTariffId.toString)),
          Array("admin", "AccountCreate", "AccountDataSet", "ROLE_ADMIN", "ROLE_TARIFFER"), "System")
        accountId
      }
    }
  }

  @ExtDirectService
  def delete(ids: Seq[String]): Unit = {
    require(ids != null)
    ids.foreach(id => {
      val _id = new ObjectId(id)
      try {
        commandGateway.sendAndWait(new AccountDeleteCommand(_id), roleChecker.getUserAuthorities(), roleChecker.getUserName());
      }
      catch {
        case e: Exception => {
          throw new RuntimeException("При удалении записи произошла ошибка: " + Option(e.getCause()).map(_.getMessage()).getOrElse(e), e)
        }
      }
      Unit
    })
  }

  @ExtDirectMethod //(value = ExtDirectMethodType.STORE_MODIFY)
  // @StoreAPI("destroy")
  def remove(ids: Seq[String], params: java.util.Map[String, java.lang.Boolean]) = {
    debug(s"Params are: $params")
    require(ids != null)
    ids.foreach(id => {
      val _id = new ObjectId(id)
      try {
        commandGateway.sendAndWait(new AccountRemoveCommand(_id, params), roleChecker.getUserAuthorities(), roleChecker.getUserName());
      }
      catch {
        case e: Exception => {
          throw new RuntimeException("При удалении записи произошла ошибка: " + Option(e.getCause()).map(_.getMessage()).getOrElse(e), e)
        }
      }
      Unit
    })
  }

  //      info("removing from accounts " + map("_id").toString)
  //      info("removing account permissions")
  //      dbpermissions.remove(MongoDBObject("recordType"->"account","item_id"->new ObjectId(map("_id").toString)))
  //      info("set objects to default acc")
  //      val obj=dbobjects.find(MongoDBObject("account" ->new ObjectId(map("_id").toString)))
  //      if(!obj.isEmpty){
  //        obj.foreach(ob => {
  //            dbobjects.update(ob,$set("accountName"->defaultAcc.get("name"),"account"->defaultAcc.get("_id")))
  //            val uid=ob.get("uid")
  //            info("change equipments account "+ uid)
  //            dbequipments.update(MongoDBObject("uid" -> uid),
  //            $set("accountId" -> defaultAcc.get("_id")), false, true, WriteConcern.Safe)
  //          })
  //      }
  //      val wc = dbaccounts.remove(MongoDBObject("_id" -> new ObjectId(map("_id").toString)), WriteConcern.Safe)
  //})


  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {
    debug("AccountsStoreServic5e read " + request)
    EDSJsonResponse(getAllAccounts(request))
  }

  def getAllAccounts(request: ExtDirectStoreReadRequest) = {
    val tariffIdToName: Map[String, String] = mdbm.getDatabase()("tariffs").find(MongoDBObject(), MongoDBObject("name" -> 1))
      .map(dbo => (dbo.as[ObjectId]("_id").toString, dbo.as[String]("name"))).toMap.withDefault(arg => arg)

    EDSMongoUtils.searchFieldFromQuery(request)

    def calcTotalCost(account: DBObject): Long = tariffPlans.calcTotalCost(account)

    val currentUserPermUserIds = roleChecker.getPermForCurrentUser().map(_.as[ObjectId]("_id")).toSet
    val data = getPermittedAccounts(request).map(s => {
      s.put("plan", (tariffIdToName(s.as[String]("plan"))))
      s.put("cost", calcTotalCost(s))
      s.put("objectsCount", dbobjects.find(notRemoved ++ MongoDBObject("account" -> s("_id"))).count)
      s.put("equipmentsCount", dbequipments.find(notRemoved ++ MongoDBObject("accountId" -> s("_id"))).count)
      s.put("usersCount", roleChecker.getPermittedUsers("account", s.as[ObjectId]("_id")).filter(userId => currentUserPermUserIds.contains(userId)).size)
      s
    })
    accountTypeFilter(data.map(wrapDBObj(_)))
  }

  def accountTypeFilter(allObjects: Iterator[MongoDBObject]): Iterator[MongoDBObject] = {
    val roles = roleChecker.getUserAuthorities()
    val superUserParams = Set("_id", "name", /*"comment",*/ "plan", "status", "paymentWay", "balance", "cost", "objectsCount", "equipmentsCount", "usersCount")
    if (roles.contains("admin")) {
      allObjects.map(wrapDBObj(_));
    }
    else if (roles.contains("superuser")) {
      allObjects.map(_.filter(item => superUserParams.contains(item._1)))
    }
    else Iterator.empty
  }

  def getPermittedAccounts(request: ExtDirectStoreReadRequest) = {
    var allAccounts: Iterator[DBObject] = Iterator.empty
    val userName = roleChecker.getUserName()
    if (roleChecker.hasRequiredStringAuthorities("admin", "AccountView")) {
      allAccounts = dbaccounts.find(EDSMongoUtils.searchAndFilters(request) ++ notRemoved, savableFields.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1)).toIterator
    }
    else if (roleChecker.hasRequiredStringAuthorities("superuser", "AccountView")) {
      val permittedAccounts = roleChecker.getInferredPermissionRecordIds(userName, "account")
      allAccounts = dbaccounts.find(notRemoved ++ ("_id" $in permittedAccounts), savableFields.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1)).toIterator
    }
    allAccounts
  }


  def findById(recordId: ObjectId) = {
    debug("AccountsStoreServicse find record ID=" + recordId)


    val tariffIdToName: Map[String, String] = mdbm.getDatabase()("tariffs").find(MongoDBObject(), MongoDBObject("name" -> 1))
      .map(dbo => (dbo.as[ObjectId]("_id").toString, dbo.as[String]("name"))).toMap.withDefault(arg => arg)

    val p = dbaccounts.findOne(MongoDBObject("_id" -> recordId), savableFields.map(_ -> 1).toMap)

    def calcTotalCost(account: DBObject): Long = tariffPlans.calcTotalCost(account)

    val data = p.map(s =>
      s
        + ("plan" -> (tariffIdToName(s.as[String]("plan"))))
        + ("cost" -> calcTotalCost(s))
        + ("objectsCount" -> dbobjects.find(notRemoved ++ MongoDBObject("account" -> s("_id"))).count)
        + ("equipmentsCount" -> dbequipments.find(notRemoved ++ MongoDBObject("accountId" -> s("_id"))).count)
        + ("usersCount" -> roleChecker.getPermittedUsers("account", s.as[ObjectId]("_id")).size)
    )

    data
  }

  @ExtDirectMethod()
  def addToAccount(accid: String, objects: Seq[String]) = {

    val accObId: ObjectId = new ObjectId(accid)

    //    val accName: String = dbaccounts.findOneByID(accObId, MongoDBObject("name" -> 1)).get.as[String]("name")

    //    var objAgg: ObjectAggregate = null

    for (obj <- objects) {
      info("change objects account")
      val currentObj = dbobjects.findOne(MongoDBObject("_id" -> new ObjectId(obj)))
      val uid = currentObj.get("uid").asInstanceOf[String]
      val objacc = currentObj.get("account").asInstanceOf[ObjectId]
      val eqs = dbequipments.find(MongoDBObject("uid" -> uid)).toSeq

      val eqData = eqs.map(eq => {
        Map("_id" -> eq.get("_id").asInstanceOf[ObjectId])
      }
      ).toSeq
      //      Exception.catching(classOf[EventStreamNotFoundException]).opt(
      //      eventsStore.readEvents("ObjectAggregate", uid).hasNext).getOrElse(false)
      //      try {
      //        objAgg=objectsAggregatesRepository.load(uid)
      //      }
      //      catch {
      //        case e: Exception => {throw new RuntimeException("Агрегат не найден: " + Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage), e)}
      //      }
      val accountName = or.getAccountName(accObId)
      val objdata = Map("account" -> accObId, "accountName" -> accountName)

      try {
        commandGateway.sendAndWait(
          new ObjectDataSetCommand(uid, objdata, eqData), roleChecker.getUserAuthorities(), roleChecker.getUserName());
      } catch {
        case e: Exception => {
          throw new RuntimeException("При изменении записи произошла ошибка: " + Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage), e)
        }
      }

      info("change equipments account for uid=" + uid)
      //val eqs=dbequipments.find(MongoDBObject("uid" -> uid))
      for (eq <- eqs) {
        val oid = eq.get("_id").asInstanceOf[ObjectId]
        val eqacc = eq.get("accountId").asInstanceOf[ObjectId]
        //Аккаунт изменится если аккаунт владельца оборудования и аккаунт владельца объекта совпадают
        if (eqacc == objacc) {
          info("change eq account from" + eqacc)
          val eqdata = Map("accountId" -> accObId)
          try {
            commandGateway.sendAndWait(
              new EquipmentDataSetCommand(oid, eqdata), roleChecker.getUserAuthorities(), roleChecker.getUserName());
          } catch {
            case e: Exception => {
              throw new RuntimeException("При изменении записи произошла ошибка: " + Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage), e)
            }
          }
        }
        eq
      }
      //      dbequipments
      //        .update(MongoDBObject("uid" -> uid),
      //        $set("accountId" -> accObId), false, true, WriteConcern.Safe)

    }


  }

}

@ExtDirectService
class AccountsStoreServiceShort extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id", "name")
  val name = "AccountsDataShort"
  val idProperty = "_id"
  val lazyLoad = false


  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var permEd: PermissionsEditor = null

  val dbName = "accounts"

  def getCollection = mdbm.getDatabase().apply(dbName)

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {
    EDSMongoUtils.searchFieldFromQuery(request)
    val p = getCurrentUserPermittedAccounts(request)
    EDSJsonResponse(p)
  }

  def getCurrentUserPermittedAccounts(request: ExtDirectStoreReadRequest) = {
    var allAccounts: Iterator[DBObject] = Iterator.empty
    val userId = permEd.getUserIdByName(permEd.getUserName());
    val roles = permEd.getUserAuthorities()
    debug("userId=" + userId)
    if (roles.contains("admin") || roles.contains("Manager")) {
      allAccounts = getCollection.find(EDSMongoUtils.searchAndFilters(request) ++ notRemoved, model.fieldsNames.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1)).toIterator
    }
    else if (roles.contains("superuser")) {
      if (userId != null) {
        val permittedAccountIds = permEd.getPermissionsRecords(userId, "account").map(_.getAs[ObjectId]("item_id").get)
        allAccounts = getCollection.find("_id" $in permittedAccountIds ++ notRemoved, model.fieldsNames.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1)).toIterator
      }
    }
    allAccounts
  }

  def getPermittedAccounts(request: ExtDirectStoreReadRequest) = {
    var allAccounts: Iterator[DBObject] = Iterator.empty
    val userId = request.getParams.get("userId").asInstanceOf[String]
    val uid = if (userId != null && userId != "new") new ObjectId(userId) else null
    debug("userId=", userId, " uid=", uid)
    if (uid != null) {
      val permittedAccountIds = permEd.getPermissionsRecords(uid, "account").map(_.getAs[ObjectId]("item_id").get)
      allAccounts = getCollection.find(("_id" $in permittedAccountIds) ++ notRemoved, model.fieldsNames.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1)).toIterator
    }

    else if (userId != "new") allAccounts = getCollection.find(EDSMongoUtils.searchAndFilters(request) ++ notRemoved, model.fieldsNames.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1)).toIterator
    allAccounts
  }
}


import scala.collection.JavaConversions.mapAsJavaMap

@ExtDirectService
class AccountInfo extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  @ExtDirectMethod
  def getObjectsStat(accountId: String): java.util.Map[String, Int] = {

    val objects = mdbm.getDatabase()("objects")

    val found = objects.find(notRemoved ++ MongoDBObject("account" -> new ObjectId(accountId)) /*, MongoDBObject("equipment" -> 1)*/)
    debug("found " + found)
    val objectsCount: Int = found.count
    //val equipmentCount: Int = found.map(dbo => dbo.getAs[DBObject]("equipment").map(_.size).getOrElse(0)).sum

    Map("objectsCount" -> objectsCount /*, "equipmentCount" -> equipmentCount*/)
  }

  @ExtDirectMethod
  def getEquiupmentsStat(accountId: String): java.util.Map[String, Int] = {

    val equipments = mdbm.getDatabase()("equipments")

    val found = equipments.find(notRemoved ++ MongoDBObject("accountId" -> new ObjectId(accountId)) /*, MongoDBObject("equipment" -> 1)*/)
    val equipmentsCount: Int = found.count
    debug("accountId " + accountId + " found " + found)
    //val equipmentCount: Int = found.map(dbo => dbo.getAs[DBObject]("equipment").map(_.size).getOrElse(0)).sum

    Map("equipmentsCount" -> equipmentsCount /*, "equipmentCount" -> equipmentCount*/)
  }
}




