package ru.sosgps.wayrecall.packreceiver

import java.util.Date
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

import com.google.common.cache.{CacheBuilder, LoadingCache}
import com.google.common.util.concurrent.UncheckedExecutionException
import org.apache.poi.ss.formula.functions.T
import org.axonframework.eventhandling.annotation.AnnotationEventListenerBeanPostProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.{AutowireCapableBeanFactory, ConfigurableBeanFactory}
import org.springframework.context.ApplicationContext
import ru.sosgps.wayrecall.core.{GPSData, MongoDBManager, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.data._
import ru.sosgps.wayrecall.initialization.MultiDbManager
import ru.sosgps.wayrecall.utils.{LimitsSortsFilters, funcLoadingCache}

import scala.concurrent.Future

/**
 * Created by nickl on 21.07.14.
 */
class DbMappingPacketsWriter extends DBPacketsWriter with grizzled.slf4j.Logging {

  @Autowired
  var mmdbm: MultiDbManager = null;

  @Autowired
  var appcontext: ApplicationContext = null;

  private var dbtowritermap: Map[MongoDBManager, BufferedMongoDBPacketsWriter] = null

  @PostConstruct
  def init {
    val factory = appcontext.getAutowireCapableBeanFactory
    def property(name: String): Option[String] = {
      scala.util.control.Exception.allCatch.opt(
        factory.asInstanceOf[ConfigurableBeanFactory].resolveEmbeddedValue("${" + name + "}")
      )
    }
    dbtowritermap = mmdbm.map(mdmb => {

      val writer = new BufferedMongoDBPacketsWriter
      writer.mongoDbManager = mdmb
      val repository = new ObjectsRepositoryReader
      repository.mdbm = mdmb

      val listenersProcessor = appcontext.getBean(classOf[AnnotationEventListenerBeanPostProcessor])

      writer.or = listenersProcessor.postProcessAfterInitialization(repository, mdmb.databaseName+"Repostitory")
        .asInstanceOf[ObjectsRepositoryReader]

      property("packreceiver.buffersize").foreach(p => writer.bufferSize = p.toInt)
      property("packreceiver.transferQps").foreach(p => writer.transferQps = p.toDouble)
      property("packreceiver.transferfsync").foreach(p => writer.fsyncBufferTransfer = p.toBoolean)

      //factory.autowireBeanProperties(writer, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false)
      //factory.autowireBean(writer)
      factory.initializeBean(writer, null)
      debug(s"buffersize:${writer.bufferSize} transferQps:${writer.transferQps}")
      (mdmb, writer)
    }).toMap
  }

  private val clusters: LoadingCache[String, BufferedMongoDBPacketsWriter] = CacheBuilder.newBuilder()
    .expireAfterWrite(3, TimeUnit.MINUTES)
    .buildWithFunction((imei: String) => {
    val r = dbtowritermap(mmdbm.dbByIMEI(imei))
    //debug("imei=" + imei + " mapped to " + r.mongoDbManager.getDatabaseName)
    r
  })

  @throws(classOf[IllegalImeiException])
  override def addToDb(gpsdata: GPSData): Boolean = try {
    clusters(gpsdata.imei).addToDb(gpsdata)
  } catch {
    case e: UncheckedExecutionException => throw e.getCause
  }

  override def updateGeogata(gpsdata: GPSData, placeName: String): Unit = try {
    clusters(gpsdata.imei).updateGeogata(gpsdata, placeName)
  } catch {
    case e: UncheckedExecutionException => throw e.getCause
  }
}

class DbMappingPacketsReader extends grizzled.slf4j.Logging with ClusteredPacketsReader {

  import com.mongodb.casbah.Imports._

  @Autowired
  var mmdbm: MultiDbManager = null;

  @Autowired
  var appcontext: ApplicationContext = null;

  @Autowired
  var cnv: PackDataConverter = null

  protected var dbtowritermap: Map[MongoDBManager, (PackagesStore, ObjectsRepositoryReader)] = null

  @PostConstruct
  def init {
    val factory = appcontext.getAutowireCapableBeanFactory
    dbtowritermap = mmdbm.map(mdmb => {
      val writer = new MongoPackagesStore
      writer.pconv = cnv
      writer.mongoDbManager = mdmb
      val repository = new ObjectsRepositoryReader
      repository.mdbm = mdmb
      writer.or = repository
      //factory.autowireBeanProperties(writer, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false)
      //factory.autowireBean(writer)
      factory.initializeBean(writer, null)
      (mdmb, (writer, repository))
    }).toMap
  }

  private val clusters: LoadingCache[String, PackagesStore] = CacheBuilder.newBuilder()
    .expireAfterWrite(3, TimeUnit.MINUTES)
    .buildWithFunction((imei: String) => {
    val r = dbtowritermap(mmdbm.dbByIMEI(imei))._1
    //debug("imei=" + imei + " mapped to " + r.mongoDbManager.getDatabaseName)
    r
  })

  @throws(classOf[IllegalImeiException])
  override def getStoreByImei(imei: String): PackagesStore = try {
    clusters(imei)
  } catch {
    case e: com.google.common.util.concurrent.UncheckedExecutionException if e.getCause != null => throw e.getCause
  }

  override def uidByImei(imei: String): Option[String] = uidByImeiCache(imei)

  //TODO: refactor this
  @deprecated("this is ugly hack must bee removed soon")
  override def futureOnImei[T](imei: String)(f: => T): Future[T] = mmdbm.dbByIMEI(imei).future(f)

  private val uidByImeiCache: LoadingCache[String, Option[String]] = CacheBuilder.newBuilder()
    .expireAfterWrite(3, TimeUnit.MINUTES)
    .buildWithFunction((imei: String) => {
    val r = dbtowritermap(mmdbm.dbByIMEI(imei))._2
    //debug("imei=" + imei + " mapped to " + r.mongoDbManager.getDatabaseName)
    Option(r.getObjectByIMEI(imei)).map(_.as[String]("uid"))
  })

}
