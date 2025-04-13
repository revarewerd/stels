package ru.sosgps.wayrecall.retranslators

import java.io.File
import java.net.InetSocketAddress
import java.util.Objects

import org.jboss.netty.util.HashedWheelTimer
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.{DummyPackDataConverter, PackDataConverter}
import ru.sosgps.wayrecall.initialization.{MultiDbManager, MultiserverConfig}
import ru.sosgps.wayrecall.packreceiver.PackProcessor
import ru.sosgps.wayrecall.utils.io.FileChangeListener

import scala.beans.BeanProperty

/**
 * Created by nickl on 01.10.14.
 */
class JsonFileConfigurator(filename: String /*, uidToImei: String => Option[String]*/) extends RetranslationConfigurator with grizzled.slf4j.Logging {

  private[this] var listenienfs = false;

  def configure(r: ConfigurableRetranslator) {
    val srcFile = new File(filename)
    if (srcFile.exists()) {
      import ru.sosgps.wayrecall.utils.typingMapJava
      import ru.sosgps.wayrecall.utils.web.ScalaJson

import scala.collection.JavaConversions.asScalaBuffer

      val confs = ScalaJson.parse[Seq[java.util.Map[String, Any]]](srcFile)

      val config = confs.map(conf => {
        val host = conf.as[String]("host")
        val port = conf.as[Int]("port")
        val forbidRetranslated = conf.getAs[Boolean]("forbidRetranslated").getOrElse(false)
        val acceptableUids: Set[String] = //conf.getAs[java.util.List[String]]("imeis").map(_.toSet).getOrElse(Set.empty) ++
          conf.getAs[java.util.List[String]]("uids").map(_.toSet).getOrElse(Set.empty) //.flatMap(uidToImei(_))
        info("loaded " + acceptableUids.size + " items from " + filename + " acceptableUids:" + acceptableUids.mkString("[", ", ", "]"))
        def canResendIfRetranslated(gps: GPSData): Boolean = {
          !(forbidRetranslated && (gps.data.get("protocol") == null || "Wialon".equals(gps.data.get("protocol"))))
        }
        val acceptor = {
          if (acceptableUids.isEmpty)
            (gps: GPSData) => canResendIfRetranslated(gps)
          else
            (gps: GPSData) =>
              acceptableUids(Objects.requireNonNull(gps.uid, "uid cant be null")) &&
                canResendIfRetranslated(gps)
        }

        (new InetSocketAddress(host, port), acceptor)
      })

      r.configure(config)
    }
    else
      warn("resending config " + srcFile.getAbsolutePath + " does not exists, ignoring")

    if (!listenienfs) {
      listenienfs = true
      FileChangeListener.addFileListener(srcFile, (f, e) => {
        configure(r)
      })
    }

  }
}



import javax.annotation.PostConstruct

class MultiRetranslatorConfigurator {

  @Autowired
  var packProcessor: PackProcessor = null

  @Autowired
  var micfg: MultiserverConfig = null

  @Autowired
  var multiDb: MultiDbManager = null

  @Autowired
  var appcontext: MultiserverConfig = null

  @Autowired
  var timer: HashedWheelTimer = new HashedWheelTimer

  @Autowired
  @BeanProperty
  var packConv: PackDataConverter = new DummyPackDataConverter

  @PostConstruct
  def subscribe() {

    packProcessor.gpsdataListeners = packProcessor.gpsdataListeners ++ micfg.instances.flatMap{ icfg =>

      val wialon = new WialonRealTimeRetranslator(
        new JsonFileConfigurator(
          icfg.path.resolve("retranslators").resolve("retranslator.json").toString //,
         )
      )
      wialon.timer = timer
      wialon.packConv = packConv

      val nis = new NisRealtimeRetranslator(new JsonFileConfigurator(
        icfg.path.resolve("retranslators").resolve("nisretranslator.json").toString //,
      ))
      Seq(wialon, nis)
    }
  }
}
