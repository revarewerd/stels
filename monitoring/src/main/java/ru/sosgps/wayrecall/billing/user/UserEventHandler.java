package ru.sosgps.wayrecall.billing.user;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import ru.sosgps.wayrecall.billing.security.PermissionsEditor;
import ru.sosgps.wayrecall.billing.user.events.*;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.initialization.WayrecallSessionManager;
import ru.sosgps.wayrecall.utils.web.package$;
import scala.Option;
import static ru.sosgps.wayrecall.utils.ScalaConverters.asScalaMapImm;
import static ru.sosgps.wayrecall.utils.ScalaConverters.nullable;


import java.util.HashMap;
import java.util.Map;

@Order(-100)
public class UserEventHandler {
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserEventHandler.class);
    @Autowired
    MongoDBManager mdbm = null;
    
    @Autowired
    CommandGateway commandGateway = null;
    
    @Autowired
    PermissionsEditor permEd = null;

    @Resource(name = "monitoringwebapp-SessionManager")
    WayrecallSessionManager sessonManager = null;
    
    DBCollection dbusers   = null;    
    
    @PostConstruct
    private void init() {
    dbusers = mdbm.getDatabase().underlying().getCollection("users");
    }
    
    @EventHandler
    public void handleUserCreatedEvent(UserCreateEvent event) { 
        ObjectId userId=event.getUserId();
        Map<String,Object> data=event.getData();
        log.debug("User create="+ userId);
        log.debug("user data="+ data);
        
        BasicDBObject newUser=new BasicDBObject("_id", userId);
        if(data!=null) newUser.putAll(data);
        dbusers.insert(
                newUser,
                WriteConcern.SAFE);
        ObjectId mainAccId=(ObjectId) data.get("mainAccId");
        if(mainAccId!=null){
            Option<DBObject> permRec = permEd.getPermissionsRecord(userId,"account", mainAccId);
            if(!permRec.isDefined()){
                log.debug("add permRec for mainAccId="+ mainAccId);
                Map <String,Object> permissions=new HashMap<String,Object>();
                permEd.addPermissionRecord(userId,"account",mainAccId,asScalaMapImm(permissions));
            }
        }
    }
    
    @EventHandler
    public void handleUserDataSetEvent(UserDataSetEvent event) {
        ObjectId userId=event.getUserId();
        log.debug("User data set userId="+ event.getUserId());
        Map<String,Object> data=event.getData();
        log.debug("User data="+ data);
        dbusers.update(
                 new BasicDBObject("_id", event.getUserId()),
                 new BasicDBObject("$set", data),
                 true, false, WriteConcern.SAFE);
        ObjectId mainAccId=(ObjectId) data.get("mainAccId");
        if(mainAccId!=null){
            Option<DBObject> permRec = permEd.getPermissionsRecord(userId,"account", mainAccId);
            if(!permRec.isDefined()){
                log.debug("add permRec for mainAccId="+ mainAccId);
                Map <String,Object> permissions=new HashMap<String,Object>();
                permEd.addPermissionRecord(userId,"account",mainAccId,asScalaMapImm(permissions));
            }
        }


        if (Boolean.FALSE.equals(data.get("enabled"))) {
            final DBObject userDbo = dbusers.findOne(userId);
            final Object login = userDbo.get("name");
            for (HttpSession httpSession : sessonManager.getActiveSessions()) {
                final Authentication auth = nullable(package$.MODULE$.getAuth(httpSession));
                if (auth != null && auth.getName().equals(login)){
                    log.debug("invalidating blocked user: {}", login);
                    httpSession.invalidate();
                }
            }
        }

    }
    
    @EventHandler
    public void handleUserDeleteEvent(UserDeleteEvent event) { 
        log.debug("Delete user userId="+ event.getUserId());        
        log.debug("Remove user permissions");
        permEd.removeUserPermissions(event.getUserId());
        dbusers.remove(new BasicDBObject("_id" , event.getUserId()));
    }
}
    
