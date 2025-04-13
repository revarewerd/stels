package ru.sosgps.wayrecall.tools

import com.beust.jcommander.{JCommander, ParameterException}
import ru.sosgps.wayrecall.jcommanderutils.{CliCommand, MultiCommandCli}
import com.mongodb.casbah
import casbah.Imports._
import java.util.Properties
import java.io.FileInputStream

import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.tools.sms.SmsLoader
import ru.sosgps.wayrecall.tools.wialon.packaging.{WialonExporter, WlnFromDbResender, WlnResender}
import ru.sosgps.wayrecall.tools.wialon.dataconverters.TableUpserter
import ru.sosgps.wayrecall.tools.wialon.webclient.WialonObjectsSync
import ru.sosgps.wayrecall.watchdog.JMXRemoteWatcher
import ru.sosgps.wayrecall.yandexMock.YandexServerMock


object Main extends MultiCommandCli {

  val commands = Seq[CliCommand](
    WlnResender,
    new WialonExporter,
    //new WialonObjectsSync,
    TableUpserter,
    WlnFromDbResender,
    AggregateCreator,
    YandexServerMock,
    DomainEventsViewCreator,
    KMLImport,
    new TranslatorUtils,
    new OpenCellIdImport,
    JMXRemoteWatcher,
    new BsonGpsSender,
    OfflineRegeocoder,
    DBEntityAnalyzer,
    SmsLoader
  )


  def getMongoDatabase(): casbah.MongoDB = {
    getMdbm().getDatabase()
  }


  def getMdbm(dbname: Option[String] = None) = {
    val properties: Properties = wrcProperties()

    //    global.defaultmongodb.databaseName = Seniel-dev2
    //    global.defaultmongodb.host=127.0.0.1
    //    global.defaultmongodb.port=27017

    val dbName = dbname.getOrElse(properties.getProperty("global.defaultmongodb.databaseName"))
    val servers = properties.getProperty("global.defaultmongodb.servers", properties.getProperty("global.defaultmongodb.host", "localhost"))

    val manager = new MongoDBManager()
    manager.servers = servers.split(",").map(_.trim).toList
    manager.databaseName = dbName
    manager
  }

  def wrcProperties(): Properties = wrcPropertiesFromFile("global.properties")


  def wrcPropertiesFromFile(fileName: String): Properties = {
    val properties = new Properties()
    properties.load(new FileInputStream(System.getenv("WAYRECALL_HOME") + "/conf/" + fileName))
    properties
  }
}
