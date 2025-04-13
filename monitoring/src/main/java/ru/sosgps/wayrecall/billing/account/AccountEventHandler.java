
package ru.sosgps.wayrecall.billing.account;

import com.mongodb.*;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import ru.sosgps.wayrecall.billing.RemovalRestorationManager;
import ru.sosgps.wayrecall.billing.account.events.*;
import ru.sosgps.wayrecall.billing.equipment.EquipmentDataSetCommand;
import ru.sosgps.wayrecall.billing.equipment.EquipmentRemoveCommand;
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectDataSetCommand;
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectRemoveCommand;
import ru.sosgps.wayrecall.billing.security.PermissionsEditor;
import ru.sosgps.wayrecall.billing.tariff.Tariff;
import ru.sosgps.wayrecall.billing.tariff.TariffDAO;
import ru.sosgps.wayrecall.billing.tariff.TariffEDS;
import ru.sosgps.wayrecall.billing.user.commands.UserDataSetCommand;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.SecureGateway;
import ru.sosgps.wayrecall.core.UserRolesChecker;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Order(-100)
public class AccountEventHandler {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccountEventHandler.class);

    @Autowired
    MongoDBManager mdbm = null;

    @Autowired
    SecureGateway commandGateway = null;

    @Autowired
    PermissionsEditor permEd = null;

    @Autowired
    UserRolesChecker roleChecker = null;

    // Не нужен тулзам и тянет за собой слишком много зависимостей
    @Autowired(required = false)
    RemovalRestorationManager rrm = null;

    @Autowired
    TariffEDS tariffEDS = null;

    DBObject defaultAcc = null;
    ObjectId defaultTariffId = null;

    DBCollection dbaccounts = null;
    DBCollection dbobjects = null;
    DBCollection dbequipments = null;
    DBCollection dbusers = null;

    @PostConstruct
    private void init() {
        dbequipments = mdbm.getDatabase().underlying().getCollection("equipments");
        dbaccounts = mdbm.getDatabase().underlying().getCollection("accounts");
        dbobjects = mdbm.getDatabase().underlying().getCollection("objects");
        dbusers = mdbm.getDatabase().underlying().getCollection("users");

        defaultAcc = dbaccounts.findOne(new BasicDBObject("default", true));
        if (defaultAcc == null) {
            //TODO: Создать дефолтный аккаунт через аггрегат
        }
    }

    @EventHandler
    public void handleAccountCreatedEvent(AccountCreateEvent event) {
        HashMap<String, Object> data = event.getData();
        if (data == null) throw new RuntimeException("Ошибка при создании аккаунта data=null");
        log.debug("Account create=" + event.getAccountId());
        log.debug("account data=" + data);
        BasicDBObject newAcc = new BasicDBObject("_id", event.getAccountId());
        String plan = (String) data.get("plan");
        if (plan == null) {
            log.debug("set account to default tariff");
            plan = tariffEDS.getDefaultTariffId().toString();
            data.put("plan", plan);
        }
        newAcc.putAll(data);
        dbaccounts.insert(
                newAcc,
                WriteConcern.SAFE);
    }

    @EventHandler
    public void handleAccountDataEvent(AccountDataSetEvent event) {
        log.debug("Data updating for accountId=", event.getAccountId());
        dbaccounts.update(
                new BasicDBObject("_id", event.getAccountId()),
                new BasicDBObject("$set", event.getData()),
                true, false, WriteConcern.SAFE);
    }

    void moveObjectsToDefaultAccount(Iterable<DBObject> objs, String userName) {
        for (DBObject obj : objs) {
            Map<String, Serializable> objdata = obj.toMap();
            String uid = objdata.remove("uid").toString();
            objdata.put("accountName", (String) defaultAcc.get("name"));
            objdata.put("accountId", ((ObjectId) defaultAcc.get("_id")).toString());
            log.debug("objdata=" + objdata);
            ObjectId[] equipmentIds = getEquipmentIds(uid);
            try {
                commandGateway.sendAndWait(
                        new ObjectDataSetCommand(uid, objdata, equipmentIds), new String[]{"admin", "ObjectDataSet"}, userName);
                //dbobjects.update(ob,$set("accountName"->defaultAcc.get("name"),"account"->defaultAcc.get("_id")))
            } catch (org.axonframework.commandhandling.CommandExecutionException e) {
                throw new RuntimeException("При сохранении данных произошла ошибка: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
            }
        }
    }

    void moveEquipmentsToDefaultAccount(Iterable<DBObject> eqs, String userName) {
        for (DBObject eq : eqs) {
            Map<String, Serializable> eqdata = eq.toMap();
            ObjectId oid = (ObjectId) eqdata.remove("_id");
            log.debug("eqdata" + eqdata);
            eqdata.put("accountId", (ObjectId) defaultAcc.get("_id"));
            log.debug("eqdata" + eqdata);
            try {
                commandGateway.sendAndWait(
                        new EquipmentDataSetCommand(oid, eqdata), new String[]{"admin", "EquipmentDataSet"}, userName);
            } catch (org.axonframework.commandhandling.CommandExecutionException e) {
                throw new RuntimeException("При сохранении данных произошла ошибка: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
            }
        }
    }

    public void setUsersMainAcc(Iterable<DBObject> usrs, ObjectId accId) {
        for (DBObject usr : usrs) {
            Map<String, Object> usrdata = usr.toMap();
            ObjectId oid = (ObjectId) usrdata.remove("_id");
            usrdata.put("mainAccId", accId);
            try {
                commandGateway.sendAndWait(
                        new UserDataSetCommand(oid, usrdata), new String[]{"admin", "UserDataSet"}, "System");
            } catch (org.axonframework.commandhandling.CommandExecutionException e) {
                throw new RuntimeException("При сохранении данных произошла ошибка: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
            }
        }
    }

    boolean isNullOrFalse(Object object) {
        return object == null || Boolean.TRUE.equals(object);
    }

    @EventHandler
    public void handleAccountRestoreEvent(AccountRestoreEvent event) {
        Objects.requireNonNull(rrm, "RemovalRestorationManager cannot be null");
        log.debug("AccountRestoreEvent = " + event);
        rrm.restoreAccount(event.accountId());
        List<DBObject> users = ((BasicDBList) (dbaccounts.findOne(new BasicDBObject("_id", event.accountId())).get("prevAccountUsers")))
                .stream()
                .map(id -> dbusers.findOne(new BasicDBObject("_id", id)))
                .filter(dbo -> dbo != null)
                .filter(dbo -> dbo.get("mainAccId") == null)
                .collect(Collectors.toList());
        setUsersMainAcc(users, event.accountId());
        dbaccounts.update(new BasicDBObject("_id", event.accountId()), new BasicDBObject("$unset", new BasicDBObject("prevAccountUsers", 1)));

    }

    @EventHandler
    public void handleAccountRemoveEvent(AccountRemoveEvent event) {
        Objects.requireNonNull(rrm, "RemovalRestorationManager cannot be null");
        final String RemoveObjectsAlongside = "remove_objects_alongside";
        final String MoveToDefaultAcc = "move_to_default_acc";
        boolean removeObjectsAndEquipments = event.getOption(RemoveObjectsAlongside);
        boolean moveToDefaultAcc = event.getOption(MoveToDefaultAcc);
        if (removeObjectsAndEquipments || moveToDefaultAcc) {
            List<DBObject> accountObjects = dbobjects.find(new BasicDBObject("account", event.accountId())).toArray(); // filter?
            List<DBObject> accountEqs = dbequipments.find(new BasicDBObject("accountId", event.accountId())).toArray();
            if (moveToDefaultAcc) {
                moveObjectsToDefaultAccount(accountObjects, roleChecker.getUserName());
                moveEquipmentsToDefaultAccount(accountEqs, roleChecker.getUserName());
            }
            if (removeObjectsAndEquipments) {
                List<String> uids = accountObjects.stream().filter(dbo -> isNullOrFalse(dbo.get("removed")))
                        .map(dbo -> (String) dbo.get("uid"))
                        .collect(Collectors.toList());
                List<ObjectId> eqIds = accountEqs.stream().filter(dbo -> isNullOrFalse(dbo.get("removed")))
                        .map(dbo -> (ObjectId) dbo.get("_id"))
                        .collect(Collectors.toList());
                for (String uid : uids) {
                    try {
                        commandGateway.sendAndWait(new ObjectRemoveCommand(uid), new String[]{"admin", "ObjectRemove"}, roleChecker.getUserName());
                    } catch (org.axonframework.commandhandling.CommandExecutionException e) {
                        throw new RuntimeException("При удалении объектов произошла ошибка: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
                    }
                }

                for (ObjectId id : eqIds) {
                    try {
                        commandGateway.sendAndWait(new EquipmentRemoveCommand(id), new String[]{"admin", "EquipmentRemove"}, roleChecker.getUserName());
                    } catch (org.axonframework.commandhandling.CommandExecutionException e) {
                        throw new RuntimeException("При удалении оборудования произошла ошибка: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
                    }
                }
            }
        }
        List<DBObject> users = dbusers.find(new BasicDBObject("mainAccId", event.getAccountId())).toArray();
        List<ObjectId> userIDs = users.stream().map(dbo -> (ObjectId) dbo.get("_id")).collect(Collectors.toList());

        dbaccounts.update(new BasicDBObject("_id", event.accountId()), new BasicDBObject("$set", new BasicDBObject("prevAccountUsers", userIDs)));

        setUsersMainAcc(users, null);
        rrm.removeAccount(event.accountId());
    }

    @EventHandler
    public void handleAccountDeleteEvent(AccountDeleteEvent event) {
        log.debug("Account deleting accountId=" + event.getAccountId());
        log.debug("removing account permissions");
        permEd.removePermissionsForItem("account", event.getAccountId());
        log.debug("set objects to default acc");

        DBCursor objs = dbobjects.find(new BasicDBObject("account", event.getAccountId()));
        log.debug("objs.count=" + objs.count());
        moveObjectsToDefaultAccount(objs, "System");
        log.debug("set equipments to default acc");
        DBCursor eqs = dbequipments.find(new BasicDBObject("accountId", event.getAccountId()));
        log.debug("eqs=" + eqs);
        log.debug("eqs.count=" + eqs.count());
        moveEquipmentsToDefaultAccount(eqs, "System");
        log.debug("remove users mainAcc");
        DBCursor usrs = dbusers.find(new BasicDBObject("mainAccId", event.getAccountId()));
        log.debug("usrs=" + usrs);
        log.debug("usrs.count=" + usrs.count());
        setUsersMainAcc(usrs, null);
        //}
        dbaccounts.remove(new BasicDBObject("_id", event.getAccountId()));
    }

    private ObjectId[] getEquipmentIds(String uid) {
        ArrayList<ObjectId> objectIds = new ArrayList<>();
        for (DBObject o : dbequipments.find(new BasicDBObject("uid", uid))) {
            objectIds.add((ObjectId) o.get("_id"));
        }
        return objectIds.toArray(new ObjectId[objectIds.size()]);
    }

}
