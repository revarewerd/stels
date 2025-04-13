package ru.sosgps.wayrecall.utils

import org.junit.{Assert, Test}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.09.12
 * Time: 21:14
 * To change this template use File | Settings | File Templates.
 */
class LimitsSortsFiltersTest {


  @Test
  def empty {
    val empty = LimitsSortsFilters.empty

    Assert.assertTrue(empty.accepts(empty))
    Assert.assertTrue(empty.accepts(new LimitsSortsFilters()))
    Assert.assertTrue(empty.accepts(new LimitsSortsFilters(1, 20)))

  }

  @Test
  def inlusive {
    val one = new LimitsSortsFilters(2, 20)
    Assert.assertTrue(one.accepts(one))
    Assert.assertFalse(one.accepts(new LimitsSortsFilters(0, 10)))
    Assert.assertTrue(one.accepts(new LimitsSortsFilters(5, 10)))
  }

  @Test
  def exclusive {
    val one = new LimitsSortsFilters(100, 100)
    Assert.assertFalse(one.accepts(new LimitsSortsFilters(200, 100)))

  }

  @Test
  def zerozero {
    val one = new LimitsSortsFilters(0, 0)
    Assert.assertFalse(one.accepts(new LimitsSortsFilters(0, 100)))

  }

}
