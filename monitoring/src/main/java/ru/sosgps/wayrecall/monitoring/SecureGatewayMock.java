package ru.sosgps.wayrecall.monitoring;

import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.common.annotation.MetaData;
import ru.sosgps.wayrecall.core.SecureGateway;

import java.util.concurrent.TimeUnit;

/**
 * Created by IVAN on 12.08.2014.
 */

public class SecureGatewayMock implements SecureGateway {

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecureGateway.class);
    public <R> void send(Object command, CommandCallback<R> callback) {
        log.debug("fake send");
    }

    public <R> R sendAndWait(Object command) {
       log.debug("fake sendAndWait");
       return null;
    }
    public <R> R sendAndWait(Object command, long timeout, TimeUnit unit){
        log.debug("fake sendAndWait");
        return null;
    }

    public void send(Object command){
        log.debug("fake send");
    }

    public void send(Object o,@MetaData("userRoles") String[] userRoles,@MetaData("userName") String userName) {
        log.debug("fake send");
    };


    public <R> R sendAndWait(Object o,@MetaData("userRoles") String[] userRoles,@MetaData("userName") String userName)
    {
        log.debug("fake sendAndWait");
        return null;
    };
}
