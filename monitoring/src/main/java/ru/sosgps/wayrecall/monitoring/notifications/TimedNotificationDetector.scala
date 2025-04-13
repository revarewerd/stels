package ru.sosgps.wayrecall.monitoring.notifications

import javax.annotation.PostConstruct

import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.monitoring.notifications.rules.TimedGPSNotificationRule.EventType
import ru.sosgps.wayrecall.monitoring.notifications.rules._
import ru.sosgps.wayrecall.utils

import scala.beans.BeanProperty
import scala.collection.Map

/**
 * Created by nickl on 21.05.15.
 */
class TimedNotificationDetector extends AbstractNotificator {

   private val timedNotificationTypes = Set("ntfNoMsg", "ntfLongParking")

   @Autowired
   @BeanProperty
   var packStore: PackagesStore = null

   @BeanProperty
   var runSelfThread = true

   @PostConstruct
   def init() {

     //    info("ensureIndex notificationRules objects")
     //    mdbm.getDatabase()("notificationRules").ensureIndex("type")
     //
     //    info("ensureIndex notificationRulesStates uid")
     //    mdbm.getDatabase()("notificationRulesStates").ensureIndex(MongoDBObject("uid" -> 1, "notificationRule" -> 1), MongoDBObject("unique" -> 1))
     //
     //    info("ensureIndex geoZonesState uid")
     //    mdbm.getDatabase()("geoZonesState").ensureIndex("uid")

     //es.subscribe(PredefinedEvents.objectGpsEvent, onGpsEvent)
     if (runSelfThread)
     startTesterThread()
   }

   @BeanProperty
   var testInterval = 20 * 1000

   private[this] def startTesterThread(): Unit = {
     val thread = new Thread(new Runnable {
       override def run(): Unit = {
         while (true) {
           try {
             doTest(System.currentTimeMillis())
           }
           catch {
             case e: Throwable => warn("TimedNotificationDetector exception:", e)
           }
           Thread.sleep(testInterval)
         }

       }
     }, "TimedNotificationDetector-" + mdbm.instance.map(_.name).orNull)
     thread.setDaemon(true)
     thread.start()
   }

   private[notifications] def doTest(nowTime: Long): Unit = {
     val notificationRules = mdbm.getDatabase()("notificationRules").find("type" $in timedNotificationTypes)

     def fireAndSave(uid: String, stateChange: NotificationStateChange[(Long, String, PackagesStore)]): WriteResult = {
       debug("fireAndSave: " + uid + " stateChange:" + stateChange)
       if (stateChange.firingNow)
         stateChange.notifications.foreach(notifyUser(_, "Событие простоя"))


       val newState = stateChange.updatedState ++ MongoDBObject(
         "uid" -> uid,
         "notificationRule" -> stateChange.notificationId,
         "lastTested" -> nowTime
       )

       if(stateChange.firingNow)
         newState.put("fireTime",nowTime)
       if(stateChange.unfired)
         newState.removeField("fireTime")

       debug("savin new state:" + newState)
       mdbm.getDatabase()("notificationRulesStates").update(MongoDBObject("uid" -> uid, "notificationRule" -> stateChange.notificationId),
         newState,
         upsert = true
       )
     }
     for (ruleDbo <- notificationRules) try {
       val allobjects = ruleDbo.getAsOrElse[Boolean]("allobjects", false)
       val objects: Set[String] = if (allobjects)
         permissions.getAvailableObjectsPermissions(ruleDbo.as[String]("user")).keySet
       else
         ruleDbo.as[MongoDBList]("objects").map(_.asInstanceOf[String])
           .filter(permissions.isObjectAvaliableFor(ruleDbo.as[String]("user"), _)).toSet

       debug(s"all objects for user ${ruleDbo.as[String]("user")} in $ruleDbo: $objects")

       val interval = utils.tryLong(ruleDbo.as[DBObject]("params").as[Any]("interval"))
       val statesToTest = mdbm.getDatabase()("notificationRulesStates")
         .find($or("lastTested" $lte nowTime - interval, "fireTime" $lte nowTime - interval)
           ++ MongoDBObject("notificationRule" -> ruleDbo("_id")) ++ ("uid" $in objects)
         ).toList

       debug("statesToTest:" + statesToTest)

       val base: NotificationRule[EventType] = NotificationRule.fromDbo[EventType](ruleDbo, appContext)
       val rule = ruleDbo.as[String]("type") match {
         case "ntfNoMsg" =>
           new LongNoMessageRule(
             base,
             interval = interval
           )
         case "ntfLongParking" =>
           new LongParkingRule(
             base,
             interval = interval
           )
       }

       permissions.withCheckPermissionsDisabled {

         // val withoutStateYet = objects -- statesToTest.map(_.as[String]("uid"))
         val withoutStateYet = objects -- mdbm.getDatabase()("notificationRulesStates")
           .find(MongoDBObject("notificationRule" -> ruleDbo("_id")), MongoDBObject("uid" -> 1)).map(_.as[String]("uid"))

         debug("withoutStateYet:" + withoutStateYet)

         val withStateIterator = statesToTest.map(stateDbo => (stateDbo.as[String]("uid"), stateDbo: Map[String, AnyRef])).iterator
         val withoutStateIterator = withoutStateYet.map((_, Map.empty[String, AnyRef])).iterator

         for( (uid, state) <-  withStateIterator ++ withoutStateIterator) try {
           val stateChange = rule.process((nowTime, uid, packStore), state)
           fireAndSave(uid, stateChange)
         } catch {
           case e: NoDataException => debug("no data for "+uid+" : "+ e.getMessage)
         }

       }

     } catch {
       case e: Exception => warn("exception processing:" + ruleDbo + " at " + nowTime, e)
     }


   }

 }
