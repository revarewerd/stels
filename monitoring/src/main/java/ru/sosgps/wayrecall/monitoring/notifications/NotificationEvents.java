/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.monitoring.notifications;

import java.io.Serializable;
import ru.sosgps.wayrecall.core.GPSData;
import ru.sosgps.wayrecall.events.EventTopic;
import ru.sosgps.wayrecall.events.DataEvent;
import ru.sosgps.wayrecall.monitoring.notifications.rules.Notification;

/**
 *
 * @author oxymo
 */
public class NotificationEvents {
     public static EventTopic<DataEvent<Notification<?>>> objectNotification = new EventTopic<>("object", "notification");

}
