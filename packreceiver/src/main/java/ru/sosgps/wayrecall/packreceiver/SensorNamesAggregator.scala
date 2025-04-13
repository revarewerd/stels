package ru.sosgps.wayrecall.packreceiver

import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, MongoDBManager, GPSData}
import org.springframework.beans.factory.annotation.Autowired
import com.google.common.cache.{CacheBuilder, LoadingCache}
import java.util.concurrent.{ThreadPoolExecutor, ArrayBlockingQueue, TimeUnit}
import ru.sosgps.wayrecall.data.MapProtocolPackConverter
import ru.sosgps.wayrecall.initialization.{MultiDbManager}
import ru.sosgps.wayrecall.utils.funcLoadingCache
import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.asScalaSet
import javax.annotation.PostConstruct
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import scala.collection.mutable
import java.util.Date

/**
 * Created by nickl on 17.03.14.
 */
class SensorNamesAggregator extends (GPSData => Unit) with grizzled.slf4j.Logging {

  @Autowired
  var mmdbm: MultiDbManager = null;

  @Autowired
  var protocolConverter: MapProtocolPackConverter = null;

  private[this] def sensorNamesDb(imei: String): MongoCollection = {
    mmdbm.dbByIMEI(imei).getDatabase()("sensorNames")
  }

  @PostConstruct
  def init() {
    mmdbm.foreach(_.getDatabase()("sensorNames").ensureIndex("imei"))
  }

  private[this] val cachedSensors: LoadingCache[String, Set[String]] = CacheBuilder.newBuilder()
    .expireAfterAccess(10, TimeUnit.HOURS)
    .buildWithFunction((imei: String) => {

    sensorNamesDb(imei).findOne(MongoDBObject("imei" -> imei))
      .map(_.as[MongoDBList]("params").map(_.asInstanceOf[String]).toSet).getOrElse(Set.empty)
  })

  private val updater = new ScalaExecutorService("sensorNamesUpdater", 1, 1, false, 10, TimeUnit.HOURS, new ArrayBlockingQueue[Runnable](1000), new ThreadPoolExecutor.DiscardPolicy)

  override def apply(v0: GPSData): Unit = {
    val v1 = protocolConverter.convertData(v0)
    updater.future {
      try {
        val exisiting = cachedSensors(v1.imei)
        val newsensors = (v1.data.keySet(): mutable.Set[String]) -- exisiting

        if (newsensors.nonEmpty) {
          val newdata = exisiting ++ newsensors
          debug("adding " + newsensors + " to " + v1.imei)
          cachedSensors.put(v1.imei, newdata)
          sensorNamesDb(v1.imei).update(
            MongoDBObject("imei" -> v1.imei),
            MongoDBObject("imei" -> v1.imei, "params" -> newdata, "date" -> new Date()), upsert = true
          )
        }
      } catch {
        case e: Throwable => warn("updatererror", e)
      }
    }

  }
}
