package ru.sosgps.wayrecall.data.sleepers

import ru.sosgps.wayrecall.core.{Translations, MongoDBManager}
import ru.sosgps.wayrecall.utils.web.extjsstore.{StoreAPI, EDSStoreServiceDescriptor}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import org.bson.types.ObjectId
import ch.ralscha.extdirectspring.bean.{ExtDirectStoreResult, ExtDirectResponseBuilder, SortDirection, ExtDirectStoreReadRequest}
import collection.mutable
import collection.JavaConversions.collectionAsScalaIterable
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import com.mongodb.casbah.Imports._
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._

import scala.Predef._
import ru.sosgps.wayrecall.utils.ExtDirectService
import com.mongodb.casbah.Imports._

@ExtDirectService
class SleeperMessages extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model(
    "_id",
    "smsId",
    "senderPhone",
    "sendDate",
    "text",
    "battery",
    "lonlat",
    "posData",
    "alarm"
  )
  val name = "SleeperMessages"
  //override val storeType: String = "Ext.data.TreeStore"
  override val autoSync = false
  val idProperty = "_id"
  val lazyLoad = false

  @Autowired
  var sleepers: SleepersDataStore = null

  @Autowired
  var trans : Translations = null

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def read(request: ExtDirectStoreReadRequest): ExtDirectStoreResult[Map[String, AnyRef]] = {
    debug("SleepMessages read " + request)

    val params = request.getParams
    val uid = params.get("uid").asInstanceOf[String]
    if (uid != null) {
      val auth = SecurityContextHolder.getContext().getAuthentication().getAuthorities()

      val sleepersData: Iterator[Map[String, AnyRef]] = for (
        sleepersData <- sleepers.getLatestData(uid).toIterator;
        (phone, history) <- sleepersData.history;
        sms <- history;
        pr <- SleeperParser.parseReport(sms.text).toOption
      ) yield {

        Map(
          "smsId" -> sms.smsId.toString,
          "senderPhone" -> sms.senderPhone,
          "text" -> (if (auth.exists(_.getAuthority == "ROLE_ADMIN")) sms.text else ""),
          "sendDate" -> sms.sendDate,
          "battery" -> Option(Seq(pr.batValue, pr.batPercentage).flatten).filter(_.nonEmpty).map(_.mkString("-")),
          "lonlat" -> Some(pr).filter(_.hasPositionData).map(posDataString).getOrElse(""),
          "posData" -> Some(pr).filter(_.hasPositionData).map(posData).getOrElse(""),
          "alarm" -> pr.alarm
        )
      }
      EDSJsonResponse(sleepersData)
    } else {
      EDSJsonResponse[Map[String, AnyRef]](Iterable.empty)
    }
  }


  private[this] def posDataString(mr: MatchResult): String = {
    val l = trans.getRequestFromSpringOrDefault

    if (mr.lonlat.isDefined)
      l.tr("sleeper.position.bycoords")
    else if (mr.positionURL.isDefined)
      l.tr("sleeper.position.byref")
    else if (mr.lbs.isDefined)
      l.tr("sleeper.position.bylbs")
    else
      ""
  }
  
  private[this] def posData(mr: MatchResult): String = {
    if (mr.lonlat.isDefined)
      mr.lonlat.toString
    else if (mr.positionURL.isDefined)
      mr.positionURL.toString
    else if (mr.lbs.isDefined)
      "LBS"
    else
      ""
  }
}
