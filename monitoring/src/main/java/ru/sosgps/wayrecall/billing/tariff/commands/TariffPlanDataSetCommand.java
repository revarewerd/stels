
package ru.sosgps.wayrecall.billing.tariff.commands;

import java.io.Serializable;
import java.util.Map;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;

public class TariffPlanDataSetCommand implements Serializable, CommandEntityInfo {
@TargetAggregateIdentifier
    private final ObjectId tariffId;
    private final Map<String,Object> data;

    public TariffPlanDataSetCommand(ObjectId tariffId, Map<String, Object> data) {
        this.tariffId = tariffId;
        this.data = data;
    }

    public ObjectId getTariffId() {
        return tariffId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getEntity(){
        return "Tariff";
    }
    public Object getEntityId(){
        return this.getTariffId();
    }
}
