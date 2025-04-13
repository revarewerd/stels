package ru.sosgps.wayrecall.watchdog

import java.lang.management._
import java.util
import java.util.Date
import javax.management.remote.{JMXConnector, JMXConnectorFactory, JMXServiceURL}

import com.beust.jcommander.{Parameters, Parameter}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import ru.sosgps.wayrecall.tools.Main
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.MailSender

import ru.sosgps.wayrecall.utils.concurrent.FuturesUtils._
import ru.sosgps.wayrecall.utils.errors.MailErrorReporter
import ru.sosgps.wayrecall.watchdog.JmxUtils.MBeanServerConnectionOps

import scala.concurrent.{Promise, Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.sys.process.{ProcessLogger, Process}
import scala.util.{Failure, Success}

/**
 * Created by nickl-mac on 17.08.15.
 */
@Parameters(commandDescription = "monitors server work via JMX and can restart server if it is needed")
object JMXRemoteWatcher extends CliCommand with grizzled.slf4j.Logging {

  @Parameter(
    names = Array("-u", "--url"),
    required = true,
    description = "JMX management url like: \"service:jmx:rmi:///jndi/rmi://wayrecall.ksb-stels.ru:6643/jmxrmi\" "
  )
  var url: String = null

  @Parameter(names = Array("-l", "--login"), required = false, description = "JMX login")
  var login: String = null

  @Parameter(names = Array("-p", "--password"), required = false, description = "JMX password")
  var password: String = null

  @Parameter(names = Array("--dumps-path"), required = false, description = "Path to heap dumps")
  var dumpsPath: String = ""

  @Parameter(names = Array("-m", "-g", "--memory-to-dump"), required = false, description = "memory to dump in GB")
  var maxMemory: Double = 6.0

  @Parameter(names = Array("-w", "--response-delay-limit"), required = false, description = "response delay limit in seconds")
  var normalDelayLimit = 60

  val hdInterval = 60 * 60 * 1000

  override val commandName: String = "jmxwatcher"

  private var lastKnownPid = -1

  private val logProcessor = ProcessLogger(
    line => info("external out: " + line),
    line => info("external err: " + line)
  )

  def process(): Unit = {

    val environment = new util.HashMap[String, AnyRef]();
    val credentials = for (l <- Option(login); p <- Option(password)) yield Array(l, p);
    credentials.foreach { credentials =>
      environment.put(JMXConnector.CREDENTIALS, credentials);
    }

    val jmxurl = new JMXServiceURL(url)
    //val jmxurl = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:7000/jmxrmi")


    def connectionLoop(): Future[Unit] = {

      val loopPrimise = Promise[Unit]()

      val connector = managedFuture(JMXConnectorFactory.connect(jmxurl, environment))

      connector
        .flatMap(connector => managedFuture {
        connector.connect();
        connector
      })
        .flatMap { connector =>
        val sharedLoading = managedFuture {
          new RemoteState(connector)
        }

        sharedLoading.flatMap { state =>
          import state._
          lastKnownPid = pid
          def workInConnection(): Future[Unit] = managedFuture {
            info("cpu:" + remote.cpuUsage)

            dumpHeapIfNedded(state)

            Thread.sleep(2000)
          }.flatMap(_ => workInConnection())

          workInConnection()
        }.andFinally(_ => connector.close())
      }.onComplete {
        case Success(value) =>
          info("suddenly success:" + value)
          loopPrimise.success()
        case Failure(e) =>
          info("everything failed:" + e)
          e.printStackTrace()
          try blocking {
            Thread.sleep(10000)
            info("restarting")

            killingRecordedPID()

            Thread.sleep(1000)
            val startCommand = "./monitoring.sh"
            info("running:" + startCommand)
            Process(startCommand) ! logProcessor
            errRep.notifyError(s"server $url was restarted ${new Date()}")
          } catch {
            case e: Exception => error("error restarting: ", e)
              errRep.notifyError(s"server $url restarting failed", e)
          }
          Thread.sleep(10000)
          loopPrimise.completeWith(connectionLoop())
      }

      loopPrimise.future
    }


    connectionLoop()
  }

  def killingRecordedPID(): Unit = {
    def kill(force: Boolean): Unit = {
      val killCommand = s"kill ${if (force) "-9" else ""} $lastKnownPid"
      val krv = Process(killCommand) ! logProcessor
      info(s"$killCommand returned $krv")
    }

    def checkAlive(): Boolean = {
      val checkCommand = s"kill -0 $lastKnownPid"
      val rv = Process(checkCommand) ! logProcessor
      info(s"$checkCommand returned $rv")
      rv == 0
    }
    if(lastKnownPid != -1) {
      kill(force = false)
      Thread.sleep(10000)
      if (checkAlive()) {
        kill(force = true)
      }
    }
  }

  def dumpHeapIfNedded(state: RemoteState): Any = {

    import state._
    val memoryGigs = memory.getHeapMemoryUsage.getUsed.toDouble / (1024 * 1024 * 1024)
    info("mem:" + memoryGigs)

    val now = System.currentTimeMillis()
    if (memoryGigs > maxMemory && (System.currentTimeMillis() - lastHeapDumpTime > hdInterval)) {
      lastHeapDumpTime = now
      Future {
        blocking {
          try {
            val filename = s"${dumpsPath}dump-$pid-$now.hprof"
            val command = s"jmap -dump:format=b,file=$filename $pid"
            info("starting command: " + command)
            Process(command).#&&(Process(s"xz -0 $filename"))
              .run(logProcessor)
            info(s"heapdump done to $filename in ${System.currentTimeMillis() - now} mills")
            errRep.notifyError(s"heap dump was build by reaching ${memoryGigs}Gb")
          } catch {
            case e: Exception => warn("heap dump exception:", e)
          }
        }
      }
    }
  }

  def managedFuture[T](action: => T): Future[T] = {
    Future(blocking(action)).withTimeout(normalDelayLimit.second)
  }


  val prps = Main.wrcProperties()

  val mailSender = new MailSender(
    email = prps.getProperty("global.sysemail"),
    host = prps.getProperty("global.sysemail.smtphost"),
    login = prps.getProperty("global.sysemail.login"),
    password = prps.getProperty("global.sysemail.password"),
    allowedAddress = prps.getProperty("global.sysemail.allowedto", "")
  )

  val errRep = {

    /*
            <property name="email" value="${global.sysemail}"/>
        <property name="host" value="${global.sysemail.smtphost}"/>
        <property name="login" value="${global.sysemail.login}"/>
        <property name="password" value="${global.sysemail.password}"/>
        <property name="allowedAddress" value="${global.sysemail.allowedto:}"/>
     */



    new MailErrorReporter(mailSender, prps.getProperty("global.errornotificationAddress"))
  }

}

class RemoteState(connector: JMXConnector) extends grizzled.slf4j.Logging {

  val remote = connector.getMBeanServerConnection
  val remoteRuntime = remote.proxy[RuntimeMXBean]
  val osRuntime = remote.proxy[OperatingSystemMXBean];
  val threadRuntime = remote.proxy[ThreadMXBean];
  val memory = remote.proxy[MemoryMXBean];

  private val remoteName = remoteRuntime.getName
  info(remoteName)
  info(osRuntime.getName)

  val pid = Option(remoteName.indexOf("@")).filter(_ > 0)
    .flatMap(i => utils.tryParseInt(remoteName.substring(0, i)))
    .getOrElse(throw new IllegalArgumentException(s"cant cat pid from $remoteName"))


  info(s"pid = $pid")

  val processorsCount = osRuntime.getAvailableProcessors()
  var lastHeapDumpTime = 0L

}
