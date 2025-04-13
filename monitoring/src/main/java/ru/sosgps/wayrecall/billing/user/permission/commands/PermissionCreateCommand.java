package ru.sosgps.wayrecall.billing.user.permission.commands;

import java.io.Serializable;
import java.util.Map;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;

public class PermissionCreateCommand  implements Serializable, CommandEntityInfo{
    @TargetAggregateIdentifier
    private final ObjectId permissionId; 

    public ObjectId getPermissionId() {
        return permissionId;
    }
    private final Map<String, Object> data;

    public Map<String, Object> getData() {
        return data;
    }

    
    public PermissionCreateCommand(ObjectId permissionId,Map<String, Object> data) {
        this.permissionId = permissionId;     
        this.data=data;
    }

    public String getEntity(){
        return "Permission";
    }
    public Object getEntityId(){
        return this.getPermissionId();
    }
}

