
package ru.sosgps.wayrecall.billing.equipment.types.commands;

import java.io.Serializable;
import java.util.Map;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;

public class EquipmentTypesDataSetCommand implements Serializable,CommandEntityInfo{
 @TargetAggregateIdentifier
    private final ObjectId eqTypeId; 
    private final Map<String, Object> data;

    public Map<String, Object> getData() {
        return data;
    }

    public ObjectId getEqTypeId() {
        return eqTypeId;
    }
    public EquipmentTypesDataSetCommand(ObjectId eqTypeId,Map<String, Object> data) {
        this.eqTypeId = eqTypeId;     
        this.data=data;
    }
    public String getEntity(){
        return "EquipmentTypes";
    }
    public Object getEntityId(){
        return this.getEqTypeId();
    }
}
