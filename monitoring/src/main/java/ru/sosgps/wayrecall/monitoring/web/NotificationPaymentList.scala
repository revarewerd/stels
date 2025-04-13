package ru.sosgps.wayrecall.monitoring.web

import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.domain.balance.NotificationRecord
import ru.sosgps.wayrecall.utils.{ExtDirectService, ScalaConverters}
import ru.sosgps.wayrecall.utils.web.{EDSJsonResponse, ScalaCollectionJson}
import ru.sosgps.wayrecall.utils.web.extjsstore.{EDSStoreServiceDescriptor, StoreAPI}
import ru.sosgps.wayrecall.domain.balance.BalanceHistoryService


/**
  * Created by ivan on 04.08.16.
  */
@ExtDirectService
class NotificationPaymentList extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  // (user: String, phone: String, fee: Double, time: Date, comment: String)
  val model = Model("num", "user", "phone", "fee", "time", "comment")

  override val idProperty: String = "num"

  val name = "notificationPaymentList"
  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var balanceHistory: BalanceHistoryService = null

  case class NotificationRecordWithNum(num: Int,user: String, phone: String, fee: Double, time: Date, comment: String)

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def read(request: ExtDirectStoreReadRequest) = {
    val userName = SecurityContextHolder.getContext().getAuthentication().getName().toString
    val userInfo = mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> userName)).get
    val showFeeDetails = userInfo.getAsOrElse[Boolean]("showfeedetails", false)

    val params = request.getParams
    val accId = userInfo.get("mainAccId").asInstanceOf[ObjectId]

    val monthShift = Option(params.get("month")).map(_.asInstanceOf[Integer].toInt).getOrElse(-1)
    val (from,to) = balanceHistory.dateIntervalForMonthShift(monthShift)

    val notifications = balanceHistory.getSMSNotifications(accId,from,to)

    if (showFeeDetails) {
      notifications.zipWithIndex.map{case (nr, n) => NotificationRecordWithNum(n, nr.user, nr.phone, nr.fee, nr.time, nr.comment )}
    }
    else
      EDSJsonResponse(Iterable(Map()))
  }

}

