
package ru.sosgps.wayrecall.billing.equipment.types;


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
import ru.sosgps.wayrecall.billing.equipment.types.commands.EquipmentTypesCreateCommand;
import ru.sosgps.wayrecall.billing.equipment.types.commands.EquipmentTypesDataSetCommand;
import ru.sosgps.wayrecall.billing.equipment.types.commands.EquipmentTypesDeleteCommand;
import ru.sosgps.wayrecall.billing.equipment.types.events.EquipmentTypesCreateEvent;
import ru.sosgps.wayrecall.billing.equipment.types.events.EquipmentTypesDataSetEvent;
import ru.sosgps.wayrecall.billing.equipment.types.events.EquipmentTypesDeleteEvent;
import ru.sosgps.wayrecall.utils.SpringContextCommandBusWrapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test.xml")
public class EquipmentTypesAggregateTest {
    
    private FixtureConfiguration fixture;


    @Autowired
    ApplicationContext applicationContext;
    SpringContextCommandBusWrapper commandBusWrapper;
    
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EquipmentTypesAggregateTest.class);
    
    private ObjectId id=new ObjectId();
    private HashMap <String,Object> testData = new HashMap<>();
    private HashMap <String,Object> newData;

    @Before
    public void setUp() throws Exception {
        fixture = Fixtures.newGivenWhenThenFixture(EquipmentTypesAggregate.class);
        commandBusWrapper = new SpringContextCommandBusWrapper(fixture.getCommandBus(), applicationContext);
    }
    @Test
    public void testEquipmentTypeCreate() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
                fixture.given()
                        .when(new EquipmentTypesCreateCommand(id, testData))
                        .expectEvents(new EquipmentTypesCreateEvent(id, testData));
            }
        });
    }

    @Test
    public void testEquipmentTypeDataSet() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
        newData =new HashMap<>();
        newData.put("name", "test");
        fixture.given(new EquipmentTypesCreateEvent(id, testData))
                .when(new EquipmentTypesDataSetCommand(id, newData))
                .expectEvents(new EquipmentTypesDataSetEvent(id, newData));
            }
        });
    }

    @Test
    public void testEquipmentTypeDelete() throws Exception {
        commandBusWrapper.withAppContext(new Runnable() {
            @Override
            public void run() {
        fixture.given(new EquipmentTypesCreateEvent(id, testData))
                .when(new EquipmentTypesDeleteCommand(id))
                .expectEvents(new EquipmentTypesDeleteEvent(id));
            }
        });
    }

}
