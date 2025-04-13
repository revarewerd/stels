package ru.sosgps.wayrecall.billing

import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.beans.factory.annotation.Autowired
import org.junit._
import org.springframework.test.context.ContextConfiguration
import ru.sosgps.wayrecall.billing.finance.MonthlyPaymentService
import ru.sosgps.wayrecall.utils.web.ScalaJson
import collection.JavaConversions.mapAsScalaMap
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.core.MongoDBManager
import scala.collection
import com.mongodb.casbah.commons.MongoDBObject
import java.util
import java.util.{Calendar, Date}
import com.mongodb.casbah.commons.ValidBSONType.ObjectId
import com.mongodb.casbah.commons.TypeImports.ObjectId
import org.joda.time.{DateTime, DateTimeUtils}
import com.mongodb.casbah.Imports
import ru.sosgps.wayrecall.utils.tryLong
import ru.sosgps.wayrecall.core.finance.{FeeProcessor, TariffPlans}

import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConversions.collectionAsScalaIterable

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/fee-test-spring.xml"))
class FeeTest extends grizzled.slf4j.Logging {

  @Autowired
  var feeProc: FeeProcessor = null

  @Autowired
  var tariff: TariffPlans = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var service: MonthlyPaymentService = null

  @PostConstruct
  def init() {

    mdbm.getDatabase().dropDatabase()


    val insMap = Map(
      "/max2014objects.json" -> "objects",
      "/max2014eqipments.json" -> "equipments",
      "/max2014.json" -> "accounts",
      "/max2014tariff.json" -> "tariffs"
    )

    for ((file, coll) <- insMap; map <- readJsonDump(file))
      mdbm.getDatabase()(coll).insert(map)


  }


  def readJsonDump(jsonObject: String): Seq[collection.Map[String, AnyRef]] = {
    val resourceAsStream = this.getClass.getResourceAsStream(jsonObject)
    val maps = try {
      ScalaJson.parse[Seq[java.util.Map[String, AnyRef]]](resourceAsStream).map(convertToObjectIds)
    }
    finally resourceAsStream.close()
    maps
  }

  def convertToObjectIds(arg: java.util.Map[String, AnyRef]): scala.collection.Map[String, AnyRef] = {
    arg.mapValues({
      case m: java.util.Map[String, AnyRef] if m.get("$oid") != null => new ObjectId(m.get("$oid").asInstanceOf[String])
      case e => e
    })
  }

  @Before
  def clearHistories() {
    mdbm.getDatabase()("balanceHistory").drop()
    mdbm.getDatabase()("balanceHistoryWithDetails").drop()
  }

  //  @Test
  //  def test() {
  //    val (acc, cost) = totalCost()
  //    val initialBalance = tryLong(acc("balance"))
  //
  //    feeProc.dailySubtract()
  //
  //    val calendar = new util.GregorianCalendar()
  //    calendar.setTime(new Date())
  //    val dailyPay = cost / calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
  //
  //    val list1 = mdbm.getDatabase()("balanceHistory").find(MongoDBObject("account" -> new ObjectId("519cce34e4b0f612af8536ba"))).toList
  //    Assert.assertEquals(1, list1.size)
  //    Assert.assertEquals(-dailyPay, list1.head.as[Long]("ammount"))
  //    Assert.assertEquals(-dailyPay, list1.head.as[Long]("ammount"))
  //
  //    val (acc2, _) = totalCost()
  //    Assert.assertEquals(initialBalance - dailyPay, tryLong(acc2("balance")))
  //  }

  @Test
  def test2() {
    val (acc, cost) = totalCost()

    for (day <- 1 to 31) {
      val now = new DateTime(2014, 1, day, 5, 2)
      DateTimeUtils.setCurrentMillisFixed(now.getMillis)
      feeProc.dailySubtractWithDetails()
    }

    val list1 = mdbm.getDatabase()("balanceHistoryWithDetails").find(MongoDBObject("account" -> new ObjectId("519cce34e4b0f612af8536ba"))).toList
    Assert.assertEquals(31, list1.size)
    Assert.assertEquals(-cost, list1.map(_.as[Long]("ammount")).sum)

    debug("list1=" + list1)

    val request = new ExtDirectStoreReadRequest()
    request.setParams(Map(
      "accountId" -> acc.as[ObjectId]("_id").toString,
      "month" -> 0.asInstanceOf[AnyRef]
    ))
    val result = service.read(request)



    val records = result.getRecords.toIterator.toSeq
    //debug("result==n" + records.mkString(",\n"))

    debug("r=" + records.map({
      case m: Map[_, _] => "Map(\n" + m.map(kv => "\"" + kv._1 + "\" -> " + (kv._2 match {
        case s: String => "\"" + s + "\""
        case e => e
      })).mkString(",\n") + "\n)"
      case e => e
    }).mkString(",\n"))

    Assert.assertEquals(Seq(
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o1263573357717097435",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е916еах50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o669221619608807459",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е735ах50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o602823400608177503",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е219ах50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o3254293393386804630",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е734ах50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o3308312517575202681",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е091ах50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o3561867402665313515",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е145ах50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o948019710797165603",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е089ах50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o1250109402769888533",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е112ах50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o2715173526670318020",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е217ах50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o2875232062468230992",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "т413во50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o433953336215617824",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е140ах50"
      ),
      Map(
        "firstDate" -> "2014-01-01T05:02:00.000+04:00",
        "cost" -> Some(100),
        "lastDate" -> "2014-01-31T05:02:00.000+04:00",
        "withdraw" -> 10000.0,
        "uid" -> "o3439179039349036975",
        "eqtype" -> "Основной абонентский терминал",
        "objectName" -> "е123ах50"
      ),
      Map(
        "objectName" -> "Дополнительная услуга",
        "eqtype" -> "Услуга1",
        "cost" -> 120
      )), records)


  }

  //  @Test
  //  @Ignore
  //  def test3() {
  //    val (acc, cost) = totalCost()
  //
  //    for (day <- 1 to 31) {
  //      val now = new DateTime(2014, 1, day, 5, 2)
  //      DateTimeUtils.setCurrentMillisFixed(now.getMillis)
  //      feeProc.dailySubtract()
  //    }
  //
  //    val list1 = mdbm.getDatabase()("balanceHistory").find(MongoDBObject("account" -> new ObjectId("519cce34e4b0f612af8536ba"))).toList
  //    Assert.assertEquals(31, list1.size)
  //    Assert.assertEquals(-cost, list1.map(_.as[Long]("ammount")).sum)
  //  }


  def totalCost(): (Imports.DBObject, Long) = {
    val list = mdbm.getDatabase()("accounts").find(MongoDBObject.empty, MongoDBObject("_id" -> 1, "plan" -> 1, "balance" -> 1)).toList
    Assert.assertEquals(1, list.size)

    val cost = tariff.calcTotalCost(list.head)
    Assert.assertEquals(132000, cost)
    (list.head, cost)
  }
}
