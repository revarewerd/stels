package ru.sosgps.wayrecall.billing.dealer

import javax.annotation.{PostConstruct, Resource}

import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.{DateTime, DateTimeUtils}
import org.junit.runner.RunWith
import org.junit.{Assert, Test}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.sosgps.wayrecall.core.{MockingUserRolesChecker, MongoDBManager, SecureGateway}
import ru.sosgps.wayrecall.initialization.MultiDbManager
import ru.sosgps.wayrecall.utils.io.Utils
import ru.sosgps.wayrecall.utils.{tryNumerics, typingMap}

import scala.collection.JavaConversions.mapAsJavaMap

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/dealers-test.xml"))
class DealerAggregateTest extends grizzled.slf4j.Logging {

  @Autowired
  var interface: DealersService = null

  @Autowired
  private var commandGateway: SecureGateway = null

  @Autowired
  private var rc: MockingUserRolesChecker = null

  @Autowired
  private var mdbm: MongoDBManager = null

  @Autowired
  private var palns: DealersTariffPlans = null

  @Resource(name = "multiDbManager")
  var mmdbm: MultiDbManager = null

  @Autowired
  private var ps: DealerMonthlyPaymentService = null

  @PostConstruct
  def cleanDb(): Unit = {
    debug("cleaning db")
    mdbm.getDatabase()("domainEvents").remove(MongoDBObject.empty)
    mdbm.getDatabase()("snapshotEvents").remove(MongoDBObject.empty)
    mdbm.getDatabase()("snapshotEvents").remove(MongoDBObject.empty)
    mdbm.getDatabase()("dealers").remove(MongoDBObject.empty)
    mdbm.getDatabase()("dealers.balanceHistory").remove(MongoDBObject.empty)


  }


  @Test
  @Deprecated
  def createAndRead0(): Unit = {

    val tariffication = Utils.ensureJavaHashMap(Map(
      "Терминал" -> 10000L,
      "Спящий блок" -> 20005L,
      "SMS" -> 15000L
    ))

    rc.runWithUserAndAuthorities("1234", Set("admin")) {
      commandGateway.sendAndWait(
        new DealerTarifficationChangeCommand("test1", tariffication),
        Array("DealerTariffer", "admin"), "1234"
      )
      ""
    }

    val result = interface.getTariffication("test1")
    debug("result =" + result)
    Assert.assertEquals(tariffication, result)

    val dealers = interface.getAllDealers(new ExtDirectStoreReadRequest).toList

    debug("dealers =" + dealers)
    Assert.assertEquals(List(Map("id" -> "test1", "accounts" -> 2, "objects" -> 4, "equipments" -> 10, "block" -> false)), dealers)

  }

  @Test
  def createAndRead(): Unit = {

    val baseTariff = 50000L
    val dealerId: String = "test1"

    setAndtTestTariff(dealerId, 10000L)
    setAndtTestTariff(dealerId, baseTariff)

    val dealers = interface.getAllDealers(new ExtDirectStoreReadRequest).toList

    debug("dealers =" + dealers)
    Assert.assertEquals(List(Map("id" -> dealerId, "accounts" -> 2, "objects" -> 4, "equipments" -> 10, "block" -> false)), dealers)

    val totalMountPrice = 212500
    testPriceAndExplanation(dealerId, baseTariff, totalMountPrice)


    // (237500,Map(o2855444165634035788 -> Map(Основной абонентский терминал -> 50000), o3463810437422151272 -> Map(Спящий блок автономного типа GSM -> 25000, Радиозакладка -> 0), o4059427016361140477 -> Map(Основной абонентский терминал -> 50000, Дополнительный абонентский терминал -> 25000), o561119587531179495 -> Map(Основной абонентский терминал -> 50000, Дополнительный абонентский терминал -> 25000, Спящий блок на постоянном питании типа Впайка -> 12500)))

    val prevBalance = 0L
    val amount1 = 1000000L
    val amount2 = -100L

    var balance = prevBalance
    testBalanceChange(dealerId, baseTariff, balance, amount1)
    balance = balance + amount1
    testBalanceChange(dealerId, baseTariff, balance, amount2)
    balance = balance + amount2

    rc.runWithUserAndAuthorities("1234", Set("admin")) {
      for (day <- 1 to 31) {
        val now = new DateTime(2014, 1, day, 5, 2)
        DateTimeUtils.setCurrentMillisFixed(now.getMillis)
        palns.doDailyPay(dealerId)
      }
    }
    balance = balance - totalMountPrice
    checkBalance(dealerId, baseTariff, balance)


    val request = new ExtDirectStoreReadRequest
    request.setParams(Map(
      "dealer" -> dealerId,
      "month" -> (0.asInstanceOf[AnyRef])
    ))
    val r = ps.read(request)

    val caclked = r.map(_.as[Any]("withdraw").tryLong).sum
    debug("r=" + r)

    Assert.assertEquals(totalMountPrice, caclked)


  }

  private def testBalanceChange(dealerId: String, baseTariff: Long, prevBalance: Long, amount: Long) {
    rc.runWithUserAndAuthorities("1234", Set("admin")) {
      commandGateway.sendAndWait(
        new DealerBalanceChangeCommand(dealerId, amount),
        Array("admin"), "1234"
      )
      ""
    }

    checkBalance(dealerId, baseTariff, prevBalance + amount)
  }

  private def checkBalance(dealerId: String, baseTariff: Long, balance: Long) {
    val result1 = interface.getDealerParams(dealerId)
    debug("result =" + result1)
    Assert.assertEquals(baseTariff, result1("baseTariff"))
    Assert.assertEquals(balance, result1("balance"))
  }

  private def setAndtTestTariff(dealerId: String, baseTariff: Long) {
    rc.runWithUserAndAuthorities("1234", Set("admin")) {
      commandGateway.sendAndWait(
        new DealerBaseTariffChangeCommand(dealerId, baseTariff),
        Array("DealerTariffer", "admin"), "1234"
      )
      ""
    }

    val result = interface.getDealerParams(dealerId)
    debug("result =" + result)
    Assert.assertEquals(baseTariff, result("baseTariff"))

  }

  private def testPriceAndExplanation(dealerId: String, baseTariff: Long, totalMountPrice: Int) {
    val mc = palns.calcMonthCost(dealerId)

    debug("mc =" + mc)
    Assert.assertEquals(totalMountPrice, mc._1)
    Assert.assertEquals(
      Map(
        "o2855444165634035788" ->
          Map("Основной абонентский терминал" -> baseTariff),
        "o3463810437422151272" ->
          Map(
            "Спящий блок автономного типа GSM" -> baseTariff * 0.5,
            "Радиозакладка" -> 0
          ),
        "o4059427016361140477" ->
          Map(
            "Основной абонентский терминал" -> baseTariff,
            "Дополнительный абонентский терминал" -> baseTariff * 0.25
          ),
        "o561119587531179495" ->
          Map(
            "Основной абонентский терминал" -> baseTariff,
            "Дополнительный абонентский терминал" -> baseTariff * 0.25,
            "Спящий блок на постоянном питании типа Впайка" -> baseTariff * 0.25
          )
      ),
      mc._2)

    val result = interface.getDealerParams(dealerId)
    Assert.assertTrue(s"result(cost)=${result("cost")} but expected $totalMountPrice",
      totalMountPrice == result("cost"))

  }
}
