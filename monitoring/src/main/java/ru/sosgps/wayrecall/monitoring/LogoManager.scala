package ru.sosgps.wayrecall.monitoring

import java.io.FilterWriter
import java.nio.file.{Path, Files}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import ru.sosgps.wayrecall.core.InstanceConfig
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.utils.web.ScalaJson
import scala.collection.JavaConversions.propertiesAsScalaMap

/**
 * Created by nickl on 08.09.14.
 */
@Controller
@RequestMapping(Array("/logo"))
class LogoManager {

  @Autowired
  var iconf: InstanceConfig = null

  @RequestMapping(Array("/loginlogo.png"))
  def loginlogo(request: HttpServletRequest,
                response: HttpServletResponse): Unit ={
    fileOrRedirect(iconf.path.resolve("loginlogo.png"), "/images/ksb_logon_screen.png", request, response)
  }

  @RequestMapping(Array("/monitoringlogo.png"))
  def monitoringlogo(request: HttpServletRequest,
                response: HttpServletResponse): Unit ={
    fileOrRedirect(iconf.path.resolve("monitoringlogo.png"), "/images/ksb_logo_text.png", request, response)
  }
  
  @RequestMapping(Array("/mobilelogo.png"))
  def mobilelogo(request: HttpServletRequest,
                response: HttpServletResponse): Unit ={
    fileOrRedirect(iconf.path.resolve("mobilelogo.png"), "/images/ksb_logo_min.png", request, response)
  }

  def fileOrRedirect(logofromconf: Path, path: String, request: HttpServletRequest, response: HttpServletResponse) {
    if (Files.exists(logofromconf)) {
      response.setContentType("image/png")
      val writer = response.getOutputStream
      try {
        Files.copy(logofromconf, writer)
      }
      finally {
        writer.close()
      }
    }
    else {
      request.getRequestDispatcher(path).forward(request, response)
    }
  }

  @RequestMapping(Array("/logodata.js"))
  def genLocalizationJs(
                         request: HttpServletRequest,
                         response: HttpServletResponse
                         ) {



    response.setCharacterEncoding("utf-8")
    response.setHeader("Content-Type","application/x-javascript; charset=utf-8")
    val out = response.getWriter
    out.println("homesite = '" + iconf.properties.getOrElse("instance.homesite","http://sosgps.ru/") + "';");
    //out.print("translations = ");
    out.flush();
    out.close();

  }

}
