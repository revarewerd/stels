package ru.sosgps.wayrecall.security

import org.springframework.security.authentication._
import org.springframework.security.core.{SpringSecurityMessageSource, Authentication, GrantedAuthority}
import org.springframework.stereotype.Component
import ru.sosgps.wayrecall.core.MongoDBManager
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import com.mongodb.casbah.Imports._
import org.springframework.security.authentication.dao.{DaoAuthenticationProvider, AbstractUserDetailsAuthenticationProvider}
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails._
import scala.Some
import grizzled.slf4j.{Logging, Logger}
import scala.Some
import scala.Some
import org.springframework.context.support.MessageSourceAccessor

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 15.08.12
 * Time: 19:28
 * To change this template use File | Settings | File Templates.
 */

@Component
class WayrecallAuthenticationProvider extends DaoAuthenticationProvider {

  private lazy val _logger = Logger(getClass)

  import _logger._

  private[this] var userDetailsService: WayrecallUserDetailsService = null

  @Autowired
  var mdbm: MongoDBManager = _

  @Autowired
  @Qualifier("wayrecallUserDetailsService")
  override def setUserDetailsService(userDetailsService: UserDetailsService) {
    this.userDetailsService = userDetailsService match {
      case e: WayrecallUserDetailsService => e;
      case _ => throw new IllegalArgumentException("userDetailsService must be WayrecallUserDetailsService")
    }

    super.setUserDetailsService(userDetailsService)
  }

  override def authenticate(authentication: Authentication): Authentication = {

    debug("authenticating " + authentication.getName)

    super.authenticate(authentication)
  }

  //  override def supports(authentication: Class[_]) = authentication.equals(classOf[UsernamePasswordAuthenticationToken])


}

import scala.collection.JavaConversions.asJavaCollection

@Component
class WayrecallUserDetailsService extends UserDetailsService {

  @Autowired
  var mdbm: MongoDBManager = _

  def loadUserByUsername(username: String): UserDetails = {
    val users = mdbm.getDatabase()("users")

    val user = users.findOne(MongoDBObject("name" -> username), MongoDBObject("name" -> 1, "password" -> 1, "enabled" -> 1))

    user match {
      case Some(data) => new User(
        data.as[String]("name"),
        data.as[String]("password"),
        true,//data.getAs[Boolean]("enabled").getOrElse(false),
        true,
        true,
        true,
        Seq[GrantedAuthority](new SimpleGrantedAuthority("ROLE_USER"))
        //java.util.Collections.emptySet()
      )
      case None => throw new UsernameNotFoundException("user " + username + " was not found in database")
    }
  }
}

class BlockedUserChecker extends UserDetailsChecker with Logging {
  protected var messages: MessageSourceAccessor = SpringSecurityMessageSource.getAccessor

  @Autowired
  var mdbm: MongoDBManager = _

  def check(user: UserDetails) {
    debug("user"+user.getAuthorities)
    if (!user.isAccountNonLocked) {
      logger.debug("User account is locked")
      throw new LockedException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.locked", s"User $user account is locked"))
    }

    val data = mdbm.getDatabase()("users")
      .findOne(
      MongoDBObject(
        "name" -> user.getUsername
      ),
      MongoDBObject(
        "name" -> 1, "password" -> 1, "enabled" -> 1, "blockcause" -> 1
      )).get

    if (!data.getAs[Boolean]("enabled").getOrElse(false)) {
      throw new DisabledException(
        "Пользователь \"" + user.getUsername + "\" был заблокирован" +
          data.getAs[String]("blockcause").filter(_.nonEmpty).map(c => " по причине \"" + c + "\"").getOrElse(""))
    }


    if (!user.isAccountNonExpired) {
      logger.debug("User account is expired")
      throw new AccountExpiredException(messages.getMessage("AbstractUserDetailsAuthenticationProvider.expired", s"User $user account has expired"))
    }

  }
}