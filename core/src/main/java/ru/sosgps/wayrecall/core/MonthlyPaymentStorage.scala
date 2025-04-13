package ru.sosgps.wayrecall.core

import com.mongodb.casbah
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date
import org.joda.time.DateTime
import com.mongodb.casbah.Imports
import scala.collection.immutable
import com.mongodb.casbah.Imports._


@Component
class MonthlyPaymentStorage extends grizzled.slf4j.Logging {

  def this(mdbm: MongoDBManager) = {
    this()
    this.mdbm = mdbm
  }

  @Autowired
  var mdbm: MongoDBManager = null

  def getDateInterval(monthShift: Int): (Date, Date) = {
    val fdtm = new DateTime().withDayOfMonth(1)
    debug("fdtm=" + fdtm)
    val fromDateTime = fdtm.plusMonths(monthShift).withDayOfMonth(1).withTime(0, 0, 0, 0)
    debug("fromDateTime=" + fromDateTime)
    val fromDate = fromDateTime.toDate
    val toDate = fromDateTime.withDayOfMonth(fromDateTime.dayOfMonth().getMaximumValue).withTime(23, 59, 59, 999).toDate
    debug("toDate=" + toDate)
    (fromDate, toDate)
  }

  def loadEquipmentDaylyPriceShiftedMonth(accId: Imports.ObjectId, monthShift: Int) = {
    val (fromDate, toDate) = getDateInterval(monthShift)
    loadEquipmentDaylyPrices(accId, fromDate, toDate)
  }

  /**
   * @return Map with uid keys of maps with equipment id keys and each day payment values
   */
  def loadEquipmentDaylyPrices(accId: Imports.ObjectId, fromDate: Date, toDate: Date):
  Map[String, Map[Imports.ObjectId, immutable.IndexedSeq[EquipmentDayPriceData]]] = {
    val balanceDetails = mdbm.getDatabase().apply("balanceHistoryWithDetails").find(
      MongoDBObject("account" -> accId, "type" -> "dailypay") ++ ("timestamp" $gte fromDate $lte toDate)
    ).sort(MongoDBObject("timestamp" -> 1))

    balanceDetailsToEqDailyPrice(balanceDetails)
  }


  def balanceDetailsToEqDailyPrice(balanceDetails: casbah.MongoCollection#CursorType):
  Map[String, Map[Imports.ObjectId, immutable.IndexedSeq[EquipmentDayPriceData]]] = {
    balanceDetails.flatMap(daylipay => {
      val time = daylipay.as[Date]("timestamp")
      daylipay.as[DBObject]("details").as[MongoDBList]("objectDetails").flatMap {
        case od: DBObject =>
          val uid = od.as[String]("uid")
          od.as[MongoDBList]("equipments").map {
            case dbo: DBObject =>
              EquipmentDayPriceData(time,
                dbo.as[ObjectId]("_id"),
                dbo.as[String]("eqIMEI"),
                dbo.as[String]("eqtype"),
                dbo.as[Long]("todayCost"),
                uid
              )
          }
      }
    }).toIterable.groupBy(_.objuid)
      .mapValues(
        _.groupBy(_.id).mapValues(_.toIndexedSeq.sortBy(_.time))
      )
  }
}

case class EquipmentDayPriceData(time: Date, id: ObjectId, eqIMEI: String, eqtype: String, todayCost: Long, objuid: String)

