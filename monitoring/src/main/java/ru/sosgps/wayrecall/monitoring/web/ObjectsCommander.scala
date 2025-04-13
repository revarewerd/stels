package ru.sosgps.wayrecall.monitoring.web

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}
import java.util.{Date, Timer, TimerTask}
import ch.ralscha.extdirectspring.annotation.ExtDirectMethod
import com.google.common.cache.{CacheBuilder, CacheLoader}
import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports.{MongoDBObject, _}

import javax.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.finance.{FeeProcessor, TariffPlans}
import ru.sosgps.wayrecall.core.{MongoDBManager, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.events._
import ru.sosgps.wayrecall.monitoring.notifications.NotificationEvents
import ru.sosgps.wayrecall.monitoring.notifications.rules.Notification
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionValue}
import ru.sosgps.wayrecall.sms._
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import ru.sosgps.wayrecall.utils.errors.{ImpossibleActionException, UserInputException}

import scala.beans.BeanProperty
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Promise}
import scala.util.{Failure, Success}


@ExtDirectService
class ObjectsCommander extends grizzled.slf4j.Logging {

  @Autowired
  var permissionsChecker: ObjectsPermissionsChecker = null

  @Autowired
  var es: EventsStore = null

  @Autowired
  var smsCommandProcessor: SMSCommandProcessor = null

  @Autowired
  var gprsCommandProcessor: GprsCommandProcessor = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var tariffs: TariffPlans = null

  @Autowired
  var feeProcessor: FeeProcessor = null

  //  @Resource(name = "directPackageStore")
  //  var packStore: PackagesStore = null

  private val gprsCapableMarks = Set("dtm", "сигнал")

  private def gprsCapable(mark: String, imei: String) = {
    val r = gprsCapableMarks.exists(mark.toLowerCase.contains) || imei == "863591029409376" /*Imei for terting*/
    debug(s"gprsCapable($mark, $imei) = $r")
    r
  }

  @PostConstruct
  def init() {
    debug("ObjectsCommander init")
    es.subscribeTyped(PredefinedEvents.commandIsDone, (event: SMSCommandEvent) => {
      event.command match {
        case command: DigioutCommand =>
          val uid = getPhoneObject(event.targetId)
          for (blocked <- command.blocked) {
            debug("updating blocking state uid " + uid + " was " + blocked)
            mdbm.getDatabase()("objects").update(MongoDBObject("uid" -> uid), $set("blocked" -> blocked))

            //            val position = getCorrespondingGPS(command, uid)
            //              .map(gps => Map("lat" -> gps.lat, "lon" -> gps.lon))
            //              .getOrElse(Map.empty)
            if (!command.silent)
              try {
                val message = if (blocked)
                  "objectscommander.objectblocked"
                else
                  "objectscommander.objectunblocked"

                val usersLogins = permissionsChecker.getUsersWithPermissions(uid, PermissionValue.VIEW)
                for (login <- usersLogins) {
                  val userMess = new UserMessage(
                    login,
                    message,
                    "objectscommander.objectblocking",
                    Some(uid),
                    Some(command.user),
                    command.receivedAnswer.map(_.sendDate.getTime).getOrElse(event.time),
                    Map("blocked" -> blocked, "tr" -> true, "readStatus" -> false) //++ position
                  )
                  debug("publishing for " + login + ": " + userMess)
                  es.publish(userMess)
                }
              }
              catch {
                case e: Exception => warn("error in user notifying", e)
              }
          }
        case command: TrackerSMSCommand => {
          val uid = getPhoneObject(event.targetId)
          if (!command.silent)
            try {
              val message = if (command.result.isDefined) command.result.get else "no result"
              val usersLogins = permissionsChecker.getUsersWithPermissions(uid, PermissionValue.VIEW)
              for (login <- usersLogins) {
                val userMess = new UserMessage(
                  login,
                  message,
                  "smscommand",
                  Some(uid),
                  Some(command.user),
                  command.receivedAnswer.map(_.sendDate.getTime).getOrElse(event.time),
                  Map("result" -> message, "readStatus" -> false) //++ position
                )
                debug("publishing for " + login + ": " + userMess)
                es.publish(userMess)
              }
            }
            catch {
              case e: Exception => warn("error in user notifying", e)
            }
        }
        case _ =>
      }
    })

    es.subscribeTyped(NotificationEvents.objectNotification, (event: DataEvent[Notification[_]]) => {
      event.data.ruleEvent.action match {
        case "block" => {
          debug("Notification event action is block. Fire action fo uid = " + event.data.uid)
          permissionsChecker.withCheckPermissionsDisabled {
            sendBlockCommand(event.data.uid, true, event.data.ruleEvent.blockpasw, event.data.ruleEvent.user, event.data.ruleEvent.name)
          }
        }
        case _ =>
      }
    })
  }

  //  private def getCorrespondingGPS(command: DigioutCommand, uid: String): Option[GPSData] = {
  //    try {
  //      val t = new DateTime(command.receivedAnswer.get.sendDate)
  //      permissionsChecker.withCheckPermissionsDisabled(packStore.getHistoryFor(uid, t.minusMinutes(15).toDate, t.plusMinutes(15).toDate).reduceOption(
  //        (g1, g2) => if (Math.abs(t.getMillis - g1.time.getTime) < Math.abs(t.getMillis - g2.time.getTime)) g1 else g2
  //      ))
  //    }
  //    catch{
  //      case e: Exception => warn("getCorrespondingGPS error", e); None
  //    }
  //  }

  @ExtDirectMethod
  def commandPasswordNeeded: Boolean = {
    commandPasswordNeeded(getCurrentUserDbo())
  }


  def commandPasswordNeeded(userDbo: DBObject): Boolean = {
    val defined = userDbo.getAs[Boolean]("hascommandpass").getOrElse(false)
    debug("commandPasswordNeeded for user " + userDbo.get("name") + ": " + defined)
    defined
  }

  private[this] val smsCommandThread = new ScalaExecutorService(
    "smsCommandThread",
    1, 1, true, 10, TimeUnit.MINUTES,
    new ArrayBlockingQueue[Runnable](100, true)
  )

  @ExtDirectMethod
  def sendGetCoordinatesCommand(uid: String, password: String) = {
    val userDbo = getCurrentUserDbo()
    val username = userDbo.as[String]("name")

    permissionsChecker.checkPermissions(uid, PermissionValue.GET_COORDS)

    if (invalidPassword(userDbo, password))
      throw new UserInputException("Неправильный пароль")

    debug("Seng getcoords command: uid " + uid + " password=" + password)
    doSendSMSCommand(uid, username, "getcoords")
  }

  @ExtDirectMethod
  def sendRestartTerminalCommand(uid: String, password: String) = {
    val userDbo = getCurrentUserDbo()
    val username = userDbo.as[String]("name")

    permissionsChecker.checkPermissions(uid, PermissionValue.RESTART_TERMINAL)

    if (invalidPassword(userDbo, password))
      throw new UserInputException("Неправильный пароль")

    debug("uid " + uid + " password=" + password)
    debug("Seng restartterminal command: uid " + uid + " password=" + password)
    doSendSMSCommand(uid, username, "restartterminal")
  }

  @ExtDirectMethod
  def sendBlockCommand(uid: String, block: Boolean, password: String): Unit = {
    val userDbo = getCurrentUserDbo()

    permissionsChecker.checkPermissions(uid, PermissionValue.BLOCK)

    if (invalidPassword(userDbo, password))
      throw new UserInputException("Неправильный пароль")

    if (blockingNotEnabled(uid))
      throw new ImpossibleActionException("Блокировка объекта невозможна!")
    val username = userDbo.as[String]("name")

    doSendBlockAsUserUnchecked(uid, username, block)
  }

  def sendBlockCommand(uid: String, block: Boolean, password: String, username: String, eventname: String = ""): Unit = {

    permissionsChecker.checkPermissions(uid, PermissionValue.BLOCK)

    val userDbo = getCurrentUserDbo(username)
    if (invalidPassword(userDbo, password)) {
      es.publish(new UserMessage(username, "Невозможно выполнить блокировку по событию «" + eventname + "». Введён неверный пароль.", "notifications.error", Some(uid)))
      throw new IllegalArgumentException("Неправильный пароль")
    }

    if (blockingNotEnabled(uid)) {
      es.publish(new UserMessage(username, "Невозможно выполнить блокировку по событию «" + eventname + "». Объект не поддерживает блокировку.", "notifications.error", Some(uid)))
      throw new IllegalArgumentException("Блокировка объекта невозможна!")
    }

    doSendBlockAsUserUnchecked(uid, username, block)
  }


  def invalidPassword(userDbo: DBObject, password: String): Boolean = {
    val commandPassw = userDbo.getAs[String]("commandpass")
    commandPasswordNeeded(userDbo) && !commandPassw.getOrElse("").equals(password)
  }

  private[this] def blockingNotEnabled(uid: String): Boolean = {
    val blockingEnabled = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("ignitionLock" -> 1, "fuelPumpLock" -> 1)).getOrElse(new BasicDBObject())
    debug("ignitionLock is " + blockingEnabled.getOrElse("ignitionLock", false))
    debug("fuelPumpLock is " + blockingEnabled.getOrElse("fuelPumpLock", false))
    val neBlocking = blockingEnabled.getOrElse("ignitionLock", false) == false && blockingEnabled.getOrElse("fuelPumpLock", false) == false
    neBlocking
  }

  private[this] def doSendSMSCommand(uid: String, username: String, command: String): Unit = {
    if (phonesCache(uid).isEmpty) throw new ImpossibleActionException("Не указан номер телефона");
    val phone: String = phonesCache(uid).get
    val terminal: Option[Imports.DBObject] = or.getMainTerminal(uid)
    val terminalMark = terminal.flatMap(_.getAs[String]("eqMark"))
    val login = terminal.flatMap(_.getAs[String]("eqLogin")).getOrElse("")
    val passw = terminal.flatMap(_.getAs[String]("eqPass")).getOrElse("")
    val teltonikaCommands = Map("getcoords" -> "getgps", "restartterminal" -> "cpureset")
    val fmb920Commands = Map("getcoords" -> "getgps", "restartterminal" -> "cpureset")
    val fmb900Commands = Map("getcoords" -> "getgps", "restartterminal" -> "cpureset")
    val ruptelaCommands = Map("getcoords" -> "coords", "restartterminal" -> "reset")
    val umkaCommands = Map("getcoords" -> "coords", "restartterminal" -> "reset")
    val arnaviCommands = Map("getcoords" -> "coords", "restartterminal" -> "reset")
    val SMSCommands: Option[Seq[SMSCommand]] = terminalMark.collect({
      case s if s.contains("Teltonika") =>
        Seq(new TeltonikaSMSCommand(username, login, passw, teltonikaCommands(command)))
      case s if s.contains("Teltonika2") =>
        Seq(new fmb920SMSCommand(username, login, passw, fmb920Commands(command)))
      case s if s.contains("Ruptela") =>
        Seq(new RuptelaSMSCommand(username, login, passw, ruptelaCommands(command)))
      case s if s.contains("глонассsoft") =>
        Seq(new UMKaSMSCommand(username, login, passw, umkaCommands(command)))
      case s if s.contains("Arnavi") =>
        Seq(new ArnaviSMSCommand(username, login, passw, arnaviCommands(command)))
    })
    if (SMSCommands.isDefined) {
      smsCommandThread.future({
        smsCommandProcessor.sendCommand(phone, SMSCommands.get)
        chargeCurrentUserForSms(username, phone, command)
      }).onFailure {
        case e: AlreadySentException =>
          es.publish(new UserMessage(
            username,
            "Команда уже была отправлена" +
              e.sms.map(sms => " (" + SMS.statusTranslation(sms.status) + ")").getOrElse("")
            , "notifications.error", Some(uid)))
        case e: Throwable => warn("throwable in callback", e)
      }
    }
    else throw new UnsupportedOperationException("Неподдерживаемая команда или тип устройства");
  }

  private[this] def doSendBlockAsUserUnchecked(uid: String, username: String, block: Boolean) {
    if (phonesCache(uid).isEmpty) throw new ImpossibleActionException("Не указан номер телефона");
    val phone: String = phonesCache(uid).get
    val terminal: Option[Imports.DBObject] = or.getMainTerminal(uid)
    val terminalMark = terminal.flatMap(terminal => {
      val strings = terminal.getAs[String]("eqMark").toList ++ terminal.getAs[String]("eqModel").toList
      if (strings.isEmpty) None else Some(strings.mkString(" "))
    })

    def sendAsSms() = {
      val digioutCommands: Option[(String, String, Boolean) => Seq[DigioutCommand]] = terminalMark.collect({
        case s if s.contains("Телтоника") =>
          (login: String, passw: String, block: Boolean) =>
            Seq(new fmb920DigioutCommand(username, login, passw, block))
        case s if s.contains("FMB") =>
          (login: String, passw: String, block: Boolean) =>
            Seq(new FMBDigioutCommand(username, login, passw, block))
        case s if s.contains("Galileo2") =>
          (_: String, _: String, block: Boolean) =>
            Seq(new GalileoDigioutCommand(username, 2, block))
        case s if s.contains("Galileo") =>
          (_: String, _: String, block: Boolean) =>
            Seq(new GalileoDigioutCommand(username, 1, block))
        case s if s.contains("Teltonika") =>
          (login: String, passw: String, block: Boolean) =>
            Seq(new TeltonikaDigioutCommand(username, login, passw, block))
        case s if s.contains("Ruptela") =>
          (login: String, passw: String, block: Boolean) =>
            Seq(new RuptelaDigioutCommand(username, login, passw, block))
        case s if s.contains("ГлонассSoft") =>
          (login: String, passw: String, block: Boolean) =>
            Seq(new umkaDigioutCommand(username, login, passw, block))
        case s if s.contains("Arnavi") =>
          (login: String, passw: String, block: Boolean) =>
            Seq(
              new ArnaviDigioutCommand(username, 1, block),
              new ArnaviDigioutCommand(username, 2, block)
            )
        case s if s.toLowerCase().contains("сигнал") =>
          (login: String, passw: String, block: Boolean) =>
            Seq(
              //new SignalDigioutCommand(username, login, passw, block, 1, silent = true),
              new SignalDigioutCommand(username, login, passw, block, 2)
            )
      })
      val login = terminal.flatMap(_.getAs[String]("eqLogin")).getOrElse("")
      val passw = terminal.flatMap(_.getAs[String]("eqPass")).getOrElse("")

      smsCommandThread.future({
        debug(s"sending commands start for '$phone' and terminalMark $terminalMark")
        val commands = digioutCommands.map(_ (login, passw, block))
          .getOrElse(Seq(new TeltonikaDigioutCommand(username, login, passw, block)))
        debug(s"sending commands: $commands for '$phone' and terminalMark $terminalMark")
        smsCommandProcessor.sendCommand(phone, commands)
        chargeCurrentUserForSms(username, phone, if (block) "Блокировка" else "Разблокировка")
      }).onFailure {
        case e: AlreadySentException =>
          es.publish(new UserMessage(
            username,
            "Команда уже была отправлена" +
              e.sms.map(sms => " (" + SMS.statusTranslation(sms.status) + ")").getOrElse("")
            , "notifications.error", Some(uid)))
        case e: Throwable => warn("throwable in callback", e)
      }
    }

    terminalMark match {
      case Some(s) if gprsCapable(s, terminal.get.as[String]("eqIMEI")) =>
        gprsCommandProcessor.sendCommand(uid: String, terminal: Option[Imports.DBObject], username: String, block: Boolean).onComplete {
          case Success(d) => notifyUsers(d)
          case Failure(t) =>
            warn(s"gprs command sending for $uid failed with", t)
            sendAsSms()
        }
      case _ => sendAsSms()
    }
  }


  private def notifyUsers(d: DeviceCommand): Unit = {
    val blocked = d.meta.as[Boolean]("blocking")
    val message = if (blocked)
      "objectscommander.objectblocked"
    else
      "objectscommander.objectunblocked"

    val uid = or.getObjectByIMEI(d.target).as[String]("uid")
    val usersLogins = permissionsChecker.getUsersWithPermissions(uid, PermissionValue.VIEW)
    for (login <- usersLogins) {
      val userMess = new UserMessage(
        login,
        message,
        "objectscommander.objectblocking",
        Some(uid),
        None,
        System.currentTimeMillis(),
        Map("readStatus" -> false, "blocked" -> blocked, "tr" -> true) //++ position
      )
      debug("publishing for " + login + ": " + userMess)
      es.publish(userMess)

    }

  }

  private[web] def getCurrentUserDbo(user: String = "") = {
    val userName = utils.web.getUserName.getOrElse(user)
    mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> userName))
      .getOrElse(throw new IllegalAccessException("unknown User: " + userName))
  }

  private[this] def chargeCurrentUserForSms(user: String, phone: String, comment: String): Unit = {
    val accountId =
      mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> user)).flatMap(_.getAs[ObjectId]("mainAccId"))
        .getOrElse(throw new IllegalArgumentException("no account for user:" + user))

    val tariff = tariffs.getTariffForAccount(accountId)
    debug("tariff=" + tariff)
    val servicePrices = tariff.toIterable.flatMap(_.getAsOrElse[MongoDBList]("servicePrice", MongoDBList.empty)
      .map({ case dbo: DBObject => (dbo.as[String]("name"), (dbo.as[String]("cost").toDouble * 100).toLong) })).toMap

    val smsPrice = servicePrices.get("Уведомление по SMS")

    if (smsPrice.isEmpty)
      warn("smsPrice for user:" + user + " is not defined")

    val finalPrice = -Math.abs(smsPrice.getOrElse(-400L))
    if (finalPrice != 0)
      feeProcessor.changeBalance(accountId, finalPrice, "Команда по SMS",
        comment,
        Map("phone" -> phone, "user" -> user))
  }


  private[web] val phonesCache = CacheBuilder.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(
      new CacheLoader[String, Option[String]]() {
        def load(uid: String): Option[String] = getObjectPhone(uid: String)
      })


  private[this] def getObjectPhone(uid: String): Option[String] = {
    lazy val oldPhone = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("phone" -> 1)).
      getOrElse(throw new IllegalArgumentException(s"object with uid=$uid can not be found"))
      .getAs[String]("phone").map(_.stripPrefix("+"))

    or.getMainTerminal(uid).flatMap(_.getAs[String]("simNumber").filter(s => s != "")).orElse(oldPhone).map(_.stripPrefix("+"))
  }


  private[this] def getPhoneObject(phone: String): String = {
    mdbm.getDatabase()("equipments").findOne(anyplus("simNumber" -> phone), MongoDBObject("uid" -> 1))
      .map(_.as[String]("uid")).get
  }

}

class TeltonikaSMSCommand(val user: String, val login: String, val password: String, command: String)
  extends TrackerSMSCommand {
  val text = login + " " + password + " " + command
}

class fmb920SMSCommand(val user: String, val login: String, val password: String, command: String)
  extends TrackerSMSCommand {
  val text = login + " " + password + " " + command
}

class RuptelaSMSCommand(val user: String, val login: String, val password: String, command: String)
  extends TrackerSMSCommand {
  val text = password + " " + command
}

class UMKaSMSCommand(val user: String, val login: String, val password: String, command: String)
  extends TrackerSMSCommand {
  val text = password + " " + command
}

class ArnaviSMSCommand(val user: String, val login: String, val password: String, command: String)
  extends TrackerSMSCommand {
  val text = password + " " + command
}

trait TrackerSMSCommand extends SMSCommand {
  def user: String

  def silent = false

  var result: Option[String] = None

  def acceptResponse(s: SMS) = {
    if (s.text.isEmpty)
      false
    else {
      result = Some(s.text)
      true
    }
  }
}

trait DigioutCommand extends SMSCommand {
  def blocked: Option[Boolean]

  def user: String

  def silent = false
}

@SerialVersionUID(8898161069505623780L)
class TeltonikaDigioutCommand(val user: String, val login: String, val password: String, block: Boolean) extends DigioutCommand {

  val paramValue = if (block) "11" else "00"

  val text = login + " " + password + " setdigout " + paramValue

  var blocked: Option[Boolean] = None

  private[this] lazy val regex = """Digital Outputs are set to: (\d+).*""".r

  def acceptResponse(s: SMS) = {
    s.text match {
      case regex(value) => {
        value match {
          case "11" => blocked = Some(true)
          case "00" => blocked = Some(false)
        }

        if (value != paramValue) {
          SmsConversation.logger.warn("Incorrect Param:" + value)
          false
        }
        else {
          true
        }
      }
      case _ => false
    }

  }
}

class fmb920DigioutCommand(val user: String, val login: String, val password: String, block: Boolean) extends DigioutCommand {

  val paramValue = if (block) "1" else "0"

  val text = login + " " + password + " setdigout " + paramValue

  var blocked: Option[Boolean] = None

  private[this] lazy val regex = """DOUT1:(\d+) Timeout:INFINITY(:? DOUT2:(\d+) Timeout:INFINITY)?""".r

  def acceptResponse(s: SMS) = {
    s.text.trim match {
      case regex(v1, _, v2) => {
        val value = v1 + Option(v2).getOrElse(v1)
        value match {
          case "1" => blocked = Some(true)
          case "0" => blocked = Some(false)
        }

        if (value != paramValue) {
          SmsConversation.logger.warn("Incorrect Param:" + value)
          false
        }
        else {
          true
        }
      }
      case _ => false
    }

  }
}

class ArnaviDigioutCommand(val user: String, m: Int, block: Boolean) extends DigioutCommand {

  val paramValue = if (block) "1" else "0"

  val text: String = m match {
    case 1 => "123456*SERV*10." + paramValue
    case 2 => "123456*SERV*9." + paramValue
  }
  var blocked: Option[Boolean] = None

  def acceptResponse(s: SMS): Boolean = {
    s.text.trim match {
      case "SERV OK" => blocked = Some(block); true
      case _ => false
    }
  }
}

@SerialVersionUID(1L)
class FMBDigioutCommand(val user: String, val login: String, val password: String, block: Boolean) extends DigioutCommand {

  val paramValue = if (block) "11" else "00"

  val text = login + " " + password + " setdigout " + paramValue

  var blocked: Option[Boolean] = None

  private[this] lazy val regex = """DOUT1:(\d+) Timeout:INFINITY(:? DOUT2:(\d+) Timeout:INFINITY)?""".r

  def acceptResponse(s: SMS) = {
    s.text.trim match {
      case regex(v1, _, v2) => {
        val value = v1 + Option(v2).getOrElse(v1)
        value match {
          case "11" => blocked = Some(true)
          case "00" => blocked = Some(false)
        }

        if (value != paramValue) {
          SmsConversation.logger.warn("Incorrect Param:" + value)
          false
        }
        else {
          true
        }
      }
      case _ => false
    }

  }
}

@SerialVersionUID(1L)
class GalileoDigioutCommand(val user: String, m: Int, block: Boolean) extends DigioutCommand {

  val text: String = m match {
    case 1 => "Out 0," + (if (block) "0" else "1")
    case 2 => "Out 1," + (if (block) "1" else "0")
  }

  var blocked: Option[Boolean] = None

  def acceptResponse(s: SMS): Boolean = {
    s.text.trim match {
      case "OUT(3..0) = 1100" | "OUT(3..0) = 1111" => blocked = Some(true); true
      case "OUT(3..0) = 1101" => blocked = Some(false); true
      case _ => false
    }
  }

}

@SerialVersionUID(8898161069505623781L)
class RuptelaDigioutCommand(val user: String, val login: String, val password: String, block: Boolean) extends DigioutCommand {

  val paramValue = if (block) "0,0" else "1,1"

  val text = password + " setio " + paramValue

  var blocked: Option[Boolean] = None

  private[this] lazy val regex = """SETIO configuration data ok""".r

  def acceptResponse(s: SMS) = {
    s.text match {
      case regex() => {
        blocked = Some(block)
        true
      }
      case _ => false
    }

  }
}

class umkaDigioutCommand(val user: String, val login: String, val password: String, block: Boolean) extends DigioutCommand {

  val paramValue = if (block) "1" else "0"

  val text = " OUTPUT0 " + paramValue

  var blocked: Option[Boolean] = None

  private[this] lazy val regex = """OUTPUT0 configuration data ok""".r

  def acceptResponse(s: SMS) = {
    s.text match {
      case regex() => {
        blocked = Some(block)
        true
      }
      case _ => false
    }

  }
}

@SerialVersionUID(1L)
class SignalDigioutCommand(val user: String, val login: String, val password: String,
                           block: Boolean, outIndex: Int,
                           override val silent: Boolean = false
                          ) extends DigioutCommand {

  val paramValue = if (block) "Y" else "N"

  val text = password + " " + outIndex + paramValue

  var blocked: Option[Boolean] = None

  def acceptResponse(s: SMS) = {
    //if (s.text.contains("C_O" + outIndex + "_" + paramValue)) {
    if (s.text.contains("O:N" + paramValue)) {
      blocked = Some(block)
      true
    }
    else
      false
  }
}


class MockSmsGate extends SmsGate with grizzled.slf4j.Logging {

  @Autowired
  var es: EventsStore = null

  @BeanProperty
  var responseInterval = 3000

  val teltonicaRegex = """(\S*) *(\S*) setdigout (\d+)""".r

  val teltonicaGetCoordRegex = """(\S*) *(\S*) getgps""".r

  val teltonicaRestartTerminalRegex = """(\S*) *(\S*) cpureset""".r

  val ruptelaRegex = """(\S*) setio (\d+),(\d+)""".r

  val ruptelaGetCoordRegex = """(\S*) coords""".r

  val ruptelaRestartTerminalRegex = """(\S*) reset""".r

  val signalRegex = """(\S*) (\d)([Y|N])""".r

  private val timer: Timer = new Timer();

  private val smsidgen = new AtomicLong(1);

  def sendSms(phone: String, text: String) = {
    val smsid = smsidgen.getAndIncrement;
    debug("sending sms " + (smsid, text, phone))
    val ressms = new SMS(smsid, text, phone)
    ressms.status = SMS.PENDING
    println("sendSms text=" + text)
    text match {
      case teltonicaRegex(l, p, v) => {
        timer.schedule(new TimerTask {
          def run() = try {
            es.publish(new SMSDeliveredEvent(phone, smsid, new Date()));
            val smsText = if (phone == "FMB-phone")
              s"DOUT1:${v(0)} Timeout:INFINITY DOUT2:${v(1)} Timeout:INFINITY "
            else
              "Digital Outputs are set to: " + v
            emulateReceivingSms(phone, smsText)
          } catch {
            case e: Throwable => error("timertask error", e)
          } finally {
            onTaskCompleted()
          }
        }, responseInterval)
        onTaskStarted()
        debug("teltonica response scheduled in " + responseInterval)
      }
      case ruptelaRegex(l, d1, d2) => {
        timer.schedule(new TimerTask {
          def run() = try {
            es.publish(new SMSDeliveredEvent(phone, smsid, new Date()));
            emulateReceivingSms(phone, "SETIO configuration data ok")
          } catch {
            case e: Throwable => error("timertask error", e)
          } finally {
            onTaskCompleted()
          }
        }, responseInterval)
        onTaskStarted()
        debug("ruptela response scheduled in " + responseInterval)
      }
      case m@signalRegex(p, i, yn) => {
        timer.schedule(new TimerTask {
          def run() = try {
            debug("device emulator recieved an sms:" + m)
            es.publish(new SMSDeliveredEvent(phone, smsid, new Date()));
            emulateReceivingSms(phone, s"M:111 O:N$yn 13.44.35 25/11/14 G:0 I:NNNNNNNN O:NNNN AK:10.5 3.8 T:N A:0.0 0.0 13.44.35 25/11/14 N055 44.8283 E037 46.1595 000 027 000065BB")
          } catch {
            case e: Throwable => error("timertask error", e)
          } finally {
            onTaskCompleted()
          }
        }, responseInterval)
        onTaskStarted()
        debug("signal response scheduled in " + responseInterval)
      }
      case teltonicaGetCoordRegex(lo, pa) => //debug("no response scheduled for " + text)
        timer.schedule(new TimerTask {
          def run() = try {
            debug("device emulator recieved an sms:")
            es.publish(new SMSDeliveredEvent(phone, smsid, new Date()));
            emulateReceivingSms(phone, "GPS:1 Sat:9 Lat:55.708740 Long:37.797497 Alt:178 Speed:0 Dir:117 Date: 2015/5/19 Time: 15:22:35")
          } catch {
            case e: Throwable => error("timertask error", e)
          } finally {
            onTaskCompleted()
          }
        }, responseInterval)
        onTaskStarted()
        debug("command response scheduled in " + responseInterval)
      case teltonicaRestartTerminalRegex(lo, pa) => //debug("no response scheduled for " + text)
        timer.schedule(new TimerTask {
          def run() = try {
            debug("device emulator recieved an sms:")
            es.publish(new SMSDeliveredEvent(phone, smsid, new Date()));
            emulateReceivingSms(phone, "Teltonica cpureset command completed")
          } catch {
            case e: Throwable => error("timertask error", e)
          } finally {
            onTaskCompleted()
          }
        }, responseInterval)
        onTaskStarted()
        debug("command response scheduled in " + responseInterval)
      case ruptelaGetCoordRegex(lo) => //debug("no response scheduled for " + text)
        timer.schedule(new TimerTask {
          def run() = try {
            debug("device emulator recieved an sms:")
            es.publish(new SMSDeliveredEvent(phone, smsid, new Date()));
            emulateReceivingSms(phone, "2015-05-19 15:11, lat. 55.5661516, long. 36.7442533, alt. 216.4, sat. 8, dir. 274.20, hdop 90, state 3")
          } catch {
            case e: Throwable => error("timertask error", e)
          } finally {
            onTaskCompleted()
          }
        }, responseInterval)
        onTaskStarted()
        debug("command response scheduled in " + responseInterval)
      case ruptelaRestartTerminalRegex(lo) => //debug("no response scheduled for " + text)
        timer.schedule(new TimerTask {
          def run() = try {
            debug("device emulator recieved an sms:")
            es.publish(new SMSDeliveredEvent(phone, smsid, new Date()));
            emulateReceivingSms(phone, "Ruptela reset command completed")
          } catch {
            case e: Throwable => error("timertask error", e)
          } finally {
            onTaskCompleted()
          }
        }, responseInterval)
        onTaskStarted()
        debug("command response scheduled in " + responseInterval)
      case _ => debug("no response scheduled for " + text)
    }
    ressms
  }


  private val _scheduled = new AtomicInteger(0)

  def scheduled = _scheduled.get()

  private var completerionPromise = Promise.successful[Unit]()

  private def onTaskStarted() {
    debug("onTaskStarted")
    if (_scheduled.incrementAndGet() == 1) synchronized {
      completerionPromise = Promise.apply()
    }
  }

  private def onTaskCompleted() = {
    debug("onTaskCompleted")
    if (_scheduled.decrementAndGet() == 0) synchronized {
      completerionPromise.success()
    }
  }

  def allTasksCompleted = synchronized {
    completerionPromise.future
  }

  def awaitCompletion() = Await.result(allTasksCompleted, (responseInterval * 20).millis)

  def emulateReceivingSms(phone: String, text: String) {
    val date = new Date()
    es.publish(new SMSEvent(new SMS(smsidgen.getAndIncrement, text, true, phone, "", date, date, SMS.DELIVERED)))
  }

  def setSmsListener(l: SmsListener) = ???

  override def getSmsListener: SmsListener = null
}

