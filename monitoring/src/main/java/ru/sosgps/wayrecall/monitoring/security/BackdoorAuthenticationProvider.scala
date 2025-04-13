package ru.sosgps.wayrecall.monitoring.security

import java.util

import org.springframework.security.authentication._
import com.mongodb.casbah.commons.MongoDBObject
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.{GrantedAuthority, Authentication}
import ru.sosgps.wayrecall.core.MongoDBManager
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import org.springframework.stereotype.Component
import org.springframework.security.web.authentication.{WebAuthenticationDetails, SimpleUrlAuthenticationFailureHandler, SimpleUrlAuthenticationSuccessHandler, AbstractAuthenticationProcessingFilter}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import grizzled.slf4j.Logger
import java.util.{Calendar, Date, UUID}
import com.mongodb.WriteConcern
import org.springframework.security.core.context.SecurityContextHolder
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.web.ScalaServletConverters._

import scala.collection.JavaConversions.seqAsJavaList

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 07.01.13
 * Time: 1:04
 * To change this template use File | Settings | File Templates.
 */
@Component
class BackdoorAuthenticationProvider extends AuthenticationProvider with grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = _

  def authenticate(authentication: Authentication): Authentication = {

    debug("authenticating" + authentication.getName)

    authentication match {
      case a: BackdoorAuthenticationToken => {
        val a = new UsernamePasswordAuthenticationToken(
          authentication.getPrincipal,
          authentication.getCredentials,
          authentication.getAuthorities)
        a.setDetails(authentication.getDetails)
        a
      };
      case _ => throw new BadCredentialsException("Bad Credentials")
    }
  }

  def supports(authentication: Class[_]) = authentication.equals(classOf[BackdoorAuthenticationToken])
}


@Component
class BackdoorAuthenticationFilter extends AbstractAuthenticationProcessingFilter("/backdoorAuth") {

  setAuthenticationSuccessHandler(new SimpleUrlAuthenticationSuccessHandler("/"))
  setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/"))

  private lazy val _logger = Logger(getClass)

  import _logger._

  @Autowired
  @Qualifier("authenticationManager")
  override def setAuthenticationManager(authenticationManager: AuthenticationManager) {
    super.setAuthenticationManager(authenticationManager)
  }

  @Autowired
  var mdbm: MongoDBManager = null

  def attemptAuthentication(request: HttpServletRequest, response: HttpServletResponse): Authentication = {

    debug("attemptAuthentication")

    val sesopt = Option(request.getSession(false))

    //val seccontext = sesopt.flatMap(s => Option(s.getAttribute("SPRING_SECURITY_CONTEXT")))

    sesopt match {
      case Some(ses) => {
        debug("session was:" + ses.toMap)
        ses.clear() //TODO: Правильнее было бы инвалидейтить сессию, но почему-то в этом случа в спринге что-то работает не так
      }
      case None => debug("there was no session")
    }

    Option(SecurityContextHolder.getContext.getAuthentication) match {
      case Some(authentication) => {
        debug("authentication was " + authentication)
        //SecurityContextHolder.getContext.setAuthentication(null)
        authentication.setAuthenticated(false)
      }
      case None => debug("there was no authentication")
    }


    val key = request.getParameter("key")
    val coll = mdbm.getDatabase()("authBackdors")

    val cal = Calendar.getInstance()
    cal.add(Calendar.MINUTE, -1)
    coll.remove("date" $lt cal.getTime, WriteConcern.SAFE)

    coll.findOne(MongoDBObject("backdoorKey" -> key)) match {
      case Some(dbo) => {
        val presetedAuthorities = authorities.map(str => new SimpleGrantedAuthority(str))
        debug("presetedAuthorities=" + presetedAuthorities)
        val authRequest = new BackdoorAuthenticationToken(dbo.get("login").asInstanceOf[String], key,
          presetedAuthorities
        )
        authRequest.setDetails(new BackdoorAuthenticationDetails(key, request))


        debug("succses for " + dbo.get("login"))
        coll.remove(dbo, WriteConcern.SAFE)
        return this.getAuthenticationManager.authenticate(authRequest)
      }
      case None => {
        debug("backdoorKey " + key + " is missing")
        throw new BadCredentialsException("backdoorKey authenication with key " + key + " failed")
      }
    }

  }

  var authorities: Seq[String] = Seq.empty

  def setAuthoritiesString(str: String) = authorities = str.split(",").map(_.trim)

}


class BackdoorAuthenticationToken(name: String, key: String, authorities: util.Collection[_ <: GrantedAuthority])
  extends AbstractAuthenticationToken(authorities) {

  def getCredentials = key

  def getPrincipal = name
}

class BackdoorAuthenticationDetails(val backdoorKey: String, request: HttpServletRequest) extends WebAuthenticationDetails(request)