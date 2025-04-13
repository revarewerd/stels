package ru.sosgps.wayrecall.billing.user;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.billing.user.commands.*;
import ru.sosgps.wayrecall.billing.user.events.*;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;
import ru.sosgps.wayrecall.utils.SpringContextCommandBusWrapper;

public class UserAggregate extends AbstractAnnotatedAggregateRoot implements Serializable {
 
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserAggregate.class);

    @AggregateIdentifier
    ObjectId userId;
    Map<String, Object> data = new HashMap<>();

    public UserAggregate() {

    }
    
    @CommandHandler
    public UserAggregate(UserCreateCommand command) {
        log.debug("User create command=" + command.getUserId());
        log.debug("User data=" + command.getData());
        checkDuplicate(command.getUserId(), command.getData());
        apply(new UserCreateEvent(command.getUserId(), new HashMap<>(command.getData())));
    }
    
    @EventHandler
    public void onUserCreate(UserCreateEvent event) {
        this.userId = event.getUserId();
        this.data.putAll(event.getData());
    }
    private void checkDuplicate(ObjectId userId, Map<String, Object> data) {
        log.debug("checkDuplicate");
        if (data != null && data.get("name") != null) {
            DBObject exsistUser = getMdbm().getDatabase().underlying().getCollection("users").findOne(new BasicDBObject("name", data.get("name")));
            if (exsistUser != null && !exsistUser.get("_id").equals(userId)) {
                throw new IllegalArgumentException("Такая запись уже существует");
            }
        }
    }
    @CommandHandler
    public void setUserData(UserDataSetCommand command) {
        processDataSet(command.getData());
    }

    private void processDataSet(Map<String, Object> data) {
        Map<String, Object> changedData = ObjectsRepositoryReader.detectChanged(this.data, data);
        log.debug("Changed data" + changedData);
        if (!changedData.isEmpty()) {
            checkDuplicate(this.userId, data);
            apply(new UserDataSetEvent(this.userId, new HashMap<>(changedData)));
        }
    }

    @EventHandler
    public void onSetUserData(UserDataSetEvent event) {
        this.data.putAll(event.getData());
    }

    @CommandHandler
    public void deleteUser(UserDeleteCommand command) {
        log.debug("User delete command=" + command.getUserId());
        apply(new UserDeleteEvent(command.getUserId()));
    }

    public MongoDBManager getMdbm() {
        return SpringContextCommandBusWrapper.getApplicationContext().getBean(MongoDBManager.class);
    }
}
