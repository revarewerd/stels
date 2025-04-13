package ru.sosgps.wayrecall.billing.account.commands;

import java.io.Serializable;
import java.util.Map;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;

public class AccountDataSetCommand implements Serializable, CommandEntityInfo{
@TargetAggregateIdentifier
    private final ObjectId accountId;

    public ObjectId getAccountId() {
        return accountId;
    }

    public Map<String, Object> getData() {
        return data;
    }
    private final Map<String, Object> data;
 
    public AccountDataSetCommand(ObjectId accountId, Map<String, Object> data) {
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
