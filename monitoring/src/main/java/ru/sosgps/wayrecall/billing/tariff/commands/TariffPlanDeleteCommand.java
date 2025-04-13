
package ru.sosgps.wayrecall.billing.tariff.commands;

import java.io.Serializable;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;

public class TariffPlanDeleteCommand implements Serializable, CommandEntityInfo {
    @TargetAggregateIdentifier
    private final ObjectId tariffId;

    public TariffPlanDeleteCommand(ObjectId tariffId) {
        this.tariffId = tariffId;
    }

    public ObjectId getTariffId() {
        return tariffId;
    }

    public String getEntity(){
        return "Tariff";
    }
    public Object getEntityId(){
        return this.getTariffId();
    }

}
