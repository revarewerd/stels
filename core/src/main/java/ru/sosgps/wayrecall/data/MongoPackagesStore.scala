package ru.sosgps.wayrecall.data

import com.mongodb.casbah.commons.{Imports, MongoDBObject}

import java.util.Date
import javax.annotation.PostConstruct;
import com.mongodb.casbah.query
import com.mongodb.casbah.query.dsl.QueryExpressionObject
import org.springframework.beans.factory.annotation.Autowired;
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.utils.CollectionUtils.additionalMethods
import ru.sosgps.wayrecall.utils.{PartialIterable, LimitsSortsFilters, impldotChain}
import collection.JavaConversions.asScalaBuffer
import scala.io.Source
import scala.collection.mutable
import scala.beans.BeanProperty
import GPSUtils.filterBadLonLat
import org.springframework.context.ApplicationContext


class MongoPackagesStore extends PackagesStore with grizzled.slf4j.Logging with MultiObjectsPacketsMixin {

  @Autowired
  var mongoDbManager: MongoDBManager = null;

  @Autowired
  var or: ObjectsRepositoryReader = null;

  protected var db: MongoDB = null;

  @BeanProperty
  var batchSize = 1000

  @BeanProperty
  var skipPreloadCache = System.getProperty("wayrecall.skipCachePreload", "false").toBoolean

  @Autowired
  var pconv: PackDataConverter = null;

  val timeField = "time"

  override def getLatestFor(uids: Iterable[String]): Iterable[GPSData] = {
    //    if (uids == null)
    //      permissionsChecker.getAvailableObjectUids().flatMap(latest.get)
    //    else uids.filter(permissionsChecker.checkIfObjectIsAvailable).flatMap(latest.get)

    uids.flatMap(uid => {
      biggestBuffer.find(MongoDBObject("uid" -> uid, "lon" -> MongoDBObject("$exists" -> true)))
        .sort(MongoDBObject("time" -> -1)).limit(1)
        .map(GPSDataConversions.fromDbo)
        .toIterable.headOption.orElse(
          collCache.get(uid).find(MongoDBObject("lon" -> MongoDBObject("$exists" -> true)))
            .sort(MongoDBObject("time" -> -1)).limit(1)
            .map(dbo => pconv.convertData(GPSDataConversions.fromDbo(dbo)))
            .toIterable.headOption)
    }).find(_ => true)
  }



//  override def getLatestForIntervals(uidsAndIntervals: Iterable[(String, Seq[(Date,Date)])]) = {
//    //    if (uids == null)
//    //      permissionsChecker.getAvailableObjectUids().flatMap(latest.get)
//    //    else uids.filter(permissionsChecker.checkIfObjectIsAvailable).flatMap(latest.get)
//    debug("getLatestForIntervals" + uidsAndIntervals.toMap)
//    uidsAndIntervals.flatMap{case(uid,intervals) => {
//      // Time limit
//      intervals.reverse.toStream.map { case(minTime,maxTime) =>
//        biggestBuffer.find(MongoDBObject("uid" -> uid, "lon" -> MongoDBObject("$exists" -> true)) ++ (timeField $lte maxTime $gte minTime))
//          .sort(MongoDBObject("time" -> -1)).limit(1)
//          .map(GPSDataConversions.fromPerObjectDbo)
//          .toIterable.headOption.orElse(
//            collCache.get(uid).find(MongoDBObject("lon" -> MongoDBObject("$exists" -> true)) ++ (timeField $lte maxTime $gte minTime))
//              .sort(MongoDBObject("time" -> -1)).limit(1)
//              .map(dbo => pconv.convertData(GPSDataConversions.fromPerObjectDbo(dbo)))
//              .toIterable.headOption)
//      }.find(_.isDefined).getOrElse(None)
//    }}.find(_ => true) // ЩИТО? Потому что вызывается только для одного объекта
//  }

  override def getLatestWithinIntervals(uid: String, intervals: Seq[(Date, Date)]) = {
    debug(s"getLatestWithinIntervals uid = $uid, intervals = $intervals")
    intervals.reverse.toStream
      .map {case (begin,end) =>
      biggestBuffer.find(MongoDBObject("uid" -> uid, "lon" -> MongoDBObject("$exists" -> true)) ++ (timeField $lte end $gte begin))
        .sort(MongoDBObject("time" -> -1)).limit(1)
        .map(GPSDataConversions.fromDbo)
        .toIterable.headOption.orElse(
          collCache.get(uid).find(MongoDBObject("lon" -> MongoDBObject("$exists" -> true)) ++ (timeField $lte end $gte begin))
            .sort(MongoDBObject("time" -> -1)).limit(1)
            .map(dbo => pconv.convertData(GPSDataConversions.fromDbo(dbo)))
            .toIterable.headOption)
    }.find(_.isDefined).getOrElse(None)
  }

  private[this] def replaceUid(uid: String): (DBObject) => DBObject = {
    dbo => {dbo.put("uid", uid); dbo}
  }

//  def getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters): MovementHistory = {
//    info(s"PerObject getHistoryFor uid=$uid, from = $from, to=$to, start=${limitsAndSorts.start}, limit=${limitsAndSorts.limit}")
//    require(uid != null, " uid cant be null")
//    require(from != null, " from cant be null")
//    require(to != null, " to cant be null")
//    require(limitsAndSorts != null, " limitsAndSorts cant be null")
//    val query = /*MongoDBObject("lon" -> MongoDBObject("$exists" -> true)) ++*/ ("time" $lte to $gte from)
//    val perObjcursor = collCache.get(uid).find(query).
//      sort(MongoDBObject("time" -> 1)).batchSize(batchSize).asInstanceOf[MongoCursor]
//
//    //debug("query(" + collCache.get(uid).name + ")" + perObjcursor.query + "explanation:" + perObjcursor.explain.mkString("[\n     ", "\n     ", "\n]"))
//
//    val maxDateInPerObject = collCache.get(uid).find(query, MongoDBObject("time" -> 1, "_id" -> 0)).
//      sort(MongoDBObject("time" -> -1)).limit(1).map(_.as[Date]("time")).toIterable.headOption.getOrElse(from)
//
//    val minBufferDate = if (maxDateInPerObject.after(from)) maxDateInPerObject else from
//
//    val bufferCursor = biggestBuffer.find(MongoDBObject("uid" -> uid /*, "lon" -> MongoDBObject("$exists" -> true)*/) ++ ("time" $lte to $gte minBufferDate)).
//      sort(MongoDBObject("time" -> 1)).batchSize(batchSize).asInstanceOf[MongoCursor]
//
//    debug("query(" + biggestBuffer.name + ") " + bufferCursor.query + "explanation:" + bufferCursor.explain.mkString("[\n     ", "\n     ", "\n]"))
//
//    val perObjSkip = limitsAndSorts.start
//    val bufferSkip = math.max(0, perObjSkip - perObjcursor.count)
//    val total = perObjcursor.count + bufferCursor.count
//
//    val limit = limitsAndSorts.limit.getOrElse(total)
// //   val total =  math.max(0, math.min(cursorSize,limit))
//
//    val bufferLimit = math.max(0,limit - perObjcursor.count + perObjSkip)
//
//    debug("loading movementhistory for " + uid + " " + from + "-" + to + " reslen=" + total +
//      " from collections:" + collCache.get(uid).getName + " with limitsAndSorts = " + limitsAndSorts)
//
//
//    debug("limitsAndSorts.start=" + limitsAndSorts.start + " globallimit=" + total + " bufferSkip=" + bufferSkip + " bufferLimit=" + bufferLimit)
//
//    val loaded = (
//      perObjcursor.skip(perObjSkip).take(total) ++
//        bufferCursor.skip(bufferSkip).take(bufferLimit)
//      ).map(dbo => pconv.convertData(GPSDataConversions.fromPerObjectDbo(dbo)))
//
//    if (loaded.isEmpty)
//      debug("loaded is empty")
//
//    //loaded.toStream
//
//    //TODO: возможно это вообще имеет смысл делать оффлайном в базе
//    val stream = filterBadLonLat(loaded).toStream
//
//    new MovementHistory(total, limitsAndSorts.start, limit, stream.iterator) // Судя по запросам start не нужен
//  }

  def getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters): MovementHistory = {
    require(uid != null, " uid cant be null")
    require(from != null, " from cant be null")
    require(to != null, " to cant be null")
    require(limitsAndSorts != null, " limitsAndSorts cant be null")
    val query = /*MongoDBObject("lon" -> MongoDBObject("$exists" -> true)) ++*/ ("time" $lte to $gte from)
    val perObjcursor = collCache.get(uid).find(query).
      sort(MongoDBObject("time" -> 1)).batchSize(batchSize).asInstanceOf[MongoCursor]

    //debug("query(" + collCache.get(uid).name + ")" + perObjcursor.query + "explanation:" + perObjcursor.explain.mkString("[\n     ", "\n     ", "\n]"))

    val maxDateInPerObject = collCache.get(uid).find(query, MongoDBObject("time" -> 1, "_id" -> 0)).
      sort(MongoDBObject("time" -> -1)).limit(1).map(_.as[Date]("time")).toIterable.headOption.getOrElse(from)

    val minBufferDate = if (maxDateInPerObject.after(from)) maxDateInPerObject else from

    val bufferCursor = biggestBuffer.find(MongoDBObject("uid" -> uid /*, "lon" -> MongoDBObject("$exists" -> true)*/) ++ ("time" $lte to $gte minBufferDate)).
      sort(MongoDBObject("time" -> 1)).batchSize(batchSize).asInstanceOf[MongoCursor]

    debug("query(" + biggestBuffer.name + ") " + bufferCursor.query + "explanation:" + bufferCursor.explain.mkString("[\n     ", "\n     ", "\n]"))

    val msize = perObjcursor.count + bufferCursor.count

    debug("loading movementhistory for " + uid + " " + from + "-" + to + " reslen=" + msize +
      " from collections:" + collCache.get(uid).getName + " with limitsAndSorts = " + limitsAndSorts)

    val bufferSkip = math.max(0, limitsAndSorts.start - perObjcursor.count)
    val globallimit = limitsAndSorts.limit.getOrElse(msize)
    val bufferLimit = math.max(0, globallimit + limitsAndSorts.start - perObjcursor.count)

    debug("limitsAndSorts.start=" + limitsAndSorts.start + " globallimit=" + globallimit + " bufferSkip=" + bufferSkip + " bufferLimit=" + bufferLimit)

    val loaded = (
      perObjcursor.skip(limitsAndSorts.start).take(globallimit) ++
        bufferCursor.skip(bufferSkip).take(bufferLimit)
      ).map(dbo => pconv.convertData(GPSDataConversions.fromDbo(dbo)))

    if (loaded.isEmpty)
      debug("loaded is empty")

    //loaded.toStream

    //TODO: возможно это вообще имеет смысл делать оффлайном в базе
    val stream = filterBadLonLat(loaded).toStream

    new MovementHistory(msize, limitsAndSorts.start, globallimit, stream.iterator)
  }


  override def at(uid: String, date: Date): Option[GPSData] = queryOne(uid, "time" $eq date, 1).find(_ => true)

  override def prev(uid: String, date: Date): Option[GPSData] = queryOne(uid, "time" $lt date, -1).maxByOpt(_.time)

  override def prev(uid: String, date: Date, lowerBound: Date): Option[GPSData] =
    queryOne(uid, "time" $lt date $gte lowerBound, -1).maxByOpt(_.time)

  override def next(uid: String, date: Date): Option[GPSData] = queryOne(uid, "time" $gt date, 1).minByOpt(_.time)

  override def next(uid: String, date: Date, upperBound: Date): Option[GPSData] =
    queryOne(uid, "time" $gt date $lte upperBound, 1).minByOpt(_.time)

  private def queryOne(uid: String, query: DBObject, sortDirection: Int): Iterator[GPSData] = {
    val perObjcursor = collCache.get(uid).find(query).
      sort(MongoDBObject("time" -> sortDirection)).limit(1).asInstanceOf[MongoCursor]

    val bufferCursor = biggestBuffer.find(MongoDBObject("uid" -> uid /*, "lon" -> MongoDBObject("$exists" -> true)*/) ++ query).
      sort(MongoDBObject("time" -> sortDirection)).limit(1).asInstanceOf[MongoCursor]

    (bufferCursor ++ perObjcursor).map(dbo => pconv.convertData(GPSDataConversions.fromDbo(dbo)))
  }

  def removePositionData(uid: String, from: Date, to: Date) = {
    val query = "time" $lte to $gte from
    val removedA = biggestBuffer.find(MongoDBObject("uid" -> uid) ++ query)
    val removedB = collCache.get(uid).find(query)
    lazy val collForRemoved = mongoDbManager.getDatabase()("objPacks.removed")

    Seq(removedA, removedB).flatten.foreach(coll => {
      collForRemoved.insert(coll)
    })

    biggestBuffer.remove(MongoDBObject("uid" -> uid) ++ query, WriteConcern.Safe)
    collCache.get(uid).remove(query, WriteConcern.Safe)
  }


  def getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double): Option[GPSData] = {
    val startTimeAll = System.currentTimeMillis()
    def distToTarget(g: GPSData) = GPSData.eulcidian2distance(g, lon, lat)

    val query = ("time" $lte to $gte from) ++
      ("lon" $lte (lon + radius) $gte (lon - radius)) ++
      ("lat" $lte (lat + radius) $gte (lat - radius))

    val fields = MongoDBObject("time" -> 1, "lon" -> 1, "lat" -> 1, "crs" -> 1,"spd" -> 1,"pn" -> 1, "_id" -> 0)
    val startTime = System.currentTimeMillis()
    val perObjCursor = collCache.get(uid).find(
      query,
      fields
    )
    debug("query(" + collCache.get(uid).name + ")" + perObjCursor.query + "explanation:" + perObjCursor.explain.mkString("[\n     ", "\n     ", "\n]"))
    debug("nlonlat query(" + collCache.get(uid).name + ") took " + (System.currentTimeMillis() - startTime) + " miils")

    val bufferCursor = biggestBuffer.find(
      query ++ ("uid" -> uid),
      fields
    )
    //debug("query(" + buffercoll.name + ") " + bufferCursor.query + "explanation:" + bufferCursor.explain.mkString("[\n     ", "\n     ", "\n]"))

    val r = (perObjCursor ++ bufferCursor).map(GPSDataConversions.fromDbo)
      .reduceOption((a, b) => if (distToTarget(a) < distToTarget(b)) a else b)

    debug("nlonlat calk took " + (System.currentTimeMillis() - startTimeAll) + " miils")

    r
  }

  def getPositionsCount(uid: String, from: Date, cur: Date): Int = {

    val query = /*MongoDBObject("lon" -> MongoDBObject("$exists" -> true)) ++ */ ("time" $lte cur $gte from)
    val perObjectCount = collCache.get(uid).find(
      query
    ).count
    val bufferCount = biggestBuffer.find(
      query ++ ("uid" -> uid)
    ).count

    debug("getPositionsCount " +(uid: String, from: Date, cur: Date) + " perObjectCount=" + perObjectCount + " bufferCount=" + bufferCount)
    perObjectCount + bufferCount

  }


  @PostConstruct
  def start(): Unit = {
    info("starting PerMonthMongoPackStore")
    db = mongoDbManager.getConnection.getDB(mongoDbManager.databaseName)
    if (!skipPreloadCache) {
      val packetsCollections = getPacketsCollections()
      if (packetsCollections.isEmpty)
        warn("no packets collections found, nothing to index")
      packetsCollections.foreach(ensurePacketsCollectionIndex)
    }
  }

  override def refresh(uid: String): Unit = {}
}

