package ru.sosgps.wayrecall.packreceiver


import com.mongodb.MongoException.DuplicateKey
import com.mongodb.casbah.Imports
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.annotation.{AnnotationEventListenerAdapter, EventHandler}
import org.bson.types.ObjectId
import ru.sosgps.wayrecall.core._
import org.springframework.beans.factory.annotation.{Autowired, Value}
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.data.{DBPacketsWriter, DirectMongoDBPacketsWriter, IllegalImeiException}
import ru.sosgps.wayrecall.utils.ScalaConverters
import ru.sosgps.wayrecall.utils.concurrent.{ReentrantLock, ReentrantReadWriteLock}
import ru.sosgps.wayrecall.utils.web.ScalaJson

import scala.beans.BeanProperty
import java.util.concurrent.{ArrayBlockingQueue, Executors, TimeUnit}

import org.springframework.jms.core.{JmsTemplate, MessageCreator}
import com.mongodb.casbah.commons.MongoDBObject
import javax.jms.{Message, Session}
import javax.annotation.{PostConstruct, PreDestroy}

import scala.Some
import com.mongodb.{DBCollection, DuplicateKeyException, MongoException}
import com.google.common.cache.{CacheBuilder, CacheLoader}
import java.util.concurrent.atomic.AtomicInteger

import scala.Some
import java.util.Date

import com.google.common.util.concurrent.RateLimiter
import com.mongodb
import kamon.Kamon
import ru.sosgps.wayrecall.utils.CollectionUtils.additionalMethods

/**
  * Created with IntelliJ IDEA.
  * User: nickl
  * Date: 06.05.13
  * Time: 15:03
  * To change this template use File | Settings | File Templates.
  */
class BufferedMongoDBPacketsWriter extends DirectMongoDBPacketsWriter with grizzled.slf4j.Logging {

  @volatile
  private[this] var _activeBuffer: Imports.MongoCollection = null

  //TODO: actually there is only one buffer
  private def activeBuffer = {
    if (_activeBuffer == null)
      _activeBuffer = setupBufferCollection(biggestBuffer.getName)
    //debug("_activeBuffer=" + _activeBuffer.getFullName() + " _activeBuffer.size=" + _activeBuffer.size)
    _activeBuffer
  }

  //TODO: remove it, not actial
  private[this] val activeBufferLock = new ReentrantReadWriteLock()

  private[this] val bufferInserter = Executors.newSingleThreadExecutor();

  @BeanProperty
  var bufferSize = 50000

  private val rateLimiter = RateLimiter.create(600)

  def transferQps_=(qps: Double) = rateLimiter.setRate(qps)

  def transferQps = rateLimiter.getRate

  private[this] val unikeys = Set("uid", "time", "lon", "lat")

  @throws(classOf[IllegalImeiException])
  override def addToDb(gpsdata: GPSData) = {

    val uidOpt = uidCache(gpsdata.imei).map(_.as[String]("uid"))

    uidOpt match {
      case Some(uid) =>
        if (gpsdata.insertTime == null) gpsdata.insertTime = new Date()
        gpsdata.uid = uid
        if (!getMainTerminal(uid).exists(_.as[String]("eqIMEI") == gpsdata.imei))
          gpsdata.data.put("mainTerminal", false.asInstanceOf[AnyRef])

        if (gpsdata.privateData.isEmpty)
          warn(s"empty private data $gpsdata")

        val mdb: DBObject = GPSDataConversions.toMongoDbObject(gpsdata)
        val disabled = or.isObjectDisabled(uid)
        if (!gpsdata.unchained()) {
          try {
            //TODO: возможно WriteConcern.Safe избыточен
            if (disabled)
              collCache.get(mdb.as[String]("uid")).insert(mdb, WriteConcern.Normal);
            else
              activeBufferLock.withRead(activeBuffer.insert(mdb, WriteConcern.Normal))
          }
          catch {
            case e: DuplicateKeyException => {
              //TODO: вообще тут надо сделать что-то более умное
              //            val existent = buffercoll.findOne(mdb.filterKeys(unikeys)).getOrElse(MongoDBObject())
              //            val diffAll = (mdb -("_id", "data")).toList.diff(existent.toList).toMap
              //            val diffData = mdb.getAsOrElse[DBObject]("data", DBObject()).toList
              //              .diff(existent.getAsOrElse[DBObject]("data", DBObject()).toList).toMap
              //            warn("mdb: " + mdb.filterKeys(unikeys) + " is duplicate diff=" + diffAll + " diffData=" + diffData +
              //              " was=" + existent.getAsOrElse[DBObject]("data", DBObject()).filterKeys(diffData.keySet))
            }
          }
          //processBufferOverflow()
        }
        !disabled
      case None => throw new IllegalImeiException(gpsdata.imei)
    }
  }


  override protected def collName(uid: String): String = {
    val disabled = or.isObjectDisabled(uid)
    val r = super.collName(uid) + (if (disabled) ".disabled" else "")
    debug("collName:" + r)
    r
  }

  private var bufferWriteConcern = new com.mongodb.WriteConcern(1, 0, true, false, true)

  def fsyncBufferTransfer_=(fsync: Boolean): Unit = {
    bufferWriteConcern = new com.mongodb.WriteConcern(1, 0, fsync, false, true)
  }

  def fsyncBufferTransfer = bufferWriteConcern.fsync()

  var perUidBufferLimit: Int = 100

  var drainSleepTime: Long = 30000

  @PostConstruct
  private def consumeOldBuffers(): Unit = {

    val PREV_BUFFER_COLLECTIONS = IndexedSeq("objPacks.buffer.A", "objPacks.buffer.B")

    for (prevbuff <- PREV_BUFFER_COLLECTIONS.map(mongoDbManager.getDatabase().getCollection(_).asScala)
      .map(ensureBufferIndex)) {

      debug(s"transfering prevbuffer $prevbuff")

      val cursor = prevbuff.find().sort(MongoDBObject("uid" -> 1)) /*.batchSize(1000)*/ .asInstanceOf[MongoCursor]
      val itemsCount = cursor.count
      debug("query" + cursor.query + "explanation:" + cursor.explain.mkString("[\n     ", "\n     ", "\n]"))
      for (groupedBlock <- cursor.grouped(600).map(_.groupBy(_.as[String]("uid")));
           (uid, dbos) <- groupedBlock
      ) {

        try {
          val collection = collCache.get(uid)
          //rateLimiter.acquire(dbos.size)
          collection.insert(dbos: _*)((obj: DBObject) => obj, bufferWriteConcern);
          debug(s"${dbos.size} elements of $uid transferred to collection ${collection.name}")

        }
        catch {
          case e: DuplicateKeyException => {
            warn("dbo: " + e + " is duplicate" /*, e*/)
          }
        }
      }
      debug("dropping collection " + prevbuff.getName)
      prevbuff.dropCollection()
      //buffercoll.remove("insTime" $lt date, WriteConcern.Safe);
      info("transfering buffer to PerObjectDone " + itemsCount + " items transfered")
    }
  }


  @PostConstruct
  private def drainBuffer(): Unit = {

    bufferInserter.submit(new Runnable {
      override def run(): Unit = try {

        val kontext = Kamon.tracer.newContext("drainBuffer")

        debug("start draining buffer")

        val segment = kontext.startSegment("quering", "drainBuffer", "receiver")

        Kamon.metrics.histogram("bufferSize", Map("db" -> mongoDbManager.databaseName)).record(activeBuffer.find().count())

        val allUids =
          if (activeBuffer.nonEmpty) activeBuffer.aggregate(List(
            MongoDBObject("$group" -> MongoDBObject("_id" -> "$uid", "count" -> MongoDBObject("$sum" -> 1))),
            MongoDBObject("$sort" -> MongoDBObject("count" -> -1))
          )).results
          else Iterable.empty

        debug("allUids = " + allUids.size)

        val bigUids = allUids.takeWhile(_.as[Int]("count") > perUidBufferLimit).map(_.as[String]("_id"))

        debug("bigUids = " + bigUids.size)

        Kamon.metrics.histogram("bigUidsSize", Map("db" -> mongoDbManager.databaseName)).record(bigUids.size)

        segment.finish()

        if (bigUids.nonEmpty) {

          val segment = kontext.startSegment("transfer", "drainBuffer", "receiver")

          for (uid <- bigUids) {

            val dbos = activeBuffer.find(MongoDBObject("uid" -> uid)).sort(MongoDBObject("time" -> 1)).toIndexedSeq

            val (minInserTime, maxInsertTime) = dbos.boundsFor(_.as[Date]("insTime"))


            val startTime = System.currentTimeMillis()

            val collection = collCache.get(uid)
            rateLimiter.acquire(dbos.size)
            try {
              collection.insert(dbos: _*)((obj: DBObject) => obj, bufferWriteConcern);
            }
            catch {
              case e: DuplicateKeyException => {
                warn("dbo: " + e + " is duplicate" /*, e*/)
              }
            }
            debug(s"${dbos.size} elements of $uid transferred to collection ${collection.name}")

            activeBuffer.remove(MongoDBObject(
              "uid" -> uid, "insTime" -> MongoDBObject("$gte" -> minInserTime, "$lte" -> maxInsertTime))
            )

            //            val spentTimeInserting = System.currentTimeMillis() - startTime
            //            Kamon.metrics.histogram("buffer-col-transferring-time").record(spentTimeInserting)
            //            Kamon.metrics.histogram("buffer-col-transferring-size").record(dbos.size)

          }

          segment.finish()
        }
        else {
          debug("sleeping draining buffer")
          Thread.sleep(drainSleepTime)
        }

        kontext.finish();


      } catch {
        case e: Exception => error("error draining buffer", e)
      } finally {
        drainBuffer()
      }
    })

  }


  @EventHandler
  private def handleObjectEnabledChanged(event: ObjectEnabledChanged) {
    debug("ObjectEnabledChanged : " + event)
    collCache.invalidate(event.uid)
  }


}

