package ru.sosgps.wayrecall.security

import javax.servlet.{FilterConfig, FilterChain, ServletResponse, ServletRequest}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}


//@WebFilter(filterName = "UnCachingFilter",urlPatterns = Array("/*"))
class UnCachingFilter extends javax.servlet.Filter with grizzled.slf4j.Logging{
  def destroy {
  }

  def doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {

    (req, resp) match {
      case (request: HttpServletRequest, response: HttpServletResponse) =>
        //println("UnCachingFilter:" + request.getRequestURL + "|" + request.getServletPath)
        if (request.getServletPath == "/" || request.getServletPath =="/login.html") {
          // Set standard HTTP/1.1 no-cache headers.
          response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
          // Set standard HTTP/1.0 no-cache header.
          response.setHeader("Pragma", "no-cache");
        }
      case _ => warn("Non HttpServletResponse")
    }
    chain.doFilter(req, resp)
    //println("Cache-Control=" + Option(resp).collect({case r: HttpServletResponse => r.getHeader("Cache-Control")}).orNull)
  }

  def init(config: FilterConfig) {
  }
}