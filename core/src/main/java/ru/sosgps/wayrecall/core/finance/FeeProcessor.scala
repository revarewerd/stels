package ru.sosgps.wayrecall.core.finance

import com.mongodb.casbah.Imports
import org.axonframework.domain.GenericEventMessage
import org.axonframework.eventhandling.EventBus
import ru.sosgps.wayrecall.billing.account.commands.AccountDataSetCommand
import ru.sosgps.wayrecall.billing.account.events.AccountBalanceChanged
import ru.sosgps.wayrecall.core.{SecureGateway, MongoDBManager}
import ru.sosgps.wayrecall.events.{EventsStore, DataEvent, PredefinedEvents}
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.{getCostForToday, tryLong, typingMap}
import java.util.Date
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import java.util
import java.util.Calendar
import com.mongodb.casbah.query.Imports.LongOk
import ru.sosgps.wayrecall.utils.DBQueryUtils._
import scala.Predef._
import org.joda.time.DateTime

import scala.beans.BeanProperty
import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 04.07.12
 * Time: 16:53
 * To change this template use File | Settings | File Templates.
 */


@Component
class FeeProcessor extends grizzled.slf4j.Logging {

  debug("FeeProcessor created")

  @Autowired
  @BeanProperty
  var es: EventBus = null

  @Autowired
  var mongodb: MongoDBManager = null

  @Autowired
  var tariffPlans: TariffPlans = null

  @Autowired
  var commandGateway: SecureGateway = null

  lazy val database = mongodb.getDatabase

  //  @deprecated("to be replaced with dailySubtractWithDetails")
  //  def dailySubtract() {
  //
  //    tariffPlans.sumCostForObjectCache.invalidateAll()
  //
  //    val accounts = database("accounts")
  //
  //    val objects = database("objects")
  //
  //    val balanceHistory = database("balanceHistory")
  //    info("ensuring Mongodb Index balanceHistory.account")
  //    balanceHistory.ensureIndex("account")
  //
  //    for (acc <- accounts.find(MongoDBObject(), MongoDBObject("_id" -> 1, "plan" -> 1, "balance" -> 1))) try {
  //      val totalCost = tariffPlans.calcTotalCost(acc)
  //
  //      val calendar = new util.GregorianCalendar()
  //      calendar.setTime(new Date())
  //      val dailyPay = totalCost / calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
  //
  //      if (acc.get("balance").isInstanceOf[String]) {
  //        debug("converting")
  //        accounts.update(MongoDBObject("_id" -> acc("_id")), $set("balance" -> acc.as[String]("balance").toLong), false, false, WriteConcern.Safe)
  //      }
  //
  //      if (dailyPay != 0) {
  //        changeBalance(acc("_id").asInstanceOf[ObjectId], -dailyPay, "dailypay")
  //      }
  //    }
  //    catch {
  //      case e: Exception => error("error while processing fee for acc:" + acc, e)
  //    }
  //    info("dailySubtract " + new Date())
  //
  //    try {
  //      dailySubtractWithDetails()
  //    } catch {
  //      case e: Throwable => warn("error in dailySubtractWithDetails", e)
  //    }
  //
  //  }

  def changeBalance(accountId: ObjectId, amount: Long, typ: String, comment: String = "", details: Map[String, Any] = Map.empty, date: Date = new DateTime().toDate, collection: String = "balanceHistoryWithDetails") {

    val accounts = database("accounts")

    val balanceHistory = database(collection)

    val balance = accounts.findOneByID(accountId, MongoDBObject("balance" -> 1)).get.getAs[Any]("balance").filter(None !=).getOrElse(0) match {
      case i: java.lang.Integer => i.toLong
      case l: java.lang.Long => l.toLong
      case d: java.lang.Double => d.toLong
      case s: String => s.toLong
    }
    val newBalance = balance + amount
    if (newBalance > 0) {
      //accounts.update(MongoDBObject("_id" -> accountId),$inc("balance" -> amount) ++ $set("status"->true), false, false, WriteConcern.Safe)
      commandGateway.sendAndWait(new AccountDataSetCommand(accountId,
        Map("balance" -> (newBalance.asInstanceOf[AnyRef]), "status" -> (true.asInstanceOf[AnyRef])).asJava),
        Set("admin", "AccountDataSet").toArray, "System")
    } else
      commandGateway.sendAndWait(new AccountDataSetCommand(accountId,
        Map("balance" -> (newBalance.asInstanceOf[AnyRef])).asJava),
        Set("admin", "AccountDataSet").toArray, "System")
    //accounts.update(MongoDBObject("_id" -> accountId),$inc("balance" -> amount), false, false, WriteConcern.Safe)

    balanceHistory.insert(MongoDBObject(
      "account" -> accountId,
      "ammount" -> amount,
      "type" -> typ,
      "timestamp" -> date,
      "newbalance" -> newBalance,
      "comment" -> comment,
      "details" -> details
    ), WriteConcern.Safe)


    es.publish(GenericEventMessage.asEventMessage(new AccountBalanceChanged(accountId, newBalance)))

    //es.publish(new DataEvent[util.HashMap[String, Serializable]](new util.HashMap(), PredefinedEvents.balanceChanged))

  }

  def getDailyPayForAccount(accountId: ObjectId): Long = {
    val accounts = database("accounts")
    val objects = database("objects")
    val balanceHistory = database("balanceHistoryWithDetails")
    info("ensuring Mongodb Index balanceHistory.account")
    balanceHistory.ensureIndex("account")

    val acc = accounts.findOne(MongoDBObject("_id" -> accountId), MongoDBObject("_id" -> 1, "plan" -> 1, "balance" -> 1, "limitType" -> 1, "limitValue" -> 1)).get;
    val tariff = tariffPlans.getTariffForAccount(acc).get
    val accObjects = tariffPlans.getAccountObjects(acc)

    val devprice = tariffPlans.mapDeviceTypeToPrice(tariff)

    val objDetails = accObjects.map(objDbo => {

      val disabled = objDbo.getAsOrElse[Boolean]("disabled", false)

      def getCostDisabledAware(eqDbo: Imports.DBObject): Long = {
        if (disabled) 0L else getCostForToday(devprice(eqDbo.as[String]("eqtype")))
      }

      Map(
        "uid" -> objDbo.as[String]("uid"),
        "name" -> objDbo.as[String]("name"),
        "equipments" -> tariffPlans.getObjectDevices(objDbo.as[String]("uid")).map(eqDbo => eqDbo ++ ("todayCost" -> getCostDisabledAware(eqDbo))).toList
      )
    }).toList

    debug("objDetails=" + objDetails)

    val additionalPrices = tariffPlans.getAdditionalPrices(tariff).map(dbo => Map("name" -> dbo("name"), "todayCost" -> getCostForToday(tryLong(dbo("cost")) * 100)))
    val datails = Map(
      "objectDetails" -> objDetails,
      "additionalPrice" -> additionalPrices
    )

    val dailyPay = objDetails.map(_.as[Seq[DBObject]]("equipments").map(_.as[Long]("todayCost")).sum).sum + additionalPrices.map(_.as[Long]("todayCost")).sum
    dailyPay
  }

  def dailySubtractWithDetails() {
    tariffPlans.invalidateCache()

    val accounts = database("accounts")

    val objects = database("objects")

    val balanceHistory = database("balanceHistoryWithDetails")
    info("ensuring Mongodb Index balanceHistory.account")
    balanceHistory.ensureIndex("account")

    for (acc <- accounts.find(notRemoved, MongoDBObject("_id" -> 1, "plan" -> 1, "balance" -> 1, "limitType" -> 1, "limitValue" -> 1));
         tariff <- tariffPlans.getTariffForAccount(acc)
    ) try {
      val accObjects = tariffPlans.getAccountObjects(acc)

      val devprice = tariffPlans.mapDeviceTypeToPrice(tariff)

      val objDetails = accObjects.map(objectToDetails(
        eqForObjectProvider = (objDbo: Imports.DBObject) => tariffPlans.getObjectDevices(objDbo.as[String]("uid")),
        objectEqCost = (dbo: Imports.DBObject, eqDbo: Imports.DBObject) => {
          val disabled = dbo.getAsOrElse[Boolean]("disabled", false)
          if (disabled) 0L else getCostForToday(devprice(eqDbo.as[String]("eqtype")))
        }
      )).toList

      debug("objDetails=" + objDetails)

      val additionalPrices = tariffPlans.getAdditionalPrices(tariff).map(dbo => Map("name" -> dbo("name"), "todayCost" -> getCostForToday(tryLong(dbo("cost")) * 100)))
      val datails = Map(
        "objectDetails" -> objDetails,
        "additionalPrice" -> additionalPrices
      )

      val dailyPay = objDetails.map(_.as[Seq[DBObject]]("equipments").map(_.as[Long]("todayCost")).sum).sum + additionalPrices.map(_.as[Long]("todayCost")).sum
      if (dailyPay != 0) {
        changeBalance(acc("_id").asInstanceOf[ObjectId], -dailyPay, "dailypay", "", datails, collection = "balanceHistoryWithDetails")
        val balance = acc.get("balance") match {
          case i: java.lang.Integer => i.toLong
          case l: java.lang.Long => l.toLong
          case d: java.lang.Double => d.toLong
          case s: String => s.toLong
        }
        val changedBalance = balance - dailyPay
        checkPayLimits(acc, changedBalance)
      }
    }
    catch {
      case e: Exception => error("error while processing fee for acc:" + acc, e)
    }
    info("dailySubtract with Detailes " + new DateTime())
  }

  def objectToDetails(
                       eqForObjectProvider: (Imports.DBObject) => Iterator[Imports.DBObject],
                       objectEqCost: (Imports.DBObject, Imports.DBObject) => Long
                       ): (Imports.DBObject) => Map[String, Object] = {
    objDbo => {
      Map(
        "uid" -> objDbo.as[String]("uid"),
        "name" -> objDbo.as[String]("name"),
        "equipments" -> eqForObjectProvider(objDbo).map(eqDbo => eqDbo ++ ("todayCost" -> objectEqCost(objDbo, eqDbo))).toList
      )
    }
  }

  def checkPayLimits(acc: DBObject, balance: java.lang.Long): Unit = {
    val accounts = database("accounts")
    val balanceHistory = database("balanceHistoryWithDetails")
    val limitType = acc.get("limitType").asInstanceOf[String]
    val limitValue = acc.get("limitValue").asInstanceOf[String]
    val accId = acc("_id").asInstanceOf[ObjectId]
    if (limitType != null && limitValue != null) {
      debug("balance=" + balance.toDouble / 100)
      val blockInfo = Map("status" -> false.asInstanceOf[AnyRef], "blockcause" -> ("Учетная запись заблокирована из-за недостатка средств на счете. Баланс: " + balance.toDouble / 100 + " руб.")).asJava
      limitType match {
        case "daysLimit" => {
          debug("daysLimit : limitValue=" + limitValue)
          var currentDate = new util.Date()
          var accBalance = balanceHistory.find(MongoDBObject("account" -> accId), MongoDBObject("timestamp" -> 1, "newbalance" -> 1)).sort(MongoDBObject("timestamp" -> (-1)))
          def checkExpired(): Unit = {
            accBalance.foreach(balanceRecord => {
              val newBalance = balanceRecord.get("newbalance").asInstanceOf[java.lang.Long]
              if (newBalance < 0) {
                val date = balanceRecord.get("timestamp").asInstanceOf[java.util.Date]
                val currentDate = new util.Date()
                if (currentDate.getTime() - date.getTime() > limitValue.toDouble * 86400000) {
                  debug("account id=" + accId + " blocked")
                  commandGateway.sendAndWait(new AccountDataSetCommand(accId, blockInfo), Set("admin", "AccountDataSet").toArray, "System")
                  return
                }
              }
              else return
            })
          }
          checkExpired()
        }
        case "currencyLimit" => {
          debug("currencyLimit: limitValue=" + limitValue)
          if (balance < (limitValue.toLong * 100)) {
            commandGateway.sendAndWait(new AccountDataSetCommand(accId, blockInfo), Set("admin", "AccountDataSet").toArray, "System")
          }
        }
        case "percentMonthlyFeeLimit" => {
          val cost = tariffPlans.calcTotalCost(acc)
          debug("percentMonthlyFeeLimit: cost=" + cost + " limitValue=" + limitValue + " percentMonthlyFeeLimit=" + cost * limitValue.toLong / 100 + " rub.")
          if (balance < (-cost * limitValue.toLong / 100)) {
            commandGateway.sendAndWait(new AccountDataSetCommand(accId, blockInfo), Set("admin", "AccountDataSet").toArray, "System")
          }
        }
      }

    }
  }

}
