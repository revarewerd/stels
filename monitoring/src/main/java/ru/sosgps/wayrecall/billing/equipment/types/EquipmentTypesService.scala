

package ru.sosgps.wayrecall.billing.equipment.types

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import ru.sosgps.wayrecall.billing.equipment.types.commands._
import ru.sosgps.wayrecall.core.{UserRolesChecker, SecureGateway, MongoDBManager}
import ru.sosgps.wayrecall.utils.web.extjsstore.{StoreAPI, EDSStoreServiceDescriptor}
import org.springframework.beans.factory.annotation.Autowired
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import org.axonframework.commandhandling.gateway.CommandGateway
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
import ru.sosgps.wayrecall.billing.security.PermissionsEditor

import scala.collection.immutable.TreeSet

@ExtDirectService
class EquipmentTypesService  extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging { 
  val model = Model("_id", "type","mark", "model", "server","port")
  val updatebleFields = Set("type","mark", "model", "server","port")

    val name = "EquipmentTypesService"
    val idProperty = "_id"

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var commandGateway: SecureGateway = null;

  val dbName = "equipmentTypes"
  
  def getCollection = mdbm.getDatabase().apply(dbName)
   
  @PostConstruct
  def init() {
    val collection = getCollection
//    info("ensuring index MongoDBObject(\"name\" -> 1)")
//    collection.ensureIndex(MongoDBObject("name" -> 1), MongoDBObject("unique" -> true))
  }
  
  @ExtDirectMethod(value = ExtDirectMethodType.STORE_MODIFY)
  @StoreAPI("destroy")
  def remove(maps: Seq[Map[String, Any]]) = {
    maps.foreach(map => {
      require(map != null)   
      info("removing from eqTypes " + map("_id").toString) 
       try {
      commandGateway.sendAndWait(new EquipmentTypesDeleteCommand(new ObjectId(map("_id").asInstanceOf[String])),roleChecker.getUserAuthorities(),roleChecker.getUserName())
       }
       catch {
         case e : Exception => { throw new RuntimeException("При удалении записи произошла ошибка: " + Option(e.getCause()).map(_.getMessage()).getOrElse(e), e) }
       }
       
      Unit
//      info("removing from " + dbName + " " + map("_id").toString)
//      val wc = getCollection.remove(
//        MongoDBObject("_id" -> new ObjectId(map("_id").toString)))
    })
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def read (request: ExtDirectStoreReadRequest) = {
    debug("EquipmentTypesService read " + request)
    val allObjects =
    if(roleChecker.hasRequiredStringAuthorities("admin","EquipmentTypesView"))
      getCollection.find(searchAndFilters(request)).toSeq
    else
      Seq.empty
    EDSJsonResponse(allObjects)
  }

}

@ExtDirectService
class EquipmentDeviceTypesService  extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val fieldName = "name"
  
  val model = Model(fieldName)

  val name = "EquipmentDeviceTypesService"
  val idProperty = fieldName

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  val predefined = TreeSet(
    "Основной абонентский терминал",
    "Дополнительный абонентский терминал",
    "Спящий блок автономного типа GSM",
    "Спящий блок на постоянном питании типа Впайка",
    "Радиозакладка",
    "Датчик уровня топлива",
    "Виртуальный терминал"
  )

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def read (request: ExtDirectStoreReadRequest) = {
    debug("EquipmentDeviceTypesService read " + request)
    val allObjects = (predefined ++ mdbm.getDatabase()("equipmentTypes").distinct("type")).map(t => Map(fieldName -> t))
    debug("EquipmentDeviceTypesService allObjects: " + allObjects)
    EDSJsonResponse(allObjects)
  }

}




