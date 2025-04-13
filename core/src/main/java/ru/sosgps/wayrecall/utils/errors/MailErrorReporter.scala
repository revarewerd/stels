package ru.sosgps.wayrecall.utils.errors

import javax.servlet.http.HttpServletRequest

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.sosgps.wayrecall.utils.web._
import ru.sosgps.wayrecall.utils.{funcLoadingCache, MailSender}
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import scala.Predef
import scala.annotation.tailrec
import scala.concurrent.duration.Duration


class MailErrorReporter extends grizzled.slf4j.Logging {
  private[errors] var minEmailInterval: Int = 30
  private var mailSender: MailSender = null
  private var emailAddress: String = null
  private var es: ScalaExecutorService = new ScalaExecutorService("emailNotification", 0, 1, true, 5, java.util.concurrent.TimeUnit.MINUTES, new LinkedBlockingQueue[Runnable](100), new ThreadPoolExecutor.DiscardPolicy)


  def this(mailSender: MailSender, emailAddress: String) {
    this()
    this.mailSender = mailSender
    this.emailAddress = emailAddress
    if (emailAddress != null && emailAddress.trim.isEmpty) this.emailAddress = null
  }

  def notifyError(message: String, ex: Throwable) {
    val stringWriter: StringWriter = new StringWriter
    val printWriter: PrintWriter = new PrintWriter(stringWriter)
    if (message != null) printWriter.println(message)
    val trace: String = printException(ex)
    printWriter.println(trace)
    printWriter.close
    notifyError(stringWriter.toString, trace.hashCode)
  }

  def notifyError(ex: Throwable) {
    notifyError(null, ex)
  }

  private def printException(ex: Throwable): String = {
    val stringWriter: StringWriter = new StringWriter
    val printWriter: PrintWriter = new PrintWriter(stringWriter)
    printException(ex, printWriter)
    printWriter.close
    return stringWriter.toString
  }

  private def printException(ex: Throwable, printWriter: PrintWriter) {
    printWriter.print("<pre>")
    ex.printStackTrace(printWriter)
    printWriter.println("</pre>")
    if (ex.getCause != null) {
      printWriter.println("<h3>Cause:</h3>")
      printWriter.println("<pre>")
      ex.getCause.printStackTrace(printWriter)
      printWriter.println("</pre>")
    }
  }

  private var cache: LoadingCache[Integer, AtomicLong] = CacheBuilder.newBuilder.expireAfterWrite(3, TimeUnit.HOURS).buildWithFunction(
    key => new AtomicLong(0)
  )

  def notifyError(e: Throwable, request: HttpServletRequest): Unit = {
    notifyError(printRequest(request), e)
  }

  def notifyError(request: HttpServletRequest, body: String) {
    notifyError("<p>" + printRequest(request) + "</p>" + body)
  }

  def notifyError(body: String) {
    notifyError(body, body.hashCode)
  }

  def notifyError(body: String, key: Int) {
    try {
      if (emailAddress != null && cache.get(key).getAndIncrement % 100 == 0) es.execute(new Runnable {
        def run {
          try {
            mailSender.sendEmail(emailAddress, "Ошибка Wrc", body, "text/html; charset=UTF-8")
          }
          catch {
            case e: Exception => {
              logger.warn("error in emailing:", e)
            }
          }
        }
      })
    }
    catch {
      case e: ExecutionException => {
        logger.error("ExecutionException", e)
      }
    }
  }


  private def printRequest(request: HttpServletRequest): String = {
    var message: String = printHttpRequest(request)
    request match {
      case request1: PayLoadWrappedHttpServletRequest =>
        message += " payload = " + new Predef.String(request1.payLoadByteArray)
      case _ =>
    }
    message
  }
}

class NotMoreThanIn(minTime: Duration) {

  private val lastSend: AtomicLong = new AtomicLong(0)

  @tailrec
  private def acquirePermit(now: Long): Boolean ={
    val lastSendSnapshot = lastSend.get()
    if(now - lastSendSnapshot < minTime.toMillis){
        false
    } else if (lastSend.compareAndSet(lastSendSnapshot, now)){
        true
    } else acquirePermit(now)
  }

  def apply[T](f: => T): Unit = {
      if(acquirePermit(System.currentTimeMillis))
        f
  }

}