package ru.sosgps.wayrecall

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.{LocalDateTime, ZoneId}
import java.util.regex.Pattern
import java.util.{Date, Properties, TimerTask}
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.query.Imports
import org.bson.types.ObjectId
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import scala.reflect.runtime.universe.{TypeTag, typeOf}


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.09.12
 * Time: 15:54
 * To change this template use File | Settings | File Templates.
 */
package object utils extends grizzled.slf4j.Logging{
  def measureTime(f: => Any): Long = {
    val start = System.currentTimeMillis
    f
    System.currentTimeMillis - start
  }

  /**
   * prints to stdout the time spend on computation of lambda function.
   */
  def logTime(f: => Any): Unit = {
    println("elapsed time= " + measureTime(f))

  }

  implicit class impldotChain[T](val self: T) extends AnyVal {
    def applySelf(f: T => Unit): T = {
      f(self);
      self
    }

    def asInstanceOpt[L: Manifest]: Option[L] = Option(self).collect({ case e: L => e })

    @inline
    def ? : Option[T] = Option(self)
  }

  implicit def impObjectId(idStr: String): ObjectId = new ObjectId(idStr)

  def parseDate(any: AnyRef): Date ={
    any match {
      case l: java.lang.Long => new Date(l)
      case s: String => parseDate(s)
    }
  }


  def parseDate(string: String): Date = {
    if (string.matches("\\d+"))
      new Date(string.toLong)
    else
      try
        new Date(string)
      catch {
        case e: IllegalArgumentException => {
          org.joda.time.format.ISODateTimeFormat.dateTimeParser().parseDateTime(string).toDate
        }
      }
  }

  def ISODateTime(date: Date): String = org.joda.time.format.ISODateTimeFormat.dateTime().print(new org.joda.time.DateTime(date))

  implicit class java8TLocalDateTimeOps(date: LocalDateTime) {

    def toDate: Date = toDate(ZoneId.systemDefault())

    def toDate(id: ZoneId): Date = Date.from(date.atZone(id).toInstant)

    def toMSKDate: Date = toDate(MSK)

  }

  val MSK: ZoneId = ZoneId.of("Europe/Moscow")

  val UTC: ZoneId = ZoneId.of("UTC")

  implicit def typingMap[K, V](map: collection.Map[K, V]): TypingMapWrapper[K, V] = new TypingMapWrapper(map)

  implicit def typingMapJava[K, V](map: java.util.Map[K, V]): TypingMapWrapper[K, V] =
    new TypingMapWrapper(scala.collection.JavaConversions.mapAsScalaMap(map))

  class TypingMapWrapper[K, V](map: collection.Map[K, V]) {

    def as[T](key: K): T = map(key).asInstanceOf[T]

    def getAs[T](key: K): Option[T] = map.get(key).map(_.asInstanceOf[T])
  }

  implicit class typingMutableMap[K, V](map: collection.mutable.Map[K, V]) extends TypingMapWrapper(map){

    def removeAs[T](key: K): Option[T] = map.remove(key).map(_.asInstanceOf[T])
  }

  def containsMatcher(searchstring: String): (String) => Boolean = {
    val matcher = Option(searchstring).filter(_ != "").
      map(str =>
      containsPattern(str)
      )

    (arg: String) => matcher.map(p => p.matcher(arg).matches()).getOrElse(true)
  }


  def containsPattern(str: String): Pattern = {
    Pattern.compile(
      (".*" + Pattern.quote(str) + ".*"),
      Pattern.CASE_INSENSITIVE)
  }

  def tryLong(arg: Any): Long = {
    arg match {
      case l: Long => l
      case i: Int => i
      case d: Double => d.toLong
      case l: java.lang.Long => l
      case i: java.lang.Integer => i.toLong
      case d: java.lang.Double => d.toLong
      case s: String => s.toLong
    }
  }

  def tryDouble(arg: Any): Double = {
    arg match {
      case l: Long => l.toDouble
      case i: Int => i.toDouble
      case i: Short => i.toDouble
      case i: Byte => i.toDouble
      case d: Double => d.toDouble
      case d: Float => d.toDouble
      case l: java.lang.Long => l.toDouble
      case i: java.lang.Integer => i.toDouble
      case d: java.lang.Double => d.toDouble
      case d: java.lang.Float => d.toDouble
      case d: java.lang.Short => d.toDouble
      case d: java.lang.Byte => d.toDouble
      case s: String => s.toDouble
      case null => Double.NaN
      case o => {
        warn(s"tryDouble cant convert $o of class ${o.getClass}")
        Double.NaN
      }
    }
  }
  
  def tryInt(arg: Any): Int = {
    arg match {
      case l: Long => l.toInt
      case i: Int => i
      case i: Short => i
      case i: Byte => i
      case d: Double => d.toInt
      case l: java.lang.Long => l.toInt
      case i: java.lang.Integer => i.toInt
      case d: java.lang.Double => d.toInt
      case d: java.lang.Short => d.toInt
      case d: java.lang.Byte => d.toInt
      case s: String => s.toInt
      //case _ => 0
    }
  }

  def tryParseInt(s: String): Option[Int] = try {
    Some(s.toInt)
  } catch {
    case _: java.lang.NumberFormatException => None
  } 
  
  def tryParseDouble(s: String): Option[Double] = try {
    Some(s.toDouble)
  } catch {
    case _: java.lang.NumberFormatException => None
  }

  def tryParseIntBased(s: String, base:Int): Option[Int] = try {
    Some(Integer.parseInt(s, base))
  } catch {
    case _: java.lang.NumberFormatException => None
  }

  @deprecated("use funcLoadingCache")
  implicit def funcToCacheLoader[A, B](f: A => B): CacheLoader[A, B] = new CacheLoader[A, B] {
    def load(key: A): B = f(key)
  }

  implicit class tryNumerics(val a: Any) extends AnyVal {

    def tryDouble: Double = utils.tryDouble(a)

    def tryLong: Long = utils.tryLong(a)

    def tryInt: Int = utils.tryInt(a)

    def tryNum[T <: AnyVal : TypeTag]: T = {
      a match {
        case _ if typeOf[T] =:= typeOf[Double] => tryDouble.asInstanceOf[T]
        case _ if typeOf[T] =:= typeOf[Int] => tryInt.asInstanceOf[T]
        case _ if typeOf[T] =:= typeOf[Long] => tryLong.asInstanceOf[T]
      }
    }

  }

  implicit class funcLoadingCache[K, V](cb: CacheBuilder[K, V]) {

    def buildWithFunction[K1 <: K, V1 <: V](f: K1 => V1): LoadingCache[K1, V1] = {

      return cb.build(new CacheLoader[K1, V1] {
        def load(key: K1): V1 = f(key)
      })
    }
  }

  implicit class funcToTimerTask(f: => Any) extends TimerTask {
    def run() {
      try {
        f
      }
      catch {
        case e: Throwable => e.printStackTrace()
      }
    }
  }

  def runnable(f: => Any): Runnable = new Runnable {
    def run() {
      try {
        f
      }
      catch {
        case e: Throwable => {
          e.printStackTrace()
          throw e
        }
      }
    }
  }


  def plusPhone(phone: String): String = if (!phone.startsWith("+")) "+" + phone else phone

  def nonPlusPhone(phone: String): String = phone.stripPrefix("+")

  def anyplus(kv: (String, String)): Imports.DBObject = $or(kv._1 -> plusPhone(kv._2), kv._1 -> nonPlusPhone(kv._2))

  def getCostForToday(monthPrice: Long): Long = {
    val now = new DateTime()
    val maxDay = now.dayOfMonth().getMaximumValue
    val dailyPrice = (monthPrice / maxDay)
    if (now.getDayOfMonth != maxDay) {
      dailyPrice
    }
    else {
      monthPrice - dailyPrice * (maxDay - 1)
    }
  }

  def maxBy[A, B](x: A, y: A)(f: A => B)(implicit cmp: Ordering[B]): A = {
    if (cmp.gteq(f(x), f(y))) x else y
  }

  def loadProperties(props: Path): Properties = {
    val source: Properties = new Properties
    val reader: BufferedReader = Files.newBufferedReader(props, StandardCharsets.UTF_8)
    source.load(reader)
    reader.close
    return source
  }

  @inline
  def iff[T](f:Boolean)(func : => T): Option[T] ={
    if(f) Some(func) else None
  }

  def bit(option: Option[_]): Int = if (option.isDefined) 1 else 0

  def bit(option: Boolean): Int = if (option) 1 else 0

  implicit class longBitReads(val l: Long) extends AnyVal {
    @inline
    def ofShift(shift: Int, bits: Int): Long = (l >> shift) & ((1 << bits) - 1)
  }

  implicit class intBitReads(val l: Int) extends AnyVal {
    @inline
    def ofShift(shift: Int, bits: Int): Int = (l >> shift) & ((1 << bits) - 1)

    def bitString: String = {
      val s = java.lang.Integer.toBinaryString(l)
      ("0" * (32 - s.length)) + s
    }
  }

  implicit class byteBitReads(val l: Byte) extends AnyVal {
    @inline
    def bit(i: Int): Boolean = (l & (1 << i)) > 0

    def bitString: String = {
      val string = java.lang.Integer.toBinaryString(l)
      val s = string.substring(Math.max(0, string.length - 8), string.length)
      ("0" * (8 - s.length)) + s
    }
  }

  implicit class shortBitReads(val l: Short) extends AnyVal {
    @inline
    def bit(i: Int): Boolean = (l & (1 << i)) > 0

    def bitString: String = {
      val string = java.lang.Integer.toBinaryString(l)
      val s = string.substring(Math.max(0, string.length - 16), string.length)
      ("0" * (16 - s.length)) + s
    }
  }

  implicit def durationAsJavaDuration(duration: FiniteDuration): java.time.Duration = {
    val nanos = java.time.Duration.ofNanos(duration.toNanos)
    debug(s"durationAsJavaDuration($duration)=${nanos.toMillis}")
    nanos
  }

  def definitely(cond: => Boolean, message: String): Boolean = {
    if (!cond)
      throw new IllegalArgumentException(message)
    else
      true
  }

  implicit class ThreadLocalOps[T](val tl: ThreadLocal[T]) {

    def withValue[R](value: T)(f: => R): R = {
      val prev = tl.get()
      tl.set(value)
      try {
        f
      }
      finally {
        tl.set(prev)
      }
    }
  }

}
