package ru.sosgps.wayrecall.packreceiver

import java.io.{FileOutputStream, BufferedOutputStream, File}
import java.text.SimpleDateFormat
import java.util
import java.util.Date
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy
import java.util.concurrent.{ThreadPoolExecutor, LinkedBlockingDeque, TimeUnit}
import javax.annotation.{PreDestroy, PostConstruct}
import com.mongodb.DBObject
import com.mongodb.casbah.Imports
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import ru.sosgps.wayrecall.core.{GPSDataConversions, GPSData}
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.concurrent.BlockingActor
import ru.sosgps.wayrecall.utils.io.DboWriter

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future


/**
 * Created by nickl on 27.12.14.
 */
class IncomingGPSLogger(val dir:File ) extends ((GPSData) => Future[GPSData]) with grizzled.slf4j.Logging with BlockingActor[DBObject] {

  def this() = this(new File("inclog"))

//  private val pool = new ScalaExecutorService("IncomingGPSLogger", 1 ,1 , false, 1L, TimeUnit.HOURS,
//    new LinkedBlockingDeque[Runnable](1000), new DiscardPolicy)


  override def apply(v1: GPSData): Future[GPSData] = {

    try {
      val dbo = GPSDataConversions.toMongoDbObject(v1)
      if(!accept(dbo)){
        error("cant enque:"+ this.qsize + " it is FATAL ERROR shutting down to prevent data loss")
        this.stop()
        System.exit(4)
      }
    }
    catch {
      case e: Exception => warn("apply exception:", e)
    }

    Future.successful(v1)
  }



  dir.mkdirs()

  private[this] def makeNewWriter() ={
    val format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS")
    val file = new File(dir, "inclog"+format.format(new Date())+".xz")
    debug("making new writer: "+file.getAbsolutePath)
    new DboWriter(new XZCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(file)),0))
    //new DboWriter(new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(file))))
  }

  var limit = 500000;

  private var written: Long = 0;
  private var writer = makeNewWriter()

  val buffer = new ArrayBuffer[DBObject]()

  protected val actorThreadName = "IncomingGPSLogger"

  protected def processMessage(head: DBObject) {
    buffer += head
    drainTo(buffer)
    writer.write(buffer: _*)

    written += buffer.size

    if (written > limit) {
      writer.close()
      written = 0L
      writer = makeNewWriter()
    }

    buffer.clear();
  }

  @PreDestroy
  override def stop(): Unit = {
    super.stop()
    writer.close()
  }
}


