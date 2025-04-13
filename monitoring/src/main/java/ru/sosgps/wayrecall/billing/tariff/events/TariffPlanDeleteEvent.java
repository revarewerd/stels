
package ru.sosgps.wayrecall.billing.tariff.events;

import java.io.Serializable;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.WayrecallAxonEvent;

public class TariffPlanDeleteEvent implements Serializable,WayrecallAxonEvent {
private final ObjectId tariffId;
    public String toHRString(){
        return "";
    }
    public TariffPlanDeleteEvent(ObjectId tariffId) {
        this.tariffId = tariffId;
    }

    public ObjectId getTariffId() {
        return tariffId;
    }
}
