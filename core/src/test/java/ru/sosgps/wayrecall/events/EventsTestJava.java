package ru.sosgps.wayrecall.events;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import scala.collection.Seq;

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 06.04.13
 * Time: 19:48
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/spring-test.xml")
public class EventsTestJava {


    @Autowired
    org.springframework.context.ApplicationContext context = null;

    @Autowired
    EventsStore es = null;

    @Test
    public void test1(){

        long starttime = System.currentTimeMillis();

        SimpleEvent e = new SimpleEvent("event1", "1","");
        es.publish(e);

        Seq<Event> eventsAfter = es.getEventsAfter(starttime);
        Assert.assertTrue(eventsAfter.size() == 1);
        Assert.assertEquals(e,eventsAfter.head());

        //val eventsAfter2 = es.getEventsAfter( EventTarget("event1", "1"), starttime)
        Seq<Event> eventsAfter2 = es.getEventsAfter(new OnTarget("event1", "1"), starttime);
        Assert.assertTrue(eventsAfter2.size() == 1);
        Assert.assertEquals(e,eventsAfter2.head());

        Assert.assertTrue(es.getEventsAfter(new OnTarget("event1", "2"), starttime).isEmpty());
        Assert.assertTrue(es.getEventsAfter(new EventTopic("event2"),starttime).isEmpty());

    }


}
