package ru.sosgps.wayrecall.billing.event


import java.nio.file.AccessDeniedException
import javax.servlet.http.HttpSession

import ru.sosgps.wayrecall.core.{UserRolesChecker, WayrecallAxonEvent, MongoDBManager}
import ru.sosgps.wayrecall.utils.web._
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.Memo
import ru.sosgps.wayrecall.utils.containsPattern
import ru.sosgps.wayrecall.utils.parseDate
import ru.sosgps.wayrecall.utils.ISODateTime
import com.mongodb.{DBObject, BasicDBObject}
import com.mongodb.casbah.{Imports, MongoCollection}
import org.axonframework.eventstore.EventStore
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import javax.annotation.PostConstruct
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._
import ch.ralscha.extdirectspring.bean._
import collection.JavaConversions.{seqAsJavaList, mapAsJavaMap, mapAsScalaMap, collectionAsScalaIterable}
import java.util.Date
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver
import org.axonframework.commandhandling.gateway._
import org.axonframework.domain.{EventMessage, MetaData}
import org.axonframework.serializer.bson.DBObjectXStreamSerializer
import org.axonframework.serializer.{SimpleSerializedObject, SerializedType}
import scala.collection
import scala.collection.mutable
import scala.ref.{SoftReference, ReferenceWrapper}

@ExtDirectService
class EventsStoreManager  extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  // "accountName"
 // val model = Model("_id", "entityName", "aggregateIdentifier","payloadType","timeStamp","type","userName","payload", "payloadData")
  override val model: Model = Model("aggregateId","aggregateType" , "payloadType","aggregateName", "userName", "time","eventData", "eventDataPreview")
  val name = "EventsDataTemp"
  val idProperty = "_id"
  val lazyLoad = true
  /*
  TODO
  1. Имя сущности для различных типов
  2. Учетная запись для объектов, оборудования, аккаунтов
  3. Окно для данных, со стороны store - таблица
  4. Не забыть, что здесь используется удаленная сортировка и возможно поиск
  5. Перелопатить toHRSTring, вернее добавить метод что-то вроде toHRTable
   */

  @Autowired
  var mdbm: MongoDBManager = null

  val serializer = new DBObjectXStreamSerializer()

  val dummySerializedType = new SerializedType {
    def getName = ???

    def getRevision = ???
  }
  var dbevents: MongoCollection = null
  var dbobjects: MongoCollection = null
  var dbusers: MongoCollection = null
  var dbaccounts: MongoCollection = null
  var dbequipments: MongoCollection = null
  var dbpermissions: MongoCollection = null
  var dbtariffs: MongoCollection = null
  @Autowired
  var eventsStore: EventStore = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @PostConstruct
  def init() {
    dbevents = mdbm.getDatabase().apply("domainEvents")
    dbusers = mdbm.getDatabase().apply("users")
    dbaccounts = mdbm.getDatabase().apply("accounts")
    dbobjects = mdbm.getDatabase().apply("objects")
    dbequipments = mdbm.getDatabase().apply("equipments")
    dbtariffs = mdbm.getDatabase().apply("tariffs")
    dbpermissions =  mdbm.getDatabase().apply("usersPermissions")
  }

  private object AggregateType {
    val AccountAggregate = "AccountAggregate"
    val ObjectAggregate = "ObjectAggregate"
    val EquipmentAggregate = "EquipmentAggregate"
    val EquipmentTypesAggregate = "EquipmentTypesAggregate"
    val PermissionAggregate = "PermissionAggregate"
    val UserAggregate = "UserAggregate"
    val TariffPlanAggregate = "TariffPlanAggregate"
  }

  private val aggregateTypesTranslate = Map(
    "AccountAggregate" -> "Аккаунт",
    "ObjectAggregate" -> "Объект",
    "EquipmentAggregate" -> "Оборудование",
    "EquipmentTypesAggregate" -> "Тип оборудования",
    "PermissionAggregate" -> "Права пользователя",
    "UserAggregate" -> "Пользователь",
    "TariffPlanAggregate" -> "Тариф"
  )
  private val eventTypesTranslate = Map(
    "ru.sosgps.wayrecall.billing.account.events.AccountCreateEvent" -> "Аккаунт создан",
    "ru.sosgps.wayrecall.billing.account.events.AccountDataSetEvent" -> "Аккаунт изменен",
    "ru.sosgps.wayrecall.billing.account.events.AccountDeleteEvent" -> "Аккаунт удален",
    "ru.sosgps.wayrecall.billing.account.events.AccountRemoveEvent" -> "Аккаунт перемещен в корзину",
    "ru.sosgps.wayrecall.billing.account.events.AccountRestoreEvent" -> "Аккаунт восстановлен",
    "ru.sosgps.wayrecall.billing.account.events.AccountStatusChanged" -> "Статус Аккаунта изменен",
    "ru.sosgps.wayrecall.billing.account.events.AccountBalanceChanged" -> "Баланс Аккаунта изменен",
    "ru.sosgps.wayrecall.core.ObjectCreatedEvent" -> "Объект создан",
    "ru.sosgps.wayrecall.core.ObjectDataChangedEvent" -> "Объект изменен",
    "ru.sosgps.wayrecall.core.ObjectDeletedEvent" -> "Объект удален",
    "ru.sosgps.wayrecall.core.ObjectRemovedEvent" -> "Объект перемещён в корзину",
    "ru.sosgps.wayrecall.core.ObjectRestoredEvent" -> "Объект восстановлен",
    "ru.sosgps.wayrecall.core.ObjectEquipmentChangedEvent"-> "Оборудование объекта изменено",
    "ru.sosgps.wayrecall.core.ObjectEnabledChanged"-> "Оборудование объекта изменено",
    "ru.sosgps.wayrecall.core.ObjectAccountChangedEvent"-> "Аккаунт объекта изменен",
    "ru.sosgps.wayrecall.core.EquipmentCreatedEvent" -> "Оборудование создано",
    "ru.sosgps.wayrecall.core.EquipmentDataChangedEvent" -> "Оборудование изменено",
    "ru.sosgps.wayrecall.core.EquipmentDeletedEvent" -> "Оборудование удалено",
    "ru.sosgps.wayrecall.core.EquipmentRemovedEvent" -> "Оборудование перемещено в корзину",
    "ru.sosgps.wayrecall.core.EquipmentRestoredEvent" -> "Оборудование восстановлено",
    "ru.sosgps.wayrecall.core.EquipmentObjectChangedEvent" -> "Объект оборудования изменен",
    "ru.sosgps.wayrecall.core.EquipmentAccountChangedEvent" -> "Аккаунт оборудования изменен",
    "ru.sosgps.wayrecall.billing.equipment.types.events.EquipmentTypesCreateEvent" -> "Тип оборудования создан",
    "ru.sosgps.wayrecall.billing.equipment.types.events.EquipmentTypesDataSetEvent" -> "Тип оборудования изменен",
    "ru.sosgps.wayrecall.billing.equipment.types.events.EquipmentTypesDeleteEvent" -> "Тип оборудования удален",
    "ru.sosgps.wayrecall.billing.user.permission.events.PermissionCreateEvent" -> "Права пользователя созданы",
    "ru.sosgps.wayrecall.billing.user.permission.events.PermissionDataSetEvent" -> "Права пользователя изменены",
    "ru.sosgps.wayrecall.billing.user.permission.events.PermissionDeleteEvent" -> "Права пользователя удалены",
    "ru.sosgps.wayrecall.billing.user.events.UserCreateEvent" -> "Пользователь создан",
    "ru.sosgps.wayrecall.billing.user.events.UserDataSetEvent" -> "Пользователь изменен",
    "ru.sosgps.wayrecall.billing.user.events.UserDeleteEvent" -> "Пользователь удален",
    "ru.sosgps.wayrecall.billing.tariff.events.TariffPlanCreateEvent" -> "Тариф создан",
    "ru.sosgps.wayrecall.billing.tariff.events.TariffPlanDataSetEvent" -> "Тариф изменен",
    "ru.sosgps.wayrecall.billing.tariff.events.TariffPlanDeleteEvent" -> "Тариф удален"
  )

  private val aggregateTypes=aggregateTypesTranslate.map(kv => (kv._2,kv._1))
  private val eventTypes=eventTypesTranslate.map(kv => (kv._2,kv._1))


  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {
    if(!roleChecker.checkAdminAuthority()) throw new AccessDeniedException("Недостаточно прав для доступа к панели событий")
    debug("EventsStoreManager read request = " + request)


    setDefaultSorterIfEmpty(request,"timeStamp", SortDirection.DESCENDING)
    val entityName = Memo((getEntityName _).tupled)
    def toEventModel(dbo: DBObject) = {

      val entityType = dbo.as[String]("type")
      val payloadType = dbo.as[String]("payloadType")
      Map(
        "aggregateId" -> dbo("aggregateIdentifier"),
        "aggregateName" -> entityName((dbo.as[String]("type"), dbo("aggregateIdentifier").toString)),
        "aggregateType" -> aggregateTypesTranslate.getOrElse(entityType, entityType),
        "time" -> dbo("timeStamp"),
        "payloadType" -> eventTypesTranslate.getOrElse(payloadType,payloadType),
        "eventDataPreview" -> parseData(dbo.as[BasicDBObject]("serializedPayload")),
        "eventData" -> parsePayloadData(dbo.as[BasicDBObject]("serializedPayload")),
        "userName" -> getUserName(parseMetaData(dbo.as[BasicDBObject]("serializedMetaData").as[MongoDBList]("meta-data"))))

      /*dbo.as[BasicDBObject]("serializedMetaData").as[MongoDBList]("meta-data").find(a => a.asInstanceOf[BasicDBObject].keySet().contains("entry")).orNull*/
    }


      val cursor = getCursor(request)
      val (total,eventsData) = cursor.totalCountAndApplyEDS(request)
      val data = eventsData.map(toEventModel)
      debug(s"total = $total")
      EDSJsonResponse(total,data)
    }





  def getCursor(request: ExtDirectStoreReadRequest) = {
    val params = request.getParams()
    val query = // Для окон
      if(!params.isEmpty && params.get("aggregateType") != null && params.get("aggregateId") != null){
        val aggregateType=params.get("aggregateType").asInstanceOf[String]
        val aggregateId=params.get("aggregateId").asInstanceOf[String]
        MongoDBObject("type"->aggregateType,"aggregateIdentifier"->aggregateId)
      }
      else MongoDBObject()

    dbevents.find(query)
  }


  private val aggregateTypesCollections = Map(
    "AccountAggregate" -> "accounts",
    "ObjectAggregate" -> "objects",
    "EquipmentAggregate" -> "equipments",
    "EquipmentTypesAggregate" -> "equipmentTypes",
    "PermissionAggregate" -> "usersPermissions",
    "UserAggregate" -> "users",
    "TariffPlanAggregate" -> "tariffs"
  )

  def getEntityName(aggregateType: String, id: String): String = try {
    if(aggregateTypesCollections.contains(aggregateType)) {
      val query = if (aggregateType == AggregateType.ObjectAggregate)
        MongoDBObject("uid" -> id)
      else
        MongoDBObject("_id" -> new ObjectId(id))
      val dbo = aggregateTypesCollections.get(aggregateType).flatMap(mdbm.getDatabase()(_).findOne(query))
      dbo.flatMap(obj => {
        aggregateType match {
          case AggregateType.AccountAggregate |
               AggregateType.ObjectAggregate |
               AggregateType.UserAggregate |
               AggregateType.TariffPlanAggregate => obj.getAs[String]("name")
          case AggregateType.PermissionAggregate => {
            val userId = obj.as[ObjectId]("userId")
            val itemId = obj.as[ObjectId]("item_id")
            val recordType = obj.as[String]("recordType")
            val itemName = recordType match {
              case "object" => "Объект: " + dbobjects.findOne(MongoDBObject("_id" -> itemId)).flatMap(_.getAs[String]("name")).getOrElse("имя не найдено")
              case "account" => "Аккаунт: " + dbaccounts.findOne(MongoDBObject("_id" -> itemId)).flatMap(_.getAs[String]("name")).getOrElse("имя не найдено")
              case _ => "Error: unknown type"
            }
            val userName = dbusers.findOne(MongoDBObject("_id" -> userId)).flatMap(_.getAs[String]("name")).getOrElse("Error: not found")
            Option(s"Пользователь: $userName, $itemName")
          }
          case AggregateType.EquipmentAggregate => {
            List("eqMark", "eqModel", "eqIMEI").flatMap(obj.getAs[String](_)).reduceOption(_ + " " + _)
          }
          case AggregateType.EquipmentTypesAggregate => {
            List("type", "mark", "model").flatMap(obj.getAs[String](_)).reduceOption(_ + " " + _)
          }
        }
      }).getOrElse("-")
    }
    else
      "-"
  } catch {
    case e:Exception => e.toString
  }

  def getUserName(parsedData: Map[AnyRef,AnyRef]) = {
    parsedData.get("userName").orNull
  }
  def getUserRole(parsedData: Map[AnyRef,AnyRef]) = {
    parsedData.get("userRole").orNull
  }
  def parseMetaData (metaData: MongoDBList) = {
    val data=metaData.map(a =>
      a.asInstanceOf[BasicDBObject].get("entry").asInstanceOf[BasicDBList].map(b =>
        b.asInstanceOf[BasicDBObject].get("string")
      )
    )
    data.map(c=>
      (c.get(0)->c.get(1))
    ).toMap
  }

  def parseData(rawData: DBObject) = try {
    val deserialize = serializer.deserialize[DBObject, AnyRef](new SimpleSerializedObject[DBObject](rawData, classOf[DBObject], dummySerializedType))
    //    val data=rawData.entrySet().map(a=>a.getValue.asInstanceOf[BasicDBList]).head.find{case e: BasicDBObject => e.containsField("data")}
    //      .get.asInstanceOf[BasicDBObject].get("data").asInstanceOf[BasicDBList].find{case e: BasicDBObject => e.containsField("scala/collection/immutable/HashMap$SerializationProxy")}
    //      .get.asInstanceOf[BasicDBObject].get("scala/collection/immutable/HashMap$SerializationProxy").asInstanceOf[BasicDBList].map(b =>
    //      b.asInstanceOf[BasicDBObject]
    //      )
    val data= deserialize.asInstanceOf[WayrecallAxonEvent].toHRString
    //debug("data "+data)
    data
  } catch {
    case e: Exception => {
      warn("Error parsing:" + rawData, e)
      e.toString
    }
  }


  def parsePayloadData(rawData: DBObject) = try {
    val deserialize = serializer.deserialize[DBObject, AnyRef](new SimpleSerializedObject[DBObject](rawData, classOf[DBObject], dummySerializedType))
    val data= deserialize.asInstanceOf[WayrecallAxonEvent].toHRTable
    data
  }
  catch {
    case  e: Exception => {
      val data = new java.util.HashMap[String,AnyRef]()
      data.put("Ошибка: ", e.toString)
      data
    }

  }

}
