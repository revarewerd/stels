package ru.sosgps.wayrecall.web.api

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation._
import ru.sosgps.wayrecall.core.GPSUtils._
import ru.sosgps.wayrecall.core.ObjectDataConversions.{getFuelData, getGPSDataSensors}
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.monitoring.notifications.OneSignalPlayerIdStorage
import ru.sosgps.wayrecall.utils.DBQueryUtils.notRemoved
import ru.sosgps.wayrecall.utils._

import java.util.Date
import scala.collection.{Iterable, mutable}

@RestController
class ObjectsEndpoint(@Autowired roleChecker: UserRolesChecker,
                      @Autowired mdbm: MongoDBManager,
                      @Autowired store: PackagesStore,
                      @Autowired or: ObjectsRepositoryReader,
                      @Autowired playerIdStorage: OneSignalPlayerIdStorage
                     ) extends grizzled.slf4j.Logging {


  @RequestMapping(Array("/getobjectsdata"))
  def getAllObjectsData(@RequestParam(value = "uid", required = false) uids0: Array[String]) = {

    val name = roleChecker.getUserName
    debug(s"getting for $name")
    val uids = uids0.?.getOrElse(Array.empty[String])
    val permittedIds = roleChecker.getInferredPermissionRecordIds(name, "object").toSet
    val ids: Iterable[ObjectId] = if (uids.isEmpty) permittedIds
    else uids.toSeq.map(or.objectIdByUid(_)).filter(permittedIds(_))
    debug("ids = " + ids)
    val objects = mdbm.getDatabase().apply("objects").find((notRemoved ++ ("_id" $in ids)))

    objects.map(d => {
      val uid = d.as[String]("uid")
      val latest = store.getLatestFor(Iterable(uid)).headOption
      val blockingEnabled = d.getOrElse("ignitionLock", false) == true ||
        d.getOrElse("fuelPumpLock", false) == true
      Map(
        "uid" -> uid,
        "name" -> d.get("name"),
        "customName" -> d.get("customName"),
        "gosnumber" -> d.get("gosnumber"),
        "position" -> latest.map(postitionData).orNull,
        "blocked" -> d.getOrElse("blocked", false),
        "blockingEnabled" -> blockingEnabled
      )
    })
  }

  private def postitionData(l: GPSData) =
    OptMap(
      "imei" -> l.imei,
      "place" -> l.placeName,
      "sat" -> l.satelliteNum,
      "lon" -> l.lon,
      "lat" -> l.lat,
      "time" -> l.time,
      "speed" -> l.speed,
      "course" -> l.course,
      "ignition" -> GPSUtils.detectIgnition(l),
      "canFuelLevel" -> getFuelData("can", l, mdbm).round,
      "flsFuelLevel" -> getFuelData("fls", l, mdbm).round,
      "extPower" -> detectPowerExt(l).getOrElse(null),
      "canCoolantTemp" -> detectCANCoolantTemp(l).getOrElse(null),
      "canRPM" -> detectCANRPM(l).getOrElse(null),
      "canAccPedal" -> detectCANAccPedal(l).getOrElse(null),
      "sensorsData" -> getGPSDataSensors(l, mdbm),
      "stateLatency" -> getStateLatency(l).getOrElse(false)
    )


  @RequestMapping(Array("/getPositions"))
  def getPositions(@RequestParam(value = "uid") uid: String,
                   @RequestParam("dateFrom") dateFrom: Date,
                   @RequestParam("dateTo") dateTo: Date
                  ) = {
    store.getHistoryFor(uid, dateFrom, dateTo).map(postitionData)
  }

  def getUserInfo = mdbm.getDatabase().apply("users").findOne(Map("name" -> roleChecker.getUserName()))

  @RequestMapping(Array("/user/balance"))
  def getBalance = {
    getUserInfo.map(userInfo =>
      if (java.lang.Boolean.TRUE == userInfo.get("showbalance"))
        Map("showbalance " -> true, "balance" ->
          userInfo.getAs[ObjectId]("mainAccId").flatMap(mainAccId =>
            mdbm.getDatabase().apply("accounts").findOneByID(mainAccId).flatMap(_.getAs[Any]("balance"))
          ).orNull
        )
      else
        Map("showbalance " -> false)
    )
  } 
  
  @RequestMapping(Array("/user/onesignal/player/register"))
  def addOneSignalPlayerId(@RequestParam(value = "id") playerId: String) = {
    playerIdStorage.assocUserId(roleChecker.getUserName, playerId)
  }


  @RequestMapping(Array("/getgroups"))
  def getGroups() = {
    val name = roleChecker.getUserName
    debug(s"getting for $name")
    val usedId = roleChecker.getCurrentUserId()
    val permittedUids = new mutable.HashSet[String]() ++= roleChecker.getInferredPermissionRecordIds(name, "object").map(or.uidByObjectId(_))
    val objectGroups = mdbm.getDatabase()("groupsOfObjects").find(MongoDBObject("uid" -> usedId))

    val groupsAndObjects = new mutable.HashMap[String, mutable.Iterable[String]]()
    val objectsInGoups = new mutable.HashSet[String]()
    for (group <- objectGroups) {
      val objectsInGroup = group.as[MongoDBList]("objects").map(objDbo => objDbo.asInstanceOf[DBObject].as[String]("uid")).filter(permittedUids(_))
      objectsInGoups ++= objectsInGroup
      groupsAndObjects += (group.as[String]("name") -> objectsInGroup)
    }
    val remainingObjects = permittedUids -- objectsInGoups

    if (remainingObjects.nonEmpty)
      groupsAndObjects += ("<no-group>" -> remainingObjects)

    groupsAndObjects
  }


}
