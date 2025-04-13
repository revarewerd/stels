package ru.sosgps.wayrecall.packreceiver

import org.springframework.context.support.{ClassPathXmlApplicationContext, GenericApplicationContext}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
import org.springframework.core.io.ClassPathResource

import scala.collection.JavaConversions.mapAsJavaMap
import org.springframework.beans.factory.config.{BeanFactoryPostProcessor, ConfigurableListableBeanFactory, PropertyOverrideConfigurer}
import java.util.Properties

import com.beust.jcommander.Parameter
import kamon.Kamon
import org.apache.activemq.broker.BrokerService
import org.springframework.context.ApplicationListener
import org.slf4j.LoggerFactory


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 09.12.12
 * Time: 17:19
 * To change this template use File | Settings | File Templates.
 */
object ReceiverServer extends CliCommand with grizzled.slf4j.Logging {

  val commandName = "receiverserver"

  @Parameter(names = Array("-b", "--buffer"))
  var bufferSize: java.lang.Integer = null;

  @Parameter(names = Array("-p", "--pause"))
  var pause: java.lang.Integer = null;

  @Parameter(names = Array("-dummy", "--dummystore"))
  var dummy = false

  def main(args: Array[String]): Unit = process

  def process() = try {

    Kamon.start()


    val context = new ClassPathXmlApplicationContext();

    context.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor {
      def postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        if (dummy)
          beanFactory.getBeanDefinition("packReceiver").getPropertyValues.add("store", new ru.sosgps.wayrecall.packreceiver.DummyPackSaver)
      }
    })

    context.addBeanFactoryPostProcessor({
      val po = new PropertyOverrideConfigurer()
      val properties = new Properties()

      //properties.put("packReceiver.store",new ru.sosgps.wayrecall.packreceiver.DummyPackSaver)

      if (bufferSize != null)
        properties.setProperty("packetsWriter.buffsize", bufferSize.toString)
      if (pause != null)
        properties.setProperty("packetsWriter.insertPause", pause.toString)
      po.setProperties(properties)

      po
    })

    context.setConfigLocation("receiver-spring.xml");
    try {
      context.refresh();
    }
    catch {
      case e: Exception =>
        error("error starting receiverserver", e)
        e.printStackTrace()
        try {
          context.close()
        } finally {
          System.exit(79)
        }
    }
    //context.registerShutdownHook();
    Runtime.getRuntime.addShutdownHook(new Thread("logbackshutdownhook"){
      override def run() = {
        context.close();
        val loggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[{def stop()}]
        loggerContext.stop()
        Kamon.shutdown()
      }
    })



    val bs = context.getBean("broker", classOf[BrokerService])


  } catch {
    case e: Throwable => error("error in intialization:", e); throw e
  }

}
