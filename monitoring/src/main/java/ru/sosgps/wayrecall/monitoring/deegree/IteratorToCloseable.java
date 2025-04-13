package ru.sosgps.wayrecall.monitoring.deegree;

import org.deegree.commons.utils.CloseableIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.10.12
 * Time: 16:24
 * To change this template use File | Settings | File Templates.
 */


public class IteratorToCloseable<T> implements CloseableIterator<T> {

    private Iterator<? extends T> internal;

    public IteratorToCloseable(Iterator<? extends T> internal) {
        this.internal = internal;
    }

    @Override
    public boolean hasNext() {
        return internal.hasNext();
    }

    @Override
    public T next() {
        return internal.next();
    }

    @Override
    public void remove() {
        internal.remove();
    }

    @Override
    public void close() {

    }

    @Override
    public List<T> getAsListAndClose() {
        ArrayList<T> res = new ArrayList<T>();
        while(this.hasNext())
            res.add(this.next());

        return res;
    }

    @Override
    public Collection<T> getAsCollectionAndClose(Collection<T> collection) {
        collection.clear();
        collection.addAll(this.getAsListAndClose());
        return collection;
    }

}
