package ru.sosgps.wayrecall.monitoring

import java.io.{File, PrintWriter, StringWriter}
import java.net.{URLDecoder, URLEncoder}
import java.text.SimpleDateFormat
import java.util.Date
import javax.activation.{DataHandler, FileDataSource}
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}
import javax.mail.{Address, Message}
import javax.servlet.http.{Cookie, HttpServletRequest, HttpServletResponse}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import ru.sosgps.wayrecall.billing.tariff.TariffEDS
import ru.sosgps.wayrecall.core.{GPSData, ObjectsRepositoryReader}
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.monitoring.web.ReportMapGenerator
import ru.sosgps.wayrecall.regeocoding.DirectNominatimRegeocoder
import ru.sosgps.wayrecall.utils.MailSender

/**
  * Created by nickl on 27.06.14.
  */
@Controller
class AddressesReportGenerator extends grizzled.slf4j.Logging {

  @Autowired
  var packStore: PackagesStore = null

  @Autowired
  var regeocoder: DirectNominatimRegeocoder = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var mailsender: MailSender = null

  @Autowired
  var reportMapGenerator: ReportMapGenerator = null

  @Autowired
  var tariffEDS: TariffEDS = null


  def writeTable(history: Iterable[GPSData], out: PrintWriter) {


    //val imgurl = "http://b.tile.openstreetmap.org/"+zoom+"/"+tilex+"/"+tiley+".png";
    //    val imgurl = "http://b.tile.openstreetmap.org/"+
    //      OsmMercator.getTileNumber(history.head.lat,history.head.lon,14 )+".png";

    //    out.println(s"<img src='$imgurl'></img>")
    //    val names = new mutable.TreeSet[String]()

    //    val executor = new ScalaExecutorService(5, 5, 1, TimeUnit.MINUTES, new LinkedTransferQueue[Runnable](), handler = new ThreadPoolExecutor.CallerRunsPolicy)
    //    try {
    //      val ftrs = for (gps <- history /*.take(100)*/ if gps.placeName != null) yield executor.future {
    //        val position = regeocoder.getPosition(gps.lon, gps.lat, true, true)
    //        executor.synchronized {
    //          names += (position match {
    //            case Right(d) => d
    //            case Left(e) => e.toString
    //          })
    //        }
    //      }
    //
    //      ftrs.foreach(Await.result(_, 40.seconds))
    //    }
    //    finally {
    //      executor.shutdown()
    //    }

    val format1 = new SimpleDateFormat("dd.MM.yyyy")
    val format2 = new SimpleDateFormat("HH:mm:ss")
    out.println("<br><br><table style=\"width: 100%;text-align:left;font-size:11px;font-family:arial;border-collapse:collapse;\"><tr><th style=\"text-align:left;ont-weight: bold;padding:4px 3px 4px 5px;border:1px solid #d0d0d0;border-left-color:#eee;background-color:#ededed;margin:0;\" width=\"160\">День</th><th style=\"text-align:left;ont-weight: bold;padding:4px 3px 4px 5px;border:1px solid #d0d0d0;border-left-color:#eee;background-color:#ededed;margin:0;\" width=\"160\">Время</th><th style=\"text-align:left;ont-weight: bold;padding:4px 3px 4px 5px;border:1px solid #d0d0d0;border-left-color:#eee;background-color:#ededed;margin:0;\">Посещённые адреса</th></tr>")
    var curdate: String = null
    var curplace: String = null
    var odd = true

    for (gps <- history if gps.goodlonlat && gps.placeName != null) {
      val day = format1.format(gps.time)
      if (curdate != day) {
        curdate = day
        out.println("<tr><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\" colspan=\"3\"><b>" + curdate + "</b></td></tr>")
      }
      if (curplace != gps.placeName) {
        curplace = gps.placeName
        if (odd) {
          out.println("<tr style=\"background-color:#ffffff;\">")
          odd = false
        } else {
          out.println("<tr style=\"background-color: #f9f9f9;\">")
          odd = true
        }
        out.println("<td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\">&nbsp;</td><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\">" + format2.format(gps.time) + "</td><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\">" + gps.placeName + "</td></tr>")
      }
    }
    out.println("</table>")
  }


  @RequestMapping(Array("/addressReport/sendEmail"))
  def sendEmail(
                 @RequestParam("uid") uid: String, // URL prefix
                 @RequestParam("from") from: Long, // Даты
                 @RequestParam("to") to: Long,
                 request: HttpServletRequest,
                 response: HttpServletResponse) {

    //implicit def longToDate(l: Long) = new Date(l)

    response.setContentType("text/html; charset=UTF-8") // Сюда пишется подтверждение успешного POST-метода
    if (request.getParameter("action") == "sendemail") {
      val organization = request.getParameter("organization")
      val email = request.getParameter("email")
      debug("sendingemail:" + email + " organization:" + organization)

      val cookieMap = request.getCookies.map(c => (c.getName, c)).toMap.withDefault(n => new Cookie(n, ""))


      val cookie = cookieMap("addresreport.organization")
      cookie.setValue(URLEncoder.encode(organization))
      response.addCookie(cookie)
      val cookie1 = cookieMap("addresreport.email")
      cookie1.setValue(URLEncoder.encode(email))
      response.addCookie(cookie1)


      val msg = new MimeMessage(mailsender.getSession) // Формат сообщения
      msg.setFrom(new InternetAddress(mailsender.getEmail, "Wayrecall"))
      val address: Array[Address] = Array(new InternetAddress(email))
      msg.setRecipients(Message.RecipientType.TO, address)
      msg.setSubject(organization + "." + or.getUserObjectName(uid) + ". отчет по посещению адресов")
      msg.setSentDate(new Date)

      val multipart = new MimeMultipart("related");
      var messageBodyPart = new MimeBodyPart();

      val writer1 = new StringWriter()
      val writer = new PrintWriter(writer1)

      writer.println("<html style=\"border:0;margin:0;padding:0;\">")
      writeTitle(uid, writer)
      writer.println("<body style=\"border:0;margin:0;padding:0;\">")

      val (fromDate, toDate) = tariffEDS.correctReportWorkingDates(new Date(from), new Date(to))


      val history = loadHistory(uid, fromDate, toDate)

      val uuid = getUUID(uid, fromDate, toDate)
      val file: File = reportMapGenerator.loadOrGenerate(uuid, history)

      //      require(file.exists())

      writeInfo(uid, fromDate, toDate, organization, writer)
      writer.println("<img src=\"cid:image\">")
      writeTable(history, writer)
      writer.println("</body></html>")
      writer.close

      messageBodyPart.setContent(writer1.toString, "text/html; charset=UTF-8");

      multipart.addBodyPart(messageBodyPart);

      messageBodyPart = new MimeBodyPart();
      val fds = new FileDataSource(file);
      messageBodyPart.setDataHandler(new DataHandler(fds));
      messageBodyPart.setHeader("Content-ID", "<image>");

      multipart.addBodyPart(messageBodyPart);

      msg.setContent(multipart);
      mailsender.send(msg)

      val writer2 = response.getWriter
      try {
        writer2.println("<html>")
        writer2.println("Отчет отправлен на: " + email)
        writer2.println("<a href=\"" + getBaseURL(request) + "&controls=true"
          + "\">назад<a>")
        writer2.println("</html>")
      }
      finally {
        writer2.close()
      }
    }

  }

  @RequestMapping(Array("/addressReport"))
  def process(
               @RequestParam("uid") uid: String,
               @RequestParam("from") from: Long,
               @RequestParam("to") to: Long,
               request: HttpServletRequest,
               response: HttpServletResponse) {
    response.setContentType("text/html; charset=UTF-8")

    //    implicit def longToDate(l: Long) = new Date(l)

    val (fromDate, toDate) = tariffEDS.correctReportWorkingDates(new Date(from), new Date(to))

    val history = loadHistory(uid, fromDate, toDate)


    val writer = response.getWriter
    try {
      val uuid = getUUID(uid, fromDate, toDate)
      val file: File = reportMapGenerator.loadOrGenerate(uuid, history)

      //val s = request.getServletPath+Option(request.getPathInfo).getOrElse("")
      val s = request.getRequestURI()

      debug("s=" + s)
      writer.println("<html style=\"border:0;margin:0;padding:0;\">")
      writeTitle(uid, writer)
      //      writer.println("<link href=\"../print.css\" rel=\"stylesheet\" type=\"text/css\">")
      writer.println("<body style=\"border:0;margin:0;padding:0;\">")
      val ss = s.substring(0, s.indexOf("addressReport"))
      if (request.getParameter("controls") != null) {
        writeControls(uid, request, writer)
      }

      writeInfo(uid, fromDate, toDate, null, writer)
      writer.println("<img src=\"" + (ss + "staticimg/" + uuid + ".png") + "\">")
      writeTable(history, writer)
      writer.println("</body></html>")
    }
    finally {
      writer.close()
    }

  }

  private def getUUID(uid: String, from: Date, to: Date): String = {
    uid + "-" + from.getTime + "-" + to.getTime
  }

  private def loadHistory(uid: String, from: Date, to: Date): Iterable[GPSData] = {
    val orig = packStore.getHistoryFor(uid, from, to)
    val filtered = orig.filter(g => g.goodlonlat && g.speed > 3)
    if (filtered.nonEmpty) filtered else orig.take(3)
  }

  def writeInfo(uid: String, from: Date, to: Date, organization: String, writer: PrintWriter) {
    writer.println("<h3>Отчет по посещенным адресам</h3><br>")
    writer.println("<table style=\"width: 100%;text-align:left;font-size:11px;font-family:arial;border-collapse:collapse;\">")
    if (organization != null) {
      writer.println("<tr style=\"background-color: #ffffff;\"><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\">Организация:</td><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\"><b>" + organization + "</b></td></tr>")
      writer.println("<tr style=\"background-color: #f9f9f9;\"><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\">Поставщик телеметрических услуг:</td><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\"><b>КСБ \"Стелс\"</b> (<a href=\"http://www.sosgps.ru/\">www.sosgps.ru</a>)</td></tr>")
    }
    writer.println("<tr style=\"background-color: #ffffff;\"><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\">Объект:</td><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\"><b>" + or.getUserObjectName(uid) + "</b></td></tr>")
    val format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
    writer.println("<tr style=\"background-color: #f9f9f9;\"><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\">c:</td><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\"><b>" + format.format(from) + "</b></td></tr>")
    writer.println("<tr style=\"background-color: #ffffff;\"><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\">по:</td><td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\"><b>" + format.format(to) + "</b></td></tr>")
    writer.println("</table>")
  }

  private def writeControls(uid: String, request: HttpServletRequest, writer: PrintWriter) {
    writer.println("<div>")
    writer.println("<a href=\"" + getBaseURL(request)
      + "\" target=\"blank\">в отдельном окне<a>")
    val cookieMap = request.getCookies.map(c => (c.getName, c.getValue)).toMap.mapValues(URLDecoder.decode)
    debug("cookieMap=" + cookieMap)
    writer.println(<div>
      <form action={request.getRequestURI + "/sendEmail"}>
        <input type="hidden" name="uid" value={uid}/>
        <input type="hidden" name="from" value={request.getParameter("from")}/>
        <input type="hidden" name="to" value={request.getParameter("to")}/>
        <input type="hidden" name="action" value="sendemail"/>
        Отправить отчет от имени организации
        <input type="text" name="organization" value={cookieMap.getOrElse("addresreport.organization", "")}/>
        на адрес электронной почты:
        <input type="text" name="email" value={cookieMap.getOrElse("addresreport.email", "")}/>
        <input type="submit" name="send" value="Отправить"/>
      </form>
    </div>)
    writer.println("</div>")
  }

  private def writeTitle(uid: String, writer: PrintWriter) {
    writer.println("<title>Посещенные адреса " + or.getUserObjectName(uid) + "</title>")
  }

  def getBaseURL(request: HttpServletRequest): String = {
    request.getServletPath + "/addressReport" + "?" +
      "uid=" + URLEncoder.encode(request.getParameter("uid")) +
      "&from=" + URLEncoder.encode(request.getParameter("from")) +
      "&to=" + URLEncoder.encode(request.getParameter("to"))
  }


}

