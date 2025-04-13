package ru.sosgps.wayrecall.avlprotocols.ruptela

import com.google.common.util.concurrent.MoreExecutors
import org.junit.{Assert, Test}
import java.nio.file.Files
import ru.sosgps.wayrecall.avlprotocols.ruptela.TestUtils._
import ru.sosgps.wayrecall.packreceiver.DummyPackSaver
import ru.sosgps.wayrecall.utils.io.{Utils, RichDataInput}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 02.11.13
 * Time: 19:23
 * To change this template use File | Settings | File Templates.
 */
class FWUploadTest {

  @Test
  def configureBysample() {

    import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

    val processor = new RuptelaPackProcessor(new DummyPackSaver)

    processor.fwIndexShift = 286

    val tempDirectory = Files.createTempDirectory("tempConfigsPath")
    processor.configsPath = tempDirectory.toString
    val imeiDirectory = tempDirectory.resolve("13226002364046")
    Files.createDirectory(imeiDirectory)
    Files.copy(getResource("FMpro3_000239.efwp"), imeiDirectory.resolve("FMpro3_000239.efwp"))
    testUpload(processor)
    Assert.assertTrue(tempDirectory.toString + " must be empty", imeiDirectory.toFile.listFiles().isEmpty)

    Files.copy(getResource("FMpro3_000239.efwp"), imeiDirectory.resolve("FMpro3_000239.efwp"))
    testUpload(processor)
    Assert.assertTrue(tempDirectory.toString + " must be empty", imeiDirectory.toFile.listFiles().isEmpty)

    tempDirectory.toFile.delete()

  }


  private[this] def testUpload(processor: RuptelaPackProcessor) {
    val dialog = "fwuploadwln.log.gz"
    val responses = blioadReader(false, getTextResource(dialog)) ++
      Iterator.continually(Array.empty[Byte])

    val in = new ArrayIteratorInputStream(blioadReader(true, getTextResource(dialog)))

    val rin = new RichDataInput(in)
    while (in.hasNext) {
      val pack = RuptelaParser.parsePackage(rin)

      val result = Await.result(processor.process(pack), 20.seconds).reverse.flatten.toArray
      val resp = responses.next()
      //      println(" -> " + Utils.toHexString(in.activeLine, " "))
      //      println(" <R " + Utils.toHexString(resp, " "))
      //      println(" <- " + Utils.toHexString(result, " "))
      Assert.assertArrayEquals(resp, result)
    }
  }
}
