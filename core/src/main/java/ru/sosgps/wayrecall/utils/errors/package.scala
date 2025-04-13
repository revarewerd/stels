package ru.sosgps.wayrecall.utils

import java.io.{StringWriter, PrintWriter}

import ru.sosgps.wayrecall.utils.concurrent.FuturesUtils

import scala.concurrent.Future

/**
 * Created by nickl on 19.02.15.
 */
package object errors {

  def stacktrace(t: Throwable) = {
    val writer = new StringWriter()
    val printWriter = new PrintWriter(writer)
    t.printStackTrace(printWriter)
    printWriter.close()
    writer.toString
  }

  @deprecated("use `ru.sosgps.wayrecall.utils.concurrent.FuturesUtils#runSafe` ")
  def safeFuture[T](f: => Future[T]) = FuturesUtils.runSafe(f)

}
