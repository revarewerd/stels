/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.utils

import java.io.Closeable
import java.net.ServerSocket
import java.net.Socket
import scala.collection.mutable.Stack

class ResourceScope private() {

  private var list = Stack[Closeable]()

  def manage[T <% Closeable](c: T): T = {
    list.push(c); c
  }

  def mfinally(f: () => Any): Unit = {
    list.push(new CloseableWrapper(f));
  }

  private def closeAll(): Unit = {
    list.foreach(_.close()); list = null;
  }

}

class CloseableWrapper(closeFunction: () => Any) extends Closeable {

  def close(): Unit = {
    closeFunction()
  }

}


//ru.sosgps.wayrecall.monitoring.utils.web.ResourceScope

object ResourceScope {

  implicit def ssocketToCloseable(s: ServerSocket): Closeable = new CloseableWrapper(s.close)

  implicit def socketToCloseable(s: Socket): Closeable = new CloseableWrapper(s.close)

  def scope[T](f: (ResourceScope) => T): T = {
    val rs = new ResourceScope();
    try {
      f(rs)
    }
    finally {
      rs.closeAll
    }
  }

  /**
   * @param args the command line arguments
   */
  def main(args: Array[String]): Unit = {
    class Ctest(name: String) extends Closeable {
      println(name + " opened")

      def close(): Unit = println(name + " closed")

    }


    val s = new ServerSocket(8899)
    scope(p => {


      p.manage(new Ctest("ot1"))
      p.manage(new Ctest("ot2"))
      p.manage(s)
      println("ss closed:" + s.isClosed);


    })
    println("ss closed:" + s.isClosed);

    println("Hello, world!")
  }

}
