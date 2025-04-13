package ru.sosgps.wayrecall.utils

import org.junit.{Assert, Test}


class PartialIterableTest {

  @Test
  def part1() {

    val seq1 = Seq.range(0, 20)

    val one = new DefaultPartialIterable[Int](seq1)

    try {
      val p1 = one.getPart(0, 19).getPart(0, 20)
      Assert.fail("IllegalArgumentException must has been thrown")
    }
    catch {case e: IllegalArgumentException => {}}

    val p1 = one.getPart(10, 10)
    Assert.assertTrue(one.size == p1.total)
    Assert.assertArrayEquals(Array(10, 11, 12, 13, 14, 15, 16, 17, 18, 19), p1.toArray)

    val p2 = one.getPart(10, 5)
    Assert.assertTrue(one.size == p2.total)
    Assert.assertArrayEquals(Array(10, 11, 12, 13, 14), p2.toArray)

    try {
      p1.getPart(0, 5)
      Assert.fail("IllegalArgumentException must has been thrown")
    }
    catch {case e: IllegalArgumentException => {}}

    val p3: DefaultPartialIterable[Int] = p1.getPart(12, 5)
    Assert.assertTrue(one.size == p3.total)
    Assert.assertArrayEquals(Array(12, 13, 14, 15, 16), p3.toArray)


  }


  @Test
  def part2() {

    val seq1 = Seq.range(0, 30)

    val one = new DefaultPartialIterable[Int](seq1)

    try {
      one.getPart(10,10).getPart(20,10)
      Assert.fail("IllegalArgumentException must has been thrown")
    }
    catch {case e: IllegalArgumentException => {}}

    try {
      one.getPart(10,10).getPart(15,10)
      Assert.fail("IllegalArgumentException must has been thrown")
    }
    catch {case e: IllegalArgumentException => {}}


    Assert.assertArrayEquals(one.getPart(0,100).toArray,seq1.toArray)

  }

  @Test
  def partialEnd() {

    val seq1 = Seq.range(0, 35)

    val one = new DefaultPartialIterable[Int](seq1)

    Assert.assertArrayEquals(one.getPart(30,10).toArray,seq1.drop(30).toArray)

  }


}
