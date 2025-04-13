package ru.sosgps.wayrecall.billing.user.events;

import java.io.Serializable;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.WayrecallAxonEvent;


public class UserDeleteEvent implements Serializable,WayrecallAxonEvent {
    private final ObjectId userId;

    public String toHRString(){
       return "";
    }

    public ObjectId getUserId() {
        return userId;
    }
    public UserDeleteEvent(ObjectId userId) {
        this.userId = userId;     
    }
}
