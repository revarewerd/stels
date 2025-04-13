package ru.sosgps.wayrecall.billing.tariff

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import com.novus.salat._
import com.novus.salat.annotations.Ignore
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.utils.salat_context._

/**
  * Created by IVAN on 15.03.2017.
  */
@Repository
class TariffDAO @Autowired()(mdbm: MongoDBManager) extends grizzled.slf4j.Logging {

  private val tariffsCollection: MongoCollection = mdbm.getDatabase()("tariffs")
  tariffsCollection.createIndex(MongoDBObject("name" -> 1), MongoDBObject("unique" -> true))

  //TODO: Должны исползоваться  AccountDAO и UserDAO
  private val accountsCollection: MongoCollection = mdbm.getDatabase()("accounts")
  private val usersCollection: MongoCollection = mdbm.getDatabase()("users")

  private def dBObjectToTariff(dBObject: DBObject): Tariff = {
    val mhlh = dBObject.getAsOrElse[Long]("messageHistoryLengthHours", -1)
    grater[Tariff].asObject(dBObject).copy(messageHistoryLengthType = identifyTariffLengthType(mhlh))
  }

  private def tariffToDBObject(tariff: Tariff): DBObject = {
    grater[Tariff].asDBObject(tariff)
  }

  private def identifyTariffLengthType(tariffLengthHours: Long): String = {
    tariffLengthHours match {
      case (a: Long) if a < 0 => TariffType.UNLIMITED
      case 4 => TariffType.FOUR_HOURS
      case 24 => TariffType.ONE_DAY
      case 168 => TariffType.ONE_WEEK
      case 744 => TariffType.ONE_MONTH
      case 2208 => TariffType.THREE_MONTHS
      case 4416 => TariffType.SIX_MONTHS
      case 8784 => TariffType.ONE_YEAR
      case _ => TariffType.CUSTOM
    }
  }

  def loadOne(id: String): Option[Tariff] = {
    loadOne(new ObjectId(id))
  }

  def loadOne(id: ObjectId): Option[Tariff] = {
    tariffsCollection.findOne(MongoDBObject("_id" -> id)).map(dBObjectToTariff)
  }

  def loadMany(request: DBObject = MongoDBObject.empty): Iterator[Tariff] = {
    tariffsCollection.find().sort(MongoDBObject("name" -> 1)).map(dBObjectToTariff)
  }

  def remove(id: ObjectId) {
    tariffsCollection.remove(MongoDBObject("_id" -> id), concern = WriteConcern.Acknowledged)
  }

  def update(tariff: Tariff) {
    val supportRequestDBO = tariffToDBObject(tariff) - "id"
    tariffsCollection.update(MongoDBObject("_id" -> tariff._id), $set(supportRequestDBO.toSeq: _*),
      concern = WriteConcern.Acknowledged)
  }

  def update(id: ObjectId, changes: Map[String, Any]) {
    tariffsCollection.update(MongoDBObject("_id" -> id), $set(changes.toSeq: _*),
      concern = WriteConcern.Acknowledged)
  }

  def insert(tariff: Tariff) {
    tariffsCollection.insert(tariffToDBObject(tariff), WriteConcern.Acknowledged)
  }

  //TODO: Метод должен быть в AccountDAO
  def getAvailableTariffIds(permAccs: Set[ObjectId]): Set[ObjectId] = {
    accountsCollection.find("_id" $in permAccs, MongoDBObject("plan" -> 1)).map(plan => new ObjectId(plan.getAs[String]("plan").get)).toSet
  }

  //TODO: Метод должен быть в AccountDAO
  def accountsCountWithTariff(tariffId: String): Int = {
    accountsCollection.find(MongoDBObject("plan" -> tariffId)).count()
  }

  //TODO: Метод должен быть в UserDAO
  def getUserMainAccTariff(userId: ObjectId): Option[Tariff] = {
    val mainAccIdOpt = usersCollection.findOne(MongoDBObject("_id" -> userId), MongoDBObject("mainAccId" -> 1))
      .flatMap(dbo => dbo.getAs[ObjectId]("mainAccId"))
    mainAccIdOpt match {
      case Some(mainAccId: ObjectId) =>
        val tariffIdOpt = accountsCollection.findOne(MongoDBObject("_id" -> mainAccIdOpt.get), MongoDBObject("plan" -> 1))
          .flatMap(dbo => dbo.getAs[String]("plan"))
        tariffIdOpt match {
          case Some(tariffId: String) => tariffsCollection.findOne(MongoDBObject("_id" -> new ObjectId(tariffId))).map(dBObjectToTariff)
          case _ => None
        }
      case _ => None
    }
  }

  def isDefault(id: ObjectId): Boolean = {
    loadOne(id.toString) match {
      case None => false
      case Some(t: Tariff) => t.default
    }
  }

  def getDefaultTariff: Option[Tariff] = {
    tariffsCollection.findOne(MongoDBObject("default" -> true)).map(dBObjectToTariff)
  }
}


case class Tariff(_id: ObjectId, name: String, comment: String = "", abonentPrice: List[Map[String, Any]] = List.empty,
                  additionalAbonentPrice: List[Map[String, Any]] = List.empty, servicePrice: List[Map[String, Any]] = List.empty,
                  hardwarePrice: List[Map[String, Any]] = List.empty, messageHistoryLengthHours: Long = -1, default: Boolean = false,
                  @Ignore messageHistoryLengthType: String = "")

object TariffType {
  final val UNLIMITED = "unlimited"
  final val FOUR_HOURS = "fourHours"
  final val ONE_DAY = "oneDay"
  final val ONE_WEEK = "oneWeek"
  final val ONE_MONTH = "oneMonth"
  final val THREE_MONTHS = "threeMonths"
  final val SIX_MONTHS = "sixMonths"
  final val ONE_YEAR = "oneYear"
  final val CUSTOM = "custom"
}