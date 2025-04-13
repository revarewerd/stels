package ru.sosgps.wayrecall.initialization

import java.io.{IOException, StringWriter, Writer}

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.ErrorHandler
import org.slf4j.{Logger, LoggerFactory}
import ru.sosgps.wayrecall.utils.errors.{BadRequestException, MailErrorReporter}
import ru.sosgps.wayrecall.utils.{MailSender, impldotChain}


class WrcJettyErrorHandler(val mailErrorReporter: MailErrorReporter) extends ErrorHandler {

  private val logger: Logger = LoggerFactory.getLogger(classOf[WrcJettyErrorHandler])

  def this(mailSender: MailSender, emailAddress: String) {
    this(new MailErrorReporter(mailSender, emailAddress))
  }

  @throws[IOException]
  override def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
    val th: Throwable = request.getAttribute("javax.servlet.error.exception").asInstanceOf[Throwable]
    if (response.getStatus == 500) {
      th match {
        case null =>
        case e if e.getMessage.?.exists(_.contains("Idle timeout expired")) => response.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT)
        case BadRequestException(e) => response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
        case _ =>
      }

    }
    super.handle(target, baseRequest, request, response)
  }

  @throws[IOException]
  override protected def writeErrorPage(request: HttpServletRequest, writer: Writer, code: Int, message: String, showStacks: Boolean) {
    if (code >= 500) {
      val buff: Writer = new StringWriter
      super.writeErrorPage(request, buff, code, message, showStacks)
      val body: String = buff.toString
      writer.write(body)
      mailErrorReporter.notifyError(request, body)
    }
    else {
      super.writeErrorPage(request, writer, code, message, showStacks)
    }
  }
}