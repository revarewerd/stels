package ru.sosgps.wayrecall.utils

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.09.12
 * Time: 16:15
 * To change this template use File | Settings | File Templates.
 */
class LimitsSortsFilters(val start: Int = 0, val limit: Option[Int] = None, val sortName: Option[String] = None, val order: Order.Order = Order.Ascending) {

  require(start >= 0, "start=" + start + " must be >=0")
  require(limit.map(_ >= 0).getOrElse(true), "limit=" + limit + " must be >=0")

  def this(start: Int, end: Int) = this(start, Some(end))


  def accepts(another: LimitsSortsFilters): Boolean = {
    another.start >= start && limit.map(e1 => another.limit.map(e2 => (e2 + (another.start - start)) <= e1).getOrElse(false)).getOrElse(true)
  }

  override def toString = "LimitsSortsFilters(" + start + " ," + limit.map(_ + "").getOrElse("None") + ", " + sortName.getOrElse("None") + ", " + order + ")"
}

object LimitsSortsFilters {
  val empty = new LimitsSortsFilters()
}


object Order extends Enumeration {
  type Order = Value
  val Descending, Ascending = Value
}