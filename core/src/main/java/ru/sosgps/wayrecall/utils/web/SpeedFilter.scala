package ru.sosgps.wayrecall.utils.web

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper}

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConversions.enumerationAsScalaIterator
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import kamon.Kamon
import ru.sosgps.wayrecall.utils.impldotChain

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 19.09.13
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */
class SpeedFilter extends javax.servlet.Filter with grizzled.slf4j.Logging {
  def init(filterConfig: FilterConfig) {}

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {

    val isMultipart = Option(request.getContentType).exists(_.contains("multipart"))

    if (!isMultipart && request.asInstanceOpt[HttpServletRequest].map(http =>
      !Option(http.getQueryString).getOrElse("").contains("Atmosphere")
    ).getOrElse(true)) {

      val startTime = System.currentTimeMillis()
      var success = false

      val payLoadByteArray = try {
        //preread parameter to make request.getInputStream return only body
        request match {
          case httpr: HttpServletRequest => httpr.getParameterMap
          case _ =>
        }
        readFully(request.getInputStream)
      }
      catch {
        case e: Exception =>
          warn("exception reading params:", e)
          readFully(request.getInputStream)
      }



      val target = try {
        val payload = ScalaCollectionJson.readValue(payLoadByteArray, classOf[Map[String, AnyRef]])
        "extJsMethod:" + payload("action") + "-" + payload("method")
      } catch {
        case e: Exception =>
          "uri:" + (request match {
            case httpr: HttpServletRequest => httpr.getRequestURI
            case req => ""
          })
      }


      try {
        chain.doFilter(replacePayload(request, payLoadByteArray), response)
        success = true
      } catch {
        case e: java.io.EOFException => throw e;
        case e: Throwable =>
          val exceptionChainString = Iterator.iterate(e)(_.getCause).takeWhile(null !=).map(_.getClass.getSimpleName).mkString("-")
          Kamon.metrics.counter("http-errors", Map("target" -> target, "exception" -> exceptionChainString)).increment()
          warn("caught by speedfliter", e); throw e;
      }
      finally {
        val spentTime = System.currentTimeMillis() - startTime

        Kamon.metrics.histogram("http-query-time", Map("target" -> (target + (if (!success) ",fault" else "")))).record(spentTime)

        if (spentTime > 3000 || !success || payLoadByteArray.length > 200000) {

          def rqstring = request match {
            case httpr: HttpServletRequest =>
              printHttpRequest(httpr) + " payload=" + new String(payLoadByteArray.take(1000))
            case req => "ServletRequest " + printParamMap(req)
          }

          warn((if (spentTime > 3000) "Slow" else "Unsuccessfull") + " request " + (if (success) "success" else "failed") + " " + spentTime + "ms " + rqstring)
        }

      }
    }
    else
      chain.doFilter(request, response)


  }


  private[this] def readFully(is: InputStream): Array[Byte] = {
    val buff = new Array[Byte](1024)
    var len = 0;
    val arrayOutputStream = new ByteArrayOutputStream()
    while ( {len = is.read(buff); len != -1}) {
      arrayOutputStream.write(buff, 0, len)
    }

    val payLoadByteArray = arrayOutputStream.toByteArray
    payLoadByteArray
  }

  private def replacePayload(request: ServletRequest, payLoadByteArray: Array[Byte]): ServletRequest = {

    request match {
      case request: HttpServletRequest => new PayLoadWrappedHttpServletRequest(request, payLoadByteArray)
      case `request` => new ServletRequestWrapper(request) {
        val servletInputStream = new ServletInputStream {
          private val stream = new ByteArrayInputStream(payLoadByteArray)

          def read() = stream.read()
        }

        override def getInputStream = servletInputStream
      }
    }

  }

  def destroy() {}
}

class PayLoadWrappedHttpServletRequest(wrapped: HttpServletRequest, val payLoadByteArray: Array[Byte])
  extends HttpServletRequestWrapper(wrapped) {
  private val servletInputStream = new ServletInputStream {
    private val stream = new ByteArrayInputStream(payLoadByteArray)

    def read() = stream.read()
  }

  override def getInputStream = servletInputStream
}
