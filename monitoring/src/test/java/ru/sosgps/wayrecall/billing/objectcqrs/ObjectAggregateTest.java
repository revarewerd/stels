package ru.sosgps.wayrecall.billing.objectcqrs;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.axonframework.eventsourcing.EventSourcingRepository;
import ru.sosgps.wayrecall.core.ObjectCreatedEvent;
import ru.sosgps.wayrecall.core.ObjectDataChangedEvent;
import ru.sosgps.wayrecall.core.ObjectDeletedEvent;
import ru.sosgps.wayrecall.core.ObjectEquipmentChangedEvent;
import ru.sosgps.wayrecall.core.EquipmentCreatedEvent;
import org.axonframework.test.FixtureConfiguration;
import org.axonframework.test.Fixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import javax.annotation.Resource;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.SimpleEventBus;
import org.axonframework.eventstore.EventStore;
import org.axonframework.eventstore.fs.FileSystemEventStore;
import org.axonframework.eventstore.fs.SimpleEventFileResolver;
import org.bson.types.ObjectId;
import org.axonframework.test.matchers.Matchers;
import ru.sosgps.wayrecall.billing.equipment.EquipmentAggregate;


public class ObjectAggregateTest {
    
    private FixtureConfiguration fixture;
    
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ObjectAggregateTest.class);
    
    private  String uid=ObjectAggregate.genUid();
    private Map <String,Serializable> testData = new HashMap<>();
    private Map <String,Serializable> newData;
    private List<Map <String,Serializable>> testEquipments = new ArrayList<>();
    private List<Map <String,Serializable>> newEquipments = new ArrayList<>();
    private List<ObjectId> removedEquipments = new ArrayList<>();
    private List<ObjectId> addedEquipments = new ArrayList<>(); 
    //private ObjectId eq1id=new ObjectId();
    //private ObjectId eq2id=new ObjectId();
    
    @Before
    public void setUp() throws Exception {
        fixture = Fixtures.newGivenWhenThenFixture(ObjectAggregate.class);
        ObjectCommandHandler ch = new ObjectCommandHandler();
        fixture.registerAnnotatedCommandHandler(ch);
        ch.setObjectsAggregatesRepository(fixture.getRepository());        
        EventSourcingRepository <EquipmentAggregate> eqrepo= new   EventSourcingRepository <EquipmentAggregate>(EquipmentAggregate.class, fixture.getEventStore());
        //eqrepo.setEventStore(fixture.getEventStore());
        eqrepo.setEventBus(fixture.getEventBus());
        ch.setEquipmentsAggregatesRepository(eqrepo);
        ch.setEventsStore(fixture.getEventStore());
        newData =new HashMap<>();
        newData.put("name", "test");
        Map <String,Serializable> eq1=new HashMap();
        ObjectId eq1id=new ObjectId();
        eq1.put("_id", eq1id);
        addedEquipments.add(eq1id);
        Map <String,Serializable> eq2=new HashMap();
        ObjectId eq2id=new ObjectId();
        eq2.put("_id", eq2id);
        addedEquipments.add(eq2id);  
        newEquipments.add(eq1);
        newEquipments.add(eq2);    
    }

    @Test
    public void testObjectCreate() throws Exception {        
        fixture.given( )
                .when(new ObjectCreateCommand(uid, testData , testEquipments))
                .expectEvents(new ObjectCreatedEvent(uid, testData, testEquipments));
    }
    
    @Test
    public void testObjectDataSet() throws Exception { 
        fixture.given(new ObjectCreatedEvent(uid, testData, testEquipments))
                .when(new ObjectDataSetCommand(uid, newData,  testEquipments))
                .expectEvents(new ObjectDataChangedEvent(uid, newData));
    }
    
    @Test
    public void testObjectDelete() throws Exception { 
        fixture.given(new ObjectCreatedEvent(uid, testData, testEquipments))
                .when(new ObjectDeleteCommand(uid))
                .expectEvents(new ObjectDeletedEvent(uid));
    }

}
