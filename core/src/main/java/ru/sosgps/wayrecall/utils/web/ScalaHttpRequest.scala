package ru.sosgps.wayrecall.utils.web

import javax.servlet.http.{HttpServletRequestWrapper, HttpServletRequest}
import org.springframework.web.context.support.WebApplicationContextUtils

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 13.11.12
 * Time: 15:46
 * To change this template use File | Settings | File Templates.
 */
class ScalaHttpRequest(request: HttpServletRequest) extends HttpServletRequestWrapper(request) {

  def getParameterOption(name: String): Option[String] = Option(this.getParameter(name))

  def getParameterStrict(name: String): String = getParameterOption(name).getOrElse(throw new IllegalArgumentException("no parameter " + name + " in request"))

  def getSpringBean[T]()(implicit m: ClassManifest[T]): T = {

    require(!m.erasure.equals(classOf[java.lang.Object]), "unspecifiedType: " + m.erasure)

    WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean[T](m.erasure.asInstanceOf[Class[T]])

  }

  def getSpringBean[T](name: String)(implicit m: Manifest[T]): T =
    WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(name, m.erasure).asInstanceOf[T]




}
