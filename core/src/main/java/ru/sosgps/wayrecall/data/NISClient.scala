package ru.sosgps.wayrecall.data

import java.text.SimpleDateFormat

import com.google.common.util.concurrent.MoreExecutors
import grizzled.slf4j.Logging
import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.concurrent.FutureCallback
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.impl.nio.client.{CloseableHttpAsyncClient, HttpAsyncClientBuilder}
import org.apache.http.impl.nio.reactor.IOReactorConfig
import org.apache.http.params.HttpConnectionParams
import org.apache.http.util.EntityUtils
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils

import scala.collection.JavaConversions.mapAsScalaMap
import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by nickl on 01.10.14.
 */
class NISClient extends Logging {

  //  protected lazy val httpclient: DefaultHttpClient = {
  //    val manager: PoolingClientConnectionManager = new PoolingClientConnectionManager()
  //    manager.setMaxTotal(5)
  //    manager.setDefaultMaxPerRoute(5)
  //    val httpclient = new DefaultHttpClient(manager);
  //    HttpConnectionParams.setConnectionTimeout(httpclient.getParams, 10000);
  //    HttpConnectionParams.setSoTimeout(httpclient.getParams, 10000);
  //    httpclient
  //  }

  private val _client: CloseableHttpAsyncClient = HttpAsyncClientBuilder.create()
    .setMaxConnTotal(500)
    .setMaxConnPerRoute(500)
    .setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(1).build())
    .setDefaultRequestConfig(RequestConfig.custom()
    .setConnectionRequestTimeout(15000)
    .setConnectTimeout(15000)
    .setSocketTimeout(15000)
    .build()
    ).build()


  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  //  private val execcontext = new ScalaExecutorService("OSMTilesCache",1,10,false,10,TimeUnit.MINUTES,
  //    new ArrayBlockingQueue[Runnable](5))

  private def client = {
    if (!_client.isRunning)
      _client.start()
    _client
  }

  def close(): Unit = client.close()

  def send(ip: String, port: Int, gps: GPSData): Future[(Int, String)] = try {
    //    val formatter = org.joda.time.format.ISODateTimeFormat.dateTimeNoMillis().
//    val dt = formatter.print(new org.joda.time.DateTime(gps.time))
    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val dt = df.format(gps.time)
    //debug("dt="+dt)
    val s = s"""<?xml version="1.0" encoding="windows-1251"?>
        <soapenv:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope">
        <soapenv:Header/>
        <soapenv:Body>
        <ws:PutCoord>
        <ObjectID>${gps.imei}</ObjectID>
        <Coord time="$dt" lon="${gps.lon}" lat="${gps.lat}" alt="${gps.data.getOrElse("alt", 0)}" speed="${gps.speed}" dir="${gps.course}" valid="1" motion="0"/>
        <AddInfo online="1"/></ws:PutCoord>
        </soapenv:Body>
        </soapenv:Envelope>"""
    //debug("NisRealtimeRetranslator sending " + gps + "s=" + s)
    val post = new HttpPost(s"http://$ip:$port")
    //val post = new HttpPost("http://91.230.151.33:6013")


    post.setEntity(new StringEntity(
      s
     // "<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n<soapenv:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope\"><soapenv:Header/><soapenv:Body><ws:PutCoord><ObjectID>354330031021861</ObjectID><Coord time=\"2014-10-01T17:52:42Z\" lon=\"37.625670\" lat=\"55.690451\" alt=\"158.000000\" speed=\"0\" dir=\"0\" valid=\"1\" motion=\"0\"/><AddInfo online=\"1\"/></ws:PutCoord></soapenv:Body></soapenv:Envelope>"
    ))

    val result = scala.concurrent.promise[(Int, String)]()

    client.execute(post, new FutureCallback[HttpResponse] {

      override def cancelled(): Unit = result.failure(new IllegalStateException("cancelled"))

      override def completed(response1: HttpResponse): Unit = {
        val statusLine = response1.getStatusLine()
        //debug("status=" + statusLine + " gps=" + gps);
        val entity1 = response1.getEntity();
        val resp: String = EntityUtils.toString(entity1)
        //debug("resp=" + resp + " gps=" + gps);
        result.success((statusLine.getStatusCode, resp))
      }

      override def failed(p1: Exception): Unit = result.failure(p1)
    });

    result.future
  } catch {
    case e: Throwable => Future.failed(e)
  }
}
