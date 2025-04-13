
package ru.sosgps.wayrecall.billing.equipment.types;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.billing.equipment.types.commands.*;
import ru.sosgps.wayrecall.billing.equipment.types.events.*;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;
import ru.sosgps.wayrecall.utils.SpringContextCommandBusWrapper;


public class EquipmentTypesAggregate extends AbstractAnnotatedAggregateRoot implements Serializable {
    
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EquipmentTypesAggregate.class);

    @AggregateIdentifier
    ObjectId eqTypeId;
    Map<String, Object> data = new HashMap<>();

    public EquipmentTypesAggregate() {

    }
    
    @CommandHandler
    public EquipmentTypesAggregate(EquipmentTypesCreateCommand command) {
        log.debug("EquipmentType create command=" + command.getEqTypeId());
        log.debug("EquipmentType data=" + command.getData());
        checkDuplicate(command.getEqTypeId(), command.getData());
        apply(new EquipmentTypesCreateEvent(command.getEqTypeId(), new HashMap<>(command.getData())));
    }
    @EventHandler
    public void onEqTypeCreate(EquipmentTypesCreateEvent event){
        this.eqTypeId = event.getEqTypeId();
        this.data.putAll(event.getData());
    }
    
    @CommandHandler 
    public void setEqTypeData(EquipmentTypesDataSetCommand command) {
        log.debug("EqType data set command=" + command.getEqTypeId());
        processDataSet(command.getData());
    }
    @EventHandler
    public void onSetEqTypeData(EquipmentTypesDataSetEvent event){
        this.data.putAll(event.getData());
    }
        
    @CommandHandler 
    public void deleteEqType(EquipmentTypesDeleteCommand command) {
        log.debug("EqType delete command=" + command.getEqTypeId());
        apply(new EquipmentTypesDeleteEvent(command.getEqTypeId()));
    }
    
    private void checkDuplicate(ObjectId eqTypeId, Map<String, Object> data) {      
        log.debug("checkDuplicate");
        if (data != null && data.get("type") != null && data.get("mark") != null && data.get("model") != null) {            
            DBObject exsistEqType= getMdbm().getDatabase().underlying().getCollection("equipmentTypes").findOne(new BasicDBObject("type", data.get("type")).append("mark", data.get("mark")).append("model", data.get("model")));
            if (exsistEqType != null && !exsistEqType.get("_id").equals(eqTypeId)) {
                throw new IllegalArgumentException("Такая запись уже существует");
            }
        }
    }
    
     private void processDataSet(Map<String, Object> data) {
        Map<String, Object> changedData = ObjectsRepositoryReader.detectChanged(this.data, data);
        log.debug("Changed data" + changedData);
        if (!changedData.isEmpty()) {
            checkDuplicate(this.eqTypeId, data);
            apply(new EquipmentTypesDataSetEvent(this.eqTypeId, new HashMap<>(changedData)));
        }
    }

    public MongoDBManager getMdbm() {
        return SpringContextCommandBusWrapper.getApplicationContext().getBean(MongoDBManager.class);
    }
}
