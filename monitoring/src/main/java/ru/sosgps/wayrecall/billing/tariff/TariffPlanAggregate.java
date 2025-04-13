package ru.sosgps.wayrecall.billing.tariff;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.common.annotation.MetaData;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.billing.tariff.commands.*;
import ru.sosgps.wayrecall.billing.tariff.events.*;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.utils.SpringContextCommandBusWrapper;

public class TariffPlanAggregate extends AbstractAnnotatedAggregateRoot implements Serializable {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TariffPlanAggregate.class);
    
    @AggregateIdentifier
    ObjectId tariffId;
    Map<String, Object> data = new HashMap<>();

    public TariffPlanAggregate() {
    }
         
    @CommandHandler
    public TariffPlanAggregate(TariffPlanCreateCommand command, @MetaData("userRoles") String[] userRoles) {
        log.debug("TariffPlan create command=" + command.getTariffId());
        log.debug("TariffPlan data=" + command.getData());
        checkDuplicate(command.getTariffId(), command.getData());
        apply(new TariffPlanCreateEvent(command.getTariffId(),new HashMap<>(command.getData())));
    }
    
    private void checkDuplicate(ObjectId tariffId, Map<String, Object> data) {
        log.debug("checkDuplicate");
        if (data != null && data.get("name") != null) {
            DBObject exsistTariff = getMdbm().getDatabase().underlying().getCollection("tariffs").findOne(new BasicDBObject("name", data.get("name")));
            if (exsistTariff != null && !exsistTariff.get("_id").equals(tariffId)) {
                throw new IllegalArgumentException("Такая запись уже существует");
            }
        }
    }
    @EventHandler
    public void onCreateTariffCommand(TariffPlanCreateEvent event){
        this.tariffId=event.getTariffId();
        this.data.putAll(event.getData());    
    }
    
    @CommandHandler
    public void setTariffData(TariffPlanDataSetCommand command){
        processDataSet(command.getData());
    }
     private void processDataSet(Map<String, Object> data) {
        Map<String, Object> changedData = ObjectsRepositoryReader.detectChanged(this.data, data);
        log.debug("Changed data" + changedData);
        if (!changedData.isEmpty()) {
            checkDuplicate(this.tariffId, data);
            apply(new TariffPlanDataSetEvent(this.tariffId, new HashMap<>(changedData)));
        }
    }
    @EventHandler
    public void onSetTariffData(TariffPlanDataSetEvent event){        
        this.data.putAll(event.getData());    
    }
    @CommandHandler
    public void deleteTariffData(TariffPlanDeleteCommand command){
        log.debug("Tariff plan delete command=" + command.getTariffId());
        apply(new TariffPlanDeleteEvent(command.getTariffId()));
    }

    public MongoDBManager getMdbm() {
        return SpringContextCommandBusWrapper.getApplicationContext().getBean(MongoDBManager.class);
    }
}
