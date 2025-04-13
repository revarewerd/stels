package ru.sosgps.wayrecall.billing.security

import org.springframework.stereotype.Component
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import grizzled.slf4j.{Logging, Logger}
import org.springframework.beans.factory.annotation.{Qualifier, Autowired}
import ru.sosgps.wayrecall.core.MongoDBManager
import org.springframework.security.core.userdetails._
import org.springframework.security.core.{SpringSecurityMessageSource, Authentication}
import com.mongodb.casbah.Imports._
import org.springframework.security.core.authority.SimpleGrantedAuthority
import scala.Some
import org.springframework.context.support.MessageSourceAccessor
import org.springframework.security.authentication.{AccountExpiredException, DisabledException, LockedException}

/**
 * Created by IVAN on 02.06.2014.
 */
@Component
class BillingAuthenticationProvider extends DaoAuthenticationProvider {

    private lazy val _logger = Logger(getClass)

    import _logger._

    private[this] var userDetailsService: BillingUserDetailsService = null

    @Autowired
    var mdbm: MongoDBManager = _

    @Autowired
    @Qualifier("billingUserDetailsService")
    override def setUserDetailsService(userDetailsService: UserDetailsService) {
      this.userDetailsService = userDetailsService match {
        case e: BillingUserDetailsService => e;
        case _ => throw new IllegalArgumentException("userDetailsService must be  BillingUserDetailsService")
      }

      super.setUserDetailsService(userDetailsService)
    }

    override def authenticate(authentication: Authentication): Authentication = {

      debug("authenticating " + authentication.getName)

      super.authenticate(authentication)
    }

  }

  import scala.collection.JavaConversions.asJavaCollection

  @Component
  class BillingUserDetailsService extends UserDetailsService {

    @Autowired
    var mdbm: MongoDBManager = _

    def loadUserByUsername(username: String): UserDetails = {
      val users = mdbm.getDatabase()("users")
      val billingPermissions = mdbm.getDatabase()("billingPermissions")
      val billingRoles = mdbm.getDatabase()("billingRoles")

      val user = users.findOne(MongoDBObject("name" -> username), MongoDBObject("name" -> 1, "password" -> 1, "enabled" -> 1,"userType"->1))

      user match {
        case Some(data) => {
          val permissionRecord= billingPermissions.findOne(MongoDBObject("userId" -> data.get("_id")))
          val userType=data.getAsOrElse[String]("userType","user")
          val templateId: String=permissionRecord.map(item => {
            item.getAsOrElse[String]("templateId","-1")
          }).getOrElse("-1")
          System.out.println("templateId "+templateId)
          val authorities=
          if(templateId!="-1"){
            val roleRecord=billingRoles.findOne(MongoDBObject("_id" -> new ObjectId(templateId)))
            roleRecord.map(item => {
              item.getAsOrElse[Seq[String]]("authorities",Seq.empty)
            }).getOrElse(Seq.empty)
          }
          else {
            permissionRecord.map(item => {
              item.getAsOrElse[Seq[String]]("authorities",Seq.empty)
            }).getOrElse(Seq.empty)
          }

          System.out.println("authorities "+authorities)

          val grantedAuthorities=authorities
            .map( auth => {
              new SimpleGrantedAuthority(auth)
          })
          System.out.println("grantedAuthorites "+grantedAuthorities)
          val authAndUType = grantedAuthorities :+ new SimpleGrantedAuthority(userType)
          System.out.println("authAndUType "+authAndUType)
          new User(
          data.as[String]("name"),
          data.as[String]("password"),
          true,//data.getAs[Boolean]("enabled").getOrElse(false),
          true,
          true,
          true,
           authAndUType
          //Seq[GrantedAuthority](new SimpleGrantedAuthority("ROLE_USER"))
          //java.util.Collections.emptySet()
        )
        }
        case None => throw new UsernameNotFoundException("user " + username + " was not found in database")
      }
    }
  }

  //TODO: похоже код дублируется с мониторигом
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
