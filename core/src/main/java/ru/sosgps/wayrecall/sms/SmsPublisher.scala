package ru.sosgps.wayrecall.sms

import java.util.Date
import org.springframework.beans.factory.annotation.Autowired
import scala.beans.BeanProperty
import ru.sosgps.wayrecall.events.EventsStore
import ru.sosgps.wayrecall.core.MongoDBManager
import com.mongodb.casbah.WriteConcern
import com.mongodb.casbah.Imports._
import javax.annotation.PostConstruct

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 02.11.13
 * Time: 15:37
 * To change this template use File | Settings | File Templates.
 */
class SmsPublisher extends SmsListener with grizzled.slf4j.Logging {

  @Autowired
  @BeanProperty
  var es: EventsStore = null

  @Autowired
  @BeanProperty
  var mdbm: MongoDBManager = null

  @PostConstruct
  def init() {
    info("ensuring index MongoDBObject(\"smsId\" -> 1)")
    mdbm.getDatabase()("smses").ensureIndex(MongoDBObject("smsId" -> 1), MongoDBObject("unique" -> true, "dropDups" -> true))
  }

  def onSmsReceived(received: Seq[SMS]) = {
    trace("received:" + received)
    for (sms <- received) {
      try {
        mdbm.getDatabase()("smses").insert(SMS.toMongoDbObject(sms), WriteConcern.Safe)
      }
      catch {
        case e: com.mongodb.MongoException.DuplicateKey => {}
        case e: Exception => warn(e.getMessage, e)
      }
      es.publish(new SMSEvent(sms))
    }
  }


  override def onSmsSent(sms: SMS): Unit = onSmsSentBatch(Seq(sms))

  def onSmsSentBatch(smses: Seq[SMS]) = {
    for (sms <- smses) {
      try {
        debug("writing sms sent:" + sms)
        mdbm.getDatabase()("smses").insert(SMS.toMongoDbObject(sms), WriteConcern.Safe)
      }
      catch {
        case e: com.mongodb.MongoException.DuplicateKey => {}
        case e: Exception => warn(e.getMessage, e)
      }
    }
  }

  def onSmsStatusChanged(smses: Seq[SMS]) = {
    for (sms <- smses) {
      try {

        debug("SMS status changed:" + sms)

        val isDelivered = sms.status == SMS.DELIVERED
        val updateFileds = Seq("status" -> sms.status) ++
          (if (isDelivered)
            Seq("deliveryDate" -> sms.deliveredDate)
          else
            Seq.empty)

        mdbm.getDatabase()("smses").update(
          MongoDBObject("smsId" -> sms.smsId),
          $set(updateFileds: _*)
        )
        if (isDelivered)
          es.publish(new SMSDeliveredEvent(sms.targetPhone, sms.smsId, sms.deliveredDate))
      }
      catch {
        case e: com.mongodb.MongoException.DuplicateKey => {}
        case e: Exception => warn(e.getMessage, e)
      }
    }
  }
}
