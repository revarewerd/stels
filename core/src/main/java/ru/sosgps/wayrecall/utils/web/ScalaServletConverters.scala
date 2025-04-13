package ru.sosgps.wayrecall.utils.web

import javax.servlet.http.{HttpServletRequest, HttpSession}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 13.11.12
 * Time: 15:46
 * To change this template use File | Settings | File Templates.
 */
object ScalaServletConverters{

  implicit def sessionToMap(ses:HttpSession) = new SessionAsMap(ses)

  implicit def requestToScala(req:HttpServletRequest) = req match  {
    case s:ScalaHttpRequest => s
    case _ => new ScalaHttpRequest(req)
  }
}
