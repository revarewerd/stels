package ru.sosgps.wayrecall.utils

/**
 * Created by nickl on 22.12.13.
 */
object OptMap {

  def apply[A, B >: Option[_]](elems: (A, B)*): Map[A, B] = {
    val pairs: Seq[(A, B)] = elems.filter(kv => kv._2 match {
      case Some(opt) => true
      case None => false
      case _ => true
    }).map {
      case (k, Some(opt: B)) => (k, opt)
      case e => e
    }
    Map[A, B](pairs:_*)
  }

  implicit class mapOptFilter[K, V >: Option[_]](map: Map[K, V]) {

    def flattenOptions: Map[K, V] = {
      map.filter(kv => kv._2 match {
        case Some(opt) => true
        case None => false
        case _ => true
      }).mapValues {
        case Some(opt: V) => opt
        case e => e
      }
    }

  }

}
