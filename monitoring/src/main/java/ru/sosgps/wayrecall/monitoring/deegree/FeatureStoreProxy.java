package ru.sosgps.wayrecall.monitoring.deegree;

import org.deegree.commons.config.DeegreeWorkspace;
import org.deegree.commons.config.ResourceInitException;
import org.deegree.commons.tom.gml.GMLObject;
import org.deegree.feature.persistence.FeatureStore;
import org.deegree.feature.persistence.FeatureStoreException;
import org.deegree.feature.persistence.FeatureStoreTransaction;
import org.deegree.feature.persistence.lock.LockManager;
import org.deegree.feature.persistence.query.Query;
import org.deegree.feature.stream.FeatureInputStream;
import org.deegree.feature.types.AppSchema;
import org.deegree.filter.FilterEvaluationException;
import org.deegree.geometry.Envelope;

import javax.xml.namespace.QName;

/**
 * Created by nickl on 04.09.14.
 */
public abstract class FeatureStoreProxy implements FeatureStore {

    abstract FeatureStore getSource();

    @Override
    public boolean isAvailable() {
        return getSource().isAvailable();
    }

    @Override
    public AppSchema getSchema() {
        return getSource().getSchema();
    }

    @Override
    public boolean isMapped(QName ftName) {
        return getSource().isMapped(ftName);
    }

    @Override
    public Envelope getEnvelope(QName ftName) throws FeatureStoreException {
        return getSource().getEnvelope(ftName);
    }

    @Override
    public Envelope calcEnvelope(QName ftName) throws FeatureStoreException {
        return getSource().calcEnvelope(ftName);
    }

    @Override
    public FeatureInputStream query(Query query) throws FeatureStoreException, FilterEvaluationException {
        return getSource().query(query);
    }

    @Override
    public FeatureInputStream query(Query[] queries) throws FeatureStoreException, FilterEvaluationException {
        return getSource().query(queries);
    }

    @Override
    public int queryHits(Query query) throws FeatureStoreException, FilterEvaluationException {
        return getSource().queryHits(query);
    }

    @Override
    public int[] queryHits(Query[] queries) throws FeatureStoreException, FilterEvaluationException {
        return getSource().queryHits(queries);
    }

    @Override
    public GMLObject getObjectById(String id) throws FeatureStoreException {
        return getSource().getObjectById(id);
    }

    @Override
    public FeatureStoreTransaction acquireTransaction() throws FeatureStoreException {
        return getSource().acquireTransaction();
    }

    @Override
    public LockManager getLockManager() throws FeatureStoreException {
        return getSource().getLockManager();
    }

    @Override
    public void init(DeegreeWorkspace workspace) throws ResourceInitException {
        getSource().init(workspace);
    }

    @Override
    public void destroy() {
        getSource().destroy();
    }
}
