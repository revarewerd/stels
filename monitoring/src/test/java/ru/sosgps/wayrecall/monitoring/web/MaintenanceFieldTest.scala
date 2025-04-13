package ru.sosgps.wayrecall.monitoring.web

import java.util.Date

import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.billing.GPSDataExport
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.monitoring.notifications.rules.MaintenanceCriteria._
import ru.sosgps.wayrecall.monitoring.notifications.rules.{MaintenanceNotificationRule, NotificationRule}

import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
 * Created by nickl on 17.07.15.
 */
class MaintenanceFieldTest {

  @Test
  def testDistanceFields() {
    val f = new MaintenanceField[Double](distance)
    val rule = new MaintenanceNotificationRule(new NotificationRule[GPSData](null, null, null, null, null, false, null, null, None), 12.0, true, 0.0, true, 0.0, true)
    val appl = f.applying(rule, Map.empty, None)

    val clientState = Map(appl.fieldsToClient.toSeq: _*)
    println("clientState = " + clientState)
    Assert.assertEquals(12.0, clientState(f.inSettingsIntervalNameDefault).asInstanceOf[Double], 0.001)
    Assert.assertEquals(12.0, clientState(f.inSettingsName).asInstanceOf[Double], 0.001)

  }

  @Test
  def testDistanceSavingDefaults() {
    val f = new MaintenanceField[Double](distance)
    val rule = new MaintenanceNotificationRule(new NotificationRule[GPSData](null, null, null, null, null, false, null, null, None), 12.0, true, 0.0, true, 0.0, true)
    val appl = f.applying(rule, Map.empty, None)

    val clientState = Map(appl.fieldsToClient.toSeq: _*)
    println("clientState = " + clientState)

    val dbState = Map(appl.fieldsToDb(clientState).toSeq: _*)
    println("dbState = " + dbState)
    val appl2 = f.applying(rule, dbState, None)

    val clientState2 = Map(appl2.fieldsToClient.toSeq: _*)
    println("clientState2 = " + clientState2)

    Assert.assertEquals(clientState, clientState2)

  }

  val someGPS = Some({
    val gps = new GPSData("o4475460171288094111",
      "354330031038816",
      37.7490496,
      55.4693312,
      new Date(1432264982981L),
      102,
      330,
      11,
      "Каширское шоссе, Новленское",
      new Date(1432265051334L),
      Map("Digital_Input_Status_1" -> 1,
        "Digital_output_1_state" -> 0,
        "Digital_output_2_state" -> 0,
        "External_Power_Voltage" -> 13716,
        "Internal_Battery_Voltage" -> 8211,
        "alt" -> 153.0,
        "mh" -> 48762655L,
        "protocol" -> "Teltonika",
        "pwr_ext" -> 13.716,
        "tdist" -> 463.5755056826105).mapValues(_.asInstanceOf[AnyRef]).asJava)
    gps.privateData.put("tdist", gps.data.get("tdist"))
    gps
  })

  @Test
  def testDistanceSavingCustom() {
    val f = new MaintenanceField[Double](distance)
    val rule = new MaintenanceNotificationRule(new NotificationRule[GPSData](null, null, null, null, null, false, null, null, None), 12.0, true, 0.0, true, 0.0, true)
    val appl = f.applying(rule,
      Map(
        f.criteria.customFieldName -> 30.0,
        f.criteria.inStateName -> 14.0
      ), someGPS)

    val clientState = Map(appl.fieldsToClient.toSeq: _*)
    println("clientState = " + clientState)

    val dbState = Map(appl.fieldsToDb(clientState).toSeq: _*)
    println("dbState = " + dbState)
    val appl2 = f.applying(rule, dbState, someGPS)

    val clientState2 = Map(appl2.fieldsToClient.toSeq: _*)
    println("clientState2 = " + clientState2)

    Assert.assertEquals(clientState, clientState2)
    Assert.assertEquals(30.0, clientState(f.inSettingsIntervalName).asInstanceOf[Double], 0.001)
    Assert.assertEquals(-419.5755056826105, clientState(f.inSettingsName).asInstanceOf[Double], 0.001)


  }


  @Test
  def testDistanceSavingInOneStep() {
    val f = new MaintenanceField[Double](distance)
    val rule = new MaintenanceNotificationRule(new NotificationRule[GPSData](null, null, null, null, null, false, null, null, None), 12.0, true, 0.0, true, 0.0, true)
    val appl = f.applying(rule, Map.empty, someGPS)

    val dbState = appl.fieldsToDb(Map(
      "distanceInterval" -> 10000,
      "distanceUntil" -> 3
    )).toMap

    println("dbState = " + dbState)

    val clientState = f.applying(rule, dbState, someGPS).fieldsToClient.toMap

    Assert.assertEquals(10000.0, clientState("distanceInterval").asInstanceOf[Double], 0.001)
    Assert.assertEquals(3.0, clientState("distanceUntil").asInstanceOf[Double], 1.0)
  }

}
