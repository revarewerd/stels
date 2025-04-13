package ru.sosgps.wayrecall.packreceiver

import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.{GrantedAuthority, Authentication}
import ru.sosgps.wayrecall.core.{UserRolesChecker, MongoDBManager}
import ru.sosgps.wayrecall.security.PermissionsManager

/**
 * Created by IVAN on 25.06.2014.
 */
class RoleChecker extends PermissionsManager with UserRolesChecker with grizzled.slf4j.Logging{

  @Autowired
  var mdbm: MongoDBManager = null

  @deprecated
  def hasRequiredAuthorities(requiredAuth: Seq[GrantedAuthority])={
    val userAuth=SecurityContextHolder.getContext().getAuthentication().getAuthorities
    var hasAuth=true
    requiredAuth.foreach(auth=>{
      if(!userAuth.contains(auth)) {hasAuth=false; debug("Hasn't got required authority - "+auth.toString)}
    })
    hasAuth
  }

  def getPermForCurrentUser() = {
    var allObjects : Iterator[DBObject] = Iterator.empty
    val userName = getUserName()
    if(hasRequiredAuthorities(Seq(new SimpleGrantedAuthority("admin"),new SimpleGrantedAuthority("UserView")))) {
      allObjects = mdbm.getDatabase().apply("users").find(MongoDBObject())
    }
    else if(hasRequiredAuthorities(Seq(new SimpleGrantedAuthority("superuser"),new SimpleGrantedAuthority("UserView")))) {
      val creatorId=getUserIdByName(userName)
      val creator=if(creatorId==null) userName else creatorId
      allObjects = mdbm.getDatabase().apply("users").find($or("name"->userName,"creator"->creator))
    }
    allObjects
  }
}
