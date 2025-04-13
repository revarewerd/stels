package ru.sosgps.wayrecall.billing.finance


import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import ru.sosgps.wayrecall.core.{MongoDBManager, UserRolesChecker}
import ru.sosgps.wayrecall.utils.errors.NotPermitted
import ru.sosgps.wayrecall.utils.web.extjsstore.{EDSStoreServiceDescriptor, StoreAPI}
import org.springframework.beans.factory.annotation.Autowired
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import org.bson.types.ObjectId
import ch.ralscha.extdirectspring.bean._

import collection.mutable
import ru.sosgps.wayrecall.utils.web.{EDSJsonResponse, ScalaHttpRequest}

import collection.JavaConversions.{collectionAsScalaIterable, mapAsJavaMap, mapAsScalaMap, seqAsJavaList}
import com.mongodb.casbah.Imports._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._

import scala.Predef._
import ru.sosgps.wayrecall.utils._
import javax.annotation.PostConstruct

import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import java.util.Date

import ru.sosgps.wayrecall.core.finance.{FeeProcessor, TariffPlans}


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 06.07.12
 * Time: 16:37
 * To change this template use File | Settings | File Templates.
 */

@ExtDirectService
class BalanceHistoryEntryTypes extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  override val model: Model = Model("type")
  override val name: String = "BalanceEntryTypes"
  override val idProperty: String = "type"
  override val lazyLoad: Boolean = false

  case class Record(`type`: String)

  val balanceEntryTypes = Array("dailypay",
    "Зачислить на счет",
    "Монтаж АТ",
    "Уведомление по SMS",
    "Пополнение счета",
    "Yandex касса",
    "Снять со счета"
  ).map(t => Map("type" -> t))

  val dealerBalanceEntryTypes = Array(
    '-', "dailypay", "sms payment", "Зачислить"
  ).map(t => Map("type" -> t))

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest) = {
    val params = request.getParams
    params("itemType").toString match {
      case "balance" => balanceEntryTypes
      case "dealerBalance" => dealerBalanceEntryTypes
      case _ => throw new IllegalArgumentException("Expected 'balance' or 'dealerBalance' as entity name")
    }
  }
}

@ExtDirectService
class BalanceHistoryStoreService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("_id", "type", "ammount", "timestamp", "newbalance","comment")

  val name = "BalanceHistory"

  val idProperty = "_id"

  override val autoSync = false

  val lazyLoad = true

  @Autowired
  var mdbm: MongoDBManager = null

  val balanceCollection = "balanceHistoryWithDetails"
  val dealerBalanceCollection = "dealers.balanceHistory"


  def loadRawData(request: ExtDirectStoreReadRequest) = {
    val params = request.getParams
    debug("Params are " + params.mkString(","))
    val dbName = params("itemType").toString match {
      case "balance" => balanceCollection
      case "dealerBalance" => dealerBalanceCollection
      case _ => throw new IllegalArgumentException("Expected 'balance' or 'dealerBalance' as entity name")
    }

    val accId = params("accountId").toString
    val typeFilter = Option(params.get("typeFilter")).map(_.asInstanceOf[java.util.List[String]])
      .map(t => if(t.isEmpty) MongoDBObject.empty else  "type" $in t.toList).getOrElse(MongoDBObject.empty)


    debug("typeFilter is: " + typeFilter)
    val dateFrom = Option(params.get("dateFrom")).map(x => parseDate(x.asInstanceOf[String])).getOrElse(new Date(111, 0, 1))
    val dateTo = Option(params.get("dateTo")).map(x => parseDate(x.asInstanceOf[String])).getOrElse(new Date())
    val period = "timestamp" $lte dateTo $gte dateFrom
    debug("Period is " + period.mkString(","))

    setDefaultSorterIfEmpty(request, "timestamp", SortDirection.DESCENDING)

    val mainQuery = dbName match {
      case `balanceCollection` => MongoDBObject("account" -> new ObjectId(accId))
      case `dealerBalanceCollection` => MongoDBObject("dealer" -> accId)
    }

    mdbm.getDatabase().apply(dbName).find(
      mainQuery ++ typeFilter ++ period,
      MongoDBObject(model.fieldsNames.map(_ -> 1).toList))
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def loadData(request: ExtDirectStoreReadRequest) = {

    val cursor = loadRawData(request)

    val (total, balanceHistoryCollection) = cursor.totalCountAndApplyEDS(request)

    val jodaISOformat = ISODateTimeFormat.dateTime()
    def format(d: Date) = jodaISOformat.print(new DateTime(d))

    EDSJsonResponse.forPart(total, balanceHistoryCollection.map(
      dbo => {dbo.put("timestamp", format(dbo.as[Date]("timestamp"))); dbo}
    ))

  }


}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod}
import ch.ralscha.extdirectspring.bean.ExtDirectResponseBuilder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.beans.factory.annotation.Autowired

@Controller
class BalanceChange {

  @Autowired
  var feeProcessor: FeeProcessor = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @ExtDirectMethod(ExtDirectMethodType.FORM_POST)
  @RequestMapping(value = Array("/balanceChange"), method = Array(RequestMethod.POST))
  def balanceChange(request: HttpServletRequest,
                    response: HttpServletResponse,
                    @RequestParam("accountId") accountId: String,
                    @RequestParam("amount") amount100: Double,
                    @RequestParam("type") typ: String,
                    @RequestParam("comment") comment: String
                     ) {
    if(roleChecker.hasRequiredAuthorities(Seq(new SimpleGrantedAuthority("admin"),new SimpleGrantedAuthority("ChangeBalance")))) {
      val amount = (amount100 * 100).toLong
      feeProcessor.changeBalance(new ObjectId(accountId), amount, typ, comment)
      ExtDirectResponseBuilder.create(request, response).successful().buildAndWrite();
    }
    else //throw new AccessDeniedException("Недостаточно прав для изменения баланса учетной записи")
    {
      ExtDirectResponseBuilder.create(request, response).addResultProperty("exception","Недостаточно прав для изменения баланса учетной записи").setException(new NotPermitted("Недостаточно прав для изменения баланса учетной записи")).buildAndWrite()
    }
  }
}

@ExtDirectService
class CommercialServices extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("name", "cost")

  val name = "CommercialServices"

  val idProperty = "_id"

  override val autoSync = false

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var tarrifs: TariffPlans = null


  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest): Iterator[Map[String, AnyRef]] = {

    val loaded = loadFromDataBase(request).map(dbo =>

      Map(
        "name" -> dbo.as[String]("name"),
        "cost" -> (dbo.as[String]("cost").toLong * (-1)).asInstanceOf[AnyRef]
      )

    )


    loaded ++ Iterator(
      Map(
        "name" -> "Зачислить на счет",
        "cost" -> "100"
      ))

  }


  private[this] def loadFromDataBase(request: ExtDirectStoreReadRequest): Iterator[MongoDBObject] = {

    val accId = request.getParams()("accountId").toString

    val tariffOption: Option[ObjectId] = tarrifs.getTariffIdForAccount(accId)

    tariffOption match {
      case Some(tariff) => {
        val result = mdbm.getDatabase()("tariffs")
          .findOneByID(tariff, MongoDBObject("servicePrice" -> 1))
          .map(
          dbo => dbo.getAs[MongoDBList]("servicePrice")
            .map(_.toIterator).getOrElse(Iterator.empty)
            .map(a => wrapDBObj(a.asInstanceOf[DBObject]))
        ).getOrElse(Iterator.empty)

        result
      }
      case None => {
        warn("no tariff data for account " + accId)
        Iterator.empty
      }
    }

  }

}

@ExtDirectService
class EquipmentTypes extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("name")

  val name = "EquipmentTypes"

  val idProperty = "name"

  override val autoSync = false

  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var tarrifs: TariffPlans = null


  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData(request: ExtDirectStoreReadRequest): TraversableOnce[scala.collection.Map[String, AnyRef]] = {

    val accId = request.getParams()("accountId").toString

    try {

      val tariff = tarrifs.getTariffForAccount(new ObjectId(accId)).getOrElse(tarrifs.emptyTariff)

      tariff.as[MongoDBList]("hardwarePrice").map(dbo => wrapDBObj(dbo.asInstanceOf[DBObject])) ++
        tariff.as[MongoDBList]("abonentPrice").map(dbo => wrapDBObj(dbo.asInstanceOf[DBObject]))
    }
    catch {
      case e: java.lang.IllegalArgumentException => {
        warn("error while processing request:" + request, e)
        Iterator.empty
      }
    }

  }

}

