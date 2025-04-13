package ru.sosgps.wayrecall.core

import java.lang
import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheLoader, CacheBuilder}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.axonframework.eventhandling.annotation.EventHandler
import ru.sosgps.wayrecall.utils.funcLoadingCache
import collection.JavaConversions.asScalaBuffer
import org.bson.types.ObjectId

import com.mongodb.casbah.Imports._

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.05.13
 * Time: 22:06
 * To change this template use File | Settings | File Templates.
 */
abstract class ObjectsRepositoryScala extends grizzled.slf4j.Logging {
  this: ObjectsRepositoryReader =>

  def getMainTerminal(uid: String): Option[DBObject] = {
    val equipment = getObjectEquipment(uid): Seq[DBObject]
    equipment.find(_.as[String]("eqtype") == "Основной абонентский терминал")
  }

  def getObjectName(uid:String):String ={
    val q = MongoDBObject("uid" -> uid)
    val f = MongoDBObject("name" -> 1)
    mdbm.getDatabase()("objects").findOne(q, f)
      .orElse(mdbm.getDatabase()("objects.removed").findOne(q, f))
      .map(_.as[String]("name")).getOrElse("Unknown")
  }

  def getUserObjectName(uid:String):String ={
    val q = MongoDBObject("uid" -> uid)
    val f = MongoDBObject("name" -> 1, "customName" -> 1)
    mdbm.getDatabase()("objects").findOne(q, f)
      .orElse({
      warn("query to objects.removed for"+q+" why do you need removed objects?")
      mdbm.getDatabase()("objects.removed").findOne(q, f)
    })
      .flatMap(dbo => dbo.getAs[String]("customName").filterNot(_.isEmpty)
      .orElse(dbo.getAs[String]("name"))).getOrElse("Unknown")
  }

  protected val disabledCache = CacheBuilder.newBuilder()
    .expireAfterWrite(1, TimeUnit.HOURS)
    .buildWithFunction[String, java.lang.Boolean]((uid:String) => {
    val orelse= Option(getObjectByUid(uid)).flatMap(_.getAs[scala.Boolean]("disabled")).getOrElse(false)
    orelse:lang.Boolean
  })

  def isObjectDisabled(uid: String) = disabledCache(uid).booleanValue()

  @EventHandler private[core] def handleObjectEnabledChanged(event: ObjectEnabledChanged) {
    debug("processing ObjectEnabledChanged: " + event)
    disabledCache.invalidate(event.uid)
  }
  
//  @Deprecated
//  def removeObject(objId: ObjectId) = {
//    info("removing from " + "objects 1" + " " + objId + " " + springCurrentRequestOption.map(_.getRemoteAddr).orNull + " " + springCurrentRequestOption.map(_.getUserPrincipal).orNull)
//    val equipments = getObjectEquipment(objId).toSeq
//    info("removing equipments:" + equipments)
////    try {
////      equipments.foreach(dbo => mdbm.getDatabase().apply("equipments.removed").insert(dbo))
////    }
////    catch {case e: Exception => warn("exception", e)}
//
//    equipments.foreach(dbo =>
//      mdbm.getDatabase()("equipments").update(dbo, $unset("uid"),false,false,WriteConcern.Safe)
//    )
//
//    mdbm.getDatabase()("objects").find(MongoDBObject("_id" -> objId)).foreach(dbo => mdbm.getDatabase().apply("objects.removed").insert(dbo))
//    val wc = mdbm.getDatabase()("objects").remove(MongoDBObject("_id" -> objId), WriteConcern.Safe)
//  }

}
