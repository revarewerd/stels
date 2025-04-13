package ru.sosgps.wayrecall.monitoring.notifications

import com.mongodb.casbah.Imports._
import javax.annotation.PostConstruct

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import ru.sosgps.wayrecall.core.{DistanceUtils, GPSData, MongoDBManager, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.events.{EventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.data.{GPSEvent, UserMessage}
import ru.sosgps.wayrecall.monitoring.geozones.{Geozone, GeozonesStore}
import ru.sosgps.wayrecall.monitoring.notifications.rules._
import ru.sosgps.wayrecall.monitoring.processing.DistanceAggregationService

import scala.collection
import scala.collection.mutable.ListBuffer
import ru.sosgps.wayrecall.utils.{MailSender, OptMap, funcLoadingCache, tryDouble, tryNumerics}
import java.util.concurrent.{ArrayBlockingQueue, RejectedExecutionException, ThreadPoolExecutor, TimeUnit}
import java.util.{Date, Objects, Timer}

import ru.sosgps.wayrecall.sms.SmsGate

import scala.beans.BeanProperty
import java.text.SimpleDateFormat

import ru.sosgps.wayrecall.utils.concurrent.{GPSDataWindow2, OrderedExecutor, ScalaExecutorService}
import com.google.common.cache.{CacheBuilder, LoadingCache}
import com.mongodb.casbah.Imports
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionValue, PermissionsManager}

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.mutable
import java.lang

import kamon.Kamon
import kamon.trace.Tracer
import ru.sosgps.wayrecall.core.finance.{FeeProcessor, TariffPlans}

import scala.concurrent.duration.DurationLong

class GpsNotificationDetector extends AbstractNotificator with grizzled.slf4j.Logging {

  @PostConstruct
  def init() {

    info("ensureIndex notificationRules objects")
    mdbm.getDatabase()("notificationRules").ensureIndex("objects")

    info("ensureIndex notificationRulesStates uid")
    mdbm.getDatabase()("notificationRulesStates").ensureIndex(MongoDBObject("uid" -> 1, "notificationRule" -> 1), MongoDBObject("unique" -> 1))

    info("ensureIndex geoZonesState uid")
    mdbm.getDatabase()("geoZonesState").ensureIndex("uid")

    es.subscribe(PredefinedEvents.objectGpsEvent, onGpsEvent)
  }

  @BeanProperty
  var detectorPoolSize: Int = 4

  @BeanProperty
  var detectorPoolSizeMax: Int = 8

  @Autowired
  @BeanProperty
  var timer: Timer = null

  private val threadExecutor = new ScalaExecutorService(
    "notificationDetectorPool", detectorPoolSize, detectorPoolSizeMax,
    true, 30, TimeUnit.MINUTES,
    new ArrayBlockingQueue[Runnable](10000), new ThreadPoolExecutor.DiscardOldestPolicy)

  @BeanProperty
  var thread = new OrderedExecutor[String](
    threadExecutor
  )

  @BeanProperty
  var windowMills = 6000

  @BeanProperty
  var notificationPool = new ScalaExecutorService("notificationPool", 5, 30, true, 48, TimeUnit.HOURS, new ArrayBlockingQueue[Runnable](150))

  private[this] val cachedWindowses =
    CacheBuilder.newBuilder()
      .expireAfterAccess(1800, TimeUnit.SECONDS)
      .buildWithFunction((uid: String) => {
      val window = new GPSDataWindow2(100, windowMills.millis, timer)
      window.onDequeued = gps => processInOrderedExecutor(gps)
      window
    })


  def onGpsEvent(gps: GPSEvent): Unit = {
    if (gps.gpsData.unchained())
      return;
    //processInOrderedExecutor(gps.gpsData)
    cachedWindowses.get(gps.gpsData.uid).enqueue(gps.gpsData)
  }

  private def processInOrderedExecutor(gpsData: GPSData): Unit = {
    try {
      val traceContext = Kamon.tracer.newContext("gpsNotificationDetector-processInOrderedExecutor")
      thread.submit(Objects.requireNonNull(gpsData.uid, "uid"), new Runnable() {
        override def run(): Unit = try {
          Tracer.withContext(traceContext) {
            processGps(gpsData)
          }
          traceContext.finish()
        } catch {
          case t: Throwable => warn("error in process event :" + gpsData, t)
        }

      })
    } catch {
      case e: RejectedExecutionException => debug("RejectedExecutionException on " + gpsData)
    }
  }

  private def traceAs[T](name: String)(body: => T) = {
    Tracer.currentContext.withNewSegment(name, "notificationDetection", "wrc")(body)
  }

  def invalidateCache() = {
    cachedRules.invalidateAll()
    cachedWindowses.asMap().values().foreach(w => w.dequeueAll)
    cachedWindowses.invalidateAll()
  }

  private[this] val cachedRules =
    CacheBuilder.newBuilder()
      .expireAfterWrite(300, TimeUnit.SECONDS)
      .buildWithFunction((uid: String) => traceAs("loadRulesToCacle"){
      mdbm.getDatabase()("notificationRules").find($or("objects" -> uid, "allobjects" -> true))
        .filter(notificationDbo => {
        notificationDbo.getAs[Boolean]("allobjects") match {
          case Some(true) => try {
            permissions.isObjectAvaliableFor(notificationDbo.as[String]("user"), uid)
          }
          catch {
            case e: Exception if (e.getMessage.contains("unknown User")) => {
              warn("removing notification for unknown user:" + notificationDbo.as[String]("user") + " notificationDbo:" + notificationDbo)
              mdbm.getDatabase()("notificationRules").remove(notificationDbo)
              false
            }
          }
          case Some(false) => true
          case None => true
        }
      }).flatMap(dboToNotificationRule).toList
    })

  def rules(uid: String) = cachedRules(uid)

  protected[this] def processGps(gps: GPSData) {
    val startTime = System.currentTimeMillis()
    val dqs = threadExecutor.getActiveQueueSize
    if (dqs > 8000 && dqs % 100 == 0)
      warn("detector queue size=" + dqs)

    val gpsData = gps
    val uid = gpsData.uid
    val rules = traceAs("loadRules")(cachedRules(uid))

    val (geozonesNotificationsRules, otherNotificationsRules) = rules.partition(_.isInstanceOf[GeozoneNotificationRule])

    //debug("processGps " + gpsData + " " + geozonesNotificationsRules.size + " " + otherNotificationsRules.size)

    val toNotify =
      traceAs("detectToNotifyGeozones")(geozones.detectToNotifyGeozones(geozonesNotificationsRules.map(_.asInstanceOf[GeozoneNotificationRule]), gpsData, uid)) ++
      traceAs("detectToNotifyOther")(detectToNotify(otherNotificationsRules, gpsData, uid))
    //    if (toNotify.nonEmpty)
    //      debug("toNotify:" + gpsData + " " + toNotify)

    if (toNotify.nonEmpty)
      notificationPool.future(try {
        debug("starting notifications for " + gpsData)
        val notificationStart = System.currentTimeMillis()
        for (n <- toNotify) {
          notifyUser(n, "Событие")
        }
        debug("NotificationDetector notification took " + (System.currentTimeMillis() - notificationStart) +
          " mills (" + (System.currentTimeMillis() - startTime) + " total) on "
          + gpsData + ", in pool = " + notificationPool.getActiveQueueSize)
      } catch {
        case e: Throwable => warn("Error in notifications", e)
      })

    val elapsed = System.currentTimeMillis() - startTime
    if (elapsed > 300 || (threadExecutor.getActiveQueueSize > 2000 && (threadExecutor.getActiveQueueSize % 100) == 0))
      debug("NotificationDetector detection took " + elapsed + " mills, in queue= " + threadExecutor.getActiveQueueSize)
  }


  private val ntfloader = new NotificationRuleLoader[GPSData](List(
    classOf[MaintenanceNotificationRule],
    classOf[BadFuelNotificationRule]
  ))

  def dboToNotificationRule(dbo: Imports.DBObject): Option[NotificationRule[GPSData]] = {

    val base: NotificationRule[GPSData] = NotificationRule.fromDbo[GPSData](dbo, appContext)
    //TODO: Нужно это заметапрограммировать. Можно сделать какой-то спринговый фектори, а NotificationRule как прототипы (а то и вообще stateless их сделать)

    val opt = Some(dbo.as[String]("type")).collect {
      case "ntfGeoZ" =>
        new GeozoneNotificationRule(
          base,
          geozoneId = dbo.as[BasicDBObject]("params").getAsOrElse[Int]("ntfGeoZCh", -1),
          onLeave = dbo.as[BasicDBObject]("params").as[Boolean]("ntfGeoZRG")
        )

      case "ntfVelo" =>
        new SpeedNotificationRule(
          base,
          maxValue = tryDouble(dbo.as[BasicDBObject]("params").get("ntfVeloMax"))
        )
      /*
{ "ntfDataSensor" : "pwr_ext", "ntfDataMin" : "10", "ntfDataMax" : "500", "ntfDataOperate" : false }
* */
      case "ntfData" =>
        new ParamNotification(
          base,
          paramName = dbo.as[BasicDBObject]("params").as[String]("ntfDataSensor"),
          operateOut = dbo.as[BasicDBObject]("params").as[Boolean]("ntfDataOperate"),
          minValue = tryDouble(dbo.as[BasicDBObject]("params").get("ntfDataMin")),
          maxValue = tryDouble(dbo.as[BasicDBObject]("params").get("ntfDataMax"))
        )
      case "ntfDist" =>
        new DistanceNotificationRule(
          base,
          dbo.as[BasicDBObject]("params").getAs[String]("ntfDistInterval").getOrElse("day"),
          tryDouble(dbo.as[BasicDBObject]("params").get("ntfDistMax"))
        )
      //      case "ntfMntnc" =>
      //        new MaintenanceNotificationRule(
      //          base,
      //          dbo.as[BasicDBObject]("params").getOrDefault("ntfDistMax", Double.box(0.0)).tryDouble,
      //          dbo.as[BasicDBObject]("params").getOrDefault("ntfMotoMax", Double.box(0.0)).tryDouble
      //        )
      case "ntfSlpr" =>
        new GosafeAlarmNotificationRule(
          base,
          ntfSlprExtPower = dbo.as[BasicDBObject]("params").getAs[Boolean]("ntfSlprExtPower").getOrElse(false),
          ntfSlprMovement = dbo.as[BasicDBObject]("params").getAs[Boolean]("ntfSlprMovement").getOrElse(false),
          ntfSlprMessage = dbo.getAs[String]("messagemask").getOrElse("---")
        )

      case _ if ntfloader.isDefinedFor(dbo) => ntfloader.load(base, dbo)
    }
    opt
  }

  private[this] def detectToNotify(notificationRules: List[NotificationRule[GPSData]], gpsData: GPSData, uid: String): List[Notification[GPSData]] = {
    if (notificationRules.isEmpty)
      return List.empty

    val prevStates = mdbm.getDatabase()("notificationRulesStates").find(MongoDBObject("uid" -> uid)).toList
    //    debug("notificationRules=" + notificationRules)
    //    debug("prevStates=" + prevStates)

    if (prevStates.flatMap(_.getAs[Date]("time")).exists(_.after(gpsData.time)))
      return List.empty

    val notificationsRulesStates: Map[Imports.ObjectId, Imports.MongoDBObject] = prevStates.map(dbo => dbo.as[ObjectId]("notificationRule") -> (dbo: MongoDBObject)).
      toMap.withDefaultValue(MongoDBObject.empty: MongoDBObject)

    val notificationStateChanges = notificationRules.toList.map(nr =>
      try {
        nr.process(gpsData, notificationsRulesStates(nr.notificationId))
      } catch {
        case e: Throwable => warn("Exception in detectToNotify", e); null
      }).filter(null !=)

    val (toFire, others) = notificationStateChanges.partition(_.firingNow)

    for (notificationStateChange <- notificationStateChanges)
      updateNotificationState(uid, notificationStateChange)

    if (toFire.nonEmpty)
      debug("paramNotifications toFire=" + toFire)
    (for (ruleEvent <- toFire) yield ruleEvent.notifications).flatten
  }

  private[this] def updateNotificationState(uid: String, notificationStateChange: NotificationStateChange[GPSData]): Unit =
    updateNotificationState(uid, notificationStateChange.notificationId, notificationStateChange.updatedState)

  private[this] def updateNotificationState(uid: String, notificationId: Imports.ObjectId, updatedState: collection.Map[String, AnyRef]) {
    mdbm.getDatabase()("notificationRulesStates").update(MongoDBObject("uid" -> uid, "notificationRule" -> notificationId),
      updatedState ++ MongoDBObject(
        "uid" -> uid,
        "notificationRule" -> notificationId
      ),
      upsert = true
    )
  }

  @BeanProperty
  var cachePrecision = 0.0003

  private object geozones {

    private val geozonesCache = CacheBuilder.newBuilder()
      .expireAfterAccess(20, TimeUnit.SECONDS)
      .maximumSize(5000)
      .buildWithFunction[(Array[Int], Double, Double), Seq[Geozone]](arg => {
      gzStore.getUsersGeozonesWithPoint(arg._1, arg._2, arg._3)
    })

    private def round(n: Double): Double = {
      math.round(n / cachePrecision) * cachePrecision
    }

    def detectToNotifyGeozones(geo: List[GeozoneNotificationRule], gpsData: GPSData, uid: String): List[Notification[GPSData]] = try {
      if (geo.isEmpty || !gpsData.containsLonLat())
        return List.empty

      val geoIds = geo.map(_.geozoneId).toArray
      // val geozonesWithPoint = gzStore.getUsersGeozonesWithPoint(geoIds, gpsData.lon, gpsData.lat)
      val geozonesWithPoint = geozonesCache(geoIds, round(gpsData.lon), round(gpsData.lat))
      //    if (geozonesWithPoint.nonEmpty)
      //      debug("geozonesWithPoint=" + geozonesWithPoint.map(_.name))
      val curGeozones = geozonesWithPoint.map(_.id).toSet

      val prevgeozonestate = mdbm.getDatabase()("geoZonesState").findOne(MongoDBObject("uid" -> uid))

      if (prevgeozonestate.flatMap(_.getAs[Date]("time")).exists(_.after(gpsData.time)))
        return List.empty

      val prevGeozones = prevgeozonestate.map(_.as[MongoDBList]("geozones").map(_.asInstanceOf[Int]).toSet).getOrElse(Set.empty)

      mdbm.getDatabase()("geoZonesState").update(
        MongoDBObject("uid" -> uid),
        MongoDBObject("uid" -> uid, "geozones" -> curGeozones, "time" -> gpsData.time),
        true
      )

      val enteredGeozones = curGeozones -- prevGeozones
      val leftGeozones = prevGeozones -- curGeozones

      if (enteredGeozones.nonEmpty || leftGeozones.nonEmpty)
        debug("enteredGeozones=" + enteredGeozones + " leftGeozones=" + leftGeozones)

      val toNotify = geo.filter(gdbo => {
        gdbo.onLeave && leftGeozones(gdbo.geozoneId) || !gdbo.onLeave && enteredGeozones(gdbo.geozoneId)
      })

      if (toNotify.nonEmpty)
        debug("geozoneNotifications toFire=" + toNotify)

      (for (ruleEvent <- toNotify) yield ruleEvent.process(gpsData, null).notifications).flatten
    } catch {
      case e: com.google.common.util.concurrent.UncheckedExecutionException =>
        e.getCause match {
          case pe: org.postgresql.util.PSQLException =>
            pe.getCause match {
              case in: java.net.ConnectException => warn("postgress connection error: " + e.getMessage); List.empty
              case _ => throw e
            }
          case _ => throw e
        }
    }

  }

}

import ru.sosgps.wayrecall.events.DataEvent;

abstract class AbstractNotificator extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var es: EventsStore = null

  @Autowired
  var gzStore: GeozonesStore = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var permissions: ObjectsPermissionsChecker = null

  @Autowired
  var mail: MailSender = null

  @BeanProperty
  var sms: SmsGate = null

  @Autowired
  var tariffs: TariffPlans = null

  @Autowired
  var feeProcessor: FeeProcessor = null

  @Autowired
  var appContext: ApplicationContext = null

  protected[this] def notifyUser(n: Notification[_], eventHeader: String) {

    if (!permissions.hasPermission(n.userMessage.targetId, n.uid, PermissionValue.VIEW)) {
      warn("Usermesage " + n.userMessage + " can not be sent because user doesn't have corresponding permissions," +
        " notification=" + n.ruleEvent.notificationId)
      return
    }

    if (or.getObjectByUid(n.uid) == null) {
      warn(s"trying to notify nonexistent object ${n.uid}, this must not happen but happens")
      return
    }

//    if(or.isRemoved(n.uid)) {
//      debug(s"This object (${n.uid}) is removed, so user should not be notified")
//      return
//    }

    if (n.ruleEvent.notifyInSystem)
      es.publish(n.userMessage)

    if (n.ruleEvent.action != "none") {
      es.publish(new DataEvent[Notification[_]](n, NotificationEvents.objectNotification, ""))
    }

    val user = n.ruleEvent.user

    val tz = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> user))
      .flatMap(_.getAs[String]("timezoneId")).map(DateTimeZone.forID).getOrElse(DateTimeZone.getDefault)

    val df = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss Z")


    val notificationString = or.getUserObjectName(n.uid) + " " + n.mesString + " " + df.withZone(tz).print(new DateTime(n.eventTime))

    n.ruleEvent.email.foreach(email =>
      try {
        debug(s"sending to email '$email' message: '$notificationString'")
        mail.sendEmail(email, eventHeader, notificationString)
      } catch {
        case e: Exception => error("error sending message:", e)
      }
    )

    n.ruleEvent.phone.foreach(phone => try {

      val accountId =
        mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> user)).flatMap(_.getAs[ObjectId]("mainAccId"))
          .getOrElse(throw new IllegalArgumentException("no account for user:" + user))


      debug("accountId=" + accountId)

      val tariff = tariffs.getTariffForAccount(accountId)
      debug("tariff=" + tariff)
      val servicePrices = tariff.toIterable.flatMap(_.getAsOrElse[MongoDBList]("servicePrice", MongoDBList.empty)
        .map({ case dbo: DBObject => (dbo.as[String]("name"), (dbo.as[String]("cost").toDouble * 100).toLong) })).toMap

      val smsPrice = servicePrices.get("Уведомление по SMS")

      if (smsPrice.isEmpty)
        warn("smsPrice for user:" + user + " is not defined")

      val finalPrice = -Math.abs(smsPrice.getOrElse(-400L))
      if (finalPrice != 0)
        feeProcessor.changeBalance(accountId, finalPrice, "Уведомление по SMS",
          "Уведомление пользователя " + user + " от объекта " + or.getObjectName(n.uid) + " на телефон: " + phone,
          Map("phone" -> phone, "user" -> user))


      sms.sendSms(phone, notificationString)
    } catch {
      case e: Exception => warn("Exception in sending sms to " + phone, e)
    }
    )
  }


}





