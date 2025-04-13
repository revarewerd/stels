package ru.sosgps.wayrecall.billing.security

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.{Authentication, GrantedAuthority}
import org.springframework.stereotype.Component
import ru.sosgps.wayrecall.core.{MongoDBManager, UserRolesChecker}
import ru.sosgps.wayrecall.security.PermissionsManager
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConverters._

/**
 * Created by IVAN on 09.06.2014.
 */

class BillingUserRolesChecker extends PermissionsManager with UserRolesChecker with grizzled.slf4j.Logging{

  @Autowired
  var mdbm: MongoDBManager = null

  def getPermForCurrentUser() = {
    var allUsers : Iterator[DBObject] = Iterator.empty
    val userName = getUserName()
    if(hasRequiredStringAuthorities("admin","UserView")) {
      allUsers = mdbm.getDatabase().apply("users").find(MongoDBObject()).sort(MongoDBObject("name" -> 1))
    }
    else if(hasRequiredStringAuthorities("superuser","UserView")) {
      val creatorId=getUserIdByName(userName)
      val creator=if(creatorId==null) userName else creatorId
      allUsers = mdbm.getDatabase().apply("users").find($or("name"->userName,"creator"->creator)).sort(MongoDBObject("name" -> 1))
    }
    allUsers
  }

  @deprecated
  def hasRequiredAuthorities(requiredAuth: Seq[GrantedAuthority])={
      val userAuth=SecurityContextHolder.getContext().getAuthentication().getAuthorities
      var hasAuth=true
      requiredAuth.foreach(auth=>{
        if(!userAuth.contains(auth)) {hasAuth=false; debug("Hasn't got required authority - "+auth.toString)}
      })
      hasAuth
  }

}

