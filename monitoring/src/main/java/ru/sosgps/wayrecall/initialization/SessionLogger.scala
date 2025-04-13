package ru.sosgps.wayrecall.initialization

import java.io._
import java.text.SimpleDateFormat
import java.util.Date
import javax.annotation.PreDestroy
import javax.servlet.http.Cookie


import org.eclipse.jetty.server.session.AbstractSession
import org.springframework.security.core.context.SecurityContext
import ru.sosgps.wayrecall.utils.concurrent.BlockingActor
import ru.sosgps.wayrecall.utils.io.{RollingFileOutputStream, DboWriter}

import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
 * Created by nickl on 19.01.15.
 */
class SessionLogger(val dir: File) extends BlockingActor[(String, CharSequence)] {

  dir.mkdirs()

  def log(comment: String, session: AbstractSession): Unit = log(comment, session, stacktrace = false)

  def log(comment: String, session: AbstractSession, stacktrace: Boolean) {

    val maybeRequest = Option(JettyServer.getCurrentRequest)
    val id = maybeRequest.map(_.getRemoteAddr).getOrElse("unknown")

    val str: StringWriter = new StringWriter(512)
    val printWriter: PrintWriter = new PrintWriter(str)
    try {
      printWriter.println(comment)
      if (stacktrace) new Exception("stacktrace queried").printStackTrace(printWriter)
      printWriter.println("request: " + maybeRequest.map(_.getRequestURI).orNull)
      printWriter.println("cookies: " +
        maybeRequest.flatMap(r => Option(r.getCookies))
          .map(_.map(cookieToString).mkString("\n    ", "\n    ", ""))
          .orNull
      )
      printWriter.println("session is: " + session)
      if (session != null) {
        printWriter.println("valid:" + session.isValid)
        printWriter.println("id:" + session.getId)
        printWriter.println("last accessed:" + session.getLastAccessedTime + " " + new Date(session.getLastAccessedTime))
        val sc: SecurityContext = session.getAttribute("SPRING_SECURITY_CONTEXT").asInstanceOf[SecurityContext]
        if (sc != null) printWriter.println("user:" + sc.getAuthentication.getName + " " + sc.getAuthentication.getDetails)
        printWriter.println("sess content:")
        import scala.collection.JavaConversions._
        for (s <- session.getNames) {
          printWriter.println("    " + s)
        }
      }
    }
    catch {
      case e: Exception => e.printStackTrace(printWriter)
    }
    finally {
      printWriter.close()
    }

    accept(id, str.getBuffer)

  }

  private def cookieToString(cookie: Cookie) = {
    import cookie._
    ListMap(
      "name" -> getName,
      "domain" -> getDomain,
      "path" -> getPath,
      "value" -> getValue,
      "maxAge" -> getMaxAge,
      "comment" -> getComment
    ).filter(_._2 != null).mkString("Cookie(", ", ", ")")
  }

  val actorThreadName = "SessionLogger"

  private val writers = new mutable.HashMap[String, PrintWriter]()
  private val fileDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS")
  private val logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS")

  override protected def processMessage(value: (String, CharSequence)): Unit = {

    //debug("processMessage "+value._1)

    val (id, data) = value

    val writer = getWriter(id)
    writer.print(logDateFormat.format(new Date()))
    writer.print(" (qs=")
    writer.print(qsize)
    writer.print(") - ")
    writer.append(data)
    writer.println()
    writer.flush()


  }

  private def getWriter(id: String): PrintWriter = {
    writers.get(id) match {
      case Some(writer) => writer
      case None => {
        val newWriter = makeNewWriter(id)
        writers.put(id, newWriter)
        newWriter
      }
    }
  }

  private[this] def makeNewWriter(id: String) = {

    val file = new File(dir, "ses-" + id + "-" + fileDateFormat.format(new Date()) + ".txt")
    debug("making new writer: " + file.getAbsolutePath)
    //new RollingFileWriter(file.getAbsolutePath, 1024L, 5000, RollingFileWriter.Compression.COMPRESS_BACKUPS)
    //new PrintWriter(new OutputStreamWriter(new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(file))), "UTF-8"))
    new PrintWriter(new OutputStreamWriter(new RollingFileOutputStream(file.getAbsolutePath), "UTF-8"))
    //new DboWriter(new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(file))))
  }

  @PreDestroy
  override def stop(): Unit = {
    super.stop()
    debug("closing writers")
    writers.values.foreach(_.close())
  }
}
