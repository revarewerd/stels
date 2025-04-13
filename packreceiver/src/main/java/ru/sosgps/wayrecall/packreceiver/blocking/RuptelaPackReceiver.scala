package ru.sosgps.wayrecall.packreceiver.blocking

import ru.sosgps.wayrecall.data.IllegalImeiException

import scala.beans.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.{PreDestroy, PostConstruct}
import java.io._
import ru.sosgps.wayrecall.wialonparser.{WialonParser, WialonPackage}
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions}
import ru.sosgps.wayrecall.avlprotocols.teltonika.TeltonikaParser
import ru.sosgps.wayrecall.utils.io.{Utils, RichDataInput}
import java.net.Socket
import ru.sosgps.wayrecall.utils.ResourceScope._
import scala.collection.JavaConversions.iterableAsScalaIterable
import resource._
import ru.sosgps.wayrecall.avlprotocols.ruptela.{RuptelaPackProcessor, RuptelaParser}
import ru.sosgps.wayrecall.packreceiver.PackProcessor
import java.text.SimpleDateFormat
import java.util.Date

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.05.13
 * Time: 17:10
 * To change this template use File | Settings | File Templates.
 */
class RuptelaPackReceiver(
                           @BeanProperty
                           protected var port: Int
                           ) extends SimpleServer with grizzled.slf4j.Logging {

  def this(store: RuptelaPackProcessor, port: Int) = {
    this(port);
    this.store = store;
  }


  @BeanProperty
  var store: RuptelaPackProcessor = null

  @PostConstruct
  override def start() {
    super.start();
  }

  @PreDestroy
  override def stop() {
    super.stop();
  }

  override protected def serverProcess(accept: Socket): Unit = {

    for (socket <- managed(accept);
         incoming <- managed(new BufferedInputStream(socket.getInputStream, 1024));
         outgoing <- managed(socket.getOutputStream)) {
      new VirginInputStream(incoming).withVirgin(virgin => {

        val input: RichDataInput = new RichDataInput(virgin)

        while (work) {
          try {
            val incomingPackage = RuptelaParser.parsePackage(input)
            try {
              val results = Await.result(store.process(incomingPackage), 20.seconds)
              results.foreach(outgoing.write)
              outgoing.flush()
            }
            catch {
              case e: IllegalImeiException => {
                warn(e.toString)
                outgoing.flush()
                socket.close()
              }
            }

          } catch {
            case e: UnsupportedOperationException => warn(e.toString, e)
          }
        }
      })

    }
  }

  def processConnection(in: InputStream, outgoing: OutputStream, firstTime: Boolean): Boolean = {

    throw new UnsupportedOperationException()

    //    outgoing.write(0x11);
    //    outgoing.flush();
    //    true
  }
}

