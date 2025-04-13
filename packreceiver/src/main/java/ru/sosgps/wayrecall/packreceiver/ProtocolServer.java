package ru.sosgps.wayrecall.packreceiver;

import io.netty.channel.EventLoopGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Created by nmitropo on 12.8.2016.
 */
public abstract class ProtocolServer {

    protected PackProcessor store;
    protected int port;

    @PostConstruct
    public abstract void start() throws InterruptedException;

    @PreDestroy
    public abstract void stop() throws InterruptedException;

    public void setPort(int port) {
        this.port = port;
    }

    public PackProcessor getStore() {
        return store;
    }

    public void setStore(PackProcessor store) {
        this.store = store;
    }
}
