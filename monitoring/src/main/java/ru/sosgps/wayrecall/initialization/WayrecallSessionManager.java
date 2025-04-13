package ru.sosgps.wayrecall.initialization;

import javax.servlet.http.HttpSession;
import java.util.Collection;

/**
 * Created by nickl on 17.09.14.
 */
public interface WayrecallSessionManager {
    Collection<HttpSession> getActiveSessions();
}
