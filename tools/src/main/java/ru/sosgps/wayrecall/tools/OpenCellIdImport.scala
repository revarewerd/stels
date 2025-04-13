package ru.sosgps.wayrecall.tools

import java.io.{FileInputStream, File}
import java.util.Date
import java.util.zip.GZIPInputStream

import com.beust.jcommander.{Parameter, Parameters}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand

import scala.io.Source

@Parameters(commandDescription = "Updates objects data from files")
class OpenCellIdImport extends CliCommand {
  val commandName = "opencellidimport"

  @Parameter(names = Array("-i", "-f"), required = true)
  var fileName: String = null

  @Parameter(names = Array("-c", "-collection"), required = false)
  var collection: String = "lbses"

  @Parameter(names = Array("-d", "-db"), required = false)
  var dbname: String = "Seniel-dev2"

  def process() {
    import com.mongodb.casbah.Imports._
    import ru.sosgps.wayrecall.utils._

    val db = MongoConnection().getDB(dbname)

    val csv = Source.fromInputStream(new GZIPInputStream(new FileInputStream(fileName))).getLines().map(_.split(","))

    val keys = csv.next()

    val data = csv.map(arr => keys.zip(arr).toMap)

    //data.take(10).foreach(println)

    for (e <- data) try {
      db(collection).insert(MongoDBObject(
        "lbs" -> MongoDBObject(
          "CID" -> e("cell").toInt,
          "LAC" -> e("area").toInt,
          "MCC" -> e("mcc").toInt,
          "MNC" -> e("net").toInt
        ),
        "lonlat" -> MongoDBObject(
          "lon" -> e("lon").toDouble,
          "lat" -> e("lat").toDouble,
          "range" -> e("range").toDouble
        ),
        "date" -> new Date(e("updated").toLong * 1000),
        "source" -> "opencellid"
      ))
    } catch {
      case e: Exception => e.printStackTrace()
    }


  }
}
