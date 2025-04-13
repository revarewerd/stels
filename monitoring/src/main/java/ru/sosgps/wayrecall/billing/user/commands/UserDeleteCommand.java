package ru.sosgps.wayrecall.billing.user.commands;

import java.io.Serializable;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;


public class UserDeleteCommand implements Serializable, CommandEntityInfo {
@TargetAggregateIdentifier
    private final ObjectId userId; 

    public ObjectId getUserId() {
        return userId;
    }
    public String getEntity(){
        return "User";
    }
    public Object getEntityId(){
        return this.getUserId();
    }
    public UserDeleteCommand(ObjectId userId) {
        this.userId = userId;     
    }
}
