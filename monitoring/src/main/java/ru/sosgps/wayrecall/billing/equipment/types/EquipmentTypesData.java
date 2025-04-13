
package ru.sosgps.wayrecall.billing.equipment.types;

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import ru.sosgps.wayrecall.billing.equipment.types.commands.*;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;
import ru.sosgps.wayrecall.core.SecureGateway;
import ru.sosgps.wayrecall.core.UserRolesChecker;
import ru.sosgps.wayrecall.utils.ExtDirectService;

/**
 *
 * @author ИВАН
 */
@ExtDirectService
public class EquipmentTypesData {
    
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EquipmentTypesData.class);
    @Autowired
    MongoDBManager mdbm = null;
    
    @Autowired
    SecureGateway commandGateway = null;

    @Autowired
    UserRolesChecker roleChecker = null;
    
    public boolean ensureItem(String id){
         DBCursor items=mdbm.getDatabase().underlying().getCollection("equipmentTypes")
                 .find(new BasicDBObject("_id", new ObjectId(id)));
         if(items.hasNext()){
             return true;
         }
    return false;
    }
    @ExtDirectMethod
    public Map<String, Object> updateData(Map<String, Object> submitMap) {
        if (submitMap == null || submitMap.isEmpty()) {
                return submitMap;
        }
        ObjectId eqTypeId = ObjectsRepositoryReader.ensureObjectId( submitMap.remove("_id"));
        Object command;
        Map<String, Object> resultControl = new HashMap<String, Object>();
         if (eqTypeId == null) {
                eqTypeId = new ObjectId();
                log.debug("New eqTypeId=" + eqTypeId);
                command=new EquipmentTypesCreateCommand(eqTypeId, submitMap);
                resultControl.put("code", "4");
                resultControl.put("msg", "Запись успешно добавлена");
                resultControl.put("eqTypeId ", eqTypeId);
            } else {
                log.debug("Data udpate for eqTypeId=" + eqTypeId);
                command=new EquipmentTypesDataSetCommand(eqTypeId, submitMap);    
                resultControl.put("code", "3");
                resultControl.put("msg", "Данные успешно обновлены");
                resultControl.put("eqTypeId", eqTypeId);
            }             
            try {
            commandGateway.sendAndWait(command,roleChecker.getUserAuthorities(),roleChecker.getUserName());
            }
            catch (org.axonframework.commandhandling.CommandExecutionException e){
                throw new RuntimeException("При сохранении данных произошла ошибка: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
            }
            return resultControl;
//        log.debug("Data updating for eqType ID={}",eqTypeId);
//        
//        try {
//            BasicDBObject currentObject = new BasicDBObject(submitMap);
//            if (eqTypeId != null /*&& ensureItem(eqTypeId)*/) {
//                log.debug("eqType update");
//                mdbm.getDatabase().underlying().getCollection("equipmentTypes").update(
//                        new BasicDBObject("_id", new ObjectId(eqTypeId)),
//                        new BasicDBObject("$set", currentObject),
//                        true, false, WriteConcern.SAFE);
//                resultControl.put("code", "3");
//                resultControl.put("msg", "Данные успешно обновлены");
//                resultControl.put("eqTypeId", eqTypeId);
//            } else {
//                log.debug("eqType insert");
//                mdbm.getDatabase().underlying().getCollection("equipmentTypes").insert(
//                        /*new BasicDBObject("_id", new ObjectId(accountId)),*/
//                        currentObject,
//                        /*,false,false, */
//                        WriteConcern.SAFE);
//                String newObjectId = currentObject.get("_id").toString();
//                log.debug("newObjectId={}", newObjectId);
//                resultControl.put("code", "4");
//                resultControl.put("msg", "Запись успешно добавлена");
//                resultControl.put("eqTypeId ", newObjectId);
//                //eqTypeId = newObjectId;
//            }            
//
//        } catch (com.mongodb.MongoException.DuplicateKey e) {
//            throw new IllegalArgumentException("Такой тип оборудования уже существует", e);
//        }
//        return resultControl;
//        
    //return null;
    }
    
    @ExtDirectMethod
    public Map<String, Object> loadData(String eqTypesId) {
        if (eqTypesId == null) {
            return Collections.emptyMap();
        }

        log.debug("Data loading for eqType ID={}", eqTypesId);
        DBObject one = mdbm.getDatabase().underlying().getCollection("equipmentTypes")
                .findOne(
                new BasicDBObject("_id", new ObjectId(eqTypesId)));
        return (Map<String, Object>) one.toMap();
    } 
       
    @ExtDirectMethod
    public List<Map<String,Object>> loadMarkByType(String eqType) {
        if (eqType== null) {
            return Collections.emptyList();
        }

        log.debug("Data loading for eqType ", eqType);
        List many = mdbm.getDatabase().underlying().getCollection("equipmentTypes")
                .distinct("mark",new BasicDBObject("type",eqType));
                //new BasicDBObject("type",eqType),new BasicDBObject("mark",1));
        List<Map<String,Object>> eqMarks=new ArrayList<>();
//        Map emptyMark=new HashMap<>();
//        emptyMark.put("eqMark", "");
//        eqMarks.add(emptyMark);
        for(Object s:many){
          Map item=new HashMap<>();
          item.put("eqMark", s);
          eqMarks.add(item);  
        }
        
        return  eqMarks;
    } 
    @ExtDirectMethod
    public List<Map<String,Object>> loadModelByMark(String eqType, String eqMark ) {
        if (eqMark== null && eqType== null) {
            return Collections.emptyList();
        }

        log.debug("Data loading for eqType ", eqMark);
        List many = mdbm.getDatabase().underlying().getCollection("equipmentTypes")
                .distinct("model",new BasicDBObject("mark",eqMark).append("type", eqType));
                //new BasicDBObject("type",eqType),new BasicDBObject("mark",1));
        List<Map<String,Object>> eqModels=new ArrayList<>();
//        Map emptyModel=new HashMap<>();
//        emptyModel.put("eqModel", "");
//        eqModels.add(emptyModel);
        for(Object s:many){
          Map item=new HashMap<>();
          item.put("eqModel", s);
          eqModels.add(item);  
        }
        
        return  eqModels;
    } 
}
