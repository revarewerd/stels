package ru.sosgps.wayrecall.utils.attempts

import org.junit.{Test, Assert}
import scala._


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 07.12.12
 * Time: 20:23
 * To change this template use File | Settings | File Templates.
 */
class AttemptsTest {

  @Test
  def test1 {

    val calk = Iterator[() => Any](
      () => throw new IllegalArgumentException,
      () => throw new IllegalArgumentException,
      () => throw new IllegalArgumentException,
      () => 1)


    new Attempts({
      case e: IllegalArgumentException => Retry
    }).doTry{
      val next = calk.next()
      next.apply()
    }
  }

  @Test
  def test2 {

    val calk = Iterator[() => Any](
      () => throw new IllegalArgumentException,
      () => throw new IllegalArgumentException,
      () => throw new IllegalArgumentException,
      () => throw new IllegalArgumentException,
      () => 1)

    try {

      new Attempts({
        case e: IllegalArgumentException => Retry
      }, maxRetryAttempts = 3).doTry{
        val next = calk.next()
        next.apply()
      }

      Assert.fail("AttemptsExhaustedException must have been thrown")

    }
    catch {
      case e: AttemptsExhaustedException =>
    }
  }

  @Test
  def test3 {

    val calk = Iterator[() => Any](
      () => throw new IllegalArgumentException,
      () => throw new IllegalArgumentException,
      () => throw new IllegalArgumentException,
      () => throw new ArithmeticException(),
      () => 1)

    try {

      new Attempts({
        case e: IllegalArgumentException => Retry
      }).doTry{
        val next = calk.next()
        next.apply()
      }

      Assert.fail("ArithmeticException must have been thrown")

    }
    catch {
      case e: ArithmeticException =>
    }
  }

  @Test
  def test4 {

    val calk = Iterator[() => Any](
      () => throw new IllegalArgumentException,
      () => throw new IllegalArgumentException,
      () => throw new ArithmeticException(),
      () => throw new IllegalStateException("must not be reached"),
      () => 1)


    new Attempts({
      case e: IllegalArgumentException => Retry
      case e: ArithmeticException => Skip
    }).doTry{
      val next = calk.next()
      next.apply()
    }

  }

  class SomeErrsBefore[T](count: Int, err: => Nothing, v: => T) extends (() => T) {

    val it: Iterator[() => T] = Iterator.continually(err).take(count) ++ Iterator(() => v)

    def apply(): T = {
      it.next().apply()
    }

  }

  @Test
  def test5 {

    val calk = Seq[() => Int](
      () => 1,
      () => 2,
      new SomeErrsBefore[Int](2, throw new IllegalArgumentException, 3),
      () => 4,
      new SomeErrsBefore[Int](6, throw new IllegalArgumentException, 5)
    )


    val attempts: Attempts = new Attempts({
      case e: IllegalArgumentException => Retry
      case e: ArithmeticException => Skip
    })


    val r: Seq[Int] = calk.map(f => attempts.doTry(f.apply()).get)

    Assert.assertArrayEquals(Array(1, 2, 3, 4, 5), r.toArray)


  }

  @Test
  def testSkip2 {

    val calk = Seq[() => Int](
      () => 1,
      () => 2,
      () => throw new ArithmeticException,
      () => throw new ArithmeticException,
      new SomeErrsBefore[Int](2, throw new IllegalArgumentException, 3),
      () => 4,
      () => throw new ArithmeticException,
      () => throw new ArithmeticException,
      () => throw new ArithmeticException,
      () => throw new ArithmeticException,
      new SomeErrsBefore[Int](6, throw new IllegalArgumentException, 5)
    )

    try {

      val attempts2: Attempts = new Attempts({
        case e: IllegalArgumentException => Retry
        case e: ArithmeticException => Skip
      }, maxSkip = 3)

      calk.foreach(f => attempts2.doTry(f.apply()))

      Assert.fail("AttemptsExhaustedException must have been thrown")

    }
    catch {
      case e: AttemptsExhaustedException =>
    }

  }


  @Test
  def testSkip1 {
    val calk = Seq[() => Int](
      () => 1,
      () => 2,
      () => throw new ArithmeticException,
      () => throw new ArithmeticException,
      new SomeErrsBefore[Int](2, throw new IllegalArgumentException, 3),
      () => 4,
      () => throw new ArithmeticException,
      () => throw new ArithmeticException,
      () => throw new ArithmeticException,
      () => throw new ArithmeticException,
      new SomeErrsBefore[Int](6, throw new IllegalArgumentException, 5)
    )


    val attempts: Attempts = new Attempts({
      case e: IllegalArgumentException => Retry
      case e: ArithmeticException => Skip
    })

    val r: Seq[Int] = calk.flatMap(f => attempts.doTry(f.apply()))

    Assert.assertArrayEquals(Array(1, 2, 3, 4, 5), r.toArray)
  }


  @Test
  def testSkipAfterRetry() {

    val calk = Seq[() => Int](
      () => 1,
      () => 2,
      () => throw new IllegalArgumentException,
      () => 3,
      () => throw new IllegalArgumentException,
      () => throw new ArithmeticException,
      () => 4
    )


    val attempts: Attempts = new Attempts({
      case e: IllegalArgumentException => Retry
      case e: ArithmeticException => Skip
    }, skipAfterRetries = true)

    val r: Seq[Int] = calk.flatMap(f => attempts.doTry(f.apply()))

    Assert.assertArrayEquals(Array(1, 2, 3, 4), r.toArray)

  }

  @Test
  def testSkipAfterRetryFail() {

    try {

      val calk = Seq[() => Int](
        () => 1,
        () => 2,
        () => throw new IllegalArgumentException,
        () => 3,
        () => throw new IllegalArgumentException,
        () => throw new IllegalArgumentException,
        () => throw new IllegalArgumentException,
        () => throw new IllegalArgumentException,
        () => throw new IllegalArgumentException,
        () => throw new IllegalArgumentException,
        () => 4
      )


      val attempts: Attempts = new Attempts({
        case e: IllegalArgumentException => Retry
        case e: ArithmeticException => Skip
      }, skipAfterRetries = true)

      val r: Seq[Int] = calk.flatMap(f => attempts.doTry(f.apply()))

      Assert.fail("AttemptsExhaustedException must have been thrown")
    }
    catch {
      case e: AttemptsExhaustedException =>
    }
  }

}
