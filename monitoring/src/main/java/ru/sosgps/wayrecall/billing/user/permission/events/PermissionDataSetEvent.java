package ru.sosgps.wayrecall.billing.user.permission.events;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.WayrecallAggregateTranslate;
import ru.sosgps.wayrecall.core.WayrecallAxonEvent;
import scala.runtime.AbstractFunction0;

public class PermissionDataSetEvent extends AbstractPermissionEvent  implements Serializable,WayrecallAxonEvent {
    private final ObjectId permissionId; 

    public ObjectId getPermissionId() {
        return permissionId;
    }
    private final HashMap<String, Object> data;

    public HashMap<String, Object> getData() {
        return data;
    }

    public String toHRString(){
        if(this.data==null) return "";
        StringBuilder sb = new StringBuilder();
        HashMap<String, Object> eventdata = new HashMap<>();
        eventdata.putAll(this.data);
        Map<String,Object> permissions = (Map<String,Object>) eventdata.remove("permissions");
        for (final Map.Entry<String, Object> item : eventdata.entrySet()) {
            sb.append(WayrecallAggregateTranslate.PermissionTranslate().getOrElse(item.getKey(), new AbstractFunction0<String>() {
                @Override
                public String apply() {
                    return item.getKey();
                };
            }));
            sb.append(":\'");
            sb.append(item.getValue());
            sb.append("\'; ");
        }
        if (permissions != null && ! permissions.isEmpty()) {
            sb.append("права:[");
            for(final Map.Entry<String, Object> citem : permissions.entrySet()) {
                sb.append(WayrecallAggregateTranslate.PermissionTranslate().getOrElse(citem.getKey(), new AbstractFunction0<String>() {
                    @Override
                    public String apply() {
                        return citem.getKey();
                    };
                }));
                sb.append(":\'");
                sb.append(citem.getValue());
                sb.append("\'; ");
            }
            sb.append("]; ");
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> toHRTable() {
        if(this.data==null) return new HashMap<>();
        HashMap<String, Object> eventdata = new HashMap<>();
        eventdata.putAll(this.data);
        Map<String,Object> permissions = (Map<String,Object>) eventdata.remove("permissions");

        Map<String,Object> result = new HashMap<>();
        for (final Map.Entry<String, Object> item : eventdata.entrySet()) {
            String key = WayrecallAggregateTranslate.UserTranslate().getOrElse(item.getKey(), new AbstractFunction0<String>() {
                @Override
                public String apply() {
                    return item.getKey();
                }
            });
            result.put(key,item.getValue());
        }
        Map<String,Object> permissionsTranslated = new LinkedHashMap<>();
        if (permissions != null && ! permissions.isEmpty()) {
            for(final Map.Entry<String, Object> citem : permissions.entrySet()) {
                String key = WayrecallAggregateTranslate.PermissionTranslate().getOrElse(citem.getKey(), new AbstractFunction0<String>() {
                    @Override
                    public String apply() {
                        return citem.getKey();
                    };
                });
                permissionsTranslated.put(key,citem.getValue());
            }
            result.put("Права", permissionsTranslated);
        }
        return result;
    }

    public PermissionDataSetEvent(ObjectId permissionId,HashMap<String, Object> data){
        if (!data.containsKey("userId"))
            throw new IllegalArgumentException("no userId");
        if (!data.containsKey("recordType"))
            throw new IllegalArgumentException("no recordType");
        if (!data.containsKey("item_id"))
            throw new IllegalArgumentException("no item_id");
        this.permissionId = permissionId;
        this.data = data;
    }

    @Override
    public String toString() {
        return "PermissionDataSetEvent{" +
                "permissionId=" + permissionId +
                ", data=" + data +
                '}';
    }
}
