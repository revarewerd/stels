package ru.sosgps.wayrecall.core

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import org.springframework.security.core.{Authentication, GrantedAuthority}
import org.springframework.stereotype.Component

import scala.annotation.varargs
import scala.collection.{immutable, mutable}

/**
 * Created by IVAN on 23.06.2014.
 */


trait UserRolesChecker {
  def checkAdminAuthority(): Boolean
  def getUserAuthorities() : Array[String]
  def getUserName() : String
  def getUserIdByName(name: String): ObjectId
  def getCurrentUserId():ObjectId
  def getPermForCurrentUser() : Iterator[DBObject]
  def hasPermissions(userName: String, recType: String, recId: Any) : Boolean
  @deprecated("use hasRequiredAuthorities(requiredAuth: String*) ")
  def hasRequiredAuthorities(requiredAuth: Seq[GrantedAuthority]) : Boolean
  @annotation.varargs
  def hasRequiredStringAuthorities(requiredAuth: String*) : Boolean
  def getPermittedUsers(recType: String, item_id: ObjectId): Seq[ObjectId]
  def getInferredPermissionRecordIds(userName: String, recType: String): Seq[ObjectId]
}

/**
 * Класс придуман для целей тестирования, но возможно он нафиг не нужен, так как можно заполнять тестовую базу
 * нужными правами и эмулировать пользователя через спринговский контекст
 */
class MockingUserRolesChecker extends UserRolesChecker {

  import scala.collection.mutable

  private var curAuthorities: Set[String] = immutable.Set.empty

  private var curUser: String = ""

  def runWithUserAndAuthorities[T](user: String, authorities: Set[String])(f: => T): T = {
    val prevUser = curUser;
    curUser = user
    val prevAuthorities = curAuthorities;
    curAuthorities = authorities
    try {
      f
    }
    finally {
      curAuthorities = prevAuthorities
      curUser = prevUser
    }
  }

  override def checkAdminAuthority(): Boolean = curAuthorities.contains("admin")

  @deprecated("use hasRequiredAuthorities(requiredAuth: String*) ")
  override def hasRequiredAuthorities(requiredAuth: Seq[GrantedAuthority]): Boolean =
    hasRequiredStringAuthorities(requiredAuth.map(_.getAuthority): _*)

  override def getPermittedUsers(recType: String, item_id: Imports.ObjectId): Seq[Imports.ObjectId] = ???

  override def getUserAuthorities(): Array[String] = curAuthorities.toArray

  override def getUserName(): String = curUser

  override def getInferredPermissionRecordIds(userName: String, recType: String): Seq[Imports.ObjectId] = ???

  //@annotation.varargs
  override def hasRequiredStringAuthorities(requiredAuth: String*): Boolean = requiredAuth.forall(curAuthorities)

  override def getUserIdByName(name: String): Imports.ObjectId = ???

  override def getPermForCurrentUser(): Iterator[Imports.DBObject] = ???

  override def hasPermissions(userName: String, recType: String, recId: Any): Boolean = ???

  override def getCurrentUserId(): _root_.com.mongodb.casbah.Imports.ObjectId = ???
}
