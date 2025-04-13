package ru.sosgps.wayrecall.sms

import scala.beans.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.events.{PredefinedEvents, EventsStore}
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import com.google.common.cache.{CacheBuilder, LoadingCache}
import java.util.concurrent.TimeUnit
import ru.sosgps.wayrecall.utils.{Memo, funcLoadingCache, funcToTimerTask}
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, MongoDBManager}
import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import ru.sosgps.wayrecall.utils.nonPlusPhone

/**
 * Created by nickl on 07.04.14.
 */
class DemoSmsGate extends SmsGate with grizzled.slf4j.Logging {

  @Autowired
  protected var mdbm: MongoDBManager = null;

  @Autowired
  protected var or: ObjectsRepositoryReader = null;

  @Autowired
  @BeanProperty
  var es: EventsStore = null

  @BeanProperty
  var wrappedGate: SmsGate = null

  @BeanProperty
  var mockSmsGate: SmsGate = null

  private case class ConfigData(mockTerminalPhones: Seq[String], phoneReverseMap: Map[String, String])

  private[this] val configData: LoadingCache[AnyRef, ConfigData] = CacheBuilder.newBuilder()
    .expireAfterWrite(24, TimeUnit.HOURS)
    .buildWithFunction[AnyRef, ConfigData]((AnyRef) => {

    val uids = mdbm.getDatabase()("objects")
      .find(MongoDBObject("demoTarget" -> MongoDBObject("$exists" -> true))).map(_.as[String]("uid")).toSet

    val phoneReverse = HashMap.newBuilder[String, String]

    val mockPhones = new ListBuffer[String]

    mdbm.getDatabase()("equipments").find("uid" $in uids).foreach(eqDbo => {
      eqDbo.getAs[String]("simNumber").foreach(sn => {
        mockPhones += nonPlusPhone(sn)
        eqDbo.getAs[String]("demoPhoneTarget").foreach(dp =>
          phoneReverse += ((nonPlusPhone(dp), nonPlusPhone(sn)))
        )
      })
    })
    val r = ConfigData(mockPhones.result(), phoneReverse.result())
    info("ConfigData:" + r)
    r
  })

  //val mockTerminalPhones = Seq("0000000000")
  //val sleepersRetranslatorPhones = Seq("4108305")


  @PostConstruct
  def init() {
    require(es != null, "EventsStore must be set")
    require(wrappedGate != null, "wrappedGate must be set")
    require(mockSmsGate != null, "mockSmsGate must be set")
    es.subscribe(PredefinedEvents.sms, processSms)
  }

  private def processSms(smsEvent: SMSEvent) {
    val sms = smsEvent.sms
    debug("processSms:" + sms)
    configData(AnyRef).phoneReverseMap.get(sms.senderPhone).foreach(rphone => {
      debug("resending:" + sms)
      val remappedSms = new SMS(-1 * sms.smsId, sms.text, sms.fromObject, rphone, sms.targetPhone, sms.sendDate, sms.deliveredDate, sms.status)
      Option(getSmsListener) match {
        case Some(sl) => sl.onSmsReceived(Seq(remappedSms))
        case None => warn("no sms listener to process sms: " + sms)
      }
    })
  }


  override def setSmsListener(l: SmsListener): Unit = {
    wrappedGate.setSmsListener(l)
  }

  override def sendSms(phone: String, text: String): SMS = {
    debug("processing: " + phone)
    if (configData(AnyRef).mockTerminalPhones.exists(phone.contains)) {
      debug("send to mock " + phone)
      mockSmsGate.sendSms(phone, text)
    }
    else {
      debug("send to wrapped " + phone)
      wrappedGate.sendSms(phone, text)
    }

  }

  override def getSmsListener: SmsListener = wrappedGate.getSmsListener
}
