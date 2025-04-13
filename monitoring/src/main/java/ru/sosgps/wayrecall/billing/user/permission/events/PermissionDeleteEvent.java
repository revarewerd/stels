package ru.sosgps.wayrecall.billing.user.permission.events;

import java.io.Serializable;
import java.util.HashMap;

import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.WayrecallAxonEvent;


public class PermissionDeleteEvent extends AbstractPermissionEvent  implements Serializable,WayrecallAxonEvent {

    private final ObjectId permissionId;

    public String toHRString(){
        return "";
    }

    public ObjectId getPermissionId() {
        return permissionId;
    }

    public PermissionDeleteEvent(ObjectId permissionId, HashMap<String, Object> data) {
        this.permissionId = permissionId;
        this.data = data;
    }

    private final HashMap<String, Object> data;

    public HashMap<String, Object> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "PermissionDeleteEvent{" +
                "permissionId=" + permissionId +
                ", data=" + data +
                '}';
    }
}
