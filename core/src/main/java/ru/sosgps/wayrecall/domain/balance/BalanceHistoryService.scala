package ru.sosgps.wayrecall.domain.balance

import java.util.Date

import grizzled.slf4j.Logging
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader

/**
  * Created by ivan on 29.07.16.
  */

// Перенести в сервис лэйер
case class NotificationRecord(user: String, phone: String, fee: Double, time: Date, comment: String)

case class AggregatedDailyPay(objectName: String, eqCount: Int, fullFee: Double, firstDate: String, lastDate: String, equipments: List[DailyPayByEquipment])

case class DailyPayByEquipment(eqType: String, firstDate: Date, lastDate: Date, var withdraw: Double)

case class DailyPayRecord(uid: String, eqId: ObjectId, time: Date, eqType: String, todayCost: Long)


class BalanceHistoryService extends Logging {
  import ru.sosgps.wayrecall.utils._

  @Autowired
  var balanceData: BalanceHistoryDAO = null

  def dateIntervalForMonthShift(monthShift: Int): (Date, Date) = {
    val fdtm = new DateTime().withDayOfMonth(1)
    val fromDateTime = fdtm.plusMonths(monthShift).withDayOfMonth(1).withTime(0, 0, 0, 0)
    val fromDate = fromDateTime.toDate
    val toDate = fromDateTime.withDayOfMonth(fromDateTime.dayOfMonth().getMaximumValue).withTime(23, 59, 59, 999).toDate
    (fromDate, toDate)
  }

  def getAggregatedDailyPay(accId: ObjectId, from: Date, to: Date) = {
    val dailyPayHistory = balanceData.getDailyPayHistory(accId, from, to)

    val records = for(entry <- dailyPayHistory;
                      objectDetails = entry.details.objectDetails;
                      obj <- objectDetails;
                      eq <- obj.equipments)
      yield DailyPayRecord(obj.uid, eq.id, entry.timestamp, eq.eqtype, eq.todayCost)


    val eqRecords = records.toIterable.groupBy(_.uid).mapValues(recList => {
      recList.groupBy(_.eqId).values.map(eqRecList => {
        val head = eqRecList.head
        val eqType = head.eqType
        var firstDate = head.time
        var lastDate = head.time
        var withdraw: Double = head.todayCost

        val tail = eqRecList.tail

        for(eqRec <- tail) {
          firstDate = Ordering[Date].min(firstDate,eqRec.time)
          lastDate = Ordering[Date].max(lastDate,eqRec.time)
          withdraw += eqRec.todayCost
        }

        withdraw /= 100
        DailyPayByEquipment(eqType, firstDate, lastDate, withdraw)
      })
    })

    val aggregated = eqRecords.map{ case(uid, eqData) => {
      AggregatedDailyPay(balanceData.getObjectName(uid), eqData.size, eqData.foldLeft(0.0)(_ + _.withdraw),
        ISODateTime(eqData.map(_.firstDate).min),
        ISODateTime(eqData.map(_.lastDate).max), eqData.toList)
    }}.toList

    aggregated
  }


  def getSMSNotifications(accId: ObjectId, from: Date, to: Date) = {
    balanceData.getSMSNotifications(accId,from, to).map(
      e => NotificationRecord(e.details.user, e.details.phone, -e.amount / 100.0, e.timestamp, e.comment))
  }


}
