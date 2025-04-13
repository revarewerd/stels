package ru.sosgps.wayrecall.billing.object;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import ru.sosgps.wayrecall.billing.RemovalRestorationManager;
import ru.sosgps.wayrecall.billing.security.PermissionsEditor;
import ru.sosgps.wayrecall.core.*;

import java.util.Objects;

@Order(100)
public class AggregateRepoDeleter {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AggregateRepoDeleter.class);

    @Autowired
    RemovalRestorationManager rrm;

    @Autowired
    ObjectsRepositoryWriter writer;

    @Autowired
    PermissionsEditor permEd;

    @Autowired
    MongoDBManager mdbm;

    @EventHandler
    void handleEquipmentDeletedEvent(EquipmentDeletedEvent event) {
        log.debug("processing "  + event);
        rrm.deleteEquipment(event.oid());
    }

    @EventHandler
    void handleEquipmentRemovedEvent(EquipmentRemovedEvent event) {
        log.debug("processing " + event);
        rrm.removeEquipment(event.oid());
    }



    @EventHandler
    void handleEquipmentRestoredEvent(EquipmentRestoredEvent event) {
        log.debug("processing " + event);
        rrm.restoreEquipment( event.oid());
    }


    @EventHandler
    void handleObjectDeletedEvent(ObjectDeletedEvent event) {
        log.debug("processing "  + event);
        rrm.deleteObject(event.uid());
    }

    @EventHandler
    void handleObjectRemovedEvent(ObjectRemovedEvent event) {
        log.debug("processing " + event);
        rrm.removeObject(event.uid());
    }

    @EventHandler
    void handleObjectRestoredEvent(ObjectRestoredEvent event) {
        log.debug("processing " + event);
        rrm.restoreObject(event.uid());
    }
}

