package ru.sosgps.wayrecall.monitoring.web

import java.util.Date

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager
import com.novus.salat._
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker
import ru.sosgps.wayrecall.utils.salat_context._
import com.mongodb.casbah.Imports._
import grizzled.slf4j.Logging
import org.springframework.http.{HttpStatus, MediaType}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, ResponseBody, ResponseStatus}

/**
  * Created by ivan on 20.02.17.
  */

case class ReportStatsEntry(user: ObjectId, time: Date, reportType: String, targetIdentifier: String, from: Date, to: Date)

@Controller
class ReportStatsCollector @Autowired() (mongo: MongoDBManager, permissions: ObjectsPermissionsChecker) extends Logging {
  val reportStatsCollection = mongo.getDatabase().apply("reportStats")
  val usersCollection = mongo.getDatabase().apply("users")

// TODO additional parameters?
  @RequestMapping(path = Array("/ReportStats"))
  @ResponseStatus(HttpStatus.OK)
  def save(@RequestParam("reportType")reportType: String,
           @RequestParam("target") target: String,
           @RequestParam("from") from: Long,
           @RequestParam("to")to: Long) = {
    doSave(reportType, target, new Date(from), new Date(to))
  }

  def doSave(reportType: String, target: String, from: Date, to: Date): Unit = {
    val user = usersCollection.findOne(MongoDBObject("name" -> permissions.username)).get.as[ObjectId]("_id")
    val stats = ReportStatsEntry(user, new Date, reportType, target, from, to)
    reportStatsCollection.insert(grater[ReportStatsEntry].asDBObject(stats))
  }
}