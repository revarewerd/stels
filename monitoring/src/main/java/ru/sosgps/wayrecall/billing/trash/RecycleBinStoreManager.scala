package ru.sosgps.wayrecall.billing.trash

import java.text.SimpleDateFormat
import java.util.Date
import javax.annotation.PostConstruct

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.{ExtDirectStoreReadRequest}
import com.mongodb.casbah.MongoCollection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import ru.sosgps.wayrecall.billing.AllObjectsService
import ru.sosgps.wayrecall.billing.`object`.ObjectsRepositoryWriter
import ru.sosgps.wayrecall.billing.account.AccountsStoreService
import ru.sosgps.wayrecall.billing.account.commands.AccountRestoreCommand
import ru.sosgps.wayrecall.billing.equipment.{EquipmentStoreService, EquipmentRestoreCommand}
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectRestoreCommand
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import ru.sosgps.wayrecall.utils.web.extjsstore.{StoreAPI, EDSStoreServiceDescriptor}
import scala.StringBuilder
import scala.collection.MapLike
import scala.collection.JavaConverters._
import scala.collection.mutable
import com.mongodb.casbah.Imports._



/**
 * Created by ilazarevsky on 6/29/15.
 */

@ExtDirectService
class RecycleBinStoreManager extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {


  override val model: Model = Model.apply("removalTime", "entityName", "accountName", "_id", "type", "payload", "payloadData")
  override val lazyLoad: Boolean = false
  override val name: String = "RecycleBinData"
  override val idProperty: String = "_id"


  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var commandGateway: SecureGateway = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  private var accountsStoreService: AccountsStoreService = null

  @Autowired
  private var equipmentStoreService: EquipmentStoreService = null

  @Autowired
  private var allObjectService: AllObjectsService = null

  var dbAccounts: MongoCollection = null
  var dbObjects: MongoCollection = null
  var dbEquipments: MongoCollection = null
  var dbUsers: MongoCollection = null

  @PostConstruct
  def init(): Unit = {
    val db = mdbm.getDatabase()
    dbAccounts = db("accounts")
    dbObjects = db("objects")
    dbEquipments = db("equipments")
    dbUsers = db("users")
  }

  object Entity {
    val Equipment = "Оборудование"
    val Object = "Объект"
    val Account = "Аккаунт"
  }


  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {
    if (!roleChecker.checkAdminAuthority())
      throw new AccessDeniedException("Недостаточно прав для доступа к корзине")


    // TODO сделать постраничным
    val queryResults =
      findRemoved(dbObjects).map(appendType(_, Entity.Object)).map(translate) ++
        findRemoved(dbAccounts).map(appendType(_, Entity.Account)).map(translate) ++
        findRemoved(dbEquipments).map(appendType(_,  Entity.Equipment)).map(translate)


    queryResults
  }



  //restrict using magic strings
  def restoreObject(uid: String): Unit = {
    try {
      commandGateway.sendAndWait(new ObjectRestoreCommand(uid), roleChecker.getUserAuthorities(), roleChecker.getUserName())
    }
    catch {
      case e: Exception => throw new RuntimeException("При восстановлении объекта произошла ошибка: " + Option(e.getCause).map(_.getMessage).getOrElse(e), e)
    }
  }

  def restoreEquipment(id: String): Unit = {
    val oid = new ObjectId(id)
    try {
      commandGateway.sendAndWait(new EquipmentRestoreCommand(oid), roleChecker.getUserAuthorities(), roleChecker.getUserName())
    }
    catch {
      case e: Exception => throw new RuntimeException("При восстановлении оборудования произошла ошибка: " + Option(e.getCause).map(_.getMessage).getOrElse(e), e)
    }
  }

  def restoreAccount(id: String): Unit = {
    val oid = new ObjectId(id)
    try {
      commandGateway.sendAndWait(new AccountRestoreCommand(oid), roleChecker.getUserAuthorities(), roleChecker.getUserName())
    }
    catch {
      case e: Exception => throw new RuntimeException("При восстановлении учетной записи произошла ошибка: " + Option(e.getCause).map(_.getMessage).getOrElse(e), e)
    }
  }


  @ExtDirectMethod
  def restore(maps: Seq[Map[String, Any]]): Unit = {
    maps.foreach(obj => {
      val _id = obj("_id").asInstanceOf[String]
      val typeOf = obj("type").asInstanceOf[String]
      debug("Restoring id = " + _id + " type = " + typeOf)
      typeOf match {
        case Entity.Object => restoreObject(_id)
        case Entity.Equipment => restoreEquipment(_id)
        case Entity.Account => restoreAccount(_id)
        case _ => throw new RuntimeException("При восстановление произошла ошибка: неизвестный тип объекта")
      }
    })
  }

  @ExtDirectMethod
  def delete(maps: Seq[Map[String, Any]]): Unit = {
    maps.foreach(obj => {
      val _id = obj("_id").asInstanceOf[String]
      val typeOf = obj("type").asInstanceOf[String]
      debug("Deleting id = " + _id + " type = " + typeOf)
      typeOf match {
        case Entity.Object => allObjectService.delete(Seq(or.getObjectByUid(_id).get("_id").toString)) // Стучусь в store из-за того что туда логики понапихали
        case Entity.Equipment => equipmentStoreService.delete(Seq(Map("_id" -> _id)))
        case Entity.Account => accountsStoreService.delete(Seq(_id))
        case _ => throw new RuntimeException("При удалении произошла ошибка: неизвестный тип объекта")
      }
    })
  }

  // implicit

  // Для атмосферы
  def loadObject(uid: String): DBObject = {
    dbObjects.findOne(MongoDBObject("uid" ->  uid)).map(appendType(_,Entity.Object)).map(translate).get
  }
  def loadAccount(id: ObjectId): DBObject = {
    dbAccounts.findOne(MongoDBObject("_id" ->  id)).map(appendType(_,Entity.Account)).map(translate).get
  }
  def loadEquipment(id: ObjectId): DBObject = {
    dbEquipments.findOne(MongoDBObject("_id" ->  id)).map(appendType(_,Entity.Equipment)).map(translate).get
  }

  private def findRemoved(coll: MongoCollection) = {
    val query = MongoDBObject("removed" -> true)
    coll.find(query).toIterable
  }


  private def appendType(mdoc: MongoDBObject, typeOf: String) = {
    mdoc.put("type", typeOf)
    mdoc
  }

  val translators = Map( Entity.Object -> WayrecallAggregateTranslate.ObjectTranslate,
    Entity.Equipment -> WayrecallAggregateTranslate.EquipmentTranslate,
    Entity.Account -> WayrecallAggregateTranslate.AccountTranslate)

//  private def getPayloadString(dbo: MongoDBObject, typeOf: String): String = {
//      val translate = translators(typeOf)
//      val sb = new StringBuilder()
//      val keysFilter = typeOf match {
//        case Entity.Object => objectKeys
//        case Entity.Equipment => equipmentKeys
//        case Entity.Account => accountKeys
//        case _ => throw new RuntimeException("При чтении произошла ошибка: неизвестный тип объекта")
//      }
//      if(typeOf ==  Entity.Equipment)
//        sb.append(translate("prevUid") + ":" + or.getObjectName(dbo.getAs[String]("prevUid").getOrElse("")) + "\n")
//      keysFilter.foreach(field => {
//        val value = dbo.get(field)
//        if(value.isDefined) {
//           sb.append(translate(field))
//           sb.append(": ")
//           sb.append(value.get)
//           sb.append("\n")
//        }
//      })
//      sb.toString()
//    }

  private def getPayloadString(dbo: MongoDBObject, typeOf: String): String = {
    val sb = new StringBuilder()
    getPayloadMap(dbo,typeOf).foreach{case (k,v) => {
      List(k, ": ", v, "\n").foreach(sb.append)
    }}
    sb.toString
  }


  private def getPayloadMap(dbo: DBObject, entityType: String) = {
    val translate = translators(entityType)
    val keysFilter = entityType match {
      case Entity.Object => objectKeys
      case Entity.Equipment => equipmentKeys
      case Entity.Account => accountKeys
      case _ => throw new RuntimeException("При чтении произошла ошибка: неизвестный тип объекта")
    }
    val payload =  mutable.LinkedHashMap.empty[String, AnyRef]
    if(entityType == Entity.Equipment)
      payload += ((translate("prevUid")) -> or.getObjectName(dbo.getAs[String]("prevUid").getOrElse("")))
    keysFilter.foreach(field => {
      val value = Option(dbo.get(field))
      value.foreach(v => payload += (translate(field) -> v))
    })
    payload
  }

  val objectKeys = List("uid", "customName", "comment", "marka", "model", "gosnumber", "VIN", "objnote", "equipmentType"
  ).filter(WayrecallAggregateTranslate.ObjectTranslate.contains)

  val equipmentKeys = List("_id", "eqtype", "eqIMEI", "eqFirmware", "simOwner", "simNumber", "simNote")
    .filter(WayrecallAggregateTranslate.EquipmentTranslate.contains)

  val accountKeys = List("_id", "accountType", "comment","cffam", "cfname", "cffathername", "cfemail", "cfworkphone1", "cfmobphone1", "cfmobphone2", "blockcause", "balance")
    .filter(WayrecallAggregateTranslate.AccountTranslate.contains)


  private def getAccountName(oid: ObjectId) = {
    dbAccounts.findOneByID(oid).flatMap(_.getAs[String]("name")).getOrElse("Аккаунт не найден")
  }

  private def getObjectName(uid: String) = {
    Option(or.getObjectByUid(uid)).flatMap(_.getAs[String]("name")).getOrElse("Объект не найден")
  }



  private def getAccountName(dbo: MongoDBObject, typeOf: String) = {
      val accountId = typeOf match {
        case Entity.Object => dbo.get("prevAccount").orElse(dbo.get("account")).map(_.toString)
        case Entity.Equipment => dbo.get("accountId").map(_.toString)
        case Entity.Account => dbo.get("_id").map(_.toString)
        case _ => None
      }
    accountId.map(new ObjectId(_))
      .flatMap(dbAccounts.findOneByID(_))
      .flatMap(dbo => {
        val removed = dbo.getAs[Boolean]("removed").getOrElse(false)
        val name = dbo.getAs[String]("name")
        if (!removed)
          name
        else
          name.map(_ + " ( удален )")
      })
      .getOrElse("")
  }


  val equipmentNameFields = List("eqMark", "eqModel", "eqIMEI", "eqSerNum")
  private def equipmentName(dbo: DBObject) = {
    equipmentNameFields.flatMap(dbo.getAs[String](_)).foldLeft("")(_ + " " + _)
  }

  private def translate(dbo: MongoDBObject): MongoDBObject = {
    val typeOf = dbo("type").asInstanceOf[String]
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    MongoDBObject(
      "_id" -> (if (typeOf ==  Entity.Object) dbo.get("uid") else dbo.get("_id")),
      "type" -> typeOf,
      "accountName" -> getAccountName(dbo,typeOf),
      "entityName" -> (if (typeOf == Entity.Equipment) equipmentName(dbo) else dbo.get("name")),
      "removalTime" -> format.format(dbo.as[Date]("removalTime")),
      "payload" -> getPayloadString(dbo, typeOf),
      "payloadData" -> getPayloadMap(dbo, typeOf)
    )
  }
}
