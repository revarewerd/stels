package ru.sosgps.wayrecall.billing.account;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by ivan on 14.12.15.
 */

// Хз, как назвать
 // Переписать в конфиг

public class AccountRepository {
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccountRepository.class);

    @Autowired
    private MongoDBManager mdbm = null;

    private DBCollection dbAccounts = null;

    @PostConstruct
    void init() {
        dbAccounts = mdbm.getDatabase().underlying().getCollection("accounts");
    }

//    private LoadingCache<ObjectId,Boolean> removedAccountsCache = CacheBuilder.newBuilder()
//            .expireAfterAccess(24, TimeUnit.HOURS)
//            .build(
//                    new CacheLoader<ObjectId, Boolean>() {
//                        @Override
//                        public Boolean load(ObjectId id) {
//                            log.debug("Loading uid in removedAccountsCache \"" + id + "\"");
//                            return checkRemoved(id);
//                        }
//                    });

    private Boolean checkRemoved(ObjectId id) {
        DBObject acc = dbAccounts.findOne(new BasicDBObject("_id", id));
        return !acc.containsField("removed") || ((Boolean)acc.get("removed"));
    }

    public boolean isRemoved(ObjectId id) {
//        try {
//            return removedAccountsCache.get(id);
//        }
//        catch(ExecutionException ex) {
//            throw new RuntimeException("Failed to retrieve value from removedAccountsCache", ex);
//        }
        return checkRemoved(id);
    }

//    public void setRemoved(ObjectId id, boolean removed) {
//        removedAccountsCache.put(id,removed);
//    }

    public DBObject getAccountById(ObjectId id) {
        return dbAccounts.findOne(new BasicDBObject("_id", id));
    }

    public String getAccountName(ObjectId id) {
        DBObject accountById = dbAccounts.findOne(new BasicDBObject("_id", id), new BasicDBObject("name",1));
        if(accountById == null)
            return null;
        return accountById.get("name").toString();
    }

    public DBObject accountById(ObjectId id) {
        return dbAccounts.findOne(new BasicDBObject("_id", id));
    }

}
