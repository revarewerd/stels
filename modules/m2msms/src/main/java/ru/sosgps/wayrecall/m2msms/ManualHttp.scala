package ru.sosgps.wayrecall.m2msms

import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpConnectionParams
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.StringEntity
import java.security.MessageDigest
import org.apache.http.util.EntityUtils
import org.apache.http.{NameValuePair, HttpResponse}
import org.apache.http.client.utils.URLEncodedUtils
import scala.collection.JavaConversions.seqAsJavaList
import org.apache.http.message.BasicNameValuePair

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 21.03.13
 * Time: 20:03
 * To change this template use File | Settings | File Templates.
 */
object ManualHttp {

  private[this] lazy val httpclient = {
    val manager: PoolingClientConnectionManager = new PoolingClientConnectionManager()
    manager.setMaxTotal(100)
    manager.setDefaultMaxPerRoute(100)
    val httpclient = new DefaultHttpClient(manager);
    //HttpConnectionParams.setConnectionTimeout(httpclient.getParams, connTimeout);
    //HttpConnectionParams.setSoTimeout(httpclient.getParams, connTimeout);
    httpclient
  }

  def main0(args: Array[String]): Unit = {
//    val params = Seq("lat" -> lat, "lon" -> lon) ++ defaultparams
//    val paramstr = params.map(p => p._1 + "=" + p._2).mkString("&")

    val userName: String = "Stels"
    //var password: String = "934388"
    var password: String = "e143f4685aa83d552c886a2a897a0c3c"
    //password = new String(MessageDigest.getInstance("MD5").digest(password.getBytes))



    val params = URLEncodedUtils.format(Seq(
//      new BasicNameValuePair("msid","79164108305"),
//      new BasicNameValuePair("message","string"),
//      new BasicNameValuePair("naming","string"),
      new BasicNameValuePair("login",userName),
      new BasicNameValuePair("password",password)
    ),"UTF-8")

    val httpGet = new HttpGet("http://www.mcommunicator.ru"+"/m2m/m2m_api.asmx/GetStatistics?"+params);

    logResponse(httpclient.execute(httpGet))
  }


  def main(args: Array[String]): Unit = {

    val userName: String = "Stels"
    var password: String = "934388"
    //var password: String = "e143f4685aa83d552c886a2a897a0c3c"
    password = ru.sosgps.wayrecall.utils.io.Utils.toHexString(MessageDigest.getInstance("MD5").digest(password.getBytes), "")

    println(password)

    val data =  """<?xml version="1.0" encoding="utf-8"?>
                  <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                  xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                  <GetStatistics xmlns="http://mcommunicator.ru/M2M">
                  <login>"""+userName+"""</login>
                  <password>"""+password+"""</password>
                  </GetStatistics>
                  </soap:Body>
                  </soap:Envelope>
                """


    val post = new HttpPost("http://www.mcommunicator.ru/m2m/m2m_api.asmx")
    post.setHeader("Content-Type","text/xml; charset=utf-8");
    //post.setHeader("Content-Length",""+data.length)
    post.setHeader("SOAPAction","http://mcommunicator.ru/M2M/GetStatistics")


    post.setEntity(new StringEntity(data,"utf-8"))

    val response1 = httpclient.execute(post);

    logResponse(response1)


  }


  def logResponse(response1: HttpResponse) {
    println("status=" + response1.getStatusLine());
    val entity1 = response1.getEntity();

    val resp: String = EntityUtils.toString(entity1)

    EntityUtils.consume(entity1);

    println("resp=" + resp);
  }
}
