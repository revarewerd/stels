package ru.sosgps.wayrecall.tools.sms

import java.util.Collections

import com.beust.jcommander.{Parameter, Parameters}
import com.mongodb.casbah.WriteConcern
import grizzled.slf4j.{Logger, Logging}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import ru.sosgps.wayrecall.m2msms.SendSms
import ru.sosgps.wayrecall.m2msms.SendSms.richMessage
import ru.sosgps.wayrecall.tools.Main
import ru.sosgps.wayrecall.m2msms.M2MClientFactory
import ru.sosgps.wayrecall.sms.{SMS, SMSEvent}

import scala.collection.JavaConversions.asScalaBuffer

@Parameters(commandDescription = "loads sms from m2m and saves to db")
object SmsLoader extends CliCommand with Logging {
  val commandName = "loadsms"

  @Parameter(names = Array("-t", "--msid"), description = "sms msid", required = true)
  var msids: java.util.List[String] = Collections.emptyList()

  @Parameter(names = Array("--hours"))
  var hours: Int = 5

  @Parameter(names = Array("-db"))
  var dbname: String = null

  def process() = {
    val mdbm = Main.getMdbm(Option(dbname))

    val sendSms = new SendSms
    sendSms.port = new M2MClientFactory().createPort()

    println("msids = " + msids.mkString)

    val received = sendSms.readLastHourSms(msids, hours).getMessageInfo.map(_.toSms)

    for (sms <- received) {
      try {
        mdbm.getDatabase()("smses").insert(SMS.toMongoDbObject(sms), WriteConcern.Safe)
      }
      catch {
        case e: com.mongodb.MongoException.DuplicateKey => {}
        case e: Exception => warn(e.getMessage, e)
      }
    }

    System.exit(0)
  }

}
