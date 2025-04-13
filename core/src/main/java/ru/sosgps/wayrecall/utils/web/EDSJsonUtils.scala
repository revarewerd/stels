package ru.sosgps.wayrecall.utils.web



object EDSJsonResponse {

  import ch.ralscha.extdirectspring.bean.ExtDirectStoreResult
  //import collection.JavaConversions.asJavaCollection

  def apply[T](record: T) = new ExtDirectStoreResult[T](record)

  def apply[T <: AnyRef](record: Array[T]) = new ExtDirectStoreResult[T](record)

  def apply(record: Map[String, Any]) = apply[Map[String, Any]](record)

  def apply[T](records: Iterable[T]) = new ExtDirectStoreResult[T](toFakeJavaSeq(records))

  def apply[T](total: Int, records: Iterable[T]) = new ExtDirectStoreResult[T](total: Integer, toFakeJavaSeq(records))

  def forPart[T](t: (Int, Iterator[T])) = new ExtDirectStoreResult[T](t._1: Integer, toFakeJavaSeq(t._2))

  def apply[T](total: Int, records: Iterable[T], success: Boolean) = new ExtDirectStoreResult[T](total, toFakeJavaSeq(records), success)

  def apply[T](records: Iterator[T]) = new ExtDirectStoreResult[T](toFakeJavaSeq(records))

  def apply[T](total: Int, records: Iterator[T]) = new ExtDirectStoreResult[T](total: Integer, toFakeJavaSeq(records))

  def apply[T](total: Int, records: Iterator[T], success: Boolean) = new ExtDirectStoreResult[T](total, toFakeJavaSeq(records), success)

  private def toFakeJavaSeq[T](iterator: TraversableOnce[T]):java.util.Collection[T] =  collection.JavaConversions.asJavaCollection(new FakeSeq[T](iterator.toIterator))

//  private def toFakeJavaSeq[T](iterable: Iterable[T]):java.util.Collection[T] = toFakeJavaSeq(iterable.iterator)
//
//  private def toFakeJavaSeq[T](iterator: scala.Iterator[T]):java.util.Collection[T] =  collection.JavaConversions.asJavaCollection(new FakeSeq[T](iterator))
//
}

class FakeSeq[T](iter: Iterator[T]) extends Seq[T]{
  def length = -1 //throw new UnsupportedOperationException("FakeSeq length")

  def apply(idx: Int) = throw new UnsupportedOperationException("FakeSeq index")

  def iterator = iter
}

