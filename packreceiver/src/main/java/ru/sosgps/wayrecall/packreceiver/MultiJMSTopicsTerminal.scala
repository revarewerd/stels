package ru.sosgps.wayrecall.packreceiver

import javax.annotation.PostConstruct

import org.axonframework.domain.EventMessage
import org.axonframework.eventhandling.{Cluster, EventBusTerminal}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import ru.sosgps.wayrecall.core.JMSEventBusTerminal
import ru.sosgps.wayrecall.initialization.MultiserverConfig

import scala.collection.mutable.ListBuffer

/**
 * Created by nickl on 22.07.14.
 */
class MultiJMSTopicsTerminal extends EventBusTerminal {

  @Autowired
  var multiInstnceConfig: MultiserverConfig = null

  @Autowired
  var applicationContext: ApplicationContext = null

  private lazy val jmsListeners = {
    val factory = applicationContext.getAutowireCapableBeanFactory
    multiInstnceConfig.instances.map(icfg => {
      val terminal = new JMSEventBusTerminal()
      terminal.topicPrefix = icfg.name
      factory.autowireBean(terminal)
      factory.initializeBean(terminal, null)
      terminal
    })
  }


//  @PostConstruct
//  def init{
//    jmsListeners
//  }

  protected[this] val clusters = new ListBuffer[Cluster]

  override def publish(events: EventMessage[_]*): Unit = {
    clusters.foreach(_.publish(events: _ *))
    jmsListeners.foreach(_.publishRemote(events: _ *))
  }

  override def onClusterCreated(cluster: Cluster): Unit = {
    clusters += cluster
    jmsListeners.foreach(_.onClusterCreated(cluster))
  }
}
