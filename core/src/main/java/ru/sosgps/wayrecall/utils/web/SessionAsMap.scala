package ru.sosgps.wayrecall.utils.web

import javax.servlet.http.HttpSession
import collection.mutable
import collection.JavaConversions.enumerationAsScalaIterator
import ref.{SoftReference, Reference, WeakReference}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 13.11.12
 * Time: 15:47
 * To change this template use File | Settings | File Templates.
 */
class SessionAsMap(ses: HttpSession) extends mutable.Map[String, Any] {


  def iterator = ses.getAttributeNames.map(e => (e, ses.getAttribute(e)))

  def get(key: String) = Option(ses.getAttribute(key))

  override def +=(kv: (String, Any)): this.type = {
    ses.synchronized {ses.setAttribute(kv._1, kv._2)}
    this
  }

  override def -=(key: String): this.type = {
    ses.synchronized {ses.removeAttribute(key)}
    this
  }

  def getTyped[T](key: String, orUpdate: => T): T = ses.synchronized({
    this.getOrElseUpdate(key, orUpdate).asInstanceOf[T]
  })

  override def getOrElseUpdate(key: String, default: => Any): Any = synchronized {super.getOrElseUpdate(key, default)}

  override def put(key: String, value: Any): Option[Any] = synchronized { super.put(key, value) }
  override def update(key: String, value: Any): Unit = synchronized { super.update(key, value) }
  override def remove(key: String): Option[Any] = synchronized { super.remove(key) }

  def getWeak[T <: AnyRef](key: String, orUpdate: => T): T = ses.synchronized({
    this.get(key).flatMap({case e: Reference[T] => e.get}).getOrElse({
      val newval = orUpdate
      this.put(key, new WeakReference[T](newval))
      newval
    })
  })

  def getSoft[T <: AnyRef](key: String, orUpdate: => T): T = ses.synchronized({
    this.get(key).flatMap({case e: Reference[T] => e.get}).getOrElse({
      val newval = orUpdate
      this.put(key, new SoftReference[T](newval))
      newval
    })
  })

}
