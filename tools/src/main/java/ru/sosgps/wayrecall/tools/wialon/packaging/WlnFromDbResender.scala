package ru.sosgps.wayrecall.tools.wialon.packaging

import com.beust.jcommander.{Parameter, Parameters}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import org.springframework.context.support.ClassPathXmlApplicationContext
import ru.sosgps.wayrecall.data.{MapProtocolPackConverter, MongoPackagesStore, PackagesStore}
import ru.sosgps.wayrecall.utils._
import java.util.{Collections, Date}
import ru.sosgps.wayrecall.tools.Main
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions, ObjectsRepositoryReader}
import java.net.InetAddress
import collection.JavaConversions.asScalaBuffer


@Parameters(commandDescription = "loads from db and resends data to wialon with specified uid and time intrtval")
object WlnFromDbResender extends CliCommand {
  val commandName = "send"

  @Parameter(names = Array("-f", "--from"), description = "from date (format 2013-08-28T00:00)", required = true)
  var from: String = null

  @Parameter(names = Array("-t", "--to"), description = "to date (format 2013-08-28T00:00)", required = true)
  var to: String = null

  @Parameter(names = Array("-u", "--uid"), description = "object uid")
  var uids: java.util.List[String] = Collections.emptyList()

  @Parameter(names = Array("--set-imei"), description = "replace loaded imei with this one")
  var settedImei: String = null

  @Parameter(names = Array("--addr"), required = true)
  var sendAddress: String = null;

  @Parameter(names = Array("--skip"))
  var skip: Int = 0

  @Parameter(names = Array("--limit"))
  var limit: Int = Int.MaxValue

  @Parameter(names = Array("-so", "--sotimeout"))
  var soTimeout = 60000

  @Parameter(names = Array("-h", "--help"), help = true)
  var help = false;

  @Parameter(names = Array("-l", "--slog"))
  var sendinglog = false;

  @Parameter(names = Array("--preloadAll"))
  var preload = false;

  @Parameter(names = Array("--slow"))
  var sleepBetween: Int = 0

  @Parameter(names = Array("-db"))
  var dbname: String = null


  def process() = {

    val packagesStore = new MongoPackagesStore()
    packagesStore.pconv = new MapProtocolPackConverter

    packagesStore.mongoDbManager = Main.getMdbm(Option(dbname))
    val or = new ObjectsRepositoryReader
    or.mdbm = Main.getMdbm(Option(dbname))
    packagesStore.or = or
    packagesStore.batchSize = 30

    val transform: (GPSData) => GPSData = if (settedImei != null) (a) => {a.imei = settedImei; a} else (a) => a

    val uidall = uids.toSeq

    require(uidall.nonEmpty, "uids or imeis must be set")

    def histories = {
      uidall.toStream.map(uid => packagesStore.getHistoryFor(uid, parseDate(from), parseDate(to)))
    }

    val total = histories.map(_.total).sum

    val packs = for ((x, i) <- histories.iterator.map(_.iterator).flatten.map(transform).zipWithIndex) yield {
      log("sending " + (i + 1) + "/" + total +" : " + x)
      GPSDataConversions.toWialonPackage(x)
    }

    val addresport = sendAddress.split(":")

    //check existence
    addresport(0)
    addresport(1)
    val address: InetAddress = InetAddress.getByName(addresport(0))
    val port: Int = addresport(1).toInt
    //new WlnResender(address, port, packs, soTimeout,
    new WlnNettyResender(address, port, packs, soTimeout,
      sendingLogEnabled = sendinglog,
      sleepBeetween = sleepBetween)
      .doSending

    System.exit(0)
  }

  def log(x: String) = {
    println(new Date() + " " + x);
  }

}
