package ru.sosgps.wayrecall.packreceiver.blocking

import scala.beans.BeanProperty
import java.net.{Socket, SocketTimeoutException, ServerSocket}
import java.util.concurrent._
import javax.annotation.{PreDestroy, PostConstruct}
import scala.Some
import ru.sosgps.wayrecall.utils.ResourceScope._
import scala.Some
import java.io.{FilterInputStream, OutputStream, InputStream, BufferedInputStream}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 09.03.13
 * Time: 17:53
 * To change this template use File | Settings | File Templates.
 */
abstract class SimpleServer extends grizzled.slf4j.Logging {

  def processConnection(in: InputStream, outgoing: OutputStream, firstTime: Boolean): Boolean

  @BeanProperty
  protected var port: Int

  @BeanProperty
  var soTimeout: Int = 5000

  @volatile
  protected var work = false;

  @BeanProperty
  var daemon: Boolean = true

  private var workingthread: Option[Thread] = None;

  private var serverSocket: Option[ServerSocket] = None;

  @BeanProperty
  var poolSize = 100

  private[this] val pool = {
    val blockingQueue: BlockingQueue[Runnable] = new SynchronousQueue[Runnable]();
    val rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy
    new ThreadPoolExecutor(1, poolSize, 20000L, TimeUnit.MILLISECONDS, blockingQueue, rejectedExecutionHandler);
  }


  def start(): Unit = {

    info("starting SimpleServer")

    val server = new Runnable() {

      override def run(): Unit = {

        serverSocket = try {
          Some(new ServerSocket(port))
        }
        catch {
          case e: java.net.BindException => {
            error("port:" + port, e);
            System.exit(3);
            throw new RuntimeException("error with port:" + port, e)
          }
        }
        try {

          info("SimpleServer work=" + work)
          info("listening " + serverSocket.get.getInetAddress + ":" + serverSocket.get.getLocalPort());

          val semaphore = new Semaphore(poolSize)
          while (work) {

            try {
              trace("acquiring semaphore")
              semaphore.acquire();
              trace("semaphore acquired" + "(" + semaphore.availablePermits() + "), waiting for connection " + serverSocket.get.getInetAddress + ":" + serverSocket.get.getLocalPort());
              val accept = serverSocket.get.accept();
              trace("accepted " + accept.getInetAddress + ":" + accept.getPort)
              accept.setSoTimeout(soTimeout)
              pool.submit(new Runnable {
                def run() {
                  try {
                    trace("start processing " + accept.getInetAddress + ":" + accept.getPort)
                    serverProcess(accept)
                  }
                  catch {
                    case e: SocketTimeoutException => trace("SocketTimeoutException while processing " + accept.getInetAddress + ":" + accept.getPort)
                    case e => error("error while processing " + accept.getInetAddress + ":" + accept.getPort, e)
                  }
                  finally {
                    trace("finished processing " + accept.getInetAddress + ":" + accept.getPort)
                    semaphore.release();
                  }
                }
              })

            }
            catch {
              case e: SocketTimeoutException =>
                warn(e.getMessage)
              case e: Exception => {
                warn("SimpleServer error:" + e.getMessage, e)
              }
            }

          }

          info("SimpleServer stopped")

        }
        finally {
          serverSocket.get.close()
          info("SimpleServer serverSocket closed in work")
        }
      }

    };

    work = true;
    info("SimpleServer started")
    workingthread = Some(new Thread(server, "SimpleServer thread"));
    workingthread.get.setDaemon(daemon);
    workingthread.get.start();

  }

  def stop(): Unit = {
    val oldWork = work
    work = false

    info("SimpleServer stopping work was:" + oldWork)

    this.serverSocket.foreach(servsock => {
      try {
        servsock.close();
        info("SimpleServer serverSocket closed in stop")
      }
      catch {
        case e: Exception => {
          warn("SimpleServer serverSocket was not closed in stop (" + e.getMessage() + ")")
        }
      }

    })

    workingthread.foreach(t => {
      t.interrupt
      info("DataAccomulator terminating")

      t.join(5000)
      if (t.isAlive)
        warn("DataAccomulator still alive (work is " + work + ")")
      else
        info("DataAccomulator thread terminated")

    })

  };


  protected def serverProcess(accept: Socket): Unit = {

    scope(p => {
      p.manage(accept)
      val incoming = p.manage(new BufferedInputStream(accept.getInputStream(), 1024));
      val outgoing = p.manage(accept.getOutputStream());

      var firstTime = true
      var continue = true
      while (work && continue) {

        new VirginInputStream(incoming).withVirgin(virgin => {
          continue = processConnection(virgin, outgoing, firstTime)
          firstTime = false
        }
        )

      }

    })
  }


}


class VirginInputStream(wrapped: InputStream) extends FilterInputStream(wrapped) {

  var isVirgin = true


  def withVirgin[T](func: (VirginInputStream) => T): T = {
    try {
      func(this)
    }
    catch {
      case e: Exception => {
        if (!isVirgin)
          throw new LostVirginityStreamException(e)
        else
          throw e;
      }
    }
  }


  private[this] def catchVirginity(func: => Int): Int = withVirgin {
    self => {
      val r = func
      if (r != -1)
        isVirgin = false;
      r
    }
  }


  override def read(): Int = catchVirginity(super.read());

  override def read(b: Array[Byte], off: Int, len: Int) = catchVirginity(super.read(b, off, len))

  override def read(b: Array[Byte]) = catchVirginity(super.read(b))
}

class LostVirginityStreamException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this(cause: Throwable) = this(cause.getMessage, cause)
}
