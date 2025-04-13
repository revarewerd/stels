package ru.sosgps.wayrecall.billing.object;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.*;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import ru.sosgps.wayrecall.billing.security.PermissionsEditor;
import ru.sosgps.wayrecall.core.*;
import ru.sosgps.wayrecall.initialization.MultiDbManager;
import ru.sosgps.wayrecall.utils.errors.ImpossibleActionException;
import scala.Option;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static ru.sosgps.wayrecall.utils.ScalaConverters.asImmutableJavaMap;
import static ru.sosgps.wayrecall.utils.ScalaConverters.nullable;

/**
 * Created by IVAN on 02.10.2014.
 */

@Order(-100)
public class ObjectsRepositoryWriter extends ObjectsRepositoryReader {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ObjectsRepositoryWriter.class);

    @Autowired
    SecureGateway commandGateway = null;

    @Autowired
    PermissionsEditor permEd = null;

    @Value("${instance.name}")
    String instanceName;

    @EventHandler
    void handleObjectCreatedEvent(ObjectCreatedEvent event) {
        log.debug("processing " + event);
        Map<String, Object> data = toMutableHashMap(event.objectData());
        data.put("uid", event.uid());
        writeObjectDataToDb(null,data);
//        es.publish(new DataEvent(mapToSerializable(data), "object", event.uid(),
//                "created"));
    }

    @EventHandler
    void handleObjectChangedEvent(ObjectDataChangedEvent event) {
        log.debug("processing "  + event);
        writeObjectDataToDb(this.objectIdByUid.apply(event.uid()), toMutableHashMap(event.changedAttributes()));
    }

    @EventHandler
    void handleEquipmentCreateEvent(EquipmentCreatedEvent event) {
        String eqId = event.oid().toString();
        String IMEI = (String) nullable(event.data().get("eqIMEI"));
        String simNumber = (String) nullable(event.data().get("simNumber"));
        checkUniqueness(eqId, IMEI, simNumber);
        updateEquipmentRecordInDb(event.oid(), toMutableHashMap(event.data()));
    }

    @EventHandler
    void handleEquipmentDataChangedEvent(EquipmentDataChangedEvent event) {
        log.debug("processing "  + event);
        updateEquipmentRecordInDb(event.oid(), toMutableHashMap(event.changedAttributes()));
    }



    @EventHandler
    void handleEquipmentObjectChangedEvent(EquipmentObjectChangedEvent event) {
        log.debug("processing "  + event);

        if(event.prevObject().isDefined()){
            equipmentsCollection().update(
                    new BasicDBObject("_id", event.oid()),
                    new BasicDBObject("$unset", new BasicDBObject("uid",1)),
                    false, false, WriteConcern.SAFE);
        }

        if(event.newObject().isDefined())
        {
            equipmentsCollection().update(
                    new BasicDBObject("_id", event.oid()),
                    new BasicDBObject("$set",new BasicDBObject("uid",event.newObject().get())),
                    false, false, WriteConcern.SAFE);
            //updateEquipmentRecordInDb(event.oid(), new BasicDBObject("uid",event.newObject().get()));
        }
    }



    /**
     * Updates or inserts objects data into objects collections ONLY.
     * Does not write any equipment data
     *
     * @param objectId  objectId of target objetct. null value means creating new object
     * @param submitMap data to be written to collection
     * @return passed objectId or new objectId if null was passed
     */
    public ObjectId writeObjectDataToDb(final ObjectId objectId, Map<String, Object> submitMap) {
        log.debug("writeObjectDataToDb");
        submitMap.remove("_id");
        final BasicDBObject currentObject = new BasicDBObject(submitMap);
        //final String uid = (String) currentObject.get("uid");
        String accountIdStr = (String) currentObject.remove("accountId");
        final ObjectId accountId = accountIdStr == null || accountIdStr.equals("") ? null : new ObjectId(accountIdStr);
        if (accountId != null) {
            currentObject.put("account", accountId);
        }
        log.debug("objectId= " + objectId);
        if (objectId != null && ensureUID(objectId)) {
            objectsCollection().update(
                    new BasicDBObject("_id", objectId),
                    new BasicDBObject("$set", currentObject),
                    true, false, WriteConcern.SAFE);
            return objectId;
        } else {
            //currentObject.put("uid", genUid());
            try {
                objectsCollection().insert(
                        /*new BasicDBObject("_id", new ObjectId(accountId)),*/
                        currentObject,
                        /*,false,false, */
                        WriteConcern.SAFE);
            } catch (MongoException.DuplicateKey e) {
                throw new ImpossibleActionException("Такой объект уже существует", e);
            }
            String newObjectId = currentObject.get("_id").toString();
            log.debug("newObjectId={}", newObjectId);
            return new ObjectId(newObjectId);
        }
    }

    private void checkUniqueness(String eqId, String IMEI, String simNumber) {
        if(IMEI.isEmpty())
            return;
        final Option<MongoDBManager> imeiManager = getMultiDbManager().dbByIMEIOpt(IMEI);
        if (imeiManager.isDefined() && !imeiManager.get().equals(mdbm)) {
            final InstanceConfig instance = nullable(imeiManager.get().instance());
            final String serverName = instance != null ? instance.name() : null;
            log.info("Оборудование с имеем '" + IMEI + "' уже существует на сервере '" + serverName + "'");
            if ("default".equals(instanceName))
                throw new ImpossibleActionException("Оборудование с таким IMEI уже существует на сервере '" + serverName + "'");
            throw new ImpossibleActionException("Оборудование с таким IMEI уже существует на сервере");
        }
        DBObject existentIMEI = mdbm.getDatabase().underlying().getCollection("equipments").findOne(new BasicDBObject("eqIMEI", IMEI));
        if (existentIMEI != null && !IMEI.equals("")) {
            if (!existentIMEI.get("_id").equals(eqId)) {
                log.debug("Оборудование с таким IMEI уже существует");
                throw new ImpossibleActionException("Оборудование с таким IMEI уже существует");
            }
        }
        DBObject existentSimNumber = mdbm.getDatabase().underlying().getCollection("equipments").findOne(new BasicDBObject("simNumber", simNumber));
        if (existentSimNumber != null && !simNumber.equals("") && !simNumber.equals(" ")) {
            if (!existentSimNumber.get("_id").equals(eqId)) {
                log.debug("Оборудование с таким номером телефона уже существует");
                throw new ImpossibleActionException("Оборудование с таким номером телефона уже существует");
            }
        }
    }

    private void updateEquipmentRecordInDb(ObjectId eqid, Map<String, Object> dataToUpdate) {
        dataToUpdate.remove("_id");
        final Object accountId = dataToUpdate.remove("accountId");
        if(accountId != null) {
            dataToUpdate.put("accountId", ensureObjectId(accountId));
        }
        equipmentsCollection().update(
                new BasicDBObject("_id", eqid),
                new BasicDBObject("$set", dataToUpdate),
                true, false, WriteConcern.SAFE);
    }

    public boolean ensureUID(ObjectId objectId){
        DBObject item=mdbm.getDatabase().underlying().getCollection("objects")
                .findOne(new BasicDBObject("_id", objectId));
        if(item!= null && item.get("uid")!=null){
            return true;
        }
        return false;
    }

    public static HashMap<String, Object> toMutableHashMap(scala.collection.immutable.Map<String, Serializable>
                                                                   serializableMap) {
        HashMap<String, Object> map = new HashMap<>();
        map.putAll(asImmutableJavaMap(serializableMap));
        return map;
    }

    public DBCollection usersPermissionsCollection() {
        return mdbm.getDatabase().underlying().getCollection("usersPermissions");
    }

    public DBCollection objectsCollection() {
        return mdbm.getDatabase().underlying().getCollection("objects");
    }

    public DBCollection equipmentsCollection() {
        return mdbm.getDatabase().underlying().getCollection("equipments");
    }

    public LoadingCache<String, ObjectId> objectIdByUid = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.HOURS)
            .build(new CacheLoader<String, ObjectId>() {
                @Override
                public ObjectId load(String uid) throws Exception {
                    if (uid == null)
                        return null;
                    DBObject dbObject = objectsCollection().findOne(
                            new BasicDBObject("uid", uid),
                            new BasicDBObject("_id", 1)
                    );
                    if (dbObject == null)
                        throw new NoSuchElementException("cant find object with uid = \"" + uid + "\"");
                    return (ObjectId) dbObject.get("_id");
                }
            });


    MultiDbManager multiDbManager = null;

    public MultiDbManager getMultiDbManager() {
        return requireNonNull(multiDbManager, "multiDbManager");
    }

    public void setMultiDbManager(MultiDbManager multiDbManager) {
        this.multiDbManager = multiDbManager;
    }

}

