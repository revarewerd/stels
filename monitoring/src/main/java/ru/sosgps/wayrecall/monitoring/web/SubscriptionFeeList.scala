package ru.sosgps.wayrecall.monitoring.web

import ru.sosgps.wayrecall.core.{EquipmentDayPriceData, MongoDBManager, MonthlyPaymentStorage, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.utils.web.extjsstore.{EDSStoreServiceDescriptor, StoreAPI}
import org.springframework.beans.factory.annotation.Autowired
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.{ExtDirectResponseBuilder, ExtDirectStoreReadRequest, SortDirection}
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._

import scala.Predef._
import ru.sosgps.wayrecall.utils.{ExtDirectService, ISODateTime, Memo}
import javax.annotation.PostConstruct

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.Imports
import org.joda.time.DateTime
import java.util.Date

import scala.collection.immutable
import org.springframework.stereotype.Component
import org.springframework.security.core.context.SecurityContextHolder
import ru.sosgps.wayrecall.domain.balance.BalanceHistoryService

// case class AggregatedDailyPay(objectName: String, eqCount: Int, fullFee: Double, firstDate: Date, lastDate: Date, equipments: List[DailyPayByEquipment])
@ExtDirectService
class SubscriptionFeeList extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("objectName", "eqCount", "firstDate", "lastDate", "fullFee", "equipments")

  val name = "SubscriptionFeeList"
  val idProperty = "uid"

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var bhs: BalanceHistoryService = null

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {
    debug("SubscriptionFeeList read " + request)

    val userName = SecurityContextHolder.getContext().getAuthentication().getName()
    val userInfo = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> userName)).get
    val showFeeDetails = userInfo.getAsOrElse[Boolean]("showfeedetails", false)
    
    val params = request.getParams
    val accId = userInfo.get("mainAccId").asInstanceOf[ObjectId]

    val monthShift = Option(params.get("month")).map(_.asInstanceOf[Integer].toInt).getOrElse(-1)
    val (from,to) = bhs.dateIntervalForMonthShift(monthShift)

    if (showFeeDetails == true) {
      EDSJsonResponse(bhs.getAggregatedDailyPay(accId, from, to))
    } else {
      EDSJsonResponse(Iterable(Map()))
    }
  }

}
