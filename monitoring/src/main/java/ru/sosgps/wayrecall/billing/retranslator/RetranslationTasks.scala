package ru.sosgps.wayrecall.billing.retranslator

import javax.annotation.Resource

import org.springframework.beans.factory.annotation.{Value, Qualifier, Autowired}
import ru.sosgps.wayrecall.core.GPSDataConversions
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.initialization.MultiDbManager
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import ru.sosgps.wayrecall.utils.web.EDSJsonResponse
import java.util.Date

import scala.beans.BeanProperty

@ExtDirectService
class RetranslationTasks extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {

  val model = Model("id", "name", "host", "port", "processed", "status", "uids", Field("from", "date"), Field("to", "date"))
  override val idProperty: String = "id"
  override val name: String = "RetranslatorsTasks"
  override val lazyLoad: Boolean = false

  private[this] var tasks: List[RetranslationTask] = Nil


  @Resource(name = "directPackageStore")
  var packagesStore: PackagesStore = null

  @ExtDirectMethod(ExtDirectMethodType.STORE_READ)
  def loadAll(request: ExtDirectStoreReadRequest) = {
    debug("loading")


    EDSJsonResponse(tasks.map(t =>
      Map(
        "id" -> t.hashCode(),
        "name" -> t.name,
        "host" -> t.host,
        "port" -> t.port,
        "uids" -> t.uids.mkString(","),
        "from" -> ISODateTime(t.from),
        "to" -> ISODateTime(t.to),
        "processed" -> (t.completedCount + "/" + t.totalCount),
        "status" -> t.status
      )
    ))
  }


  @ExtDirectMethod
  def remove(ids: Seq[Int]) {
    debug("removing :" + ids)
    val toRemove = ids.map(_.toInt).toSet
    tasks.filter(i => toRemove(i.hashCode())).foreach(_.kill())
  }


  @BeanProperty
  @Value("${instance.name}")
  var instanceName: String = null

  @Resource(name = "multiDbManager")
  var mmdbm: MultiDbManager = null

  @ExtDirectMethod
  def addRetranslation(id: String, name: String, host: String, port: Int, protocol: String, uids: Seq[String], from: Date, to: Date) {

    debug(s"addRetranslation: id = $id, name = $name, host = $host, port = $port, uids = $uids, from = $from,to = $to,")

    //    val tr = if ("ODS-mos-ru" == id)
    //      new ODSMosRuRetranslationTask(uids, from, to)
    //    else
    //      new WialonRetranslationTask(name, host, port, uids, from, to)

    def nis: NISRetranslationTask = {

      def histories = {
        uids.toStream.map(uid => packagesStore.getHistoryFor(uid, from, to))
      }

      val total = histories.map(_.total).sum
      val packs = histories.iterator.map(_.iterator).flatten
      new NISRetranslationTask(name, host, port, uids, from, to, total, packs)

    }
    val tr = id match {
      case null => protocol match {
        case "wialon" => new WialonRetranslationTask(name, host, port, uids, mmdbm.dbmanagers(instanceName).getDatabaseName, from, to)
        case "nis" => nis
      }
      case _ if id.startsWith("ODS-mos-ru") => new ODSMosRuRetranslationTask(uids, instanceName, from, to)
      case _ if id.startsWith("Wialon-") => new WialonRetranslationTask(name, host, port, uids, mmdbm.dbmanagers(instanceName).getDatabaseName, from, to)
      case _ if id.startsWith("NIS-") => nis
    }


    tr.start()

    debug(tr + " started")

    tasks = (tr :: tasks).take(20)

  }

}
