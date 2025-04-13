package ru.sosgps.wayrecall.utils.io

import java.io.Writer

/**
 * Created by nickl on 24.02.14.
 */
object NullWriter extends Writer {
  override def close(): Unit = {}

  override def flush(): Unit = {}

  override def write(cbuf: Array[Char], off: Int, len: Int): Unit = {}
}
