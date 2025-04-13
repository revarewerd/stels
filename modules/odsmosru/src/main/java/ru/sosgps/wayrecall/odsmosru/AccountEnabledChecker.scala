package ru.sosgps.wayrecall.odsmosru

import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheLoader, CacheBuilder}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.axonframework.eventhandling.annotation.EventHandler
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.account.events.AccountStatusChanged
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, MongoDBManager}
import ru.sosgps.wayrecall.utils.funcLoadingCache

/**
 * Created by nickl on 15.07.14.
 */
class AccountEnabledChecker extends EnabledChecker with grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  def isAllowed(uid: String) = enabledCache(uid)

  private val enabledCache = CacheBuilder.newBuilder()
    .expireAfterAccess(3, TimeUnit.HOURS)
    .buildWithFunction[String, java.lang.Boolean](uid => {

    val acc = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("account" -> 1)).flatMap(_.getAs[ObjectId]("account"))
    val r = acc match {
      case Some(accoid) =>
        val accstatus = mdbm.getDatabase()("accounts").findOneByID(accoid, MongoDBObject("status" -> 1))
          .flatMap(_.getAs[Boolean]("status"))
        accstatus.getOrElse(true)
      case None => warn("no account found for object " + uid); true
    }
    debug("enabled for object:" + uid + " : " + r)
    r
  })

  @EventHandler
  private def handleAccountStatusChanged(event: AccountStatusChanged) {
    debug("ObjectDataChangedEvent : " + event)
    val uids = mdbm.getDatabase()("objects").find(MongoDBObject("account" -> event.id), MongoDBObject("uid" -> 1)).map(_.as[String]("uid"))
    uids.foreach(enabledCache.invalidate)
  }


}

trait EnabledChecker{
  def isAllowed(uid: String):Boolean
}

class AllowAllEnabledChecker extends EnabledChecker{
  override def isAllowed(uid: String): Boolean = true
}
