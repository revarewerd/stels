package ru.sosgps.wayrecall.packreceiver

import java.util.Date

import com.mongodb.casbah.commons.MongoDBObject
import java.util.concurrent.{ArrayBlockingQueue, SynchronousQueue, ThreadPoolExecutor, TimeUnit}

import org.springframework.beans.factory.annotation.{Autowired, Value}
import javax.annotation.{PostConstruct, PreDestroy}

import ru.sosgps.wayrecall.core.{GPSData, GPSUtils}
import ru.sosgps.wayrecall.data.{DBPacketsWriter, IllegalImeiException}
import org.springframework.jms.core.{JmsTemplate, MessageCreator}
import javax.jms.{Message, Session}

import kamon.Kamon
import ru.sosgps.wayrecall.initialization.MultiDbManager
import ru.sosgps.wayrecall.utils.errors.{HeapDumper, MailErrorReporter}
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.concurrent.FuturesUtils.extensions

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import ru.sosgps.wayrecall.utils.concurrent.{FuturesUtils, OrderedScalaExecutorService, ScalaExecutorService}

import scala.beans.BeanProperty
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Future, promise}
import scala.util.{Failure, Success}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 09.12.12
 * Time: 15:57
 * To change this template use File | Settings | File Templates.
 */

class PackProcessor extends grizzled.slf4j.Logging {

  protected[this] val fl = new PerMinuteLogger

  var gpsdataListeners: scala.collection.immutable.Seq[(GPSData) => Unit] = scala.collection.immutable.Seq.empty

  def setListeners(ls: java.util.List[(GPSData) => Unit]) = gpsdataListeners = ls.asScala.toList

  type Preprocessor = (GPSData) => Future[GPSData]

  var gpsdataPreprocessors: scala.collection.immutable.Seq[Preprocessor] = scala.collection.immutable.Seq.empty

  def setPreprocessors(ls: java.util.List[Preprocessor]) = gpsdataPreprocessors = ls.asScala.toList

  @Autowired(required = false)
  var mmdbm: MultiDbManager = null

  @Autowired(required = false)
  var mail: MailSender = null

  @BeanProperty
  @Value("${packreceiver.preprocessorTimeout:12000}")
  var preprocessorTimeout: Long = 12000

  @BeanProperty
  @Value("${packreceiver.insertionTimeout:24000}")
  var insertionTimeout: Long = 24000

  lazy val errorReporter = new MailErrorReporter(mail, "errors@ksb-stels.ru")

  lazy val heapDumper = new HeapDumper

  var inserterPool = new ScalaExecutorService("PackProcessorInserterPool", 4, 8, true, 10L, TimeUnit.MINUTES, new ArrayBlockingQueue[Runnable](10000), new ThreadPoolExecutor.AbortPolicy)

  private[this] val gpsdataListenersExecutor = new ScalaExecutorService("gpsdataListenersExecutor", 1, 1, true, 30, TimeUnit.SECONDS,
    new ArrayBlockingQueue[Runnable](10000),
    new ThreadPoolExecutor.AbortPolicy)

  private val orderedExecutor = new OrderedScalaExecutorService[String](ScalaExecutorService.globalService)

  protected def chainPreprocessors(gps: GPSData): Future[GPSData] = {
    implicit val ctxt = orderedExecutor.contextBoundToKey(gps.imei)
    val foldedPreprocessor = gpsdataPreprocessors.fold(
      (gps: GPSData) => Future.successful(gps))((p1, p2) => p1.andThen(_.flatMap(p2))
      )

    foldedPreprocessor(gps).withTimeout(preprocessorTimeout.millisecond)(ScalaExecutorService.implicitSameThreadContext)
  }

  protected val incomingLogger = new IncomingGPSLogger()

  @PostConstruct
  protected def start(): Unit ={
    incomingLogger.start()
    //heapDumper.dumpHeap();
    //errorReporter.notifyError("started")
  }


  @PreDestroy
  protected def predestroy(): Unit ={
    incomingLogger.stop()
  }

  @throws(classOf[IllegalImeiException])
  final def addGpsDataAsync(gpsdata: GPSData): Future[GPSData] = {
    if (!GPSUtils.valid(gpsdata))
      return Future.failed(new IllegalArgumentException("invalid gps data:" + gpsdata))
    gpsdata.insertTime = calcInsertTime()
    Kamon.metrics.counter("device-message", Map("protocol" -> gpsdata.data.getOrDefault("protocol", "unknown").toString)).increment()
    val kontext = Kamon.tracer.newContext("addGpsData")

    WeakTracer.trace(gpsdata, "addGpsDataAsync")
    incomingLogger(gpsdata)
    WeakTracer.trace(gpsdata, "sent to incomingLogger")
    val startTime = System.currentTimeMillis()

    //    val unchained = gpsdata.clone()
    //    unchained.data.put("unchained", true.asInstanceOf[AnyRef])
    //    val unchainedFuture = save(unchained)
    //implicit val ctxt = inserterPool.executionContext
    val res = chainPreprocessors(gpsdata).flatMap(save)(inserterPool.executionContext).withTimeout(insertionTimeout.millisecond)(ScalaExecutorService.implicitSameThreadContext)
    res.onComplete({
      case e => {
        WeakTracer.trace(gpsdata, "res completed "+e)
        val tookTime = System.currentTimeMillis() - startTime;
        kontext.finish()
        if (tookTime > 40000)
          debug("addGpsDataAsync took " + tookTime + " for " + gpsdata.imei + " e=" + e + WeakTracer.getHistory(gpsdata).mkString("\n    ", "\n    ", "\n"))
        e.failed.toOption.filter(e => !e.isInstanceOf[IllegalImeiException]).foreach(e => {
          warn("exception in addGpsDataAsync for "+gpsdata,e)
          //if(e.isInstanceOf[java.util.concurrent.TimeoutException]){
          val errorString = "addGpsDataAsync took " + tookTime + " for " + gpsdata.imei +
            " e=" + e + "\n" + errors.stacktrace(e) + "\n gps=" + gpsdata +
            "gps item history:" + WeakTracer.getHistory(gpsdata).mkString("\n    ", "\n    ", "\n")
          errorReporter.notifyError(errorString.replaceAll("\n","<br/>"))
          //heapDumper.dumpHeap();
          //}
        })
      }
    })(ScalaExecutorService.implicitSameThreadContext)
//    unchainedFuture.onComplete({
//      case e => {
//        val tookTime = System.currentTimeMillis() - startTime;
//        WeakTracer.trace(gpsdata, "unchained completed")
//        if (tookTime > 1000)
//          debug("addGpsDataAsync unchained took " + tookTime + " for " + gpsdata.imei + " e=" + e)
//      }
//    })(ScalaExecutorService.implicitSameThreadContext)
//    unchainedFuture
    res
  }

  protected def calcInsertTime(): Date =  new Date()

  protected def save(gpsdata: GPSData): Future[GPSData] = {
    inserterPool.future {
      presave(gpsdata)
      if (isDebugEnabled)
        fl.logPerMinute { (minutePackCount, mis) => debug("store packspersecond:" + minutePackCount / 60 + " mis=" + mis)}
      gpsdata
    }
  }

  protected def valid(data: GPSData): Boolean = data.time.getTime < System.currentTimeMillis() + 1000 * 60 * 30

  private def presave(gpsdata: GPSData): Unit = {
    WeakTracer.trace(gpsdata, "presaving")
    require(gpsdata.imei != null, "imei must not be null")
    //require(gpsdata.uid != null, "uid must not be null")
    if (!valid(gpsdata)) {
      warn("invalid gpsdata:" + gpsdata)
      return
    }

    if (!markedAsDuplicate(gpsdata) && packetsWriter.addToDb(gpsdata: GPSData)) {
      WeakTracer.trace(gpsdata, "added to db so notifing")
      publishJMS(instanceName(gpsdata) + ".receiver.newmessage", gpsdata)

      if(!gpsdata.unchained()) {
        try {
          gpsdataListeners.foreach(f => gpsdataListenersExecutor.execute(runnable {
            try {
              f(gpsdata)
            } catch {
              case e: Throwable => error("error in listener ", e)
            }
          }))
          WeakTracer.trace(gpsdata, "gpsdataListeners notified")
        }
        catch {
          case e: Throwable => error("error in listeners starting ", e)
        }
      }

    }


  }

  private def markedAsDuplicate(gpsdata: GPSData): Boolean = {
    java.lang.Boolean.TRUE.equals(gpsdata.data.get("duplicate"))
  }

  protected def instanceName(gpsdata: GPSData): String = {
    mmdbm.dbByIMEI(gpsdata.imei).instance.get.name
  }

  @Autowired(required = false)
  var jmsTemplate: JmsTemplate = null

  protected[this] def publishJMS(topic: String, gpsdata: java.io.Serializable) {
    //debug("sending to "+topic+" "+gpsdata)
    try {
      jmsTemplate.send(topic, new MessageCreator {
        def createMessage(session: Session): Message = {
          val r = session.createObjectMessage(gpsdata)
          //debug("sending message " + r)
          r
        }
      })
    }
    catch {
      case e => error("error in gpsdata retranslation ", e)
    }
  }


  @Autowired(required = false)
  var packetsWriter: DBPacketsWriter = null

}


class DummyPackSaver extends PackProcessor with grizzled.slf4j.Logging {
  override protected def save(gpsdata: GPSData): Future[GPSData] = {
    debug("received:" + gpsdata)
    if (isDebugEnabled)
      fl.logPerMinute { (minutePackCount, mis) => debug("store packspersecond:" + minutePackCount / 60 + " mis=" + mis)}
    Future.successful(gpsdata)
  }
}

