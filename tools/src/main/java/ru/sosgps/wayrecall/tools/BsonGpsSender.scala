package ru.sosgps.wayrecall.tools

import java.io.{File, InputStream, ObjectOutputStream}
import java.net.Socket
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

import com.beust.jcommander.Parameter
import org.apache.commons.compress.archivers.zip.ZipFile
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import ru.sosgps.wayrecall.utils.errors.CloseableIterator
import ru.sosgps.wayrecall.utils.io.{DboReader, DboWriter}

import scala.collection.JavaConversions.asScalaBuffer


/**
  * Created by nickl-mac on 25.03.16.
  */
class BsonGpsSender extends CliCommand {


  @Parameter(description = "files", required = true)
  var files: java.util.List[String] = null

  override val commandName: String = "sendemulating"

  @Parameter(names = Array("--addr"))
  var sendAddress: String = null

  @Parameter(names = Array("--speed", "-k"))
  var speed: Double = 1.0


  @Parameter(names = Array("--now"))
  var useNow: Boolean = false

  def expand(file: String): CloseableIterator[InputStream] = {
    if (file.endsWith(".zip")) {

      import scala.collection.JavaConversions.enumerationAsScalaIterator
      val zipFile = new ZipFile(file)
      CloseableIterator.iterateAndClose(zipFile.getEntries.map(e => zipFile.getInputStream(e)), zipFile.close())


    }
    else throw new UnsupportedOperationException(s"not supported file type $file")
  }

  def readOrdered(sources: CloseableIterator[InputStream]): CloseableIterator[GPSData] = {

    val readers = sources.toStream.map(is => new DboReader(is).iterator.map(GPSDataConversions.fromDbo)
      /*.toVector.sortBy(_.time).iterator*/ .buffered)

    def read(nowTime: Long): Stream[GPSData] = {
      val nonEmptyReaders = readers.filter(_.nonEmpty)
      if (nonEmptyReaders.isEmpty)
        return Stream.empty
      // val selectedSource = readers.foldLeft[Option[BufferedIterator[GPSData]]](None)(_.m)
      val selectedSource = nonEmptyReaders.minBy(_.head.time.getTime - nowTime)
      val toReturn = selectedSource.head
      if (toReturn.time.getTime < nowTime)
        throw new IllegalArgumentException(s"received in wrong order, maybe source data was not sorted by time $toReturn")

      selectedSource.next()
      toReturn #:: read(toReturn.time.getTime)
    }

    CloseableIterator.iterateAndClose(read(0), sources.close())
  }

  def getAllInOrder(paths: Seq[String]): CloseableIterator[GPSData] = readOrdered(CloseableIterator.concatSeq(paths.map(expand)))


  override def process(): Unit = {

    val (address, port) = ConnectionUtils.readAddressOrUseLocal(sendAddress, "packreceiver.gpsbson.port")

    val gpses = getAllInOrder(this.files)

    try {

      if (gpses.nonEmpty) {

        val socket = new Socket(address, port)
        socket.setSoTimeout(5000)
        try {

          val out = new DboWriter(socket.getOutputStream)

          def nowTime() = System.currentTimeMillis()

          def send(gps: GPSData): Unit = {
            val gpsToSend = if (useNow) {
              val gpsToSend = gps.clone()
              gpsToSend.time = new Date()
              gpsToSend
            } else gps
            println("sending " + gpsToSend)
            out.write(GPSDataConversions.toMongoDbObject(gpsToSend))
            out.flush()
          }

          val first = gpses.next()
          var prevGpstime = first.time.getTime
          var prevSendTime = nowTime()

          send(first)

          for (gps <- gpses) {

            val timeshift = ((gps.time.getTime - prevGpstime) * (1 / speed)).round

            val sleepTime = prevSendTime + timeshift - nowTime()

            if (sleepTime > 0) {
              Thread.sleep(sleepTime)
            }

            prevGpstime = gps.time.getTime
            send(gps)
            prevSendTime = nowTime()
          }

          out.close()

        }
        finally {
          socket.close()
        }
      }

    }finally {
      gpses.close()
    }


  }
}
