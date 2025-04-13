package ru.sosgps.wayrecall.initialization

import java.io.IOException
import java.util.Comparator
import java.util.concurrent.{SynchronousQueue, TimeUnit}
import javax.servlet.ServletException
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}


import org.eclipse.jetty.server.{Server, Request, Handler}
import org.eclipse.jetty.server.handler.{ContextHandler, HandlerCollection}
import org.eclipse.jetty.util.component.AbstractLifeCycle
import org.eclipse.jetty.util.{ArrayUtil, MultiException}
import org.eclipse.jetty.util.annotation.ManagedAttribute
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService

import scala.concurrent.{Future, future}


import scala.collection.mutable.{ListBuffer, ArrayBuffer}
import scala.collection.mutable

/**
 * Created by nickl on 27.08.14.
 */
class AsyncHandlerCollection extends HandlerCollection(true) {

  private implicit val exctxt = new ScalaExecutorService("AsyncHandlerCollection", 4, 10, true, 1,
    TimeUnit.MINUTES, new SynchronousQueue[Runnable]).executionContext

  private val cmp = new Comparator[ContextHandler]() {
    override def compare(o1: ContextHandler, o2: ContextHandler): Int = {
      (o1, o2) match {
        case (o1: ContextHandler, o2: ContextHandler) =>
          val path1 = o1.getVirtualHosts.mkString("{",",","}")+o1.getContextPath
          val path2 = o2.getVirtualHosts.mkString("{",",","}")+o2.getContextPath
          val c1 = path2.length - path1.length
          if (c1 == 0)
            path1.compareTo(path2)
          else
            c1

        case _ => 0
      }
    }
  }
  //private val handlers = new ArrayBuffer[Handler]() with mutable.SynchronizedBuffer[Handler]
  private val handlers = new mutable.TreeSet[ContextHandler]()(Ordering.comparatorToOrdering(cmp)) with mutable.SynchronizedSet[ContextHandler]

  val pendingFutures = new mutable.HashSet[Future[Any]]() with mutable.SynchronizedSet[Future[Any]]

  /**
   * @return Returns the handlers.
   */
  @ManagedAttribute(value = "Wrapped handlers", readonly = true)
  override def getHandlers: Array[Handler] = {
    return handlers.toArray
  }


  /* ------------------------------------------------------------ */
  /**
   * @param handlers The handlers to set.
   */
  override def setHandlers(handlers: Array[Handler]) = handlers.synchronized {

    val hs = getHandlers
    hs.foreach(removeHandler)
    Option(handlers).getOrElse(Array.empty).foreach(addHandler)


    //    if (!_mutableWhenRunning && isStarted) throw new IllegalStateException(AbstractLifeCycle.STARTED)
    //    if (handlers != null) for (handler <- handlers) if (handler.getServer ne getServer) handler.setServer(getServer)
    //    updateBeans(_handlers, handlers)
    //    _handlers = handlers
  }


  override def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {

    val _handlers = handlers.toList

//    println("_handlers="+_handlers.map({
//      case ctxt: ContextHandler => ctxt.getVirtualHosts.mkString("{",",","}")+ctxt.getContextPath
//      case _ => "[none]"
//    }).mkString("[",",","]"))

    val requestHost = request.getServerName;
    //println("requestHost="+requestHost)

    if (_handlers != null && isStarted) {
      var mex: MultiException = null

      for (h <- _handlers;  if h.getVirtualHosts.contains(requestHost)) {
        try {
          h.handle(target, baseRequest, request, response)
          //println(h.asInstanceOf[ContextHandler].getContextPath+" "+ baseRequest.isHandled)

          if(baseRequest.isHandled)
            return
        }
        catch {
          case e: IOException => {
            throw e
          }
          case e: RuntimeException => {
            throw e
          }
          case e: Exception => {
            if (mex == null) mex = new MultiException
            mex.add(e)
          }
        }
      }

      if (mex != null) {
        if (mex.size == 1) throw new ServletException(mex.getThrowable(0))
        else throw new ServletException(mex)
      }
    }
  }

  override def setServer(server: Server) {
    super.setServer(server)
    val handlers: Array[Handler] = getHandlers
    if (handlers != null) for (h <- handlers) h.setServer(server)
  }

  override def addHandler(handler: Handler) {
    handler match {
      case handler: ContextHandler =>
        if (handler.getServer ne getServer) handler.setServer(getServer)
        val f = future {
          addBean(handler)
          handlers += handler
        }
        pendingFutures += f
        f.onComplete {
          case _ => pendingFutures -= f
        }
      case _ => throw new IllegalArgumentException("handler:" + handler + " must be ContextHandler")
    }
  }

  override def removeHandler(handler: Handler) {
    handler match {
      case handler: ContextHandler =>
        handlers -= handler
      case _ =>
    }
  }

  protected override def expandChildren(list: java.util.List[Handler], byClass: Class[_]) {
    if (getHandlers != null) for (h <- getHandlers) expandHandler(h, list, byClass)
  }

  override def destroy {
    if (!isStopped) throw new IllegalStateException("!STOPPED")
    val children: Array[Handler] = getChildHandlers
    setHandlers(null)
    for (child <- children) child.destroy
    super.destroy
  }


}
