package ru.sosgps.wayrecall.utils



/**
  * Created by nickl on 27.12.16.
  * Key-Typed Map, ассоциативный массив в котором информация о типе значения хранится в ключе.
  * Работает по принципу Identity HaskMap то есть ключ должен быть объявлен заранее
  *
  * ```
  *
  * val key1 = KTMap.key[String]
  * val key2 = KTMap.key[Int]
  *
  * val map = KTMap(key1 -> "asdf", key2 -> 3)
  *
  * val s: String = map(key1)
  * val i: Int = map(key2)
  *
  *
  * ```
  */
class KTMap private(private val store: Map[KTMap.Key[_], Any]) {


  def apply[T](key:KTMap.Key[T]): T = store(key).asInstanceOf[T]

  def get[T](key:KTMap.Key[T]): Option[T] = store.get(key).map(_.asInstanceOf[T])

  def +[T] (pair:(KTMap.Key[T], T)) = new KTMap(store + pair)

  def ++ (other: KTMap) = new KTMap(store ++ other.store)

}


object KTMap{

  val empty = new KTMap(Map.empty)

  def apply(defs: Pair[_]*) = new KTMap(Map(defs.map(_.asTuple):_*))

  def key[T] = new Key[T]

  class Key[T]{
    def -> (value:T) = new Pair[T](this, value)
  }

  class Pair[T](val key: Key[T], val value: T) extends KTMap(Map((key ,value))){
    def asTuple: (Key[T], T) = (key, value)
  }

}