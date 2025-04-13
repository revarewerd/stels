package ru.sosgps.wayrecall.monitoring.web

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import ch.ralscha.extdirectspring.annotation.ExtDirectMethod
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker
import ru.sosgps.wayrecall.utils.{ExtDirectService, web}

/**
 * Created by nickl on 23.04.15.
 */
@Controller
@ExtDirectService
class ErrorReporter extends grizzled.slf4j.Logging {
  @ExtDirectMethod
  def error(message: String, request: HttpServletRequest): Unit = {
    logerror(request, message)
  }

  @RequestMapping(Array("/senderror"))
  def senderror(@RequestParam("message") message: String, request: HttpServletRequest,
                response: HttpServletResponse): Unit = {
    logerror(request, message)
  }

  private def logerror(request: HttpServletRequest, message: String): Unit = {
    val errorMessage = s"User: '${web.getUserName.getOrElse("")}', message: $message \n" +
      s"\t${web.printHttpRequest(request)}\n\t${web.printHttpHeaders(request)}"
    error(errorMessage)
  }
}
