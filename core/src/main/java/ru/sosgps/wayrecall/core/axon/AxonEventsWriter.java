package ru.sosgps.wayrecall.core.axon;

import com.mongodb.DBCollection;
import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.SimpleDomainEventStream;
import org.axonframework.eventstore.mongo.MongoEventStore;
import org.axonframework.eventstore.mongo.MongoTemplate;
import org.axonframework.repository.ConcurrencyException;
import org.axonframework.serializer.Serializer;
import org.springframework.beans.factory.annotation.Autowired;
import ru.sosgps.wayrecall.core.MongoDBManager;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Created by ivan on 25.01.16.
 */
public class AxonEventsWriter extends MongoEventStore {
    private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AxonEventsWriter.class);

    @Autowired
    private MongoDBManager mdbm;

    @Autowired
    private EventsViewConverter eventsViewConverter;

    private DBCollection dbEventsView = null;

    public String getViewCollectionName() {
        return viewCollectionName;
    }

    public void setViewCollectionName(String viewCollectionName) {
        this.viewCollectionName = viewCollectionName;
    }

    private String viewCollectionName = null;
    @PostConstruct
    void init() {
        dbEventsView = mdbm.getDatabase().underlying().getCollection(viewCollectionName);
    }

    private Serializer eventSerializer = null;

    public AxonEventsWriter(Serializer eventSerializer, MongoTemplate mongo) {
        super(eventSerializer, mongo);
        this.eventSerializer = eventSerializer;
    }


    @Override
    public void appendEvents(String type, DomainEventStream events) {
        log.debug(this.getClass().getName() + " appendEvents ");
        List<DomainEventMessage> eventList = new ArrayList<>();
        while(events.hasNext()) {
            eventList.add(events.next());
        }
        log.debug("EventList is " + eventList);
        try {
            super.appendEvents(type, new SimpleDomainEventStream(eventList));
        }
        catch (ConcurrencyException ex) {
            throw new ConcurrencyException("Duplicate collection write cancelled ", ex);
        }

        for(DomainEventMessage message : eventList) {
            dbEventsView.insert(eventsViewConverter.fromEventMessage(type, message));
        }
        log.debug("All events written");
    }

}
