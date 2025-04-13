package ru.sosgps.wayrecall.billing.equipment;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.axonframework.test.FixtureConfiguration;
import org.axonframework.test.Fixtures;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import ru.sosgps.wayrecall.core.EquipmentCreatedEvent;
import ru.sosgps.wayrecall.core.EquipmentDataChangedEvent;
import ru.sosgps.wayrecall.core.EquipmentDeletedEvent;

public class EquipmentAggregateTest {
    
    private FixtureConfiguration fixture;
    
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EquipmentAggregateTest.class);
    
    private ObjectId id=new ObjectId();
    private Map <String,Serializable> testData = new HashMap<>();
    private Map <String,Serializable> newData;

    @Before
    public void setUp() throws Exception {
        fixture = Fixtures.newGivenWhenThenFixture(EquipmentAggregate.class); 
        EquipmentCommandHandler ch = new EquipmentCommandHandler();
        fixture.registerAnnotatedCommandHandler(ch);
        ch.setEquipmentsAggregatesRepository(fixture.getRepository());
        ch.setEventsStore(fixture.getEventStore());
    }
    @Test
    public void testEquipmentCreate() throws Exception {
        fixture.given()
                .when(new EquipmentCreateCommand(id, testData))
                .expectEvents(new EquipmentCreatedEvent(id, testData));
    }
    @Test
    public void testEquipmentDataSet() throws Exception {        
        newData =new HashMap<>();
        newData.put("name", "test");
        fixture.given(new EquipmentCreatedEvent(id, testData))
                .when(new EquipmentDataSetCommand(id, newData))
                .expectEvents(new EquipmentDataChangedEvent(id, newData));
    }
    @Test
    public void testEquipmentDelete() throws Exception { 
        fixture.given(new EquipmentCreatedEvent(id, testData))
                .when(new EquipmentDeleteCommand(id))
                .expectEvents(new EquipmentDeletedEvent(id, new scala.collection.immutable.HashMap<String, Serializable>()));
    }
}
