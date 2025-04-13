package ru.sosgps.wayrecall.security

import org.springframework.stereotype.Component
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, MongoDBManager}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.errors.NotPermitted
import scala.collection.JavaConversions.asJavaIterable
import scala.collection.JavaConversions.mapAsScalaMap
import java.util.concurrent.{TimeUnit, ConcurrentHashMap}
import ru.sosgps.wayrecall.utils.typingMap
import com.google.common.cache.{CacheLoader, CacheBuilder}
import com.mongodb.casbah.commons.MongoDBObject
import ru.sosgps.wayrecall.events._
import javax.annotation.PostConstruct
import javax.jms.{ObjectMessage, Message}
import java.util
import org.springframework.web.context.request.{ServletRequestAttributes, RequestContextHolder}
import com.mongodb.{WriteConcern, BasicDBObject}
import org.axonframework.eventhandling.annotation.EventHandler

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 20.08.12
 * Time: 0:06
 * To change this template use File | Settings | File Templates.
 */

/**
 * Generic implementation-independent interface which is used in components that need to check permissions.
 * If you need to check permissions you should add field of this type to your component
 * to make DI-container autowire one for you.
 */
trait ObjectsPermissionsChecker {
  def withCheckPermissionsDisabled[T](f: => T): T

  def getAvailableObjectsPermissions(): Map[String, DBObject]

  def getAvailableObjectsPermissions(login: String): Map[String, DBObject]

  def checkIfObjectIsAvailable(uid: String): Boolean

  def username: String

  def hasPermission(uid: String, requestedAuthority: String): Boolean

  def hasPermission(username: String, uid: String, requestedAuthority: String): Boolean

  def checkPermissions(uid: String, requestedAuthority: String): Unit =
    if (!hasPermission(uid, requestedAuthority))
      throw new NotPermitted("Access Denied for '" + username + "' to '" + requestedAuthority + "' on '" + uid + "'")

  def getUsersWithPermissions(uid: String, requestedAuthority: String): Seq[String]

  def isObjectAvaliableFor(username: String, uid: String): Boolean

}

/**
 * Dummy implementation of ObjectsPermissionsChecker which allows everything.
 * Could be used
 */
class AllPermissivePermissionsChecker extends ObjectsPermissionsChecker {
  def withCheckPermissionsDisabled[T](f: => T) = f

  def getAvailableObjectsPermissions() = Map.empty.withDefaultValue(DBObject("view" -> true, "control" -> true, "sleepersView" -> true))

  def getAvailableObjectsPermissions(login: String) = getAvailableObjectsPermissions()

  def checkIfObjectIsAvailable(uid: String) = true

  def username: String = Option(SecurityContextHolder.getContext().getAuthentication()).get.getName()

  def hasPermission(uid: String, requestedAuthority: String) = true

  def hasPermission(username: String, uid: String, requestedAuthority: String): Boolean = true

  var defaultUsers: Seq[String] = Seq.empty

  def setDefaultUsers(du: java.util.List[String]) = {
    import scala.collection.JavaConversions.asScalaBuffer
    defaultUsers = du
  }

  def getUsersWithPermissions(uid: String, requestedAuthority: String) = defaultUsers

  def isObjectAvaliableFor(username: String, uid: String) = true
}
