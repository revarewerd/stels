package ru.sosgps.wayrecall.tools.wialon.packaging

import collection.mutable
import java.io._

import scala.io.Source
import java.net.{InetAddress, Socket}

import ru.sosgps.wayrecall.wialonparser.{WialonPackage, WialonPackager, WialonParser}
import ru.sosgps.wayrecall.utils.ResourceScope
import java.util.{Date, Properties}
import java.util.zip.ZipInputStream

import com.beust.jcommander.{JCommander, Parameter, Parameters}
import ru.sosgps.wayrecall.jcommanderutils.{CliCommand, JCommanderCreator}
import java.util

import ru.sosgps.wayrecall.tools.ConnectionUtils
import ru.sosgps.wayrecall.utils.web.ScalaJson
import ru.sosgps.wayrecall.tools.wialon.packaging.WialonItem
import ru.sosgps.wayrecall.tools.wialon.dataconverters.ObjectsTableParser
import ru.sosgps.wayrecall.utils.io.NullWriter

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 20.08.12
 * Time: 18:08
 * To change this template use File | Settings | File Templates.
 */
class WlnResender(
                   val address: InetAddress,
                   val port: Int,
                   packetsStream: Iterator[WialonPackage],
                   soTimeout: Int = 10000,
                   maxReconnectionsCount: Int = 50000,
                   reconnectionInterval: Int = 10000,
                   sendingLogEnabled: Boolean = false,
                   sleepBeetween: Int = 0,
                   useNowTime: Boolean = false
                   ) {

  def connect = {
    val socket = new Socket(address, port)
    socket.setSoTimeout(soTimeout)
    socket
  }


  def doSending {

    import ResourceScope._

    val sendinglog = if (sendingLogEnabled) new PrintWriter("sendinglog.log") else null

    var binary: WialonPackage = null

    var reconnectionCount = 0

    while (packetsStream.hasNext && reconnectionCount < maxReconnectionsCount) {
      try {
        reconnectionCount = reconnectionCount + 1;
        scope(m => {

          println("connecting " + reconnectionCount)
          val clientSocket = m.manage(connect)

          val outputStream = m.manage(new BufferedOutputStream(clientSocket.getOutputStream))
          val inputStream = m.manage(clientSocket.getInputStream)
          val dis = m.manage(new DataInputStream(inputStream))
          while (packetsStream.hasNext || binary != null) {

            if (binary == null)
              binary = packetsStream.next()

            if (useNowTime)
              binary.time = new Date()

            if (sendingLogEnabled) {
              val parsedPackage = binary
              sendinglog.println(parsedPackage)
              sendinglog.flush()
            }
            outputStream.write(WialonPackager.wlnPackToBinary(binary))
            outputStream.flush();
            val readByte = dis.readByte()

            if (sendingLogEnabled) {
              sendinglog.println("confirmationByte=")
              sendinglog.println(readByte)
            }
            binary = null;
            Thread.sleep(sleepBeetween)
          }
        })
      }
      catch {
        case e: java.net.ConnectException => {
          println(new Date() + " catched " + e)
          System.err.println(new Date() + " catched " + e)
          e.printStackTrace(System.err)
          println("waiting for " + reconnectionInterval + " mils")
          Thread.sleep(reconnectionInterval)
        }
        case e: Exception => {
          println(new Date() + " catched " + e)
          System.err.println(new Date() + " catched " + e)
          e.printStackTrace(System.err)
        }
      }
    }
    if (sendingLogEnabled)
      sendinglog.close()
  }

}


class PacketsStreamProvider(val wlnsFolder: File, val uidFromFileName: (String) => Option[String], val skip: Int = 0, val limit: Int = Int.MaxValue) {
  require(wlnsFolder.exists() && wlnsFolder.isDirectory, wlnsFolder.getPath + " is not directory or doesn't exists")

  def genPacketsStream(): Iterator[WialonPackage] = {
    (for ((file, uid) <- getWlnsWithUids()) yield {

      var is: InputStream = new FileInputStream(file);

      if (file.getName.endsWith(".zip")) {
        val zin = new ZipInputStream(is)
        require(zin.getNextEntry != null, "cant read zip file " + file.getName + " maybe it is empty")
        is = zin
      }

      val lines = Source.fromInputStream(is, "UTF8").getLines()
      for (line <- lines) yield {
        WialonPackager.parseWLNlineToBinary(line, uid)
      }

    }).flatten
  }

  private def getWlnsWithUids(): Iterator[(File, String)] = {

    val wlnsFiles: mutable.IndexedSeq[File] =
      (wlnsFolder.listFiles(new FilenameFilter {def accept(dir: File, name: String) = name.endsWith(".wln") || name.endsWith(".zip")}): mutable.IndexedSeq[File]).sortBy(_.getName)

    val totallength = wlnsFiles.size



    val mappedOptions: mutable.IndexedSeq[(File, Option[String])] = wlnsFiles.drop(skip).take(limit).map(f => {
      val correctedName = f.getName.replace("\t", " ").replaceAll(" +", " ").trim
      (f, uidFromFileName(f.getName))
    })

    val notFound = mappedOptions.filter(_._2.isEmpty)

    if (!notFound.isEmpty) {
      println("cannot find uids for:")
      notFound.map(_._1.getName).foreach(println)
      throw new IllegalArgumentException("cannot find uids")
    }

    val mapped = mappedOptions.map(pair => (pair._1, pair._2.get))
    println("mapped:")
    mapped.foreach(println)
    println()
    mapped.iterator.zipWithIndex.map({
      case (e, i) =>
        println(new Date() + "(" + (i + skip + 1) + "/" + totallength + ") processing " + e._1.getName + " " + e._2); e
    })
  }

}

@Parameters(commandDescription = "Resends wialon data to specified address")
object WlnResender extends CliCommand {


  @Parameter(description = "wlns_folder", required = true)
  var wlnsFolderName: java.util.List[String] = null;

  @Parameter(names = Array("--objects"))
  var objFiles: String = null;

  @Parameter(names = Array("--addr"))
  var sendAddress: String = null;

  @Parameter(names = Array("--skip"))
  var skip: Int = 0

  @Parameter(names = Array("--limit"))
  var limit: Int = Int.MaxValue

  @Parameter(names = Array("-so", "--sotimeout"))
  var soTimeout = 60000

  @Parameter(names = Array("-h", "--help"), help = true)
  var help = false;

  @Parameter(names = Array("--slow"))
  var sleepBetween: Int = 0

  @Parameter(names = Array("--now"))
  var useNowTime: Boolean = false

  def main(args: Array[String]) {

    val commander = JCommanderCreator.create(this)
    commander.parse(args: _*);
    if (this.help)
      commander.usage()
    else {
      process()
    }
  }


  def process() {


    val (address, port) = ConnectionUtils.readAddressOrUseLocal(sendAddress, "packreceiver.wialon.port")

    val wlnsFolder: File = new File(wlnsFolderName.iterator().next())
    val objectsDataFile: File = new File(Option(objFiles).getOrElse(System.getenv("WAYRECALL_HOME") + "/data/objects.data"))

    val jsonItemsFile = new File(wlnsFolder, "wialonItems.json")

    val filenametouiid: Map[String, String] = (if (jsonItemsFile.exists()) {
      println("reading uid from " + jsonItemsFile)
      ScalaJson.parse[Seq[java.util.Map[String,String]]](jsonItemsFile).map(wi => (wi.get("name"), wi.get("uid")))

    } else if(objectsDataFile.exists()) {
      println("reading uid from " + objectsDataFile)
      ObjectsTableParser.parseLinesToMap(objectsDataFile).map(
        m => (m("name").toString, m("uid").toString)
      )
    } else Seq.empty).map(p => (convertObjectNameToFileName(p._1), p._2)).toMap ++
      wlnsFolder.listFiles.filter(_.getName.endsWith(".uid")).map(f => {
        (f.getName.stripSuffix(".uid").stripSuffix(".zip").stripSuffix(".wln"), Source.fromFile(f, "utf8").getLines().next())
      })

    filenametouiid.toSeq.sortBy(_._1).foreach(println)


    def getUidFromFileName(name: String): Option[String] = {
      val correctedNames = Stream(
        name.replace("\t", "").replaceAll(" +", " ").trim,
        name.replace("\t", " ").replaceAll(" +", " ").trim
      )

      val ress: Option[String] = correctedNames.flatMap(correctedName =>
        filenametouiid.keysIterator.find(key => correctedName.startsWith(key.replaceAll(" +", " ").trim))
      ).headOption

      ress.map(filenametouiid)
    }



    val provider = new PacketsStreamProvider(wlnsFolder, getUidFromFileName, skip, limit)
    new WlnResender(address, port, provider.genPacketsStream(), soTimeout,
      sleepBeetween = sleepBetween, useNowTime = useNowTime)
      .doSending
  }


  def convertObjectNameToFileName(objectName: String): String = {
    Translitter.translit(objectName.replace( """\""", "_").trim)
  }

  val commandName = "wlnresend"
}
