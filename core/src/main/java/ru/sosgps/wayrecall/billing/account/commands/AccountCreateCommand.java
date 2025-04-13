package ru.sosgps.wayrecall.billing.account.commands;

import java.io.Serializable;
import java.util.Map;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;

public class AccountCreateCommand implements Serializable, CommandEntityInfo{
    @TargetAggregateIdentifier
    private final ObjectId accountId; 
    private final Map<String, Object> data;
    
     public ObjectId getAccountId() {
        return accountId;
    }
      public Map<String, Object> getData() {
        return data;
    }
    
    public AccountCreateCommand(ObjectId accountId,Map<String, Object> data) {
        this.accountId = accountId;     
        this.data=data;
    }
    public String getEntity(){
        return "Account";
    }
    public Object getEntityId(){
        return this.getAccountId();
    }
}
