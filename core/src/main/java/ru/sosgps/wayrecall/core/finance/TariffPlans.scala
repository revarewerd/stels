package ru.sosgps.wayrecall.core.finance

import com.google.common.cache.{CacheBuilder, LoadingCache}
import com.mongodb.BasicDBObject
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.sosgps.wayrecall.core.{MongoDBManager, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.events.{DataEvent, EventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.utils.DBQueryUtils._
import ru.sosgps.wayrecall.utils.{ThreadLocalOps, funcLoadingCache}

import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 28.06.12
 * Time: 15:22
 * To change this template use File | Settings | File Templates.
 */


@Component
class TariffPlans extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  def getTariffIdForAccount(accId: String): Option[ObjectId] = getTariffIdForAccount(new ObjectId(accId))

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var es: EventsStore = null


  @PostConstruct
  def init() {
    es.subscribeTyped(PredefinedEvents.objectChange, onObjectChange)
    es.subscribeTyped(PredefinedEvents.equipmentChange, onEqChange)
  }

  private[this] def onEqChange(e: DataEvent[java.io.Serializable]) { // Сделать чтобы DataEvent отправлялся обработчиками EquimpmentRemoved
    debug("onEqChange:"+e)
    val uid = mdbm.getDatabase()("equipments").findOneByID(new ObjectId(e.targetId)).flatMap(_.getAs[String]("uid"))
    debug("uid:"+uid)
    uid.foreach{ uid =>
      val objectId = or.objectIdByUid(uid)
      debug("invalidating " + objectId)
      sumCostForObjectCache.invalidate(objectId)
    }
  }

  private[this] def onObjectChange(e: DataEvent[java.io.Serializable]) {
    val objectId = or.objectIdByUid(e.targetId)
    debug("invalidating " + objectId)
    sumCostForObjectCache.invalidate(objectId)
  }

  def getTariffIdForAccount(accId: ObjectId): Option[ObjectId] = {
    val tariffOption = mdbm.getDatabase()("accounts").findOneByID(accId, MongoDBObject("plan" -> 1))
      .flatMap(_.getAs[AnyRef]("plan").filter(None !=)).flatMap({
      case str: String => try {
        Some(new ObjectId(str))
      } catch {
        case e: java.lang.IllegalArgumentException => None
      }
      case oid: ObjectId => Some(oid)
    })
    tariffOption
  }

  private val tariffContext = new ThreadLocal[DBObject]()

  private val sumCostForObjectCache: LoadingCache[ObjectId, java.lang.Long] = CacheBuilder.newBuilder()
    .expireAfterWrite(4, TimeUnit.HOURS)
    .buildWithFunction(
      id => try {
        val tariffPlan = tariffContext.get()
        if (!or.isObjectDisabled(or.uidByObjectId(id))) {
          val devicesPrice = mapDeviceTypeToPrice(tariffPlan)
          val devices = getObjectDevices(id).map(_.as[String]("eqtype"))
          devices.map(devicesPrice).sum: java.lang.Long
        }
        else
          0L: java.lang.Long
      } catch {
        case e: Exception => warn("error processing cost for object " + id + " tariffContext=" + tariffContext.get(), e); -1L
      }
    )

  def getObjectDevices(id: ObjectId): MongoCollection#CursorType =
    getObjectDevices(or.uidByObjectId(id))

  def getObjectDevices(uid: String): MongoCollection#CursorType = {
    mdbm.getDatabase()("equipments").find(
      MongoDBObject("uid" -> uid), // ++ notRemoved,
      MongoDBObject("eqtype" -> 1, "eqIMEI" -> 1)
    )
  }

  def mapDeviceTypeToPrice(tariffPlan: DBObject): Map[String, Long] = {
    tariffPlan.as[MongoDBList]("abonentPrice")
      .map(v => {
        val mdbo: MongoDBObject = wrapDBObj(v.asInstanceOf[DBObject])
        (mdbo.as[String]("name"), mdbo.as[String]("cost").toLong * 100)
      }).toMap.withDefaultValue(0L)
  }

  def getAdditionalPrices(plan: DBObject): Seq[DBObject] =
    plan.getAs[MongoDBList]("additionalAbonentPrice").getOrElse(MongoDBList.empty).map(o => o.asInstanceOf[DBObject])

  def sumCostForObject(obj: DBObject, tariffPlan: DBObject): Long =
    sumCostForObject(obj.get("_id").asInstanceOf[ObjectId], tariffPlan)

  def sumCostForObject(id: ObjectId, tariffPlan: DBObject): Long =
    tariffContext.withValue(tariffPlan) {
      sumCostForObjectCache(id)
    }


  def calcTotalCost(account: DBObject): Long = try {

    val planOption = getTariffForAccount(account)

    planOption match {
      case Some(plan) =>
        getAccountObjects(account)
          .map(o => sumCostForObject(o, plan)).sum +
          getAdditionalPrices(plan).map(_.as[String]("cost").toLong * 100).sum
      case None => 0L
    }

  } catch {
    case e: Throwable => error("calcTotalCost error", e); -1
  }

  def invalidateCache(): Unit = sumCostForObjectCache.invalidateAll()

  def getAccountObjects(account: DBObject): MongoCollection#CursorType = {
    mdbm.getDatabase()("objects")
      .find(
        MongoDBObject("account" -> account("_id")) ++ notRemoved,
        MongoDBObject("_id" -> 1, "uid" -> 1, "name" -> 1, "disabled" -> 1)
      )
  }

  val emptyTariff = new BasicDBObject {
    override def get(key: String) = MongoDBList.empty
  }

  def getTariffForAccount(account: DBObject): Option[DBObject] =
    getTariffForAccount(account.as[ObjectId]("_id"))

  def getTariffForAccount(accId: ObjectId): Option[DBObject] = {
    getTariffIdForAccount(accId).flatMap(tid =>
      mdbm.getDatabase()("tariffs").findOneByID(tid)
    )
  }
}
