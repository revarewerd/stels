package ru.sosgps.wayrecall.utils

import collection.SeqLike

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.09.12
 * Time: 20:00
 * To change this template use File | Settings | File Templates.
 */
trait PartialIterable[T] extends Iterable[T] {

  type Repr

  val total: Int;

  val start: Int;

  val limit: Int;

  def getPart(nstart: Int, nlimit: Int): Repr = {
    require(this.start <= nstart && (this.limit >= total && nlimit >= total - nstart || this.limit >= (nlimit + (nstart - this.start))), "incompatible limits: has(" + this.start + "," + this.limit + ") needs (" + nstart + "," + nlimit + ")")

    val t = total

    if (nstart == start && nlimit == limit)
      this.asInstanceOf[Repr]
    else {
      val self = this.drop(nstart - start).take(nlimit);
      subInstance(t, nstart, nlimit, self.iterator)
    }
  }

  def todString() = {
    "PartialIterable(total=" + total + ", start=" + start + ", limit=" + limit + ", elems=" + iterator.mkString("[", ",", "]")
  }

  protected def subInstance(_total: Int, _start: Int, _limit: Int, it: => Iterator[T]): Repr

}

class DefaultPartialIterable[T](val total: Int, val start: Int, val limit: Int, it: => Iterator[T]) extends PartialIterable[T] {

  def this(iterable: Iterable[T]) = this(iterable.size, 0, iterable.size, iterable.iterator)

  type Repr = DefaultPartialIterable[T]

  def iterator = it

  protected def subInstance(_total: Int, _start: Int, _limit: Int, it: => Iterator[T]) = new DefaultPartialIterable(_total, _start, _limit, it)
}
