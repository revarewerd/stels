
package ru.sosgps.wayrecall.billing.equipment.types;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import javax.annotation.PostConstruct;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import ru.sosgps.wayrecall.billing.equipment.types.events.*;
import ru.sosgps.wayrecall.core.MongoDBManager;


public class EquipmentTypesEventHandler {
    
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EquipmentTypesEventHandler.class);

    @Autowired
    MongoDBManager mdbm = null;
    
    @Autowired
    CommandGateway commandGateway = null;
    
    DBCollection dbeqtypes = null;
    
    @PostConstruct
    private void init() {    
    dbeqtypes= mdbm.getDatabase().underlying().getCollection("equipmentTypes"); 
    }
    
    @EventHandler
    public void handleEqTypeCreatedEvent(EquipmentTypesCreateEvent event) { 
        log.debug("New type of equipment create="+ event.getEqTypeId());
        log.debug("eqType data="+ event.getData());
        
        BasicDBObject newEqType=new BasicDBObject("_id", event.getEqTypeId());
        if(event.getData()!=null) newEqType.putAll(event.getData());
        
        dbeqtypes.insert(
                newEqType,
                WriteConcern.SAFE);  
    }
    
    @EventHandler
    public void handleEqTypeDataSetEvent(EquipmentTypesDataSetEvent event) { 
        log.debug("eqType data set eqTypeId="+ event.getEqTypeId());
        log.debug("eqType data="+ event.getData()); 
        dbeqtypes.update(
                 new BasicDBObject("_id", event.getEqTypeId()),
                 new BasicDBObject("$set", event.getData()),
                 true, false, WriteConcern.SAFE);     
    }
    
    @EventHandler
    public void handleEqTypeDeleteEvent(EquipmentTypesDeleteEvent event) { 
        log.debug("Delete eqType eqTypeId="+ event.getEqTypeId());                    
        dbeqtypes.remove(new BasicDBObject("_id" , event.getEqTypeId()));
    }
}
