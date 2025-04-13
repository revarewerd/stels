package ru.sosgps.wayrecall.billing.dealer

import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.sosgps.wayrecall.core.{MongoDBManager, MonthlyPaymentStorage}
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

import scala.collection.immutable.TreeMap
import scala.collection.mutable


@ExtDirectService
class DealerMonthlyPaymentService extends EDSStoreServiceDescriptor
with DealersManagementMixin with grizzled.slf4j.Logging {
  val model = Model("objectName", "uid", "eqtype", "firstDate", "lastDate", "withdraw", "cost")

  val name = "DealerMonthlyPaymentService"
  val idProperty = "uid"

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var plans: DealersTariffPlans = null


  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def read(request: ExtDirectStoreReadRequest) = {
    debug("MonthlyPaymentService read " + request)

    val params = request.getParams
    val dealer = params.as[String]("dealer")
    val monthShift = Option(params.get("month")).map(_.asInstanceOf[Integer].toInt).getOrElse(-1)


    //val dealerdb = mmdbm.dbmanagers(dealer)

    val storage = new MonthlyPaymentStorage()

    val (fromDate, toDate) = storage.getDateInterval(monthShift)

    debug("dbname=" + mdbm.getDatabase().name)
    debug("dealer=" + dealer)

    val allBalanceHistory = mdbm.getDatabase().apply("dealers.balanceHistory").find(
      MongoDBObject("dealer" -> dealer, "type" -> "dailypay") ++ ("timestamp" $gte fromDate $lte toDate)
    ).sort(MongoDBObject("timestamp" -> 1))
    debug("balanceDetailsq="+allBalanceHistory.query)
    debug("balanceDetails.nonEmpty="+allBalanceHistory.nonEmpty)
    //assert(balanceDetails.nonEmpty)

    /*
          Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o2875232062468230992",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "т413во50"
      ),
     */

    val monthCosts = plans.calcMonthCost(dealer)._2

    case class UidAndEqType(uid: String, eqtype: String) /* extends Ordered[UidAndEqType]{
      override def compare(that: UidAndEqType): Int = Ordering.Tuple2[String, String].compare((uid,eqtype),(that.uid,that.eqtype))
    }*/
    case class ObjectDailyPriceData(uidantType: UidAndEqType, time: Date, todayCost: Long)


    val refs = allBalanceHistory.flatMap(entry => {
      val details = entry.as[DBObject]("details").mapValues(_.asInstanceOf[DBObject].mapValues(_.tryLong))

      details.flatMap({
        case (uid, eqs) => eqs.map({
          case (eqtype, cost) =>
            ObjectDailyPriceData(UidAndEqType(uid, eqtype), entry.as[Date]("timestamp"), cost)
        })
      })
      }).toStream.groupBy(_.uidantType).mapValues(_.toIndexedSeq.sortBy(_.time))

    for ((uidAndEqType, daylyPays) <- refs) yield {

      val head = daylyPays.head

      Map(
        "uid" -> uidAndEqType.uid,
        "objectName" -> uidAndEqType.uid,
        "eqtype" -> uidAndEqType.eqtype,
        "cost" -> monthCosts.get(uidAndEqType.uid).flatMap(_.get(uidAndEqType.eqtype)).getOrElse(0L) / 100.0,
        "firstDate" -> ISODateTime(daylyPays.head.time),
        "lastDate" -> ISODateTime(daylyPays.last.time),
        "withdraw" -> daylyPays.map(_.todayCost).sum.toDouble
      )
    }
      //    {
      //      "_id" : ObjectId("549c2aec84aebf7b40825de8"),
      //      "dealer" : "test1",
      //      "ammount" : NumberLong(-7670),
      //      "type" : "daylipay",
      //      "timestamp" : ISODate("2014-01-31T01:02:00Z"),
      //      "newbalance" : NumberLong(762400),
      //      "comment" : "",
      //      "details" : {
      //        "o2855444165634035788" : {
      //        "Основной абонентский терминал" : NumberLong(1640)
      //      },
      //        "o3463810437422151272" : {
      //        "Спящий блок автономного типа GSM" : NumberLong(820),
      //        "Радиозакладка" : NumberLong(0)
      //      },
      //        "o4059427016361140477" : {
      //        "Основной абонентский терминал" : NumberLong(1640),
      //        "Дополнительный абонентский терминал" : NumberLong(820)
      //      },
      //        "o561119587531179495" : {
      //        "Основной абонентский терминал" : NumberLong(1640),
      //        "Дополнительный абонентский терминал" : NumberLong(820),
      //        "Спящий блок на постоянном питании типа Впайка" : NumberLong(410)
      //      }
      //      }
      //    }


      //    val eqDp = storage.balanceDetailsToEqDailyPrice(balanceDetails)
      //
      //    debug("eqDp="+eqDp)

      //    val tariffPlan = getTariffForAccount(accId)
      //
      //    val monthShift = Option(params.get("month")).map(_.asInstanceOf[Integer].toInt).getOrElse(-1)
      //
      //    val abonentPrice = getPriceMap("abonentPrice", tariffPlan)
      //    val additionalAbonentPrice = getPriceMap("additionalAbonentPrice", tariffPlan)
      //
      //    val eqDatas = mps.loadEquipmentDaylyPriceShiftedMonth(accId, monthShift)
      //
      //    val equipmentsPriceData = eqDatas.values.flatMap(_.values)
      //      .map(eqData => loadEquipmentPriceData(abonentPrice, eqData))
      //
      //    val additionalPriceData = additionalAbonentPrice.map(o => {
      //      //o.put("eqtype",o.getAs[String]("name"))
      //      Map("objectName" -> "Дополнительная услуга", "eqtype" -> o._1, "cost" -> o._2)
      //    })

      //  EDSJsonResponse(equipmentsPriceData ++ additionalPriceData)
    }


  }
