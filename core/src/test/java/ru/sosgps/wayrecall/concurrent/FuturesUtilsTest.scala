package ru.sosgps.wayrecall.concurrent

import java.util.concurrent.TimeoutException

import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.utils.concurrent.FuturesUtils.extensions

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.DurationInt

/**
  * Created by nickl-mac on 19.05.16.
  */
class FuturesUtilsTest {


  @Test
  def timeoutTest(): Unit = {


    val never = Promise()

    val timedOut = never.future.withTimeout(30.millis)

    try {
      Await.result(timedOut, 1.second)
      Assert.fail("exception should had been thrown")
    } catch {
      case e: TimeoutException => //Ok
    }


  }

}
