package ru.sosgps.wayrecall.monitoring.web

import com.mongodb.casbah.Imports._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.servlet.{FilterChain, ServletRequest, ServletResponse}
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.web.authentication.www.{BasicAuthenticationEntryPoint, BasicAuthenticationFilter}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody}
import org.springframework.web.servlet.view.RedirectView
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.utils.DBQueryUtils.notRemoved
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.errors.NotPermitted
import ru.sosgps.wayrecall.utils.web.ScalaCollectionJson

import scala.collection.Iterable

class EDSEndpointBase(a: AuthenticationManager) extends grizzled.slf4j.Logging {
  protected  val filter = new BasicAuthenticationFilter(a, new BasicAuthenticationEntryPoint)

  protected def basicAuth(request: HttpServletRequest, response: HttpServletResponse)(handler: => Unit) {
    try {
      filter.doFilter(request, response, new FilterChain {
        override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse): Unit = {
          handler
        }
      })
    }
    catch {
      case e: NotPermitted =>
        response.addHeader("WWW-Authenticate", "Basic realm=\"" + "Realm" + "\"")
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage)
      case e: Exception =>
        error("error", e)
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage)
    }
  }


    protected def json_response(request: HttpServletRequest, response: HttpServletResponse, content: AnyRef): Unit = {
      response.setContentType("application/json;charset=UTF-8")
      val outputStream = response.getOutputStream()
      try {
        ScalaCollectionJson.generate(content, outputStream)
      }
      finally {
        outputStream.close()
      }
    }


//  protected def json_response(str: AnyRef)
}

/**
 * @deprecated use [[ru.sosgps.wayrecall.web.api.ObjectsEndpoint]]
 */
@Controller
@Deprecated
class StateReport(
                   @Qualifier("org.springframework.security.authenticationManager")
                   @Autowired a: AuthenticationManager,
                   @Autowired roleChecker: UserRolesChecker,
                   @Autowired mdbm: MongoDBManager,
                   @Autowired store: PackagesStore,
                   @Autowired or: ObjectsRepositoryReader
                 ) extends EDSEndpointBase(a) {

  //  private val filter = new BasicAuthenticationFilter(a, new BasicAuthenticationEntryPoint)

  def extractSensorData(g: GPSData) = {
    val sensors = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> g.uid), MongoDBObject("sensors" -> 1))
      .flatMap(_.getAs[MongoDBList]("sensors").map((m: MongoDBList) => m.toSeq.map(_.asInstanceOf[BasicDBObject]))).getOrElse(Seq.empty)

    sensors.filter(_.contains("paramName")).map(
      s => {
        val paramName = s("paramName").toString
        val conv = ObjectDataConversions.getConverter(g.uid, paramName, mdbm)
        //        val conv = if (Set("Digital_fuel_sensor_B1", "fuel_lvl", "io_2_67", "Fuel_level_%").contains(s("paramName").toString))
        //          new FuelConversions(g.uid, s("paramName").toString, mdbm) else new ObjectDataConversions(g.uid, s("paramName").toString, mdbm)
        if (s.contains("showInInfo") && s("showInInfo") == false) {
          Map.empty
        } else {
          Map(
            "name" -> (if (s.contains("name")) s("name") else paramName),
            "paramName" -> paramName,
            "value" -> (
              if (ObjectDataConversions.isFuelSensor(paramName))
                conv.getPointValues(g)._1.round
              else
                conv.getPointValues(g)._1),
            "unit" -> (if (s.contains("unit")) s("unit") else "")
          )
        }
      }
    ).filter(_.isEmpty == false).asInstanceOf[Seq[Map[String, Any]]]
  }

  /**
   * @deprecated use [[ru.sosgps.wayrecall.web.api.ObjectsEndpoint.getAllObjectsData()]]
   */
  @RequestMapping(Array("/getobjectsdata"))
  @ResponseBody
  def getAllObjectsData(request: HttpServletRequest, response: HttpServletResponse): Unit = basicAuth(request, response) {

    val name = roleChecker.getUserName()
    debug(s"getting for $name")
    val uids = request.getParameterValues("uid").?.getOrElse(Array.empty[String])

    val sensors = request.getParameter("sensors").?.exists(_.toBoolean)

    val permittedIds = roleChecker.getInferredPermissionRecordIds(name, "object").toSet
    val ids: Iterable[ObjectId] = if (uids.isEmpty) permittedIds
    else uids.toSeq.map(or.objectIdByUid(_)).filter(permittedIds(_))
    debug("ids = " + ids)
    val objects = mdbm.getDatabase().apply("objects").find((notRemoved ++ ("_id" $in ids)))
    response.setContentType("application/json;charset=UTF-8")
    val outputStream = response.getWriter
    try {
      ScalaCollectionJson.generate(objects.map(d => {
        val uid = d.as[String]("uid")
        val latest = store.getLatestFor(Iterable(uid)).headOption
        val sensorsData = if( sensors) {
          Map("sensors" -> latest.map(extractSensorData).getOrElse(Seq.empty))
        } else Map.empty

        Map(
          "uid" -> uid,
          "name" -> d.get("name"),
          "gosnumber" -> d.get("gosnumber"),
          "position" -> latest.map(l => Map(
            "imei" -> l.imei,
            "lon" -> l.lon,
            "lat" -> l.lat,
            "time" -> l.time,
            "speed" -> l.speed
          )).orNull) ++ sensorsData

      }), outputStream)
    } finally outputStream.close()
  }

  /**
   * @deprecated use [[ru.sosgps.wayrecall.web.api.ObjectsEndpoint.getGroups()]]
   */
  @RequestMapping(Array("/getgroups"))
  def getGroups(request: HttpServletRequest, response: HttpServletResponse) = {
    val rv = new RedirectView("/api/getgroups")
    rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY)
    rv
  }

}


