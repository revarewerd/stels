package ru.sosgps.wayrecall.billing.dealer

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import ru.sosgps.wayrecall.core.{MongoDBManager, SecureGateway, UserRolesChecker}
import ru.sosgps.wayrecall.utils.DBQueryUtils._
import ru.sosgps.wayrecall.utils._

import java.util.Date
import scala.collection.JavaConversions.asJavaCollection
import scala.collection.immutable.ListMap
/**
 * Created by nickl on 20.12.14.
 */
class DealersTariffPlans extends DealersManagementMixin with grizzled.slf4j.Logging {

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  private var commandGateway: SecureGateway = null

  @Autowired
  var mdbm: MongoDBManager = null

  private def sortedGroup[A, B](data: Stream[A], f: A => B): Stream[(B, Stream[A])] = {

    if (data.isEmpty)
      Stream.empty
    else {
      val headKey = f(data.head)
      lazy val tail = sortedGroup(data.tail, f)
      lazy val (same, another) = tail.partition(_._1 == headKey)
      lazy val sameStream = Stream(data.head) ++ same.flatMap(_._2)
      Stream.cons[(B, Stream[A])]((headKey, sameStream), another)
    }




    //    if(!data.hasNext)
    //      Iterator.empty
    //    else
    //    {
    //      val head = data.next()
    //      val key = f(head)
    //
    //      lazy val nextStep: Iterator[(B, Iterator[A])] = sortedGroup(data, f).buffered
    //
    //
    //      Iterator(key, Iterator(head, nextStep.takeWhile(_._1 == key).map(_._2))) ++ nextStep
    //    }

  }

  private val isTerminal = Set("Основной абонентский терминал", "Дополнительный абонентский терминал")

  private val order = Seq(
    "Основной абонентский терминал",
    "Дополнительный абонентский терминал",
    "Спящий блок автономного типа GSM",
    "Спящий блок на постоянном питании типа Впайка"
  ).zipWithIndex.toMap.withDefaultValue(Int.MaxValue)

  def objectPrice(equipments: Seq[String]): Map[String, Double] = {

    var terminalsCount = 0

    ListMap((for (eq <- equipments.sortBy(order)) yield {
      eq match {
        case eq if isTerminal(eq) => {
          terminalsCount = terminalsCount + 1
          eq -> (if (terminalsCount == 1) 1.0 else 0.25)
        }
        case ("Спящий блок автономного типа GSM" | "Спящий блок на постоянном питании типа Впайка")
        => eq -> (if (terminalsCount > 1) 0.25 else 0.5)
        case _ => eq -> 0.0
      }
    }): _*)
  }


  def calcMonthCost(dealerId: String): (Long, Map[String, Map[String, Long]]) = {

    val baseTariff = mdbm.getDatabase()("dealers").findOne(MongoDBObject("id" -> dealerId),
      MongoDBObject("baseTariff" -> 1)).flatMap(_.getAs[Any]("baseTariff").filter(None !=).map(_.tryLong)).getOrElse(0L)

    val dealerDb = this.mmdbm.dbmanagers(dealerId).getDatabase()

    debug("dealerDb=" + dealerDb.name)

    val allInstalledEq = dealerDb("equipments")
      .find(MongoDBObject("uid" -> MongoDBObject("$exists" -> true))).sort(MongoDBObject("uid" -> 1))

    debug("allInstalledEq=" + allInstalledEq.count)

    def objectExists(dbo: DBObject) = dealerDb("objects")
      .findOne(MongoDBObject("uid" -> dbo.as[String]("uid")) ++ notRemoved, MongoDBObject("_id" -> 1)).isDefined

    val eq = sortedGroup(allInstalledEq.filter(objectExists).toStream, (m: DBObject) => m.as[String]("uid"))

    val data: Map[String, Map[String, Long]] = ListMap((for ((uid, eqs) <- eq) yield {
      val eqTypes = eqs.map(_.as[String]("eqtype"))
      debug("data:" + uid + " eq=" + eqTypes.mkString(","))
      val eqPrice = objectPrice(eqTypes).mapValues(p => (p * baseTariff).toLong)
      debug("price=\n" + eqPrice.map(kv => kv._1 + " -> " + kv._2).mkString("\n"))
      uid -> eqPrice
    }): _*)

    val sum = data.map(_._2.values.sum).sum

    (sum, data)
  }

  def changeBalance(dealerId: String, amount: Long, typ: String, comment: String = "",
                    details: Map[String, Any] = Map.empty, date: Date = new DateTime().toDate) {

    val accounts = mdbm.getDatabase()("dealers")

    val balanceHistory = mdbm.getDatabase()("dealers.balanceHistory")

    debug(s"changeBalance dealerId=$dealerId amount=$amount")

     val balance = accounts.findOne(MongoDBObject("id" -> dealerId), MongoDBObject("balance" -> 1)).get
      .getAs[Any]("balance").filter(None !=).getOrElse(0).tryLong
    val newBalance = balance + amount
    //accounts.update(MongoDBObject("_id" -> accountId),$inc("balance" -> amount) ++ $set("status"->true), false, false, WriteConcern.Safe)
    commandGateway.sendAndWait(new DealerBalanceChangeCommand(dealerId, amount), roleChecker.getUserAuthorities(), roleChecker.getUserName())

    balanceHistory.insert(MongoDBObject(
      "dealer" -> dealerId,
      "ammount" -> amount,
      "type" -> typ,
      "timestamp" -> date,
      "newbalance" -> newBalance,
      "comment" -> comment,
      "details" -> details
    ), WriteConcern.Safe)

  }

  def doDailyPayForAll():Unit = {
    subDealers().foreach(icfg => doDailyPay(icfg.name))
  }

  def doDailyPay(deaerId: String): Unit = try {

    ensurePermissions()

    val (total, details) = calcMonthCost(deaerId)

    changeBalance(
      deaerId,
      amount = -getCostForToday(total),
      typ = "dailypay",
      details = details.mapValues(_.mapValues(getCostForToday))
    )

  } catch {
    case e: Exception => warn("doDailyPay error:", e); throw e;
  }

  def ensurePermissions() {
    if (SecurityContextHolder.getContext().getAuthentication == null)
      SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("System", "", Seq("admin").map(d => new SimpleGrantedAuthority(d)))
      )
  }
}
