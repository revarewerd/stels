package ru.sosgps.wayrecall.core

import com.google.common.cache.{CacheLoader, CacheBuilder}
import java.util.concurrent.TimeUnit
import com.mongodb.{casbah, DBCollection}
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.funcLoadingCache
import scala.collection.Set

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 06.05.13
 * Time: 18:16
 * To change this template use File | Settings | File Templates.
 */
trait MultiObjectsPacketsMixin extends grizzled.slf4j.Logging {

  protected[this] var mongoDbManager: MongoDBManager;

  protected[this] var or: ObjectsRepositoryReader;

  //val BUFFER_COLLECTIONS = IndexedSeq("objPacks.buffer.A", "objPacks.buffer.B")
  val BUFFER_COLLECTIONS = IndexedSeq("objPacks.buffer")

  protected[this] val bbCache = CacheBuilder.newBuilder()
    .expireAfterWrite(15, TimeUnit.SECONDS).buildWithFunction((a: AnyRef) => {
    if (buffercolls.nonEmpty)
      buffercolls.maxBy(_.size)
    else
      mongoDbManager.getDatabase().getCollection(BUFFER_COLLECTIONS.head).asScala
  })

  /*protected[this]*/ def biggestBuffer: casbah.MongoCollection = bbCache("")

  protected[this] def buffercolls = BUFFER_COLLECTIONS
    .filter(mongoDbManager.getDatabase().collectionExists)
    .map(mongoDbManager.getDatabase().getCollection(_).asScala)

  protected def setupBufferCollection(name: String) = {
    val exists = mongoDbManager.getDatabase().collectionExists(name)
    debug(s"collection $name exists=$exists")
    val bc = if (exists)
      mongoDbManager.getDatabase().getCollection(name).asScala
    else try {
      mongoDbManager.getDatabase().createCollection(name, MongoDBObject("size" -> (100 * 1024 * 1024))).asScala
    } catch {
      case e: com.mongodb.CommandFailureException if e.getMessage.contains("collection already exists")  =>
        warn(s"it is very strange but told that collection $name already exists")
        mongoDbManager.getDatabase().getCollection(name).asScala
    }

    ensureBufferIndex(bc)
  }

  protected def ensureBufferIndex(bc: casbah.MongoCollection): MongoCollection = {
    info("ensuring Mongodb index " + bc.name+ "(\"time\" -> -1, \"lon\" -> 1, \"lat\" -> 1)")
    bc.ensureIndex(MongoDBObject("uid" -> 1, "time" -> -1, "lon" -> 1, "lat" -> 1), MongoDBObject("unique" -> true, "dropDups" -> true))
    bc.ensureIndex(MongoDBObject("insTime" -> 1))
    bc
  }

  protected def getPacketsCollections(): Seq[MongoCollection] = {
    mongoDbManager.getDatabase().getCollectionNames().filter(_.matches( """objPacks.o\d+""")).map(mongoDbManager.getDatabase()(_)).toIterable.toSeq
  }

  protected[this] val collCache = CacheBuilder.newBuilder()
    .expireAfterAccess(24, TimeUnit.HOURS)
    .build(
      new CacheLoader[String, MongoCollection]() {
        def load(uid: String): MongoCollection = {
          val cn = collName(uid)
          val coll = if (System.getProperty("wayrecall.skipPacketsEnsureIndex", "false").toBoolean ||
            mongoDbManager.getDatabase().collectionExists(cn))
            mongoDbManager.getDatabase().getCollection(cn).asScala
          else
            mongoDbManager.getDatabase().createCollection(cn, MongoDBObject("size" -> (4 * 1024 * 1024))).asScala
          ensurePacketsCollectionIndex(coll)
          coll
        }
      })


  protected def collName(uid: String): String = "objPacks." + uid

  def ensurePacketsCollectionIndex(coll: MongoCollection) {
    if (!System.getProperty("wayrecall.skipPacketsEnsureIndex", "false").toBoolean) {
      info("ensuring Mongodb index " + coll.name + "(\"time\" -> -1, \"lon\" -> 1, \"lat\" -> 1)")
      coll.ensureIndex(MongoDBObject("time" -> -1, "lon" -> 1, "lat" -> 1), MongoDBObject("unique" -> true))
    }
  }

  protected[this] val uidCache = CacheBuilder.newBuilder()
    .expireAfterWrite(3, TimeUnit.MINUTES)
    .build(
      new CacheLoader[String, Option[DBObject]]() {
        def load(imei: String): Option[DBObject] = Option(or.getObjectByIMEI(imei))
      })

  protected[this] val getMainTerminal = CacheBuilder.newBuilder()
    .expireAfterWrite(3, TimeUnit.MINUTES)
    .build(
      new CacheLoader[String, Option[DBObject]]() {
        def load(uid: String): Option[DBObject] = or.getMainTerminal(uid)
      })

  protected[this] def getTargetCollection(gpsdata: GPSData): Option[MongoCollection] = {
    Option(gpsdata.uid)
      .orElse(uidCache.get(gpsdata.imei).map(_.as[String]("uid")))
      .map(uid => {
      gpsdata.uid = uid
      collCache.get(uid)
    })
  }

  protected[this] def getTargetCollection(imei: String): Option[MongoCollection] = {
    uidCache.get(imei).map(_.as[String]("uid")).map(uid => {
      collCache.get(uid)
    })
  }

}
