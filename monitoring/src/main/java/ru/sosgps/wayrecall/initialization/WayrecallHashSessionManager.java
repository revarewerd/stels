package ru.sosgps.wayrecall.initialization;

import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.HashedSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Created by nickl on 04.05.14.
 */
public class WayrecallHashSessionManager extends HashSessionManager implements WayrecallSessionManager {

    @Override
    protected AbstractSession newSession(long created, long accessed, String clusterId) {
        return new MyHashedSession(this, created, accessed, clusterId);

    }

    @Override
    public Collection<HttpSession> getActiveSessions(){
        return Collections.<HttpSession> unmodifiableCollection(_sessions.values());
    }

    @Override
    protected AbstractSession newSession(HttpServletRequest request) {
        return new MyHashedSession(this, request);
    }

    private static class MyHashedSession extends HashedSession{
        private MyHashedSession(HashSessionManager hashSessionManager, HttpServletRequest request) {
            super(hashSessionManager, request);
        }

        private MyHashedSession(HashSessionManager hashSessionManager, long created, long accessed, String clusterId) {
            super(hashSessionManager, created, accessed, clusterId);
        }

        @Override
        public synchronized void save(OutputStream os)  throws IOException
        {
            DataOutputStream out = new DataOutputStream(os);
            out.writeUTF(getClusterId());
            out.writeUTF(getNodeId());
            out.writeLong(getCreationTime());
            out.writeLong(getAccessed());

            out.writeInt(getRequests());

            int count = 0;
            for(Object v: getAttributeMap().values()){
               if(isSerializable(v))
                   count++;
            }

            out.writeInt(count);
            ObjectOutputStream oos = new ObjectOutputStream(out);
            Enumeration<String> e=getAttributeNames();
            while(e.hasMoreElements())
            {
                String key=e.nextElement();
                Object obj = doGet(key);
                if(isSerializable(obj)) {
                    oos.writeUTF(key);
                    oos.writeObject(obj);
                }
            }

            out.writeInt(getMaxInactiveInterval());
        }

        private boolean isSerializable(Object obj) {
            return java.io.Serializable.class.isAssignableFrom(obj.getClass());
        }
    }

}
