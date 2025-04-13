package ru.sosgps.wayrecall.data

import javax.annotation.Resource

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.security.{PermissionValue, ObjectsPermissionsChecker}
import ru.sosgps.wayrecall.utils.errors.NotPermitted
import scala.beans.BeanProperty
import ru.sosgps.wayrecall.security.PermissionsManager

/**
 * Created by nickl on 04.07.14.
 */
trait DBPacketsWriter {
  @throws(classOf[IllegalImeiException])
  def addToDb(gpsdata: GPSData): Boolean

  def updateGeogata(gpsdata: GPSData, placeName: String)
}

class IllegalImeiException(@BeanProperty val imei: String) extends Exception("no objects for imei=" + imei)

class DirectMongoDBPacketsWriter extends DBPacketsWriter with MultiObjectsPacketsMixin with grizzled.slf4j.Logging {

  @Autowired
  var mongoDbManager: MongoDBManager = null;

  @Autowired
  var or: ObjectsRepositoryReader = null;

  @BeanProperty
  var writeSafety = WriteConcern.Safe

  @throws(classOf[IllegalImeiException])
  def addToDb(gpsdata: GPSData) = {
    addToDbDirect(gpsdata: GPSData)
    true
  }

  def addToDbDirect(gpsdata: GPSData) {

    val uidOpt = getTargetCollection(gpsdata)

    uidOpt match {
      case Some(coll) => {
        val mdb: DBObject = GPSDataConversions.toMongoDbObject(gpsdata)
        if (gpsdata.placeName != null) {
          mdb.put("pn", gpsdata.placeName)
        }
        coll.insert(mdb, WriteConcern.Normal)
      }
      case None => throw new IllegalImeiException(gpsdata.imei)
    }

  }


  def updateGeogata(gpsdata: GPSData, placeName: String) {

    getTargetCollection(gpsdata).get.update(
      MongoDBObject("time" -> gpsdata.time),
      $set("pn" -> placeName), false, true, writeSafety
    )
    //println("gpsdata.uid=" + gpsdata.uid + " gpsdata.time=" + gpsdata.time + " placeName=" + placeName)
    gpsdata.placeName = placeName
    trace("(" + gpsdata.uid + "," + gpsdata.time + ")=" + placeName)

  }
}

class PermissionCheckedDBPacketsWriter(
                                        wrapped: DBPacketsWriter
                                        ) extends DBPacketsWriter {
  @Autowired
  var permissions: UserRolesChecker = _

  @throws(classOf[IllegalImeiException])
  override def addToDb(gpsdata: GPSData) = {
    if(permissions.checkAdminAuthority()) wrapped.addToDb(gpsdata)
    else throw new NotPermitted("Недостаточно прав для выполнения операции ")
  }

  override def updateGeogata(gpsdata: GPSData, placeName: String): Unit = {
    if(permissions.checkAdminAuthority()) wrapped.updateGeogata(gpsdata, placeName)
    else throw new NotPermitted("Недостаточно прав для выполнения операции")
  }
}
