package ru.sosgps.wayrecall.utils

import javax.servlet.ServletRequest

import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.context.request.{ServletRequestAttributes, RequestContextHolder}
import javax.servlet.http.{HttpSession, HttpServletRequest}
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.context.WebApplicationContext
import org.springframework.security.core.context.{SecurityContext, SecurityContextHolder}

import ru.sosgps.wayrecall.utils.web.ScalaServletConverters.sessionToMap

package object web {

  def springCurrentRequest: HttpServletRequest = {
    (RequestContextHolder.currentRequestAttributes().asInstanceOf[ServletRequestAttributes]).ensuring(_ != null, " cant get request context")
      .getRequest()
  }

  def springCurrentRequestOption: Option[HttpServletRequest] = {
    val opt = scala.util.control.Exception.catching(classOf[IllegalStateException]).opt {
      RequestContextHolder.currentRequestAttributes()
    }
    opt.collect({
      case f: ServletRequestAttributes => f.getRequest
    })
  }

  def getAuth(sess: HttpSession) = {
    val sc = sess.get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY).map(_.asInstanceOf[SecurityContext])
    val auth = sc.flatMap(sc => Option(sc.getAuthentication))
    auth
  }

  def getUserName = Option(SecurityContextHolder.getContext.getAuthentication).map(_.getName)

  def springWebContext: WebApplicationContext = {
    WebApplicationContextUtils.getWebApplicationContext(springCurrentRequest.getServletContext).ensuring(_ != null, " cant get WebApplicationContext")
  }

  def printParamMap(httpr: ServletRequest): String = {

    import scala.collection.JavaConversions.mapAsScalaMap

    httpr.getParameterMap.map({
      case (k, v) if v.length == 1 => k + " -> " + v(0)
      case (k, v) => k + " -> " + v.mkString("[", ",", "]")
    }).mkString("[", ";", "]")
  }

  def printHttpRequest(httpr: HttpServletRequest): String = {
    "HttpServletRequest to " + httpr.getServerName + " from " + httpr.getRemoteHost + "/" + getUserName.getOrElse("anonimous") + " " + httpr.getMethod + " " +
      httpr.getRequestURI + " " +
      httpr.getQueryString + " " +
      printParamMap(httpr)
  }

  def printHttpHeaders(httpr: HttpServletRequest): String = {
    import scala.collection.JavaConversions.enumerationAsScalaIterator
    "HttpHeaders :" + httpr.getHeaderNames.map(h => h + " : " + httpr.getHeader(h)).mkString("(", ", ", ")")
  }

}
