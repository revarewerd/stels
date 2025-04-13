package ru.sosgps.wayrecall.billing.equipment


import org.axonframework.eventsourcing.EventSourcingRepository
import org.axonframework.eventsourcing.annotation.{AggregateIdentifier, AbstractAnnotatedAggregateRoot}
import org.axonframework.eventstore.mongo.MongoEventStore
import org.axonframework.repository.Repository
import org.bson.types.ObjectId
import javax.annotation.Resource
import java.io.Serializable
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectAggregate
import scala.collection.immutable.HashMap
import scala.collection.mutable
import org.axonframework.commandhandling.annotation.{TargetAggregateIdentifier, CommandHandler}
import org.axonframework.eventhandling.annotation.EventHandler
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectAggregate
import scala.beans.BeanProperty
import scala.collection.JavaConverters._
;
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import scala.collection.mutable.ListBuffer
import scala.util.control.Exception
import org.axonframework.eventstore.EventStore
import org.axonframework.eventstore.EventStreamNotFoundException
import ru.sosgps.wayrecall.core._


class EquipmentAggregate extends AbstractAnnotatedAggregateRoot[ObjectId] with Serializable with grizzled.slf4j.Logging {

  private val data = new mutable.HashMap[String, Serializable]

  private[this] var obj: Option[String] = None

  @AggregateIdentifier
  var oid: ObjectId = null

//  def this(oid: ObjectId, data: Map[String, Serializable]) = {
//    this()
//    this.oid = oid
//    this.data++=data
//    this(new EquipmentCreatedEvent(oid, data))
//  }
  
  def aggregateByOid(oid: ObjectId = new ObjectId(), data: Map[String, Serializable]) = {
    this.oid = oid
    this.data++=data
    this(new EquipmentCreatedEvent(oid,data))
    val account = data.get("accountId").map(ObjectsRepositoryReader.ensureObjectId)
    if (account.isDefined)
      this(new EquipmentAccountChangedEvent(this.oid,
        None,
        account
      ))
    this
  }

  def processEquipmentDataSet(map: Map[String, Serializable]) = {
    
    val changed = EquipmentAggregate.detectChanged(this.data, map)
    debug("processEquipmentDataSet changed:" + changed)
    val toPublish = new ListBuffer[AnyRef]
    if (changed.nonEmpty) {
      toPublish += new EquipmentDataChangedEvent(this.oid, changed)
      val account = changed.get("accountId")
      if (account.isDefined) {
        toPublish += new EquipmentAccountChangedEvent(this.oid,
          this.data.get("accountId").map(ObjectsRepositoryReader.ensureObjectId),
          account.map(ObjectsRepositoryReader.ensureObjectId))
      }

    }
    else debug("no changes")

    toPublish.foreach(this.apply)
  }

  def processEquipmentRemoving(oid: ObjectId) {
    this(new EquipmentDeletedEvent(this.oid, this.data))
    val account = this.data.get("accountId").map(ObjectsRepositoryReader.ensureObjectId)
    if (account.isDefined)
      this(new EquipmentAccountChangedEvent(this.oid,
        account,
        None))
  }

  def processEquipmentRemovingNew(oid: ObjectId): Unit = {
    this.apply(new EquipmentRemovedEvent(this.oid,this.data))
  }

  def processEquipmentRestoring(oid: ObjectId): Unit = {
    this.apply(new EquipmentRestoredEvent(this.oid, this.data))
  }



  def processObjectChange(prevObject: Option[String], newObject: Option[String]) {
    this(new EquipmentObjectChangedEvent(this.oid, prevObject, newObject))
  }
  
  @EventHandler private def handleEquipmentCreateEvent(event: EquipmentCreatedEvent) {
    debug("me before EquipmentCreatedEvent=" + this.toString)
    debug("EquipmentCreatedEvent is =" + event.toString)
    this.oid = event.oid    
    this.data ++= Option(event.data).getOrElse(Map.empty)
    debug("me after EquipmentCreatedEvent=" + this.toString)
  }
  
  @EventHandler private def handleEquipmentChangedEvent(event: EquipmentDataChangedEvent) {
    debug("me before EquipmentDataChangedEvent=" + this.toString)
    debug("EquipmentDataChangedEvent is =" + event.toString)
    this.oid = event.oid
    this.data --= event.changedAttributes.collect({case (k, v) if v == null => k})
    this.data ++= event.changedAttributes
    debug("me after EquipmentDataChangedEvent=" + this.toString)
  }

  @EventHandler private def handleObjectChange(event: EquipmentObjectChangedEvent) {
    this.oid = event.oid
    if (this.obj != event.prevObject)
      warn("unexpected object change, setted owner =" + this.obj + " but told that it was " + event.prevObject)
    this.obj = event.newObject
    debug("me after EquipmentObjectChangedEvent=" + this.toString)
  }

  override def toString = s"EquipmentAggregate( oid =$oid, obj=$obj, data=$data)"

}

class EquipmentCreateCommand(val oid: ObjectId, val data: Map[String, Serializable]) extends  CommandEntityInfo{
  def this(oid: ObjectId,
           data: java.util.Map[String, Serializable]) = {
    this(oid, data.asScala.toMap)
  }
  def getEntity()= {
    "Equipment"
  }
  def getEntityId()= {
    this.oid
  }
  override def toString = "EquipmentCreatedCommand(oid = " + oid + ", data = " + data + ")"
}

class EquipmentDataSetCommand(val oid: ObjectId, val data: Map[String, Serializable]) extends  CommandEntityInfo{
  require(oid != null, "oid must not be null")    
  require(data != null, "equipmentData must not be null")

  def this(oid: ObjectId,
           data: java.util.Map[String, Serializable]) = {
    this(oid, data.asScala.toMap)
  }
  def getEntity()= {
    "Equipment"
  }
  def getEntityId()= {
    this.oid
  }
  override def toString = "EquipmentDataSetCommand( oid =" + oid + ", data=" + data + ")"
}

class EquipmentDeleteCommand(val oid: ObjectId) extends  CommandEntityInfo{
  def getEntity()= {
    "Equipment"
  }
  def getEntityId()= {
    this.oid
  }
}

class EquipmentRemoveCommand(val oid: ObjectId) extends  CommandEntityInfo{
  def getEntity()= {
    "Equipment"
  }
  def getEntityId()= {
    this.oid
  }
}

class EquipmentRestoreCommand(val oid: ObjectId) extends  CommandEntityInfo{
  def getEntity()= {
    "Equipment"
  }
  def getEntityId()= {
    this.oid
  }
}


class EquipmentObjectChangeCommand(val oid: ObjectId, val prevObject: Option[String], val newObject: Option[String]) extends  CommandEntityInfo{
  def getEntity()= {
    "Equipment"
  }
  def getEntityId()= {
    this.oid
  }
}

class EquipmentCommandHandler extends grizzled.slf4j.Logging {
  @Resource
  @BeanProperty
  var equipmentsAggregatesRepository: Repository[EquipmentAggregate] = null

  @Autowired
  @BeanProperty
  var eventsStore: EventStore = null
  
  @CommandHandler
  private def handle(equipmentCreateCommand: EquipmentCreateCommand) = {
    var obj : EquipmentAggregate = null
    if(equipmentCreateCommand.oid!=null) {
      debug("create equipment with oid="+equipmentCreateCommand.oid)      
      obj = new EquipmentAggregate().aggregateByOid(equipmentCreateCommand.oid, equipmentCreateCommand.data)
    }
    else{
      debug("oid not specified")
      val oid=new ObjectId()
      obj = new EquipmentAggregate().aggregateByOid(oid, equipmentCreateCommand.data)
      debug("generate new ID="+oid)
    }
      equipmentsAggregatesRepository.add(obj)
      obj.oid    
  }
  @CommandHandler
  private def handle(equipmentDataSetCommand: EquipmentDataSetCommand) = {
    debug("received update command=" + equipmentDataSetCommand)
    val equipmentAggregate = retrieveEquipmentAggregate(equipmentDataSetCommand.oid)
    debug("retrieveEquipmentAggregate=" + equipmentAggregate)
    equipmentAggregate.processEquipmentDataSet(equipmentDataSetCommand.data)
    
  }
  @CommandHandler
   private def handle(command: EquipmentDeleteCommand ) {
        debug("Equipment delete command=" + command.oid)
        val equipmentAggregate = retrieveEquipmentAggregate(command.oid)
        debug("retrieveEquipmentAggregate=" + equipmentAggregate)
        equipmentAggregate.processEquipmentRemoving(command.oid)        
    }

  @CommandHandler
  private def handle(command: EquipmentRemoveCommand): Unit = {
    debug("Equipment remove command=" + command.oid)
    val equipmentAggregate = retrieveEquipmentAggregate(command.oid)
    debug("retrieveEquipmentAggregate=" + equipmentAggregate)
    equipmentAggregate.processEquipmentRemovingNew(command.oid)
  }

  @CommandHandler
  private def handle(command: EquipmentRestoreCommand): Unit = {
    debug("Equipment remove command=" + command.oid)
    val equipmentAggregate = retrieveEquipmentAggregate(command.oid)
    debug("retrieveEquipmentAggregate=" + equipmentAggregate)
    equipmentAggregate.processEquipmentRestoring(command.oid)
  }


  @CommandHandler
   private def handle(command: EquipmentObjectChangeCommand ) {
        debug("EquipmentObjectChangeCommand=" + command.oid)
        val equipmentAggregate = retrieveEquipmentAggregate(command.oid)
        debug("retrieveEquipmentAggregate=" + equipmentAggregate)
        equipmentAggregate.processObjectChange(command.prevObject,command.newObject)
        
   }
  private[this] def retrieveEquipmentAggregate(oid: ObjectId): EquipmentAggregate = {
    require(oid != null, "oid cant be null")
    val exists = Exception.catching(classOf[EventStreamNotFoundException]).opt(
      eventsStore.readEvents("EquipmentAggregate", oid).hasNext).getOrElse(false)
    debug("exists "+exists)
//
//    if (!exists) {
//      info("creating new equipment " + oid)
//      val obj = new EquipmentAggregate(oid,Map.empty)
//      equipmentsAggregatesRepository.add(obj)
//      obj
//    } else 
//    
      try {
      equipmentsAggregatesRepository.load(oid)
      }
      catch {
        case e: Exception => {throw new RuntimeException("Агрегат не найден: " + Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage))}
      }
  }
}

object EquipmentAggregate {
  /**
   * Returns key-value pairs which differs in oldData and newData or persists in newData and doesn't persists in oldData.
   * In other words returns newData minus pairs than already exists in oldData and are equal to newData ones
   * @param oldData old map pairs
   * @param newData new map pairs
   * @return diff map
   */
  def detectChanged(oldData: scala.collection.Map[String, Serializable],
                    newData: scala.collection.Map[String, Serializable]): HashMap[String, Serializable] = {  
    val builder = HashMap.newBuilder[String, Serializable]
//    val removedKeys = oldData -- newData.keys
    for ((k, v) <- newData) {
      oldData.get(k) match {
        case Some(old) if old != v => builder += ((k, v))
        case None => builder += ((k, v))
        case _ =>
      }
    }

//    for (removedKey <- removedKeys) {
//      builder += ((removedKey._1, null))
//    }

    val result = builder.result()
    result
  }

}
