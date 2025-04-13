package ru.sosgps.wayrecall.utils.concurrent

import org.junit.{Assert, Test}
import Assert._

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.01.13
 * Time: 18:51
 * To change this template use File | Settings | File Templates.
 */
class LimitedLifoTest {


  @Test
  def test1 {

    val lifo = new LimitedLifo[Int](4)

    lifo.push(2)
    assertEquals(2, lifo.pop())

    lifo.push(5)
    assertEquals(5, lifo.pop())

    lifo.push(5)
    lifo.push(7)
    assertEquals(7, lifo.pop())
    assertEquals(5, lifo.pop())

    lifo.push(5)
    lifo.push(8)
    assertTrue(lifo.push(12).isEmpty)
    assertTrue(lifo.push(13).isEmpty)
    assertEquals(5, lifo.push(15).head)
    lifo.push(18)
    lifo.push(10)
    lifo.push(3)

    assertEquals(3, lifo.pop())
    assertEquals(10, lifo.pop())
    assertEquals(18, lifo.pop())
    assertEquals(15, lifo.pop())

    try {
      lifo.pop()
      fail("exception must have been thrown")
    }
    catch {
      case e: IllegalArgumentException => {}
    }

  }

  @Test
  def test2 {

    val lifo = new LimitedLifo[Int](5)

    Range(1, 100).foreach(i => {

      lifo.push(5)
      lifo.push(8)
      lifo.push(12)
      lifo.push(13)
      lifo.push(15)
      assertEquals(15, lifo.pop())
      lifo.push(18)
      lifo.push(10)
      lifo.push(3)

      //println(lifo)

      assertEquals(3, lifo.pop())
      assertEquals(10, lifo.pop())
      assertEquals(18, lifo.pop())
      assertEquals(13, lifo.pop())
      assertEquals(12, lifo.pop())

      //println(lifo)

      try {
        lifo.pop()
        fail("exception must have been thrown")
      }
      catch {
        case e: IllegalArgumentException => {}
      }
    })
  }

  @Test
  def test3 {

    val lifo = new LimitedLifo[Int](1)

    assertTrue(lifo.push(12).isEmpty)
    assertEquals(12, lifo.push(15).head)
    assertEquals(15, lifo.pop())

    assertTrue(lifo.push(8).isEmpty)
    assertEquals(8, lifo.push(2).head)
    assertEquals(2, lifo.pop())

  }

  @Test
  def testPopAll {

    val lifo = new LimitedLifo[Int](5)

    assertTrue(lifo.isEmpty())

    lifo.push(2)
    lifo.push(3)
    lifo.push(4)
    lifo.push(5)
    lifo.push(6)
    lifo.push(7)

    assertArrayEquals(Array(7,6,5,4,3),lifo.popAll().toArray)

    assertTrue(lifo.isEmpty())


  }


}
