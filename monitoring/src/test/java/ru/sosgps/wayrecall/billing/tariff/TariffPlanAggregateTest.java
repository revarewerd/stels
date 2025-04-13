
package ru.sosgps.wayrecall.billing.tariff;

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
import ru.sosgps.wayrecall.billing.tariff.commands.*;
import ru.sosgps.wayrecall.billing.tariff.events.*;
import ru.sosgps.wayrecall.utils.SpringContextCommandBusWrapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test.xml")
public class TariffPlanAggregateTest {

    private FixtureConfiguration fixture;

    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TariffPlanAggregateTest.class);

    private ObjectId id = new ObjectId();
    private HashMap<String, Object> testData = new HashMap<>();
    private HashMap<String, Object> newData;

    @Autowired
    ApplicationContext applicationContext;
    SpringContextCommandBusWrapper commandBusWrapper;

    @Before
    public void setUp() throws Exception {
        fixture = Fixtures.newGivenWhenThenFixture(TariffPlanAggregate.class);
        commandBusWrapper = new SpringContextCommandBusWrapper(fixture.getCommandBus(), applicationContext);
    }

    @Test
    public void testTariffCreate() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                fixture.given()
                        .when(new TariffPlanCreateCommand(id, testData))
                        .expectEvents(new TariffPlanCreateEvent(id, testData));
            }
        });
    }

    @Test
    public void testTariffDataSet() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                newData = new HashMap<>();
                newData.put("name", "test");
                fixture.given(new TariffPlanCreateEvent(id, testData))
                        .when(new TariffPlanDataSetCommand(id, newData))
                        .expectEvents(new TariffPlanDataSetEvent(id, newData));
            }
        });
    }

    @Test
    public void testTariffDelete() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                fixture.given(new TariffPlanCreateEvent(id, testData))
                        .when(new TariffPlanDeleteCommand(id))
                        .expectEvents(new TariffPlanDeleteEvent(id));
            }
        });
    }
}
