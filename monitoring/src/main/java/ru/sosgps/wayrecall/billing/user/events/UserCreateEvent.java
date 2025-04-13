package ru.sosgps.wayrecall.billing.user.events;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.WayrecallAggregateTranslate;
import ru.sosgps.wayrecall.core.WayrecallAxonEvent;
import scala.Option;
import scala.runtime.AbstractFunction0;


public class UserCreateEvent implements Serializable,WayrecallAxonEvent {
    private final ObjectId userId; 
    private final HashMap<String, Object> data;

    @Override
    public Option<String> getInitialName() {
        return Option.apply(Objects.toString(data.get("name"), null));
    }

    public HashMap<String, Object> getData() {
        return data;
    }
    public String toHRString(){
        if(this.data==null) return "";
        StringBuilder sb = new StringBuilder();
        HashMap<String, Object> eventdata = new HashMap<>();
        eventdata.putAll(this.data);
        for (final Map.Entry<String, Object> item : eventdata.entrySet()) {
            sb.append(WayrecallAggregateTranslate.UserTranslate().getOrElse(item.getKey(), new AbstractFunction0<String>() {
                @Override
                public String apply() {
                    return item.getKey();
                };
            }));
            sb.append(":\'");
            sb.append(item.getValue());
            sb.append("\'; ");
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> toHRTable() {
        if(this.data==null) return new HashMap<>();
        HashMap<String, Object> eventdata = new HashMap<>();
        for (final Map.Entry<String, Object> item : data.entrySet()) {
            String key = WayrecallAggregateTranslate.UserTranslate().getOrElse(item.getKey(), new AbstractFunction0<String>() {
                @Override
                public String apply() {
                    return item.getKey();
                }
            });
            eventdata.put(key,item.getValue());
        }
        return eventdata;
    }

    public ObjectId getUserId() {
        return userId;
    }
    public UserCreateEvent(ObjectId userId,HashMap<String, Object> data) {
        this.userId = userId;     
        this.data=data;
    }
}
