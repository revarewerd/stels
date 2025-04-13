package ru.sosgps.wayrecall.billing.retranslator

import java.io.{FileOutputStream, PrintWriter, File}
import scala.io.Source
import ru.sosgps.wayrecall.utils
import scala.collection.mutable
import ru.sosgps.wayrecall.utils.web.ScalaJsonObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.typingMap
import collection.JavaConversions.collectionAsScalaIterable

case class RetranslatorData(id: String, name: String, protocol:String, host: String, port: Int, uids: Seq[String])

trait RetranslatorProvider {

  def setRetranslatorParams(id: String, params: RetranslatorData): Unit

  def getRetranslatorParams(id: String): RetranslatorData

  def removeRetranslator(id: String)

  def listRetranslators: Seq[RetranslatorData]

}


class ODSMOSRuRetranslatorProvider(file: File) extends RetranslatorProvider with grizzled.slf4j.Logging {

  def getRetranslatorParams(id: String): RetranslatorData = synchronized {
    require(id == "ODS-mos-ru", "cant process retranslator " + id + " others than 'ODS-mos-ru'")
    new RetranslatorData("ODS-mos-ru", "ODS-mos-ru", "odsmosru",  "ods.mos.ru/telemetry/telemetryWebService", 80,
     if(file.exists()) Source.fromFile(file, "UTF-8").getLines().filterNot(_.matches("o\\s*")).toList else List.empty
    )
  }

  def setRetranslatorParams(id: String, params: RetranslatorData) = {
    val out = new PrintWriter(new FileOutputStream(utils.io.backupPrev(file)))
    try {
      for (uid <- params.uids) {
        debug("set odsmosru uid=" + uid)
        out.println(uid)
      }
    }
    finally out.close()
  }

  override def listRetranslators = Seq(getRetranslatorParams("ODS-mos-ru"))

  override def removeRetranslator(id: String): Unit = throw new UnsupportedOperationException("can not remove ODS-mos-ru")
}

class JsonConfigRetranslatorProvider(file: File, idPrefix:String, protocol: String) extends RetranslatorProvider {
  //  idPrefix = "Wialon-"
  //  protocol = "wialon"
  private[this] def readJson(): mutable.Buffer[mutable.Map[String, Any]] = if(file.exists())
    mapper.parse[mutable.Buffer[java.util.Map[String, Any]]](file).map(m => collection.JavaConversions.mapAsScalaMap(m))
  else mutable.Buffer.empty

  val mapper = new ScalaJsonObjectMapper
  mapper.enable(SerializationFeature.INDENT_OUTPUT)


  private[this] def saveConfig(config: mutable.Iterable[mutable.Map[String, Any]]) {
    mapper.generate(config, utils.io.backupPrev(file))
  }

  override def setRetranslatorParams(id0: String, params: RetranslatorData) = {
    val config = readJson()
    val id = if (id0 == null)
      idPrefix + System.currentTimeMillis()
    else
      id0

    val rmap = config.find(_.get("id").exists(id ==)).getOrElse {
      val newmap = mutable.Map[String, Any]("id" -> id)
      config += newmap
      newmap
    }

    rmap -= "imeis"
    rmap("name") = params.name
    rmap("uids") = params.uids.toSet.toList
    rmap("host") = params.host
    rmap("port") = params.port
    rmap("protocol") = protocol

    saveConfig(config)
  }

  override def getRetranslatorParams(id: String) = {

    val record = readJson().find(_.get("id").exists(id ==)).get

    readRecord(record)
  }

  private[this] def readRecord(record: mutable.Map[String, Any]): RetranslatorData = {
    val id_ = record.as[String]("id")
    val host = record.as[String]("host")
    val name = record.getAs[String]("name").getOrElse(id_)
    val port = record.as[Int]("port")

    val uids = record.getAs[java.util.List[String]]("uids")
      .map(_.toIterator).getOrElse(Iterator.empty).toList

    new RetranslatorData(id_, name, protocol, host, port, uids)
  }

  override def listRetranslators: Seq[RetranslatorData] = {
    readJson().map(readRecord)
  }

  override def removeRetranslator(id: String): Unit = synchronized {
    saveConfig(readJson().filter(_.as[String]("id") != id))
  }
}