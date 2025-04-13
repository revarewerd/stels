package ru.sosgps.wayrecall.retranslators

import java.net.InetSocketAddress
import java.util.concurrent.{ThreadPoolExecutor, ArrayBlockingQueue, TimeUnit}

import com.google.common.util.concurrent.MoreExecutors

import scala.concurrent.duration.DurationInt
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.params.HttpConnectionParams
import org.apache.http.util.EntityUtils
import org.joda.time.format.ISODateTimeFormat
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.NISClient
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import scala.collection.JavaConversions.mapAsScalaMap
import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration.Duration
import scala.util.{Success, Failure}

/**
 * Created by nickl on 01.10.14.
 */
class NisRealtimeRetranslator extends NISClient with (GPSData => Unit)  with ConfigurableRetranslator with grizzled.slf4j.Logging {

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  def this(configurer: RetranslationConfigurator) = {
    this()
    configurer.configure(this);
  }


  def main(args: Array[String]) {
    //val post = new HttpPost("http://77.243.111.83:20438")
    val ip = "185.22.60.17"
    val port = 2211
    //send(ip, port)
  }

  override def apply(v1: GPSData): Unit = rules.filter(_._2(v1)).foreach(rule => {

    send(rule._1.getHostName, rule._1.getPort, v1).onComplete {
      case Success(s) => {
        if (s._1 != 200) {
          warn("unexpected answer for " + v1 + ":" + s._2)
        }
      }
      case Failure(f) => warn("exception sending message " + v1, f)
    }

  })

  private var rules: Seq[(InetSocketAddress, (GPSData) => Boolean)] = Seq.empty

  override def configure(rules: Seq[(InetSocketAddress, (GPSData) => Boolean)]): Unit = {
    info("setting retranslation to " + rules)
    this.rules = rules
  }
}
