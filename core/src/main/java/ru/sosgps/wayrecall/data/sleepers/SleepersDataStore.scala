package ru.sosgps.wayrecall.data.sleepers

import com.mongodb
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, MongoDBManager}
import ru.sosgps.wayrecall.data.RemovalIntervalsManager
import ru.sosgps.wayrecall.events.{PredefinedEvents, DataEvent, EventsStore}
import javax.annotation.PostConstruct
import ru.sosgps.wayrecall.security
import ru.sosgps.wayrecall.sms.{SMS, SMSEvent}
import ru.sosgps.wayrecall.utils.{Memo, funcLoadingCache, funcToTimerTask}
import com.google.common.cache.{LoadingCache, CacheBuilder}
import java.util.concurrent.TimeUnit
import collection.JavaConversions.asScalaBuffer
import collection.JavaConversions.collectionAsScalaIterable
import collection.JavaConversions.mapAsScalaMap
import collection.JavaConversions.asJavaIterable
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.anyplus
import java.util.{Date, TimerTask, Timer}
import java.util.regex.{Matcher, Pattern}
import scala.collection.mutable.ListBuffer
import java.util.concurrent.atomic.AtomicInteger
import java.text.SimpleDateFormat
import ru.sosgps.wayrecall.security.{requirePermission, PermissionValue, ObjectsPermissionsChecker, PermissionsManager}
import scala.collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 15.07.13
 * Time: 19:41
 * To change this template use File | Settings | File Templates.
 */
class SleepersDataStore extends grizzled.slf4j.Logging {

  @Autowired
  protected var mdbm: MongoDBManager = null;

  @Autowired
  protected var or: ObjectsRepositoryReader = null;

  @Autowired
  protected var timer: Timer = null

  @Autowired
  var es: EventsStore = null

  @Autowired
  var permissions: ObjectsPermissionsChecker = null

  @Autowired
  var intervals: RemovalIntervalsManager = null


  @PostConstruct
  def init() {
    es.subscribeTyped(PredefinedEvents.sms, onNewSms)
    es.subscribeTyped(PredefinedEvents.objectChange, onObjectChange)
    //timer.schedule({onNewSms(new SMSEvent(new SMS(0, "fake sms", true, "79151043552", "", new Date, new Date())))}, 10000, 10000)
  }

  private[this] def onObjectChange(e: DataEvent[java.io.Serializable]) {
    debug("received Event:" + e)
    val uid = e.targetId
    val phones = Option(sleeperByUidCache.getIfPresent(uid)).getOrElse(Seq.empty)
    debug("removing uids from cache:" + uid)
    sleeperByUidCache.invalidate(uid)
    debug("removing phones from cache:" + phones)
    uidBySleeperCache.invalidateAll(phones)
    phoneHistoryCache.invalidateAll(phones)
    for ((k, v) <- uidBySleeperCache.asMap()) {
      if (v.exists(_ == uid)) {
        debug("additionally invalidate:" + uid)
        uidBySleeperCache.invalidate(k)
      }
    }

    debug("finished invalidating")

  }

  private[this] val sleeperByUidCache: LoadingCache[String, Seq[String]] = CacheBuilder.newBuilder()
    .expireAfterWrite(30, TimeUnit.HOURS)
    .buildWithFunction((uid: String) => {
    val buffer = for (
      slepper <- or.getObjectEquipment(uid).filter(_.as[String]("eqtype").contains("Спящий блок"));
      phoneNum <- slepper.getAs[String]("simNumber").filter(_.nonEmpty)
    ) yield phoneNum.stripPrefix("+")
    //debug("sleepers for " + uid + " = " + buffer)
    buffer
  })

  private[this] val uidBySleeperCache: LoadingCache[String, Option[String]] = CacheBuilder.newBuilder()
    .expireAfterAccess(30, TimeUnit.HOURS)
    .expireAfterWrite(30, TimeUnit.MINUTES) //FIXME:this is temporary, normally this cache is invalidated by code when needed
    .buildWithFunction((phone: String) => {
    debug("loading uidBySleeperCache for " + phone)
    val uidFromValues = sleeperByUidCache.asMap().find(kv => kv._2.exists(_ == phone)).map(_._1)
    val r = uidFromValues.orElse {

      mdbm.getDatabase()("equipments").findOne(anyplus("simNumber" -> phone))
        .filter(_.as[String]("eqtype").contains("Спящий блок")).flatMap(_.getAs[String]("uid"))
    }
    debug("uidBySleeperCache for " + phone + " = " + r)
    r
  })

  def uidBySleeperPhone(phone: String) = uidBySleeperCache(phone)

  private[this] def onNewSms(e: SMSEvent) {
    permissions.withCheckPermissionsDisabled {
      debug("onNewSms:" + e.sms)

      val uidBySleeper = uidBySleeperCache(e.sms.senderPhone)

      uidBySleeper.foreach(uid => {
        mdbm.getDatabase()("smses")
          .update(
            MongoDBObject("smsId" -> e.sms.smsId),
            $set("uid" -> uid),
            concern = mongodb.WriteConcern.SAFE
          )
      })

      debug("invalidating phoneHistoryCache=" + e.sms.senderPhone)
      phoneHistoryCache.invalidate(e.sms.senderPhone)

      debug(e.sms.senderPhone + " is corresponding to uid=" + uidBySleeper)
      uidBySleeper.foreach(uid => {
        //
        //sleeperHistoryCache.invalidate(uid)
        debug("publish SleeperData for " + uid + " :" + e.sms)
        es.publish(new DataEvent(getLatestData(uid).get, PredefinedEvents.objectNewSleeperData, uid))
      })
    }
  }

  private[this] val phoneHistoryCache: LoadingCache[String, Stream[SMS]] = CacheBuilder.newBuilder()
    .expireAfterAccess(48, TimeUnit.HOURS)
    .buildWithFunction((phone: String) => {
    debug("phoneHistoryCache loading for phone:" + phone)
    val uidOpt = uidBySleeperPhone(phone)
    val smses = if(uidOpt.isDefined) {
      debug("phoneHistoryCache loading with filter:" + phone)
      val uid = uidOpt.get
      intervals.getFilteredIntervals(uid).reverse.toStream.flatMap{
        case (begin,end) =>
          mdbm.getDatabase()("smses")
            .find(("sendDate" $gte begin $lte end) ++ MongoDBObject("senderPhone" -> phone, "uid" -> uidBySleeperCache(phone)))
            .sort(MongoDBObject("sendDate" -> -1)).limit(50)
      }.take(50).map(SMS.fromMongoDbObject(_))
    }
    else {
      debug("PhoneHistoryCache: unknown uid")
      mdbm.getDatabase()("smses")
        .find( MongoDBObject("senderPhone" -> phone, "uid" -> uidBySleeperCache(phone)))
        .sort(MongoDBObject("sendDate" -> -1))
        .limit(50).toStream.map(SMS.fromMongoDbObject(_))
    }
    smses
  }
    )

  def getLatestData(uid: String): Option[SleeperData] = {
    requirePermission(permissions.checkIfObjectIsAvailable(uid), "user is not permitted to work with object " + uid)
    permissions.checkPermissions(uid, PermissionValue.VIEW_SLEEPER)
    val historyMap = sleeperByUidCache(uid).map(phone => (phone, phoneHistoryCache(phone))).toMap
    if (historyMap.nonEmpty)
      Some(new SleeperData(uid, historyMap))
    else
      None
  }
}

