/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.monitoring.web;

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod;
import com.google.common.collect.HashBiMap;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.*;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.sosgps.wayrecall.billing.user.commands.UserDataSetCommand;
import ru.sosgps.wayrecall.core.MongoDBManager;
import ru.sosgps.wayrecall.core.SecureGateway;
import ru.sosgps.wayrecall.core.UserRolesChecker;
import ru.sosgps.wayrecall.core.finance.FeeProcessor;
import ru.sosgps.wayrecall.core.Translations;
import ru.sosgps.wayrecall.monitoring.security.DefaultObjectsPermissionsChecker;
import ru.sosgps.wayrecall.utils.ExtDirectService;

import javax.annotation.PostConstruct;

import static ru.sosgps.wayrecall.utils.ScalaConverters.asJavaList;

/**
 * @author ИВАН
 */
@ExtDirectService
public class UserInfo {
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserInfo.class);

    @Autowired
    SecureGateway commandGateway = null;

    @Autowired
    UserRolesChecker roleChecker = null;

    @Autowired
    MongoDBManager mdbm = null;

    @Autowired
    FeeProcessor fee = null;

    @Autowired
    DefaultObjectsPermissionsChecker permissionsChecker = null;

    @Autowired
    Translations translations = null;

    @PostConstruct
    private void init() {
        log.info("ensuring index for balanceHistoryWithDetails");
        mdbm.getDatabase().underlying().getCollection("balanceHistoryWithDetails")
                .createIndex(new BasicDBObject("account", 1).append("timestamp", -1));
    }

    private List<DBObject> loadUserAccounts(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("userId", new ObjectId(userId));
        map.put("recordType", "account");
        log.debug("Data loading for user ID={}", userId);
        DBCursor one = mdbm.getDatabase().underlying().getCollection("usersPermissions")
                .find(new BasicDBObject(map));
        return one.toArray();
    }

    @ExtDirectMethod
    public List<String> getWelcomeMessages() {
        Translations.LocaleTranslations locale = translations.getRequestFromSpringOrDefault();
        final List<DBObject> accounts = asJavaList(permissionsChecker.getDisabledAccounts());
        final ArrayList<String> messages = new ArrayList<>(accounts.size());
        for (DBObject acc : accounts) {
            String blockcause = "";
            if (acc.get("blockcause") != null && !acc.get("blockcause").toString().isEmpty())
                blockcause = locale.tr("userinfo.reason") + ": «" + acc.get("blockcause") + "» ";
            double balance = 0.0;
            try {
                balance = (Long) acc.get("balance") / 100.0;
            } catch (Exception ignored) {
            }
            messages.add(locale.tr("userinfo.accblocking").replace("%ACCNAME%", acc.get("name").toString()) + " " + blockcause + "(" + locale.tr("userinfo.balance") + ": " + balance + ")");
        }
        messages.addAll(getLowFundsNotification());
        messages.addAll(getPeriodToBlockNotification());
        if (isEmptyNotificationActionPwd()) {
            messages.add(locale.tr("userinfo.emptyactionpasw"));
        }
        return messages;
    }

    private List<String> getLowFundsNotification() {
        Translations.LocaleTranslations locale = translations.getRequestFromSpringOrDefault();
        final List<DBObject> accPermRecs = asJavaList(permissionsChecker.getPermissionsRecords("account"));
        List<ObjectId> permittedAccIds = new ArrayList<>();
        for (DBObject acc : accPermRecs) {
            permittedAccIds.add((ObjectId) acc.get("item_id"));
        }
        final DBCursor permittedAccs = mdbm.getDatabase().underlying().getCollection("accounts").find(new BasicDBObject("_id", new BasicDBObject("$in", permittedAccIds)));
        final ArrayList<String> messages = new ArrayList<>(permittedAccs.size());
        for (DBObject acc : permittedAccs) {
            DBCursor accBalance = mdbm.getDatabase().underlying().getCollection("balanceHistoryWithDetails").find(
                    new BasicDBObject("account", acc.get("_id")), new BasicDBObject("timestamp", 1)
                            .append("newbalance", 1)).sort(new BasicDBObject("timestamp", (-1)));
            for (DBObject balanceRecord : accBalance) {
                Long newBalance = (Long) balanceRecord.get("newbalance");
                Long dailyPay = fee.getDailyPayForAccount((ObjectId) acc.get("_id"));
                if (newBalance > 0 && newBalance < dailyPay * 3) {
                    //Date date = (Date) balanceRecord.get("timestamp");
                    //Date currentDate = new Date();
                    //Long daysToBlock = limitValue * 86400000 - (currentDate.getTime() - date.getTime());
                    Double days = newBalance.doubleValue() / dailyPay.doubleValue();
                    log.debug("newBalance =" + newBalance + " dailyPay =" + dailyPay + " days= " + days);
                    int daysToZero = new BigDecimal(newBalance.doubleValue() / dailyPay.doubleValue()).setScale(0, RoundingMode.UP).intValue();
                    Date dateToBlock = new Date();
                    DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
                    dateToBlock.setDate(dateToBlock.getDate() + daysToZero);
                    messages.add(locale.tr("userinfo.accbeforeblocking").replace("%ACCNAME%", acc.get("name").toString()).replace("%BLOCKDATE%", dateFormat.format(dateToBlock)));
                    break;
                } else break;
            }
        }
        return messages;
    }

    private List<String> getPeriodToBlockNotification() {
        Translations.LocaleTranslations locale = translations.getRequestFromSpringOrDefault();
        final List<DBObject> accPermRecs = asJavaList(permissionsChecker.getPermissionsRecords("account"));
        List<ObjectId> permittedAccIds = new ArrayList<>();
        for (DBObject acc : accPermRecs) {
            permittedAccIds.add((ObjectId) acc.get("item_id"));
        }
        final DBCursor permittedAccs = mdbm.getDatabase().underlying().getCollection("accounts").find(new BasicDBObject("_id", new BasicDBObject("$in", permittedAccIds)));
        final ArrayList<String> messages = new ArrayList<>(permittedAccs.size());
        for (DBObject acc : permittedAccs) {
            String limitType = (String) acc.get("limitType");
            if (limitType != null && limitType.equals("daysLimit")) {
                Long limitValue = Long.parseLong((String) acc.get("limitValue"));
                DBCursor accBalance = mdbm.getDatabase().underlying().getCollection("balanceHistoryWithDetails").find(
                        new BasicDBObject("account", acc.get("_id")), new BasicDBObject("timestamp", 1)
                                .append("newbalance", 1)).sort(new BasicDBObject("timestamp", (-1)));
                for (DBObject balanceRecord : accBalance) {
                    Long newBalance = (Long) balanceRecord.get("newbalance");
                    if (newBalance < 0) {
                        Date date = (Date) balanceRecord.get("timestamp");
                        Date currentDate = new Date();
                        Long daysToBlock = limitValue * 86400000 - (currentDate.getTime() - date.getTime());
                        messages.add(locale.tr("userinfo.accdaysblocking").replace("%ACCNAME%", acc.get("name").toString()) + new BigDecimal(daysToBlock / 86400000).setScale(0, RoundingMode.UP).longValue());
                        log.debug("account =" + acc.get("name") + "daysToBlock =" + daysToBlock);
                        break;
                    } else break;
                }
            }
        }
        return messages;
    }

    private boolean isEmptyNotificationActionPwd() {
        Map<String, Object> userInfo = getUserInfo().toMap();

        BasicDBObject params = new BasicDBObject("user", userInfo.get("name"));
        params.append("action", "block");
        params.append("blockpasw", new BasicDBObject("$exists", false));
        DBObject existsEmptyPwd = mdbm.getDatabase().underlying().getCollection("notificationRules").findOne(params);

        return Boolean.TRUE.equals(userInfo.get("hascommandpass")) && existsEmptyPwd != null;
    }

    private List<String> loadUserAccountNames(String userId) {
        List<DBObject> userAccounts = loadUserAccounts(userId);
        List<String> accountNames = new ArrayList<String>();
        for (int i = 0; i < userAccounts.size(); i++) {
            DBObject one = mdbm.getDatabase().underlying().getCollection("accounts")
                    .findOne(new BasicDBObject("_id", userAccounts.get(i).get("item_id")));
            accountNames.add(one.toMap().get("name").toString());
        }
        return accountNames;
    }

    private String getUserId() {
        DBObject one = getUserInfo();
        Map<String, Object> user = one.toMap();
        return user.get("_id").toString();
    }

    @ExtDirectMethod
    public DBObject getUserMainAcc() {
        Translations.LocaleTranslations locale = translations.getRequestFromSpringOrDefault();
        DBObject one = getUserInfo();
        Map<String, Object> user = one.toMap();
        ObjectId mainAccId = (ObjectId) user.get("mainAccId");
        if (mainAccId != null) {
            DBObject two = mdbm.getDatabase().underlying().getCollection("accounts")
                    .findOne(new BasicDBObject("_id", mainAccId));
            return two;//.get("name").toString();
        } else
            return new BasicDBObject("name", locale.tr("userinfo.accundefined"));
    }


    private String getAccBalance(String accountid) {
        ObjectId accid = new ObjectId(accountid);
        //String username=SecurityContextHolder.getContext().getAuthentication().getName();
        DBObject one = mdbm.getDatabase().underlying().getCollection("accounts")
                .findOne(new BasicDBObject("_id", accid), new BasicDBObject("balance", 1));
        Map<String, Object> accbalance = one.toMap();
        return accbalance.getOrDefault("balance", 0).toString();
    }

    public String getMainAccBalance() {
        DBObject mainAcc = getUserMainAcc();
        if (mainAcc == null || mainAcc.get("_id") == null)
            return null;
        return getAccBalance(mainAcc.get("_id").toString());
    }

    public DBObject getUserInfo() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DBObject one = mdbm.getDatabase().underlying().getCollection("users")
                .findOne(new BasicDBObject("name", username));
        return one;
    }

    @ExtDirectMethod
    public Map<String, Object> getDetailedBalanceRules() {
        Map<String, Object> rules = new HashMap<String, Object>();
        Map<String, Object> userInfo = getUserInfo().toMap();
        if (Boolean.TRUE.equals(userInfo.get("showbalance"))) {
            rules.put("showbalance", true);
            rules.put("balance", getMainAccBalance());
        } else {
            rules.put("showbalance", false);
        }
        if (Boolean.TRUE.equals(userInfo.get("showfeedetails"))) {
            rules.put("showfeedetails", true);
        } else {
            rules.put("showfeedetails", false);
        }
        return rules;
    }

    @ExtDirectMethod
    public Map<String, Object> getUserContacts() {
        DBObject userInfo = getUserInfo();
        DBObject userAcc = getUserMainAcc();
        Map<String, Object> contacts = new HashMap<>();
        String email = Objects.toString(userInfo.get("email"), "");
        String phone = Objects.toString(userInfo.get("phone"), "");
        if ("".equals(email)) {
            email = Objects.toString(userAcc.get("cfemail"), "");
        }
        if ("".equals(phone)) {
            phone = Objects.toString(userAcc.get("cfmobphone1"), "");
        }
        if ("".equals(phone)) {
            phone = Objects.toString(userAcc.get("cfmobphone2"), "");
        }
        contacts.put("email", email);
        contacts.put("phone", phone);
        return contacts;
    }

    @ExtDirectMethod
    public boolean isObjectsClustering() {
        DBObject userInfo = getUserInfo();

        return (userInfo.get("isClustering") != null && userInfo.get("isClustering").equals(true));
    }

    @ExtDirectMethod
    public Map<String, Object> getUserSettings() {
        DBObject userInfo = getUserInfo();
        Map<String, Object> userSettings = new HashMap<>();
        userSettings.put("isClustering", userInfo.get("isClustering"));
        userSettings.put("showPopupNotificationsWindow", userInfo.get("showPopupNotificationsWindow"));
        userSettings.put("showEventMarker", userInfo.get("showEventMarker"));
        Boolean showUnreadNotificationsCount = (Boolean) userInfo.toMap().getOrDefault("showUnreadNotificationsCount", true);
        userSettings.put("showUnreadNotificationsCount", showUnreadNotificationsCount);
        Boolean supportNotificationsByEmail = (Boolean) userInfo.get("supportNotificationsByEmail");
        if (supportNotificationsByEmail == null)
            userSettings.put("supportNotificationsByEmail", false);
        else
            userSettings.put("supportNotificationsByEmail", supportNotificationsByEmail);
        return userSettings;
    }

    @ExtDirectMethod
    public String updateUserSettings(Map<String, Object> settings) {
        String username = roleChecker.getUserName();
        DBObject userInfo = getUserInfo();
        boolean editperm = (boolean) userInfo.get("canchangepass");

        BasicDBObject updData = new BasicDBObject();
        String usrPasw = (String) settings.get("usrPasw");
        if (usrPasw != null && !"".equals(usrPasw)) {
            if (!editperm) {
                return "PASSWORD MODIFICATION PROHIBITED";
            }
            if (!userInfo.get("password").toString().equals(settings.get("usrOldPasw"))) {
                return "WRONG PASSWORD";
            }
            updData.put("password", settings.get("usrPasw"));
        }
        if (settings.get("usrEmail") != null) updData.put("email", settings.get("usrEmail"));
        if (settings.get("usrPhone") != null) updData.put("phone", settings.get("usrPhone"));
        if (settings.get("timezoneId") != null) updData.put("timezoneId", settings.get("timezoneId"));
        if (settings.get("isClustering") != null) updData.put("isClustering", settings.get("isClustering"));
        if (settings.get("isEquipGrouping") != null) updData.put("isEquipGrouping", settings.get("isEquipGrouping"));
        if (settings.get("showEventMarker") != null) updData.put("showEventMarker", settings.get("showEventMarker"));
        if (settings.get("showPopupNotificationsWindow") != null)
            updData.put("showPopupNotificationsWindow", settings.get("showPopupNotificationsWindow"));
        if (settings.get("showUnreadNotificationsCount") != null)
            updData.put("showUnreadNotificationsCount", settings.get("showUnreadNotificationsCount"));
        if (settings.get("supportNotificationsByEmail") != null)
            updData.put("supportNotificationsByEmail", settings.get("supportNotificationsByEmail"));

        log.debug("Update user data = " + updData);
        commandGateway.sendAndWait(new UserDataSetCommand(roleChecker.getUserIdByName(username), updData), roleChecker.getUserAuthorities(), username);

        return "SUCCESS";
    }

    @ExtDirectMethod
    public boolean canChangePassword() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return (boolean) mdbm.getDatabase().underlying().getCollection("users").findOne(new BasicDBObject("name", username), new BasicDBObject("canchangepass", 1)).get("canchangepass");
    }
}
