/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.monitoring.web

import java.util.regex.Pattern

import ru.sosgps.wayrecall.utils
import utils.web.extjsstore.StoreAPI
import utils.containsMatcher
import ru.sosgps.wayrecall.core.MongoDBManager
import com.mongodb.casbah.Imports._
import utils.ExtDirectService
import utils.web.extjsstore.EDSStoreServiceDescriptor
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.springframework.beans.factory.annotation.Autowired

import collection.immutable.IndexedSeq
import scala.Predef._
import java.util.Date

import ru.sosgps.wayrecall.monitoring.notifications.GpsNotificationDetector
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker

import scala.collection.mutable

@ExtDirectService
class NotificationRules extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model(
    "name",
    "type",
    "allobjects",
    "showmessage",
    "messagemask",
    "email",
    "phone",
    "params",
    "objects",
    "action"
  )
  val name = "NotificationRules"
  //override val storeType: String = "Ext.data.TreeStore"
  override val autoSync = false
  val idProperty = "name"
  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null
  
  @Autowired
  var permissions: ObjectsPermissionsChecker = null
  
  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def loadObjects(request: ExtDirectStoreReadRequest) = {

//    val ntfRulesList = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> permissions.username),
//      MongoDBObject("notificationRules" -> 1)).flatMap(_.getAs[MongoDBList]("notificationRules")).getOrElse(new MongoDBList())
    val ntfRulesList = mdbm.getDatabase()("notificationRules").find(MongoDBObject("user" -> permissions.username))

    val r = ntfRulesList.map(_.asInstanceOf[DBObject]).map(
      dbo => Map(
        "name" -> dbo("name"),
        "type" -> dbo("type"),
        "allobjects" -> dbo.getAs[Boolean]("allobjects").getOrElse(false),
        "showmessage" -> dbo.getAs[Boolean]("showmessage").getOrElse(true),
        "messagemask" -> dbo.getAs[String]("messagemask").getOrElse(""),
        "email" -> dbo.getAs[String]("email").getOrElse(""),
        "phone" -> dbo.getAs[String]("phone").getOrElse(""),
        "params" -> dbo("params"),
        "objects" -> dbo.getAs[Iterable[String]]("objects").getOrElse(""),
        "action" -> dbo.getAs[String]("action").getOrElse("none")
      )).toList
    debug("loadObjects="+r)
    r.iterator
  }
  
  @ExtDirectMethod
  def addNotificationRule(newRule: Map[String, Any]) = {    
    debug("Adding notification rule " + newRule)

    //TODO: Вообще это какая-то фигня, что уведомления идентифицируются по имени, у них же _id есть
    val findRule = mdbm.getDatabase()("notificationRules").findOne(MongoDBObject("user" -> permissions.username, "name" -> newRule("name")))
    debug("Found notification rule " + findRule)

    if (findRule.isEmpty) {
      mdbm.getDatabase()("notificationRules").insert(newRule + ("user" -> permissions.username))
      newRule
    } else
      Map("exists" -> true)
  }
  
  @ExtDirectMethod
  def delNotificationRule(ruleName: String) {
    debug("Deleting notification rule " + ruleName)
    mdbm.getDatabase()("notificationRules").remove(Map("user" -> permissions.username, "name" -> ruleName))
    //mdbm.getDatabase()("users").update(MongoDBObject("name" -> permissions.username), $pull("notificationRules" -> MongoDBObject("name" -> ruleName)))
  }
  
  @ExtDirectMethod
  def updNotificationRule(newRule: Map[String, Any]) {
    debug("Updating notification rule " + newRule)
    //mdbm.getDatabase()("users").update(MongoDBObject("name" -> permissions.username, "notificationRules.name" -> newRule("name")), $set("notificationRules.$" -> newRule))

    mdbm.getDatabase()("notificationRules").update(
      Map("user" -> permissions.username, "name" -> newRule("name")),
      newRule + ("user" -> permissions.username)
    )

  }
}
