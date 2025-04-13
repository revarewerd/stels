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
import ru.sosgps.wayrecall.billing.user.permission.commands.*;
import ru.sosgps.wayrecall.billing.user.permission.events.*;
import ru.sosgps.wayrecall.utils.SpringContextCommandBusWrapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test.xml")
public class PermissionAggregateTest {

    private FixtureConfiguration fixture;

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PermissionAggregateTest.class);

    private ObjectId id = new ObjectId();
    private HashMap<String, Object> testData = new HashMap<>();
    private HashMap<String, Object> newData;

    @Autowired
    ApplicationContext applicationContext;
    SpringContextCommandBusWrapper commandBusWrapper;

    @Before
    public void setUp() throws Exception {
        fixture = Fixtures.newGivenWhenThenFixture(PermissionAggregate.class);
        commandBusWrapper = new SpringContextCommandBusWrapper(fixture.getCommandBus(), applicationContext);
    }

    @Test
    public void testPermissionCreate() throws Exception {

        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                fixture.given()
                        .when(new PermissionCreateCommand(id, testData))
                        .expectEvents(new PermissionCreateEvent(id, testData));
            }
        });
    }

    @Test
    public void testPermissionDataSet() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                newData = new HashMap<>();
                newData.put("name", "test");
                newData.put("userId", "");
                newData.put("recordType", "");
                newData.put("item_id", "");
                fixture.given(new PermissionCreateEvent(id, testData))
                        .when(new PermissionDataSetCommand(id, newData))
                        .expectEvents(new PermissionDataSetEvent(id, newData));
            }
        });
    }

    @Test
    public void testPermissionDelete() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                newData = new HashMap<>();
                newData.put("name", "test");
                newData.put("userId", "");
                fixture.given(new PermissionCreateEvent(id, testData))
                        .when(new PermissionDeleteCommand(id, newData))
                        .expectEvents(new PermissionDeleteEvent(id, newData));
            }
        });
    }

}
