package ru.sosgps.wayrecall.initialization;

import org.eclipse.jetty.nosql.NoSqlSession;
import org.eclipse.jetty.nosql.NoSqlSessionManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.session.AbstractSession;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionContext;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

/**
 * Created by nickl on 16.09.14.
 */
public class NoSqlSessionProxy extends NoSqlSession {
    NoSqlSession wrapped;

    public NoSqlSessionProxy( NoSqlSession wrapped) {
        super((NoSqlSessionManager) wrapped.getSessionManager(), getCreationTime(wrapped), wrapped.getAccessed(), wrapped.getClusterId(), wrapped.getVersion());
        this.wrapped = wrapped;
    }

    private static long getCreationTime(NoSqlSession wrapped){
        try{
            return   wrapped.getCreationTime();
        }
        catch(Exception e){
            return   -1;
        }
    }

    @Override
    public Object doPutOrRemove(String name, Object value) {
        return wrapped.doPutOrRemove(name, value);
    }

    @Override
    public void setAttribute(String name, Object value) {
        wrapped.setAttribute(name, value);
    }

    @Override
    public boolean isDirty() {
        return wrapped.isDirty();
    }

    @Override
    public Set<String> takeDirty() {
        return wrapped.takeDirty();
    }

    @Override
    public Object getVersion() {
        return wrapped.getVersion();
    }

    @Override
    public void setClusterId(String clusterId) {
        wrapped.setClusterId(clusterId);
    }

    @Override
    public void setNodeId(String nodeId) {
        wrapped.setNodeId(nodeId);
    }

    @Override
    public AbstractSession getSession() {
        return wrapped.getSession();
    }

    @Override
    public long getAccessed() {
        return wrapped.getAccessed();
    }

    @Override
    public Map<String, Object> getAttributeMap() {
        return wrapped.getAttributeMap();
    }

    @Override
    public Object getAttribute(String name) {
        return wrapped.getAttribute(name);
    }

    @Override
    public int getAttributes() {
        return wrapped.getAttributes();
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return wrapped.getAttributeNames();
    }

    @Override
    public Set<String> getNames() {
        return wrapped.getNames();
    }

    @Override
    public long getCookieSetTime() {
        return wrapped.getCookieSetTime();
    }

    @Override
    public long getCreationTime() throws IllegalStateException {
        return wrapped.getCreationTime();
    }

    @Override
    public String getId() throws IllegalStateException {
        return wrapped.getId();
    }

    @Override
    public String getNodeId() {
        return wrapped.getNodeId();
    }

    @Override
    public String getClusterId() {
        return wrapped.getClusterId();
    }

    @Override
    public long getLastAccessedTime() throws IllegalStateException {
        return wrapped.getLastAccessedTime();
    }

    @Override
    public void setLastAccessedTime(long time) {
        wrapped.setLastAccessedTime(time);
    }

    @Override
    public int getMaxInactiveInterval() {
        return wrapped.getMaxInactiveInterval();
    }

    @Override
    public ServletContext getServletContext() {
        return wrapped.getServletContext();
    }

    @Override
    @Deprecated
    public HttpSessionContext getSessionContext() throws IllegalStateException {
        return wrapped.getSessionContext();
    }

    @Override
    @Deprecated
    public Object getValue(String name) throws IllegalStateException {
        return wrapped.getValue(name);
    }

    @Override
    @Deprecated
    public String[] getValueNames() throws IllegalStateException {
        return wrapped.getValueNames();
    }

    @Override
    public void renewId(HttpServletRequest request) {
        wrapped.renewId(request);
    }

    @Override
    public SessionManager getSessionManager() {
        return wrapped.getSessionManager();
    }

    @Override
    public void invalidate() throws IllegalStateException {
        wrapped.invalidate();
    }

    @Override
    public void clearAttributes() {
        wrapped.clearAttributes();
    }

    @Override
    public boolean isIdChanged() {
        return wrapped.isIdChanged();
    }

    @Override
    public boolean isNew() throws IllegalStateException {
        return wrapped.isNew();
    }

    @Override
    @Deprecated
    public void putValue(String name, Object value) throws IllegalStateException {
        wrapped.putValue(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        wrapped.removeAttribute(name);
    }

    @Override
    @Deprecated
    public void removeValue(String name) throws IllegalStateException {
        wrapped.removeValue(name);
    }

    @Override
    public void setIdChanged(boolean changed) {
        wrapped.setIdChanged(changed);
    }

    @Override
    public void setMaxInactiveInterval(int secs) {
        wrapped.setMaxInactiveInterval(secs);
    }

    @Override
    public String toString() {
        return wrapped.toString();
    }

    @Override
    public void bindValue(String name, Object value) {
        wrapped.bindValue(name, value);
    }

    @Override
    public boolean isValid() {
        return wrapped.isValid();
    }

    @Override
    public int getRequests() {
        return wrapped.getRequests();
    }

    @Override
    public void setRequests(int requests) {
        wrapped.setRequests(requests);
    }

    @Override
    public void unbindValue(String name, Object value) {
        wrapped.unbindValue(name, value);
    }

    @Override
    public void willPassivate() {
        wrapped.willPassivate();
    }

    @Override
    public void didActivate() {
        wrapped.didActivate();
    }
}
