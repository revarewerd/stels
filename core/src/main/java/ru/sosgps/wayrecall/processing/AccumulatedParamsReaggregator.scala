package ru.sosgps.wayrecall.processing

import java.io.BufferedOutputStream
import java.nio.file.{Files, Path}
import java.util.Date
import java.util.zip.GZIPOutputStream

import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.GPSDataConversions
import ru.sosgps.wayrecall.data.{ClusteredPacketsReader, DBPacketsWriter, PackDataConverter, PackagesStore}
import ru.sosgps.wayrecall.utils.io.{DboReader, DboWriter}
import ru.sosgps.wayrecall.utils.stubs.DeviceUnawareInMemoryPackStore

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class AccumulatedParamsReaggregator @Autowired()(reader: PackagesStore, writer: DBPacketsWriter, cnv: PackDataConverter) extends grizzled.slf4j.Logging {

  @tailrec
  final def update(uid: String, from: Date) {

    val path = Files.createTempFile(s"AccumulatedParamsReaggregator-$uid-", ".bson.gz")
    val lastCorrected = try {
      val lastCorrected = storeCorrectedData(uid, from, path).orNull
      debug(s"lastCorrected for $uid = $lastCorrected at ${path.toString}")
      if (lastCorrected == null || lastCorrected == from) return
      debug(s"removing all data for $uid from $from to $lastCorrected")
      reader.removePositionData(uid, from, lastCorrected)
      val dboReader = new DboReader(path.toString)
      try {
        dboReader.iterator.foreach(dbo => writer.addToDb(GPSDataConversions.fromDbo(dbo)))
      } finally dboReader.close()
      debug(s"restored for $uid  from $from to $lastCorrected")
      lastCorrected
    } finally {
      Files.delete(path)
    }

    update(uid, lastCorrected)
  }


  private val MAX_DATE = new Date(Long.MaxValue)

  private def storeCorrectedData(uid: String, from: Date, path: Path) = {
    var lastProcessed: Option[Date] = None
    val dboWriter = new DboWriter(new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(path))))
    try {
      val processor = new LazySeqAccumilatedParamsProcessor(new ClusteredPacketsReader {
        override def getStoreByImei(imei: String): PackagesStore = new DeviceUnawareInMemoryPackStore()

        override def uidByImei(imei: String): Option[String] = Some(uid)
      }, cnv)
      for (gps <- reader.getHistoryFor(uid, from, MAX_DATE)) {
        val newG = Await.result(processor.apply(gps), 10 seconds)
        dboWriter.write(GPSDataConversions.toMongoDbObject(newG))
        lastProcessed = Some(newG.time)
      }
    } finally {
      dboWriter.close()
    }
    lastProcessed
  }
}
