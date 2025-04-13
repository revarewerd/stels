package ru.sosgps.wayrecall.billing.user;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.billing.user.permission.commands.*;
import ru.sosgps.wayrecall.billing.user.permission.events.*;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;
import ru.sosgps.wayrecall.utils.SpringContextCommandBusWrapper;

public class PermissionAggregate extends AbstractAnnotatedAggregateRoot implements Serializable {
    
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PermissionAggregate.class);
    
    @AggregateIdentifier
    ObjectId permissionId;
    Map<String, Object> data = new HashMap<>();

    public PermissionAggregate() {

    }
    
     @CommandHandler
    public PermissionAggregate(PermissionCreateCommand command) {
        log.debug("Permission create command=" + command.getPermissionId());
        log.debug("permission data=" + command.getData());
        apply(new PermissionCreateEvent(command.getPermissionId(), new HashMap<>(command.getData())));
    }
    
    @EventHandler
    public void onPermissionCreate(PermissionCreateEvent event) {
        this.permissionId = event.getPermissionId();
        this.data.putAll(event.getData());
    }
    @CommandHandler
    public void setPermissionData(PermissionDataSetCommand command) {
        processDataSet(command.getData());
    }

    private void processDataSet(Map<String, Object> data) {
        Map<String, Object> changedData = ObjectsRepositoryReader.detectChanged(this.data, data);
        log.debug("Changed data" + changedData);
        if (!changedData.isEmpty()) {
            final HashMap<String, Object> data1 = new HashMap<>(changedData);
            data1.put("userId", Objects.requireNonNull(data.get("userId")));
            data1.put("recordType", Objects.requireNonNull(data.get("recordType")));
            data1.put("item_id", Objects.requireNonNull(data.get("item_id")));
            apply(new PermissionDataSetEvent(this.permissionId, data1));
        }
    }

    @EventHandler
    public void onSetNewData(PermissionDataSetEvent event) {
        this.data.putAll(event.getData());
    }

    @CommandHandler
    public void deleteAccount(PermissionDeleteCommand command) {
        log.debug("Permission delete command=" + command.getPermissionId());
        apply(new PermissionDeleteEvent(command.getPermissionId(), new HashMap<>(command.getData())));
    }
    public MongoDBManager getMdbm() {
        return SpringContextCommandBusWrapper.getApplicationContext().getBean(MongoDBManager.class);
    }
}
