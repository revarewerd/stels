package ru.sosgps.wayrecall.billing.user

import java.net.URL
import java.util.{Date, HashMap}
import javax.annotation.{PostConstruct, Resource}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.{ExtDirectStoreReadRequest, SortDirection}
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.security.core.context.{SecurityContext, SecurityContextHolder}
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import ru.sosgps.wayrecall.billing.security.PermissionsEditor
import ru.sosgps.wayrecall.billing.user.commands._
import ru.sosgps.wayrecall.core.{MongoDBManager, ObjectsRepositoryReader, SecureGateway, UserRolesChecker}
import ru.sosgps.wayrecall.initialization.WayrecallSessionManager
import ru.sosgps.wayrecall.utils.{ExtDirectService, ISODateTime, typingMapJava}
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._
import ru.sosgps.wayrecall.utils.web.ScalaServletConverters.sessionToMap
import ru.sosgps.wayrecall.utils.web.extjsstore.{EDSStoreServiceDescriptor, StoreAPI}
import ru.sosgps.wayrecall.utils.web.{EDSJsonResponse, ScalaJson}

import scala.Predef._
import scala.beans.BeanProperty
import scala.collection.JavaConversions.{collectionAsScalaIterable, mapAsJavaMap, mapAsScalaMap}
import scala.collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 31.07.12
 * Time: 18:32
 * To change this template use File | Settings | File Templates.
 */

@ExtDirectService
class UsersService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id", "name", "comment", "password", "phone", "email", Field("lastLoginDate", "date"),
    Field("lastAction", "date"), "mainAccId", "mainAccName", "hascommandpass", "commandpass", "enabled",
    "blockcause","canchangepass","showbalance","showfeedetails","userType","creator","hasBlockedMainAccount","hasObjectsOnBlockedAccount")

  val updatebleFields = Set("name", "comment", "password", "phone", "email", "mainAccId", "hascommandpass",
    "commandpass", "enabled", "blockcause","canchangepass","showbalance","showfeedetails","userType","creator")

  val name = "UsersService"
  val idProperty = "_id"

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var permissions: PermissionsEditor = null

  @Autowired
  var commandGateway: SecureGateway = null

  @Resource(name = "monitoringwebapp-SessionManager")
  var sessionManager: WayrecallSessionManager = null

  val dbName = "users"

  @BeanProperty
  @Value("${global.monitoringApiUrl}")
  var monitoringUrl: String = null

  @PostConstruct
  def init() {
    val collection = mdbm.getDatabase().apply(dbName)
    info("ensuring index MongoDBObject(\"name\" -> 1)")
    collection.ensureIndex(MongoDBObject("name" -> 1), MongoDBObject("unique" -> true))
  }

  @ExtDirectMethod
  def load(userId: String): Map[String,Object] = {
    if(!userId.isEmpty && (roleChecker.checkAdminAuthority() || roleChecker.hasPermissions(roleChecker.getUserName(),"user",new ObjectId(userId)))) {
      val lastAccessActive = getActiveLastSessions2()
      val data = mdbm.getDatabase().apply("users").findOne(MongoDBObject("_id" -> new ObjectId(userId)))
      val s = data /*.applyEDS(request)*/ .map(dbo => {
        dbo.getAs[Date]("lastLoginDate").foreach(d => dbo.put("lastLoginDate", ISODateTime(d)))
        val mainAccId = dbo.getAs[ObjectId]("mainAccId")
        val userId = dbo.getAs[ObjectId]("_id")
        mainAccId.foreach(oid =>
          mdbm.getDatabase().apply("accounts").findOne(DBObject("_id" -> oid)).foreach(d => dbo.put("mainAccName", d.as[String]("name")))
        )
        if (mainAccId.isDefined) dbo.put("hasBlockedMainAccount", hasBlockedMainAccount(mainAccId.get));
        else {
          dbo.put("blockedMainAccount", false); dbo.put("mainAccId", "-1")
        }
        if (userId.isDefined) dbo.put("hasObjectsOnBlockedAccount", hasObjectsOnBlockedAccount(userId.get))
        lastAccessActive.get(dbo.as[String]("name"))
          .orElse(dbo.getAs[Date]("lastAction"))
          .foreach(d => dbo.put("lastAction", ISODateTime(d)))
        dbo
      })
      s.map(wrapDBObj(_).toMap[String, Object]).getOrElse(Map.empty)
    } else Map.empty
  }

  @ExtDirectMethod
  def update(map0: Map[String, Object]) = {

    val map = new scala.collection.mutable.HashMap() ++ map0

    map.get("mainAccId")
      .collect({case a: String if (a.nonEmpty && a!="-1") => new ObjectId(a)
                case a: String if (a.nonEmpty && a=="-1") => null.asInstanceOf[ObjectId]
    })
    match {
      case Some(oid) => map("mainAccId") = oid
      case None => map.remove("mainAccId")
    }
      var userId= ObjectsRepositoryReader.ensureObjectId(map.remove("_id").getOrElse(null));
      val dataToUpdate=new HashMap[String, AnyRef]()++ map.filterKeys(updatebleFields.contains).toMap
      var command : AnyRef = null;
      if (userId == null) {
                userId = new ObjectId()
                val userName=roleChecker.getUserName()
                var creatorId=roleChecker.getUserIdByName(userName)
                debug("creator=" + creatorId)
                if(creatorId!=null)
                  map.put("creator",creatorId)
                else
                  map.put("creator",userName)
                debug("New  user userId=" + userId)
                command=new UserCreateCommand(userId, dataToUpdate)
            } else {
                debug("Data udpate for user userId=" + userId);
                command=new UserDataSetCommand(userId, dataToUpdate)
     }
     try {
      commandGateway.sendAndWait(command,roleChecker.getUserAuthorities(),roleChecker.getUserName())
     }
     catch {
      case e: org.axonframework.commandhandling.CommandExecutionException => throw new RuntimeException("При сохранении данных произошла ошибка: " + e.getCause.getMessage(), e);
     }

    map + ("_id" -> userId.toString);
  }

  @ExtDirectMethod
  def create(map0: Map[String, Object]): mutable.Map[String, Object] = {
    info("create new user")
    val map = new scala.collection.mutable.HashMap() ++ map0

    map.get("mainAccId")
      .collect({case a: String if (a.nonEmpty && a!="-1") => new ObjectId(a)
                case a: String if (a.nonEmpty && a=="-1") => null.asInstanceOf[ObjectId]
    })
    match {
      case Some(oid) => map("mainAccId") = oid
      case None => map.remove("mainAccId")
    }
    var userId= ObjectsRepositoryReader.ensureObjectId(map.remove("_id").getOrElse(null));
    if (userId== null) {
                userId = new ObjectId()
                val userName=roleChecker.getUserName()
                var creatorId=roleChecker.getUserIdByName(userName)
                debug("creator=" + creatorId)
                if(creatorId!=null)
                  map.put("creator",creatorId)
                else
                  map.put("creator",userName)
                debug("New user userId="+userId)
     }
     val dataToInsert =new HashMap[String, AnyRef]()++ map.filterKeys(updatebleFields.contains).toMap
     debug("objectToInsert"+dataToInsert)
     try{
      commandGateway.sendAndWait(new UserCreateCommand(userId, dataToInsert),roleChecker.getUserAuthorities(),roleChecker.getUserName());
     }
     catch{
      case e: org.axonframework.commandhandling.CommandExecutionException => throw new RuntimeException("При сохранении данных произошла ошибка: " + e.getCause.getMessage(), e);
     }
     map + ("_id" -> userId.toString);
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_MODIFY)
  @StoreAPI("destroy")
  def remove(request: HttpServletRequest, response: HttpServletResponse, maps: Seq[Map[String, Any]]) = {

    info("maps" + maps)
    maps.foreach(map => {
        info("removing user" + map("_id").toString)
      try {
        commandGateway.sendAndWait(new UserDeleteCommand(new ObjectId(map("_id").toString)),roleChecker.getUserAuthorities(),roleChecker.getUserName());
      }
      catch {
        case e : Exception => { throw new RuntimeException("При удалении записи произошла ошибка: " + Option(e.getCause()).map(_.getMessage()).getOrElse(e), e) }
      }
          map("_id").toString
    })
  }



  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def loadAllUsers(request: ExtDirectStoreReadRequest) = {
    EDSJsonResponse(getAllUsers(request))
  }

  def getAllUsers(request: ExtDirectStoreReadRequest) = {
    val auth=SecurityContextHolder.getContext.getAuthentication
    //val lastAccessActive = getActiveLastAccess()
    val lastAccessActive = getActiveLastSessions2()

    //setDefaultSorterIfEmpty(request,"name", SortDirection.ASCENDING)
    val query=request.getQuery
    val allObjects =
      if (query != null && query != "") {
        roleChecker.getPermForCurrentUser().filter(_.getAsOrElse[String]("name", "").contains(query))
      }
      else
        roleChecker.getPermForCurrentUser()
    //mdbm.getDatabase().apply(dbName).find(searchAndFilters(request), model.fieldsNames.map(_ -> 1).toMap)
    val s = allObjects/*.applyEDS(request)*/.map(dbo => {
        dbo.getAs[Date]("lastLoginDate").foreach(d => dbo.put("lastLoginDate", ISODateTime(d)))
        val mainAccId=dbo.getAs[ObjectId]("mainAccId")
        val userId=dbo.getAs[ObjectId]("_id")
        if(mainAccId.isDefined){
          mainAccId.foreach(oid =>
          mdbm.getDatabase().apply("accounts").findOne(DBObject("_id" -> oid)).foreach(d => dbo.put("mainAccName", d.as[String]("name"))))
          dbo.put("hasBlockedMainAccount",hasBlockedMainAccount(mainAccId.get));        }
        else {dbo.put("blockedMainAccount",false); dbo.put("mainAccId","-1"); dbo.put("mainAccName","- нет -")}
        if(userId.isDefined) dbo.put("hasObjectsOnBlockedAccount",hasObjectsOnBlockedAccount(userId.get))
        lastAccessActive.get(dbo.as[String]("name"))
          .orElse(dbo.getAs[Date]("lastAction"))
          .foreach(d => dbo.put("lastAction", ISODateTime(d)))
        dbo
      })
    userTypeFilter(s.map(wrapDBObj(_)).toList)
  }

  def hasBlockedMainAccount(mainAccId: ObjectId): Boolean = try {
      !mdbm.getDatabase().apply("accounts").findOne(DBObject("_id" -> mainAccId)).get("status").asInstanceOf[Boolean]
  } catch {
    case e: Exception => warn("hasBlockedMainAccount error:", e); false
  }

  def hasObjectsOnBlockedAccount(userId: ObjectId): Boolean = try {
     val permAccs : Seq[DBObject] =permissions.getPermissionsRecords(userId,"account")
      permAccs.foreach(acc=>{
        val id=acc.get("item_id")
        //debug("account id="+id)
        val accEnabled=mdbm.getDatabase().apply("accounts").findOne(DBObject("_id" -> id)).get("status").asInstanceOf[Boolean]
        if(!accEnabled) return true
      })
     val permObjs: Seq[DBObject] =permissions.getPermissionsRecords(userId,"object")
     permObjs.foreach(obj=>{
       val id=obj.get("item_id")
       //debug("object id="+id)
       val accId=mdbm.getDatabase().apply("objects").findOne(DBObject("_id" -> id)).get("account").asInstanceOf[ObjectId]
       val accEnabled=mdbm.getDatabase().apply("accounts").findOne(DBObject("_id" -> accId)).get("status").asInstanceOf[Boolean]
       if(!accEnabled) return true
     })
    false
  } catch {
    case e: Exception => warn("hasObjectsOnBlockedAccount error:", e); false
  }

  def userTypeFilter(allObjects:List[MongoDBObject]) : List[MongoDBObject]  = {
    val roles = roleChecker.getUserAuthorities()
    val superUserParams = Set("_id", "name","password", "email","phone","lastLoginDate","lastAction", "mainAccId", "mainAccName","hascommandpass", "commandpass", "enabled", "blockcause","canchangepass","showbalance","showfeedetails","userType","creator")
    if (roles.contains("admin")) {
      allObjects.map(wrapDBObj(_));
    }
    else if (roles.contains("superuser")) {
      allObjects.map(_.filter(item => superUserParams.contains(item._1)))
    }
    else List.empty
  }

  def findById(recordId : ObjectId) = {

    val lastAccessActive = getActiveLastSessions2()

    val allObjects = mdbm.getDatabase().apply(dbName).findOne(DBObject("_id"->recordId), model.fieldsNames.map(_ -> 1).toMap)
    val user = allObjects.map(dbo => {
      dbo.getAs[Date]("lastLoginDate").foreach(d => dbo.put("lastLoginDate", ISODateTime(d)))
      val mainAccId=dbo.getAs[ObjectId]("mainAccId")
      if(mainAccId.isDefined){
        mainAccId.foreach(oid =>
        mdbm.getDatabase().apply("accounts").findOne(DBObject("_id" -> oid)).foreach(d => dbo.put("mainAccName", d.as[String]("name"))))
        dbo.put("hasBlockedMainAccount",hasBlockedMainAccount(mainAccId.get));}
      else {dbo.put("blockedMainAccount",false); dbo.put("mainAccId","-1"); dbo.put("mainAccName","- нет -")}
      lastAccessActive.get(dbo.as[String]("name"))
        .orElse(dbo.getAs[Date]("lastAction"))
        .foreach(d => dbo.put("lastAction", ISODateTime(d)))
      dbo
    })
    user
  }

  private[this] def getActiveLastSessions2(): Map[String, Date] = {
    val result = new mutable.HashMap[String, Date]()

    for (session <- sessionManager.getActiveSessions;
         name <- getAuth(session).map(_.getName)
    ) {
      result.get(name) match {
        case Some(d) if session.getLastAccessedTime > d.getTime => result.put(name, new Date(session.getLastAccessedTime))
        case None => result.put(name, new Date(session.getLastAccessedTime))
        case _ =>
      }
    }

    result.toMap
  }

  private[this] def getAuth(sess: HttpSession) = {
    val sc = sess.get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY).map(_.asInstanceOf[SecurityContext])
    val auth = sc.flatMap(sc => Option(sc.getAuthentication))
    auth
  }

//  private[this] def getActiveLastAccess(): Map[String, Date] = {
//    try {
//      debug("monitoringUrl=" + monitoringUrl)
//      val r = ScalaJson.parse[List[java.util.Map[String, AnyRef]]](new URL(monitoringUrl + "/EDS/sessions/list"))
//      debug("r=" + r)
//      r.filter(_.containsKey("login")).
//        map(m => (m.as[String]("login"), m.as[java.lang.Long]("lastAccess"))).
//        groupBy(_._1).mapValues(v => new Date(v.map(_._2).max))
//    } catch {
//      case e: Exception => {
//        warn("session request exception:", e)
//        Map.empty
//      }
//    }
//  }
}
