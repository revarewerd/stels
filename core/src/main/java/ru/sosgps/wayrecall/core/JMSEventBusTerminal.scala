package ru.sosgps.wayrecall.core

import java.util.Objects.requireNonNull
import java.util.{Objects, UUID}
import javax.annotation.PostConstruct
import javax.jms._

import com.mongodb.DBObject
import org.axonframework.domain.EventMessage
import org.axonframework.eventhandling.{Cluster, EventBusTerminal}
import org.axonframework.serializer._
import org.axonframework.serializer.bson.DBObjectXStreamSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.jms.core.{JmsTemplate, MessageCreator}
import org.springframework.jms.listener.DefaultMessageListenerContainer

import scala.beans.BeanProperty
import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer

class JMSEventBusTerminal(val localPublish: Boolean) extends EventBusTerminal with grizzled.slf4j.Logging /*with InitializingBean with ApplicationContextAware*/ {

  def this() = this(false)

  protected[this] val clusters = new ListBuffer[Cluster]

  @Autowired(required = false)
  var jmsTemplate: JmsTemplate = null

  @Autowired
  var jmsFactory: TopicConnectionFactory = null

  val uuid = UUID.randomUUID()

  val serializer = new DBObjectXStreamSerializer()

  val dummySerializedType = new SerializedType {
    def getName = ???

    def getRevision = ???
  }

  private[this] val container = new DefaultMessageListenerContainer()

  @BeanProperty
  var topicPrefix: String = null

  def fullTopicName = topicPrefix + ".axonevents"

  @PostConstruct
  def init() {
    require(topicPrefix != null, "topicPrefix must be set")
    debug(s"JMSEventBusTerminal created topicPrefix=$topicPrefix localPublish=$localPublish")
    require(jmsTemplate != null || localPublish, "jmsTemplate must be set or localPublish flag must be set")
    container.setConnectionFactory(jmsFactory)
    container.setCacheLevel(DefaultMessageListenerContainer.CACHE_AUTO)
    container.setMessageListener(new MessageListener {
      def onMessage(message: Message) = {
        message match {
          case m: ObjectMessage => {
            val obj = m.getObject
            debug(topicPrefix + " received: body=" + obj)
            val objMap = obj.asInstanceOf[Map[String, AnyRef]]
            val dBObjects = objMap("eventsList").asInstanceOf[Seq[DBObject]]
            val receivedUUID = objMap("senderUUID").asInstanceOf[UUID]

            if(receivedUUID != uuid)
            publishLocally(for (dbo <- dBObjects) yield {
              val deserialize = serializer.deserialize[DBObject, EventMessage[_]](new SimpleSerializedObject[DBObject](dbo, classOf[DBObject], dummySerializedType))
              //debug("deserialized=" + deserialize)
              deserialize
            })
            else
              debug(topicPrefix +" received self sent messages:" + dBObjects)

          }
          case _ => debug(topicPrefix +" received: body=" + " not object")
        }
      }
    });
    container.setDestinationName(fullTopicName);
    container.setPubSubDomain(true)
    container.initialize()
    container.start()

  }

  override def onClusterCreated(cluster: Cluster) = synchronized(clusters += cluster)

  override def publish(events: EventMessage[_]*) = {     
    publishLocally(events)
    if (!localPublish)
    publishRemote(events: _ *)
  }

  def publishRemote(events: EventMessage[_]*) {
    try {
      requireNonNull(jmsTemplate, "jmsTemplate").send(fullTopicName, new MessageCreator {
        def createMessage(session: Session): Message = {

          val list = events.map(e => {
            //MessageSerializer.serializePayload(e, serializer, classOf[DBObject]).getData
            serializer.serialize(e, classOf[DBObject]).getData
          }).toVector
          val r = session.createObjectMessage(HashMap("eventsList" -> list, "senderUUID" -> uuid))
          debug(topicPrefix +" sending message " + r)
          r
        }
      })
    } catch {
      case e: Exception => {
        warn(topicPrefix +" error in jms sending:", e)
      }
    }
  }

  protected[this] def publishLocally(events: Seq[EventMessage[_]]) {
    debug(topicPrefix +" publishing locally: " + events.mkString("[", ",", "]"))
    clusters.foreach(_.publish(events: _*))
  }
}
