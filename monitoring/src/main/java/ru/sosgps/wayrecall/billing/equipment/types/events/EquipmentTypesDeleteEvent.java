
package ru.sosgps.wayrecall.billing.equipment.types.events;

import java.io.Serializable;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.WayrecallAxonEvent;

public class EquipmentTypesDeleteEvent implements Serializable,WayrecallAxonEvent {

    private final ObjectId eqTypeId;
    public String toHRString(){
        return "";
    }
    public ObjectId getEqTypeId() {
        return eqTypeId;
    }
    public EquipmentTypesDeleteEvent(ObjectId eqTypeId) {
        this.eqTypeId = eqTypeId;     
    }
}
