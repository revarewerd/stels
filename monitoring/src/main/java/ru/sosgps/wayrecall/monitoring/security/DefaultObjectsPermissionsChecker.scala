package ru.sosgps.wayrecall.monitoring.security

import org.springframework.stereotype.Component
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, MongoDBManager}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import com.mongodb.casbah.Imports._
import scala.collection.JavaConversions.asJavaIterable
import scala.collection.JavaConversions.mapAsScalaMap
import java.util.concurrent.{TimeUnit, ConcurrentHashMap}
import ru.sosgps.wayrecall.utils.typingMap
import com.google.common.cache.{CacheLoader, CacheBuilder}
import com.mongodb.casbah.commons.MongoDBObject
import ru.sosgps.wayrecall.events._
import javax.annotation.PostConstruct
import javax.jms.{MessageListener, ObjectMessage, Message}
import java.util
import org.springframework.web.context.request.{ServletRequestAttributes, RequestContextHolder}
import com.mongodb.{WriteConcern, BasicDBObject}
import org.axonframework.eventhandling.annotation.{AnnotationEventListenerAdapter, EventHandler}
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionsManager}
import ru.sosgps.wayrecall.billing.user.permission.events.{PermissionDeleteEvent, PermissionDataSetEvent, PermissionCreateEvent}
import org.axonframework.eventhandling.EventBus
import scala.collection.JavaConversions.mapAsScalaMap
import ru.sosgps.wayrecall.utils.typingMapJava


/**
 * First implementation of [[ru.sosgps.wayrecall.security.ObjectsPermissionsChecker O b j e c t s P e r m i s s i o n s C h e c k e r]]
 * based on [[ru.sosgps.wayrecall.security.PermissionsManager P e r m i s s i o n s M a n a g e r]]
 */
class DefaultObjectsPermissionsChecker extends PermissionsManager with ObjectsPermissionsChecker with grizzled.slf4j.Logging /*with MessageListener*/ {

  @Autowired
  var mdbm: MongoDBManager = _

  @Autowired
  var es: EventsStore = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var eventBus: EventBus = null

  @PostConstruct
  def init {
    info("ensuring index MongoDBObject(\"userId\":1, \"recordType\":1)")
    mdbm.getDatabase()("usersPermissions").ensureIndex(MongoDBObject("userId" -> 1, "recordType" -> 1))
    mdbm.getDatabase()("usersPermissions").ensureIndex(MongoDBObject("recordType" -> 1, "item_id" -> 1))

    es.subscribeTyped(PredefinedEvents.objectChange, (e: DataEvent[java.io.Serializable]) => {
      debug("event " + e + " received data=" + e.data)
      val uid = e.targetId
      val usersId = this.getPermittedUsers("object", or.objectIdByUid(uid))
      val loginsToUpdate = mdbm.getDatabase()("users").find("_id" $in usersId, MongoDBObject("name" -> 1)).map(_.as[String]("name")).toList
      debug("cleaning cache for " + loginsToUpdate + "")
      permissionsCache.invalidateAll(loginsToUpdate)
      for ((k, v) <- permissionsCache.asMap() if (v.contains(uid))) {
        debug("cleaning cache for " + k + "")
        permissionsCache.invalidate(k)
      }
      notifyUsers(loginsToUpdate: _*)
    })

    es.subscribeTyped(PredefinedEvents.accountChange, (e: Event) => {
      debug("event " + e)
      val accid = e.targetId
      val usersId = this.getPermittedUsers("account", new ObjectId(accid))
      val loginsToUpdate = mdbm.getDatabase()("users").find("_id" $in usersId, MongoDBObject("name" -> 1)).map(_.as[String]("name")).toList
      debug("cleaning cache for " + loginsToUpdate + "")
      permissionsCache.invalidateAll(loginsToUpdate)
      notifyUsers(loginsToUpdate: _*)
    })

    eventBus.subscribe(new AnnotationEventListenerAdapter(this))
  }

  private[this] val checkIsDisabled_threadlocal = new ThreadLocal[Boolean]() {
    override def initialValue() = false
  }

  def allPermitted: Boolean = checkIsDisabled_threadlocal.get()

  def withCheckPermissionsDisabled[T](f: => T): T = {
    val prev = checkIsDisabled_threadlocal.get()
    checkIsDisabled_threadlocal.set(true)
    try {
      f
    }
    finally {
      checkIsDisabled_threadlocal.set(prev)
    }
  }


  def principalUserObjectId(): ObjectId = getUserObjectId(this.username)

  private[this] def getUserObjectId(username: String): ObjectId = {
    mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> username))
      .getOrElse(throw new IllegalAccessException("unknown User: " + username))
      .as[ObjectId]("_id")
  }

  def username: String = Option(SecurityContextHolder.getContext.getAuthentication).map(_.getName).getOrElse(throw new IllegalAccessException("no authentication data on this thread"))


  //private[this] val permissionsCache: mutable.Map[String, Map[String,String]] = collection.JavaConversions.asScalaConcurrentMap(new ConcurrentHashMap[String, Map[String, String]]())

  private[this] val permissionsCache = CacheBuilder.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build(
      new CacheLoader[String, Map[String, DBObject]]() {
        def load(username: String): Map[String, DBObject] = {
          trace("getting avaliable objects for " + username)
          getPermittedObjectsUids(getUserObjectId(username))
        }
      })

  def getAvailableObjectsPermissions(): Map[String, DBObject] = permissionsCache(username)

  def getAvailableObjectsPermissions(login: String): Map[String, DBObject] = permissionsCache(login)

  @EventHandler def handlePermissionCreatedEvent(event: PermissionCreateEvent) {
    debug("handlePermissionCreatedEvent " + event.toHRString)
    processPermChange(event.getData)
  }

  private[this] def processPermChange(data: util.HashMap[String, AnyRef]) = try {
    val userId = ObjectsRepositoryReader.ensureObjectId(data.get("userId"))
    val userName = mdbm.getDatabase()("users").findOneByID(userId).get.as[String]("name")
    debug("removing permissions for user " + userName)
    permissionsCache.invalidate(userName)
    notifyUsers(userName)
  } catch {
    case e: Exception => warn("exception processing data:" + data, e)
  }

  @EventHandler def handlePermissionDataSetEvent(event: PermissionDataSetEvent) {
    debug("handlePermissionDataSetEvent " + event.toHRString)
    processPermChange(event.getData)
  }

  @EventHandler def handlePermissionDeleteEvent(event: PermissionDeleteEvent) {
    debug("handlePermissionDeleteEvent " + event.toHRString)
    processPermChange(event.getData)
  }

  //    def onMessage(p1: Message) {
  //      debug("onMessage " + p1)
  //      p1 match {
  //        case p1: ObjectMessage => p1.getObject match {
  //          case map: collection.Map[String, Any] => {
  //            debug("new permissions received " + map)
  //              val userName = mdbm.getDatabase()("users").findOneByID(map.as[ObjectId]("userId")).get.as[String]("name")
  //              debug("removing permissions for user " + userName)
  //              permissionsCache.invalidate(userName)
  //              notifyUsers(userName)
  //          }
  //          case o: Any => warn("unmatched object " + o)
  //        }
  //        case p1: Any => warn("unmatched message " + p1)
  //      }
  //
  //    }


  private[this] def notifyUsers(userNames: String*) {
    userNames.foreach(userName => {
      debug("notifyUsers " + userName)
      es.publish(new DataEvent[util.HashMap[String, Object]](new util.HashMap[String, Object](), PredefinedEvents.userObjectsListChanged, userName))
    }
    )
  }

  def checkIfObjectIsAvailable(uid: String): Boolean = if (allPermitted) {
    true
  }
  else {
    isObjectAvaliableFor(username, uid)
  }


  def isObjectAvaliableFor(username: String, uid: String): Boolean = {
    Option(permissionsCache.getIfPresent(username)).map(_.contains(uid)).getOrElse {
      val objectData = mdbm.getDatabase()("objects")
        .findOne(MongoDBObject("uid" -> uid), MongoDBObject("account" -> 1, "_id" -> 1)).get
      val r = getInferredPermissionRecord(getUserObjectId(username), "object", objectData.as[ObjectId]("_id"))
        .exists(isAllowingPermissionRecord)
      trace("checkIfObjectIsAvailable(" + uid + ")=" + r + " for user " + username)
      r
    }
  }

  private[this] def getPermittedObjectsUids(userId: ObjectId): Map[String, DBObject] = {
    val (permitted, forbidden) = {
      trace("getting permitted and forbidden")
      val (permitted, forbidden) = getPermissionsRecords(userId, "object").partition(isAllowingPermissionRecord)

      def getUidsByItem_Id(oids: Iterable[DBObject]): Iterator[String] =
        mdbm.getDatabase()("objects").find("_id" $in oids.map(_.as[ObjectId]("item_id")), Map("uid" -> 1)).map(_.as[String]("uid"))

      trace("loading items uids")
      (getUidsByItem_Id(permitted).toSet, getUidsByItem_Id(forbidden).toSet)
    }

    trace("loading permitted by account")
    val permittedAccountsOids = getPermittedAccountsOids(userId)
    val permittedByAccount = mdbm.getDatabase()("objects").find("account" $in permittedAccountsOids, Map("uid" -> 1)).map(_.as[String]("uid")).toSet

    val disabledAccounts = mdbm.getDatabase()("accounts")
      .find(("_id" $in permittedAccountsOids) ++ MongoDBObject("status" -> false), Map("_id" -> 1))
      .map(_.as[ObjectId]("_id")).toList
    //debug("disabledAccounts=" + disabledAccounts)
    val disabledByAccount = mdbm.getDatabase()("objects").find("account" $in disabledAccounts, Map("uid" -> 1)).map(_.as[String]("uid")).toSet
    //debug("disabledByAccount=" + disabledByAccount)

    trace("sum permitted and permitted by account (" + permittedByAccount.size + ")")
    val r = permittedByAccount -- forbidden ++ permitted -- disabledByAccount
    trace("returning avaliable objects (" + r.size + ")")
    r.map(r => (r, getObjectPermission(userId, r))).toMap
  }

  def getDisabledAccounts(): List[DBObject] = {
    val permittedAccountsOids = getPermittedAccountsOids(principalUserObjectId())
    mdbm.getDatabase()("accounts")
      .find(("_id" $in permittedAccountsOids) ++ MongoDBObject("status" -> false))
      .toList
  }

  private[this] def getPermittedAccountsOids(userObjectId: ObjectId): Seq[ObjectId] = {
    getPermissionsRecords(userObjectId, "account").filter(dbo => isAllowingPermissionRecord(dbo)).map(_.as[ObjectId]("item_id"))
  }

  def getRemoteAddress = scala.util.control.Exception.allCatch.opt(RequestContextHolder.currentRequestAttributes()).collect {
    case arrt: ServletRequestAttributes => arrt.getRequest.getRemoteAddr
  }

  def hasPermission(uid: String, requestedAuthority: String): Boolean = if (allPermitted) true
  else {
    debug("getting permissions for user:" + username + " / " + getRemoteAddress.orNull)
    hasPermission(this.username, uid, requestedAuthority)
  }

  def hasPermission(username: String, uid: String, requestedAuthority: String): Boolean = if (allPermitted) true
  else {
    hasPermission(getUserObjectId(username), uid, requestedAuthority)
  }

  def getUsersWithPermissions(uid: String, requestedAuthority: String) = {
    val permittedUsers = getPermittedUsers("object", or.objectIdByUid(uid)).filter(hasPermission(_, uid, requestedAuthority))
    mdbm.getDatabase()("users").find(("_id" $in permittedUsers), MongoDBObject("name" -> 1)).map(_.as[String]("name")).toList
  }
}

