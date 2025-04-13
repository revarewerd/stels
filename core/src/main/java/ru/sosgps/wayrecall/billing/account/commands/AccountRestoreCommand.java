
package ru.sosgps.wayrecall.billing.account.commands;

import java.io.Serializable;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.CommandEntityInfo;

public class AccountRestoreCommand implements Serializable, CommandEntityInfo{
    @TargetAggregateIdentifier
    private final ObjectId accountId;

    public ObjectId getAccountId() {
        return accountId;
    }
    public AccountRestoreCommand(ObjectId accountId) {
        this.accountId = accountId;
    }
    public String getEntity(){
        return "Account";
    }
    public Object getEntityId(){
        return this.getAccountId();
    }
}
