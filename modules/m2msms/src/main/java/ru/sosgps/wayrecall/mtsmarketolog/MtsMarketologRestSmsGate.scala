package ru.sosgps.wayrecall.mtsmarketolog

import com.google.common.base.Charsets
import grizzled.slf4j.Logging
import org.apache.commons.io.IOUtils
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.config.{ConnectionConfig, SocketConfig}
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{DefaultHttpClient, HttpClientBuilder}
import org.apache.http.impl.conn.{PoolingClientConnectionManager, PoolingHttpClientConnectionManager}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.params.HttpConnectionParams
import org.apache.http.util.EntityUtils
import org.eclipse.jetty.server.{HandlerContainer, Request}
import org.eclipse.jetty.server.handler.{AbstractHandler, ContextHandler, HandlerCollection}
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import ru.sosgps.wayrecall.initialization.MultiserverConfig
import ru.sosgps.wayrecall.sms.{SMS, SmsGate, SmsListener}
import ru.sosgps.wayrecall.utils.{parseDate, typingMap}
import ru.sosgps.wayrecall.utils.web.{ScalaCollectionJson, ScalaJsonObjectMapper}

import java.io.{ByteArrayInputStream, InputStream, PrintWriter}
import java.util
import java.util.{Date, Objects, UUID}
import javax.annotation.Resource
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.beans.BeanProperty

class MtsMarketologRestSmsGate extends SmsGate with Logging {

  @BeanProperty
  var connTimeout = 50000

  @BeanProperty
  @Value("${instance.mtsmarketolog.login}")
  var login: String = null

  @BeanProperty
  @Value("${instance.mtsmarketolog.password}")
  var password: String = null

  protected lazy val httpclient = {
    val manager = new PoolingHttpClientConnectionManager()
    manager.setMaxTotal(5)
    manager.setDefaultMaxPerRoute(5)

    val config = RequestConfig.custom()
      .setConnectTimeout(connTimeout)
      .setConnectionRequestTimeout(connTimeout)
      .setSocketTimeout(connTimeout).build();

    HttpClientBuilder.create().setDefaultRequestConfig(config).build()
  }

  private val mapper = new ScalaJsonObjectMapper()

  override def sendSms(phone: String, text: String): SMS = {
    val sentSMSID = sendSmsRest(phone, text)
    val sms = new SMS(sentSMSID, text, phone)
    sms.status = SMS.SENT
    smsListener.onSmsSent(sms)
    sms
  }

  private def sendSmsRest(phone: String, text: String): Long = {
    val httpPost = new HttpPost("https://omnichannel.mts.ru/http-api/v1/messages")
    import org.apache.http.impl.auth.BasicScheme
    val creds = new UsernamePasswordCredentials(login, password);
    httpPost.addHeader(new BasicScheme().authenticate(creds, httpPost, null))

    val idGenerated = UUID.randomUUID().getLeastSignificantBits
    val qyeryText = mapper.generate(Map("messages" -> Seq(
      Map(
        "class" -> 1,
        "from" -> Map("sms_address" -> "79166400536"),
        "to" -> Seq(
          Map("msisdn" -> phone, "message_id" -> idGenerated.toHexString)
        ),
        "content" -> Map("short_text" -> text)
      )
    )))
    debug("qyeryText = " + qyeryText)
    httpPost.setEntity(new StringEntity(qyeryText, ContentType.APPLICATION_JSON))
    val response = httpclient.execute(httpPost)
    try {
      debug("response.getStatusLine = " + response.getStatusLine)
      debug("response.getEntity = " + EntityUtils.toString(response.getEntity))
      idGenerated
    }
    finally {
      response.close()
    }
  }

  private[this] var smsListener: SmsListener = new SmsListener {

    def onSmsReceived(smses: Seq[SMS]) = {
      debug("no sms listeners to process onSmsReceived: " + smses)
    }

    def onSmsSentBatch(smses: Seq[SMS]) = {
      debug("no sms listeners to process onSmsSent:" + smses)
    }

    def onSmsStatusChanged(smses: Seq[SMS]) = {
      debug("no sms listeners to process onSmsStatusChanged:" + smses)
    }

    override def onSmsSent(sms: SMS): Unit = {
      debug("no sms listeners to process onSmsSent:" + sms)
    }
  }

  def setSmsListener(l: SmsListener) = {
    smsListener = Objects.requireNonNull(l, "listener must not be null")
  }

  override def getSmsListener: SmsListener = smsListener

  @BeanProperty
  @Value("${instance.name}")
  var instanceName: String = null

  @Resource(name = "multiserverConfig")
  var mscfg: MultiserverConfig = null

  @Bean
  def httpListener(): org.eclipse.jetty.server.Handler = {
    val myHandler = new AbstractHandler {
      override def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse): Unit = {
        info("MtsMarketologRestSmsGate listener called " + target
          + " reqest = " + request.getParameterMap)
        val inputStream = request.getInputStream
        var body: String = null
        try {
          try {
            body = IOUtils.toString(inputStream)
            info("req body = " + body)
            val parsed = parseIncomingBody(new ByteArrayInputStream(body.getBytes(Charsets.UTF_8)))
            info("req parsed = " + parsed)
            smsListener.onSmsReceived(parsed.incomingSms)
            smsListener.onSmsStatusChanged(parsed.statusChanged)
            response.setStatus(200)
          } catch {
            case e: Exception =>
              warn(s"error handling request with body $body", e)
              response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage)
          }
        }
        finally {
          inputStream.close()
        }
        baseRequest.setHandled(true)
      }
    }
    val handler = new ContextHandler("/receivesms")
    handler.setHandler(myHandler)
    val maybeConfig = mscfg.instances.find(_.name == instanceName)
    handler.addVirtualHosts(maybeConfig.map(_.vhosts).getOrElse(Array.empty))
    handler.setAllowNullPathInfo(true)
    handler
  }

  private[mtsmarketolog] def parseIncomingBody(body: InputStream): IncomingInfo = {
    val value = ScalaCollectionJson.parse[Map[String, AnyRef]](body)
    val messagesJsonOpt = value.getAs[Seq[Map[String, Any]]]("messages")
    val incoming = messagesJsonOpt.toSeq.flatMap(_.map(message => {
      val text = message.as[Map[String, Any]]("content").as[String]("text")
      val from = message.as[Map[String, Any]]("from")
      val phone = from.as[String]("user_contact")
      val received_at = new Date(from.as[Int]("received_at") * 1000L)
      val id = UUID.fromString(message.as[String]("internal_id"))
      new SMS(id.getLeastSignificantBits, text, true, phone, "", received_at, received_at, SMS.DELIVERED)
    }))
    val items = value.getAs[Seq[Map[String, Any]]]("items").toSeq.flatMap(_.flatMap(message => {
      val destination = message.as[String]("destination")
      val received_at = parseDate(message.as[String]("received_at"))
      val id =  BigInt(message.as[String]("message_id").toUpperCase, 16).longValue()
      val status = message.as[Int]("status")
      if (status == 300)
        Some(new SMS(id, "<no-content>", false, "", destination, received_at, received_at,
          SMS.DELIVERED))
      else
        None
    }))
    IncomingInfo(incoming, items)
  }

  private[mtsmarketolog] case class IncomingInfo(incomingSms: Seq[SMS], statusChanged: Seq[SMS])

}

object MtsMarketologRestSmsGate {
  def main(args: Array[String]): Unit = {
    new MtsMarketologRestSmsGate().sendSms("79836362347", "example text")
  }
}
