package ru.sosgps.wayrecall.regeocoding

import com.beust.jcommander.{JCommander, Parameter, Parameters}
import com.mongodb.casbah.Imports
import ru.sosgps.wayrecall.initialization.MultiDbManager
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import ru.sosgps.wayrecall.utils.concurrent.{SameThreadExecutorService, ScalaExecutorService}
import scala.beans.BeanProperty
import java.util.concurrent.atomic.{AtomicReference, AtomicInteger}
import java.util.concurrent._
import org.springframework.beans.factory.annotation.Autowired
import com.mongodb
import java.util.Date
import ru.sosgps.wayrecall.utils.PerMinuteLogger
import java.lang.IllegalArgumentException

import scala.concurrent.Future


class ReGeocoderRunner extends grizzled.slf4j.Logging {

  import com.mongodb.casbah.Imports._
  import org.springframework.context.support.ClassPathXmlApplicationContext
  import ru.sosgps.wayrecall.core.MongoDBManager
  import com.mongodb.casbah.MongoDB
  import com.mongodb.casbah.Imports._


  @BeanProperty
  var unsafe: Boolean = false;

//  @BeanProperty
//  var parallel: Int = 10;

  @BeanProperty
  var connTimeout = 5000

  @BeanProperty
  var waitInterval = 30

  private[this] val processed = new AtomicInteger(0)

  private[this] val sequentialErrors = new AtomicInteger(0)

  private[this] val maxSequentialErrors = 100;

  private[this] val regeoQueuelimit = 500

  private[this] val poolError = new AtomicReference[Throwable](null);

  private[this] val perlogger = new PerMinuteLogger(60 * 1000);

  private[this] val sem = new Semaphore(regeoQueuelimit);

  @BeanProperty
  var regeocoder: ReGeocoder = null

  @BeanProperty
  @Autowired
  var mmdbm: MultiDbManager = null

  @BeanProperty
  var bufferNames: Seq[String] = IndexedSeq("objPacks.buffer")

  private[this] var wc: mongodb.WriteConcern = null


  private implicit val executionContext = new SameThreadExecutorService().executionContext

  private[this] def initialize() {        //pool = Executors.newFixedThreadPool(parallel);

    //    pool = new ScalaExecutorService(
    //      "regeocodingPool", parallel, (parallel * 1.5).toInt,
    //      true, 30, TimeUnit.MINUTES,
    //      new ArrayBlockingQueue[Runnable](regeoQueuelimit), new ThreadPoolExecutor.DiscardOldestPolicy
    //    )
    poolError.set(null)
    sequentialErrors.set(0)
    processed.set(0)

    wc = if (unsafe) WriteConcern.Normal else WriteConcern.Safe
    require(regeocoder.enabled, "regeocoder isn't enabled")

  }

  def process() {
    initialize()
    try {

      def ungeocodedGen: Iterator[(MongoCollection, DBObject)] =  getUngeocodedFromBuffer()


      val ungeocoded =
        ungeocodedGen ++ Stream.continually({
          val gen = ungeocodedGen
          if (!gen.hasNext) {
            info("no data for infinite regeocoding, waiting for " + waitInterval + " seconds")
            Thread.sleep(waitInterval * 1000)
          }
          gen
        }).iterator.flatten


      for (dbo <- ungeocoded) {
        if (poolError.get() != null) {
          throw poolError.get()
        }

        sem.acquire()


        val (collection, obj) = dbo
        val positions = try {
          regeocoder.getPosition(
            obj.as[Double]("lon"),
            obj.as[Double]("lat")
          )
        } catch {
          case e: Exception => {
            error("regecoding error", e);
            Future.failed(e)
          }
        }
        positions.onSuccess {
          case positions => writePosition(collection, obj, positions)
        }
        positions.onComplete {
          case _ => sem.release()
        }
      }
    }
    finally {

      info("regecoding finished")
      //pool.shutdown();
    }
  }

  def processTrulyInfinitely() {
    if (this.regeocoder != null && this.regeocoder.enabled) {
      new Thread(new Runnable {
        def run() {
          try {

            while (true) {
              try {
                process()
              }
              catch {
                case e: Exception => error("processTrulyInfinitely error", e)
              }
              val seconds = 30
              info("Background regecocoder have some problems, waiting for " + seconds + " seconds and restarting")
              Thread.sleep(seconds * 1000)

            }
          }
          finally {
            info("infinite regeocoding finished")
          }
        }
      }, "background buffer regeocoding").start()

    }
    else {
      info("regecoder was not started because it was not enabled")
    }


  }

  private[this] def getUngeocodedFromBuffer(): Iterator[(MongoCollection, DBObject)] = {
    var query = MongoDBObject("pn" -> MongoDBObject("$exists" -> false), "lon" -> MongoDBObject("$exists" -> true));

    debug("using packetsCollections=" + bufferNames)

    val ungeocoded = for (collection <- mmdbm.iterator.flatMap(mdbm => bufferNames.iterator.map(bn => mdbm.getDatabase()(bn)))) yield {

      val data = collection.find(query
      ).batchSize(300).sort(MongoDBObject("$natural" -> -1)).asInstanceOf[MongoCursor].toIterator

      data.map(dbo => (collection, dbo))
    }

    ungeocoded.flatten
  }

  //  private[this] def regeocode(collection: MongoCollection, dbo: DBObject) {
  //    val positions = regeocoder.getPosition(
  //      dbo.as[Double]("lon"),
  //      dbo.as[Double]("lat")
  //    )
  //
  //    writePosition(collection, dbo, positions)
  //  }

  def writePosition(collection: Imports.MongoCollection, dbo: Imports.DBObject, positions: Either[Throwable, String]) {
    //db("packets").update(MongoDBObject("uid" -> gpsdata.uid, "time" -> gpsdata.time), $set("placeName" -> placeName), false, false)
    if (positions.isRight)
      this.synchronized {
        collection.update(dbo, $set("pn" -> positions.right.get), false, false, wc)
        trace("dbo " + dbo.get("_id") + " was updated with " + positions.right.get)
        sequentialErrors.set(0)
      }
    else if (positions.left.toOption.collect({ case e: NoSuchElementException => true}).getOrElse(false)) {
      this.synchronized {
        collection.update(dbo, $set("pn" -> "-"), false, false, wc)
        trace("dbo " + dbo.get("_id") + " was updated with -")
        sequentialErrors.set(0)
      }
    } else {
      positions.left.get match {
        case e: IllegalArgumentException => debug("dbo " + dbo.get("_id") + " has response:" + e)
        case e: Throwable => debug("dbo " + dbo.get("_id") + " has response:", e)
      }

      if (sequentialErrors.incrementAndGet() > maxSequentialErrors) {
        poolError.set(new IllegalStateException("too many errors", positions.left.get))
      }
    }

    processed.incrementAndGet()


    if (isDebugEnabled)
      perlogger.logPerMinute((per, mis) => {
        debug(" processed:" + per + "(" + processed.get() + ")" + " mis=" + mis)
      })
  }
}
