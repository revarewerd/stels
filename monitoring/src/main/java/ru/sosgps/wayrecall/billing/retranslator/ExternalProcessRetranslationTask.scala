package ru.sosgps.wayrecall.billing.retranslator


import java.util.Date
import scala.sys.process.{ProcessLogger, Process, ProcessBuilder}
import org.joda.time.format.ISODateTimeFormat
import java.nio.file.Paths
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import java.util.concurrent.{SynchronousQueue, LinkedBlockingQueue, TimeUnit}
import scala.concurrent.ExecutionContext

//   val model = Model("id", "name", "host", "port", "processed", "status")

private object ExternalProcessRetranslationTask {
  private val pool = new ScalaExecutorService("RetranslationTaskBlockers", 0, Integer.MAX_VALUE, true, 5, TimeUnit.DAYS, new SynchronousQueue[Runnable]())
}

abstract class ExternalProcessRetranslationTask extends grizzled.slf4j.Logging with RetranslationTask {

  override def completedCount = _completed

  override def totalCount = _total

  @volatile
  private[this] var _exitCode: Option[Int] = None

  def exitCode: Option[Int] = _exitCode

  override def status = exitCode match {
    case None => "В обработке"
    case Some(0) => "Выполнено"
    case Some(x) => "Ошибка(" + x + ")"
  }

  protected[this] var _completed = 0

  protected[this] var _total = 0

  protected[this] val regex = """.*sending (\d+)/(\d+)""".r

  protected[this] val format = ISODateTimeFormat.dateHourMinute()

  protected[this] def f(d: Date) = format.print(d.getTime)

  protected[this] val wrc_home = System.getenv("WAYRECALL_HOME")

  protected[this] val pl = new ProcessLogger {

    override def err(s: => String): Unit = error(s)

    override def out(s: => String): Unit =
      tryDectectProgress(s)

    override def buffer[T](f: => T): T = f
  }

  private[this] def tryDectectProgress(s: => String) {
    debug(s)
    s match {
      case regex(c, t) =>
        debug(" -> " + c + " / " + t)
        _completed = c.toInt
        _total = t.toInt

      case _ =>
    }
  }

  protected[this] var processBuilder: ProcessBuilder = null

  protected[this] var process: Process = null

  override def kill() {
    process.destroy()
  }

  override def start() {
    processBuilder = createProcess()
    process = processBuilder.run(pl)



    ExternalProcessRetranslationTask.pool.future {
      try {
        _exitCode = Some(process.exitValue())
        debug("completed _exitCode=" + _exitCode)
      } catch {
        case e: Throwable =>
          _exitCode = Some(-3)
          warn("exitFailed with=" + e.getMessage, e)
      }
    }

  }

  protected def createProcess(): ProcessBuilder

}

class WialonRetranslationTask(val name: String,
                              val host: String,
                              val port: Int,
                              val uids: Seq[String],
                              val dbname: String,
                              val from: Date,
                              val to: Date
                               ) extends ExternalProcessRetranslationTask with grizzled.slf4j.Logging {

  def createProcess() = {
    val command = s"""java -jar ${Paths.get(wrc_home, "bin", "tools.jar").toString} send --addr $host:$port -f ${f(from)} -t ${f(to)} -db $dbname """ +
      uids.map("-u " + _).mkString(" ")
    debug("command = " + command)
    Process(command)
  }

}

class ODSMosRuRetranslationTask(val uids: Seq[String],
                                val instance: String,
                                val from: Date,
                                val to: Date
                                 ) extends ExternalProcessRetranslationTask with grizzled.slf4j.Logging {

  def createProcess() = {
    //java -jar -Xmx256m /home/niks/Wayrecallhome/bin/odsmosru.jar send -f 2014-02-18T00:00 -t 2014-03-10T00:00 -i 13226008592947
    val command = s"""java -Xmx256m -jar ${Paths.get(wrc_home, "bin", "odsmosru.jar").toString} send -f ${f(from)} -t ${f(to)} --instance $instance """ +
      uids.map("-u " + _).mkString(" ")
    debug("command = " + command)
    Process(command)
  }

  override val port: Int = 80

  override val host: String = "odsmosru"

  override val name: String = "odsmosru"
}
