package ru.sosgps.wayrecall.utils.concurrent

import org.junit.{Assert, Test}
import java.util.concurrent.{Callable, Executors, ExecutorService}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.01.13
 * Time: 17:02
 * To change this template use File | Settings | File Templates.
 */
class ConcurentTest1 {


  implicit def funcToCallable[T](f: => T) = new Callable[T] {
    def call() = f
  }

  @Test
  def testSemaphore() {

    val sem = new Semaphore(3)
    Assert.assertTrue(sem.availablePermits() == 3)

    val st = System.currentTimeMillis()

    val res = (1 to 5).par.map(i =>
      sem.acquired {
        Thread.sleep(100)
        i + 1
      })

    val d = System.currentTimeMillis() - st

    Assert.assertTrue(d > 200)
    Assert.assertArrayEquals(Array(2, 3, 4, 5, 6), res.toArray)
  }

  @Test
  def testSemaphore2() {

    val sem = new Semaphore(3)
    Assert.assertTrue(sem.availablePermits() == 3)

    val pool = Executors.newFixedThreadPool(4)

    val res = (1 to 5).map(i => pool.submit(
      sem.tryAcquired {
        Thread.sleep(30)
        i + 1
      })).map(_.get())

    Assert.assertEquals(res.flatten.length, 3)

  }


  @Test
  def testLimitedSimultaneous() {

    val sem = new LimitedSimultaneous(3)
    Assert.assertTrue(sem.maxLimit == 3)

    val pool = Executors.newFixedThreadPool(4)

    val res = (1 to 5).map(i => {
      Thread.sleep(i * 10)
      pool.submit(
        sem.processIfCan {
          Thread.sleep(200)
          i + 1
        })
    }).map(_.get())

    Assert.assertArrayEquals(Array(2, 3, 4), res.flatten.toArray)

  }


}
