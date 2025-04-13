package ru.sosgps.wayrecall.workflow

import javax.annotation.PostConstruct

import com.mongodb.{BasicDBObject, DBCollection}
import com.mongodb.casbah.WriteConcern
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.ValidBSONType.BasicDBObject
import org.axonframework.eventhandling.annotation.EventHandler
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import ru.sosgps.wayrecall.billing.account.AccountsStoreService
import ru.sosgps.wayrecall.billing.equipment.{EquipmentObjectChangeCommand, EquipmentDataSetCommand}
import ru.sosgps.wayrecall.core.{UserRolesChecker, SecureGateway, MongoDBManager}
import scala.collection.JavaConverters._
import com.mongodb.casbah.Imports._
import  scala.collection.JavaConverters.collectionAsScalaIterableConverter
import  scala.collection.JavaConverters.mapAsScalaMapConverter

import scala.collection.Map

/**
 * Created by IVAN on 19.01.2015.
 */

@Order(-100)
class TicketEventsHandler  extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var commandGateway: SecureGateway = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @EventHandler
  def handleTicketCreatedEvent (event: TicketCreatedEvent) {
    debug("processing TicketCreatedEvent: " + event)
    val data=event.data.asJava
    val dBObject=MongoDBObject("_id"->event.id)
    dBObject.putAll(data)
    mdbm.getDatabase().apply("tickets").insert(dBObject,WriteConcern.Safe)
  }

  @EventHandler
  def handleTicketDataChanged(event: TicketDataChangedEvent) {
    debug("processing TicketDataChangedEvent: " + event)
    val changedAttributes = event.changedAttributes
    debug("changedAttributes: " + changedAttributes)
    mdbm.getDatabase().apply("tickets").update(MongoDBObject("_id" -> event.id), $set(changedAttributes.toSeq: _*), false, false, WriteConcern.Safe)
    val status = changedAttributes.getOrElse("status", "none").asInstanceOf[String]
    if (status == "close") {
      val ticketRec = mdbm.getDatabase().apply("tickets").findOne(MongoDBObject("_id" -> event.id))
      ticketRec.foreach(rec => {
        val works: Seq[BasicDBObject] = rec.get("works").asInstanceOf[java.util.List[BasicDBObject]].asScala
        works.foreach(item => {
          val uid = item.get("object").asInstanceOf[String]
          val workResult = item.get("workResult").asInstanceOf[BasicDBObject]
          processWork(uid, workResult)
        })
      })
    }

  }

  def processWork(uid: String, workResult: Map[String, Any]) = {
    debug("process work workResult=" + workResult)
    val remove = workResult.get("remove").map(_.asInstanceOf[BasicDBObject].asScala)
    remove.foreach(rem => {
      val data = rem.get("data").filter(null !=).map(_.asInstanceOf[java.util.Map[String, java.io.Serializable]].asScala)
      data.foreach(remdata => {
        debug("remove data " + remdata)
        val storeType = rem.get("storeType").filter(null !=).map(_.asInstanceOf[String])
        val accStoreId: ObjectId = storeType match {
          case Some("accStore") => {
            val objAccId = remdata.get("accountId").map(item => new ObjectId(item.asInstanceOf[String]))
            objAccId.getOrElse(getDefaultAccountId())
          }
          case Some("instStore") => {
            val userId = roleChecker.getUserIdByName(roleChecker.getUserName())
            val data = mdbm.getDatabase().apply("users").findOne(MongoDBObject("_id" -> userId))
            val mainAccId = data.flatMap(dbo => {
              dbo.getAs[ObjectId]("mainAccId")
            })
            mainAccId.getOrElse(getDefaultAccountId())
          }
          case Some("mainStore") => {
            getDefaultAccountId()
          }
          case _ => {
            getDefaultAccountId()
          }
        }
        debug("acc store ID to remove " + accStoreId)
        val eqId = remdata("_id").asInstanceOf[String]
        val eqDataSetCommand = new EquipmentDataSetCommand(new ObjectId(eqId), (remdata + ("accountId" -> accStoreId) - "_id").toMap)
        commandGateway.sendAndWait(eqDataSetCommand, roleChecker.getUserAuthorities, roleChecker.getUserName)
        val uid = remdata.get("uid").map(_.asInstanceOf[String])
        uid.foreach(objUID => {
          val eqObjChangeCommand = new EquipmentObjectChangeCommand(new ObjectId(eqId), Some(objUID), None)
          commandGateway.sendAndWait(eqObjChangeCommand, roleChecker.getUserAuthorities, roleChecker.getUserName)
          Unit
        })
      })
    })
    val install = workResult.get("install").map(_.asInstanceOf[BasicDBObject].asScala)
    install.foreach(inst => {
      val data = inst.get("data").filter(null !=).map(_.asInstanceOf[java.util.Map[String,java.io.Serializable]].asScala)
      data.foreach(instdata => {
        debug("install data " + instdata)
        val uid = inst.get("objectUID").filter(null !=).map(_.asInstanceOf[String]).get
        debug("object to install" + uid)
        val eqId = instdata("_id").asInstanceOf[String]
        val eqDataSetCommand = new EquipmentDataSetCommand(new ObjectId(eqId), (instdata /*+ ("accountId" -> "")*/ - "_id").toMap)
        commandGateway.sendAndWait(eqDataSetCommand, roleChecker.getUserAuthorities, roleChecker.getUserName)
        val eqObjChangeCommand = new EquipmentObjectChangeCommand(new ObjectId(eqId), None, Some(uid))
        commandGateway.sendAndWait(eqObjChangeCommand, roleChecker.getUserAuthorities, roleChecker.getUserName)
        Unit
      })
    })
  }

  def getDefaultAccountId(): ObjectId = {
    info("getDefaultAccountId")
    val defaultAcc = mdbm.getDatabase().apply("accounts").findOne($or("name" -> "Без Аккаунта", "default" -> true))
    defaultAcc.get.get("_id").asInstanceOf[ObjectId]
  }

  @EventHandler
  def handleTicketDeletedEvent(event: TicketDeletedEvent) {
    debug("processing TicketDeletedEvent: " + event)
    val id=event.id
    mdbm.getDatabase().apply("tickets").remove(MongoDBObject("_id"->id))
  }
}
