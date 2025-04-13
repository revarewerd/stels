package ru.sosgps.wayrecall.avlprotocols.ruptela

import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.data.IllegalImeiException
import scala.concurrent.{ExecutionContext, Future}
import scala.beans.BeanProperty
import ru.sosgps.wayrecall.packreceiver.{PackProcessor}
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions.iterableAsScalaIterable
import com.google.common.base.Charsets
import java.io._
import resource._
import ru.sosgps.wayrecall.utils.io.{CRC16CCITT, Utils}
import com.google.common.cache.{CacheLoader, CacheBuilder}
import java.util.concurrent.TimeUnit
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import ru.sosgps.wayrecall.avlprotocols.ruptela.StateMachine.StateEvent
import ru.sosgps.wayrecall.core.GPSData

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 07.06.13
 * Time: 17:42
 * To change this template use File | Settings | File Templates.
 */
class RuptelaPackProcessor(store: PackProcessor)(implicit executor: ExecutionContext) extends grizzled.slf4j.Logging {


  @BeanProperty
  var configsPath: String = null

  /**
   * package indexShift.
   * in wialon for unknown reasons indexes are shifted by 286,
   * but in documentation there is no shift, so by default it is equal to 0
   */
  @BeanProperty
  var fwIndexShift: Int = 0

  @BeanProperty
  var statelistener: StateMachine.Listener = StateMachine.zeroListener

  private[this] val generalListener = new StateMachine.Listener {
    override def apply(v1: StateEvent): Unit = {

      if (v1.name != "initialState") {
        val gps = new GPSData(null, v1.imei, v1.time)
        gps.data.put("ruptelaEventSource", v1.eventSourceName)
        gps.data.put("ruptelaEventName", v1.name)
        gps.data.put("ruptelaEventDataSource", v1.dataSourceName)
        store.addGpsDataAsync(gps)
      }

      statelistener.apply(v1)
    }
  }

  protected[this] val configuratorState = CacheBuilder.newBuilder()
    .expireAfterAccess(2, TimeUnit.HOURS)
    .build(
      new CacheLoader[String, RuptelaConfiguratorState]() {
        def load(imei: String): RuptelaConfiguratorState = {
          new RuptelaConfiguratorState(imei, new DirConfigSource(configsPath, imei,
            Seq("fp3c", "ft3c", "fe3c", "fr3c", "fp4c", "ft4c", "fe4c", "fr4c", "fo4c")
          ), generalListener)
        }
      })

  protected[this] val fwupdaterState = CacheBuilder.newBuilder()
    .expireAfterAccess(2, TimeUnit.HOURS)
    .build(
      new CacheLoader[String, RuptelaFWUpdaterState]() {
        def load(imei: String): RuptelaFWUpdaterState = {
          new RuptelaFWUpdaterState(imei, new DirConfigSource(configsPath, imei,
            Seq("efwt", "efwe", "efwp", "efwr", "efwt4", "efwe4", "efwp4", "efwr4")
          ), generalListener, fwIndexShift)
        }
      })

  def process(pack: RuptelaIncomingPackage): Future[Seq[Array[Byte]]] = {

    val result = new ListBuffer[Future[Array[Byte]]]

    val confstate = configuratorState(pack.imei)
    val fwstate = fwupdaterState(pack.imei)
    pack.commandId match {
      case 1 => {
        result += processGpsData(pack)
      }

      case 2 => {
        result += Future.successful(confstate.tell(pack))
      }

      case 4 => {
        result += Future.successful(fwstate.tell(pack))
      }
    }
    result += Future.successful(confstate.ask)
    result += Future.successful(fwstate.ask)
    Future.sequence(result.reverse)
  }

  private[this] def processGpsData(pack: RuptelaIncomingPackage): Future[Array[Byte]] = {
    try {
      val writingPackages = for (gps <- RuptelaParser.getGpsDatas(pack)) yield {
        trace("writing " + gps)
        if (Math.abs(gps.lat) < 0.001)
          gps.lat = Double.NaN
        if (Math.abs(gps.lon) < 0.001)
          gps.lon = Double.NaN

        store.addGpsDataAsync(gps)
      }
      Future.sequence(writingPackages).map(_ => RuptelaParser.recordACKcommand(true))
    }
    catch {
      case e: IllegalImeiException => {
        warn(e.toString)
        throw new ProtocolExceptionWithResponse(e.toString, RuptelaParser.recordACKcommand(false), e)
      }
    }
  }

  class FileConfigSource(configsPath: String, imei: String, extension: String) extends ConfigSource {

    //debug("RuptelaPackProcessor configsPath=" + configsPath)
    require(configsPath != null, "configsPath is null")
    val file = new File(configsPath + "/" + imei + "." + extension)
    //debug("RuptelaPackProcessor for " + imei + " exists=" + file.exists() + " (" + file.getAbsolutePath + ")")

    override def delete(): Unit = file.delete()

    override def getInputStream: InputStream = new FileInputStream(file)

    override def exists: Boolean = file.exists()

    override def getName: String = imei
  }

  class DirConfigSource(configsPath: String, imei: String, extensions: Seq[String]) extends ConfigSource {

    //debug("RuptelaPackProcessor configsPath=" + configsPath)
    require(configsPath != null, "configsPath is null")
    //    val file = new File(configsPath + "/" + imei + "." + extension)
    //    debug("RuptelaPackProcessor for " + imei + " exists=" + file.exists() + " (" + file.getAbsolutePath + ")")
    val dir = new File(configsPath + "/" + imei)

    private var curFile: Option[File] = None

    private[this] def retriveCurFile = {
      //trace("curFilebefore="+curFile)
      curFile.orElse({
        curFile = getFile
        //trace("curFile="+curFile)
        curFile
      })
      //trace("curFileafter="+curFile)
      curFile
    }

    private[this] def getFile: Option[File] = {
      val files = Option(dir.listFiles()).getOrElse(Array.empty).filter(file => extensions.exists(file.getName.endsWith))
      if (files.size > 1)
        warn("there are more than one " + extensions + " files in " + dir.getAbsolutePath)
      //debug(s"configuration files for imei $imei : ${files.mkString("[", ", ", "]")}")
      files.headOption
    }

    override def delete(): Unit = {
      curFile.foreach(_.delete())
      curFile = None
    }

    override def getInputStream: InputStream = new FileInputStream(retriveCurFile.get)

    override def exists: Boolean = retriveCurFile.isDefined

    override def getName: String = curFile.map(_.getName).getOrElse("None")
  }

}

class ProtocolExceptionWithResponse(message: String, val respData: Array[Byte], cause: Throwable) extends Exception(message, cause)


