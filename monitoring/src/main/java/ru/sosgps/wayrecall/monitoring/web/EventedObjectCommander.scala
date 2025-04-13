package ru.sosgps.wayrecall.monitoring.web

import ru.sosgps.wayrecall.utils.ExtDirectService
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.security.{PermissionValue, ObjectsPermissionsChecker}
import ru.sosgps.wayrecall.events.{PredefinedEvents, EventsStore}
import ru.sosgps.wayrecall.sms.SMSCommandProcessor
import ru.sosgps.wayrecall.core.{GPSUtils, GPSData, ObjectsRepositoryReader, MongoDBManager}
import java.util.{TimerTask, Date, Timer}
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import scala.beans.BeanProperty
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import java.util.concurrent.{RejectedExecutionException, ThreadPoolExecutor, ArrayBlockingQueue, TimeUnit}
import ru.sosgps.wayrecall.data.GPSEvent
import ch.ralscha.extdirectspring.annotation.ExtDirectMethod
import scala.collection.mutable
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.{ArrayBuffer, ListBuffer}


@ExtDirectService
class EventedObjectCommander extends grizzled.slf4j.Logging {

  @Autowired
  var permissionsChecker: ObjectsPermissionsChecker = null

  @Autowired
  var es: EventsStore = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  //@Autowired
  var timer: Timer = new Timer()

  @Autowired
  var commander: ObjectsCommander = null

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
  var thread = new ScalaExecutorService("EventedObjectCommander", 1, 1, false, 30, TimeUnit.MINUTES, new ArrayBlockingQueue[Runnable](3000), new ThreadPoolExecutor.DiscardOldestPolicy)

  @BeanProperty
  var notificationPool = new ScalaExecutorService("EventedObjectCommanderProcessingPool", 5, 30, true, 48, TimeUnit.HOURS, new ArrayBlockingQueue[Runnable](150))

  private def onGpsEvent(gps: GPSEvent) = try {
    if (!gps.gpsData.unchained())
      thread.execute({
        try {
          processGps(gps)
        } catch {
          case t: Throwable => warn("error in process event :" + gps + " " + gps.gpsData, t)
        }
      })

  } catch {
    case e: RejectedExecutionException => debug("RejectedExecutionException on " + gps.gpsData)
  }

  private abstract class ScheduledTask {

    def onGps(gps: GPSData) {}

    def cancel() {}

    def toString: String
  }

  private val gpsListeners = new TrieMap[String, mutable.Buffer[ScheduledTask]]()

  private def processGps(gps: GPSEvent) {
    trace("processgps:" + gps.gpsData)
    gpsListeners.get(gps.gpsData.uid) match {
      case Some(ls) if ls.nonEmpty =>
        notificationPool.execute {
          ls.foreach(_.onGps(gps.gpsData))
        }
      case _ =>
    }
  }

  @ExtDirectMethod
  def sendBlockCommandAtDate(uid: String, block: Boolean, password: String, date0: Long): Unit = {
    val date = new Date(date0)
    debug("sendBlockCommandAtDate:" +(uid: String, block: Boolean, password: String, date))

    val dbo = checkCanSend(uid, password)

    val tl = getOrCreateTaskList(uid);

    tl += new ScheduledTask {
      val self = this
      private val timerTask = new TimerTask {
        override def run(): Unit = {
          tl -= self
          permissionsChecker.withCheckPermissionsDisabled {
            commander.sendBlockCommand(uid, true, password, dbo.as[String]("name"))
          }
        }
      }
      timer.schedule(timerTask, date);

      override def cancel(): Unit = {
        debug("cancelling: "+ this)
        timerTask.cancel()
        tl -= self
      }

      override def toString: String = s"BlockCommandAtDate($date, $uid)"
    }

    debug("taskList uid=" + uid + " tl=" + tl)
  }


  private[this] def checkCanSend(uid: String, password: String) = {
    permissionsChecker.checkPermissions(uid, PermissionValue.BLOCK)

    val dbo = commander.getCurrentUserDbo()
    if (commander.invalidPassword(dbo, password))
      throw new IllegalArgumentException("Неправильный пароль")

    dbo
  }

  @ExtDirectMethod
  def sendBlockAfterStop(uid: String, block: Boolean, password: String): Unit = {

    debug("sendBlockAfterStop " +(uid: String, block: Boolean, password: String))

    val dbo = checkCanSend(uid, password)

    val taskList = getOrCreateTaskList(uid)
    lazy val task: ScheduledTask = new ScheduledTask {
      override def onGps(gps: GPSData) {
        trace(this.toString + " processgps:" + gps)
        if (gps.speed <= 0 && gps.insertTime.getTime - gps.time.getTime < 5 * 60 * 1000) {
          debug("sending command")
          permissionsChecker.withCheckPermissionsDisabled {
            commander.sendBlockCommand(uid, true, password, dbo.as[String]("name"))
            taskList -= task
          }
        }
      }

      override def toString: String = s"BlockAfterStop($uid)"
    }
    taskList += task
    debug("taskList uid=" + uid + " tl=" + taskList)
  }

  private[this] def getOrCreateTaskList(uid: String): mutable.Buffer[ScheduledTask] = {
    val newList = new ArrayBuffer[ScheduledTask] with mutable.SynchronizedBuffer[ScheduledTask]
    gpsListeners.putIfAbsent(uid, newList).getOrElse(newList)
  }

  @ExtDirectMethod
  def sendBlockAfterIgnition(uid: String, block: Boolean, password: String): Unit = {
    debug("sendBlockAfterIgnition " +(uid: String, block: Boolean, password: String))
    val dbo = checkCanSend(uid, password)

    val taskList = getOrCreateTaskList(uid)
    lazy val task: ScheduledTask = new ScheduledTask {
      override def onGps(gps: GPSData) {
        debug(this.toString + " processgps:" + gps)
        if (GPSUtils.detectIgnitionInt(gps).exists(_ <= 0) && gps.insertTime.getTime - gps.time.getTime < 5 * 60 * 1000) {
          debug("sending command")
          permissionsChecker.withCheckPermissionsDisabled {
            commander.sendBlockCommand(uid, true, password, dbo.as[String]("name"))
            taskList -= task
          }
        }
      }

      override def toString: String = s"BlockAfterIgnition($uid)"

    }

    taskList += task
    debug("taskList uid=" + uid + " tl=" + taskList)
  }

  @ExtDirectMethod
  def countTasks(uid: String) = {
    gpsListeners.get(uid).map(_.size).getOrElse(0)
  }

  @ExtDirectMethod
  def cancelTasks(uid: String) = {
    debug("canceliing tasks for:"+uid)
    permissionsChecker.checkPermissions(uid, PermissionValue.BLOCK)
    gpsListeners.get(uid).foreach(scheduledTasks => {
      debug("canceliing tasks: "+scheduledTasks)
      scheduledTasks.foreach(_.cancel())
      scheduledTasks.clear()
    } )
  }


}


