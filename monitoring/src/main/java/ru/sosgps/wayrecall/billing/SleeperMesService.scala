package ru.sosgps.wayrecall.billing


import java.util.Date

import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, Translations, MongoDBManager}
import javax.annotation.{PostConstruct, Resource}
import ru.sosgps.wayrecall.data.RemovalIntervalsManager
import ru.sosgps.wayrecall.sms.{SMS, SmsGate}
import com.mongodb.casbah.Imports._
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.data.sleepers.{SleepersDataStore, MatchResult, SleeperParser}


/**
 * Created by IVAN on 29.05.2014.
 */
@ExtDirectService
class SleeperMesService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id", "smsId", "text", "senderPhone", "sendDate", "targetPhone", "status","lonlat" /*,"fromObject","deliveryDate","user"*/)

  val name = "SleeperMesService"
  val idProperty = "_id"

  val lazyLoad = false

  override val autoSync = false

  @Autowired
  var mdbm: MongoDBManager = null
  @Resource(name = "m2mSmsGate")
  var smsgate: SmsGate = null

  @Autowired
  var sleepersDataStore: SleepersDataStore = null

  @Autowired
  var intervals: RemovalIntervalsManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null


  val dbName = "smses"

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
    debug("SleepMesService read " + request)
    val params = request.getParams
    val paramPhone = params.get("phone").asInstanceOf[String]
    val uid = or.getUidByPhone(paramPhone) // Мб null будет, хз

    val phone = paramPhone.stripPrefix("+")


    if (phone != null && !phone.isEmpty) {
      val sleeperData =  intervals.getFilteredIntervals(uid).flatMap{ case (begin,end) =>
        getCollection.find(
          ("sendDate" $gte begin $lte end) ++
        $or("senderPhone" -> phone,
          "targetPhone" -> phone)
//          $or(MongoDBObject("senderPhone" -> phone, "uid" -> sleepersDataStore.uidBySleeperPhone(phone) ),
//          MongoDBObject("targetPhone" -> phone))
      ).sort(MongoDBObject("date" -> 1)).map(dbo => {
        val stname = dbo.getAsOrElse[String]("status", "Unknown")
        val pr = SleeperParser.parseReport(dbo.get("text").asInstanceOf[String])
        dbo.put("status", SMS.statusTranslation(stname));
        dbo.put("smsId", dbo.get("smsId").toString);
        dbo.put("lonlat", pr.filter(_.hasPositionData).map(posDataString).getOrElse(""));
        dbo
      })}
      EDSJsonResponse(sleeperData)
    }
    else
      EDSJsonResponse(Map.empty)
  }
  private[this] def posDataString(mr: MatchResult): String = {
    if (mr.lonlat.isDefined)
      "По координатам"
    else if (mr.positionURL.isDefined)
      "По ссылке"
    else if (mr.lbs.isDefined)
      "По LBS"
    else
      ""
  }
}