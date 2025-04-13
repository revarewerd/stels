package ru.sosgps.wayrecall.monitoring.web

import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.junit.runner.RunWith
import org.junit.{Ignore, Assert, Test}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import java.util.Date
import java.util.zip.GZIPInputStream
import java.io.FileInputStream
import ru.sosgps.wayrecall.data.Posgenerator
import ru.sosgps.wayrecall.monitoring.processing.fuelings.{FuelState, FuelingReportService}
import ru.sosgps.wayrecall.utils.io.DboReader
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(Array("/spring-fuelings-test.xml"))
//Кто-то прибил DirectNominatimRegeocoder гвоздями к fuelings, теперь не понятно как запустить этот тест
class FuelingTest extends grizzled.slf4j.Logging{
  
  @Autowired
  var fuelRep: FuelingReportService = null
  
  @Test
  @Ignore
  def test1() {
    testWithData("/fuelingsSHACMAN.bson.gz", 10, 10)
  }

  @Test
  @Ignore
  def test2() {

    val reader = new DboReader(new CompressorStreamFactory().createCompressorInputStream(this.getClass.getResourceAsStream("/o2285456779789705869.bson.xz")))

    val points = reader.iterator.map(GPSDataConversions.fromDbo).toSeq

    val fuelings = fuelRep.getFuelings(points.headOption.getOrElse(new GPSData(null, null, 0, 0, null, 0, 0, 0)).uid, points).states.toSeq

    for (f: FuelState <- fuelings) {
      println(f)
    }

    Assert.assertEquals(3, fuelings.count(_.isFueling))
    Assert.assertEquals(10, fuelings.size)

  }

  private[this] def testWithData(s: String, fuelings: Int, total: Int) {
    val reader = new DboReader(new CompressorStreamFactory().createCompressorInputStream(this.getClass.getResourceAsStream(s)))

    val points = reader.iterator.map(GPSDataConversions.fromDbo).toSeq.sortBy(_.time)
    debug(s"first: ${points.head} last: ${points.last}")
    testWithData(points, fuelings, total)
  }

  private def testWithData(points: Seq[GPSData], fCount: Int, total: Int) {
    val fuelings = fuelRep.getFuelings(points.headOption.getOrElse(new GPSData(null, null, 0, 0, null, 0, 0, 0)).uid, points).states.toSeq

    for (f: FuelState <- fuelings) {
      println(f)
    }

    Assert.assertEquals(fCount, fuelings.count(_.isFueling))
    Assert.assertEquals(total, fuelings.size)
  }
}
