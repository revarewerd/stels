/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.packreceiver.blocking

import java.io._
import ru.sosgps.wayrecall.wialonparser.{WialonPackage, WialonParser}
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{GPSDataConversions, GPSData}
import scala.beans.BeanProperty
import ru.sosgps.wayrecall.packreceiver.PackProcessor

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

//@Component
//@Primary
class WialonPackReceiver(
                          @BeanProperty
                          protected var port: Int
                          ) extends SimpleServer with grizzled.slf4j.Logging {


  def this(store: PackProcessor, port: Int) = {
    this(port);
    this.store = store;
  }


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


  def processConnection(in: InputStream, outgoing: OutputStream, firstTime: Boolean): Boolean = {
    val pack: WialonPackage = WialonParser.parsePackage(in);
    //println("reading " + pack.imei)

    Await.result(store.addGpsDataAsync(GPSDataConversions.fromWialonPackage(null, pack)), 20.seconds)

    outgoing.write(0x11);
    outgoing.flush();
    true
  }
}


