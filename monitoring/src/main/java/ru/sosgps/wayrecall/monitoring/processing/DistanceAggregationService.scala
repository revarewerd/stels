package ru.sosgps.wayrecall.monitoring.processing

import java.util.Timer
import java.util.concurrent.TimeUnit
import javax.annotation.Resource

import com.google.common.cache.CacheBuilder
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{GPSData, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.processing.lazyseq.{DistanceAggregator, GpsIntervalSplitter, PackageStoreGPSWalker, TimeoutCachingGPSHistoryWalker}
import ru.sosgps.wayrecall.utils.funcLoadingCache

/**
 * Created by nickl on 13.04.15.
 */
class DistanceAggregationService {

  var evictingTime = 300000

  val timer = new Timer()

  @Resource(name = "directPackageStore")
  var store: PackagesStore = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  private val getAggregator = CacheBuilder.newBuilder()
    .expireAfterAccess(10, TimeUnit.MINUTES).buildWithFunction((uid: String) => {
    //val innerTree = store.asInstanceOf[{def put(gps: GPSData)}]
    val splitter = new GpsIntervalSplitter(new TimeoutCachingGPSHistoryWalker(
      new PackageStoreGPSWalker(uid, store, (gps) => {
        //innerTree.put(gps)
      }), timer, evictingTime))

    //TODO: must not be 0.0
    new DistanceAggregator(0.0, splitter)
  })


  def updateAccumulator(gps: GPSData, prev: Double): Double = {

    val aggregator = getAggregator(gps.uid)

    aggregator.synchronized {
      aggregator.sum = prev
      aggregator.add(gps)
      aggregator.sum
    }
  }


}
