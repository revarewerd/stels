
package ru.sosgps.wayrecall.billing.objectcqrs

import org.axonframework.eventsourcing.annotation.{AggregateIdentifier, AbstractAnnotatedAggregateRoot}
import java.io.Serializable
import org.axonframework.repository.Repository
import org.bson.types.ObjectId
import scala.collection.mutable
import org.axonframework.eventsourcing.EventSourcingRepository

import org.axonframework.commandhandling.annotation.{TargetAggregateIdentifier, CommandHandler}
import org.axonframework.eventhandling.annotation.EventHandler
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import ru.sosgps.wayrecall.billing.equipment.EquipmentAggregate
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader.ensureObjectId
import org.axonframework.eventstore.mongo.MongoEventStore
import scala.collection.immutable.HashMap
import javax.annotation.Resource
import scala.collection.mutable.ListBuffer
import scala.util.control.Exception
import org.axonframework.eventstore.EventStore
import org.axonframework.eventstore.EventStreamNotFoundException

import scala.beans.BeanProperty
import scala.collection.JavaConverters.asScalaBufferConverter;
import scala.collection.JavaConverters.mapAsScalaMapConverter
import ru.sosgps.wayrecall.core._

import ru.sosgps.wayrecall.utils.{CollectionUtils, Memo, typingMap}


/**
 * Created by nickl on 04.01.14.
 */
class ObjectAggregate extends AbstractAnnotatedAggregateRoot[String] with Serializable with grizzled.slf4j.Logging {

  @AggregateIdentifier
  var uid: String = "undefined"

  private val data: mutable.Map[String, Serializable] = new mutable.HashMap[String, Serializable]

  private val _equipments: mutable.Set[ObjectId] = new mutable.HashSet[ObjectId]()

  def equipments: Set[ObjectId] = _equipments.toSet

  //  def this(uid: String) = {
  //    this()
  //    this.uid = uid
  //    this(new ObjectCreatedEvent(uid,null,null))
  //  }

  //  def asNewAggregate = {
  //    this.uid = ObjectAggregate.genUid
  //    this(new ObjectCreatedEvent(uid,null,null))
  //    this
  //  }

  def aggregateByUid(uid: String = ObjectAggregate.genUid, objectData: Map[String, Serializable], equipmentData: Seq[Map[String, Serializable]]) = {
    this.uid = uid
    debug("uid=" + uid)
    val eqData = equipmentData.map(eqData => Map("_id" -> ensureObjectId(eqData("_id")))).toList
    this(new ObjectCreatedEvent(uid, objectData, eqData))
    val account = objectData.get("account").orElse(objectData.get("accountId")).map(ensureObjectId)
    if (account.isDefined)
      this(new ObjectAccountChangedEvent(this.uid,
        None,
        account
      ))
    this
  }

  def processObjectDataSet(data: Map[String, Serializable]) = {
    val changed = ObjectAggregate.detectChanged(this.data, data)
    val toPublish = new ListBuffer[AnyRef]
    if (changed.nonEmpty) {
      toPublish += new ObjectDataChangedEvent(this.uid, changed)
      val account = changed.get("account").orElse(changed.get("accountId"))
      if (account.isDefined) {
        toPublish += new ObjectAccountChangedEvent(this.uid,
          this.data.get("account").orElse(this.data.get("accountId")).map(ensureObjectId),
          account.map(ensureObjectId)
        )
      }
      val disabled = changed.get("disabled")
      disabled.foreach(disabled => {
        toPublish += new ObjectEnabledChanged(this.uid, disabled.toString.toBoolean)
      })
    }
    toPublish.foreach(this.apply)
  }

  def processChangeEquipments(newEqs: Set[ObjectId]): (Set[ObjectId], Set[ObjectId]) = {
    val added = newEqs -- equipments
    val removed = equipments -- newEqs
    if (added.nonEmpty || removed.nonEmpty)
      this(new ObjectEquipmentChangedEvent(this.uid, removed, added))
    (removed, added)
  }

  def processObjectRemoving(uid: String) {
    this(new ObjectDeletedEvent(this.uid))
    val account = this.data.get("account").orElse(this.data.get("accountId")).map(ensureObjectId)
    if (account.isDefined)
      this(new ObjectAccountChangedEvent(this.uid,
        account,
        None
      ))
  }

  def processObjectRemovingNew(uid: String): Unit = {
    this.apply(new ObjectRemovedEvent(this.uid))
  }

  def processObjectRestoring(uid: String): Unit = {
    this.apply(new ObjectRestoredEvent(this.uid))
  }


  @EventHandler private def handleObjectCreatedEvent(event: ObjectCreatedEvent) {
    debug("me before ObjectCreatedEvent=" + this.toString)
    debug("ObjectCreatedEvent is =" + event.toString)
    this.uid = event.uid
    this.data ++= event.objectData
    this._equipments ++= event.equipmentData.map(eqData => ensureObjectId(eqData("_id"))).toSet
    debug("me after ObjectCreatedEvent=" + this.toString)

  }

  @EventHandler private def handleObjectChangedEvent(event: ObjectDataChangedEvent) {
    debug("me before ObjectDataChangedEvent=" + this.toString)
    debug("ObjectDataChangedEvent is =" + event.toString)
    this.uid = event.uid
    this.data --= event.changedAttributes.collect({ case (k, v) if v == null => k})
    this.data ++= event.changedAttributes
    debug("me after ObjectChangedEvent=" + this.toString)
  }

  @EventHandler private def handleObjectEquipmentChangedEvent(event: ObjectEquipmentChangedEvent) {
    debug("me before ObjectEquipmentChangedEvent=" + this.toString)
    debug("ObjectEquipmentChangedEvent is =" + event.toString)
    this.uid = event.uid
    this._equipments --= event.removed
    this._equipments ++= event.added
    debug("me after ObjectEquipmentChangedEvent=" + this.toString)
  }

  override def toString = "ObjectAggregate( uid =" + uid + ", _equipments=" + _equipments + ", data=" + data + ")"

  def getData: scala.collection.Map[String, Serializable] = data
}

object ObjectAggregate {
  def genUid = "o" + (java.util.UUID.randomUUID.getLeastSignificantBits & 0x7FFFFFFFFFFFFFFFL)

  @deprecated("use `CollectionUtils.detectChanged`")
  def detectChanged(oldData: scala.collection.Map[String, Serializable],
                    newData: scala.collection.Map[String, Serializable]): HashMap[String, Serializable] = {
   CollectionUtils.detectChanged(oldData, newData)
  }

}

class ObjectCreateCommand(val uid: String, val objectData: Map[String, Serializable], val equipmentData: Seq[Map[String, Serializable]]) extends CommandEntityInfo {
  def this(uid: String,
           objectData: java.util.Map[String, Serializable],
           equipmentData: java.util.List[java.util.Map[String, Serializable]]) = {
    this(uid, objectData.asScala.toMap, equipmentData.asScala.toSeq.map(_.asScala.toMap))
  }
  def getEntity()= {
    "Object"
  }
  def getEntityId()= {
    this.uid
  }

  override def toString = "ObjectCreateCommand((uid = " + uid + " objectData = " + objectData + " equipmentData = " + equipmentData + ")"
}



class ObjectDataSetCommand(val uid: String,
                           val objectData: Map[String, Serializable],
                           val equipmentData: Option[Array[ObjectId]])  extends CommandEntityInfo {
  require(uid != null, "uid must not be null")
  require(uid.nonEmpty, "uid must not be empty")
  require(objectData != null, "objectData must not be null")
  //require(equipmentData != null, "equipmentData must not be null")

  @deprecated
  def this(uid: String,
           objectData: Map[String, Serializable],
           equipmentData: Seq[Map[String, Serializable]]) {
    this(uid, objectData, Option(equipmentData.map(eqData => ensureObjectId(eqData("_id"))).toArray))
  }

  def this(uid: String,
           objectData: java.util.Map[String, Serializable],
           equipmentData: java.util.List[java.util.Map[String, Serializable]]) = {
    this(uid, objectData.asScala.toMap, equipmentData.asScala.toSeq.map(_.asScala.toMap))
  }
   def this(uid: String,
           objectData: java.util.Map[String, Serializable],
           equipmentData: Array[ObjectId]) = {
    this(uid, objectData.asScala.toMap, Option(equipmentData))
  }

  def getEntity()= {
   "Object"
  }
  def getEntityId()= {
    this.uid
  }

  override def toString = "ObjectUpdateDataCommand(uid = " + uid + " objectData = " + objectData + " equipmentData = " + equipmentData.mkString("[", ",", "]") + ")"

}

class ObjectDeleteCommand(val uid: String)  extends CommandEntityInfo {
  def getEntity()= {
    "Object"
  }
  def getEntityId()= {
    this.uid
  }
}

class ObjectRemoveCommand(val uid: String) extends CommandEntityInfo {
  def getEntity()= {
    "Object"
  }
  def getEntityId()= {
    this.uid
  }
}

class ObjectRestoreCommand(val uid: String) extends CommandEntityInfo {
  def getEntity()= {
    "Object"
  }
  def getEntityId()= {
    this.uid
  }
}



class ObjectCommandHandler extends grizzled.slf4j.Logging {

  @Resource
  @BeanProperty
  var objectsAggregatesRepository: Repository[ObjectAggregate] = null

  @Resource
  @BeanProperty
  var equipmentsAggregatesRepository: Repository[EquipmentAggregate] = null

  @Autowired
  @BeanProperty
  var eventsStore: EventStore = null

  @CommandHandler
  private def handle(objectCreateCommand: ObjectCreateCommand) = {

    debug("received create command=" + objectCreateCommand)

    var obj: ObjectAggregate = null
    if (objectCreateCommand.uid != null) {
      debug("uid =" + objectCreateCommand.uid)
      obj = new ObjectAggregate().aggregateByUid(objectCreateCommand.uid, objectCreateCommand.objectData, objectCreateCommand.equipmentData)
    }
    else {
      debug("uid null")
      obj = new ObjectAggregate().aggregateByUid(objectData = objectCreateCommand.objectData, equipmentData = objectCreateCommand.equipmentData)
    }
    objectsAggregatesRepository.add(obj)

    if (!objectCreateCommand.equipmentData.isEmpty) {
      val getEquipmentAggregate = Memo(retrieveEquipmentAggregate)

      val objAccId = ObjectsRepositoryReader.ensureObjectId(
        objectCreateCommand.objectData.getOrElse("accountId", null)
      )

      for (eqData0 <- objectCreateCommand.equipmentData) {
        val eqData = eqData0 + ("uid" -> obj.uid) ++ (if(objAccId != null) Map("accountId" -> objAccId) else Map.empty)
        val id = ensureObjectId(eqData("_id"))

        debug("updating Equipment with id=" + id + " with data=" + eqData)
        getEquipmentAggregate(id).processObjectChange(None, Some(obj.uid))
        getEquipmentAggregate(id).processEquipmentDataSet(eqData)
      }
      //      for (eqData <- objectCreateCommand.equipmentData) {
      //        val id = ensureObjectId(eqData("_id"))
      //        getEquipmentAggregate(id).processObjectChange(None, Some(objectCreateCommand.uid))
      //      }
    }

    obj.uid

  }

  @CommandHandler
  private def handle(updateDataCommand: ObjectDataSetCommand) = {

    debug("received update command=" + updateDataCommand)

    val objectAggregate = retrieveObjectAggregate(updateDataCommand.uid)
    val prevAcc = ObjectsRepositoryReader.ensureObjectId(objectAggregate.getData.get("accountId").orNull)
    objectAggregate.processObjectDataSet(updateDataCommand.objectData)

    if(updateDataCommand.equipmentData.isDefined) {
      val getEquipmentAggregate = Memo(retrieveEquipmentAggregate)

      val eqOids = updateDataCommand.equipmentData.get.toSet
      val (removed, added) = objectAggregate.processChangeEquipments(eqOids)

      debug("equipment for " + updateDataCommand.uid + " removed=" + removed + " added=" + added)

      val objAccId = ObjectsRepositoryReader.ensureObjectId(
        updateDataCommand.objectData.getOrElse("accountId", null)
      )

      for (eqId <- removed) {
        getEquipmentAggregate(eqId).processObjectChange(Some(objectAggregate.uid), None)
      }

      for (eqId <- added) {
        val eqAgg = getEquipmentAggregate(eqId)
        eqAgg.processObjectChange(None, Some(objectAggregate.uid))
        if (objAccId != null) {
          debug("change eq account to=" + objAccId)
          eqAgg.processEquipmentDataSet(Map("accountId" -> objAccId))
        }
      }

      debug(s"prevAcc=$prevAcc newAcc=$objAccId")
      if (prevAcc != objAccId)
        objectAggregate.equipments.map(getEquipmentAggregate).foreach(
          _.processEquipmentDataSet(Map("accountId" -> objAccId))
        )
    }
    objectAggregate.uid
  }

  @CommandHandler
  private def handle(command: ObjectDeleteCommand) {
    debug("Object delete command=" + command.uid)
    val objectAggregate = retrieveObjectAggregate(command.uid)
    debug("retrieveObjectAggregate=" + objectAggregate)
    objectAggregate.processObjectRemoving(command.uid)
  }


  @CommandHandler
  private def handle(command: ObjectRestoreCommand): Unit = {
    debug("Object restore command=" + command.uid)
    val objectAggregate = retrieveObjectAggregate(command.uid)
    debug("retrieveObjectAggregate=" + objectAggregate)
    objectAggregate.processObjectRestoring(command.uid)
  }

  @CommandHandler
  private def handle(command: ObjectRemoveCommand): Unit = {
    debug("Object remove command=" + command.uid)
    val objectAggregate = retrieveObjectAggregate(command.uid)
    debug("retrieveObjectAggregate=" + objectAggregate)
    objectAggregate.processObjectRemovingNew(command.uid) //new
  }


  private[this] def retrieveObjectAggregate(uid: String): ObjectAggregate = {
    require(uid != null, "updateDataCommand.uid cant be null")
    val exists = Exception.catching(classOf[EventStreamNotFoundException]).opt(
      eventsStore.readEvents("ObjectAggregate", uid).hasNext).getOrElse(false)
    //    if (!exists) {
    //      val obj = new ObjectAggregate(uid)
    //      objectsAggregatesRepository.add(obj)
    //      obj
    //    } else
    try {
      objectsAggregatesRepository.load(uid)
    }
    catch {
      case e: Exception => {throw new RuntimeException("Агрегат не найден: " + Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage), e)}
    }

  }

  private[this] def retrieveEquipmentAggregate(oid: ObjectId): EquipmentAggregate = {
    require(oid != null, "oid cant be null")
    debug("oid" + oid)
    val exists = Exception.catching(classOf[EventStreamNotFoundException]).opt(
      eventsStore.readEvents("EquipmentAggregate", oid).hasNext).getOrElse(false)
    debug("exists " + exists)
    //    if (!exists) {
    //      info("creating new equipment " + oid)
    //      val obj = new EquipmentAggregate(oid,Map.empty)
    //      equipmentsAggregatesRepository.add(obj)
    //      obj
    //    } else
    try {
      equipmentsAggregatesRepository.load(oid)
    }
    catch {
      case e: Exception => {
        error("error in retrieveEquipmentAggregate:", e)
        throw new RuntimeException("Агрегат не найден: " + Option(e.getCause).map(_.getMessage).getOrElse(oid))
      }
    }
  }

  //  @EventHandler private def handleObjectChangedEvent(event: ObjectDataChangedEvent) {
  //    debug("processing ObjectChangedEvent = " + event)
  //  }

}
