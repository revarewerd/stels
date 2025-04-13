package ru.sosgps.wayrecall.billing.equipment;

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;

import java.io.Serializable;
import java.util.*;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import ru.sosgps.wayrecall.billing.account.AccountRepository;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;
import ru.sosgps.wayrecall.core.SecureGateway;
import ru.sosgps.wayrecall.core.UserRolesChecker;
import ru.sosgps.wayrecall.initialization.MultiDbManager;
import ru.sosgps.wayrecall.utils.ExtDirectService;
import ru.sosgps.wayrecall.billing.equipment.EquipmentCreateCommand;
import ru.sosgps.wayrecall.billing.equipment.EquipmentDataSetCommand;
import scala.Option;
import ru.sosgps.wayrecall.utils.DBQueryUtils;
import javax.annotation.PostConstruct;

import static java.util.Objects.requireNonNull;

@ExtDirectService
public class EquipmentData {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EquipmentData.class);
    @Autowired
    MongoDBManager mdbm = null;

    MultiDbManager multiDbManager = null;

    @Autowired
    AccountRepository accRepo = null;

    @Autowired
    ObjectsRepositoryReader or = null;
    
    @Autowired
    SecureGateway commandGateway = null;

    @Autowired
    UserRolesChecker roleChecker = null;
    
    @PostConstruct
    private void init() {
        requireNonNull(multiDbManager, "multiDbManager");
    }
    private DBObject getDefaultAcc() {
        BasicDBList or = new BasicDBList();
        or.add(new BasicDBObject("name", "Без Аккаунта"));
        or.add(new BasicDBObject("default", true));
        return requireNonNull(mdbm.getDatabase().underlying().getCollection("accounts").findOne(
                new BasicDBObject("$or", or),
                new BasicDBObject("_id", 1).append("name", 1)), "default account does not exists");
    }

//    @ExtDirectMethod
//    public long getEqCount(){
//       long count=mdbm.getDatabase().underlying().getCollection("equipments").count(new BasicDBObject("accountId",getDefaultAcc().get("_id")).append("uid",null));
//       log.debug("count ="+count);
//       return count; //mdbm.getDatabase().underlying().getCollection("equipments").count(new BasicDBObject("accountId",getDefaultAcc().get("_id")));
//    }
    
    @ExtDirectMethod
    public Map<String, Object> updateData(Map<String, Serializable> submitMap) {

        //TODO: глобальная блокировка дабы гарантировано избежать записи повторяющихся IMEI
        // Это со всех сторон плохое решение, так как synchronized не гарантирует глобальну блокирвку
        // и сама по себе глобальная блокировка по всему оборудованию избыточна и т.п.
        // Вообще тут лучше всего использовать распределенную блокировку по конкретному imei или транзакции
        // И обрабатываться она должна не здесь, а в CommandHandler-е оборудования
        synchronized (this.getClass()) {

//        if (submitMap == null || submitMap.isEmpty()) {
//            return new HashMap();
//        }
            String eqId = (String) submitMap.remove("_id");

            log.debug("Data updating for eq ID={}", eqId);
            Map<String, Object> resultControl = new HashMap<>();
            Object command;
            ObjectId oid = ObjectsRepositoryReader.ensureObjectId(eqId);
            DBObject existent = mdbm.getDatabase().underlying().getCollection("equipments").findOne(new BasicDBObject("_id", oid));
            log.debug("existent={}", existent);
            String accoutId = (String) submitMap.get("accountId");
            if (accoutId == null) {
                submitMap.put("accountId", (ObjectId) getDefaultAcc().get("_id"));
            }
            if (eqId != null && oid != null) {
                log.debug("eq update");
                submitMap.put("oid", oid);
                command = new EquipmentDataSetCommand(oid, submitMap);
                resultControl.put("code", "3");
                resultControl.put("msg", "Данные успешно обновлены");
                resultControl.put("eqId", eqId);
            } else {
                log.debug("eq insert");

                command = new EquipmentCreateCommand(oid, submitMap);
                resultControl.put("code", "4");
                resultControl.put("msg", "Запись успешно добавлена");
                //resultControl.put("eqId", eqId);
            }
            try {
                commandGateway.sendAndWait(command, roleChecker.getUserAuthorities(), roleChecker.getUserName());
            } catch (org.axonframework.commandhandling.CommandExecutionException e) {
                throw new RuntimeException("При сохранении данных произошла ошибка: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
            }
            //getEqCount();
            return resultControl;
        }
    }


    @ExtDirectMethod
    public Map<String, Object> loadData(String eqId) {
        List<String> roles = new ArrayList<>(Arrays.asList(roleChecker.getUserAuthorities()));
        if (eqId != null && (roles.contains("admin") || roles.contains("servicer") || roleChecker.hasPermissions(roleChecker.getUserName(),"equipment",new ObjectId(eqId)))) {
            log.debug("Data loading for eq ID={}", eqId);
            DBObject one = mdbm.getDatabase().underlying().getCollection("equipments")
                    .findOne(
                            new BasicDBObject("_id", new ObjectId(eqId)));
            String uid = (String) one.get("uid");
            if (uid != null) {
                String objectName = or.getObjectName(uid);
                if(or.isRemoved(uid))
                    one.put("objectName", "Объект " + objectName + " в корзине");
                else
                    one.put("objectName", objectName);
            } else one.put("objectName", "не установлен");
            ObjectId accountId = (ObjectId) one.get("accountId");
            if (accountId != null) {
                String accountName = accRepo.getAccountName(accountId);
                if(accRepo.isRemoved(accountId))
                    one.put("accountName", accountName + " (в корзине)");
                else
                    one.put(accountName,accountName);
            } else one.put("accountName", "на складе");
            return userTypeFilter(one.toMap());
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> userTypeFilter(Map<String, Object> map) {
        String[] superUserParams = {"_id", "objectid", "objectName", "accountId", "accountName",
                //Вкладка Основные сведения
                "eqOwner", "eqRightToUse", "eqSellDate", "eqWork", "eqWorkDate", "eqNote",
                //Вкладка Характеристики устройства
                "eqtype", "eqMark", "eqModel", "eqSerNum",
                "eqIMEI", "eqFirmware",//"eqLogin","eqPass",
                //Вкладка SIM-карта
                //"simOwner","simProvider","simNumber","simICCID","simNote",
                //Место установки
                "instPlace"};
        Set<String> superUserParamsSet = new HashSet<>(Arrays.asList(superUserParams));
        List<String> roles = new ArrayList<>(Arrays.asList(roleChecker.getUserAuthorities()));
        if (roles.contains("admin") || roles.contains("servicer")) {
            return map;
        } else if (roles.contains("superuser")) {
            Map<String, Object> filteredMap = new HashMap<>();
            for (Map.Entry<String, Object> item : map.entrySet()) {
                if (superUserParamsSet.contains(item.getKey()))
                    filteredMap.put(item.getKey(), item.getValue());
            }
            return filteredMap;
        } else return Collections.emptyMap();
    }

    public MultiDbManager getMultiDbManager() {
        return requireNonNull(multiDbManager, "multiDbManager");
    }

    public void setMultiDbManager(MultiDbManager multiDbManager) {
        this.multiDbManager = multiDbManager;
    }
}
