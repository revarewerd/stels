package ru.sosgps.wayrecall.utils;

import org.axonframework.domain.EventMessage;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventListener;

/**
 * Created by nickl on 26.05.14.
 */
public class EventBusProxy implements EventBus {

    protected EventBus delegate;

    public EventBusProxy(EventBus delegate) {
        this.delegate = delegate;
    }

    protected EventBusProxy() {
    }

    @Override
    public void publish(EventMessage... events) {
        delegate.publish(events);
    }

    @Override
    public void subscribe(EventListener eventListener) {
        delegate.subscribe(eventListener);
    }

    @Override
    public void unsubscribe(EventListener eventListener) {
        delegate.unsubscribe(eventListener);
    }
}
