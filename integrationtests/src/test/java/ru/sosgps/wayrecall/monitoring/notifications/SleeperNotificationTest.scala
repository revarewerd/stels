
package ru.sosgps.wayrecall.monitoring.notifications

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{GPSDataConversions, MongoDBManager}
import ru.sosgps.wayrecall.events.{PredefinedEvents, EventsStore}
import org.bson.types.ObjectId
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.monitoring.web.MockSmsGate
import ru.sosgps.wayrecall.utils.EventsTestUtils
import ru.sosgps.wayrecall.utils.io.DboReader
import java.util.zip.GZIPInputStream
import com.mongodb.casbah.Imports._
import scala.Some
import EventsTestUtils._
import scala.Some
import ru.sosgps.wayrecall.data.{UserMessage, GPSEvent}
import java.util.Date
import org.joda.time.DateTime

import scala.collection.JavaConversions.iterableAsScalaIterable

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class SleeperPowerExtNotificationTest extends NotificationTestCommon {

  @Autowired
  var smg: SleeperNotificationDetector = null

  val title = "notifications.sleeper"

  val powerMess = "Отключение внешнего питания"

  val rule: Map[String, Any] = Map(
    "_id" -> new ObjectId(),
    "name" -> "Тест спящих",
    "email" -> "",
    "params" -> Map(
      "ntfSlprExtPower" -> true
    ),
    "showmessage" -> true,
    "objects" -> List(
      objUid
    ),
    "type" -> "ntfSlpr",
    "user" -> user,
    "messagemask" -> "Отключение внешнего питания",
    "phone" -> "+79164108305"
  )


  @Test
  def test1() {

    val smstexts = """
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E GSM:-94dBm T=33.0C Bat=4.2V-99.4% Ex_Batt=11.4V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Moving GSM:-94dBm T=45.5C Bat=4.2V-100.0% Ex_Batt=11.4V
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 Alarm: Light-Input GSM:-56dBm T=18.0C Bat=5.8V-89.1% M=1 #34
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Moving GSM:-94dBm T=46.0C Bat=4.3V-100.0% Ex_Batt=11.5V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E GSM:-94dBm T=44.0C Bat=4.2V-100.0% Ex_Batt=11.4V
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 Alarm: Light-Input GSM:-56dBm T=18.0C Bat=5.8V-89.1% M=1 #34
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Moving GSM:-94dBm T=45.5C Bat=4.2V-100.0% Ex_Batt=11.5V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Low Ex_Batt GSM:-94dBm T=45.0C Bat=3.7V-30.3% Ex_Batt=2.1V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=2A0 CID=3270 Alarm: Low Inter_Batt Low Ex_Batt GSM:-94dBm T=43.5C Bat=3.6V-21.1% Ex_Batt=0.3V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Low Inter_Batt Low Ex_Batt GSM:-94dBm T=42.5C Bat=4.2V-100.0% Ex_Batt=11.3V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E GSM:-94dBm T=44.0C Bat=4.2V-100.0% Ex_Batt=11.4V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E GSM:-94dBm T=45.5C Bat=4.2V-100.0% Ex_Batt=11.4V
                   """.stripMargin


    precleanDatabase()

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)

    val dates = Stream.iterate(new DateTime(1390478736000L))(_.plusMinutes(2)).map(_.toDate)

    for ((smstext, date) <- smstexts.lines zip dates.iterator) {
      //smg.emulateReceivingSms(phone, smstext)
      smg.emulateSms(phone, smstext, date)
    }


    val aggregatedEventsAfter = aggregatedEvents.toIndexedSeq

    for (userMessage <- aggregatedEventsAfter) {
      println(new Date(userMessage.time) + " " + userMessage)
    }
    
    Assert.assertEquals(new UserMessage(
      user,
      powerMess,
      title,
      Some(objUid),
      None,
      1390479696000L, Map("readStatus" -> false)), aggregatedEventsAfter(0))

    Assert.assertEquals(3, aggregatedEventsAfter.size)

  }
}


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class SleeperMovementNotificationTest extends NotificationTestCommon {

  @Autowired
  var smg: SleeperNotificationDetector = null

  val title = "notifications.sleeper"

  val powerMess = "Зарегистрировано перемещение."

  val rule: Map[String, Any] = Map(
    "_id" -> new ObjectId(),
    "name" -> "Тест спящих",
    "email" -> "",
    "params" -> Map(
      "ntfSlprMovement" -> true
    ),
    "showmessage" -> true,
    "objects" -> List(
      objUid
    ),
    "type" -> "ntfSlpr",
    "user" -> user,
    "messagemask" -> "Зарегистрировано перемещение.",
    "phone" -> "+79164108305"
  )


  @Test
  def test1() {

    val smstexts = """
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E GSM:-94dBm T=33.0C Bat=4.2V-99.4% Ex_Batt=11.4V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Moving GSM:-94dBm T=45.5C Bat=4.2V-100.0% Ex_Batt=11.4V
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 Alarm: Light-Input GSM:-56dBm T=18.0C Bat=5.8V-89.1% M=1 #34
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Moving GSM:-94dBm T=46.0C Bat=4.3V-100.0% Ex_Batt=11.5V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E GSM:-94dBm T=44.0C Bat=4.2V-100.0% Ex_Batt=11.4V
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 Alarm: Light-Input GSM:-56dBm T=18.0C Bat=5.8V-89.1% M=1 #34
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Moving GSM:-94dBm T=45.5C Bat=4.2V-100.0% Ex_Batt=11.5V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Low Ex_Batt GSM:-94dBm T=45.0C Bat=3.7V-30.3% Ex_Batt=2.1V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=2A0 CID=3270 Alarm: Low Inter_Batt Low Ex_Batt GSM:-94dBm T=43.5C Bat=3.6V-21.1% Ex_Batt=0.3V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Low Inter_Batt Low Ex_Batt GSM:-94dBm T=42.5C Bat=4.2V-100.0% Ex_Batt=11.3V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E GSM:-94dBm T=44.0C Bat=4.2V-100.0% Ex_Batt=11.4V
                     |Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E GSM:-94dBm T=45.5C Bat=4.2V-100.0% Ex_Batt=11.4V
                   """.stripMargin


    precleanDatabase()

    val aggregatedEvents = collectEvents(es, PredefinedEvents.userMessage)

    val dates = Stream.iterate(new DateTime(1390478736000L))(_.plusMinutes(2)).map(_.toDate)

    for ((smstext, date) <- smstexts.lines zip dates.iterator) {
      //smg.emulateReceivingSms(phone, smstext)
      smg.emulateSms(phone, smstext, date)
    }


    val aggregatedEventsAfter = aggregatedEvents.toIndexedSeq

    for (userMessage <- aggregatedEventsAfter) {
      println(new Date(userMessage.time) + " " + userMessage)
    }

    Assert.assertEquals(3, aggregatedEventsAfter.size)

    val movingMess = "Зарегистрировано перемещение."
    Assert.assertEquals(new UserMessage(
      user,
      movingMess,
      title,
      Some(objUid),
      None,
      1390478976000L, Map("readStatus" -> false)), aggregatedEventsAfter(0))

    Assert.assertEquals(new UserMessage(
      user,
      movingMess,
      title,
      Some(objUid),
      None,
      1390479216000L, Map("readStatus" -> false)), aggregatedEventsAfter(1))

    Assert.assertEquals(new UserMessage(
      user,
      movingMess,
      title,
      Some(objUid),
      None,
      1390479576000L, Map("readStatus" -> false)), aggregatedEventsAfter(2))

  }
}


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-notification-test.xml"))
class SleeperLightInputNotificationTest extends NotificationTestCommon with grizzled.slf4j.Logging{

  @Autowired
  var smg: SleeperNotificationDetector = null

  val title = "notifications.sleeper"

  val rule: Map[String, Any] = Map(
    "_id" -> new ObjectId(),
    "name" -> "Тест света спящих",
    "email" -> "",
    "params" -> Map(
      "ntfSlprLightInput" -> true
    ),
    "showmessage" -> true,
    "objects" -> List(
      objUid
    ),
    "type" -> "ntfSlpr",
    "user" -> user,
    "messagemask" -> "Сработал фотодатчик",
    "phone" -> "+79164108305"
  )


  @Test
  def test1() {

    val smstexts = """
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 Alarm: Light-Input GSM:-58dBm T=25.0C Bat=6.3V-100.0% M=1 #17
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=322,CID=5192,-61dBm LAC1=FFFF,CID=FFFF,-63dBm LAC2=FFFF,CID=FFFF,-63dBm LAC3=FFFF,CID=FFFF,-63dBm GSM:-58dBm T=25.0C Bat=6.3V-100.0% M=1 #18
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=322,CID=5195,-58dBm LAC1=322,CID=FFFF,-50dBm LAC2=322,CID=5194,-67dBm LAC3=322,CID=5196,-70dBm GSM:-56dBm T=35.0C Bat=5.7V-83.3% M=1 #19
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 Alarm: Light-Input GSM:-56dBm T=19.5C Bat=6.0V-97.9% M=1 #20
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=32C,CID=DD5,-48dBm LAC1=FFFF,CID=FFFF,-63dBm LAC2=FFFF,CID=FFFF,-63dBm LAC3=FFFF,CID=FFFF,-63dBm GSM:-56dBm T=21.5C Bat=5.8V-88.4% M=1 #21
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 Alarm: Light-Input GSM:-56dBm T=18.5C Bat=5.8V-90.7% M=1 #22
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=32C,CID=DD5,-48dBm LAC1=FFFF,CID=FFFF,-63dBm LAC2=FFFF,CID=FFFF,-63dBm LAC3=FFFF,CID=FFFF,-63dBm GSM:-56dBm T=19.0C Bat=5.8V-89.0% M=1 #23
                     |Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 Alarm: Light-Input GSM:-56dBm T=18.0C Bat=5.8V-90.2% M=1 #24
                     | """.stripMargin


    precleanDatabase()

    val aggregatedEvents = collectEventsQueue(es, PredefinedEvents.userMessage)

    val dates = Stream.iterate(new DateTime(new DateTime(1390478736000L)))(_.plusMinutes(2)).map(_.toDate)

    for ((smstext, date) <- smstexts.lines zip dates.iterator) {
      //smg.emulateReceivingSms(phone, smstext)
      smg.emulateSms(phone, smstext, date)
    }

    EventsTestUtils.waitUntil(aggregatedEvents.size >= 4, 10000)
    val aggregatedEventsAfter = aggregatedEvents.filter(_.subjObject == Some("o16177799108917461") ).toIndexedSeq

    for (userMessage <- aggregatedEventsAfter) {
      debug(new Date(userMessage.time) + " " + userMessage)
    }

    Assert.assertEquals(4, aggregatedEventsAfter.size)

    val messTest = "Сработал фотодатчик"
    Assert.assertEquals(new UserMessage(
      user,
      messTest,
      title,
      Some(objUid),
      None,
      1390478856000L, Map("readStatus" -> false)), aggregatedEventsAfter(0))

    Assert.assertEquals(new UserMessage(
      user,
      messTest,
      title,
      Some(objUid),
      None,
      1390479216000L, Map("readStatus" -> false)), aggregatedEventsAfter(1))

    Assert.assertEquals(new UserMessage(
      user,
      messTest,
      title,
      Some(objUid),
      None,
      1390479456000L, Map("readStatus" -> false)), aggregatedEventsAfter(2))

    Assert.assertEquals(new UserMessage(
      user,
      messTest,
      title,
      Some(objUid),
      None,
      1390479696000L, Map("readStatus" -> false)), aggregatedEventsAfter(3))


  }
}


trait NotificationTestCommon {

  val user = "1234"

  val objUid = "o16177799108917461"

  val phone = "79167359253"

  val obj = Map(
    "VIN" -> "",
    //"_id" -> ObjectId("5284720d0cf2746561f423e3"),
    "account" -> new ObjectId("51e6ce51e4b0be775cae8762"),
    "comment" -> "Задонский проезд 32 б (С-1)",
    "fuelPumpLock" -> false,
    "gosnumber" -> "",
    "ignitionLock" -> false,
    "marka" -> "Задонская",
    "model" -> "С-1",
    "name" -> "Задонская 1",
    "objnote" -> "",
    "subscriptionfee" -> "",
    "type" -> "Автомобиль",
    "uid" -> objUid
  )

  val equipment = Map(
    // "_id" -? ObjectId("528471fc0cf2746561f423e2"),
    "accountId" -> new ObjectId(),
    "eqFirmware" -> "",
    "eqIMEI" -> "351802052563982",
    "eqLogin" -> "",
    "eqMark" -> "Gosafe",
    "eqModel" -> "1000",
    "eqNote" -> "",
    "eqOwner" -> "Андрей и Евгения машины из Рича",
    "eqPass" -> "445754",
    "eqRightToUse" -> "собственность",
    "eqSellDate" -> "2013-11-13T00:00:00",
    "eqSerNum" -> "",
    "eqWork" -> "Монтаж Конов",
    "eqWorkDate" -> "2013-11-13T00:00:00",
    "eqtype" -> "Спящий блок автономного типа GSM",
    "instPlace" -> "",
    "simICCID" -> "89701010065006798712",
    "simNote" -> "привязка к IMEI",
    "simNumber" -> phone,
    "simOwner" -> "КСБ Стелс",
    "simProvider" -> "МТС",
    "uid" -> objUid
  )

  val rule: Map[String, Any]

  def precleanDatabase() {
    mdbm.getDatabase()("equipments").remove(MongoDBObject("uid" -> objUid))
    mdbm.getDatabase()("equipments").remove(MongoDBObject("simNumber" -> phone))
    mdbm.getDatabase()("objects").remove(MongoDBObject("uid" -> objUid))
    mdbm.getDatabase()("objects").insert(obj)
    mdbm.getDatabase()("equipments").insert(equipment, WriteConcern.Safe)
    mdbm.getDatabase()("smses").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRulesStates").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRules").remove(MongoDBObject.empty)
    mdbm.getDatabase()("notificationRules").insert(rule)
  }

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var es: EventsStore = null


}
