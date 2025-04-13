package ru.sosgps.wayrecall.testutils.spring

import javax.jms.Destination


import org.apache.activemq.spring.ActiveMQConnectionFactory
import org.apache.activemq.xbean.XBeanBrokerService
import org.springframework.context.annotation.Bean
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.listener.DefaultMessageListenerContainer
import org.springframework.jms.listener.adapter.MessageListenerAdapter

/**
  * Created by nickl-mac on 10.06.16.
  */
trait JMSContext extends grizzled.slf4j.Logging {

  @Bean
  def broker = {
    val brokerService = new XBeanBrokerService
    brokerService.setUseJmx(false)
    brokerService.setPersistent(false)
    brokerService.setTransportConnectorURIs(Array("tcp://localhost:0"))
    brokerService
  }

  @Bean
  def conectionFactory = {
    val cf = new ActiveMQConnectionFactory()
    cf.setBrokerURL("vm://localhost")
    cf
  }

  def defaultDestination: Destination

  @Bean
  def producerTemplate = {
    val t = new JmsTemplate
    t.setConnectionFactory(conectionFactory)
    t.setPubSubDomain(true)
    t.setDefaultDestination(defaultDestination)
    t.setDeliveryPersistent(false)
    t
  }

  def listener(bean: AnyRef, method: String, topic: String) = {
    debug(s"starting listener $bean $method $topic")
    val ct = new DefaultMessageListenerContainer()
    ct.setConnectionFactory(conectionFactory)
    ct.setPubSubDomain(true)
    ct.setSessionAcknowledgeMode(javax.jms.Session.AUTO_ACKNOWLEDGE)
    ct.setDestinationName(topic);
    val adapter = new MessageListenerAdapter(bean)
    adapter.setDefaultListenerMethod(method)
    ct.setMessageListener(adapter);
    ct
  }


}