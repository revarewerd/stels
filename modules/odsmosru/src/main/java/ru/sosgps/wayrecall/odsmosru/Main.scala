package ru.sosgps.wayrecall.odsmosru

import java.io._
import java.nio.file.{Path, Paths}
import java.util

import org.springframework.beans.factory.support.{BeanDefinitionRegistry, BeanDefinitionRegistryPostProcessor}
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.beans.factory.config.{BeanFactoryPostProcessor, ConfigurableListableBeanFactory, PropertyOverrideConfigurer}
import java.util.{Collections, Date, Properties}

import org.springframework.core.env.{MutablePropertySources, PropertiesPropertySource, StandardEnvironment}
import ru.sosgps.wayrecall.initialization.MultiserverConfig
import ru.sosgps.wayrecall.jcommanderutils.{CliCommand, MultiCommandCli}
import com.beust.jcommander.{Parameter, Parameters}
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.utils.attempts.{Attempts, Retry}
import ru.sosgps.wayrecall.utils.io.{DboReader, DboWriter}
import ru.sosgps.wayrecall.utils.parseDate
import com.mongodb.casbah.Imports._
import kamon.Kamon

import scala.collection.JavaConversions.asScalaBuffer
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions, ObjectsRepositoryReader}

import scala.collection.mutable


object Main extends MultiCommandCli {

  val commands = Seq[CliCommand](
    RealTimeResender, IntervalResender
  )

}


abstract class AbstractOdsCommand extends CliCommand {

  @Parameter(names = Array("--instance"), description = "instance name")
  var instanceName: String = "default"


  protected def bindToInstanceData(context: ClassPathXmlApplicationContext) {
    val confPath: Path = Paths.get(System.getenv("WAYRECALL_HOME"), "conf")

    val config = new MultiserverConfig(confPath)
    val instanceConf = config.instances.find(_.name == instanceName).getOrElse(throw new IllegalArgumentException("no instance " + instanceName))

    val environment: StandardEnvironment = new StandardEnvironment {
      protected override def customizePropertySources(propertySources: MutablePropertySources) {
        super.customizePropertySources(propertySources)
        propertySources.addLast(new PropertiesPropertySource("instance", instanceConf.properties))
        propertySources.addLast(new PropertiesPropertySource("global", config.gloabalProps))
        val generatedProps = new Properties()
        generatedProps.setProperty("instance.name", instanceConf.name)
        propertySources.addLast(new PropertiesPropertySource("generated", generatedProps))
      }
    }

    context.addBeanFactoryPostProcessor(new BeanDefinitionRegistryPostProcessor {
      def postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
      }

      def postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        if (!beanFactory.containsBean("instanceConfig")) {
          beanFactory.registerSingleton("instanceConfig", instanceConf)
        }
      }
    }
    )

    context.setEnvironment(environment)
  }
}

@Parameters(commandDescription = "Resend to ods mos ru all packages set in odsmosruimeis.txt")
object RealTimeResender extends AbstractOdsCommand with grizzled.slf4j.Logging {

  val commandName = "realtime"

  def process() = try{

    Kamon.start()

    val context = new ClassPathXmlApplicationContext();
    bindToInstanceData(context)

    context.setConfigLocation("odsmosru-realtime.xml");
    context.refresh();
    context.registerShutdownHook();
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      override def run(): Unit = Kamon.shutdown()
    }))
  } catch {
    case e: Exception =>
      error("intitialization error", e)
      e.printStackTrace()
  }

}

@Parameters(commandDescription = "loads from db and resends data with specified imei and time intrtval")
object IntervalResender extends AbstractOdsCommand {
  val commandName = "send"

  @Parameter(names = Array("-f", "--from"), description = "from date (format 2013-08-28T00:00)")
  var from: String = null

  @Parameter(names = Array("-t", "--to"), description = "to date (format 2013-08-28T00:00)")
  var to: String = null

  @Parameter(names = Array("-u", "--uid"), description = "object uid")
  var uids: java.util.List[String] = Collections.emptyList()

  @Parameter(names = Array("--file"), description = "bson gps dump")
  var files: java.util.List[String] = null

  @Parameter(names = Array("-i", "--imei"), description = "object imei")
  var imeis: java.util.List[String] = Collections.emptyList()

  @Parameter(names = Array("--set-imei"), description = "send with another imei", required = false)
  var forceIMEI: String = null

  def process() = {
    val context = new ClassPathXmlApplicationContext();
    bindToInstanceData(context)
    context.setConfigLocation("odsmosru-sender-spring.xml");
    context.refresh();
    context.registerShutdownHook();

    val resender = context.getBean(classOf[ODSMosruSender])

    val (total, datas: Iterator[GPSData]) = if(files != null) {
      val flatten: Iterator[GPSData] = files.toIterator.map(f => new DboReader(f).iterator.map(GPSDataConversions.fromDbo)).flatten
      (-1, flatten)
    }
    else dataFromDb(context)

    val attempts = new Attempts(maxRetryAttempts = 20)
    attempts.handler = {
      case e: Exception => {
        e.printStackTrace();
        val interval = 1000 * attempts.retryAttemt * attempts.retryAttemt * attempts.retryAttemt
        log("waiting for:" + interval + " and retry")
        Thread.sleep(interval)
        Retry
      }
    }
    val forcedImei = Option(forceIMEI)
    for ((x, i) <- datas.zipWithIndex) {
      log("sending " + (i + 1) + "/" + total)
      forcedImei.foreach(imei => x.imei = imei)
      attempts.doTry(resender.sendToOdsMosRu(x))
    }

    context.close()
    System.exit(0)
  }

  def dataFromDb(context: ClassPathXmlApplicationContext): (Int, Iterator[GPSData]) = {
    val or = context.getBean(classOf[ObjectsRepositoryReader])
    val packagesStore = context.getBean(classOf[PackagesStore])
    val uidall = uids ++ imeis.map(imei => or.getObjectByIMEI(imei)).filter(null !=).map(_.as[String]("uid"))

    require(uidall.nonEmpty, "uids or imeis must be set")
    require(from != null, "from must be set")
    require(to != null, "to must be set")

    def histories = {
      uidall.toStream.map(uid => packagesStore.getHistoryFor(uid, parseDate(from), parseDate(to)))
    }

    val total = histories.iterator.map(_.total).sum

    val datas = histories.iterator.map(_.iterator).flatten

    val result = cacheOnDisk(datas)
    (total, result)
  }

  def cacheOnDisk(datas: Iterator[GPSData]): Iterator[GPSData] = {
    val tempFile = File.createTempFile("cached", "dbos")
    log("file " + tempFile.getAbsolutePath + " created")
    tempFile.deleteOnExit()
    val dbowrr = new DboWriter(new BufferedOutputStream(new FileOutputStream(tempFile)))
    datas.foreach(gps => dbowrr.write(GPSDataConversions.toMongoDbObject(gps)))
    dbowrr.close()
    log("file " + tempFile.getAbsolutePath + " writed " + tempFile.length() + " bytes")
    val reader = new DboReader(new BufferedInputStream(new FileInputStream(tempFile)))
    val ri = reader.iterator.map(GPSDataConversions.fromDbo)
    new Iterator[GPSData] {
      override def next(): GPSData = ri.next()

      override def hasNext: Boolean = {
        val hn = ri.hasNext
        if (!hn) {
          tempFile.delete()
          log(tempFile.getAbsolutePath + " deleted")
        }
        hn
      }
    }
  }

  def log(x: String) = {
    println(new Date() + " " + x);
  }
}
