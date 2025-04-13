package ru.sosgps.wayrecall.monitoring.notifications.rules

import java.util.Date

import com.mongodb.BasicDBList
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.monitoring.notifications.{Param, Rule}
import ru.sosgps.wayrecall.utils.{tryNumerics, OptMap, typingMap}
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable

import scala.collection.mutable.ArrayBuffer

/**
  * Created by maxim on 08.03.16.
  */
@Rule(id = "ntfBadFuel")
class BadFuelNotificationRule(base: NotificationRule[GPSData], @Param("ntfBadFuelCount") val ntfBadFuelCount: Int
							 ) extends NotificationRule[GPSData](base) with grizzled.slf4j.Logging {

	val fields = Set("Digital_fuel_sensor_B1", "fuel_lvl", "io_2_67", "Fuel_level_%")

	override def process(gps: GPSData, state: scala.collection.Map[String, AnyRef]) = {
		new GpsNotificationStateChange(notificationId, gps, state) {
			var currentCountBadFuel: Int = 0
			var goodPackages: mutable.Buffer[Long] = ArrayBuffer[Long]()

			def lastUnfired = state.getAs[Date]("lastUnfired").getOrElse(new Date(0L))

			currentCountBadFuel = state.getOrElse("currentCountBadFuel", 0).asInstanceOf[Int]
			goodPackages = state.getAs[BasicDBList]("goodPackages").getOrElse(new BasicDBList()).toBuffer.map((a: AnyRef) => a.tryLong)
			if (isBad) {
				currentCountBadFuel += 1
				if (currentCountBadFuel == ntfBadFuelCount) {
					goodPackages.clear()
				}
			}
			if (!isBad) {
				if (!goodPackages.contains(gps.time.getTime)) {
					goodPackages += gps.time.getTime
				}
				if (goodPackages.size == ntfBadFuelCount) {
					currentCountBadFuel = 0
				}
			}


			lazy val fired = {
				currentCountBadFuel == ntfBadFuelCount
			}

			override def unfired: Boolean = {
				goodPackages.size == ntfBadFuelCount
			}

			override def updatedState = {
				val map: Map[String, AnyRef] = Map(
					"prevSpeed" -> data.speed.asInstanceOf[AnyRef],
					"prevTime" -> data.time.asInstanceOf[AnyRef],
					"currentCountBadFuel" -> currentCountBadFuel.asInstanceOf[AnyRef],
					"goodPackages" -> goodPackages.asInstanceOf[AnyRef]
				) ++ OptMap(
					"lastUnfired" -> (if (unfiringNow) Some(data.time) else None)
				)

				super.updatedState ++ map
			}

			lazy val notifications = {
				val mesString = messageMask.map(_.replace("%VALUE%", gps.speed.toString))
					.getOrElse("BadFuelNotificationRule мой мессендж")
				Seq(Notification[GPSData](BadFuelNotificationRule.this, new UserMessage(
					BadFuelNotificationRule.this.user,
					mesString,
					"notifications.badfuelcontrol",
					Some(gps.uid),
					None,
					gps.time.getTime, Map("readStatus" -> false,"lon" -> gps.lon, "lat" -> gps.lat)),
					gps.uid, mesString, gps, gps.time
				))
			}

			def isBad = {
				var isBroken: Boolean = false
				fields.foreach(field => if (gps.data.containsKey(field) && !isBroken) {
					isBroken = gps.data.get(field).tryInt match {
						case v: Int if v >= 65535 || v == 0 => true
						case _ => false
					}
//					debug("field: " + field + " -> " + gps.data.get(field).tryInt + "; isBroken = " + isBroken)
				})
				isBroken
			}
		}
	}
}