package ru.sosgps.wayrecall.core

import java.nio.file.Path
import java.util.Properties


import scala.io.Source

/**
 * Created by nickl on 18.05.15.
 */
class InstanceConfig(val path: Path/*, val gloabalProps: Properties*/) extends grizzled.slf4j.Logging {

  def isDefault: Boolean = name == "default" //  insconf.endsWith("default")

  val name = path.getFileName.toString

  val properties: Properties = ru.sosgps.wayrecall.utils.loadProperties(path.resolve("instance.properties"))

//  //TODO: вынести это отсюда куда-нибудь
//  def configureContext(): AbstractRefreshableConfigApplicationContext = {
//    val resolve: Path = path.resolve("spring-main.xml")
//
//    val mainContext: AbstractXmlApplicationContext = if (Files.exists(resolve)) {
//      val mainContext = new FileSystemXmlApplicationContext
//      mainContext.setConfigLocation(resolve.toAbsolutePath.toUri.toString)
//      mainContext
//    }
//    else {
//      val mainContext = new ClassPathXmlApplicationContext
//      mainContext.setConfigLocation("/shared-beans-config.xml")
//      mainContext
//    }
//
//    val environment: StandardEnvironment = new StandardEnvironment {
//      protected override def customizePropertySources(propertySources: MutablePropertySources) {
//        super.customizePropertySources(propertySources)
//        propertySources.addLast(new PropertiesPropertySource("instance", properties))
//        propertySources.addLast(new PropertiesPropertySource("global", gloabalProps))
//        val generatedProps = new Properties()
//        generatedProps.setProperty("instance.name", name)
//        propertySources.addLast(new PropertiesPropertySource("generated", generatedProps))
//      }
//    }
//
//    mainContext.setEnvironment(environment)
//    //mainContext.addBeanFactoryPostProcessor(new )
//    mainContext.addBeanFactoryPostProcessor(new BeanDefinitionRegistryPostProcessor {
//      override def postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry): Unit = {
//
//      }
//
//      override def postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory): Unit =
//        if (!beanFactory.containsBean("instanceConfig")) {
//          debug("beanFactory.registerSingleton(\"instanceConfig\", this)")
//          beanFactory.registerSingleton("instanceConfig", InstanceConfig.this)
//        }
//
//    })
//  // mainContext.getBeanFactory.registerSingleton("instanceConfig", this)
//    mainContext.refresh
//    mainContext
//  }

  val vhosts: Array[String] = Source.fromFile(path.resolve("vhosts").toFile, "UTF8").getLines().toArray

  val dbconf =  DbConf(
    properties.getProperty("instance.defaultmongodb.databaseName"),
    properties.getProperty("instance.defaultmongodb.servers",
      properties.getProperty("instance.defaultmongodb.host")
    ).split(",").map(_.trim).toList,
    Option(properties.getProperty("instance.defaultmongodb.waitTime")).map(_.toInt).getOrElse(10000),
    Option(properties.getProperty("instance.defaultmongodb.connectionsPerHost")).map(_.toInt).getOrElse(32)
  )

}

case class DbConf(name: String, servers: List[String] = List("127.0.0.1"), waitTime: Int = 10000, connectionsPerHost: Int = 32)

