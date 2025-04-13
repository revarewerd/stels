package ru.sosgps.wayrecall.billing.dealer

import java.util
import java.util.{Date, HashMap => jHashMap}
import javax.annotation.Resource
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.{ExtDirectResponseBuilder, ExtDirectStoreReadRequest, SortDirection}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam}
import ru.sosgps.wayrecall.core.{MongoDBManager, SecureGateway, UserRolesChecker}
import ru.sosgps.wayrecall.initialization.{MultiDbManager, MultiserverConfig}
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.errors.NotPermitted
import ru.sosgps.wayrecall.utils.io.Utils
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils._
import com.mongodb.casbah.Imports._

import scala.beans.BeanProperty
import scala.collection.immutable.ListMap
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable
import ru.sosgps.wayrecall.utils.DBQueryUtils._

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 31.07.12
 * Time: 18:32
 * To change this template use File | Settings | File Templates.
 */

@ExtDirectService
class DealersService extends DealersManagementMixin with EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  val model = Model("id", "accounts", "objects", "equipments","block")

  val updatebleFields = Set()

  val name = "DealersService"
  val idProperty = "id"

  val lazyLoad = false

  @Autowired
  private var commandGateway: SecureGateway = null

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  private var plans: DealersTariffPlans = null

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def loadAll(request: ExtDirectStoreReadRequest) = {
    EDSJsonResponse(getAllDealers(request))
  }

  def getAllDealers(request: ExtDirectStoreReadRequest) = {

    require(instanceName != null)

    subDealers().map(cfg => {
      val currentDealer =mmdbm.getMainDBManager.getDatabase()("dealers").findOne(MongoDBObject("id"->cfg.name))
      val block =if(currentDealer.isDefined) currentDealer.get.getAsOrElse[Boolean]("block",false) else false
      val mdbm = mmdbm.dbmanagers(cfg.name)

      ListMap(
        "id" -> cfg.name,
        "accounts" -> mdbm.getDatabase()("accounts").find(notRemoved).count,
        "objects" -> mdbm.getDatabase()("objects").find(notRemoved).count,
        "equipments" -> mdbm.getDatabase()("equipments").find(notRemoved).count,
        "block" -> block
      )

    })

    //    Seq(ListMap(
    //      "name" -> "default",
    //      "accounts" -> 1000,
    //      "objects" -> 30,
    //      "equipments" -> 5
    //    ))

  }

  @ExtDirectMethod
  def dealerBlocking(id: String,block: Boolean): Map[String, AnyRef] = try {
    if (!roleChecker.checkAdminAuthority) throw new RuntimeException("Недостаточно прав для изменения аккаунта")
    if (id == null) {
      return Map("status" -> "no such id")
    }
    debug("dealerBlocking id:" + id)
    commandGateway.sendAndWait(
      new DealerBlockingCommand(id, block),
      roleChecker.getUserAuthorities, roleChecker.getUserName
    )
    Map("status" -> "ok")
  } catch {
    case e: Throwable => error("error:", e); throw e;
  }

  @ExtDirectMethod
  @deprecated("not used")
  def updateTariffication(submitMap: Map[String, AnyRef]): Map[String, AnyRef] = try {
    if (!roleChecker.checkAdminAuthority) throw new RuntimeException("Недостаточно прав для изменения аккаунта")
    if (submitMap == null || submitMap.isEmpty) {
      return submitMap
    }
    debug("updateTarrification:" + submitMap)

    type TR = java.util.HashMap[String, AnyRef]

    val r: java.util.HashMap[String, Long] = submitMap("tariffication") match {
      case a: java.util.ArrayList[TR] => Utils.ensureJavaHashMap(a.map(hm => (hm.as[String]("name"), (hm.get("cost").tryDouble * 100).toLong)).toMap)
    }

    commandGateway.sendAndWait(
      new DealerTarifficationChangeCommand(submitMap("id").asInstanceOf[String], r),
      roleChecker.getUserAuthorities, roleChecker.getUserName
    )
    Map("status" -> "ok")
  } catch {
    case e: Throwable => error("error:", e); throw e;
  }

  @ExtDirectMethod
  @deprecated("not used")
  def updateDealerParams(submitMap: Map[String, AnyRef]): Map[String, AnyRef] = try {
    if (!roleChecker.checkAdminAuthority) throw new RuntimeException("Недостаточно прав для изменения аккаунта")
    if (submitMap == null || submitMap.isEmpty) {
      return submitMap
    }
    debug("updateTarrification:" + submitMap)

    type TR = java.util.HashMap[String, AnyRef]

    val r: Long = submitMap("baseTariff").tryLong

    commandGateway.sendAndWait(
      new DealerBaseTariffChangeCommand(submitMap("id").asInstanceOf[String], r),
      roleChecker.getUserAuthorities, roleChecker.getUserName
    )
    Map("status" -> "ok")
  } catch {
    case e: Throwable => error("error:", e); throw e;
  }

  @ExtDirectMethod
  @deprecated("not used")
  def getTariffication(id: String) = {

    debug("getTariffication " + id)

    val r = mdbm.getDatabase()("dealers").findOne(MongoDBObject("id" -> id))
      .map(_("tariffication")).getOrElse(Map(
      "Терминал" -> 10000,
      "Спящий блок" -> 20000,
      "SMS" -> 15000
    ))

    debug("getTariffication r=" + r)
    r
  }

  @ExtDirectMethod
  def getDealerParams(id: String) = {

    val paramsToSend = Set("baseTariff", "balance")

    debug("getDealerParams " + id)

    val d = mdbm.getDatabase()("dealers").findOne(MongoDBObject("id" -> id),
      MongoDBObject(paramsToSend.toSeq.map(_ -> 1): _*))

    val res = d.map(m => m: mutable.Map[String, AnyRef]).getOrElse(mutable.Map.empty)

    res.put("cost", plans.calcMonthCost(id)._1.asInstanceOf[AnyRef])

    val r = mutable.Map[String, Any](
      "baseTariff" -> 500L
    ) ++= res

    //    if(!res.isDefinedAt("baseTariff")){
    //      res.put()
    //    }

    debug("getTariffication r=" + r)
    r
  }

  //  def getPermForCurrentUser(request: ExtDirectStoreReadRequest) = {
  //    var allObjects : Iterator[DBObject] = Iterator.empty
  //    val userName = roleChecker.getUserName()
  //    if(roleChecker.hasRequiredAuthorities(Seq(new SimpleGrantedAuthority("admin"),new SimpleGrantedAuthority("UserView")))) {
  //      allObjects = mdbm.getDatabase().apply(dbName).find(searchAndFilters(request), model.fieldsNames.map(_ -> 1).toMap).applyEDS(request)
  //    }
  //    else if(roleChecker.hasRequiredAuthorities(Seq(new SimpleGrantedAuthority("superuser"),new SimpleGrantedAuthority("UserView")))) {
  //        val creatorId=roleChecker.getUserIdByName(userName)
  //        val creator=if(creatorId==null) userName else creatorId
  //        allObjects = mdbm.getDatabase().apply(dbName).find($or("name"->userName,"creator"->creator),model.fieldsNames.map(_ -> 1).toMap).applyEDS(request)
  //    }
  //    allObjects
  //  }

}

@Controller
class DealersBalanceChange {

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var dealerdTariffPlanns: DealersTariffPlans = null

  @ExtDirectMethod(ExtDirectMethodType.FORM_POST)
  @RequestMapping(value = Array("/dealerbalanceChange"), method = Array(RequestMethod.POST))
  def dealerbalanceChange(request: HttpServletRequest,
                    response: HttpServletResponse,
                    @RequestParam("accountId") accountId: String,
                    @RequestParam("amount") amount100: Double,
                    @RequestParam("type") typ: String,
                    @RequestParam("comment") comment: String
                     ) {
    if(roleChecker.hasRequiredStringAuthorities("admin","ChangeBalance")) {
      val amount = (amount100 * 100).toLong

      dealerdTariffPlanns.changeBalance(dealerId = accountId, amount = amount, typ = typ)

      ExtDirectResponseBuilder.create(request, response).successful().buildAndWrite();
    }
    else //throw new AccessDeniedException("Недостаточно прав для изменения баланса учетной записи")
    {
      ExtDirectResponseBuilder.create(request, response).addResultProperty("exception","Недостаточно прав для изменения баланса учетной записи").setException(new NotPermitted("Недостаточно прав для изменения баланса учетной записи")).buildAndWrite()
    }
  }
}

@ExtDirectService
class DealerBalanceEntryTypes extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  override val model: Model = Model("type")
  override val name: String = "DealerBalanceEntryTypes"
  override val idProperty: String = "type"
  override val lazyLoad: Boolean = false

  val entryTypes = Array(
    '-', "dailypay", "sms payment", "Зачислить"
  ).map(t => Map("type" -> t))

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ)
  def loadData() = {
    entryTypes
  }
}

@ExtDirectService
class DealersBalanceHistoryStoreService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("id", "type", "ammount", "timestamp", "newbalance","comment")

  val name = "DealersBalanceHistory"

  val idProperty = "id"

  override val autoSync = false

  val lazyLoad = true

  @Autowired
  var mdbm: MongoDBManager = null


  def loadRawData(request: ExtDirectStoreReadRequest) = {
    val params = request.getParams
    val accId = params.as[String]("accountId")
    val typeFilter = Option(params.get("typeFilter")).map(_.asInstanceOf[java.util.List[String]])
      .map(t => "type" $in t.toList).getOrElse(MongoDBObject.empty)
    val dateFrom = Option(params.get("dateFrom")).map(x => parseDate(x.asInstanceOf[String])).getOrElse(new Date(111, 0, 1))
    val dateTo = Option(params.get("dateTo")).map(x => parseDate(x.asInstanceOf[String])).getOrElse(new Date())

    val period = "timestamp" $lte dateTo $gte dateFrom

    setDefaultSorterIfEmpty(request, "timestamp", SortDirection.DESCENDING)

    mdbm.getDatabase()("dealers.balanceHistory").find(
      MongoDBObject("dealer" -> accId) ++ typeFilter ++ period,
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








