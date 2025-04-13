package ru.sosgps.wayrecall.billing;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.annotation.AnnotationEventListenerAdapter;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.bson.types.ObjectId;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.WebApplicationContext;
import ru.sosgps.wayrecall.billing.account.AccountsStoreService;
import ru.sosgps.wayrecall.billing.account.events.*;
import ru.sosgps.wayrecall.billing.equipment.EquipmentStoreService;
import ru.sosgps.wayrecall.billing.equipment.types.events.*;
import ru.sosgps.wayrecall.billing.support.SupportRequestEDS;
import ru.sosgps.wayrecall.billing.tariff.events.*;
import ru.sosgps.wayrecall.billing.trash.RecycleBinStoreManager;
import ru.sosgps.wayrecall.billing.user.UsersService;
import ru.sosgps.wayrecall.billing.user.events.*;
import ru.sosgps.wayrecall.billing.user.permission.events.AbstractPermissionEvent;
import ru.sosgps.wayrecall.billing.user.permission.events.PermissionCreateEvent;
import ru.sosgps.wayrecall.billing.user.permission.events.PermissionDataSetEvent;
import ru.sosgps.wayrecall.billing.user.permission.events.PermissionDeleteEvent;
import ru.sosgps.wayrecall.core.*;
import ru.sosgps.wayrecall.utils.ScalaConverters;
import ru.sosgps.wayrecall.utils.web.ScalaJson;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import scala.Option;

import javax.annotation.PostConstruct;

import static ru.sosgps.wayrecall.core.ObjectsRepositoryReader.ensureObjectId;

/**
 * Created by IVAN on 07.05.2014.
 */
@Order(1000)
public class AtmosphereEventHandler {

    @Autowired
    MongoDBManager mdbm = null;

    @Autowired
    AccountsStoreService accountsStoreService = null;

    @Autowired
    EquipmentStoreService equipmentStoreService= null;

    @Autowired
    AllObjectsService allObjectService= null;

    @Autowired
    RecycleBinStoreManager recycleBinService = null;

    @Autowired
    UsersService usersService = null;

    @Autowired
    ObjectsRepositoryReader or = null;

    @Autowired
    SupportRequestEDS supportRequestEDS = null;

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AtmosphereEventHandler.class);

    @Autowired
    WebApplicationContext context;

    Broadcaster getBroadcaster(){
        log.debug("context="+context);
        log.debug(System.identityHashCode(this)+" getting context:" + System.identityHashCode(context)+" "+ System.identityHashCode(context.getParent()));
        BroadcasterFactory broadcasterFactory = Objects.requireNonNull(
                (BroadcasterFactory) context.getServletContext().getAttribute(BroadcasterFactory.class.getName()), "BroadcasterFactory"
        );
        Broadcaster broadcaster= broadcasterFactory.lookup("servermes", true);
        log.debug("getting broadcasterfactory:" + System.identityHashCode(broadcasterFactory) + " :" + broadcasterFactory);
        log.debug("getting broadcaster:" + System.identityHashCode(broadcaster) + " :" + broadcaster);
        return broadcaster;
    }
    private  Map<String, Object> broadcastData(String aggregate, String action, Object itemId, Map<String,? extends Object> data) {
        HashMap<String, Object> brdata = new HashMap<>();
        brdata.put("eventType", "aggregateEvent");
        brdata.put("aggregate", aggregate);
        brdata.put("action", action);
        brdata.put("itemId",itemId);
        brdata.put("data", data);
        return brdata;
    }

    @Autowired
    EventBus eventBus = null;

    @PostConstruct
    void init(){
        eventBus.subscribe(new AnnotationEventListenerAdapter(this));
    }

    @EventHandler
    void handleAccountCreatedEvent(AccountCreateEvent event) {
        log.debug("AccountCreatedEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        //DBObject obj=mdbm.getDatabase().underlying().getCollection("accounts").findOne(new BasicDBObject("_id", event.getAccountId()));
        Map obj=ScalaConverters.asJavaMap(accountsStoreService.findById(event.getAccountId()).get());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("account","create",event.getAccountId(),obj)));
    }

    @EventHandler
    void handleAccountDataSetEvent(AccountDataSetEvent event) {
        log.debug("AccountDataSetEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        final ObjectId accountId = event.getAccountId();
        refreshAccount(accountId);
    }

    private void refreshAccount(ObjectId accountId) {
        Option<scala.collection.mutable.Map<String, Object>> accOption = accountsStoreService.findById(accountId);
        if(accOption.isDefined()) {
            Map obj = ScalaConverters.asJavaMap(accOption.get());
            getBroadcaster().broadcast(ScalaJson.generate(broadcastData("account", "update", accountId, obj/*event.getData()*/)));
        }
    }

    @EventHandler
    void handleAccountBalanceChanged(AccountBalanceChanged event) {
        log.debug("AccountBalanceChanged atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        //Map obj=ScalaConverters.asJavaMap(accountsStoreService.findById(event.getAccountId()).get());
        final HashMap<String, Object> data = new HashMap<>();
        data.put("balance", event.balance());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("account", "update", event.id(), data)));
    }

    @EventHandler
    void handleAccountDeleteEvent(AccountDeleteEvent event) {
        log.debug("AccountDeleteEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("account", "delete", event.getAccountId(), new HashMap())));
    }

    @EventHandler
    void handleAccountRemoveEvent(AccountRemoveEvent event) {
        log.debug("AccountRemoveEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj = recycleBinService.loadAccount(event.accountId()).toMap();
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("account", "remove", event.getAccountId(), obj)));
    }

    @EventHandler
    void handleAccountRestoreEvent(AccountRestoreEvent event) {
        log.debug("AccountRemoveEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj=ScalaConverters.asJavaMap(accountsStoreService.findById(event.getAccountId()).get());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("account", "restore", event.getAccountId(), obj)));
    }


    @EventHandler
    void handleObjectCreatedEvent(ObjectCreatedEvent event) {
        log.debug("ObjectCreatedEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj=ScalaConverters.asJavaMap(allObjectService.findByUID(event.uid()).get());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("object","create", event.uid(),obj)));
        final Option<Serializable> account = event.objectData().get("accountId");
        if (account.isDefined()) {
            refreshAccount(ensureObjectId(account.get()));
        }
    }

    @EventHandler
    void handleObjectChangedEvent(ObjectDataChangedEvent event) {
        log.debug("ObjectDataChangedEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj=ScalaConverters.asJavaMap(allObjectService.findByUID(event.uid()).get());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("object","update", event.uid(),  obj/*ScalaConverters.asJavaMap(event.changedAttributes())*/)));

        final Option<Serializable> account = event.changedAttributes().get("account");
        if (account.isDefined()) {
            refreshAccount((ObjectId) account.get());
        }

    }

    @EventHandler
    void handleObjectEnabledChanged(ObjectEnabledChanged event) {
        log.debug("ObjectEnabledChanged: " + event);
        final ObjectId account = (ObjectId) or.getObjectByUid(event.uid()).get("account");
        if(account != null)
            refreshAccount(account);
    }

    @EventHandler
    void handleObjectAccountChangedEvent(ObjectAccountChangedEvent event) {
        handleEntityAccountChangedEvent(event);
    }

    @EventHandler
    void handleEquipmentAccountChangedEvent(EquipmentAccountChangedEvent event) {
        handleEntityAccountChangedEvent(event);
    }
    private void handleEntityAccountChangedEvent(AccountChangedEventTrait event) {
        log.debug("EntityAccountChangedEvent atmosphere broadcast: " + event);

        if (event.prevAccount().isDefined()) {
            refreshAccount(event.prevAccount().get());
        }
        if (event.newAccount().isDefined()) {
            refreshAccount(event.newAccount().get());
        }
    }

    @EventHandler
    void handleUserAccountChangedEvent(PermissionCreateEvent event) {
        accoutPermissionChange(event);
    }

    @EventHandler
    void handleUserAccountChangedEvent(PermissionDataSetEvent event) {
        accoutPermissionChange(event);
    }

    @EventHandler
    void handleUserAccountChangedEvent(PermissionDeleteEvent event) {
        accoutPermissionChange(event);
    }

    private void accoutPermissionChange(AbstractPermissionEvent event) {
        log.debug("PermissionCreateEvent atmosphere broadcast: " + event);
        if(event.getRecordType().equals("account"))
            refreshAccount(ensureObjectId(event.getItemId()));
    }


    @EventHandler
    void handleEquipmentObjectChangedEvent(EquipmentObjectChangedEvent event) {
        log.debug("EquipmentObjectChangedEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Option<String> newObject=event.newObject();
        Option<String> prevObject=event.prevObject();
        if(newObject.isDefined()){
            Map obj=ScalaConverters.asJavaMap(allObjectService.findByUID(newObject.get()).get());
            getBroadcaster().broadcast(ScalaJson.generate(broadcastData("object","equpdate", newObject,  obj)));
        }
        if(prevObject.isDefined()){
            Map obj=ScalaConverters.asJavaMap(allObjectService.findByUID(prevObject.get()).get());
            getBroadcaster().broadcast(ScalaJson.generate(broadcastData("object","equpdate", prevObject,  obj)));
        }
        Map obj=equipmentStoreService.findById(event.oid()).get().toMap();
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("equipment","update", event.oid(),  obj)));
    }

    @EventHandler
    void handleObjectDeletedEvent(ObjectDeletedEvent event) {
        log.debug("ObjectDeleteEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("object","delete",  event.uid(), new HashMap())));
    }


    @EventHandler
    void handleObjectRemovedEvent(ObjectRemovedEvent event) {
        log.debug("ObjectRemovedEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj = recycleBinService.loadObject(event.uid()).toMap();
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("object", "remove", event.uid(),obj))); //be careful
    }

    @EventHandler
    void handleObjectRestoredEvent(ObjectRestoredEvent event) {
        log.debug("ObjectRestoredEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj = ScalaConverters.asJavaMap(allObjectService.findByUID(event.uid()).get());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("object", "restore", event.uid(), obj))); //be careful
    }

    @EventHandler
    void handleEquipmentCreateEvent(EquipmentCreatedEvent event) {
        log.debug("EquipmentCreatedEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj=equipmentStoreService.findById(event.oid()).get().toMap();
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("equipment","create", event.oid(), obj)));
    }

    @EventHandler
    void handleEquipmentDataChangedEvent(EquipmentDataChangedEvent event) {
        log.debug("EquipmentDataChangedEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj=equipmentStoreService.findById(event.oid()).get().toMap();
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("equipment","update", event.oid(), obj)));
        DBObject object = mdbm.getDatabase().getCollection("equipments").findOne(new BasicDBObject("_id", event.oid()), new BasicDBObject("uid", 1));
        String uid;
        if(object!=null && (uid = (String)object.get("uid")) != null) {
            Map obj1 = ScalaConverters.asJavaMap(allObjectService.findByUID(uid).get());
            getBroadcaster().broadcast(ScalaJson.generate(broadcastData("object", "update", uid, obj1/*ScalaConverters.asJavaMap(event.changedAttributes())*/)));
        }
    }

    @EventHandler
    void handleEquipmentDeletedEvent(EquipmentDeletedEvent event) {
        log.debug("EquipmentDeletedEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("equipment","delete", event.oid(), new HashMap<>())));
    }

    @EventHandler
    void handleEquipmentRemovedEvent(EquipmentRemovedEvent event) {
        log.debug("EquipmentRemovedEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj = recycleBinService.loadEquipment(event.oid()).toMap();
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("equipment","remove", event.oid(), obj)));
    }

    @EventHandler
    void handleEquipmentRestoredEvent(EquipmentRestoredEvent event) {
        log.debug("EquipmentRestoredEvent atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map data = equipmentStoreService.findById(event.oid()).get().toMap();
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("equipment","restore", event.oid(), data)));
    }

    @EventHandler
    void handleEqTypeCreatedEvent(EquipmentTypesCreateEvent event) {
        log.debug("EqType created event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("equipmentType","create", event.getEqTypeId(), event.getData())));
    }

    @EventHandler
    void handleEqTypeDataSetEvent(EquipmentTypesDataSetEvent event) {
        log.debug("EqType data set event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("equipmentType","update", event.getEqTypeId(), event.getData())));
    }

    @EventHandler
    void handleEqTypeDeleteEvent(EquipmentTypesDeleteEvent event) {
        log.debug("EqType delete event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("equipmentType","delete", event.getEqTypeId(), new HashMap())));
    }
    @EventHandler
    void handleUserCreatedEvent(UserCreateEvent event) {
        log.debug("User created event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj=usersService.findById(event.getUserId()).get().toMap();
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("user","create", event.getUserId(), obj)));
    }

    @EventHandler
    void handleUserDataSetEvent(UserDataSetEvent event) {
        log.debug("User data set event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        Map obj=usersService.findById(event.getUserId()).get().toMap();
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("user","update", event.getUserId(), obj)));
    }

    @EventHandler
    void handleUserDeleteEvent(UserDeleteEvent event) {
        log.debug("User delete  event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("user","delete",  event.getUserId(), new HashMap())));
    }

    @EventHandler
    void handleTariffPlanCreatedEvent(TariffPlanCreateEvent event) {
        log.debug("Tariff plan create  event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("tariff","create",event.getTariffId(),event.getData())));
    }

    @EventHandler
    public void handleTariffPlanDataSetEvent(TariffPlanDataSetEvent event) {
        log.debug("Tariff plan data set  event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("tariff","update", event.getTariffId(),event.getData())));
    }

    @EventHandler
    public void handleTariffPlanDeleteEvent(TariffPlanDeleteEvent event) {
        log.debug("Tariff plan deletet  event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        getBroadcaster().broadcast(ScalaJson.generate(broadcastData("tariff","delete",event.getTariffId(),new HashMap())));
    }

    @EventHandler
    public void handleTicketReadStatusChangedEvent(TicketSupportReadStatusChangedEvent event) {
        log.debug("Ticket support read status changed event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        HashMap<String, Object> brdata = new HashMap<>();
        brdata.put("eventType","unreadSupportTickets");
        brdata.put("unreadTicketsCount", supportRequestEDS.getUnreadTicketsCount());
        getBroadcaster().broadcast(ScalaJson.generate(brdata));
    }

    @EventHandler
    public void handleTicketReadStatusChangedEvent(SupportTicketDeletedEvent event) {
        log.debug("Support ticket delete event atmosphere broadcast: " + getBroadcaster().getAtmosphereResources());
        HashMap<String, Object> brdata = new HashMap<>();
        brdata.put("eventType","unreadSupportTickets");
        brdata.put("unreadTicketsCount", supportRequestEDS.getUnreadTicketsCount());
        getBroadcaster().broadcast(ScalaJson.generate(brdata));
    }

}
