
package ru.sosgps.wayrecall.billing.equipment.types.commands;

import java.io.Serializable;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;

public class EquipmentTypesDeleteCommand implements Serializable, CommandEntityInfo{
@TargetAggregateIdentifier
    private final ObjectId eqTypeId; 

    public ObjectId getEqTypeId() {
        return eqTypeId;
    }
    public EquipmentTypesDeleteCommand(ObjectId eqTypeId) {
        this.eqTypeId = eqTypeId;     
    }
    public String getEntity(){
        return "EquipmentTypes";
    }
    public Object getEntityId(){
        return this.getEqTypeId();
    }
}

