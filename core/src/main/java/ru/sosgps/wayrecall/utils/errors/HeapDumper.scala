package ru.sosgps.wayrecall.utils.errors

import java.lang.management.ManagementFactory

/**
 * Created by nickl on 15.02.15.
 */
class HeapDumper extends grizzled.slf4j.Logging {

  private var lastDumpTime = 0L

  def getProcessId( fallback: =>String): String = {
    // Note: may fail in some JVM implementations
    // therefore fallback has to be provided

    // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
    val jvmName = ManagementFactory.getRuntimeMXBean().getName();
    val index = jvmName.indexOf('@');

    if (index < 1) {
      // part before '@' empty (index = 0) / '@' not found (index = -1)
      return fallback;
    }

    try {
      return (java.lang.Long.parseLong(jvmName.substring(0, index))).toString;
    } catch  {
      case e: NumberFormatException =>   // ignore
    }
    return fallback;
  }

  def dumpHeap():Unit = synchronized{
   val now = System.currentTimeMillis();

    if(now - lastDumpTime < 5*60 * 1000)
      return
    lastDumpTime = now

    val pid = getProcessId("")

    import scala.sys.process.Process

    val filename = s"dump-$now.hprof"
    Process(s"jmap -dump:format=b,file=$filename $pid").#&&(Process(s"xz -0 $filename")).run()
    debug(s"heapdump done to $filename in ${System.currentTimeMillis()-now} mills")
  }

}
