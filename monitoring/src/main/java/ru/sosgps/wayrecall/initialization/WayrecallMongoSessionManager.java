package ru.sosgps.wayrecall.initialization;

import com.google.common.collect.ImmutableMap;
import com.mongodb.MongoException;
import kamon.Kamon;
import kamon.metric.instrument.Gauge;
import org.eclipse.jetty.nosql.NoSqlSession;
import org.eclipse.jetty.nosql.mongodb.MongoSessionManager;
import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.util.component.LifeCycle;
import org.springframework.beans.factory.annotation.Value;

import static kamon.util.JavaTags.tagsFromMap;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by nickl on 16.09.14.
 */
public class WayrecallMongoSessionManager extends MongoSessionManager implements WayrecallSessionManager {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WayrecallMongoSessionManager.class);

    public WayrecallMongoSessionManager() throws UnknownHostException, MongoException {
        super();
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private SessionLogger sessionLogger = new SessionLogger(new File("sessionLog"));

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WayrecallMongoSessionManager(");
        sb.append(name);
        sb.append(", ");
        sb.append(System.identityHashCode(this));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public AbstractSession getSession(String idInCluster) {
        log.debug(this + "loading session for: " + idInCluster);
        try {
            final AbstractSession session = super.getSession(idInCluster);
            sessionLogger.log("loading session for", session);
            return session;
        } catch (Exception e) {
            log.warn("Error loading session for " + idInCluster, e);
            return null;
        }
    }

    @Override
    protected boolean remove(NoSqlSession session) {
        sessionLogger.log("removing session for", session, true);
        return super.remove(session);
    }

    @Override
    protected synchronized Object save(NoSqlSession session, Object version, boolean activateAfterSave) {

        log.debug("saving session:" + session);
        sessionLogger.log("saving session:", session);
        return super.save(new NoSqlSessionProxy(session) {
            @Override
            public Set<String> getNames() {
                final Set<String> names = super.getNames();
                final Set<String> result = new HashSet<String>(names.size());

                log.debug("session items:" + names);

                for (String s : names) {
                    if (getAttribute(s) instanceof Serializable)
                        result.add(s);
                }

                log.debug("serializable session items:" + result);

                return result;
            }

            @Override
            public Set<String> takeDirty() {
                final Set<String> names = super.takeDirty();

                final Set<String> result = new HashSet<String>(names.size());

                log.debug("dirty session items:" + names);

                for (String s : names) {
                    if (getAttribute(s) instanceof Serializable)
                        result.add(s);
                }

                log.debug("dirty serializable session items:" + result);

                return result;
            }
        }, version, activateAfterSave);
    }

    @Override
    public Collection<HttpSession> getActiveSessions() {
        return Collections.<HttpSession>unmodifiableCollection(_sessions.values());
    }


    String instanceName = null;
    String appname = null;

    @Override
    protected void start(LifeCycle l) throws Exception {
        super.start(l);
        sessionLogger.start();

        Kamon.metrics().gauge(
                "user-sessions",
                tagsFromMap(ImmutableMap.of(
                        "instance", instanceName != null ? instanceName : "unknown",
                        "appname", appname != null ? appname : "unknown" )
                ), _sessions::size);

    }

    @PreDestroy
    private void preDestroy(){
        sessionLogger.stop();
    }

}


