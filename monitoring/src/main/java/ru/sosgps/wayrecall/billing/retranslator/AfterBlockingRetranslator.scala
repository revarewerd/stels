package ru.sosgps.wayrecall.billing.retranslator

import java.util.Date
import javax.annotation.PostConstruct

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.annotation.{AnnotationEventListenerAdapter, EventHandler}
import org.bson.types
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.account.events.AccountStatusChanged
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.globalService

/**
 * Created by nickl on 15.07.14.
 */
class AfterBlockingRetranslator extends grizzled.slf4j.Logging {

  @Autowired
  var eb: EventBus = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var retranslatorsListService: RetranslatorsListService = null

  @Autowired
  var retranslationTasks: RetranslationTasks = null

  @Autowired
  var packStore: PackagesStore = null

  @PostConstruct
  def init() {
    eb.subscribe(new AnnotationEventListenerAdapter(this))
  }

  @Autowired
  var permissionsChecker: ObjectsPermissionsChecker = _

  @EventHandler
  private def handleAccountStatusChanged(event: AccountStatusChanged) {
    debug("AccountStatusChanged : " + event)
    val now = new Date()
    val accId = event.id
    val q = MongoDBObject("account" -> accId, "status" -> event.status)
    //TODO: find and modify
    collection.update(
      q,
      q ++ MongoDBObject("time" -> now), true
    )
    if (event.status) {
      globalService.execute {
        val disabledTime = collection.findOne(MongoDBObject("account" -> accId, "status" -> false)).map(_.as[Date]("time"))
        debug("disabledTime=" + disabledTime)
        disabledTime.foreach { disabledTime =>
          val uids = getAccountObjectUids(accId)
          val retranslatingUids = retranslatorsListService.odsMosRuRetranslator.listRetranslators.flatMap(_.uids).toSet
          debug("retranslatingUids=" + retranslatingUids)
          val uidsToresend = permissionsChecker.withCheckPermissionsDisabled {
            uids.filter(uid => retranslatingUids(uid) && packStore.getPositionsCount(uid, disabledTime, now) > 0).toSeq
          }
          retranslationTasks.addRetranslation("ODS-mos-ru", null, null, 0, "odsmosru", uidsToresend, disabledTime, now)
          debug("retranslatingCheduled")
        }
      }
    }
  }

  def getAccountObjectUids(accId: types.ObjectId): Iterator[String] = {
    mdbm.getDatabase()("objects").find(MongoDBObject("account" -> accId), MongoDBObject("uid" -> 1)).map(_.as[String]("uid"))
  }

  private[this] def collection = {
    mdbm.getDatabase()("accountslastblocks")
  }
}
