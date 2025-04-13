package ru.sosgps.wayrecall.regeocoding

import java.util.concurrent.{Callable, TimeUnit, ThreadPoolExecutor, ArrayBlockingQueue}

import org.springframework.stereotype.Component;
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.params.{HttpConnectionParams, BasicHttpParams}
import org.apache.http.util.EntityUtils
import org.apache.http.impl.conn.PoolingClientConnectionManager

import collection.immutable.IndexedSeq



import com.mongodb
import java.util.Date
import com.beust.jcommander.{Parameters, JCommander, Parameter}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.utils.web.ScalaJson

import scala.beans.BeanProperty
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class NominatimJsonReGeocoder extends ReGeocoder with grizzled.slf4j.Logging  {

  @BeanProperty
  var enabled = true;

  @BeanProperty
  //var regeocodingServiceURL: String = "http://open.mapquestapi.com/nominatim/v1/reverse"
  var regeocodingServiceURL: String = "http://localhost:7080/nominatim/reverse.php"

  private val defaultparams = Seq(
    "format" -> "json",
    "zoom" -> "18",
    "accept-language" -> "ru",
    "addressdetails" -> "1",
    "email" -> "lkuka+nominatim@yandex.ru"
  )

  @BeanProperty
  var connTimeout = 5000

  private[this] lazy val httpclient = {
    val manager: PoolingClientConnectionManager = new PoolingClientConnectionManager()
    manager.setMaxTotal(100)
    manager.setDefaultMaxPerRoute(100)
    val httpclient = new DefaultHttpClient(manager);
    HttpConnectionParams.setConnectionTimeout(httpclient.getParams, connTimeout);
    HttpConnectionParams.setSoTimeout(httpclient.getParams, connTimeout);
    httpclient
  }

  private[this] def getName(lon: Double, lat: Double): String = {
    debug("getting:" + lon + " " + lat)

    val str = {

      val params = Seq("lat" -> lat, "lon" -> lon) ++ defaultparams
      val paramstr = params.map(p => p._1 + "=" + p._2).mkString("&")

      val httpGet = new HttpGet(regeocodingServiceURL + "?" + paramstr);

      try {
        debug("httpGet=" + httpGet.getURI)

        val response1 = httpclient.execute(httpGet);

        debug("status=" + response1.getStatusLine());
        val entity1 = response1.getEntity();

        val resp: String = EntityUtils.toString(entity1)

        EntityUtils.consume(entity1);
        try {
          ScalaJson.parse[Map[String, AnyRef]](resp).apply("display_name").asInstanceOf[String].split(',').take(3).mkString(",")
        }
        catch {
          case e: com.fasterxml.jackson.core.JsonParseException => {
            throw new RuntimeException("not a correct answer:" + resp)
          }
        }
      } finally {
        httpGet.releaseConnection();
      }
    }

    debug("getted")
    str
  }

  def getPosition(lon: Double, lat: Double): Future[Either[java.lang.Throwable, String]] = Future.successful(if (enabled) {
    try {
      Right(getName(lon, lat))
    }
    catch {
      case e@(_: java.net.SocketTimeoutException
              | _: java.util.NoSuchElementException
              | _: org.apache.http.conn.HttpHostConnectException
        ) => {
        //System.err.println("getPosition:" + e.toString)
        Left(e)
      }
      case e: Throwable => {
        e.printStackTrace()
        Left(e)
      }
    }
  } else Left(new UnsupportedOperationException("ReGeocoder was disabled")))

  private[this] lazy val pool = {
    val blockingQueue = new ArrayBlockingQueue[Runnable](10);
    val rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
    new ThreadPoolExecutor(0, 10, 2000L, TimeUnit.MILLISECONDS, blockingQueue, rejectedExecutionHandler);
  }

  private[this] def getPositionRequest(positions: IndexedSeq[(Double, Double)]): IndexedSeq[Either[java.lang.Throwable, String]] = {

    debug("Group stated")

    val futures: IndexedSeq[java.util.concurrent.Future[Either[java.lang.Throwable, String]]] = positions.map(pos => pool.submit(new Callable[Either[java.lang.Throwable, String]] {
      def call():Either[java.lang.Throwable, String] = Await.result(getPosition _ tupled pos, Duration.Inf)
    }
    )).toIndexedSeq

    val r = futures.map(_.get())
    debug("Group finished")
    r
  }

  def getPositions(positions: Iterable[(Double, Double)]): Iterable[Either[java.lang.Throwable, String]] = positions.grouped(10).map(g => getPositionRequest(g.toIndexedSeq)).flatten.toIterable

  def setTimeout(timeout: Int) { this.connTimeout = timeout}
}

