package ru.sosgps.wayrecall.billing.retranslator

import java.util.NoSuchElementException

import ru.sosgps.wayrecall.core.{InstanceConfig, ObjectsRepositoryReader, MongoDBManager}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.{Value, Autowired}
import collection.JavaConversions.collectionAsScalaIterable
import javax.servlet.http.HttpServletResponse
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ru.sosgps.wayrecall.utils.web.{ScalaJsonObjectMapper, EDSJsonResponse}

import scala.Predef._
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.typingMap
import ru.sosgps.wayrecall.utils.impObjectId
import ru.sosgps.wayrecall.security.PermissionsManager
import java.nio.file.{Files, Paths, FileSystems, Path}
import java.io.{FileOutputStream, PrintWriter, File}
import scala.io.Source
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import com.fasterxml.jackson.databind.SerializationFeature
import ru.sosgps.wayrecall.utils

@ExtDirectService
class RetranslatorsListService @Autowired() (val instance: InstanceConfig) extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val confDir: Path = instance.path.resolve("retranslators")
  Files.createDirectories(confDir)

  val odsMosRuRetranslator = new ODSMOSRuRetranslatorProvider(confDir.resolve("odsmosruimeis.txt").toFile)
  val wialonRetranslator = new JsonConfigRetranslatorProvider(confDir.resolve("retranslator.json").toFile, "Wialon-", "wialon")
  val nisRetranslator = new JsonConfigRetranslatorProvider(confDir.resolve("nisretranslator.json").toFile, "NIS-", "nis")

  private val stringToProvider = Map(
    "ODS-mos-ru" -> odsMosRuRetranslator,
    "Wialon-" -> wialonRetranslator,
    "NIS-" -> nisRetranslator
  )
  val retranslatorsMapping =
    stringToProvider.withDefault(
      k => stringToProvider.keys.find(k.startsWith)
        .map(stringToProvider)
        .getOrElse(throw new NoSuchElementException("no provider for key: " + k))
    )

  // TODO: вынести куданить мепинг протокола в префикс
  val protocolToProvider = Map(
    "wialon" -> wialonRetranslator,
    "nis" -> nisRetranslator
  )


  val retranslators = new RetranslatorProvider {

    def getRetranslatorParams(id: String) = retranslatorsMapping(id).getRetranslatorParams(id)

    def setRetranslatorParams(id: String, params: RetranslatorData) = {
      if (id != null)
        retranslatorsMapping(id).setRetranslatorParams(id, params)
      else
        protocolToProvider(params.protocol).setRetranslatorParams(id, params)
    }

    override def listRetranslators: Seq[RetranslatorData] = retranslatorsMapping.values.flatMap(_.listRetranslators).toList

    override def removeRetranslator(id: String): Unit = retranslatorsMapping(id).removeRetranslator(id)
  }


  val model = Model("id", "name", "host", "port", "protocol")
  override val idProperty: String = "id"
  override val name: String = "RetranslatorsListService"
  override val lazyLoad: Boolean = false

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadAll(request: ExtDirectStoreReadRequest) = {
    debug("loading")
    val r = retranslators.listRetranslators.map(r => Map(
      "id" -> r.id,
      "name" -> r.name,
      "port" -> r.port,
      "host" -> r.host,
      "protocol" -> r.protocol
    ))
    debug("r=" + r)
    EDSJsonResponse(r)
  }


  @ExtDirectMethod
  def remove(ids: Seq[String]) {
    ids.foreach {
      id =>
        retranslators.removeRetranslator(id)
    }
  }

}

@ExtDirectService
class RetranslatorsService extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model("id", "name", "uid", "eqIMEI", "accountName")
  val name = "RetranslatorsService"
  val idProperty = "id"
  val lazyLoad = false

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var rl: RetranslatorsListService = null


  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadAll(request: ExtDirectStoreReadRequest) = {
    debug("request.getParams=" + request.getParams)

    val index = new AtomicInteger(0);

    Option(request.getParams.get("retranslatorId")).map(_.toString) match {
      case Some(retrId) =>
        val r =  rl.retranslators.getRetranslatorParams(retrId).uids.flatMap(uid => Option(or.getObjectByUid(uid)))
          .map(dbo =>
          Map(
            "id" -> index.getAndIncrement,
            "name" -> dbo.as[String]("name"),
            "accountName" -> or.getAccountName(dbo.as[ObjectId]("account")),
            "uid" -> dbo.as[String]("uid"),
            "eqIMEI" -> or.getMainTerminal(dbo.as[String]("uid")).map(_.as[String]("eqIMEI"))
          ))
          .toList.sortBy(_.as[String]("name"))

        debug("requestedItems count = " + r.size)
        EDSJsonResponse(r)
      case None =>
        EDSJsonResponse(Iterator.empty)
    }

  }

  @ExtDirectMethod
  def updateData(retranslatorId: String, name: String, host: String, port: Int, protocol0: String, data: Seq[Map[String, AnyRef]]) = {
    debug("retranslatorId=" + retranslatorId + " datalen=" + data.size + " data=" + data)
    debug("host=" + host + " port=" + port + " protocol=" + protocol0)
    //rl.retranslators.setResendingImeis(retranslatorId, data.map(_.as[String]("eqIMEI")).toSet)

    val protocol = if (retranslatorId != null && retranslatorId.contains("ODS-mos-ru")) "odsmosru" else protocol0

    rl.retranslators.setRetranslatorParams(retranslatorId, RetranslatorData(retranslatorId, name, protocol, host, port, data.map(_.as[String]("uid"))))
  }

}

