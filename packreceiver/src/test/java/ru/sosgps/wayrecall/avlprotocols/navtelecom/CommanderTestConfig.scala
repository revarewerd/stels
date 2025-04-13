package ru.sosgps.wayrecall.avlprotocols.navtelecom

import java.util
import javax.annotation.PostConstruct
import javax.jms.{ConnectionFactory, Destination, Message, MessageListener}

import org.apache.activemq.command.ActiveMQTopic
import org.apache.activemq.spring.ActiveMQConnectionFactory
import org.apache.activemq.xbean.XBeanBrokerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context._
import org.springframework.context.annotation.{Bean, Configuration, DependsOn}
import org.springframework.context.support.StaticApplicationContext
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.listener.DefaultMessageListenerContainer
import org.springframework.jms.listener.adapter.MessageListenerAdapter
import ru.sosgps.wayrecall.packreceiver.DeviceCommandJMSListener
import ru.sosgps.wayrecall.packreceiver.netty.{DTMServer, NavTelecomNettyServer}
import ru.sosgps.wayrecall.testutils.spring.JMSContext


/**
  * Created by nickl-mac on 23.02.16.
  */
@Configuration
class CommanderTestConfig extends JMSContext with grizzled.slf4j.Logging {


  private val NAVTELECOM_COMMAND_TOPIC: String = "receiver.networkcommand.navtelecom"

  @Bean
  def defaultDestination = new ActiveMQTopic(NAVTELECOM_COMMAND_TOPIC)

  @Bean
  def navTelecomCommanderListener = listener(deviceCommandJMSListener, "receiveCommand", NAVTELECOM_COMMAND_TOPIC)

  @Bean
  def messageReceiverListener = listener(messageReceiver, "receive", "receiver.networkcommand.result")


  @Bean
  def messageReceiver = new ru.sosgps.wayrecall.avlprotocols.navtelecom.MessageReceiver

  @Bean
  def navTelecomCommander = new ru.sosgps.wayrecall.avlprotocols.navtelecom.NavTelecomCommander

  @Bean
  def deviceCommandJMSListener = new DeviceCommandJMSListener

  @Bean
  def bossEventLoopGroup = new io.netty.channel.nio.NioEventLoopGroup

  @Bean
  def workerEventLoopGroup = bossEventLoopGroup

  @Bean
  def navTelecomNettyServer = {
    val nav = new NavTelecomNettyServer
    nav.setPort(50125)
    nav.setStore(new ru.sosgps.wayrecall.packreceiver.DummyPackSaver)
    nav
  }

  @Bean
  def dtmServer = {
    val nav = new DTMServer
    nav.setPort(50126)
    nav.setStore(new ru.sosgps.wayrecall.packreceiver.DummyPackSaver)
    nav
  }


}

