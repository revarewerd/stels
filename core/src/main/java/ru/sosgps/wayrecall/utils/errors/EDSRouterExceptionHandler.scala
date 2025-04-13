package ru.sosgps.wayrecall.utils.errors

import java.lang.reflect.InvocationTargetException

import ch.ralscha.extdirectspring.bean.BaseResponse
import ch.ralscha.extdirectspring.controller.ConfigurationService
import ch.ralscha.extdirectspring.controller.DefaultRouterExceptionHandler
import ch.ralscha.extdirectspring.controller.RouterExceptionHandler
import ch.ralscha.extdirectspring.util.MethodInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.utils.MailSender
import ru.sosgps.wayrecall.utils.web._
import javax.servlet.http.HttpServletRequest

import kamon.Kamon

import scala.Predef
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created by nickl on 30.03.15.
 */


@Autowired
class EDSRouterExceptionHandler(configurationService: ConfigurationService, errorReporter: MailErrorReporter)
  extends DefaultRouterExceptionHandler(configurationService) with RouterExceptionHandler with grizzled.slf4j.Logging {

  val ignoredExceptions : mutable.Set[Class[_ <: Throwable]] =
    mutable.Set(
      //classOf[org.springframework.security.access.AccessDeniedException]
    )


  override def handleException(methodInfo: MethodInfo, response: BaseResponse, e: Exception, request: HttpServletRequest): AnyRef = {

    val cause: Throwable = unwrap(e)

    cause match {
      case UnwrappedWrcLogicException(wrc) =>
      case ex if ignoredExceptions(ex.getClass) =>
      case e: Throwable =>
        val exceptionChainString = Iterator.iterate(e)(_.getCause).takeWhile(null !=).map(_.getClass.getSimpleName).mkString("-")
        val target = try methodInfo.getAction.getName + "-" + methodInfo.getMethod.getName catch {
          case _: Throwable => methodInfo + ""
        }
        Kamon.metrics.counter("http-errors", Map("target" -> target, "exception" -> exceptionChainString)).increment()
        errorReporter.notifyError(e, request)
    }

    super.handleException(methodInfo, response, e, request)
  }

  private def unwrap(e: Exception): Throwable = {
    e match {
      case e: InvocationTargetException if e.getCause != null => e.getCause
      case _ => e
    }
  }
}