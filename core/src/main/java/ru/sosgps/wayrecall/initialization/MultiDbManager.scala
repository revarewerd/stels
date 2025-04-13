package ru.sosgps.wayrecall.initialization

import java.io.Serializable
import java.util.Objects
import java.util.concurrent.TimeUnit
import javax.naming.InitialContext

import com.google.common.cache.CacheBuilder
import com.mongodb.casbah.commons.Imports._
import org.axonframework.eventhandling.annotation.EventHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.data.IllegalImeiException
import ru.sosgps.wayrecall.utils.funcLoadingCache

import scala.beans.BeanProperty

/**
 * Created by nickl on 21.07.14.
 */
class MultiDbManager extends Iterable[MongoDBManager] with grizzled.slf4j.Logging {

  @BeanProperty
  var config: MultiserverConfig = null

  lazy val dbmanagers = config.instances.map(icfg => {
    val manager = createMdbm(icfg)
    if (icfg.isDefault) {
      mainMongoDbm = manager
    }
    (icfg.name, manager)
  }).toMap

  protected def createMdbm(icfg: InstanceConfig): MongoDBManager = {
    new MongoDBManager(icfg)
  }

  private var mainMongoDbm: MongoDBManager = null

  override def iterator: Iterator[MongoDBManager] = dbmanagers.valuesIterator

  protected val managerImeiCache = CacheBuilder.newBuilder()
    .expireAfterWrite(3, TimeUnit.HOURS)
    .buildWithFunction((imei: String) => {
    //TODO: неплохо было бы распараллелить через future каждого отдельного пула
    dbmanagers.values.find(_.getDatabase()("equipments").findOne(MongoDBObject("eqIMEI" -> imei)).isDefined)
  })

  @throws(classOf[IllegalImeiException])
  def dbByIMEI(imei: String): MongoDBManager =
    dbByIMEIOpt(imei).getOrElse(throw new IllegalImeiException("object with imei " + imei + " was not found in all dbs"))

  def dbByIMEIOpt(imei: String): Option[MongoDBManager] = {
    managerImeiCache(imei)
  }

  def getMainDBManager: MongoDBManager = {
    dbmanagers
    Objects.requireNonNull(mainMongoDbm, "mainMongoDbm is null, mb no default instance is set?")
  }

  @EventHandler
  def handleEquipmentEvent(event: EquipmentCreatedEvent) {
    debug("EquipmentEvent : " + event)
    val eqIMEI = event.data.get("eqIMEI").map(_.asInstanceOf[String])
    eqIMEI.foreach(managerImeiCache.invalidate)
    eqIMEI.foreach(fireImeiOwnerChanged)
  }

  @EventHandler
  def handleEquipmentEvent(event: EquipmentDataChangedEvent) {
    debug("EquipmentEvent : " + event)
    val eqIMEI = event.changedAttributes.get("eqIMEI").map(_.asInstanceOf[String])
    eqIMEI.foreach(managerImeiCache.invalidate)
    eqIMEI.foreach(fireImeiOwnerChanged)
  }

  @EventHandler
  def handleEquipmentEvent(event: EquipmentDeletedEvent) {
    debug("EquipmentEvent : " + event)
    val eqIMEI = event.data.get("eqIMEI").map(_.asInstanceOf[String])
    eqIMEI.foreach(s => managerImeiCache.invalidate(s))
    eqIMEI.foreach(fireImeiOwnerChanged)
  }

//  @EventHandler
//  def handleEquipmentEvent(event: EquipmentRemovedEvent): Unit = {
//    debug("EquipmentEvent : " + event)
//    val eqIMEI = event.data.get("eqIMEI").map(_.asInstanceOf[String])
//    eqIMEI.foreach(s => managerImeiCache.invalidate(s))
//    eqIMEI.foreach(fireImeiOwnerChanged)
//  }
//
//  @EventHandler
//  def handleEquipmentEvent(event: EquipmentRestoredEvent): Unit = {
//    debug("EquipmentEvent : " + event)
//    val eqIMEI = event.data.get("eqIMEI").map(_.asInstanceOf[String])
//    eqIMEI.foreach(s => managerImeiCache.invalidate(s))
//    eqIMEI.foreach(fireImeiOwnerChanged)
//  }


  protected def fireImeiOwnerChanged(imei: String): Unit = {}

}

class JNDIMultiDbManager extends MultiDbManager {
  @Autowired
  var jmsTemplate: JmsTemplate = null

  private val ctx = new InitialContext()

  override protected def createMdbm(icfg: InstanceConfig): MongoDBManager = {
    ctx.lookup("java:global/env/mdbm/" + icfg.name).asInstanceOf[MongoDBManager]
  }

  override protected def fireImeiOwnerChanged(imei: String): Unit = {
    debug("fireImeiOwnerChanged " + imei)
    jmsTemplate.convertAndSend("dealers.imeiOwnerChanged", imei)
  }

  def invalidateImeiOwner(imei: String): Unit = {
    debug("invalidating owner for imei=" + imei)
    managerImeiCache.invalidate(imei)
  }

}

//'java:global/env/mdbm/'+'${instance.name}'
