package ru.sosgps.wayrecall.events

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.junit.{Assert, Test}
import org.springframework.context.ApplicationContext
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array("/spring-test.xml"))
class EventsTest {


  @Autowired
  var context: ApplicationContext = null

  @Autowired
  var es: EventsStore = null

  @Test
  def test1 {

    val starttime1 = System.currentTimeMillis();

    for (predefinedEvent <- Iterator.from(1).map(i => new SimpleEvent("event3", "" + i)).take(7000 - 1)) {
      es.publish(predefinedEvent)
    }

    Thread.sleep(100)

    val starttime = System.currentTimeMillis();

    val e = new SimpleEvent("event1", "1")
    es.publish(e)

    val eventsAfter = es.getEventsAfter(starttime)
    Assert.assertEquals(1, eventsAfter.size)
    Assert.assertEquals(e, eventsAfter.head)

    //val eventsAfter2 = es.getEventsAfter( EventTarget("event1", "1"), starttime)
    val eventsAfter2 = es.getEventsAfter(new OnTarget("event1", "1"), starttime)
    Assert.assertEquals(1, eventsAfter2.size)
    Assert.assertEquals(e, eventsAfter2.head)

    Assert.assertTrue(es.getEventsAfter(new OnTarget("event1", "2"), starttime).isEmpty)
    Assert.assertTrue(es.getEventsAfter(new EventTopic("event2"), starttime).isEmpty)

    val e2 = new SimpleEvent("event1", "2")
    es.publish(e2)

    val eventsAfter3 = es.getEventsAfter(starttime)
    Assert.assertTrue(eventsAfter3.size == 2)
    Assert.assertTrue(Seq(e, e2).forall(eventsAfter3.contains))

    val eventsAfter4 = es.getEventsAfter(new EventTopic("event1", ""), starttime)
    Assert.assertTrue(eventsAfter4.size == 2)
    Assert.assertTrue(Seq(e, e2).forall(eventsAfter4.contains))
    Thread.sleep(10)
    Assert.assertTrue(es.getEventsAfter(new EventTopic("event1", ""), System.currentTimeMillis()).isEmpty)

    println("allsize=" + es.getEventsAfter(0).size)

    println("timeinterval=" + (System.currentTimeMillis() - starttime))
    println("timeinterval=" + (System.currentTimeMillis() - starttime1))

  }

  @Test
  def test2 {

    var v1 = 0;

    val e = new SimpleEvent("event1", "1")

    val listener = (event: Event) => {
      Assert.assertSame(e, event)
      v1 = v1 + 1
    }
    es.subscribe(new EventTopic("event1"), listener)

    Assert.assertEquals(v1, 0)
    es.publish(new SimpleEvent("event2", "1"));
    Assert.assertEquals(v1, 0)
    es.publish(e);
    Assert.assertEquals(v1, 1)
    v1 = 0;

    es.unsubscribe(new EventTopic("event1"), listener)
    Assert.assertEquals(v1, 0)
    es.publish(new SimpleEvent("event1", "1"));
    Assert.assertEquals(v1, 0)
  }

  @Test
  def test3 {

    var v1 = 0;

    val e = new SimpleEvent("event1", "1")

    val listener = (event: Event) => {
      Assert.assertSame(e, event)
      v1 = v1 + 1
    }
    es.subscribe(new OnTarget("event1", "1"), listener)
    Assert.assertEquals(v1, 0)
    es.publish(new SimpleEvent("event1", "2"));
    Assert.assertEquals(v1, 0)
    es.publish(e);
    Assert.assertEquals(v1, 1)
    v1 = 0;

    es.unsubscribe(new OnTarget("event1", "1"), listener)
    Assert.assertEquals(v1, 0)
    es.publish(new SimpleEvent("event1", "1"));
    Assert.assertEquals(v1, 0)
  }


  @Test
  def eventsTypeTest {

    var v1 = 0;
    var v2 = 0;

    val e = new SimpleEvent("event1", "1", "add")

    val listener1 = (event: Event) => {
      Assert.assertSame(e, event)
      v1 = v1 + 1
    }

    val listener2 = (event: Event) => {
      Assert.assertSame(e, event)
      v2 = v2 + 1
    }

    es.subscribe(new OnTarget("event1", "1", "add"), listener1)
    es.subscribe(new EventTopic("event1", "add"), listener2)

    Assert.assertEquals(v1, 0)
    Assert.assertEquals(v2, 0)
    es.publish(new SimpleEvent("event1", "1"));
    Assert.assertEquals(v1, 0)
    Assert.assertEquals(v2, 0)
    es.publish(new SimpleEvent("event1", "1", "remove"));
    Assert.assertEquals(v1, 0)
    Assert.assertEquals(v2, 0)

    es.publish(e);
    Assert.assertEquals(v1, 1)
    Assert.assertEquals(v2, 1)
    v1 = 0;
    v2 = 0;

    es.unsubscribe(new OnTarget("event1", "1", "add"), listener1)
    es.unsubscribe(new EventTopic("event1", "add"), listener2)
    Assert.assertEquals(v1, 0)
    Assert.assertEquals(v2, 0)
    es.publish(new SimpleEvent("event1", "1", "add"));
    Assert.assertEquals(v1, 0)
    Assert.assertEquals(v2, 0)
  }


  class MyEvent(typ: TargetType, id: String, eventType: EventType = "")
    extends SimpleEvent(typ: TargetType, id: String, eventType: EventType)

  @Test
  def eventsTypeTypedTest {

    var v1 = 0;
    var v2 = 0;

    val e = new MyEvent("event1", "1", "add")

    val listener1 = (event: MyEvent) => {
      Assert.assertSame(e, event)
      v1 = v1 + 1
    }

    val listener2 = (event: Event) => {
      v2 = v2 + 1
    }

    es.subscribeTyped(new OnTarget[MyEvent]("event1", "1", "add"), listener1)
    es.subscribeTyped(new EventTopic[Event]("event1", "add"), listener2)

    Assert.assertEquals(v1, 0)
    Assert.assertEquals(v2, 0)
    es.publish(new SimpleEvent("event1", "1"));
    Assert.assertEquals(v1, 0)
    Assert.assertEquals(v2, 0)
    es.publish(new SimpleEvent("event1", "1", "add"));
    Assert.assertEquals(v1, 0)
    Assert.assertEquals(v2, 1)
    v2 = 0;

    es.publish(e);
    Assert.assertEquals(v1, 1)
    Assert.assertEquals(v2, 1)
    v1 = 0;
    v2 = 0;

    es.unsubscribeTyped(new OnTarget("event1", "1", "add"), listener1)
    es.unsubscribe(new EventTopic("event1", "add"), listener2)
    Assert.assertEquals(v1, 0)
    Assert.assertEquals(v2, 0)
    es.publish(new MyEvent("event1", "1", "add"));
    Assert.assertEquals(v1, 0)
    Assert.assertEquals(v2, 0)
  }


}
