package ru.sosgps.wayrecall.billing.security

import java.nio.file.{Paths, Path}
import javax.annotation.Resource
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import ru.sosgps.wayrecall.billing.dealer.DealersManagementMixin
import ru.sosgps.wayrecall.initialization.{MultiserverConfig, MultiDbManager}
import ru.sosgps.wayrecall.utils.ExtDirectService
import org.springframework.beans.factory.annotation.{Value, Autowired}
import ru.sosgps.wayrecall.core.{UserRolesChecker, MongoDBManager}
import java.util.{Date, UUID}
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.WriteConcern
import ch.ralscha.extdirectspring.annotation.ExtDirectMethod

import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import org.springframework.stereotype.Controller


@ExtDirectService
@Controller
class BackdoorEnterProvider extends grizzled.slf4j.Logging with DealersManagementMixin{

  @Autowired
  var mdbm: MongoDBManager = null

//  @BeanProperty
//  @Value("${global.monitoringUrl}")
//  var monitoringUrl: String = null
  val confPath: Path = Paths.get(System.getenv("WAYRECALL_HOME"), "conf")
  val mconfig: MultiserverConfig = new MultiserverConfig(confPath)
  val port: Int = mconfig.gloabalProps.getProperty("global.port").toInt
  
  @Autowired
  var roleChecker: UserRolesChecker = null

  @RequestMapping(value = Array("/monitoringbackdoor"))
  def monitoringBackdoor(@RequestParam("login") login: String, request: HttpServletRequest): String = {
    val key = reserveBackdoorKey(mdbm, login)
    debug("backdoor authentication key for user " + login + " is " + key)
    val redirectUrl = request.getRequestURL.toString.stripSuffix(
      Option(request.getContextPath).getOrElse("")  +
      Option(request.getServletPath).getOrElse("")  +
      Option(request.getPathInfo).getOrElse("")
    )+ "/backdoorAuth" + "?key=" + key
    debug("redirecting to " + redirectUrl)
    "redirect:" + redirectUrl
  }

  @RequestMapping(value = Array("/dealerbackdoor"))
  def dealerBackdoor(@RequestParam("dealer") dealer: String,
                     request: HttpServletRequest, response: HttpServletResponse): String = {
    if(!roleChecker.hasRequiredStringAuthorities("DealerBackdoor")){
      response.sendError(HttpServletResponse.SC_FORBIDDEN," cant backdoor to dealer")
      return null
    }

    val key = reserveBackdoorKey(mmdbm.dbmanagers(dealer), dealer)

    debug("backdoor authentication key for dealer " + dealer + " is " + key)

    val host = this.subDealers().find(_.name == dealer).get.vhosts.head

    val redirectUrl = "http://"+host+":"+port +"/billing/backdoorAuth" + "?key=" + key
    //val redirectUrl = request.getScheme + "://"+host+":"+request.getLocalPort+"/billing/backdoorAuth" + "?key=" + key
    debug("redirecting to " + redirectUrl)
    "redirect:" + redirectUrl
  }


  def reserveBackdoorKey(mdbm: MongoDBManager, userLogin: String): String = {

    val coll = mdbm.getDatabase()("authBackdors")
    val backdoorKey = UUID.randomUUID().toString

    debug("backdoor " + backdoorKey + " was created")

    coll.insert(MongoDBObject("backdoorKey" -> backdoorKey, "login" -> userLogin, "date" -> new Date), WriteConcern.SAFE)

    backdoorKey
  }

}
