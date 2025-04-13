package ru.sosgps.wayrecall.events;

import ru.sosgps.wayrecall.data.GPSEvent;
import ru.sosgps.wayrecall.data.sleepers.SleeperData;
import ru.sosgps.wayrecall.data.UserMessage;
import ru.sosgps.wayrecall.sms.SMSCommandEvent;
import ru.sosgps.wayrecall.sms.SMSDeliveredEvent;
import ru.sosgps.wayrecall.sms.SMSEvent;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 05.08.13
 * Time: 19:42
 * To change this template use File | Settings | File Templates.
 */
public class PredefinedEvents {

    public static EventTopic<DataEvent<Serializable>> objectChange = new EventTopic<>("object", "change");
    public static EventTopic<DataEvent<Serializable>> equipmentChange = new EventTopic<>("equipment", "change");
    public static EventTopic<GPSEvent> objectGpsEvent = new EventTopic<>("object", "gpsEvent");
    public static EventTopic<UserMessage> userMessage = new EventTopic<>("user", "notification");
    public static EventTopic<DataEvent<SleeperData>> objectNewSleeperData = new EventTopic<>("object", "newSleeperData");
    public static EventTopic<Event> accountChange = new EventTopic<>("account", "change");
    public static EventTopic<DataEvent<HashMap<String, Object>>> userObjectsListChanged = new EventTopic<>("user", "objectsListChanged");
    public static EventTopic<SMSEvent> sms = new EventTopic<>("smsconversation", "sms");
    public static EventTopic<SMSDeliveredEvent> smsDelivered = new EventTopic<>("smsconversation", "smsDelivered");
    public static EventTopic<SMSCommandEvent> commandIsDone = new EventTopic<>("objectPhone", "commandIsDone");
    public static EventTopic<DataEvent<HashMap<String, Object>>> balanceChanged = new EventTopic<>("account", "changeBalance");
}
