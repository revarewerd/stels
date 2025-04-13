package ru.sosgps.wayrecall.billing.account;

import java.util.HashMap;
import org.axonframework.test.FixtureConfiguration;
import org.axonframework.test.Fixtures;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.sosgps.wayrecall.billing.account.commands.*;
import ru.sosgps.wayrecall.billing.account.events.*;
import ru.sosgps.wayrecall.utils.SpringContextCommandBusWrapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test.xml")
public class AccountAggregateTest {
     
    private FixtureConfiguration fixture;

    @Autowired
    ApplicationContext applicationContext;
    
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccountAggregateTest.class);
    
    private ObjectId id=new ObjectId();
    private HashMap <String,Object> testData = new HashMap<>();
    private HashMap <String,Object> newData;

    SpringContextCommandBusWrapper commandBusWrapper;

    @Before
    public void setUp() throws Exception {
        fixture = Fixtures.newGivenWhenThenFixture(AccountAggregate.class);
        commandBusWrapper = new SpringContextCommandBusWrapper(fixture.getCommandBus(), applicationContext);
    }
    @Test
    public void testAccountCreate() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                fixture.given()
                        .when(new AccountCreateCommand(id, testData))
                        .expectEvents(new AccountCreateEvent(id, testData));
            }
        });

    }
    @Test
    public void testAccountDataSet() throws Exception {        
        newData =new HashMap<>();
        newData.put("name", "test");
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                fixture.given(new AccountCreateEvent(id, testData))
                        .when(new AccountDataSetCommand(id, newData))
                        .expectEvents(new AccountDataSetEvent(id, newData));
            }
        });
    }
    @Test
    public void testAccountDelete() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                fixture.given(new AccountCreateEvent(id, testData))
                        .when(new AccountDeleteCommand(id))
                        .expectEvents(new AccountDeleteEvent(id));
            }
        });
    }
}
