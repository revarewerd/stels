package ru.sosgps.wayrecall.billing.user.commands;

import java.io.Serializable;
import java.util.Map;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;

public class UserCreateCommand  implements Serializable, CommandEntityInfo{
    @TargetAggregateIdentifier
    private final ObjectId userId; 
    private final Map<String, Object> data;

    public Map<String, Object> getData() {
        return data;
    }

    public ObjectId getUserId() {
        return userId;
    }
    public String getEntity(){
        return "User";
    }
    public Object getEntityId(){
        return this.getUserId();
    }
    public UserCreateCommand(ObjectId userId,Map<String, Object> data) {
        this.userId = userId;     
        this.data=data;
    }
}

