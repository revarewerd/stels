package ru.sosgps.wayrecall.billing

import org.atmosphere.cpr.{BroadcasterFactory, Broadcaster}
import org.axonframework.eventhandling.annotation.EventHandler
import org.springframework.core.annotation.Order
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.events.{SimpleEvent, PredefinedEvents, EventsStore, DataEvent}
import ru.sosgps.wayrecall.utils.io.Utils._
import org.springframework.beans.factory.annotation.Autowired
import scala.collection.JavaConverters.mapAsJavaMapConverter
import ru.sosgps.wayrecall.billing.account.events._
import java.{io, util}


@Order(-50)
class LegacyEventHandlersAdapter extends grizzled.slf4j.Logging {
  @Autowired
  var es: EventsStore = null

  @EventHandler
  private def handleObjectCreatedEvent(event: ObjectCreatedEvent) {
    debug("handleObjectCreatedEvent:" + event)
    es.publish(new DataEvent(mapToSerializable(event.objectData.asJava), PredefinedEvents.objectChange, event.uid))
  }

  @EventHandler
  private def handleObjectChangedEvent(event: ObjectDataChangedEvent) {
    debug("handleObjectChangedEvent:" + event)
    es.publish(new DataEvent(mapToSerializable[java.io.Serializable](event.changedAttributes.asJava), PredefinedEvents.objectChange, event.uid))
  }

  @EventHandler
  private def handleObjectDeletedEvent(event: ObjectDeletedEvent) {
    debug("handleObjectDeletedEvent:" + event)
    es.publish(new DataEvent(new java.util.HashMap(), PredefinedEvents.objectChange, event.uid))
  }

  @EventHandler
  private def handleObjectRemovedEvent(event: ObjectRemovedEvent) {
    debug("handleObjectRemovedEvent:" + event)
    es.publish(new DataEvent(new java.util.HashMap(), PredefinedEvents.objectChange, event.uid))
  }

  @EventHandler
  private def handleObjectRestoredEvent(event: ObjectRestoredEvent) {
    debug("handleObjectRestoredEvent:" + event)
    es.publish(new DataEvent(new java.util.HashMap(), PredefinedEvents.objectChange, event.uid))
  }


  @EventHandler private def handleAccountCreatedEvent(event: AccountCreateEvent) {
    debug("handleAccountCreatedEvent:" + event)
    es.publish(new SimpleEvent(PredefinedEvents.accountChange, event.getAccountId.toString));
  }

  @EventHandler private def handleAccountDataSetEvent(event: AccountDataSetEvent) {
    debug("handleAccountDataSetEvent:" + event)
    es.publish(new SimpleEvent(PredefinedEvents.accountChange, event.getAccountId.toString));
  }

  @EventHandler private def handleAccountDeleteEvent(event: AccountDeleteEvent) {
    debug("handleAccountDeleteEvent:" + event)
    es.publish(new SimpleEvent(PredefinedEvents.accountChange, event.getAccountId.toString));
  }

  @EventHandler private def handleAccountRemoveEvent(event: AccountRemoveEvent) {
    debug("handleAccountRemoveEvent:" + event)
    es.publish(new SimpleEvent(PredefinedEvents.accountChange, event.getAccountId.toString));
  }

  @EventHandler private def handleAccountRestoreEvent(event: AccountRestoreEvent ): Unit = {
    debug("handleAccountRestoreEvent:" + event)
    es.publish(new SimpleEvent(PredefinedEvents.accountChange, event.getAccountId.toString));
  }

  @EventHandler private def handleEquipmentCreateEvent(event: EquipmentCreatedEvent) {
    debug("handleEquipmentCreateEvent:" + event)
    es.publish(new DataEvent(mapToSerializable(event.data.asJava), PredefinedEvents.equipmentChange, event.oid.toString))

  }

  @EventHandler private def handleEquipmentDataChangedEvent(event: EquipmentDataChangedEvent) {
    debug("handleEquipmentDataChangedEvent:" + event)
    es.publish(new DataEvent(mapToSerializable[java.io.Serializable](event.changedAttributes.asJava), PredefinedEvents.equipmentChange, event.oid.toString))

  }

  @EventHandler private def handleEquipmentObjectChangedEvent(event: EquipmentObjectChangedEvent) {
    debug("handleEquipmentObjectChangedEvent:" + event)
    val data = Map[String, io.Serializable]("prev" -> event.prevObject.orNull, "new" -> event.newObject.orNull)
    es.publish(new DataEvent(mapToSerializable[java.io.Serializable](data.asJava), PredefinedEvents.equipmentChange, event.oid.toString))
    event.newObject.foreach(uid =>
      es.publish(new DataEvent(mapToSerializable[java.io.Serializable](data.asJava), PredefinedEvents.objectChange, uid)))
    event.prevObject.foreach(uid =>
      es.publish(new DataEvent(mapToSerializable[java.io.Serializable](data.asJava), PredefinedEvents.objectChange, uid)))
  }

  @EventHandler private def handleEquipmentDeletedEvent(event: EquipmentDeletedEvent) {
    debug("handleEquipmentDeletedEvent:" + event)
    es.publish(new DataEvent(new java.util.HashMap(), PredefinedEvents.equipmentChange, event.oid.toString))
  }


  @EventHandler private def handleEquipmentRemovedEvent(event: EquipmentRemovedEvent) {
    debug("handleEquipmentRemovedEvent:" + event)
    es.publish(new DataEvent(new java.util.HashMap(), PredefinedEvents.equipmentChange, event.oid.toString))
  }

  @EventHandler private def handleEquipmentRestoredEvent(event: EquipmentRestoredEvent) {
    debug("handleEquipmentRestoredEvent:" + event)
    es.publish(new DataEvent(new java.util.HashMap(), PredefinedEvents.equipmentChange, event.oid.toString))
  }

}
