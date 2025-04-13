package ru.sosgps.wayrecall.tools.wialon.webclient

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.params.HttpConnectionParams
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.{NameValuePair, HttpResponse}
import org.apache.http.util.EntityUtils
import org.apache.http.client.{HttpClient, CookieStore}
import java.util
import org.apache.http.message.BasicNameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import scala.collection.JavaConversions.collectionAsScalaIterable
import java.io.Closeable
import java.util.concurrent.ThreadFactory

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 12.08.13
 * Time: 16:37
 * To change this template use File | Settings | File Templates.
 */
class WialonAdminHttpClient(wialonUrl: String, user: String, password: String){

  import java.util.concurrent.{SynchronousQueue, TimeUnit, ThreadPoolExecutor, Executors}

  import scala.concurrent.{Promise, Future, promise, Await, future, ExecutionContext}

  //TODO: use http://dispatch.databinder.net/Dispatch.html
  protected val tpool = Executors.newFixedThreadPool(10, new ThreadFactory {
    val tf = Executors.defaultThreadFactory()

    def newThread(r: Runnable) = {
      val newThread = tf.newThread(r)
      newThread.setDaemon(true)
      newThread
    }
  })

  private val executionContext = ExecutionContext.fromExecutor(tpool)

  protected val _httpClient: DefaultHttpClient = {

    val manager: PoolingClientConnectionManager = new PoolingClientConnectionManager()
    manager.setMaxTotal(100)
    manager.setDefaultMaxPerRoute(100)
    val httpclient = new DefaultHttpClient(manager);
    HttpConnectionParams.setConnectionTimeout(httpclient.getParams, 1000 * 60 * 2);
    HttpConnectionParams.setSoTimeout(httpclient.getParams, 1000 * 60 * 15);
    httpclient
  }

  def authorize() {
    val httpPost: HttpPost = new HttpPost(wialonUrl)
    val response: HttpResponse = _httpClient.execute(httpPost)
    val result: String = EntityUtils.toString(response.getEntity)
    val cookieStore: CookieStore = _httpClient.getCookieStore
    val httpPost2: HttpPost = new HttpPost(wialonUrl + "/index.html")
    val params2 = new util.ArrayList[NameValuePair]()
    params2.add(new BasicNameValuePair("login_user", user))
    params2.add(new BasicNameValuePair("login_passw", password))
    params2.add(new BasicNameValuePair("login_action", "login"))
    httpPost2.setEntity(new UrlEncodedFormEntity(params2))
    //log("authorizing with login: \"" + user + "\"...")
    val response2: HttpResponse = _httpClient.execute(httpPost2)
    val result2: String = EntityUtils.toString(response2.getEntity)
    val cookieStore2: CookieStore = _httpClient.getCookieStore

    if (cookieStore2.getCookies.exists(c => c.getName == "wal" && c.getValue == user)) {
      //log("authorized")
    }
    else
      throw new RuntimeException("cant authorize")

  }

  def requestURL(url: String): String = {
    val httpGet: HttpGet = new HttpGet(wialonUrl + "/" + url)
    val response: HttpResponse = httpClient.execute(httpGet)
    EntityUtils.toString(response.getEntity)
  }

  def requestAsync(url: String): Future[String] = future {
     requestURL(url)
  }(executionContext)


  def httpClient: HttpClient = _httpClient

}
