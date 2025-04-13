package ru.sosgps.wayrecall.utils

import java.util.Date

import com.google.common.collect.MapMaker
import org.joda.time.format.DateTimeFormat

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created by nickl on 17.02.15.
 */
object WeakTracer {

  private val map = new MapMaker().concurrencyLevel(8).weakKeys().makeMap[AnyRef, mutable.Buffer[Entry]]()

  // 2015-02-12 20:55:10,700

  private val format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss,SSS")

  def trace(identity: AnyRef, message: String) = {
//    var buffer = map.get(identity)
//    if (buffer == null) {
//      buffer = new ListBuffer[Entry]()
//      val prevBuffer = map.putIfAbsent(identity, buffer)
//      if (prevBuffer != null) {
//        buffer = prevBuffer
//      }
//    }
//
//    buffer.synchronized {
//      buffer += Entry(System.currentTimeMillis(),message)
//      if (buffer.size > 200)
//        buffer.remove(0, buffer.size - 200)
//    }

  }

  def getHistory(identity: AnyRef): Seq[String] = {
    val buffer = map.get(identity)
    if (buffer != null) buffer.synchronized(buffer.map(_.logstring).toList) else Seq.empty
  }

  case class Entry(date: Long, message: String){
    def logstring = format.print(date) +" " + message
  }

}
