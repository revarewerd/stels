package ru.sosgps.wayrecall.monitoring.deegree

import javax.servlet.http.{HttpServletRequestWrapper, HttpSession, HttpServletResponse, HttpServletRequest}
import com.google.common.cache.CacheBuilder
import ru.sosgps.wayrecall.utils.concurrent.{LimitedSimultaneous, LimitedLifo, Semaphore}
import ru.sosgps.wayrecall.utils.{funcLoadingCache, web}
import ru.sosgps.wayrecall.utils.web.ScalaServletConverters.sessionToMap
import ru.sosgps.wayrecall.utils.web.SessionAsMap
import javax.servlet._
import java.io.{File, PrintWriter}
import org.springframework.web.context.request.{ServletRequestAttributes, RequestAttributes, RequestContextHolder}
import scala.collection.mutable
import java.util.Date
import java.lang.Object
import java.lang
import java.util.concurrent._
import org.springframework.security.core.context.{SecurityContext, SecurityContextHolder}
import javax.imageio.ImageIO

class DeegreeServlet extends org.deegree.services.controller.OGCFrontController {

  private[this] final val log = grizzled.slf4j.Logger[this.type]

  private[this] val maxInQueue: Int = 50

  val semaphoreLimit: Int = 5

  private val TOO_MANY_REQUESTS = 429

  private val pool = new ThreadPoolExecutor(
    semaphoreLimit * 2,
    semaphoreLimit * 2,
    0L, TimeUnit.MILLISECONDS,
    new ArrayBlockingQueue[Runnable](50),
    new ThreadPoolExecutor.AbortPolicy
  )


  override def doPost(request: HttpServletRequest, response: HttpServletResponse) {
    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
  }

  private[this] var entered = 0;

  val processingQueueSet = new mutable.HashSet[LimitedLifo[StoredEntry]] with mutable.SynchronizedSet[LimitedLifo[StoredEntry]]

  override def init(config: ServletConfig) {
    super.init(config)

    ImageIO.setUseCache(false) // TODO: вообще правильнее это делать путем создания WMSController.imageSerializers
  }

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) {

    synchronized {
      entered = entered + 1
      log.trace("degreeservlet entered (" + entered + ")")
    }
    try {
      log.trace("entering sessionFilter (" + request.getQueryString + ")")
      sessionFilter(request, response)
      //onePerSession(request, response)
    }
    catch {
      case e: Throwable => {log.info("catching and rethrowing:" + e); throw e}
    }
    finally {
      synchronized {
        entered = entered - 1
        log.trace("degreeservlet left (" + entered + ")")
      }
    }

  }

  private val semaphores = CacheBuilder.newBuilder()
    .weakKeys()
    .buildWithFunction[HttpSession, Semaphore](ses => new Semaphore(1))

  private[this] def sessionFilter(request: HttpServletRequest, response: HttpServletResponse) {

    val ses: HttpSession = request.getSession()
    val sessionMap: SessionAsMap = ses
    val lock: Semaphore = semaphores(ses)
    val processingQueue = sessionMap.getTyped("DeegreeServletQueuedRequest", new LimitedLifo[StoredEntry](1))

    val async: AsyncContext = request.startAsync()

    val se: StoredEntry = makeStoredEntry(async)

    log.trace("out of lock creating(" + se)

    processingQueue.push(se).foreach(e => {
      sendCancelled(e)
    })
    log.trace("async(" + se + ") putted in queue")

    //
    if (processingQueueSet.add(processingQueue)) try {
      pool.submit(new Runnable {
        def run() {
          try {
            lock.acquired {
              processingQueueSet.remove(processingQueue)
              log.trace("draining processingQueue " + processingQueue.isEmpty())
              processingQueue.drain(processStored)
            }
          }
          catch {
            case e: Exception => log.error("error", e)
          }
        }
      })
    } catch {
      case e: Exception => processingQueueSet.remove(processingQueue)
    }

    log.trace("leaving sessionFilter")
  }


  def makeStoredEntry(async: AsyncContext): StoredEntry = {
    val currentRequestAttributes = RequestContextHolder.currentRequestAttributes().asInstanceOf[ServletRequestAttributes]

    val originalRequest = currentRequestAttributes.getRequest
    val servletContext = originalRequest.getServletContext
    require(servletContext != null)
    val se = new StoredEntry(async, new ServletRequestAttributes(new HttpServletRequestWrapper(originalRequest) {
      override def getServletContext: ServletContext = servletContext
    }))
    se
  }

  private[this] def sendCancelled(se: StoredEntry) = try {
    try {
      log.trace("TOO_MANY_REQUESTS (" + se + ")" + ")")
      //stored.dispatch("/images/ksb_logo_small.png")
      se.async.getResponse.asInstanceOf[HttpServletResponse].sendError(TOO_MANY_REQUESTS)
    } finally {
      se.async.complete()
    }
  } catch {
    case e: Exception => log.warn("sendCancelled exception: ", e)
  }

  private[this] val starttime = new Date().getTime

  def processStored(se: StoredEntry) {

    val currentRequestAttributes: RequestAttributes = scala.util.control.Exception.catching(classOf[IllegalStateException]).opt {
      RequestContextHolder.currentRequestAttributes()
    }.orNull

    val currentSecurityContext: SecurityContext = scala.util.control.Exception.catching(classOf[IllegalStateException]).opt {
      SecurityContextHolder.getContext
    }.orNull

    try {
      RequestContextHolder.setRequestAttributes(se.attr)
      SecurityContextHolder.setContext(se.security)
      log.trace("processing  stored(" + se + ")" + ")")
      processProtected(
        new NullPathWrapper(se.async.getRequest.asInstanceOf[HttpServletRequest]),
        se.async.getResponse.asInstanceOf[HttpServletResponse]
      )
      log.trace("finished stored(" + se + ")")
    }
    finally {
      se.async.complete()
      SecurityContextHolder.setContext(currentSecurityContext)
      RequestContextHolder.setRequestAttributes(currentRequestAttributes)
    }

  }

  private[this] lazy val limitedSimultaneous = new LimitedSimultaneous(maxInQueue);
  private[this] val globalSemaphore = new Semaphore(semaphoreLimit)

  def processProtected(request: HttpServletRequest, response: HttpServletResponse) {

    limitedSimultaneous.processIfCan(
      globalSemaphore.acquired {
        //log.trace("semaphore <- = " + (semaphoreLimit - globalSemaphore.availablePermits()) + "(" + globalSemaphore.getQueueLength + ")")
        //require(request.getServletContext != null, "request.getServletContext cant be null")
        //require(web.springWebContext.getServletContext  != null ,"web.springWebContext.getServletContext cant be null" )
        super.doGet(request, response)
        //log.trace("semaphore -> = " + (semaphoreLimit - globalSemaphore.availablePermits()) + "(" + globalSemaphore.getQueueLength + ")")
      }
    ).
      getOrElse(response.sendError(TOO_MANY_REQUESTS))

  }


  case class StoredEntry(async: AsyncContext, attr: RequestAttributes, security: SecurityContext = SecurityContextHolder.getContext, date: Date = new Date()) {
    override def toString = System.identityHashCode(this) + " time=" + (date.getTime - starttime) + " request=" + this.async.getRequest.asInstanceOf[HttpServletRequest].getQueryString
  }

}


class NullPathWrapper(request1: HttpServletRequest) extends HttpServletRequestWrapper(request1) {
  override def getPathInfo = null
}