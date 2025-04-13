package ru.sosgps.wayrecall.data.sleepers

import com.mongodb.casbah.Imports._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.utils.ExtDirectService

import scala.util.Try

@ExtDirectService
@Controller
class ShowLBSService extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var lbsConverter: LBSConverter = null

  def parseReportBySmsId(smsId: Long): Try[MatchResult] = {
    val resp = mdbm.getDatabase()("smses").findOne(MongoDBObject("smsId" -> smsId), MongoDBObject("text" -> 1)).getOrElse(DBObject())
    SleeperParser.parseReport(resp.getAsOrElse[String]("text", ""))
  }

  @RequestMapping(value = Array("/showbylbs"))
  def showLBS(@RequestParam("id") id: Long): String = {

    val matchResult = parseReportBySmsId(id).get

    val redirectUrl =
      matchResult.lonlat.map(urlFromLonLat _ tupled)
        .orElse(
          matchResult.positionURL.map(_.toString))
        .getOrElse {
        val lbs = matchResult.lbs.get
        val ll = lbsConverter.convertLBS(lbs)
        val lon = ll.lon
        val lat = ll.lat
        urlFromLonLat(lon, lat)
      }

    debug("redirecting to " + redirectUrl)

    "redirect:" + redirectUrl
  }


  private[this] def urlFromLonLat(lon: Double, lat: Double): String = {
    s"http://maps.yandex.ru/?ll=$lon,$lat&pt=$lon,$lat&z=13"
  }
}
