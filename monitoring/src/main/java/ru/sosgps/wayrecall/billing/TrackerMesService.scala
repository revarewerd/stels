package ru.sosgps.wayrecall.billing

import java.util.Date

import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, UserRolesChecker, MongoDBManager}
import ru.sosgps.wayrecall.data.RemovalIntervalsManager
import ru.sosgps.wayrecall.utils.web.extjsstore.{StoreAPI, EDSStoreServiceDescriptor}
import org.springframework.beans.factory.annotation.Autowired
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import org.bson.types.ObjectId
import ch.ralscha.extdirectspring.bean.{ExtDirectResponseBuilder, SortDirection, ExtDirectStoreReadRequest}
import collection.mutable
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import com.mongodb.casbah.Imports._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._

import scala.Predef._
import ru.sosgps.wayrecall.sms.SMS
import ru.sosgps.wayrecall.sms.SmsGate
import ru.sosgps.wayrecall.utils.ExtDirectService
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import javax.annotation.Resource

@ExtDirectService
class TrackerMesService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id", "smsId", "text", "senderPhone", "sendDate", "targetPhone", "status" /*,"fromObject","deliveryDate","user"*/)
  //val updatebleFields = Set("type","mark", "model", "server","port")

  val name = "TrackerMesService"
  val idProperty = "_id"

  val lazyLoad = false

  override val autoSync = false

  @Autowired
  var mdbm: MongoDBManager = null
  @Resource(name = "m2mSmsGate")
  var smsgate: SmsGate = null

  val dbName = "smses"

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var intervals: RemovalIntervalsManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  def getCollection = mdbm.getDatabase().apply(dbName)

  @PostConstruct
  def init() {
    val collection = getCollection
    info("ensuring index MongoDBObject(\"targetPhone\" -> 1)")
    info("ensuring index MongoDBObject(\"senderPhone\" -> 1)")
    collection.ensureIndex(MongoDBObject("targetPhone" -> 1))
    collection.ensureIndex(MongoDBObject("senderPhone" -> 1))
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def read(request: ExtDirectStoreReadRequest) = {
    debug("TrackerMesService read " + request)
    val params = request.getParams
    val paramPhone = params.get("phone").asInstanceOf[String]
    val uid = or.getUidByPhone(paramPhone) // Мб null будет, хз

    val phone = paramPhone.stripPrefix("+")


    if (phone != null && !phone.isEmpty) {
      val trackerData = intervals.getFilteredIntervals(uid).flatMap{ case (begin,end) =>
      getCollection.find(
        ("sendDate" $gte begin $lte end) ++
        $or("senderPhone" -> phone,
          "targetPhone" -> phone)

      ).sort(MongoDBObject("sendDate" -> -1)).map(dbo => { // Поля date нет
        val stname = dbo.getAsOrElse[String]("status", "Unknown")
        dbo.put("status", SMS.statusTranslation(stname));
        dbo
      })}
      EDSJsonResponse(trackerData)
    }
    else
      EDSJsonResponse(Map.empty)
  }

  @ExtDirectMethod
  def attachToWRC(phone: String)/*: Map[String, Any] =*/ {
    if(!roleChecker.checkAdminAuthority()) throw new RuntimeException("Недостаточно прав на перепрошивку на WRC")
    val device=mdbm.getDatabase().apply("equipments").findOne(MongoDBObject("simNumber" -> phone)).getOrElse(throw new IllegalArgumentException("phone "+ phone+" was not found"))
    val login=device.getAsOrElse[String]("eqLogin"," ")
    val pass=device.getAsOrElse[String]("eqPass"," ")
    val correctPhone = phone.stripPrefix("+")
    debug("Phone " + correctPhone )
    val sendSmses: Thread = new Thread(new Runnable()
    {
      def run()
      {
        smsgate.sendSms(correctPhone,login+" "+pass+" setparam 1242 m2m.msk")
        debug("Sms sended:"+correctPhone+" "+login+" "+pass+" setparam 1242 m2m.msk")
        Thread.sleep(10000)
        smsgate.sendSms(correctPhone,login+" "+pass+" setparam 1245 91.230.215.12")
        debug("Sms sended:"+correctPhone+" "+login +" "+pass+" setparam 1245 91.230.215.12")
        Thread.sleep(10000)
        smsgate.sendSms(correctPhone,login+" "+pass+" setparam 1246 9088")
        debug("Sms sended:"+correctPhone+" "+login+" "+pass+" setparam 1246 9088")
      }
    });
    sendSmses.start();
  }

  @ExtDirectMethod
  def ipAndPortToWRC(phone: String)/*: Map[String, Any] =*/ {
    if(!roleChecker.checkAdminAuthority()) throw new RuntimeException("Недостаточно прав на смену IP и порта на WRC")
    val device=mdbm.getDatabase().apply("equipments").findOne(MongoDBObject("simNumber" -> phone)).getOrElse(throw new IllegalArgumentException("phone "+ phone+" was not found"))
    val login=device.getAsOrElse[String]("eqLogin"," ")
    val pass=device.getAsOrElse[String]("eqPass"," ")
    val correctPhone = phone.stripPrefix("+")
    debug("Phone " + correctPhone )
    val sendSmses: Thread = new Thread(new Runnable()
    {
      def run()
      {
        smsgate.sendSms(correctPhone,login+" "+pass+" setparam 1245 91.230.215.12")
        debug("Sms sended:"+correctPhone+" "+login +" "+pass+" setparam 1245 91.230.215.12")
        Thread.sleep(10000)
        smsgate.sendSms(correctPhone,login+" "+pass+" setparam 1246 9088")
        debug("Sms sended:"+correctPhone+" "+login+" "+pass+" setparam 1246 9088")
      }
    });
    sendSmses.start();
  }

  @ExtDirectMethod
  def sendTeltonikaCMD(phone: String,command: String)/*: Map[String, Any] =*/ {
    if(!roleChecker.checkAdminAuthority()) throw new RuntimeException("Недостаточно прав на отправку команды оборудованию")
    val device=mdbm.getDatabase().apply("equipments").findOne(MongoDBObject("simNumber" -> phone)).getOrElse(throw new IllegalArgumentException("phone "+ phone+" was not found"))
    val login=device.getAsOrElse[String]("eqLogin"," ")
    val pass=device.getAsOrElse[String]("eqPass"," ")
    val correctPhone = phone.stripPrefix("+")
    debug("Phone " + correctPhone )
    debug("Sms sended:"+correctPhone+" "+login+" "+pass+" "+command)
    smsgate.sendSms(correctPhone,login+" "+pass+" "+command)
  }

  @ExtDirectMethod
  //добавлена команда для нового оборудования Телтоника
  def sendFMB920CMD(phone: String,command: String)/*: Map[String, Any] =*/ {
    if(!roleChecker.checkAdminAuthority()) throw new RuntimeException("Недостаточно прав на отправку команды оборудованию")
    val device=mdbm.getDatabase().apply("equipments").findOne(MongoDBObject("simNumber" -> phone)).getOrElse(throw new IllegalArgumentException("phone "+ phone+" was not found"))
    val login=device.getAsOrElse[String]("eqLogin"," ")
    val pass=device.getAsOrElse[String]("eqPass"," ")
    val correctPhone = phone.stripPrefix("+")
    debug("Phone " + correctPhone )
    debug("Sms sended:"+correctPhone+" "+login+" "+pass+" "+command)
    smsgate.sendSms(correctPhone,login+" "+pass+" "+command)
  }

  @ExtDirectMethod
  def sendRuptelaCMD(phone: String,command: String)/*: Map[String, Any] =*/ {
    if(!roleChecker.checkAdminAuthority()) throw new RuntimeException("Недостаточно прав на отправку команды оборудованию")
    val device=mdbm.getDatabase().apply("equipments").findOne(MongoDBObject("simNumber" -> phone)).getOrElse(throw new IllegalArgumentException("phone "+ phone+" was not found"))
    val pass=device.getAsOrElse[String]("eqPass"," ")
    val correctPhone = phone.stripPrefix("+")
    debug("Phone " + correctPhone )
    debug("Sms sended:"+correctPhone+" "+pass+" "+command)
    smsgate.sendSms(correctPhone,pass+" "+command)
  }

  @ExtDirectMethod
  def sendArnaviCMD(phone: String,command: String)/*: Map[String, Any] =*/ {
    if(!roleChecker.checkAdminAuthority()) throw new RuntimeException("Недостаточно прав на отправку команды оборудованию")
    val device=mdbm.getDatabase().apply("equipments").findOne(MongoDBObject("simNumber" -> phone)).getOrElse(throw new IllegalArgumentException("phone "+ phone+" was not found"))
    val pass=device.getAsOrElse[String]("eqPass"," ")
    val correctPhone = phone.stripPrefix("+")
    debug("Phone " + correctPhone )
    debug("Sms sended:"+correctPhone+" "+pass+" "+command)
    smsgate.sendSms(correctPhone,pass+" "+command)
  }

  @ExtDirectMethod
  def sendumkaCMD(phone: String,command: String)/*: Map[String, Any] =*/ {
    if(!roleChecker.checkAdminAuthority()) throw new RuntimeException("Недостаточно прав на отправку команды оборудованию")
    val device=mdbm.getDatabase().apply("equipments").findOne(MongoDBObject("simNumber" -> phone)).getOrElse(throw new IllegalArgumentException("phone "+ phone+" was not found"))
    val pass=device.getAsOrElse[String]("eqPass"," ")
    val correctPhone = phone.stripPrefix("+")
    debug("Phone " + correctPhone )
    debug("Sms sended:"+correctPhone+" "+pass+" "+command)
    smsgate.sendSms(correctPhone,command)
  }

  @ExtDirectMethod
  def sendSMSToTracker(phone: String, text: String): Map[String, Any] = {
    if(!roleChecker.checkAdminAuthority()) throw new RuntimeException("Недостаточно прав на отправку СМС оборудованию")
    val correctPhone = phone.stripPrefix("+")
    debug("Phone " + correctPhone + "text " + text)
    val sms = smsgate.sendSms(correctPhone, text)
    Map(
      "text" -> sms.text,
      "smsId" -> sms.smsId,
      "senderPhone" -> sms.senderPhone,
      "sendDate" -> sms.sendDate,
      "targetPhone" -> sms.targetPhone,
      "status" -> SMS.statusTranslation(sms.status)
    )
  }
}