package ru.sosgps.wayrecall.billing.tariff

import java.util
import java.util.Date
import javax.annotation.PostConstruct

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.billing.tariff.commands._
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.utils.io.Utils
import ru.sosgps.wayrecall.utils.web.EDSMongoUtils._
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.{ExtDirectService, typingMap}

import scala.collection.JavaConversions.{iterableAsScalaIterable, mapAsJavaMap}
import scala.collection.immutable.HashMap

@ExtDirectService
class TariffEDS extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging with TariffService {

  val model = Model("_id", "name")
  val name = "Tariffs"
  val idProperty = "_id"
  val lazyLoad = false

  @Autowired
  var roleChecker: UserRolesChecker = null

  @Autowired
  var commandGateway: SecureGateway = null

  @Autowired
  var tariffDAO: TariffDAO = null

  @PostConstruct
  def init(): Unit = {
    if (tariffDAO.getDefaultTariff.isEmpty)
      createDefaultTariff()
  }

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadAllObjects(request: ExtDirectStoreReadRequest): Iterator[Tariff] = {
    debug("request=" + request)
    if (roleChecker.hasRequiredStringAuthorities("admin", "TariffPlanView"))
      tariffDAO.loadMany(searchAndFilters(request))
    else {
      tariffDAO.loadMany("_id" $in getAvailableTariffIds)
    }
  }

  def getAvailableTariffIds: Set[ObjectId] = {
    val permAccs = roleChecker.getInferredPermissionRecordIds(roleChecker.getUserName(), "account").toSet
    tariffDAO.getAvailableTariffIds(permAccs)
  }


  @ExtDirectMethod
  def remove(maps: Seq[String]) {
    maps.foreach(tariffId => {
      info("removing tariff plan " + tariffId)
      if (tariffDAO.accountsCountWithTariff(tariffId) > 0) {
        throw new IllegalArgumentException("Удаление невозможно:<br/>Тариф установлен на одной или нескольких учетных записях.")
      }
      else {
        try {
          commandGateway.sendAndWait(new TariffPlanDeleteCommand(new ObjectId(tariffId)), roleChecker.getUserAuthorities(), roleChecker.getUserName())
        }
        catch {
          case e: Exception =>
            throw new RuntimeException("При удалении записи произошла ошибка: " + Option(e.getCause).map(_.getMessage()).getOrElse(e), e)
        }
      }
      tariffId
    }
    )
  }


  @ExtDirectMethod
  def updateTariff(arg: Map[String, Any]): HashMap[String, AnyRef] = {
    debug("updateTariff arg=" + arg)
    val data = HashMap[String, AnyRef](
      "name" -> arg.getAs[String]("name").getOrElse(""),
      "comment" -> arg.getAs[String]("comment").getOrElse(""),
      "abonentPrice" -> arg.as[util.List[util.Map[String, Any]]]("abonentPrice").filter(_.get("cost").asInstanceOf[String].nonEmpty),
      "additionalAbonentPrice" -> arg.as[util.List[util.Map[String, Any]]]("additionalAbonentPrice").filter(_.get("cost").asInstanceOf[String].nonEmpty),
      "servicePrice" -> arg.as[util.List[util.Map[String, Any]]]("servicePrice").filter(_.get("cost").asInstanceOf[String].nonEmpty),
      "hardwarePrice" -> arg.as[util.List[util.Map[String, Any]]]("hardwarePrice").filter(_.get("cost").asInstanceOf[String].nonEmpty),
      "messageHistoryLengthHours" -> Long.box(arg.getAs[String]("messageHistoryLengthHours").getOrElse("-1").toLong)
    )
    val idOpt = Option(ObjectsRepositoryReader.ensureObjectId(arg.getAs[String]("_id").orNull))
    val command = idOpt match {
      case Some(id) => new TariffPlanDataSetCommand(id, data + ("default" -> Boolean.box(tariffDAO.isDefault(id))))
      case None => new TariffPlanCreateCommand(new ObjectId(), data)
    }
    try {
      commandGateway.sendAndWait(command, roleChecker.getUserAuthorities(), roleChecker.getUserName())
    }
    catch {
      case e: org.axonframework.commandhandling.CommandExecutionException => throw new RuntimeException("При сохранении данных произошла ошибка: " + e.getCause.getMessage, e);
    }
    data
  }


  @ExtDirectMethod
  def loadTariff(_id: String) = {
    debug("loadTariff _id=" + _id)
    tariffDAO.loadOne(_id) match {
      case Some(tariff) if (roleChecker.hasRequiredStringAuthorities("admin", "TariffPlanView")
        || getAvailableTariffIds.contains(tariff._id)) => tariff
      case _ => Map.empty
    }
  }

  def loadMainAccTariff(): Option[Tariff] = {
    val userId = roleChecker.getCurrentUserId()
    tariffDAO.getUserMainAccTariff(userId)
  }

  def correctReportWorkingDates(interval: (Date, Date)): (Date, Date) = {
    val (startDate, endDate) = interval
    loadMainAccTariff() match {
      case Some(mainTariff) =>
        val messageHistoryLengthHours = mainTariff.messageHistoryLengthHours
        val startDateRestriction = new DateTime().minusHours(messageHistoryLengthHours.toInt).toDate
        if (mainTariff.messageHistoryLengthType != TariffType.UNLIMITED && startDate.before(startDateRestriction))
          (startDateRestriction, endDate)
        else interval
      case None => interval
    }
  }

  def createDefaultTariff(): ObjectId = {
    debug("createDefaultTariff")
    val tariffId = new ObjectId()
    val abonentPrice = List[java.util.Map[String, Object]](
      Utils.ensureJavaHashMap(Map("name" -> "Основной абонентский терминал", "cost" -> "0", "comment" -> "")),
      Utils.ensureJavaHashMap(Map("name" -> "Дополнительный абонетский терминал", "cost" -> "0", "comment" -> "")),
      Utils.ensureJavaHashMap(Map("name" -> "Спящий блок автономного типа GSM", "cost" -> "0", "comment" -> "")),
      Utils.ensureJavaHashMap(Map("name" -> "Спящий блок на постоянном питании типа Впайка", "cost" -> "0", "comment" -> "")),
      Utils.ensureJavaHashMap(Map("name" -> "Радиозакладка", "cost" -> "0", "comment" -> "")))
    commandGateway.sendAndWait(new TariffPlanCreateCommand(tariffId,
      Map("default" -> true.asInstanceOf[AnyRef], "name" -> "Базовый", "abonentPrice" -> abonentPrice, "messageHistoryLengthHours" -> Long.box(4))),
      Array("admin", "TariffPlanCreate"), "System")
    tariffId
  }

  def getDefaultTariffId: ObjectId = {
    debug("getDefaultTariff")
    val defaultTariff = tariffDAO.getDefaultTariff
    defaultTariff match {
      case Some(tariff) => tariff._id
      case None => createDefaultTariff()
    }
  }

}

trait TariffService {
  def correctReportWorkingDates(interval: (Date, Date)): (Date, Date)
}



