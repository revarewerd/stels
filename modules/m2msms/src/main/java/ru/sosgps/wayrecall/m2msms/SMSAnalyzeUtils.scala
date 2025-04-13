package ru.sosgps.wayrecall.m2msms

import SendSms.richMessage
import ru.sosgps.wayrecall.m2msms.generated.MessageInfo
import scala.collection.mutable.ArrayBuffer
import scala.collection.TraversableLike

object SMSAnalyzeUtils {

  def splitSentResponse(smses: Seq[MessageInfo]): Seq[(Seq[MessageInfo], Seq[MessageInfo])] = {

    val reader = new PredicateReader(smses.filter(p => p.incoming || p.delivered))

    val res = new ArrayBuffer[(Seq[MessageInfo], Seq[MessageInfo])](5)

    while (reader.remains.nonEmpty) {
      val in = reader.takeWhile(_.incoming)
      val out = reader.takeWhile(!_.incoming)
      res.+=((in, out))
    }

    res
  }

}

class PredicateReader[T, Coll[T] <: TraversableLike[T, Coll[T]]](orig: Coll[T]) {

  var remains: Coll[T] = orig

  def takeWhile(f: (T) => Boolean): Coll[T] = {
    val (read, tail) = remains.span(f)
    remains = tail
    read
  }


}

