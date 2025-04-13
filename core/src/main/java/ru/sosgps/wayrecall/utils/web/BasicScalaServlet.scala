package ru.sosgps.wayrecall.utils.web


import javax.servlet.http._
import org.springframework.web.context.support.WebApplicationContextUtils
import collection.mutable
import ref.WeakReference

abstract class BasicScalaServlet extends HttpServlet {

  protected def processRequest(request: ScalaHttpRequest, response: HttpServletResponse): Unit

  override protected def doGet(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    processRequest(new ScalaHttpRequest(request), response);
  }


  override protected def doPost(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    processRequest(new ScalaHttpRequest(request), response);
  }


  override def getServletInfo(): String = {
    return "Short description";
  }

  protected def getSpringBean[T]()(implicit m: ClassManifest[T]): T = {

    require(!m.erasure.equals(classOf[java.lang.Object]), "unspecifiedType: " + m.erasure)

    WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean[T](m.erasure.asInstanceOf[Class[T]])

  }

  protected def getSpringBean[T](name: String)(implicit m: Manifest[T]): T =
    WebApplicationContextUtils.getWebApplicationContext(getServletContext()).getBean(name, m.erasure).asInstanceOf[T]


}
