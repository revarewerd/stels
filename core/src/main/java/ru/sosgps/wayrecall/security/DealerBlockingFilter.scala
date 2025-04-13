package ru.sosgps.wayrecall.security

import java.lang
import java.util.concurrent.TimeUnit
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterChain, FilterConfig, ServletRequest, ServletResponse}

import com.google.common.cache.CacheBuilder
import com.mongodb.ReadPreference
import com.mongodb.casbah.commons.MongoDBObject
import ru.sosgps.wayrecall.initialization.MultiDbManager
import ru.sosgps.wayrecall.utils.funcLoadingCache

/**
 * Created by IVAN on 24.03.2016.
 */

class DealerBlockingFilter  extends javax.servlet.Filter with grizzled.slf4j.Logging{

  def destroy {
  }
  def init(config: FilterConfig) {
  }

  private val dealersStatesCache = CacheBuilder.newBuilder()
    .expireAfterWrite(1, TimeUnit.MINUTES)
    .buildWithFunction[String, Option[DealerState]]((iname:String) => {
    val mmdbm=ru.sosgps.wayrecall.utils.web.springWebContext.getBean("multiDbManager").asInstanceOf[MultiDbManager]

    mmdbm.dbmanagers("default").getDatabase()("dealers").findOne(MongoDBObject("id" -> iname), readPrefs = ReadPreference.secondaryPreferred())
      .map(dealer => DealerState(
        id = dealer.get("id"),
        blocked = dealer.get("block").asInstanceOf[Any] match {
          case "true" => true
          case "false" => false
          case b: Boolean => b
          case _ => false
        },
        balance = dealer.get("balance").asInstanceOf[Long])
      )
  })

  private def currentDealerState: Option[DealerState] = {
    val iname=ru.sosgps.wayrecall.utils.web.springWebContext.getEnvironment.getProperty("instance.name")
    dealersStatesCache(iname)
  }

  def doFilter(req: ServletRequest, resp: ServletResponse, chain: FilterChain) {

    currentDealerState match {
      case None => chain.doFilter(req, resp)
      case Some(DealerState(id, blocked, balance)) =>
        val contextPath = req.getServletContext.getContextPath
        val service = contextPath match {
          case "/billing" => "billing"
          case _ => "monitoring"
        }
        val path = req.asInstanceOf[HttpServletRequest].getRequestURI.substring(contextPath.length)
        debug("id: " + id + "; blocked: " + blocked + "; contextPath: " + contextPath)
        if (blocked && path != "/EDS/banned")
          req.getServletContext.getRequestDispatcher("/EDS/banned?id=" + id + "&balance=" + balance / 100 + "&service=" + service).forward(req, resp)
        //  resp.asInstanceOf[HttpServletResponse].sendRedirect(req.getServletContext.getContextPath + "/EDS/banned?id="+id+"&balance="+balance/100+"&service="+service)
        else chain.doFilter(req, resp)

    }
  }

  private case class DealerState(id: Object, blocked: Boolean, balance: Long)
}
