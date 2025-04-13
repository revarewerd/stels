package ru.sosgps.wayrecall.tools

import java.util.Properties

import com.beust.jcommander.{Parameter, Parameters}
import com.mongodb.casbah.commons.MongoDBObject
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.core.env.{PropertiesPropertySource, MutablePropertySources, StandardEnvironment}
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.core.axon.{AggregateType, EventsViewConverter}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand

/**
  * Created by ivan on 05.02.16.
  */
@Parameters(commandDescription = "Fill domainEventsView for given database")
object DomainEventsViewCreator extends CliCommand {
  override val commandName: String = "domainEventsViewCreate"

  @Parameter(names = Array("-d", "-db"), required = false, description = "database name, default loaded from config")
  var dbname: String = null

  @Parameter(names = Array("-t"), required = false, description = "aggregate type")
  var aggregateType:  String = null

  var mdbm: MongoDBManager = null
  var context: ClassPathXmlApplicationContext = null
  var converter: EventsViewConverter = null;

  override def process(): Unit = {
    context = new ClassPathXmlApplicationContext();
    context.setConfigLocation("axon-config.xml");

    if (dbname != null) {
      context.setEnvironment(new StandardEnvironment {
        protected override def customizePropertySources(propertySources: MutablePropertySources) {
          super.customizePropertySources(propertySources)
          val generatedProps: Properties = new Properties
          generatedProps.setProperty("instance.defaultmongodb.databaseName", dbname)
          propertySources.addFirst(new PropertiesPropertySource("generated", generatedProps))
        }
      })
    }

    context.refresh();
    context.registerShutdownHook();
    mdbm = context.getBean(classOf[MongoDBManager]);
    converter = context.getBean(classOf[EventsViewConverter])
    if(aggregateType != null && !AggregateType.values.contains(aggregateType)) {
      println("Wrong aggregate type")
      println("Aggregate types: ")
      AggregateType.values.foreach(println)
    }
    createDomainEventsView();
  }

  def createDomainEventsView(): Unit = {
    val (eventQuery, eventViewQuery) =
      if (aggregateType != null)
        (MongoDBObject("type" -> aggregateType), MongoDBObject("aggregateType" -> aggregateType))
      else
        (MongoDBObject(),MongoDBObject())
    val dbEvents = mdbm.getDatabase().apply("domainEvents")
    val dbEventsView = mdbm.getDatabase().apply("domainEventsView")
    println(s"removed ${dbEventsView.remove(eventViewQuery).getN} documents")
    var counter = 0;
    dbEvents.find(eventQuery).map(converter.fromDomainEventDocument).foreach(doc => {
      dbEventsView.insert(doc);
      counter += 1;
    })
    println(s"Success: $counter documents written")
  }
}
