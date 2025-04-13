package ru.sosgps.wayrecall.billing.user.permission.events;

import org.bson.types.ObjectId;

import java.util.HashMap;

/**
 * Created by nickl on 02.07.14.
 */
abstract public class AbstractPermissionEvent {

    abstract public ObjectId getPermissionId();

    abstract public HashMap<String, Object> getData();

    public String getRecordType(){
        return (String) getData().get("recordType");
    }

    public Object getItemId(){
        return getData().get("item_id");
    }

    public String getUserId(){
        return (String) getData().get("userId");
    }

    public Object getPermissions(){
        return getData().get("permissions");
    }

}
