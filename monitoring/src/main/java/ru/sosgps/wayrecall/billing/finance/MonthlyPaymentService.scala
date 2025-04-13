package ru.sosgps.wayrecall.billing.finance

import ru.sosgps.wayrecall.core.{EquipmentDayPriceData, MonthlyPaymentStorage, MongoDBManager, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.utils.web.extjsstore.{StoreAPI, EDSStoreServiceDescriptor}
import org.springframework.beans.factory.annotation.Autowired
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}

import ch.ralscha.extdirectspring.bean.{ExtDirectResponseBuilder, SortDirection, ExtDirectStoreReadRequest}

import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._

import scala.Predef._
import ru.sosgps.wayrecall.utils.{Memo, ExtDirectService, ISODateTime}
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._

import com.mongodb.casbah.Imports
import org.joda.time.DateTime
import java.util.Date
import scala.collection.immutable
import org.springframework.stereotype.Component


@ExtDirectService
class MonthlyPaymentService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("_id", "accountId", "objectName", "uid", "eqtype", "firstDate", "lastDate", "withdraw", "cost")

  val name = "MonthlyPaymentService"
  val idProperty = "_id"

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var mps: MonthlyPaymentStorage = null

  var dbequipments: MongoCollection = null
  var dbaccounts: MongoCollection = null

  @PostConstruct
  def init() {
    dbequipments = mdbm.getDatabase().apply("equipments")
    dbaccounts = mdbm.getDatabase().apply("accounts")
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def read(request: ExtDirectStoreReadRequest) = {
    debug("MonthlyPaymentService read " + request)

    val params = request.getParams
    val accId = new ObjectId(params.get("accountId").asInstanceOf[String])
    val tariffPlan = getTariffForAccount(accId)

    val monthShift = Option(params.get("month")).map(_.asInstanceOf[Integer].toInt).getOrElse(-1)

    val abonentPrice = getPriceMap("abonentPrice", tariffPlan)
    val additionalAbonentPrice = getPriceMap("additionalAbonentPrice", tariffPlan)

    val eqDatas = mps.loadEquipmentDaylyPriceShiftedMonth(accId, monthShift)

    val equipmentsPriceData = eqDatas.values.flatMap(_.values)
      .map(eqData => loadEquipmentPriceData(abonentPrice, eqData))

    val additionalPriceData = additionalAbonentPrice.map(o => {
      //o.put("eqtype",o.getAs[String]("name"))
      Map("objectName" -> "Дополнительная услуга", "eqtype" -> o._1, "cost" -> o._2)
    })

    EDSJsonResponse(equipmentsPriceData ++ additionalPriceData)
  }

  private[this] def loadEquipmentPriceData(abonentPrice: Map[String, AnyVal],
                                           daylyPays: IndexedSeq[EquipmentDayPriceData]): scala.collection.Map[String, Any] = {
    if (daylyPays.isEmpty)
      return Map.empty

    val head = daylyPays.head

    Map(
      "uid" -> head.objuid,
      "objectName" -> or.getObjectName(head.objuid),
      "eqtype" -> head.eqtype,
      "cost" -> abonentPrice.get(head.eqtype),
      "firstDate" -> ISODateTime(daylyPays.head.time),
      "lastDate" -> ISODateTime(daylyPays.last.time),
      "withdraw" -> daylyPays.map(_.todayCost).sum.toDouble
    )

  }

  private[this] def getTariffIdForAccount(accId: ObjectId): Option[ObjectId] = {
    val tariffOption = mdbm.getDatabase()("accounts").findOneByID(accId, MongoDBObject("plan" -> 1))
      .flatMap(_.getAs[AnyRef]("plan").filter(None !=)).flatMap({
      case str: String => try {
        Some(new ObjectId(str))
      } catch {
        case e: java.lang.IllegalArgumentException => None
      }
      case oid: ObjectId => Some(oid)
    })
    tariffOption
  }

  private[this] def getTariffForAccount(accId: ObjectId): Option[DBObject] = {
    getTariffIdForAccount(accId).flatMap(tid =>
      mdbm.getDatabase()("tariffs").findOneByID(tid)
    )
  }

  private[this] def getPriceMap(priceName: String, plan: Option[Imports.DBObject]) = plan.flatMap(_.getAs[MongoDBList](priceName)).getOrElse(MongoDBList.empty)
    .map(v => {
    val mdbo = v.asInstanceOf[DBObject]
    (mdbo.as[String]("name"), mdbo.as[String]("cost").toInt)
  }).toMap.withDefaultValue(0L)


}
