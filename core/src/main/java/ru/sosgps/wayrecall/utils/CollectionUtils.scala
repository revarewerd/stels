package ru.sosgps.wayrecall.utils

import java.io.Serializable
import java.util

import scala.collection.immutable.HashMap

/**
 * Created by nickl on 10.03.14.
 */
object CollectionUtils {

  implicit class additionalMethods[A](val arg: TraversableOnce[A]) extends AnyVal{

    def maxByOpt[B](f: A => B)(implicit cmp: Ordering[B]): Option[A] = {
      if (arg.isEmpty)
        None
      else
        Some(arg.maxBy(f))
    }

    def minByOpt[B](f: A => B)(implicit cmp: Ordering[B]): Option[A] = {
      if (arg.isEmpty)
        None
      else
        Some(arg.minBy(f))
    }

    def boundsFor[B](f: A => B)(implicit cmp: Ordering[B]): (B, B) = {
      val iterator = arg.toIterator
      val first = iterator.next()
      val v = f(first)
      var min = v
      var max = v
      for(e <- iterator){
        val v = f(e)
        if(cmp.gt(v, max))
          max = v
        if(cmp.lt(v, min))
          min = v
      }
      (min, max)

    }

  }

  def detectChanged(oldData: scala.collection.Map[String, Serializable],
                    newData: scala.collection.Map[String, Serializable]): HashMap[String, Serializable] = {
    val builder = HashMap.newBuilder[String, Serializable]
    //val removedKeys = oldData -- newData.keys
    for ((k, v) <- newData) {
      oldData.get(k) match {
        case Some(old) if old != v => builder += ((k, v))
        case None => builder += ((k, v))
        case _ =>
      }
    }

    //    for (removedKey <- removedKeys) {
    //      builder += ((removedKey._1, null))
    //    }

    val result = builder.result()
    result
  }

  def detectChanged[T](oldData: util.Map[String, T], newData: util.Map[String, T]): util.Map[String, T] = {
    if (oldData != null && !oldData.isEmpty) {
      val changes = new util.HashMap[String, T]
      changes.putAll(newData)
      changes.entrySet.removeAll(oldData.entrySet)
      changes
    }
    else{
       newData
    }
  }

  def detectChangedMarkRemoved[T](oldData: util.Map[String, T], newData: util.Map[String, T], rmark: T = null): util.Map[String, T] = {
    import scala.collection.JavaConversions.asScalaSet
    val removedKeys = oldData.keySet() -- newData.keySet()
    val changed =  detectChanged(oldData, newData)
    for(rkey <- removedKeys){
      changed.put(rkey, rmark)
    }
    changed
  }


}
