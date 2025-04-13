package ru.sosgps.wayrecall.billing.security

import java.util.NoSuchElementException
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Created by IVAN on 25.11.2014.
 */

@Controller
class UserAuthoritiesService  extends grizzled.slf4j.Logging {

  @Autowired
  var roles: RolesService = null

  @RequestMapping(value = Array("/authorities.js"))
  def generateAuthoritiesJs(request: HttpServletRequest, response: HttpServletResponse) = try {
    debug("generating authorities.js")
    response.setContentType("application/x-javascript; charset=utf-8")
    val out = response.getWriter()
    try {

      out.println(
        """userRoles= """ + roles.getUserAuthorities().map(item => "'"+item+"'").mkString("[",",","]") +""";"""
        //+ """console.log("roles", userRoles)"""
       )
    }
    finally {
      out.close()
    }
  } catch {
    case e: NoSuchElementException => {
      warn("not found page:" + e + " " + e.getMessage)
      response.setStatus(HttpServletResponse.SC_NOT_FOUND)
    }
  }
}
