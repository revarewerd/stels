package ru.sosgps.wayrecall.packreceiver.blocking

import ru.sosgps.wayrecall.data.IllegalImeiException

import scala.beans.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.{PreDestroy, PostConstruct}
import java.io.{BufferedInputStream, OutputStream, InputStream}
import ru.sosgps.wayrecall.utils.io.RichDataInput
import java.net.Socket
import scala.collection.JavaConversions.iterableAsScalaIterable
import resource._
import ru.sosgps.wayrecall.avlprotocols.ruptela.RuptelaParser
import ru.sosgps.wayrecall.packreceiver.PackProcessor
import ru.sosgps.wayrecall.avlprotocols.gosafe.{GosafeParseResult, GosafeParser}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}
import ru.sosgps.wayrecall.data.sleepers.{LBSConverter, LBSHttp}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.05.13
 * Time: 17:10
 * To change this template use File | Settings | File Templates.
 */
class GosafePackReceiver(
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

  @Autowired
  @BeanProperty
  var lbsConverter: LBSConverter = null

  @BeanProperty
  var lbsClientPool = new ScalaExecutorService("gosafeclient", 10, 30, true, 1L, TimeUnit.MINUTES, new ArrayBlockingQueue[Runnable](50))

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
            val parseResult = GosafeParser.processPackage(input)
            for (gps <- parseResult.gpsData) {
              try {
                GosafeParser.appendAdditionalData(parseResult, gps)
                trace("writing " + gps)
                Await.result(store.addGpsDataAsync(gps), 20.seconds)
              }
              catch {
                case e: IllegalImeiException => {
                  warn(e.toString)
                }
              }
              outgoing.write(0x01)
            }

            if (parseResult.lbsData.nonEmpty) {
              lbsClientPool.future {
                try {
                  val gpss = GosafeParser.convertLBSToGpsData(lbsConverter, parseResult)
                  for (gps <- gpss) {
                    GosafeParser.appendAdditionalData(parseResult, gps)
                    Await.result(store.addGpsDataAsync(gps), 20.seconds)
                  }
                }
                catch {
                  case e: IllegalImeiException => {
                    warn(e.toString)
                  }
                }
              }
              outgoing.write(0x01)
            }



            outgoing.flush()
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

