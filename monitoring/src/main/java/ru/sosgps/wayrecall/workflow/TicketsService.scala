package ru.sosgps.wayrecall.workflow

import java.io.Serializable
import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.joda.time.format.ISODateTimeFormat
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.ObjectStoreManager
import ru.sosgps.wayrecall.billing.account.AccountsStoreServiceShort
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, MongoDBManager, UserRolesChecker, SecureGateway}
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.{EDSMongoUtils, EDSJsonResponse}
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.ISODateTime
import  scala.collection.JavaConverters.collectionAsScalaIterableConverter
import  scala.collection.JavaConverters.mapAsScalaMapConverter

import scala.collection._
/**
 * Created by IVAN on 18.12.2014.
 */

@ExtDirectService
class TicketsService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id", "accountId","accountName","status", "assignee","creator","creatorName","openDate","closeDate","works","worksCount","userAssign")

  val updatebleFields = Set("accountId", "status", "assignee",/*"creator","openDate",*/"closeDate","works")

  val name = "TicketsService"
  val idProperty = "_id"

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var commandGateway: SecureGateway = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def loadAllTickets(request: ExtDirectStoreReadRequest) = {
    val status=request.getParams.get("status")
    val assign=request.getParams.get("assign").asInstanceOf[String]
    val collection=mdbm.getDatabase().apply("tickets")
    val rawData=collection.find(EDSMongoUtils.searchAndFilters(request) ++ ("status" -> status),model.fieldsNames.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1))
    val data = rawData.map( dbo => {
      dbo.getAs[Date]("openDate").foreach(date =>  dbo.put("openDate",ISODateTime(date)))
      dbo.getAs[Date]("closeDate").foreach(date => dbo.put("closeDate",ISODateTime(date)))
      dbo.getAs[String]("accountId").foreach(accId => dbo.put("accountName",or.getAccountName(new ObjectId(accId))))
      dbo.getAs[ObjectId]("creator").foreach(creator => dbo.put("creatorName",getUserName(creator)))
      dbo.getAs[ObjectId]("assignee").foreach(assignee=> dbo.put("userAssign",getUserName(assignee)))
      dbo.getAs[MongoDBList]("works").foreach(works => dbo.put("worksCount",works.length))
      dbo
    })
    EDSJsonResponse(assignFilter(assign,data).toList)
  }

  def assignFilter(assign: String, collection: Iterator[DBObject]) ={
    val filteredCollection = assign match {
      case "my" => collection.filter(dbo=>dbo.get("userAssign")==roleChecker.getUserName())
      case "notmy" =>collection.filter(dbo=>dbo.get("userAssign")!=roleChecker.getUserName())
      case "notassign" =>collection.filter(dbo=> dbo.get("userAssign")==null)
      case "any" =>collection
      case _ => collection
    }
    filteredCollection
  }

  //@ExtDirectMethod
  def createTicket(data: Map[String,Serializable]) = {
    debug("createTicket data="+data)
    val fullData = data ++ Map[String,java.io.Serializable](
      "openDate"->new Date(),
      "status"->"open",
      "creator"->roleChecker.getUserIdByName(roleChecker.getUserName())
    )
    val oid= new ObjectId();
    debug("createTicket id="+oid)
    debug("createTicket fullData="+fullData)
    val command = new TicketCreateCommand(oid, fullData.toMap)
    commandGateway.sendAndWait(command, roleChecker.getUserAuthorities, roleChecker.getUserName)
    oid
  }

  @ExtDirectMethod
  def updateTicket(data: Map[String,Serializable]) = {
    debug("updateTicket data="+data)
    val id = data.get("_id")
    var oid : ObjectId= null
    if(id.isEmpty)    {
      oid=createTicket(data)
    }
    else {
      val assignee = data.getOrElse("assignee","nobody")
      var userAssign: ObjectId = null
      var updatableData=data.filter(item => updatebleFields.contains(item._1))
      val fullData=
        assignee match {
        case "current" => {
          userAssign=roleChecker.getUserIdByName(roleChecker.getUserName())
          updatableData + ("assignee"->userAssign)
        }
        case "nobody" => {
          updatableData + ("assignee"->null)
        }
        case userId: AnyRef =>
          updatableData + ("assignee"->userId)
        case _ => updatableData + ("assignee"->null)
      }
      debug("fullData="+fullData)
      oid=new ObjectId(id.get.asInstanceOf[String])
      val command=new TicketDataSetCommand(oid, fullData.toMap)
      commandGateway.sendAndWait(command, roleChecker.getUserAuthorities, roleChecker.getUserName)
    }
    oid
  }

  @ExtDirectMethod
  def removeTicket(ticketId: String) = {
    debug("removeTicket")
    val oid=new ObjectId(ticketId);
    val command = new TicketDeleteCommand(oid)
    commandGateway.sendAndWait(command, roleChecker.getUserAuthorities, roleChecker.getUserName)
    oid
  }

  private def getUserName(userId: ObjectId): String = {
    val userWithName= mdbm.getDatabase.apply("users").findOne(MongoDBObject("_id" -> userId), MongoDBObject("name"->1))
    userWithName.flatMap(dbo => dbo.getAs[String]("name")).getOrElse("")
  }
}
