package ru.sosgps.wayrecall.utils.errors

import java.io.Closeable

/**
  * Taken from https://github.com/rjmac/scala-query/blob/master/src/main/scala/org/scalaquery/util/CloseableIterator.scala
  */
trait CloseableIterator[+T] extends Iterator[T] with Closeable { self =>
  /**
    * Close the underlying data source. The behaviour of any methods of this
    * object after closing it is undefined.
    */


  private var closed = false

  private def doClose(): Unit = synchronized{
    if(!closed){
      close()
      closed = true
    }
  }

  override def map[B](f: T => B): CloseableIterator[B] = new CloseableIterator[B] {
    def hasNext = self.hasNext
    def next() = f(self.next())
    def close() = self.close()
  }
  final def use[R](f: (Iterator[T] => R)): R =
    try f(this) finally doClose()
  final def use[R](f: =>R): R =
    try f finally doClose()
  /**
    * Return a new CloseableIterator which also closes the supplied Closeable
    * object when itself gets closed.
    */
  final def thenClose(c: Closeable): CloseableIterator[T] = new CloseableIterator[T] {
    def hasNext = self.hasNext
    def next() = self.next()
    def close() = try self.close() finally c.close()
  }
  protected final def noNext = throw new NoSuchElementException("next on empty iterator")
}

object CloseableIterator{

  def iterateAndClose[T](collection: TraversableOnce[T], closer: => Unit): CloseableIterator[T] = new CloseableIterator[T] {

    private val iterator = collection.toIterator

    override def close(): Unit = closer

    override def hasNext: Boolean = iterator.hasNext

    override def next(): T = iterator.next()
  }


  def concatSeq[T](seq: Seq[CloseableIterator[T]]): CloseableIterator[T] = iterateAndClose(seq.flatten, seq.foreach(_.close()))

}

