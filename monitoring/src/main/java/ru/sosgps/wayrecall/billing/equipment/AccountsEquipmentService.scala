package ru.sosgps.wayrecall.billing.equipment

import ru.sosgps.wayrecall.billing.account.AccountsStoreService
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
import org.axonframework.commandhandling.gateway.CommandGateway
//import ru.sosgps.wayrecall.billing.equipment.EquipmentDataSetCommand

import scala.Predef._
import ru.sosgps.wayrecall.utils.ExtDirectService
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.ISODateTime
import java.util.Date
import scala.beans.BeanProperty
import ru.sosgps.wayrecall.billing.security.PermissionsEditor
import ru.sosgps.wayrecall.utils.DBQueryUtils._

@ExtDirectService
class AccountsEquipmentService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id","uid","objectid","objectName","accountId",/*"accountName"*/            
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
  val name = "AccountsEquipmentService"
  val idProperty = "_id"

  val lazyLoad = false
  
  @BeanProperty
  var accountsStoreService : AccountsStoreService = null

  @Autowired
  var mdbm: MongoDBManager = null
  
  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var roleChecker: UserRolesChecker = null
  
   @Autowired
  var commandGateway: SecureGateway = null;

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
  @ExtDirectMethod
  def modify(accountId:ObjectId,updatemaps: Seq[Map[String, Any]],removemaps: Seq[Map[String, Any]]){    
    update(accountId,updatemaps)
    remove(accountId,removemaps)    
  }
    
  @ExtDirectMethod
  def update(accountId:ObjectId,maps: Seq[Map[String, Any]]){
    maps.foreach(map => {
      val oid =new ObjectId(map("_id").asInstanceOf[String])
      info("updating from " + dbName + " " + oid)
      val eqdata = Map("accountId" -> accountId)        
      try {
          info("commandGateway " + commandGateway)
          commandGateway.sendAndWait(
                  new EquipmentDataSetCommand(oid, eqdata),roleChecker.getUserAuthorities(),roleChecker.getUserName());
      } catch {
         case e : Exception => { throw new RuntimeException("При изменении записи произошла ошибка: " + Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage), e) }
       }
//      val wc = getCollection.update(
//        MongoDBObject("_id" -> new ObjectId(map("_id").toString)),
//        $set("accountId" -> accountId))
      oid
  })
  }
  
//  @ExtDirectMethod(value = ExtDirectMethodType.STORE_MODIFY)
//  @StoreAPI("destroy")
  @ExtDirectMethod
  def remove(accountId:ObjectId,maps: Seq[Map[String, Any]]) = {
    maps.foreach(map => {
      val oid =new ObjectId(map("_id").asInstanceOf[String])  
      info("removing from " + dbName + " " + oid)
      val eqdata = Map("accountId" -> defaultAcc("_id").asInstanceOf[ObjectId]/*,"uid" -> null*/)        
      try {
          commandGateway.sendAndWait(
                  new EquipmentDataSetCommand(oid, eqdata),roleChecker.getUserAuthorities(),roleChecker.getUserName());
      } catch {
         case e : Exception => { throw new RuntimeException("При удалении записи произошла ошибка: " + Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage), e) }
       }
//      val wc = getCollection.update(
//        MongoDBObject("_id" -> new ObjectId(map("_id").toString)),
//        $set("accountId" -> defaultAcc.get("_id"),"uid" -> null))
        oid
    })
  //eqhistory
  //у объекта не скидывается абонентка
  //надо sumCostForObjectCache.invalidate()
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def read (request: ExtDirectStoreReadRequest) = {
    debug("EquipmentService read " + request)
    
    val params = request.getParams
    val accountId = params.get("accountId").asInstanceOf[String]  
    val items = getCollection.find(
    MongoDBObject("accountId" -> new ObjectId(accountId)) ++ notRemoved )
    .map(dbo => {
        dbo.put("objectName", dbo.getAs[String]("uid").map(uid => {              
              or.getObjectName(uid)
            }).getOrElse("не установлен"));
        dbo} 
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
    if(roles.contains("admin")) {
      allObjects;
    }
    else if(roles.contains("superuser")){
      allObjects.map(_.filter(item => superUserParams.contains(item._1)))
    }
    else List.empty

  }
}



