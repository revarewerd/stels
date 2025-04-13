
package ru.sosgps.wayrecall.billing.account.commands;

import java.io.Serializable;
import java.util.Map;

import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;

public class AccountRemoveCommand implements Serializable, CommandEntityInfo{
    @TargetAggregateIdentifier
    private final ObjectId accountId;
    public final Map<String,Boolean> options;



    public ObjectId getAccountId() {
        return accountId;
    }
    public AccountRemoveCommand(ObjectId accountId, Map<String,Boolean> options) {
        this.accountId = accountId;
        this.options = options;
    }
    public String getEntity(){
        return "Account";
    }
    public Object getEntityId(){
        return this.getAccountId();
    }
}
