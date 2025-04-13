package ru.sosgps.wayrecall.monitoring.security

import org.springframework.context.{ApplicationEvent, ApplicationListener}
import org.springframework.security.authentication.event.AbstractAuthenticationEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager

import com.mongodb.casbah.Imports._
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.security.core.Authentication
import java.util.Date
import javax.servlet.http.HttpSessionEvent
import org.springframework.security.access.event.{AuthorizedEvent, AuthorizationFailureEvent}
import org.springframework.web.context.support.ServletRequestHandledEvent
import org.springframework.security.core.session.{SessionCreationEvent, SessionDestroyedEvent}
import collection.JavaConversions.asScalaBuffer
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.web.session.HttpSessionDestroyedEvent

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 23.05.13
 * Time: 20:54
 * To change this template use File | Settings | File Templates.
 */
@Component
class AuthenticationListener extends ApplicationListener[ApplicationEvent] with grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = _


  def onApplicationEvent(event: ApplicationEvent) {

    event match {
      case e: AuthenticationSuccessEvent => logAuthEvent(e.getAuthentication, e, true)
      case e: org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent => logAuthEvent(e.getAuthentication, e, true)
      case e: AbstractAuthenticationEvent => logAuthEvent(e.getAuthentication, e)
      case e: AuthorizedEvent =>  logAuthEvent(e.getAuthentication, e)
      case e: SessionDestroyedEvent =>
        e.getSecurityContexts.map(_.getAuthentication).find(_.getDetails.isInstanceOf[WebAuthenticationDetails]).foreach(auth =>
          logAuthEvent(
            auth, e
          ))
      //      case e: ApplicationEvent if !e.isInstanceOf[ServletRequestHandledEvent] => mdbm.getDatabase()("authlog").insert(
      //        MongoDBObject("eventData" -> e.toString, "eventClass" -> e.getClass.getName, "date" -> new Date())
      //      )
      case _ =>
    }

  }


  def logAuthEvent(a: Authentication, event: ApplicationEvent, markLastEntry: Boolean = false) {

    val eventName = event.getClass.getName

    val userName = a.getPrincipal match {
      case u: User => u.getUsername
      case s: String => s
    }

    val ip: String = a.getDetails match {
      case d: WebAuthenticationDetails => d.getRemoteAddress
      case o => {
        warn("incorrect details:" + o)
        "unknown"
      }
    }

    val date = new Date()
    mdbm.getDatabase()("authlog").insert(
      MongoDBObject(
        "login" -> userName,
        "event" -> eventName,
        "date" -> date,
        "ip" -> ip)
    )

    if(markLastEntry && !a.getDetails.isInstanceOf[BackdoorAuthenticationDetails])
    {
      mdbm.getDatabase()("users").update(MongoDBObject("name" -> userName), $set("lastLoginDate" -> date))
    }

    event match {
      case sesEvent: HttpSessionDestroyedEvent if !a.getDetails.isInstanceOf[BackdoorAuthenticationDetails] =>
        mdbm.getDatabase()("users").update(MongoDBObject("name" -> userName),
          $set("lastAction" -> new Date(sesEvent.getSession.getLastAccessedTime)))
      case _ =>
    }


  }
}