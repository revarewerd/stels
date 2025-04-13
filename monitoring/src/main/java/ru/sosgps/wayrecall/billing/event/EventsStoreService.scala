package ru.sosgps.wayrecall.billing.event

import java.util.{Objects, Date}
import javax.annotation.PostConstruct

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import ch.ralscha.extdirectspring.bean.{SortDirection, ExtDirectStoreReadRequest}
import com.mongodb.DBObject
import com.mongodb.casbah.MongoCollection
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.web._
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._
import com.mongodb.casbah.Imports._
import collection.JavaConversions.{seqAsJavaList, mapAsJavaMap, mapAsScalaMap, collectionAsScalaIterable}
/**
  * Created by ivan on 11.02.16.
  */

private object AggregateType {
  val AccountAggregate = "AccountAggregate"
  val ObjectAggregate = "ObjectAggregate"
  val EquipmentAggregate = "EquipmentAggregate"
  val EquipmentTypesAggregate = "EquipmentTypesAggregate"
  val PermissionAggregate = "PermissionAggregate"
  val UserAggregate = "UserAggregate"
  val TariffPlanAggregate = "TariffPlanAggregate"
  val DealerAggregate = "DealerAggregate"
  val TicketAggregate = "TicketAggregate"
}

object EventsTranslate {


  val aggregateTypesTranslate = Map(
    AggregateType.AccountAggregate -> "Аккаунт",
    AggregateType.ObjectAggregate -> "Объект",
    AggregateType.EquipmentAggregate -> "Оборудование",
    AggregateType.EquipmentTypesAggregate -> "Тип оборудования",
    AggregateType.PermissionAggregate -> "Права пользователя",
    AggregateType.UserAggregate -> "Пользователь",
    AggregateType.TariffPlanAggregate -> "Тариф",
    AggregateType.DealerAggregate -> "Дилер",
    AggregateType.TicketAggregate -> "Заявка"
  )

   val eventTypesTranslate = Map(
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
    "ru.sosgps.wayrecall.billing.tariff.events.TariffPlanDeleteEvent" -> "Тариф удален",
    "ru.sosgps.wayrecall.billing.dealer.DealerBaseTariffChangeEvent" -> "Базовый тариф дилера изменен",
    "ru.sosgps.wayrecall.billing.dealer.DealerBalanceChangeEvent" -> "Баланс дилера изменен"
  )

  val aggregateTypesReverseTranslate = aggregateTypesTranslate.map(kv => (kv._2,kv._1))
  val eventTypesReverseTranslate = eventTypesTranslate.map(kv => (kv._2,kv._1))
}

@ExtDirectService
class EventsStoreService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  override val model: Model = Model("aggregateId","aggregateType" , "payloadType","aggregateName", "userName", "time","eventData", "eventDataPreview")
  override val lazyLoad: Boolean = true
  override val name: String = "EventsData"
  override val idProperty: String = "_id"

  @Autowired
  var mdbm: MongoDBManager = null

  var dbEventsView: MongoCollection = null


  @PostConstruct
  def init(): Unit = {
    dbEventsView = mdbm.getDatabase()("domainEventsView")
  }

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {
    debug("read " + request)
    setDefaultSorterIfEmpty(request,"time", SortDirection.DESCENDING)

    val query = makeQuery(request)
    val cursor = dbEventsView.find(query)
    val (total,eventsData) = cursor.totalCountAndApplyEDS(request)
    val data = eventsData.map(translate)


    debug(s"total = $total")
    EDSJsonResponse(total,data)
  }

  val fieldsWithTranslation = Array("aggregateType", "payloadType")
  val fieldsWithoutTranslation =  model.fieldsNames.filter(!fieldsWithTranslation.contains(_))

  def makePreview(obj: AnyRef, level: Int = 0) : String = {
    if(!obj.isInstanceOf[DBObject])
      Objects.toString(obj, "null")
    else {
      val dbo = obj.asInstanceOf[DBObject]
      if(level == 0)
        dbo.map{case(k,v) => k + ": " + makePreview(v, level + 1)}.mkString(",\n")
      else
        dbo.map{case(k,v) => k + ": " + makePreview(v, level + 1)}.mkString("{",",\n","}")
    }
  }
  def translate(dbo: DBObject) = {
    dbo.put("eventDataPreview", makePreview(dbo.as[DBObject]("eventData")))

    val agType = dbo.get("aggregateType").toString
    val payloadType = dbo.get("payloadType").toString
    val translatedFields =  Map(
      "aggregateType" -> EventsTranslate.aggregateTypesTranslate.getOrElse(agType,agType),
      "payloadType" -> EventsTranslate.eventTypesTranslate.getOrElse(payloadType,payloadType)
    )
    translatedFields ++ fieldsWithoutTranslation.map(f => (f, dbo.get(f)))
  }

  def makeQuery(request: ExtDirectStoreReadRequest) = {
    val params = request.getParams()

    val query = // Для окон
      if(!params.isEmpty && params.get("aggregateType") != null && params.get("aggregateId") != null){
        val aggregateType=params.get("aggregateType").asInstanceOf[String]
        val aggregateId=params.get("aggregateId").asInstanceOf[String]
        MongoDBObject("aggregateType"->aggregateType,"aggregateId"->aggregateId)
      }
      else MongoDBObject()

    val searchObject = {
      (for (
        searchfield: String <- request.getParams().toMap.get("searchfield").flatMap(Option(_)).map(_.toString);
        searchstring <- request.getParams().toMap.get("searchstring");
        if (searchstring != "")
      ) yield (searchfield -> containsPattern(searchstring.asInstanceOf[String]))).toList
    }.toMap

    if(searchObject.contains("aggregateType")){
      val strs = EventsTranslate.aggregateTypesTranslate.filter(kv => kv._2.toLowerCase.contains(request.getParams().toMap.get("searchstring").get.asInstanceOf[String].toLowerCase)).map(_._1 )
      debug("strs="+strs)
      val obj=("aggregateType" $in strs)
      query.putAll(obj)
    }
    else if(searchObject.contains("payloadType")){
      val strs = EventsTranslate.eventTypesTranslate.filter(kv => kv._2.toLowerCase.contains(request.getParams().toMap.get("searchstring").get.asInstanceOf[String].toLowerCase)).map(_._1 )
      debug("strs="+strs)
      val obj=("payloadType" $in strs)
      query.putAll(obj)
    }
    else query.putAll(collection.JavaConversions.mapAsJavaMap(searchObject))
    debug("searchObject="+searchObject)

    val dateFrom = Option(params.get("dateFrom")).map(x => parseDate(x.asInstanceOf[String])).getOrElse(new Date(111, 0, 1))
    val dateTo = Option(params.get("dateTo")).map(x => parseDate(x.asInstanceOf[String])).getOrElse(new Date())
    val period = ("time" $lte ISODateTime(dateTo) $gte ISODateTime(dateFrom))

    query.++=(period)
    debug("Final query = " + query)
    query
  }



}
