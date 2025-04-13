package ru.sosgps.wayrecall.core;
import org.axonframework.commandhandling.gateway.*;
import org.axonframework.common.annotation.MetaData;

public interface SecureGateway extends CommandGateway {

    void send(Object o,@MetaData("userRoles") String[] userRoles,@MetaData("userName") String userName);


    <R> R sendAndWait(Object o,@MetaData("userRoles") String[] userRoles,@MetaData("userName") String userName);
}
