package ru.sosgps.wayrecall.billing

import javax.jms.{ObjectMessage, Message}
import org.springframework.web.context.WebApplicationContext
import ru.sosgps.wayrecall.core.{MongoDBManager, SecureGateway, ObjectsRepositoryReader, GPSData}
import org.atmosphere.cpr.{Broadcaster, BroadcasterFactory}
import ru.sosgps.wayrecall.utils.typingMap
import java.util.{Objects, Date}
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.utils.web.ScalaJson
import collection.JavaConversions.collectionAsScalaIterable
import java.io.Serializable
import scala.collection
import org.axonframework.commandhandling.gateway.CommandGateway
import ru.sosgps.wayrecall.billing.equipment.EquipmentDataSetCommand
import com.mongodb.casbah.Imports._

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 03.11.13
 * Time: 17:36
 * To change this template use File | Settings | File Templates.
 */
class RuptelaListener extends grizzled.slf4j.Logging with javax.jms.MessageListener {

  /*
    HashMap(
  "name" -> s.name,
  "uid" -> s.uid,
  "time" -> s.time,
  "source" -> s.eventSourceName
)
 */

  private val sourceMap: Map[String, String] = Map(
    "configurator" -> "Конфигурирование",
    "fwupdater" -> "Перепрошивка"
  ).withDefault(k => k)

  private val eqFileldMap: Map[String, String] = Map(
    "configurator" -> "eqConfig",
    "fwupdater" -> "eqFirmware"
  )

  private val statesMap = Map(
    "blockSendingState" -> "началась пересылка данных",
    "finishState" -> "запись прошивки",
    "cfgWriteState" -> "запись конфигурации",
    "finished" -> "Успешно завершена",
    "error" -> "Ошибка"
  )

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var commandGateway: SecureGateway = null;

  def onMessage(message: Message) {
    debug("ruptela listener received"+message)
    message match {
      case m: ObjectMessage => {
        m.getObject match {
          //          case (name:String, uid:String) => {
          //            BroadcasterFactory.getDefault.lookup[Broadcaster]("servermes", true).broadcast(name + " "+uid)
          //          }
          case content: scala.collection.Map[String, Any] => {
            try {
              atmosphereBroadcast(content)
            }
            catch {
              case e: Exception => warn("error in broadcast " + content, e)
            }
            try {
              if (content.as[String]("name") == "finished") {
                val imei = content.as[String]("imei")
                val eqDbo = mdbm.getDatabase()("equipments").findOne(MongoDBObject("eqIMEI" -> imei))
                  .getOrElse(throw new IllegalArgumentException("no imei " + imei + " in " + mdbm.getDatabaseName))
                commandGateway.sendAndWait(
                  new EquipmentDataSetCommand(
                    eqDbo.as[ObjectId]("_id"),
                    Map(eqFileldMap(content.as[String]("source")) -> content.as[String]("dataSourceName")
                    )), Array("admin", "EquipmentDataSet"), "System"
                );
              }
            }
            catch {
              case e: Exception => warn("error in cqrs setting " + content, e)
            }
          }
          case e: Any => warn("received unrecognized ObjectMessage with object: " + e)
        }

      }
      case _ => warn("received unrecognized message")
    }

  }

  private var broadcaster: Broadcaster = null
  @Autowired private[billing] var context: WebApplicationContext = null

  private[billing] def getBroadcaster: Broadcaster = {
    if (broadcaster != null) return broadcaster
    val broadcasterFactory: BroadcasterFactory = Objects.requireNonNull(context.getServletContext.getAttribute(classOf[BroadcasterFactory].getName).asInstanceOf[BroadcasterFactory], "BroadcasterFactory")
    broadcaster = broadcasterFactory.lookup("servermes", true)
    return broadcaster
  }


  private[this] def atmosphereBroadcast(content: Serializable with collection.Map[String, Any]) {
    val stateName = content.as[String]("name")
    debug("received map:" + content)
    statesMap.get(stateName).foreach(translatedStateName => {
      val sourceName = sourceMap(content.as[String]("source"))
      val objectName = Option(or.getObjectByIMEI(content.as[String]("imei"))).map(_.get("name"))
        .getOrElse(throw new IllegalArgumentException("no objects with imei: " + content + " in " + mdbm.getDatabaseName))
      val broadcaster = getBroadcaster
      debug("active atmosphere resources: " + broadcaster.getAtmosphereResources)
      val message = ScalaJson.writeValueAsString(
        Map(
          "eventType" -> "textMessage",
          "time" -> content.as[Date]("time"),
          "text" -> (objectName + " " + sourceName.take(4) + ". " + translatedStateName)
        ))
      debug("broadcasting: " + message)
      broadcaster.broadcast(message)
    }
    )
  }
}
