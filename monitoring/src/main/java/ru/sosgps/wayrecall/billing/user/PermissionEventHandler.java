
package ru.sosgps.wayrecall.billing.user;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import javax.annotation.PostConstruct;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import ru.sosgps.wayrecall.billing.user.permission.events.*;
import ru.sosgps.wayrecall.core.MongoDBManager;

@Order(-100)
public class PermissionEventHandler {
private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PermissionEventHandler.class);
    @Autowired
    MongoDBManager mdbm = null;
    
    @Autowired
    CommandGateway commandGateway = null;    
    
    DBCollection dbpermissions = null;
    
    @PostConstruct
    private void init() {    
    dbpermissions= mdbm.getDatabase().underlying().getCollection("usersPermissions"); 
    }
    
    @EventHandler
    public void handlePermissionCreatedEvent(PermissionCreateEvent event) { 
        log.debug("Permission create="+ event.getPermissionId());
        log.debug("permission data="+ event.getData());
        
        BasicDBObject newPermission= new BasicDBObject("_id", event.getPermissionId());
        if(event.getData()!=null) newPermission.putAll(event.getData());
        
        dbpermissions.insert(
                newPermission,
                WriteConcern.SAFE);  
    }
    
    @EventHandler
    public void handlePermissionDataSetEvent(PermissionDataSetEvent event) { 
        log.debug("Permission data set userId="+ event.getPermissionId());
        log.debug("permission data="+ event.getData()); 
       
        dbpermissions.update(
                 new BasicDBObject("_id", event.getPermissionId()),
                 new BasicDBObject("$set", event.getData()),
                 true, false, WriteConcern.SAFE);     
    }
    
    @EventHandler
    public void handlePermissionDeleteEvent(PermissionDeleteEvent event) { 
        log.debug("Permission Id="+ event.getPermissionId());        
        log.debug("remove permission");        
        dbpermissions.remove(new BasicDBObject("_id",event.getPermissionId()));
    }
}
