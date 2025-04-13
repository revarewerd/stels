package ru.sosgps.wayrecall.packreceiver

import java.io.{BufferedInputStream, File, FileInputStream}
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.Executors

import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.core.GPSDataConversions
import ru.sosgps.wayrecall.data.Posgenerator
import ru.sosgps.wayrecall.utils.concurrent.Semaphore
import ru.sosgps.wayrecall.utils.io.DboReader
import ru.sosgps.wayrecall.utils.durationAsJavaDuration

import scala.concurrent.duration.DurationInt

/**
 * Created by nickl on 27.12.14.
 */
class IncomingGPSLoggerTest {


  val totalCount: Int = 5200
  val loggerlimit: Int = 1000


  @Test
  def test(): Unit ={

    val pg = new Posgenerator("1234", 1419673753000L)

    val sourceData = pg.genMoving(1000 seconds,totalCount).toSet

    val dir = Files.createTempDirectory("IncomingGPSLoggertmm")
    //val dir: Path = Paths.get("ttm")

    //dir.mkdirs()
    val executors = Executors.newFixedThreadPool(10)

    val logger = new IncomingGPSLogger(dir.toFile)
    logger.limit = loggerlimit
    logger.start()
    val sem = new Semaphore(10000)
    for (gpss <- sourceData.grouped(10000)) {
      for (gps <- gpss) {
        sem.acquired(logger.apply(gps))
      }

      while (logger.qsize > 0)
        Thread.sleep(50)

    }

    logger.stop()

    val factory = new CompressorStreamFactory()

    val writtenData =
      dir.toFile.listFiles().iterator.flatMap(
        file => new DboReader(factory.createCompressorInputStream(
          new BufferedInputStream(new FileInputStream(file)))
        ).iterator.map(GPSDataConversions.fromDbo)
      )

    Assert.assertEquals(sourceData, writtenData.toSet)

  }

}
