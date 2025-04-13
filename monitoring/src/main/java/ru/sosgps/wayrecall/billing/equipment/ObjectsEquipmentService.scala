/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.billing.equipment

import ru.sosgps.wayrecall.core.{UserRolesChecker, SecureGateway, MongoDBManager, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.utils.web.extjsstore.{StoreAPI, EDSStoreServiceDescriptor}
import org.springframework.beans.factory.annotation.Autowired
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import org.bson.types.ObjectId
import ch.ralscha.extdirectspring.bean.{ExtDirectResponseBuilder, SortDirection, ExtDirectStoreReadRequest}
import collection.mutable
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import collection.JavaConversions.{seqAsJavaList, mapAsJavaMap, mapAsScalaMap, collectionAsScalaIterable}
import com.mongodb.casbah.Imports._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._

import scala.Predef._
import ru.sosgps.wayrecall.utils.ExtDirectService
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.ISODateTime
import java.util.Date
import org.axonframework.commandhandling.gateway.CommandGateway
import ru.sosgps.wayrecall.billing.security.PermissionsEditor
import ru.sosgps.wayrecall.utils.DBQueryUtils._

// Склад (либо учетной записи, либо основной)
@ExtDirectService
class ObjectsEquipmentStoreService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id","uid",/*"objectid",*//*"objectName",*/"accountId",/*"accountName"*/            
            "eqOwner","eqRightToUse","eqSellDate",//"installDate",
            /*"eqStatus",*/"eqWork","eqWorkDate","eqNote",            
            "eqtype","eqMark","eqModel","eqSerNum",
            "eqIMEI","eqFirmware","eqLogin","eqPass",            
            "simOwner","simProvider",/*"simContract","simTariff",
            "simSetDate",*/"simNumber","simICCID","simNote",            
            "instPlace")
  val updatebleFields = Set("eqOwner","eqRightToUse","eqSellDate","eqWork","eqWorkDate","eqNote",
                            "eqtype","eqMark","eqModel","eqSerNum","eqIMEI","eqFirmware","eqLogin","eqPass",
                            "simOwner","simProvider","simNumber","simICCID","simNote","instPlace")
  val name = "ObjectsEquipmentStoreService"
  val idProperty = "_id"

  val lazyLoad = false

  @Autowired
  var roleChecker: UserRolesChecker = null
  
  @Autowired
  var mdbm: MongoDBManager = null
  
  @Autowired
  var or: ObjectsRepositoryReader = null

  var defaultAcc: MongoDBObject  = null
  val dbName = "equipments"
  
  def getCollection = mdbm.getDatabase().apply(dbName)
  
  @PostConstruct
  def init() {
   defaultAcc = mdbm.getDatabase().apply("accounts").findOne(
      $or("name" -> "Без Аккаунта","default" -> true), 
      MongoDBObject("_id" -> 1, "name" -> 1)).get
  }
  
  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def read (request: ExtDirectStoreReadRequest) = {
    debug("ObjectsEquipmentStoreService read " + request)

    val params = request.getParams.toMap
    val accountId = params.get("accountId").filter(null !=)
    if (roleChecker.checkAdminAuthority() || !accountId.isEmpty) {
    var accId = accountId.map(accstr => new ObjectId(accstr.toString)).getOrElse(defaultAcc("_id").asInstanceOf[ObjectId])
    debug("Account Id " + accId)
    val items = getCollection.find(
      notRemoved ++
      MongoDBObject("uid" -> null,
        "accountId" -> accId)
    )
      EDSJsonResponse(items)
    }
    else if(roleChecker.hasRequiredStringAuthorities("Installer")){
      val eqtype=params.get("eqtype").filter(item=> item!=null && item!="")
      val eqMark=params.get("eqMark").filter(item=> item!=null && item!="")
      val eqModel=params.get("eqModel").filter(item=> item!=null && item!="")
      val userId=roleChecker.getUserIdByName(roleChecker.getUserName())
      val mainAccId=mdbm.getDatabase().apply("users").findOne(MongoDBObject("_id"->userId)).map(dbo=>dbo.get("mainAccId"))
      if(mainAccId.isDefined) {
        debug("mainAccId="+mainAccId.get)
        var mdbo=MongoDBObject("uid" -> null,
          "accountId" -> mainAccId.get.asInstanceOf[ObjectId])
        if(!eqtype.isEmpty) mdbo.put("eqtype",eqtype)
        if(!eqMark.isEmpty) mdbo.put("eqMark",eqMark)
        if(!eqModel.isEmpty) mdbo.put("eqModel",eqModel)
        val items = getCollection.find(mdbo ++ notRemoved)
        EDSJsonResponse(items)
      }
    }
    else
      EDSJsonResponse(Map.empty)
  }
}

@ExtDirectService
class ObjectsEquipmentService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id","uid",/*"objectid",*//*"objectName",*/"accountId",/*"accountName"*/            
            "eqOwner","eqRightToUse","eqSellDate",//"installDate",
            /*"eqStatus",*/"eqWork","eqWorkDate","eqNote",            
            "eqtype","eqMark","eqModel","eqSerNum",
            "eqIMEI","eqFirmware","eqLogin","eqPass",            
            "simOwner","simProvider",/*"simContract","simTariff",
            "simSetDate",*/"simNumber","simICCID","simNote",            
            "instPlace")
  val updatebleFields = Set("eqOwner","eqRightToUse","eqSellDate","eqWork","eqWorkDate","eqNote",
                            "eqtype","eqMark","eqModel","eqSerNum","eqIMEI","eqFirmware","eqLogin","eqPass",
                            "simOwner","simProvider","simNumber","simICCID","simNote","instPlace")
  val name = "ObjectsEquipmentService"
  val idProperty = "_id"

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null
  
  @Autowired
  var or: ObjectsRepositoryReader = null
  
  @Autowired
  var commandGateway: SecureGateway = null;

  @Autowired
  var roleChecker: UserRolesChecker = null

  var defaultAcc: MongoDBObject  = null
  val dbName = "equipments"
  
  def getCollection = mdbm.getDatabase().apply(dbName)
  
  @PostConstruct
  def init() {
   //TODO: Сделать через агрегат и вынести в общий класс
   defaultAcc = mdbm.getDatabase().apply("accounts").findOne(
      $or("name" -> "Без Аккаунта","default" -> true), 
      MongoDBObject("_id" -> 1, "name" -> 1)).get
  }
  
  @ExtDirectMethod(value = ExtDirectMethodType.STORE_MODIFY)
  @StoreAPI("destroy")
  def remove(maps: Seq[Map[String, Any]]) = {
    maps.foreach(map => {
      info("removing from " + dbName + " " + map("_id").toString)
      val objdata = Map("uid" -> null)  
      try {
        commandGateway.sendAndWait(new EquipmentDataSetCommand(new ObjectId(map("_id").toString),objdata),roleChecker.getUserAuthorities(),roleChecker.getUserName);
        }
       catch {
         case e : Exception => { throw new RuntimeException("При удалении записи произошла ошибка: " + Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage), e) }
       }
//      val wc = getCollection.update(
//        MongoDBObject("_id" -> new ObjectId(map("_id").toString)),
//        MongoDBObject("uid" -> null))
    })
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def read (request: ExtDirectStoreReadRequest) = {
    debug("ObjectsEquipmentService read " + request)
    
    val params = request.getParams.toMap
    val uid = params.get("uid") 
    debug("uid " + uid)
    val items = getCollection.find(
        MongoDBObject("uid" -> uid
                      ) ++ notRemoved
      ).toList
      EDSJsonResponse(userTypeFilter(items))
    }

  def userTypeFilter(allObjects:List[DBObject])  = {
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
    if(roles.contains("admin")||roles.contains("servicer")) {
      allObjects;
    }
    else if(roles.contains("superuser")){
      allObjects.map(_.filter(item => superUserParams.contains(item._1)))
    }
    else List.empty

  }

  @ExtDirectMethod
  def loadEquipmentByUid(uid: String) = {
    debug("ObjectsEquipmentService read for uid=" + uid)
    val items = getCollection.find(
      MongoDBObject("uid" -> uid
      )
    ).toList
    userTypeFilter(items)
  }
  }

