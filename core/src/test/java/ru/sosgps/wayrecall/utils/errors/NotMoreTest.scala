package ru.sosgps.wayrecall.utils.errors

import java.util.concurrent.atomic.AtomicInteger

import org.junit.Assert
import org.junit.Test

import scala.concurrent.duration.DurationLong

/**
 * Created by nickl on 25.05.15.
 */

class NotMoreTest {


  @Test
  def test(): Unit ={


    val ind = new AtomicInteger(0)

    val in = new NotMoreThanIn(100.milliseconds)

    (0 to 10).par foreach(_ => in{ind.incrementAndGet()})

    Assert.assertEquals(1, ind.get())
    Thread.sleep(100)
    (0 to 10) foreach(_ => in{ind.incrementAndGet()})
    Assert.assertEquals(2, ind.get())

  }


}
