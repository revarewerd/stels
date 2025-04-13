package ru.sosgps.wayrecall.monitoring.security

import java.util.concurrent.ConcurrentHashMap
import java.util.{Collections, Date}
import javax.annotation.Resource
import javax.servlet.http.{HttpServletResponse, HttpSession}

import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.{ApplicationEvent, ApplicationListener}
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.session.{HttpSessionCreatedEvent, HttpSessionDestroyedEvent}
import org.springframework.stereotype.{Component, Controller}
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}
import org.springframework.web.context.request.{RequestContextHolder, ServletRequestAttributes}
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.initialization.WayrecallSessionManager
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker
import ru.sosgps.wayrecall.utils.{web, OptMap}
import ru.sosgps.wayrecall.utils.web.ScalaJsonObjectMapper
import ru.sosgps.wayrecall.utils.web.ScalaServletConverters.sessionToMap

import scala.collection.JavaConverters.{asScalaSetConverter, collectionAsScalaIterableConverter}

/**
 * Created by nickl on 21.12.13.
 */
@Component
class SessionListener extends ApplicationListener[ApplicationEvent] with grizzled.slf4j.Logging {

  private[this] val _activeSessions = Collections.newSetFromMap[HttpSession](
    new ConcurrentHashMap[HttpSession, java.lang.Boolean]()).asScala


  def activeSessions: Iterable[HttpSession] = _activeSessions

  def onApplicationEvent(event: ApplicationEvent) = {
    event match {
      case e: HttpSessionCreatedEvent => {
        debug("received event0:" + event)
        val session: HttpSession = e.getSession
        Option(RequestContextHolder.currentRequestAttributes())
          .collect {case e: ServletRequestAttributes => e.getRequest.getRemoteAddr}
          .foreach(addr => session.setAttribute("remoteAddr", addr))

        _activeSessions += session
      }
      case e: HttpSessionDestroyedEvent => {
        debug("received event1:" + event)
        _activeSessions -= e.getSession
      }
      case _ =>
    }
  }
}

@Controller
@RequestMapping(Array("/sessions"))
class SessionsList extends grizzled.slf4j.Logging {


  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var packstore: PackagesStore = null

  @Autowired
  var prm: ObjectsPermissionsChecker = null

  val recalcInterval = 1000 * 60 * 10


  //  @Autowired
  //  var sesreg: SessionRegistry = null

  @Autowired
  var sesreg: SessionListener = null

  @Resource(name = "monitoringwebapp-SessionManager")
  var sesmngr: WayrecallSessionManager = null

  @RequestMapping(value = Array("/list"), produces = Array("text/plain; charset=utf-8"))
  @ResponseBody
  def process(@RequestParam(value = "sort", required = false) sort: String, response: HttpServletResponse) {

    response.setContentType("text/json; charset=UTF-8")
    val mapper = new ScalaJsonObjectMapper
    mapper.enable(SerializationFeature.INDENT_OUTPUT)

    val writer = response.getWriter
    try {
      //val principals = sesreg.getAllPrincipals

      //debug("principals="+principals)

      //val str = for (sess <- sesreg.activeSessions.iterator) yield {
      val str = for (sess <- sesmngr.getActiveSessions.asScala.iterator) yield {
        val auth = web.getAuth(sess)
        //writer.println("=" + sc.map(_.getAuthentication.getName) + " " + new Date(sess.getLastAccessedTime))
        //sess.
        OptMap(
          "login" -> auth.map(_.getName),
          "lastAccess" -> new Date(sess.getLastAccessedTime),
          "creationTime" -> new Date(sess.getCreationTime),
          //"auth" -> auth.toString
          "remoteAddr" -> sess.get("remoteAddr")
        )
      }

      val res = if (sort != null)
        str.toIndexedSeq.sortBy(_.get(sort).toString)
      else
        str
      mapper.generate(res, writer)
    }
    finally writer.close()

  }


  @RequestMapping(value = Array("/invalidate"), produces = Array("text/plain; charset=utf-8"))
  @ResponseBody
  def invalidate(@RequestParam("login") login: String) = throw new UnsupportedOperationException("session invalidate")
//  {
//
//    for (sess <- sesreg.activeSessions;
//         auth <- getAuth(sess);
//         if auth.getName == login
//    ) {
//      sess.invalidate()
//    }
//
//    "Ok"
//  }


}
