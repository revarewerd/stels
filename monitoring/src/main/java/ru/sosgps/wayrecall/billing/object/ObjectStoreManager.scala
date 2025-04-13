package ru.sosgps.wayrecall.billing

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.{ExtDirectStoreReadRequest, SortDirection}
import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import ru.sosgps.wayrecall.billing.account.AccountRepository
import ru.sosgps.wayrecall.billing.equipment.EquipmentObjectChangeCommand
import ru.sosgps.wayrecall.billing.objectcqrs.{ObjectDeleteCommand, ObjectRemoveCommand}
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.core.finance.TariffPlans
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.data.sleepers.{SleeperData, SleepersDataStore, Warning}
import ru.sosgps.wayrecall.utils.DBQueryUtils._
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.web.{EDSJsonResponse, EDSMongoUtils}
import ru.sosgps.wayrecall.utils.{ExtDirectService, Memo}

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable
;
@ExtDirectService
class ObjectStoreServiceShort extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model( "_id",
  "name","customName", "comment", "uid", "type",/*'contract'*/"cost","subscriptionfee", "marka", "accountId",
  "model", "gosnumber", "VIN", /*'instplace',*/ "objnote","fuelPumpLock","ignitionLock", "disabled")

  val name = "ObjectsDataShort"
  val idProperty = "uid"
  val lazyLoad = false

  val fieldsNames = model.fieldsNames - "cost"

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var tariffplans: TariffPlans = null

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {
    EDSMongoUtils.searchFieldFromQuery(request)
    val collection = mdbm.getDatabase().apply("objects")
    var allObjects: Iterator[DBObject] = Iterator.empty
    val accId = request.getParams.get("accountId")
    if(accId!=null){
      allObjects =collection.find(
        (EDSMongoUtils.searchAndFilters(request)+=("account" -> new ObjectId(accId.asInstanceOf[String]))) ++ notRemoved,
        model.fieldsNames.map(_ -> 1).toMap).sort(MongoDBObject("name" -> 1)
        ).toIterator
    }
    EDSJsonResponse(allObjects)
  }
}

@ExtDirectService
class ObjectStoreManager extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("uid", "account", "comment", /*"phone"*,*/ "accountId", "_id", "accountName", "equipmentType", "name","customName", "cost", "type", "contract", "marka", "model", "gosnumber", "VIN", "instplace", "objnote", "disabled")

  val name = "ObjectsData"
  val idProperty = "uid"
  val lazyLoad = false

  val fieldsNames = model.fieldsNames - "cost"

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var accRepo: AccountRepository = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var tariffplans: TariffPlans = null
   
  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {

    val collection = mdbm.getDatabase().apply("objects")
    val accId = request.getParams.get("accountId").asInstanceOf[String]

    val tariffPlan = tariffplans.getTariffForAccount(new ObjectId(accId)).getOrElse(tariffplans.emptyTariff)

    val p = collection.find(notRemoved ++ MongoDBObject("account" -> new ObjectId(accId))).sort(MongoDBObject("name" -> 1))

    val getAccName = Memo(accRepo.getAccountName)

    val data = p.toIterable.map(s =>
      Map() ++
        s.filterKeys(fieldsNames.contains).toList
          .map(kv => (kv._1, kv._2.toString)
        )
        ++
        Map(
          "accountId" -> s.get("account").toString, //TODO: Это воркэраунд так как на клиенте где-то используйтся accountId, а где-то account
          "cost" -> tariffplans.sumCostForObject(s, tariffPlan),
          "accountName" -> s.getAs[ObjectId]("account").map(acc => getAccName(acc)).getOrElse("<unknown>") // Где же это
        )
    )

    EDSJsonResponse(data)
  }

}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

@ExtDirectService
class AllObjectsService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val terminalFields = Set("eqIMEI", "simNumber","eqMark","eqModel")

  val objectFields = Set(    "_id",    "account",    "accountName",    "name",    "customName",    "comment",    "uid",
    "type",    "contract",    "cost",    "marka",    "model",    "gosnumber",    "VIN",    "instplace",    "blocked",
    "ignition",    "objnote",    "latestmsg",    "latestmsgprotocol",    "sms",    "speed",    "satelliteNum",    "placeName",
    "sleeper",    "sleepertime",    "trackerModel",    "disabled", "radioUnit"  )

  val model = Model((terminalFields ++ objectFields).toSeq.map(a => a: Field): _*)

  val name = "AllObjectsService"
  val idProperty = "_id"
  val lazyLoad = false

  val dbName: String = "objects"

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var accRepo: AccountRepository = null

  @Autowired
  var sleepers: SleepersDataStore = null

  @Autowired
  var store: PackagesStore = null

  @Autowired
  var tariffplans: TariffPlans = null
  
  @Autowired
  var commandGateway: SecureGateway = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @ExtDirectMethod
  def delete(maps: Seq[String]): Unit = {
    maps.foreach(_id => {      
      val objId=new ObjectId(_id)
      val uid=or.uidByObjectId.apply(objId)
      val equipments = or.getObjectEquipment(objId).toSeq
      info("removing equipments:" + equipments+"from object uid="+uid)
      equipments.foreach(dbo =>{
          try {
            commandGateway.sendAndWait(new EquipmentObjectChangeCommand(new ObjectId(dbo("_id").toString), Some(uid), None), roleChecker.getUserAuthorities(),roleChecker.getUserName())
          }
          catch {
            case e: Exception => {throw new RuntimeException("При изменении объекта оборудования произошла ошибка: "+ Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage),e)}
          } 
          dbo("_id").toString
        }
      )      
      info("put object uid=" + uid+" into object.removed collection")
     // mdbm.getDatabase()("objects").find(MongoDBObject("_id" -> objId)).foreach(dbo => mdbm.getDatabase().apply("objects.removed").insert(dbo))
      info("removing object uid=" + uid)
      try {
        commandGateway.sendAndWait(new ObjectDeleteCommand(uid),roleChecker.getUserAuthorities(),roleChecker.getUserName())
      }
      catch {
        case e: org.axonframework.commandhandling.CommandExecutionException => throw new RuntimeException("При удалении объекта произошла ошибка: " + e.getCause.getMessage(), e);
      }  
      objId
      //or.removeObject(ObjectsRepository.ensureObjectId(_id))
    })
  }

  @ExtDirectMethod
  def remove(objectIds: Seq[String], deinstallEquipments: Boolean): Unit = { // Сделать нормальные параметры
    objectIds.foreach(id => {
      val objId = new ObjectId(id)
      val uid = or.uidByObjectId(new ObjectId(id))
      if(deinstallEquipments) {
        val equipments = or.getObjectEquipment(objId).toSeq
        info("removing equipments:" + equipments+"from object uid="+uid)
        equipments.foreach(dbo =>{
          try {
            commandGateway.sendAndWait(new EquipmentObjectChangeCommand(new ObjectId(dbo("_id").toString), Some(uid), None), roleChecker.getUserAuthorities(),roleChecker.getUserName())
          }
          catch {
            case e: Exception => {throw new RuntimeException("При изменении объекта оборудования произошла ошибка: "+ Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage),e)}
          }
          dbo("_id").toString
        }
        )
      }
      try {
        commandGateway.sendAndWait(new ObjectRemoveCommand(uid), roleChecker.getUserAuthorities(),roleChecker.getUserName())
      }
      catch {
        case e: org.axonframework.commandhandling.CommandExecutionException => throw new RuntimeException("При удалении объекта произошла ошибка: " + e.getCause.getMessage(), e);
      }
      objId
    })
  }

  def sleeperData(s: SleeperData): Map[String, Any] = {
    Map(
      "time" -> s.time,
      "alert" -> (s.sleeperState match {
        case Warning(info) => "Wrn" + (if (info.keys.nonEmpty) info.keys.mkString("(", ", ", ")") else "")
        case state => state.stateName
      }),
      "info" -> (s.sleeperState match {
        case Warning(info) => info.map(kv => kv._1 + ": " + kv._2).mkString(", ")
        case _ => ""
      }),
      "battery" -> Option(Seq(s.batValue, s.batPercentage).flatten).filter(_.nonEmpty).map(_.mkString("-"))
    )
  }

  def loadAccountName(accId: ObjectId) = {
    Option(accRepo.getAccountById(accId)).flatMap(dbo => {
        if(dbo.getAs[Boolean]("removed").getOrElse(false))
          dbo.getAs[String]("name").map(_ + " (в корзине)")
        else
          dbo.getAs[String]("name")
      }).getOrElse("ERROR: Acc not found")
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def loadAllObjects(request: ExtDirectStoreReadRequest) = {
    setDefaultSorterIfEmpty(request,"name", SortDirection.ASCENDING)
    debug("loadAllObjects request=" + request)



    //debug("data=" + data0.toSeq)
   // debug("data length=" + data0.toSeq.length)
    //EDSJsonResponse.forPart(data0.toSeq.length, data0)
    EDSJsonResponse(getAllObjects(request))
  }

  def getAllObjects(request: ExtDirectStoreReadRequest) = {
    val nonAccFilter = request.getParams.asScala.get("nonAccount").map(_.asInstanceOf[String]).map(nonAccId => {
      "account" $ne new ObjectId(nonAccId)
    }).getOrElse(new BasicDBObject)

    val filters = searchAndFiltersList(request)

    debug("filters=" + filters)
    val auth=SecurityContextHolder.getContext.getAuthentication

    val terminalFilters = filters.filterKeys(terminalFields)

    def allTerminalsQuery(q: Map[String, AnyRef]) = mdbm.getDatabase().apply("equipments").find(
      q + ("eqtype" -> "Основной абонентский терминал") ++ terminalFilters)

    def hasSmses(q: scala.collection.Map[String, AnyRef]): Boolean = {
      val phone = q.get("simNumber").map(_.asInstanceOf[String].stripPrefix("+")).orNull
      if(phone!=null && !phone.isEmpty){
        return     mdbm.getDatabase().apply("smses").findOne(
          $or("senderPhone"-> phone,
            "targetPhone"->phone)
        ).isDefined

      }
      return false
    }

    val data = getPermittedObjects(request) .map(dbo => dbo ++= allTerminalsQuery(Map("uid" -> dbo.get("uid"))).toIterable.headOption.getOrElse(MongoDBObject.empty)
      .filterKeys(terminalFields).toSeq)

    val tariffPlan = Memo {accid: ObjectId => tariffplans.getTariffForAccount(accid).getOrElse(tariffplans.emptyTariff)}
    val getAccName = Memo(loadAccountName)
    val data0 = data.map(d => {
      val latest = store.getLatestFor(Iterable(d.as[String]("uid"))).headOption
      val sleeperLatestOpt = sleepers.getLatestData(d.as[String]("uid"))
      (d:mutable.Map[String, AnyRef]) ++ Map(
        "latestmsg" -> latest.map(_.time).orNull,
        "ignition" -> latest.map(g => GPSUtils.detectIgnition(g).getOrElse(-1)).orNull,
        "latestmsgprotocol" -> latest.map(g => Option(g.data.get("protocol")).getOrElse("Wialon")).orNull,
        "sms" -> hasSmses(d),
        "speed" -> latest.map(_.speed).getOrElse(0),
        "satelliteNum" -> latest.map(_.satelliteNum).getOrElse(0),
        "placeName" -> latest.map(_.placeName).filter(null !=).orNull,
        "cost" -> d.getAs[ObjectId]("account").map(acc => tariffplans.sumCostForObject(d.as[ObjectId]("_id"), tariffPlan(acc))).getOrElse(0),
        "sleeper" -> sleeperLatestOpt.map(s => sleeperData(s)).orNull,
        "sleepertime" -> (sleeperLatestOpt match {
          case Some(sd) => sd.time.map(_.getTime).getOrElse(-1L)
          case None => -2L
        }),
        "accountName" -> d.getAs[ObjectId]("account").map(acc => getAccName(acc)).getOrElse("<unknown>"),
        "trackerModel" -> (d.getAsOrElse[String]("eqMark","")+" "+d.getAsOrElse[String]("eqModel","")),
        "radioUnit" -> getRadioUnit(or.getObjectEquipment(d.as[String]("uid")).toSeq)
      )
    })
    objectTypeFilter(data0.map(wrapDBObj(_)))
  }

  def objectTypeFilter(allObjects: Iterator[MongoDBObject]): Iterator[MongoDBObject] = {
    val roles = roleChecker.getUserAuthorities()
    val superUserParams = Set("uid", "account", /*"phone"*,*/ "accountId", "_id", "accountName", "equipmentType",
      "name", "customName", "cost", "type", "disabled", "blocked", "ignition", "latestmsg", "latestmsgprotocol",
      "sms", "speed", "satelliteNum", "placeName", "sleeper", "sleepertime", "trackerModel", "disabled", "eqIMEI", "radioUnit")
    if (roles.contains("admin")) {
      allObjects.map(wrapDBObj(_));
    }
    else if (roles.contains("superuser")) {
      allObjects.map(_.filter(item => superUserParams.contains(item._1)))
    }
    else Iterator.empty
  }

  def getPermittedObjects(request: ExtDirectStoreReadRequest) = {
    var allObjects : Iterator[DBObject] = Iterator.empty
    val userName = roleChecker.getUserName()
    if(roleChecker.hasRequiredStringAuthorities("admin","ObjectView")) {
      val query = notRemoved ++ searchAndFilters(request)
      info(s"getPermittedObjects query = $query")
      allObjects = mdbm.getDatabase().apply(dbName).find(query, model.fieldsNames.map(_ -> 1).toMap).applyEDS(request)
    }
    else if(roleChecker.hasRequiredStringAuthorities("superuser","ObjectView")) {
        val permittedObjects=roleChecker.getInferredPermissionRecordIds(userName,"object")
        allObjects =  mdbm.getDatabase().apply(dbName).find((notRemoved ++ ("_id" $in permittedObjects)))
    }
    allObjects
  }

  // Используется атмосферой
  def findByUID(uid: String) = {


    def data = mdbm.getDatabase().apply("objects").findOne(MongoDBObject("uid"->uid))

    def terminalsQuery = mdbm.getDatabase().apply("equipments").findOne(Map("uid" -> uid) + ("eqtype" -> "Основной абонентский терминал"))
      .getOrElse(MongoDBObject(("eqIMEI"->""), ("simNumber"->""),("eqMark"->""),("eqModel"->""))).filterKeys(terminalFields)

    def hasSmses(q: scala.collection.Map[String, AnyRef]): Boolean = {
      val phone = q.get("simNumber").map(_.asInstanceOf[String].stripPrefix("+")).orNull
      if(phone!=null && !phone.isEmpty){
        return     mdbm.getDatabase().apply("smses").findOne(
          $or("senderPhone"-> phone,
            "targetPhone"->phone)
        ).isDefined

      }
      return false
    }

    val tariffPlan = Memo {accid: ObjectId => tariffplans.getTariffForAccount(accid).getOrElse(tariffplans.emptyTariff)}
    val getAccName = Memo(loadAccountName)
    val latest = store.getLatestFor(Iterable(uid)).headOption
    val sleeperLatestOpt = sleepers.getLatestData(uid)

    val data0 = data.map(d =>
        d
        +("latestmsg" -> latest.map(_.time).orNull)
          + ("ignition" -> latest.map(g => GPSUtils.detectIgnition(g).getOrElse(-1)).orNull)
        +("latestmsgprotocol" -> latest.map(g => Option(g.data.get("protocol")).getOrElse("Wialon")).orNull)
        +("sms" -> hasSmses(d))
        +("speed" -> latest.map(_.speed).getOrElse(0))
        +("satelliteNum" -> latest.map(_.satelliteNum).getOrElse(0))
        +("placeName" -> latest.map(_.placeName).filter(null !=).orNull)
        +("cost" -> tariffplans.sumCostForObject(d.as[ObjectId]("_id"), tariffPlan(d.as[ObjectId]("account"))))
        +("sleeper" -> sleeperLatestOpt.map(s => sleeperData(s)).orNull)
        +("sleepertime" -> (sleeperLatestOpt match {
          case Some(sd) => sd.time.map(_.getTime).getOrElse(-1L)
          case None => -2L
        }))
        +("accountName" -> d.getAs[ObjectId]("account").map(acc => getAccName(acc)).getOrElse("<unknown>"))
        +("trackerModel" -> (terminalsQuery.getOrElse("eqMark","")+" "+terminalsQuery.getOrElse("eqMark","")))
        +("eqIMEI"->terminalsQuery.getOrElse("eqIMEI",""))
        +("simNumber"->terminalsQuery.getOrElse("simNumber",""))
        +("eqMark"->terminalsQuery.getOrElse("eqMark",""))
        +("eqModel"->terminalsQuery.getOrElse("eqModel",""))
        +("radioUnit" -> getRadioUnit(or.getObjectEquipment(d.as[String]("uid")).toSeq))
  )

     data0
  }

  private[this] def getRadioUnit(eqList: Seq[DBObject]): Map[String, Any] = {
    eqList.find(_.as[String]("eqtype").equals("Радиозакладка")).map(radio =>
      Map(
        "installed" -> true,
        "model" -> radio.getAsOrElse[String]("eqMark", ""),
        "type" -> radio("eqtype"),
        "serialNum" -> radio.getAsOrElse[String]("eqSerNum", ""),
        "owner" -> radio.getAsOrElse[String]("eqOwner", ""),
        "work" -> radio.getAsOrElse[String]("eqWork", ""),
        "workDate" -> radio.getAs[String]("eqWorkDate").orNull
      )
    ).getOrElse(Map("installed" -> false))
  }

}
