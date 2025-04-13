package ru.sosgps.wayrecall.utils

import scala.collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 15.07.13
 * Time: 15:16
 * To change this template use File | Settings | File Templates.
 */

case class Memo[A, B](f: A => B) extends (A => B) {
  val cache = mutable.Map.empty[A, B]

  def apply(x: A) = cache getOrElseUpdate(x, f(x))
}