package ru.sosgps.wayrecall.workflow

import java.io.Serializable
import javax.annotation.Resource

import org.axonframework.commandhandling.annotation.{TargetAggregateIdentifier, CommandHandler}
import org.axonframework.common.annotation.MetaData
import org.axonframework.eventhandling.annotation.EventHandler
import org.axonframework.eventsourcing.annotation.{AggregateIdentifier, AbstractAnnotatedAggregateRoot}
import org.axonframework.eventstore.{EventStreamNotFoundException, EventStore}
import org.axonframework.repository.Repository
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader}


import scala.beans.BeanProperty
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.control.Exception

/**
 * Created by IVAN on 15.01.2015.
 */
class TicketAggregate  extends AbstractAnnotatedAggregateRoot[ObjectId] with Serializable with grizzled.slf4j.Logging {

  private val data = new mutable.HashMap[String, Serializable]

  @AggregateIdentifier
  var id: ObjectId = null

  def aggregateByOid(id: ObjectId = new ObjectId(), data: Map[String, Serializable]) = {
    this.id = id
    this.data++=data
    this(new TicketCreatedEvent(id,data))
    this
  }

  def processTicketDataSet(map: Map[String, Serializable]) = {

    val changed = TicketAggregate.detectChanged(this.data, map)
    debug("processEquipmentDataSet changed:" + changed)
    val toPublish = new ListBuffer[AnyRef]
    if (changed.nonEmpty) {
      toPublish += new TicketDataChangedEvent(this.id, changed)
    }
    else debug("no changes")

    toPublish.foreach(this.apply)
  }

  def processTicketRemoving(oid: ObjectId) {
    this(new TicketDeletedEvent(this.id, this.data))
  }

  @EventHandler private def handleTicketCreatedEvent(event: TicketCreatedEvent): Unit = {
    debug("TicketCreatedEven is =" + event.toString)
    this.id = event.id
    this.data ++= Option(event.data).getOrElse(Map.empty)
  }

  @EventHandler private def handleTicketDataChangedEvent(event: TicketDataChangedEvent): Unit = {
    debug("TicketDataChangedEvent is =" + event.toString)
    this.id = event.id
    this.data --= event.changedAttributes.collect({case (k, v) if v == null => k})
    this.data ++= event.changedAttributes
  }

  override def toString = "TicketAggregate( id =" + id + ", data=" + data + ")"
}

object TicketAggregate {

  def detectChanged(oldData: scala.collection.Map[String, Serializable],
                    newData: scala.collection.Map[String, Serializable]): HashMap[String, Serializable] = {
    val builder = HashMap.newBuilder[String, Serializable]
    //val removedKeys = oldData -- newData.keys
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

class TicketCommandHandler extends grizzled.slf4j.Logging {

  @Resource
  @BeanProperty
  var ticketAggregatesRepository: Repository[TicketAggregate] = null

  @Autowired
  @BeanProperty
  var eventsStore: EventStore = null

  @CommandHandler
  private def handle(ticketCreateCommand: TicketCreateCommand) = {
    var obj : TicketAggregate = null
    if(ticketCreateCommand.id!=null) {
      debug("create ticket with id="+ticketCreateCommand.id)
      obj = new TicketAggregate().aggregateByOid(ticketCreateCommand.id, ticketCreateCommand.data)
    }
    else{
      debug("id not specified")
      val id=new ObjectId()
      obj = new TicketAggregate().aggregateByOid(id, ticketCreateCommand.data)
      debug("generate new ID="+id)
    }
    ticketAggregatesRepository.add(obj)
    obj.id
  }

  @CommandHandler
  private def handle(ticketDataSetCommand: TicketDataSetCommand) = {
    debug("Ticket update command=" + ticketDataSetCommand)
    val equipmentAggregate = retrieveTicketAggregate(ticketDataSetCommand.id)
    debug("retrieve TicketAggregate=" + equipmentAggregate)
    equipmentAggregate.processTicketDataSet(ticketDataSetCommand.data)

  }

  @CommandHandler
  private def handle(command: TicketDeleteCommand ) {
    debug("Ticket delete command=" + command.id)
    val equipmentAggregate = retrieveTicketAggregate(command.id)
    debug("retrieve TicketAggregate=" + equipmentAggregate)
    equipmentAggregate.processTicketRemoving(command.id)
  }

  private[this] def retrieveTicketAggregate(id: ObjectId):TicketAggregate = {
    require(id != null, "oid cant be null")
    val exists = Exception.catching(classOf[EventStreamNotFoundException]).opt(
      eventsStore.readEvents("TicketAggregate", id).hasNext).getOrElse(false)
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
      ticketAggregatesRepository.load(id)
    }
    catch {
      case e: Exception => {throw new RuntimeException("Агрегат не найден: " + Option(e.getCause).map(_.getMessage).getOrElse(e.getMessage))}
    }
  }
}