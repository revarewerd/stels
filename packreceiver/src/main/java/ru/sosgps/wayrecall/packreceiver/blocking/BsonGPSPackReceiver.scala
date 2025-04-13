package ru.sosgps.wayrecall.packreceiver.blocking

import java.io.{BufferedInputStream, InputStream, OutputStream}
import java.net.Socket
import javax.annotation.{PostConstruct, PreDestroy}

import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.avlprotocols.teltonika.TeltonikaParser
import ru.sosgps.wayrecall.core.GPSDataConversions
import ru.sosgps.wayrecall.data.IllegalImeiException
import ru.sosgps.wayrecall.packreceiver.PackProcessor
import ru.sosgps.wayrecall.utils.ResourceScope._
import ru.sosgps.wayrecall.utils.io.{DboReader, RichDataInput, Utils}
import ru.sosgps.wayrecall.wialonparser.{WialonPackage, WialonParser}

import scala.beans.BeanProperty
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
  * Created by nickl-mac on 28.03.16.
  */
class BsonGPSPackReceiver(
                           @BeanProperty
                           protected var port: Int
                         ) extends SimpleServer with grizzled.slf4j.Logging {


  @Autowired
  @BeanProperty
  var store: PackProcessor = null

  @PostConstruct
  override def start() {
    super.start();
  }

  @PreDestroy
  override def stop() {
    super.stop();
  }

  override protected def serverProcess(accept: Socket): Unit = {

    scope(p => {
      p.manage(accept)
      val incoming = p.manage(new BufferedInputStream(accept.getInputStream(), 1024));
      val outgoing = p.manage(accept.getOutputStream());
      new VirginInputStream(incoming).withVirgin(virgin => {

        val input = p.manage(new DboReader(virgin))


        while (work) {

          val gps = GPSDataConversions.fromDbo(input.read())

          try {
            debug(s"writing $gps")
            Await.result(store.addGpsDataAsync(gps), 20.seconds)
          } catch {
            case e: IllegalImeiException => warn(s"illegai imei ${gps.imei} in $gps")
          }

        }
      })

    })
  }

  def processConnection(in: InputStream, outgoing: OutputStream, firstTime: Boolean): Boolean = {
    throw new UnsupportedOperationException()
  }

}
