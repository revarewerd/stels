package ru.sosgps.wayrecall.processing

import java.util.{Date, Timer}
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.google.common.cache.{CacheBuilder, LoadingCache, RemovalListener, RemovalNotification}
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.{ClusteredPacketsReader, IllegalImeiException, PackDataConverter}
import ru.sosgps.wayrecall.processing.lazyseq._
import ru.sosgps.wayrecall.utils.CollectionUtils.additionalMethods
import ru.sosgps.wayrecall.utils.concurrent.FuturesUtils.runSafe
import ru.sosgps.wayrecall.utils.concurrent.{FuturesUtils, OrderedScalaExecutorService, ScalaExecutorService}
import ru.sosgps.wayrecall.utils.{funcLoadingCache, tryNumerics}

import scala.concurrent.Future

/**
 * Created by nickl on 08.04.15.
 */
class LazySeqAccumilatedParamsProcessor(val cacheTime: Long) extends ((GPSData) => Future[GPSData]) with grizzled.slf4j.Logging {

  def this(reader: ClusteredPacketsReader, cnv: PackDataConverter, cacheTime: Long) {
    this(cacheTime)
    this.reader = reader
    this.cnv = cnv
  }

  def this(reader: ClusteredPacketsReader, cnv: PackDataConverter) {
    this(reader, cnv, 900000)
  }

  def this() = this(cacheTime = 900000)

  @Autowired
  var reader: ClusteredPacketsReader = null

  @Autowired
  var cnv: PackDataConverter = null

  var evictingTime = 1200000

  val timer = new Timer()

  override def apply(gps: GPSData): Future[GPSData] = runSafe {
    if (getWalker(gps.imei).isDefined) {

      for (ac <- accs) {
        try {
          ac.evaluate(gps)
        } catch {
          case e: Throwable => warn("error processing " + gps + " by " + ac, e)
        }
      }
      if(gps.privateData.isEmpty)
        warn(s"empty private data for $gps")
    }

    Future.successful(gps.clone())
  }

  private val accs = Seq(mh, distance)

  private object distance extends AggregatorCalculator[Double, DistanceAggregator] {

    val paramName: String = "tdist"

    protected def genAggregator(v: Double, splitter: GpsIntervalSplitter): DistanceAggregator = {
      new DistanceAggregator(v, splitter)
    }

    override protected def numeric: Numeric[Double] = implicitly

  }

  private object mh extends AggregatorCalculator[Long, MotohoursAggregator] {

    val paramName: String = "mh"

    protected def genAggregator(v: Long, splitter: GpsIntervalSplitter): MotohoursAggregator = {
      new MotohoursAggregator(v, splitter, cnv)
    }

    override protected def numeric: Numeric[Long] = implicitly
  }

  private val getWalker: LoadingCache[String, Option[GPSHistoryWalker]] = CacheBuilder.newBuilder()
    .expireAfterAccess(cacheTime, TimeUnit.MILLISECONDS).buildWithFunction((imei: String) => {
    val maybeWalker = try {
      //debug("loading walker for " + imei)
      val storeByImei = reader.getStoreByImei(imei)
      //val innerTree = storeByImei.asInstanceOf[{def put(gps: GPSData)}]
      reader.uidByImei(imei).map(uid => {
        val historyWalker = new TimeoutCachingGPSHistoryWalker(
          new PackageStoreGPSWalker(uid, storeByImei, (gps) => {
            //innerTree.put(gps)
          }), timer, evictingTime)
        val latest = storeByImei.getLatestFor(Iterable(uid)).headOption
        //debug("putting latest:" + latest)
        latest.foreach(latest => {
          historyWalker.put(latest)
          accs.foreach(_.setValueFromGPS(latest))
        })
        historyWalker
      })
    }
    catch {
      case e: IllegalImeiException => None
    }
    trace(s"for $imei walker is defined: ${maybeWalker.isDefined}")
    maybeWalker
  })


  private trait AggregatorCalculator[K, T <: Aggregator[K]] {

    val paramName: String

    protected val getAggregator: LoadingCache[String, T] = CacheBuilder.newBuilder()
      //      .removalListener(new RemovalListener[String, T]() {
      //      override def onRemoval(notification: RemovalNotification[String, T]): Unit = {
      //        getWalker(notification.getKey).get.prev(new Date(Long.MaxValue))
      //          .foreach(gps => {
      //          debug("onRemoval writing " + notification.getValue.sum + " to " + gps)
      //          gps.privateData.put(paramName, notification.getValue.sum.asInstanceOf[AnyRef])
      //        })
      //      }
      //    })
      .expireAfterAccess(cacheTime, TimeUnit.MILLISECONDS).buildWithFunction((imei: String) => {
      val historyWalker = new DelegatingGPSHistoryWalker(getWalker(imei).get)
      //val innerTree = storeByImei.asInstanceOf[{def put(gps: GPSData)}]
      val splitter = new GpsIntervalSplitter(historyWalker)
      //val prev = new FiltredGPSWalker(historyWalker, g => valueFromGps(g).isDefined).prev(new Date(Long.MaxValue))
      //debug("creating aggregator, newest value is " + prev)
      //genAggregator(prev.flatMap(valueFromGps).getOrElse(numeric.zero), splitter)
      genAggregator(sumstore.getOrDefault(imei, numeric.zero), splitter)
    })

    //TODO: Make it persistent
    val sumstore = new ConcurrentHashMap[String, K]()

    protected implicit def numeric: Numeric[K]

    protected def genAggregator(startValue: K, splitter: GpsIntervalSplitter): T

    def evaluate(gps: GPSData): Unit = {
      val da: T = getAggregator(gps.imei)
      val splitResult = da.add(gps)
      trace("splitResult=" + splitResult)
      splitResult match {
        case split: Split =>
          valueFromGps(split.prev.start)
            .orElse(valueFromGps(split.prev.end)).foreach(v => {
            trace(paramName + " v =" + v)
            gps.privateData.put(paramName, v.asInstanceOf[AnyRef])
            sumstore.put(gps.imei, v)
          })
        case n: NewInterval =>
          trace(paramName + " accsum =" + da.sum)
          //          val maxFromInterval = n.interval.ends.filter(_.privateData.get(paramName) != null).maxByOpt(_.privateData.get(paramName).tryDouble)
          //
          //          maxFromInterval.foreach(end => {
          //            val value = end.privateData.get(paramName).tryDouble
          //            trace(paramName + " frominterval =" + da.sum)
          //            if (value > numeric.toDouble(da.sum))
          //              da.updateSumSafe(numeric.plus(da.sum, end.privateData.get(paramName).asInstanceOf[K]))
          //          })

          trace(paramName + " sum =" + da.sum)
          gps.privateData.put(paramName, da.sum.asInstanceOf[AnyRef])
          sumstore.put(gps.imei, da.sum)
        case _ =>
          trace(paramName + " sum =" + da.sum)
          gps.privateData.put(paramName, da.sum.asInstanceOf[AnyRef])
          sumstore.put(gps.imei, da.sum)
      }
    }

    def setValueFromGPS(gps: GPSData) = {
      valueFromGps(gps).foreach(sumstore.put(gps.imei, _))
    }

    def valueFromGps(start: GPSData): Option[K] = {
      Option(start.privateData.get(paramName)).map(_.asInstanceOf[K])
    }
  }


}
