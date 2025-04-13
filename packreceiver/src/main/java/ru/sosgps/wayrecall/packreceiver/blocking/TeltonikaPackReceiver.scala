package ru.sosgps.wayrecall.packreceiver.blocking

import scala.beans.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.{PreDestroy, PostConstruct}
import java.io.{BufferedInputStream, DataInputStream, OutputStream, InputStream}
import ru.sosgps.wayrecall.wialonparser.{WialonParser, WialonPackage}
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions}
import ru.sosgps.wayrecall.avlprotocols.teltonika.TeltonikaParser
import ru.sosgps.wayrecall.utils.io.{Utils, RichDataInput}
import java.net.Socket
import ru.sosgps.wayrecall.utils.ResourceScope._
import scala.collection.JavaConversions.iterableAsScalaIterable
import ru.sosgps.wayrecall.packreceiver.PackProcessor

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 09.03.13
 * Time: 19:20
 * To change this template use File | Settings | File Templates.
 */
class TeltonikaPackReceiver(
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

  override protected def serverProcess(accept: Socket): Unit = {

    scope(p => {
      p.manage(accept)
      val incoming = p.manage(new BufferedInputStream(accept.getInputStream(), 1024));
      val outgoing = p.manage(accept.getOutputStream());
      new VirginInputStream(incoming).withVirgin(virgin => {

        val input: RichDataInput = new RichDataInput(virgin)

        val imei = TeltonikaParser.readIMEI(input)
        trace("incoming imei =" + imei)
        outgoing.write(0x01)
        outgoing.flush()

        while (work) {

          val size: Int = TeltonikaParser.readPrefix(input)
          trace("incoming pack size =" + size)
          val avlDataArray = input.readNumberOfBytes(size)
          debug("avlDataArray = " + Utils.toHexString(avlDataArray, " "))
          val gpsDatas = TeltonikaParser.readAVLDataArray(new RichDataInput(avlDataArray), imei)
          val CRC: Int = TeltonikaParser.readCRC(input)
          debug("CRC = " + CRC)

          for (gps <- gpsDatas) {
            Await.result(store.addGpsDataAsync(gps), 20.seconds)
          }

          outgoing.write(gpsDatas.size())
          outgoing.flush()

        }
      })

    })
  }

  def processConnection(in: InputStream, outgoing: OutputStream, firstTime: Boolean): Boolean = {

    throw new UnsupportedOperationException()

    //    outgoing.write(0x11);
    //    outgoing.flush();
    //    true
  }
}
