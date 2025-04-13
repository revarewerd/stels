package ru.sosgps.wayrecall.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.*;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.events.EventsStore;
import ru.sosgps.wayrecall.utils.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 05.05.13
 * Time: 16:07
 * To change this template use File | Settings | File Templates.
 */

//@Component
@Order(-100)
public class ObjectsRepositoryReader extends ObjectsRepositoryScala {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ObjectsRepositoryReader.class);

    public ObjectsRepositoryReader() {
        log.info("ObjectsRepository created " + System.identityHashCode(this));
    }

    @PostConstruct
    void init() {
        log.info("ObjectsRepository PostConstruct " + System.identityHashCode(this));
        log.info("ensuring index objectsCollection MongoDBObject(\"uid\" -> 1)");
        objectsCollection().ensureIndex(new BasicDBObject("uid", 1), null, true);
        log.info("ensuring index equipmentsCollection MongoDBObject(\"uid\" -> 1)");
        equipmentsCollection().ensureIndex(new BasicDBObject("uid", 1), null, false);
    }

    @Autowired
    public MongoDBManager mdbm = null;

    @Autowired(required = false)
    public EventsStore es = null;

    private boolean checkDuplicateEquipment (List<Map<String,Object>> equipments,Map<String,Object> currenteq){
        for (Map<String, Object> equip : equipments) {
            String currentIMEI=currenteq.get("eqIMEI").toString();
            String equipIMEI=equip.get("eqIMEI").toString();
            if (currentIMEI.equals(equipIMEI) && !currentIMEI.equals("")) {
                log.debug("duplicate IMEI "+currentIMEI);
                return false;
            }
        }
        return true;
    }

    public List<DBObject> getObjectEquipment(String uid) {
        return getObjectEquipment(objectIdByUid.apply(uid));
    }

    public List<DBObject> getObjectSleepers(String uid) {
        List<DBObject> eqList = getObjectEquipment(uid);
        if(!eqList.isEmpty()){
        List<DBObject> sleepersList=new ArrayList<>();
        for(DBObject item: eqList){
            String eqtype=(String) item.get("eqtype");
            if(eqtype.contains("Спящий блок")){
                sleepersList.add(item);
            }
        }
        return sleepersList;
        }
        else return null;
    }

    public static ObjectId ensureObjectId(Object stringOrOid) {
        if (stringOrOid instanceof ObjectId) {
            return (ObjectId) stringOrOid;
        } else
            return validateObjectId((String) stringOrOid);
    }

    /**
     * checks if this ObjectId is correct, returns null otherwise
     *
     * @param objectId
     * @return
     */
    public static ObjectId validateObjectId(String objectId) {
        if (objectId == null || objectId.startsWith("temp_id")) {
            return null;
        }
        return new ObjectId(objectId);
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

    public LoadingCache<ObjectId, String> uidByObjectId = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.HOURS)
            .build(new CacheLoader<ObjectId, String>() {
                @Override
                public String load(ObjectId oid) throws Exception {
                    return (String) objectsCollection().
                            findOne(oid,
                                    new BasicDBObject("uid", 1)
                            ).get("uid");
                }
            });

    public LoadingCache<String,Boolean> removedObjectsCache = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build(
                    new CacheLoader<String, Boolean>() {
                        @Override
                        public Boolean load(String uid) {
                            log.debug("Loading uid in removedObjectsCache \"" + uid + "\"");
                            return checkRemoved(uid);
                        }
                    });


    private Boolean checkRemoved(String uid) {
      //  log.debug("database is " + mdbm.getDatabase().getName());
        DBObject query = new BasicDBObject() {{
            put("uid", uid);
            put("removed", true);
        }};
        return mdbm.getDatabase().underlying().getCollection("objects").findOne(query) != null;
    }

    public void setRemoved(String uid, boolean flag) {
        removedObjectsCache.put(uid,flag);
    }

    public boolean isRemoved(String uid) {
        try {
            return removedObjectsCache.get(uid);
        }
        catch(ExecutionException ex) {
            throw new RuntimeException("Failed to retrieve value from removedObjectsCache", ex);
        }
    }


    public List<DBObject> getObjectEquipment(ObjectId objectId) {
        List<DBObject> objeqlist = equipmentsCollection().find(
                new BasicDBObject("uid", this.uidByObjectId.apply(objectId))).toArray();
        return objeqlist;
    }

    public DBObject getObjectByIMEI(String imei) {
        final DBCursor obIdiesByEquipment = equipmentsCollection()
                .find(new BasicDBObject("eqIMEI", imei), new BasicDBObject("uid", 1));
        for(DBObject eqobj: obIdiesByEquipment)
        {
            String uid = (String) eqobj.get("uid");
            final DBObject objId = getObjectByUid(uid);
            if(objId != null)
            return objId;
        }
        return null;
    }

    // С префиксом
    public String getUidByPhone(String phone) {
        DBObject dbo = equipmentsCollection().findOne(new BasicDBObject("simNumber", phone), new BasicDBObject("uid", 1));
        if(dbo == null)
            return null;
        else
            return (String)dbo.get("uid");
    }

    public DBObject getObjectByUid(String uid) {
        return objectsCollection().findOne(new BasicDBObject("uid", uid));
    }


    /**
     * Loads objects data with equipment data putted to field "equipment"
     *
     * @param objectId ObjectId of object to load
     * @return map of objects data and equipment data
     */
    public Map<String, Object> loadData(ObjectId objectId) {

        if (objectId == null)
            return Collections.emptyMap();

        Map<String, Object> reply;

        log.debug("Data loading for object ID={}", objectId);
        DBObject obj = objectsCollection()
                .findOne(
                        new BasicDBObject("_id", objectId));
        reply = (Map<String, Object>) obj.toMap();
//        DBCursor equipment = equipmentsCollection()
//                .find(
//                        new BasicDBObject("uid", obj.get("uid")));
//        reply.put("equipment", equipment.toArray());
        return reply;
    }
    

    public Map<String, Object> findByUid(String uid) {

        
        Map<String, Object> reply;

        log.debug("Data loading for object uid={}", uid);       
        DBObject obj = objectsCollection()
                .findOne(
                        new BasicDBObject("uid",uid));
        if(obj == null)
            return null;
//        log.debug("obj={}", obj);
        reply = (Map<String, Object>) obj.toMap();
//        DBCursor equipment = equipmentsCollection()
//                .find(
//                        new BasicDBObject("uid", obj.get("uid")));
//        reply.put("equipment", equipment.toArray());
        return reply;
    }

    @Deprecated
    public static Map <String, Object> detectChanged(Map<String, Object> oldData,
                                                     Map<String, Object> newData) {
        return CollectionUtils.detectChanged(oldData, newData);
    }

    public String genUid() {
        return "o" + (java.util.UUID.randomUUID().getLeastSignificantBits() & 0x7FFFFFFFFFFFFFFFL);
    }

    protected DBCollection objectsCollection() {
        return mdbm.getDatabase().underlying().getCollection("objects");
    }

    protected DBCollection equipmentsCollection() {
        return mdbm.getDatabase().underlying().getCollection("equipments");
    }

    public String getAccountName(final ObjectId accountId) {
        DBObject accountWithName = mdbm.getDatabase().apply("accounts").underlying().findOne(
                new BasicDBObject("_id", accountId),
                new BasicDBObject("name", 1)
        );
        if(accountWithName == null)
            return null;
        return accountWithName.get("name").toString();
    }

    protected String getObjectName(final ObjectId id) {
        DBObject obj = mdbm.getDatabase().apply("objects").underlying().findOne(
                new BasicDBObject("_id", id),
                new BasicDBObject("name", 1)
        );
        log.info("getObjectName for id="+id);
        //log.info("objname="+obj.get("name").toString()); 
        return obj != null ? obj.get("name").toString() : null;
    }
}
