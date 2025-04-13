package ru.sosgps.wayrecall.tools.wialon.packaging

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.HttpConnectionParams
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.{NameValuePair, HttpResponse}
import org.springframework.http.HttpEntity
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.util.EntityUtils
import java.io.{PrintWriter, FileWriter, File, FileOutputStream}
import org.apache.http.client.{HttpClient, CookieStore}
import org.apache.http.message.BasicNameValuePair
import java.util
import scala.collection.JavaConversions._
import scala.util.matching.Regex.Match
import collection.mutable
import util.{Locale, Date}
import java.text.{SimpleDateFormat, DateFormat}
import ru.sosgps.wayrecall.utils.attempts.{Skip, Retry, Attempts}
import com.beust.jcommander.{Parameters, JCommander, Parameter}
import scala.Array
import ru.sosgps.wayrecall.jcommanderutils.{CliCommand, JCommanderCreator}
import ru.sosgps.wayrecall.utils.web.ScalaJson


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 03.12.12
 * Time: 21:28
 * To change this template use File | Settings | File Templates.
 */

case class WialonItem(val name: String, val id: String, val uid: String)

class UnexpectedResponseException(str: String, val response: String) extends RuntimeException(str + " \"" + response + "\"")


@Parameters(commandDescription = "Downloads packages data from wialon server")
class WialonExporter extends CliCommand {

  def log(x: String) = {
    println(new Date() + " " + x);
  }

  private[tools] var httpClient: DefaultHttpClient = {
    val client: DefaultHttpClient = new DefaultHttpClient
    HttpConnectionParams.setConnectionTimeout(client.getParams, 1000 * 60 * 2);
    HttpConnectionParams.setSoTimeout(client.getParams, 1000 * 60 * 15);
    client
  }

  private[this] def parseDate(str: String): Date = {
    ru.sosgps.wayrecall.utils.parseDate(str)
  }

  @Parameter(names = Array("--wialonUrl"), description = " Wialon Server URL")
  var wialonUrl = "http://b3.sosgps.ru:8022"

  @Parameter(names = Array("-l"), description = "server login")
  var user = "0011"

  @Parameter(names = Array("-p"), description = "server password")
  val password = "0011"

  @Parameter(names = Array("-f", "--from"), description = "from date (format 2013-08-28T00:00)", required = true)
  var from: String = null

  @Parameter(names = Array("-t", "--to"), description = "to date (format 2013-08-28T00:00)", required = true)
  var to: String = null

  @Parameter(names = Array("-d", "--dir"), description = "output dir")
  var dir: String = null

  @Parameter(names = Array("--skip"))
  var skip: Int = 0

  @Parameter(names = Array("--limit"))
  var limit: Int = Int.MaxValue

  @Parameter(names = Array("--noarh"))
  var noarh: Boolean = false

  @Parameter(names = Array("-h", "--help"), help = true)
  var help = false;


  def loadWlnsFromWialon() {


    val from = parseDate(this.from)
    val to = parseDate(this.to)

    println("from=" + from)
    println("to=" + to)

    val dir = new File(Option(this.dir).getOrElse({
      val dateformat = new SimpleDateFormat("yyyyMMddHHmmss")
      "wlns" + dateformat.format(from) + "-" + dateformat.format(to)
    }))

    dir.mkdirs();

    require(dir.exists() && dir.isDirectory, dir.getAbsolutePath + " is not a directory")

    authorize()

    log("loading items")
    val wialonItems = getWialonItems()

    val itemsfile = new File(dir, "wialonItems.json")
    log("writing items to " + itemsfile)
    ScalaJson.generate(wialonItems, itemsfile)
    log("written")

    for ((item, i) <- wialonItems.zipWithIndex.drop(this.skip).take(this.limit)) {

      new Attempts(
        skipAfterRetries = true,
        maxRetryAttempts = 3,
        handler = {
          case e: UnexpectedResponseException => {
            log(item.name + " returned " + e.toString)
            if (e.response == "Not authorized") {
              authorize();
              Retry
            }
            else {
              Skip
            }
          }
          case e: java.net.SocketTimeoutException => {
            log(e.toString)
            log("retrying")
            Retry
          }
          case e: Exception => {
            log(e.toString)
            e.printStackTrace()
            Skip
          }
        }
      ).doTry {
        log("loading " + "(" + (i + 1) + "/" + wialonItems.size + ") \"" + item.name + "\"...")
        writeWialonDateFile(item, from, to, dir)
        log("\"" + item.name + "\" loaded")
      }

    }
  }

  private[this] def writeWialonDateFile(item: WialonItem, from: Date, to: Date, dir: File) {
    val fileGet: HttpGet = new HttpGet(
      wialonUrl + "/messages_filter/export_msgs.html?fmt=wln&id=" + item.id + "&from=" + from.getTime / 1000 + "&to=" + to.getTime / 1000 + "&arh=" + (if (noarh) 0 else 1)
    )
    log("connecting...")
    val fileresponse: HttpResponse = httpClient.execute(fileGet)
    log("connected")

    val contendDisposition = fileresponse.getHeaders("Content-Disposition").headOption.map(_.getValue).getOrElse({
      throw new UnexpectedResponseException("server response error:", EntityUtils.toString(fileresponse.getEntity))
    })
    val fname: String = """filename="(.*)"""".r.findFirstMatchIn(contendDisposition).get.group(1).replaceAll( """[\\/]""", "_")

    log("writing file...")
    val entity = fileresponse.getEntity
    try {
      val uidFile = new PrintWriter(new File(dir, fname + ".uid"), "UTF8")
      uidFile.println(item.uid)
      uidFile.close()
      entity.writeTo(new FileOutputStream(new File(dir, fname)))
      log("written")
    }
    finally {
      entity.getContent().close();
    }
  }

  def getWialonItems(): Seq[WialonItem] = {
    val httpGet: HttpGet = new HttpGet(wialonUrl + "/service.html")
    val response: HttpResponse = httpClient.execute(httpGet)
    val result: String = EntityUtils.toString(response.getEntity)


    val wlnintiparams = """(?m)Wialon\.init_state_impl\((.*)\);\s*\/\/ rebrand hardware types\s*WebCMS.*rebrand_hw\(\{\}\);""".r

    val m: Match = wlnintiparams.findFirstMatchIn(result).get

    val json: Map[String, AnyRef] = ScalaJson.parse[Map[String, AnyRef]](m.group(1))

    val wialonItems: mutable.Buffer[WialonItem] = json("items").asInstanceOf[util.List[util.Map[String, AnyRef]]].collect({
      case e if (e.get("cls") == 3) => new WialonItem(
        name = e.get("nm").toString,
        id = e.get("id").toString,
        uid = e.get("uid").toString
      )
    })

    wialonItems
  }

  def authorize() {
    val httpPost: HttpPost = new HttpPost(wialonUrl + "/login_action.html")
    val response: HttpResponse = httpClient.execute(httpPost)
    val result: String = EntityUtils.toString(response.getEntity)
    val cookieStore: CookieStore = httpClient.getCookieStore
    val httpPost2: HttpPost = new HttpPost(wialonUrl + "/login_action.html")
    val params2 = new util.ArrayList[NameValuePair]()
    params2.add(new BasicNameValuePair("user", user))
    params2.add(new BasicNameValuePair("passw", password))
    params2.add(new BasicNameValuePair("lang", "ru"))
    params2.add(new BasicNameValuePair("submit", "Войти"))
    params2.add(new BasicNameValuePair("action", "login"))
    httpPost2.setEntity(new UrlEncodedFormEntity(params2))
    log("authorizing with login: \"" + user + "\"...")
    val response2: HttpResponse = httpClient.execute(httpPost2)
    val result2: String = EntityUtils.toString(response2.getEntity)
    val cookieStore2: CookieStore = httpClient.getCookieStore

    if (cookieStore2.getCookies.exists(c => c.getName == "wwl" && c.getValue == user)) {
      log("authorized")
    }
    else
      throw new RuntimeException("cant authorize")

  }

  val commandName = "wlnexport"

  def process() {
    loadWlnsFromWialon()
  }
}

