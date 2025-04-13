package ru.sosgps.wayrecall.utils.concurrent

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.01.13
 * Time: 18:32
 * To change this template use File | Settings | File Templates.
 */
class LimitedLifo[T: ClassManifest](val maxsize: Int) {


  private[this] val internalStore = new Array[AnyRef](maxsize)

  private[this] var usedSize = 0;

  private[this] var lead = 0;

  def push(e: T): Iterable[T] = synchronized {
    if (usedSize < maxsize) {
      internalStore(realIndex(usedSize)) = e.asInstanceOf[AnyRef]
      usedSize = usedSize + 1
      Iterable.empty
    }
    else {
      val toPop = internalStore(lead)
      internalStore(lead) = e.asInstanceOf[AnyRef]
      lead = (lead + 1) % maxsize
      Iterable(toPop.asInstanceOf[T])
    }

  }


  def isEmpty(): Boolean = synchronized(usedSize == 0)

  def popAll(): Iterable[T] = synchronized {
    val builder = IndexedSeq.newBuilder[T]
    drain(builder +=)
    builder.result()
  }

  def drain(f: T => Any) = {
    while (!isEmpty()) {
      f(pop())
    }
  }

  def pop(): T = synchronized {
    require(usedSize > 0, "getting elem from empty stack")

    val toPop = internalStore(realIndex(usedSize - 1))
    internalStore(realIndex(usedSize - 1)) = null

    usedSize = usedSize - 1

    toPop.asInstanceOf[T]
  }

  private def realIndex(i: Int) = {
    (lead + i) % maxsize
  }

  override def toString = "LimitedLifo: " + internalStore.mkString("[", ",", "]") + " lead=" + lead + " usedSize=" + usedSize
}