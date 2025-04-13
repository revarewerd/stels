package ru.sosgps.wayrecall.domain.balance

import java.util.Date
import javax.annotation.PostConstruct

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{MongoDBManager, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.utils.salat_context._
import com.mongodb.casbah.Imports._
/**
  * Created by ivan on 29.07.16.
  */


class BalanceHistoryDAO {
  object Type {
    val Dailypay = "dailypay"
    val SMSNotification = "Уведомление по SMS"
  }

  @Autowired
  var mdbm: MongoDBManager = _

  @Autowired
  var or: ObjectsRepositoryReader = _

  var balanceCollection: MongoCollection = _

  @PostConstruct
  def init(): Unit ={
    balanceCollection = mdbm.getDatabase().apply("balanceHistoryWithDetails")
  }

  def getObjectName(uid: String) =
  {
    mdbm.getDatabase()("objects")
      .findOne(MongoDBObject("uid" -> uid), MongoDBObject("customName" -> 1))
      .flatMap(_.getAs[String]("customName"))
      .filter(_ != "")
      .getOrElse(or.getObjectName(uid))
  }

  def getDailyPayHistory(accId: ObjectId, from: Date, to: Date) = {
    balanceCollection.find(MongoDBObject("account" -> accId, "type" -> Type.Dailypay, "timestamp" -> MongoDBObject("$gte" -> from, "$lte" -> to))).sort(MongoDBObject("timestamp" -> 1))
      .map(grater[DailyPayEntry].asObject(_))
  }

  def getSMSNotifications(accId: ObjectId, from: Date, to: Date) = {
    balanceCollection.find(MongoDBObject(
      "account" -> accId,
      "type" -> Type.SMSNotification,
      "timestamp" -> MongoDBObject("$gte" -> from, "$lte" -> to)
    )).sort(MongoDBObject("timestamp" -> 1))
      .map(grater[SMSNotificationPaymentEntry].asObject(_))
  }

}
