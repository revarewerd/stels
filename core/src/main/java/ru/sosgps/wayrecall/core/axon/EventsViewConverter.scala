package ru.sosgps.wayrecall.core.axon

import javax.annotation.PostConstruct

import com.mongodb.casbah.Imports._
import com.mongodb.{BasicDBObject, DBObject}
import org.axonframework.domain.DomainEventMessage
import org.axonframework.serializer.bson.DBObjectXStreamSerializer
import org.axonframework.serializer.{SerializedType, SimpleSerializedObject}
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{MongoDBManager, WayrecallAxonEvent}

import scala.collection.JavaConversions.{mapAsScalaMap, seqAsJavaList}
/**
  * Created by ivan on 27.01.16.
  */
class EventsViewConverter extends grizzled.slf4j.Logging  {
  val model = Set("aggregateId", "payloadType", "aggregateName", "aggregateType", "userName", "time", "eventData", "eventId")

  @Autowired
  var mdbm: MongoDBManager = null

  var dbEventsView: MongoCollection = null;
  @PostConstruct
  def init() :Unit = {
    dbEventsView = mdbm.getDatabase().apply("domainEventsView")
  }

  def getNameIfCreated(message: DomainEventMessage[_]) = {
    try {
      val payload = message.getPayload.asInstanceOf[WayrecallAxonEvent]
      payload.getInitialName
    }
    catch {
      case e: Exception => {
        warn(e.getMessage)
        None
      }
    }
  }

  def fromEventMessage(aggregateType: String, message: DomainEventMessage[_]): DBObject = {
    MongoDBObject(
      "aggregateId" -> message.getAggregateIdentifier.toString,
      "aggregateType" -> aggregateType,
      "payloadType" -> message.getPayloadType.getName,
      "aggregateName" -> getAggregateName(aggregateType,message.getAggregateIdentifier.toString)
                                  .orElse(getNameIfCreated(message)).getOrElse(""),
      "userName" -> getUserName(message),
      "time" -> message.getTimestamp.toString,
      "eventData" -> ensureValidFieldNames(getEventData(message)),
      "eventId" -> message.getIdentifier
    )
  }

  def fromDomainEventDocument(dbo: DBObject): DBObject = {
    val agId = dbo.as[String]("aggregateIdentifier")
    val agType = dbo.as[String]("type")
    val eventOpt = deserializeEvent(dbo)
    MongoDBObject(
      "aggregateId" -> agId,
      "aggregateType" -> agType,
      "payloadType" -> dbo.get("payloadType"),
      "aggregateName" -> getAggregateName(agType,agId).orElse(eventOpt.flatMap(_.getInitialName)).getOrElse(""),
      "userName" -> getUserName(dbo),
      "time" -> dbo.get("timeStamp"),
      "eventData" -> ensureValidFieldNames(eventOpt.map(_.toHRTable).getOrElse(java.util.Collections.emptyMap())),
      "eventId" -> dbo.get("eventIdentifier")
    )
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

  def getUserName(dbo: DBObject) = {
    val metadata = dbo.as[BasicDBObject]("serializedMetaData").as[MongoDBList]("meta-data")
    val parsedData = parseMetaData(metadata)
    parsedData.get("userName").getOrElse("")
  }

  val dummySerializedType = new SerializedType {
    def getName = ???

    def getRevision = ???
  }

  val serializer = new DBObjectXStreamSerializer()

  def deserializeEvent(dbo: DBObject) = {
    try {
      val payload = dbo.as[BasicDBObject]("serializedPayload")
      val deserialize = serializer.deserialize[DBObject, AnyRef](new SimpleSerializedObject[DBObject](payload, classOf[DBObject], dummySerializedType))
      Option(deserialize.asInstanceOf[WayrecallAxonEvent])
    }
    catch {
      case e: Exception => {
        warn(e.getMessage)
        None
      }
    }
  }

  def getEventData(message: DomainEventMessage[_]) = {
    message.getPayload.asInstanceOf[WayrecallAxonEvent].toHRTable
  }

  def ensureValidFieldNames(data: java.util.Map[String,AnyRef]) = {
    data.map{case(k,v) => (k.replace(".","\uff0e"),v)}
  }

  def getUserName(message: DomainEventMessage[_]) = {
    message.getMetaData.getOrDefault("userName", "").toString
  }

  def getByOid(collName: String, id: String) = {
    mdbm.getDatabase().apply(collName).findOne(MongoDBObject("_id" -> new ObjectId(id)))
  }

  def collection(agType: String) = aggregateTypesCollections(agType)
  def getByUid(uid:  String) = {
    mdbm.getDatabase().apply("objects").findOne(MongoDBObject("uid" -> uid))
  }


  def getAggregateName(agType: String, id: String): Option[String] = {
    try {
      if (AggregateType.hasName(agType)) {
        val nameOpt: Option[String] = agType match {
          case AggregateType.DealerAggregate => Option(id)
          case AggregateType.AccountAggregate |
               AggregateType.UserAggregate |
               AggregateType.TariffPlanAggregate => getByOid(collection(agType), id).flatMap(_.getAs[String]("name"))
          case AggregateType.ObjectAggregate => getByUid(id).flatMap(_.getAs[String]("name"))
          case AggregateType.EquipmentAggregate => {
            val eqOpt = getByOid(collection(agType), id)
            eqOpt.map(dbo => EventsViewConverter.eqNameFields.flatMap(dbo.getAs[String](_)).mkString(" "))
          }
          case AggregateType.EquipmentTypesAggregate => {
            val eqTypeOpt = getByOid(collection(agType), id)
            eqTypeOpt.map(dbo => EventsViewConverter.eqTypeNameFields.flatMap(dbo.getAs[String](_)).mkString(" "))
          }
          case AggregateType.PermissionAggregate => {
            val permOpt = getByOid(collection(agType), id);
            permOpt.flatMap(obj => {
              val userId = obj("userId").toString
              val itemId = obj("item_id").toString
              val recordType = obj.as[String]("recordType")
              val itemName = recordType match {
                case "object" => "Объект: " + getByOid("objects", itemId).flatMap(_.getAs[String]("name")).getOrElse("-")
                case "account" => "Аккаунт: " + getByOid("accounts", itemId).flatMap(_.getAs[String]("name")).getOrElse("-")
                case _ => "Error: unknown type"
              }
              val userName = getByOid("users", userId).flatMap(_.getAs[String]("name")).getOrElse("-")
              Option(s"Пользователь: $userName, $itemName")
            })
          }
          case _ => None
        }
//        nameOpt.getOrElse(
//          dbEventsView.find(MongoDBObject("aggregateType" -> agType, "aggregateId" -> id))
//            .sort(MongoDBObject("time" -> -1))
//            .find(d => true) // чет headOption нет
//            .flatMap(_.getAs[String]("aggregateName"))
//            .getOrElse("")
//        )
        nameOpt.orElse(
          dbEventsView.find(MongoDBObject("aggregateType" -> agType, "aggregateId" -> id))
            .sort(MongoDBObject("time" -> -1))
            .find(d => true) // чет headOption нет
            .flatMap(_.getAs[String]("aggregateName"))
        )
      }
      else None
    }
    catch {
      case e: Exception => Option(e.toString)
    }
  }

}

object EventsViewConverter {
  val eqNameFields = List("eqMark", "eqModel", "eqIMEI", "eqSerNum")
  val eqTypeNameFields = List("type", "mark", "model")
}
