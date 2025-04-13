package ru.sosgps.wayrecall.billing.account;

import com.mongodb.BasicDBObject;
import java.io.Serializable;

import com.mongodb.DBCursor;
import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.bson.types.ObjectId;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.billing.account.commands.*;
import ru.sosgps.wayrecall.billing.account.events.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;
import com.mongodb.DBObject;
import ru.sosgps.wayrecall.utils.SpringContextCommandBusWrapper;

public class AccountAggregate extends AbstractAnnotatedAggregateRoot implements Serializable {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccountAggregate.class);

    @AggregateIdentifier
    ObjectId accountId;
    Map<String, Object> data = new HashMap<>();

    public AccountAggregate() {

    }

    @CommandHandler
    public AccountAggregate(AccountCreateCommand command) {
        log.debug("Account create command=" + command.getAccountId());
        checkDuplicate(command.getAccountId(), command.getData());
        final ArrayList<Object> events = new ArrayList<>(3);
        events.add(new AccountCreateEvent(command.getAccountId(), new HashMap<>(command.getData())));
        final Object status = data.get("status");
        if (status != null)
            events.add(new AccountStatusChanged(accountId, (Boolean) status));
        for (Object o : events) {
            apply(o);
        }
    }

    @EventHandler
    public void onAccountCreate(AccountCreateEvent event) {
        this.accountId = event.getAccountId();
        this.data.putAll(event.getData());
    }

    @CommandHandler
    public void setAccountData(AccountDataSetCommand command) {
        processDataSet(command.getData());
    }

    private void processDataSet(Map<String, Object> data) {
        Map<String, Object> changedData = ObjectsRepositoryReader.detectChanged(this.data, data);
        log.debug("Changed data" + changedData);
        final ArrayList<Object> events = new ArrayList<>(3);
        if (!changedData.isEmpty()) {
            checkDuplicate(this.accountId, data);
            events.add(new AccountDataSetEvent(this.accountId, new HashMap<>(changedData)));
        }

        final Object status = changedData.get("status");
        if (status != null)
            events.add(new AccountStatusChanged(accountId, (Boolean) status));

        for (Object o : events) {
            apply(o);
        }
    }

    @EventHandler
    public void onSetNewData(AccountDataSetEvent event) {
        this.data.putAll(event.getData());
    }

    @CommandHandler
    public void deleteAccount(AccountDeleteCommand command) {
        log.debug("Account delete command=" + command.getAccountId());
        apply(new AccountDeleteEvent(command.getAccountId()));
    }

    @CommandHandler
    public void removeAccount(AccountRemoveCommand command) {
        log.debug("Account remove command=" + command.getAccountId());

        apply(new AccountRemoveEvent(command.getAccountId(), command.options));
    }

    @CommandHandler
    public void restoreAccount(AccountRestoreCommand command) {
        log.debug("Account restore command=" + command.getAccountId());
        apply(new AccountRestoreEvent(command.getAccountId()));
    }

    private void checkDuplicate(ObjectId accountId, Map<String, Object> data) {
        log.debug("checkDuplicate");
        if (data != null && data.get("name") != null) {
            DBObject exsistAcc = getMdbm().getDatabase().underlying().getCollection("accounts").findOne(new BasicDBObject("name", data.get("name")));
            if (exsistAcc != null && !exsistAcc.get("_id").equals(accountId)) {
                throw new IllegalArgumentException("Такая запись уже существует");
            }
        }
    }

    public MongoDBManager getMdbm() {
       return SpringContextCommandBusWrapper.getApplicationContext().getBean(MongoDBManager.class);
    }
}
