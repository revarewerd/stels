package ru.sosgps.wayrecall.billing.user;

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
import ru.sosgps.wayrecall.billing.user.commands.*;
import ru.sosgps.wayrecall.billing.user.events.*;
import ru.sosgps.wayrecall.utils.SpringContextCommandBusWrapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test.xml")
public class UserAggregateTest {
    
    private FixtureConfiguration fixture;

    @Autowired
    ApplicationContext applicationContext;
    SpringContextCommandBusWrapper commandBusWrapper;
    
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserAggregateTest.class);
    
    private ObjectId id=new ObjectId();
    private HashMap <String,Object> testData = new HashMap<>();
    private HashMap <String,Object> newData;

    @Before
    public void setUp() throws Exception {
        fixture = Fixtures.newGivenWhenThenFixture(UserAggregate.class);
        commandBusWrapper = new SpringContextCommandBusWrapper(fixture.getCommandBus(), applicationContext);
    }
    @Test
    public void testUserCreate() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
        fixture.given()
                .when(new UserCreateCommand(id, testData))
                .expectEvents(new UserCreateEvent(id, testData));
            }
        });
    }
    @Test
    public void testUserDataSet() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
        newData =new HashMap<>();
        newData.put("name", "test");
        fixture.given(new UserCreateEvent(id, testData))
                .when(new UserDataSetCommand(id, newData))
                .expectEvents(new UserDataSetEvent(id, newData));
            }
        });
    }
    @Test
    public void testUserDelete() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                fixture.given(new UserCreateEvent(id, testData))
                        .when(new UserDeleteCommand(id))
                        .expectEvents(new UserDeleteEvent(id));
            }
        });
    }

}
