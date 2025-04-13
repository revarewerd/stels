/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.billing.object;

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod;
//import ch.ralscha.extdirectspring.bean.ExtDirectResponseBuilder;
import java.io.Serializable;
import java.util.*;
//import org.springframework.stereotype.Controller;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
import ru.sosgps.wayrecall.billing.equipment.ObjectsEquipmentService;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;
import ru.sosgps.wayrecall.core.UserRolesChecker;
import ru.sosgps.wayrecall.core.finance.TariffPlans;
import ru.sosgps.wayrecall.utils.ExtDirectService;
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectDataSetCommand;
import ru.sosgps.wayrecall.billing.objectcqrs.ObjectCreateCommand;
import ru.sosgps.wayrecall.core.SecureGateway;
import static ru.sosgps.wayrecall.utils.ScalaConverters.asJavaList;


/**
 * @author ИВАН
 */
@ExtDirectService
public class ObjectData {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ObjectData.class);

    @Autowired
    ObjectsRepositoryWriter or = null;

    @Autowired
    SecureGateway commandGateway = null;

    @Autowired
    UserRolesChecker roleChecker = null;

    @Autowired
    TariffPlans tariffPlans = null;

    @Autowired
    ObjectsEquipmentService oes = null;

    @ExtDirectMethod
    public Map<String, Object> updateOnlyObjectData(Map<String, Serializable> submitMap /*, int eqCount*/) throws Exception {
        Map<String, Serializable> dataToUpdate=submitMap;
        Object uid = submitMap.get("uid");
        List equipment;
        if(uid==null)
            equipment = new ArrayList<>();
        else
            equipment=asJavaList(oes.loadEquipmentByUid(uid.toString()));
        dataToUpdate.put("equipment",(Serializable) equipment);
        return updateData(dataToUpdate);
    }

    @ExtDirectMethod
    public Map<String, Object> updateData(Map<String, Serializable> submitMap /*, int eqCount*/) throws Exception {
        String uid = submitMap.remove("uid").toString();
        List<Map<String, Serializable>> equipment = (List<Map<String, Serializable>>) submitMap.remove("equipment");
        Object command;
        if (uid == null || uid.isEmpty()) {
            log.debug("Object insert");
            command = new ObjectCreateCommand(null, submitMap, equipment);
//           uid = Objects.requireNonNull(commandGateway.<String>sendAndWait(
//                    new ObjectCreateCommand(null,null,null))," object creation command returned null");
        } else {
            log.debug("Object  update");
            command = new ObjectDataSetCommand(uid, submitMap, equipment);
        }

        try {
            uid = commandGateway.sendAndWait(
                    command, roleChecker.getUserAuthorities(), roleChecker.getUserName());
            log.debug("uid=" + uid);
        } catch (org.axonframework.commandhandling.CommandExecutionException e) {
            throw new RuntimeException("При сохранении данных произошла ошибка: " +
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        return data;
        //return or.updateData(submitMap);
    }


    @ExtDirectMethod
    public Map<String, Object> loadData(String uid) {
        if(uid != null && (roleChecker.checkAdminAuthority()|| roleChecker.hasRequiredStringAuthorities("Manager") || roleChecker.hasPermissions(roleChecker.getUserName(),"object",uid))) {
            Map<String, Object> objectData = or.findByUid(uid);
            ObjectId accId = (ObjectId) objectData.get("account");
            DBObject tariffPlan = tariffPlans.emptyTariff();
            try {
                tariffPlan = tariffPlans.getTariffForAccount(accId).get();
            } catch (Exception e) {
                log.error("No such tariff plan for object uid=" + uid, e);
            }
            objectData.put("cost", tariffPlans.sumCostForObject((ObjectId) objectData.get("_id"), tariffPlan));
            return userTypeFilter(objectData);
        }
        return Collections.emptyMap();
    }

    @ExtDirectMethod
    public List<DBObject> getObjectSleepers(String uid)
    {
        log.debug("uid="+uid);
        return or.getObjectSleepers(uid);
    }

    public Map<String, Object> userTypeFilter(Map<String, Object> map) {
        String [] superUserParams={"_id","name","customName", "comment", "uid", "type","subscriptionfee", "marka",
                "account","accountId","accountName","cost",
                "model","gosnumber", "VIN", "objnote","fuelPumpLock","ignitionLock","placeName" ,"disabled"};
        Set<String> superUserParamsSet= new HashSet<>(Arrays.asList(superUserParams));
        List<String> roles=new ArrayList<>(Arrays.asList(roleChecker.getUserAuthorities()));
        if(roleChecker.hasRequiredStringAuthorities("admin", "ObjectView")) {
            return map;
        }
        else if(roleChecker.hasRequiredStringAuthorities("superuser","ObjectView")|| roleChecker.hasRequiredStringAuthorities("Manager")){
            Map<String, Object> filteredMap=new HashMap<>();
            for(Map.Entry<String,Object> item: map.entrySet()){
                if(superUserParamsSet.contains(item.getKey()))
                    filteredMap.put(item.getKey(),item.getValue());
            }
            return filteredMap;
        }
        else return Collections.emptyMap();
    }

}
