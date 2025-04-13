package ru.sosgps.wayrecall

import java.net.ServerSocket
import java.util.Collections

import org.junit.Assert
import org.springframework.security.access.intercept.RunAsUserToken
import org.springframework.security.core.context.{SecurityContextHolder, SecurityContextImpl}
import org.springframework.security.core.userdetails.User

import scala.reflect.ClassTag

/**
  * Created by nickl-mac on 20.12.15.
  *
  * @see also [[ru.sosgps.wayrecall.utils.EventsTestUtils]]
  */
package object testutils {

  def getFreePort = {
    val s = new ServerSocket(0);
    val port = s.getLocalPort
    s.close()
    port
  }

  def runAsUser[T](user: String)(f: => T): T = {

    val context = SecurityContextHolder.getContext
    try {
      val impl = new SecurityContextImpl()
      impl.setAuthentication(new RunAsUserToken(user, new User(user, "", Collections.emptyList()), null, Collections.emptyList(), null))
      SecurityContextHolder.setContext(impl)
      f
    }
    finally {
      SecurityContextHolder.setContext(context)
    }

  }

  def expectException[T <: Throwable : ClassTag](code: => Any) {
    try {
      code
      Assert.fail("exception " + scala.reflect.classTag[T].runtimeClass.getName + " expected")
    } catch {
      case e: T => e
    }

  }

}
