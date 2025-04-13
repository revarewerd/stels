package ru.sosgps.wayrecall.monitoring.processing.parkings

import com.mongodb.casbah.commons.MongoDBObject
import ru.sosgps.wayrecall.core.MongoDBManager
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils._
/**
  * Created by ivan on 17.07.17.
  */
class ObjectTripSettings(uid: String, mongo: MongoDBManager) {
  require(mongo != null)

  val settingsDbo = mongo.getDatabase()("objects")
                   .findOne(MongoDBObject("uid" -> uid)).flatMap(_.getAs[DBObject]("settings")).getOrElse(new BasicDBObject)

  lazy val minMovementSpeed = tryInt(settingsDbo.getOrElse("repTripsMinMovementSpeed",1))
  lazy val minParkingTime = tryInt(settingsDbo.getOrElse("repTripsMinParkingTime",300))
  lazy val maxDistanceBetweenMessages = tryInt(settingsDbo.getOrElse("repTripsMaxDistanceBetweenMessages",10000))
  lazy val minTripTime = tryInt(settingsDbo.getOrElse("repTripsMinTripTime",180))
  lazy val minTripDistance = tryInt(settingsDbo.getOrElse("repTripsMinTripDistance",400))
  lazy val useIgnitionToDetectMovement = settingsDbo.getAsOrElse[Boolean]("repTripsUseIgnitionToDetectMovement",false)
  lazy val minSatelliteNum = tryInt(settingsDbo.getOrElse("repTripsMinSatelliteNum",3))
}
