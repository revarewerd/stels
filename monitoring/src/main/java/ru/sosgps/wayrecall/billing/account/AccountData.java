/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.billing.account;

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod;
import java.util.*;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;
import ru.sosgps.wayrecall.core.UserRolesChecker;
import ru.sosgps.wayrecall.core.finance.TariffPlans;
import ru.sosgps.wayrecall.utils.ExtDirectService;
import ru.sosgps.wayrecall.billing.account.commands.AccountCreateCommand;
import ru.sosgps.wayrecall.billing.account.commands.AccountDataSetCommand;
import ru.sosgps.wayrecall.core.SecureGateway;

/**
 * Component for editing account data
 * @see AccountsStoreService
 * @author ИВАН
 */
@ExtDirectService
public class AccountData {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccountData.class);
    @Autowired
    MongoDBManager mdbm = null;

    @Autowired
    TariffPlans tariffPlans = null;

    @Autowired
    UserRolesChecker roleChecker = null;
    
    @Autowired
    SecureGateway commandGateway = null;

//    @ExtDirectMethod
//    public Map<String, Object> updateData(Map<String, Object> submitMap) {
//        Object contractCount= submitMap.get("contractCount");
//        log.debug("submitMap="+submitMap);
//        log.debug("contractCount="+contractCount);
//        if(contractCount==null)
//            return updateData(submitMap, 0);
//        else
//            return updateData(submitMap,Integer.parseInt((String) contractCount));
//        //return submitMap;
//    }

    @ExtDirectMethod
    public Map<String, Object> updateData(Map<String, Object> submitMap, Integer contractCount) {
        Object smcontractCount= submitMap.get("contractCount");
        if(smcontractCount==null && contractCount==null)
            contractCount=0;
        else if(smcontractCount!=null)
            contractCount=Integer.parseInt((String) smcontractCount);
        if(!roleChecker.checkAdminAuthority() && !roleChecker.hasRequiredStringAuthorities("servicer")) throw new RuntimeException("Недостаточно прав для изменения аккаунта");
        if (submitMap == null || submitMap.isEmpty()) {
            return submitMap;
        }
        submitMap.remove("balance");
        submitMap.remove("cost");
        submitMap.put("contractCount", String.valueOf(contractCount));
        ObjectId accountId = ObjectsRepositoryReader.ensureObjectId(submitMap.remove("_id"));
        Object command;
        Map<String, Object> resultControl = new HashMap<String, Object>();
            if (accountId == null) {
                accountId = new ObjectId();
                log.debug("New acc accountId=" + accountId);
                command=new AccountCreateCommand(accountId, submitMap); 
                resultControl.put("code", "4");
                resultControl.put("msg", "Запись успешно добавлена");
                resultControl.put("accountId", accountId);
            } else {
                log.debug("Data udpate for acc accountId=" + accountId);
                command=new AccountDataSetCommand(accountId, submitMap);  
                resultControl.put("code", "3");
                resultControl.put("msg", "Данные успешно обновлены");
                resultControl.put("accountId", accountId);
            }             
            try {
            commandGateway.sendAndWait(command,roleChecker.getUserAuthorities(),roleChecker.getUserName());
            }
            catch (org.axonframework.commandhandling.CommandExecutionException e){
                throw new RuntimeException("При сохранении данных произошла ошибка: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
            }
            return resultControl;
    }

    @ExtDirectMethod
    public Map<String, Object> loadData(String accountId) {
        if(accountId != null && (roleChecker.checkAdminAuthority() || roleChecker.hasRequiredStringAuthorities("Manager") || roleChecker.hasPermissions(roleChecker.getUserName(),"account",new ObjectId(accountId)))) {
            log.debug("Data loading for account ID={}", accountId);
            DBObject one = mdbm.getDatabase().underlying().getCollection("accounts")
                    .findOne(
                            new BasicDBObject("_id", new ObjectId(accountId)));
            one.put("cost", tariffPlans.calcTotalCost(one));
            return userTypeFilter(one.toMap());
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> userTypeFilter(Map<String, Object> map) {
        String [] superUserParams={"_id", "name", /*"comment",*/ "fullClientName", "accountType","plan", "status","blockcause","cost","balance","paymentWay"};
        Set<String> superUserParamsSet= new HashSet<>(Arrays.asList(superUserParams));
        List<String> roles=new ArrayList<>(Arrays.asList(roleChecker.getUserAuthorities()));
        if(roleChecker.hasRequiredStringAuthorities("admin", "AccountView")|| roleChecker.hasRequiredStringAuthorities("Manager")) {
            return map;
        }
        else if(roleChecker.hasRequiredStringAuthorities("superuser","AccountView")){
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
