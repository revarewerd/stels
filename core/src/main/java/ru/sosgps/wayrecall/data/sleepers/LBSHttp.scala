package ru.sosgps.wayrecall.data.sleepers

import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.StringEntity
import org.apache.http.params.{BasicHttpParams, HttpConnectionParams}
import org.apache.http.util.EntityUtils
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair

import scala.collection.JavaConverters._
import ru.sosgps.wayrecall.utils.web.ScalaJson
import ru.sosgps.wayrecall.utils.funcLoadingCache
import java.util

import org.apache.http.client.utils.URLEncodedUtils

import scala.xml.XML
import scala.beans.BeanProperty
import com.google.common.cache.CacheBuilder
import java.util.concurrent.TimeUnit

import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager

import scala.Some
import java.util.Date
import javax.annotation.PostConstruct

import scala.concurrent.blocking


/**
  * Created with IntelliJ IDEA.
  * User: nickl
  * Date: 25.11.13
  * Time: 21:01
  * To change this template use File | Settings | File Templates.
  */
class LBSHttp extends LBSConverter with grizzled.slf4j.Logging {

  import ru.sosgps.wayrecall.utils.typingMapJava

  @BeanProperty
  var connTimeout = 50000

  @BeanProperty
  var method = "yandex"

  override def sourceName: String = method

  protected lazy val httpclient = {
    val manager: PoolingClientConnectionManager = new PoolingClientConnectionManager()
    manager.setMaxTotal(5)
    manager.setDefaultMaxPerRoute(5)
    val httpclient = new DefaultHttpClient(manager);
    HttpConnectionParams.setConnectionTimeout(httpclient.getParams, connTimeout);
    HttpConnectionParams.setSoTimeout(httpclient.getParams, connTimeout);
    httpclient
  }

  private val lonlatCache = CacheBuilder.newBuilder()
    .expireAfterWrite(3, TimeUnit.MINUTES)
    //.buildWithFunction(yandex)
    .buildWithFunction((m: LBS) => methodsMap(method)(m))

  val methodsMap = Map("yandex" -> yandex _, "ultrastar" -> ultrastar _)

  def convertLBS(lbs: LBS): LBSLonLat = lonlatCache(lbs)

  protected[data] def yandex(lbs: LBS): LBSLonLat = blocking {
    val post = new HttpPost("http://api.lbs.yandex.net/geolocation")
    try {

      val params = List(new BasicNameValuePair("json", ScalaJson.generate(
        Map(
          "common" -> Map(
            "version" -> "1.0",
            "api_key" -> "AJQd1lUBAAAAU1m9EAMAgAaCK6VTGKMYCsMc63Ip6aNaK2cAAAAAAAAAAAB2df7dOocrZ1d3xyuIujPAhX_TDQ=="
          ),
          "gsm_cells" -> List(Map(
            "countrycode" -> lbs.MCC,
            "operatorid" -> lbs.MNC,
            "cellid" -> lbs.CID,
            "lac" -> lbs.LAC
          )),
          "ip" -> Map(
            "address_v4" -> "176.195.98.107"
          )
        )
      )))
      //println(params)
      post.setEntity(new UrlEncodedFormEntity(params.asJava))
      debug(s"sending post query: $post with params: $params")
      val response1 = httpclient.execute(post);

      val statusLine = response1.getStatusLine()
      debug("status=" + statusLine);

      val entity1 = response1.getEntity();

      val resp: String = EntityUtils.toString(entity1)
      if (statusLine.getStatusCode != 200)
        throw new IllegalArgumentException("unexpected response=" + resp)

      val parsedresp = ScalaJson.parse[Map[String, util.Map[String, Any]]](resp)

      val pos = parsedresp("position")
      LBSLonLat(pos.as[Double]("longitude"), pos.as[Double]("latitude"), pos.get("precision") match { case d: Double => d; case i: Int => i.toDouble })
    }
    finally {
      post.releaseConnection()
    }
  }


  protected[data] def openCellId(lac: Int, cellId: Int, mcc: Int, mnc: Int): LBSLonLat = blocking {
    val params = URLEncodedUtils.format(List(
      new BasicNameValuePair("key", "1604dfa5ac4d64df0945327fb764b0b7"),
      new BasicNameValuePair("mnc", mnc.toString),
      new BasicNameValuePair("mcc", mcc.toString),
      new BasicNameValuePair("lac", lac.toString),
      new BasicNameValuePair("cellid", cellId.toString)
    ).asJava, "utf-8")


    val get = new HttpGet("http://www.opencellid.org/cell/get?" + params)
    try {

      println(get.getRequestLine)

      val result = httpclient.execute(get)

      println(result.getStatusLine)

      val resp = EntityUtils.toString(result.getEntity)
      println(resp)
      val cell = ((XML.loadString(resp)) \ "cell").head

      val lat = cell.attribute("lat").get.head.text.toDouble
      val lon = cell.attribute("lon").get.head.text.toDouble
      val range = cell.attribute("range").get.head.text.toDouble

      //println(resp)
      LBSLonLat(lon, lat, range)
    }
    finally {
      get.releaseConnection()
    }
  }

  protected[data] def ultrastar(lbs: LBS): LBSLonLat = {
    val post = new HttpPost("http://lbs.ultrastar.ru/")
    try {

      val params = List(
        new BasicNameValuePair("mcc", lbs.MCC.toString),
        new BasicNameValuePair("mnc", lbs.MNC.toString),
        new BasicNameValuePair("lac", lbs.LAC.toHexString),
        new BasicNameValuePair("cid", lbs.CID.toHexString),
        new BasicNameValuePair("submit", "Получить координаты")
      )

      post.setEntity(new UrlEncodedFormEntity(params.asJava))


      val response1 = httpclient.execute(post);

      println("status=" + response1.getStatusLine());
      val entity1 = response1.getEntity();

      val resp: String = EntityUtils.toString(entity1)
      //println(resp)

      val latr = """lat: (-?\d+.\d+)""".r
      val lonr = """lon: (-?\d+.\d+)""".r

      val (lat, lon) = (for (latm <- latr.findFirstMatchIn(resp);
                             lonm <- lonr.findFirstMatchIn(resp))
        yield (latm.group(1).toDouble, lonm.group(1).toDouble)).get

      LBSLonLat(lon, lat, -1.0)
    }
    finally {
      post.releaseConnection()
    }
  }

}

case class LBSLonLat(lon: Double, lat: Double, range: Double)

trait LBSConverter {
  def convertLBS(lbs: LBS): LBSLonLat

  def sourceName: String

}

object LBSConverter {
  def apply(f: (LBS) => LBSLonLat) = new LBSConverter {
    override def convertLBS(lbs: LBS): LBSLonLat = try f(lbs) catch {
      case e: Exception => throw new IllegalArgumentException(s"internal function cant process ${lbs.toHexString}", e)
    }

    override def sourceName: String = "func"
  }
}

class MongoStoredLBSConverter extends LBSConverter with grizzled.slf4j.Logging {

  import com.mongodb.casbah.Imports._

  @Autowired(required = false)
  @BeanProperty
  var mdbm: MongoDBManager = null

  @BeanProperty
  var innerConverter: LBSConverter = null


  override def sourceName: String = s"localdb(${innerConverter.sourceName})"

  private def lbsesCollection = mdbm.getDatabase()("lbses")

  @PostConstruct
  def init() {
    lbsesCollection.ensureIndex(MongoDBObject("lbs.CID" -> 1, "lbs.LAC" -> 1, "lbs.MCC" -> 1, "lbs.MNC" -> 1))
  }

  override def convertLBS(lbs: LBS): LBSLonLat = try blocking {

    val lbsQuery = MongoDBObject(
      "CID" -> lbs.CID,
      "LAC" -> lbs.LAC,
      "MCC" -> lbs.MCC,
      "MNC" -> lbs.MNC
    )

    val dataOpt = lbsesCollection.findOne(MongoDBObject("lbs" -> lbsQuery))

    dataOpt match {
      case Some(mongoData) => {
        val llobj = mongoData.as[BasicDBObject]("lonlat")
        LBSLonLat(
          lon = llobj.as[Double]("lon"),
          lat = llobj.as[Double]("lat"),
          range = llobj.as[Double]("range")
        )
      }

      case None => {
        debug(s"lbs $lbs was not found in mongo trying ${innerConverter.sourceName}")
        val r = innerConverter.convertLBS(lbs)
        lbsesCollection.insert(MongoDBObject(
          "lbs" -> lbsQuery,
          "lonlat" -> MongoDBObject(
            "lon" -> r.lon,
            "lat" -> r.lat,
            "range" -> r.range
          ),
          "date" -> new Date(),
          "source" -> innerConverter.sourceName
        ))
        r
      }

    }

  } catch {
    case e: Exception =>
      error(s"exception processing $lbs", e)
      throw new IllegalArgumentException(s"cant process ${lbs.toHexString} sourceName=" + sourceName, e)
  }
}
