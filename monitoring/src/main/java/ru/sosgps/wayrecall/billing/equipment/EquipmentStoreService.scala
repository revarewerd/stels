/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.billing.equipment

import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import ru.sosgps.wayrecall.billing.account.AccountRepository
import ru.sosgps.wayrecall.core.{UserRolesChecker, SecureGateway, MongoDBManager, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.utils.web.extjsstore.{StoreAPI, EDSStoreServiceDescriptor}
import org.springframework.beans.factory.annotation.Autowired
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import org.axonframework.commandhandling.gateway.CommandGateway
import org.bson.types.ObjectId
import ch.ralscha.extdirectspring.bean.{ExtDirectResponseBuilder, SortDirection, ExtDirectStoreReadRequest}
import collection.mutable
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import collection.JavaConversions.{seqAsJavaList, mapAsJavaMap, mapAsScalaMap, collectionAsScalaIterable}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._

import scala.Predef._
import ru.sosgps.wayrecall.utils.{Memo, DBQueryUtils, ExtDirectService, ISODateTime}
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import java.util.Date
import ru.sosgps.wayrecall.billing.security.PermissionsEditor
import ru.sosgps.wayrecall.utils.DBQueryUtils._

@ExtDirectService
class EquipmentStoreService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id","objectid","objectName","accountId","accountName",
            //Вкладка Основные сведения
            "eqOwner","eqRightToUse","eqSellDate",//"installDate",
            /*"eqStatus",*/"eqWork","eqWorkDate","eqNote",
            //Вкладка Характеристики устройства
            "eqtype","eqMark","eqModel","eqSerNum",
            "eqIMEI","eqFirmware","eqLogin","eqPass",
            //Вкладка SIM-карта
            "simOwner","simProvider",/*"simContract","simTariff",
            "simSetDate",*/"simNumber","simICCID","simNote",
            //Вкладка SIM-карта
            "instPlace")
  val updatebleFields = Set("eqOwner","eqRightToUse","eqSellDate","eqWork","eqWorkDate","eqNote",
                            "eqtype","eqMark","eqModel","eqSerNum","eqIMEI","eqFirmware","eqLogin","eqPass",
                            "simOwner","simProvider","simSetDate","simNumber","simICCID","simNote","instPlace")
  val name = "EquipmentStoreService"
  val idProperty = "_id"

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var accRepo: AccountRepository = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var commandGateway: SecureGateway = null;

  val dbName = "equipments"

  def getCollection = mdbm.getDatabase().apply(dbName)


  @ExtDirectMethod(value = ExtDirectMethodType.STORE_MODIFY)
  @StoreAPI("destroy")
  def remove(maps: Seq[Map[String, Any]]) = {
    info("maps for removing" + maps)
    maps.foreach(map => {
      info("removing equipment" + map("_id").toString)
      try {
        commandGateway.sendAndWait(new EquipmentRemoveCommand(new ObjectId(map("_id").toString)),roleChecker.getUserAuthorities(),roleChecker.getUserName());
      }
      catch {
        case e : Exception => { throw new RuntimeException("При удалении записи произошла ошибка: " + Option(e.getCause()).map(_.getMessage()).getOrElse(e), e) }
      }
      map("_id").toString
    })
  }

  @ExtDirectMethod
  def delete(maps: Seq[Map[String, Any]]) = {
    info("maps for removing" + maps)
     maps.foreach(map => {
        info("removing equipment" + map("_id").toString)
        try {
        commandGateway.sendAndWait(new EquipmentDeleteCommand(new ObjectId(map("_id").toString)),roleChecker.getUserAuthorities(),roleChecker.getUserName());
        }
       catch {
         case e : Exception => { throw new RuntimeException("При удалении записи произошла ошибка: " + Option(e.getCause()).map(_.getMessage()).getOrElse(e), e) }
       }
       map("_id").toString
    })
//    maps.foreach(map => {
//      info("removing from " + dbName + " " + map("_id").toString)
//      val wc = getCollection.remove(
//        MongoDBObject("_id" -> new ObjectId(map("_id").toString)),
//        WriteConcern.Safe)
//    })
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def read(request: ExtDirectStoreReadRequest) = {
    debug("EquipmentService read " + request)
    EDSJsonResponse(getAllEquipments(request))
  }

  def loadAccountName(oid: ObjectId) = {
      Option(accRepo.getAccountById(oid))
      .flatMap(dbo => {
        if(!dbo.getAs[Boolean]("removed").getOrElse(false))
          dbo.getAs[String]("name")
        else
          dbo.getAs[String]("name").map(_ + " (в корзине)")
      }).getOrElse("Ошибка: аккаунт не найден")
  }


  def getAllEquipments(request: ExtDirectStoreReadRequest)={
    val getAccountName = Memo(loadAccountName)
    val allObjects = getPermittedEquipments(request)//getCollection.find(searchAndFilters(request))
      .map(dbo => try {
       dbo.put("objectName", dbo.getAs[String]("uid").map(uid => {
        val dbo = or.getObjectByUid(uid)
        val isRemoved = dbo.containsField("removed")
        val objectName = dbo.as[String]("name")
        if (isRemoved)
          objectName + " (в корзине)"
        else
          objectName
      }).getOrElse("не установлен"));

      dbo.put("accountName", dbo.getAs[ObjectId]("accountId").map(getAccountName).getOrElse(""))
      dbo
    }
    catch {
      case e: Throwable => throw new RuntimeException("error processing dbo:" + dbo, e)
    }
      ).toList.sortBy(_.as[String]("eqtype"))
    userTypeFilter(allObjects)
  }

  def userTypeFilter(allObjects:List[DBObject]) : List[MongoDBObject]  = {
    val roles=roleChecker.getUserAuthorities()
    val superUserParams = Set("_id","objectid","objectName","accountId","accountName",
      //Вкладка Основные сведения
      "eqOwner","eqRightToUse","eqSellDate","eqWork","eqWorkDate","eqNote",
      //Вкладка Характеристики устройства
      "eqtype","eqMark","eqModel","eqSerNum",
      "eqIMEI","eqFirmware",//"eqLogin","eqPass",
      //Вкладка SIM-карта
      //"simOwner","simProvider","simNumber","simICCID","simNote",
      //Место установки
      "instPlace")
    if(roles.contains("admin") || roles.contains("servicer")) {
      allObjects.map(wrapDBObj(_));
    }
    else if(roles.contains("superuser")){
     allObjects.map(_.filter(item => superUserParams.contains(item._1)))
    }
    else List.empty

  }

  def getPermittedEquipments(request: ExtDirectStoreReadRequest) = {
    var allObjects : Iterator[DBObject] = Iterator.empty
    val userName = roleChecker.getUserName()
    if(roleChecker.hasRequiredStringAuthorities("admin","EquipmentView")) {
      allObjects = getCollection.find(searchAndFilters(request) ++ notRemoved )
    }
    else if(roleChecker.hasRequiredStringAuthorities("superuser","EquipmentView") || roleChecker.hasRequiredStringAuthorities("servicer")) {
      val permittedAccounts=roleChecker.getInferredPermissionRecordIds(userName,"account")
      allObjects =  getCollection.find(notRemoved ++ ("accountId" $in permittedAccounts))
    }
    debug("allObjects="+allObjects.length)
    allObjects
  }

  def findById(recordId: ObjectId) = {
    debug("EquipmentService find record ID=" + recordId)
    val eqRec = getCollection.findOne(MongoDBObject("_id" -> recordId))
      .map(dbo => try {
      dbo.put("objectName", dbo.getAs[String]("uid").map(uid => {
        val dbo = or.getObjectByUid(uid)
        val isRemoved = DBQueryUtils.isRemoved(dbo)
        val objectName = dbo.as[String]("name")
        if (isRemoved)
          objectName + " (в корзине)"
        else
          objectName
      }).getOrElse("не установлен"));
     // dbo.put("accountName", dbo.getAs[ObjectId]("accountId").map(or.getAccountName).getOrElse("на складе"));
      dbo.put("accountName", dbo.getAs[ObjectId]("accountId").map(loadAccountName).getOrElse(""))
      dbo
    }
    catch {
      case e: Throwable => throw new RuntimeException("error processing dbo:" + dbo, e)
    }
      )
    eqRec
  }
}
