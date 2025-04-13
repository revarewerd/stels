package ru.sosgps.wayrecall.avlprotocols.ruptela

import com.google.common.util.concurrent.MoreExecutors
import ru.sosgps.wayrecall.packreceiver.DummyPackSaver
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext}
import scala.io.{BufferedSource, Source}
import ru.sosgps.wayrecall.utils.io.{CRC16CCITT, CRC16, RichDataInput, Utils}
import java.io.{FileInputStream, File, InputStream}
import com.google.common.base.Charsets
import java.net.Socket
import TestUtils._
import java.nio.file.{StandardCopyOption, Path, Files}
import org.junit.{Test, Assert}
import java.util.Properties
import scala.util.control.Exception

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 07.06.13
 * Time: 20:52
 * To change this template use File | Settings | File Templates.
 */
object ConfiguratorTest {

  def main(args: Array[String]) {

    val properties = new Properties()
    properties.load(new FileInputStream(System.getenv("WAYRECALL_HOME") + "/conf/packreceiver.properties"))

    val confPath = properties.getProperty("packreceiver.ruptela.configpath").replaceAll( """\$\{WAYRECALL_HOME\}""", System.getenv("WAYRECALL_HOME"))

    val imeiPath = new File(confPath).toPath.resolve("13226008589182")

    if (!Files.exists(imeiPath))
      Files.createDirectories(imeiPath)

    Files.copy(getResource("RupProGlo_WRC_30sec.fp3c"), imeiPath.resolve("RupProGlo_WRC_30sec.fp3c"), StandardCopyOption.REPLACE_EXISTING)
    println(Utils.toHexString("#cfg_send@".getBytes(Charsets.US_ASCII), " "))

    val socket = new Socket("localhost", 9089)
    socket.setSoTimeout(2000)
    val in = socket.getInputStream
    val out = socket.getOutputStream

    Exception.catching(classOf[java.net.SocketTimeoutException]).toTry {
      for (d <- blioadReader(true, getTextResource("reconfigdialog.log.gz"))) {
        out.write(d)
        Thread.sleep(200)
        println("->" + Utils.toHexString(d, " "))
        val bytes = Array.ofDim[Byte](1000)
        val n = in.read(bytes)
        println("<-" + Utils.toHexString(bytes.take(n), " "))
      }
    }

    println("the end");

  }
}

class ConfiguratorTest {


  @Test
  def configureBysample() {
    import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

    val tempDirectory = Files.createTempDirectory("tempConfigsPath")

    //   Files.copy(getResource("RupProGlo_WRC_30sec.fp3c"), tempDirectory.resolve("13226008589182.fp3c"))

    val path = tempDirectory.resolve("13226008589182")
    Files.createDirectory(path)
    Files.copy(getResource("RupProGlo_WRC_30sec.fp3c"), path.resolve("RupProGlo_WRC_30sec.fp3c"))

    val processor = new RuptelaPackProcessor(new DummyPackSaver)

    processor.configsPath = tempDirectory.toString

    val responses = blioadReader(false, getTextResource("reconfigdialog.log.gz")) ++
      Iterator.continually(Array.empty[Byte])

    val in = new ArrayIteratorInputStream(blioadReader(true, getTextResource("reconfigdialog.log.gz")))

    val rin = new RichDataInput(in)
    while (in.hasNext) {
      val pack = RuptelaParser.parsePackage(rin)

      val result = Await.result(processor.process(pack), 20.seconds).reverse.flatten.toArray
      val resp = responses.next()
      println(" -> " + Utils.toHexString(in.activeLine, " "))
      println(" <R " + Utils.toHexString(resp, " "))
      println(" <- " + Utils.toHexString(result, " "))
      Assert.assertArrayEquals(resp, result)
    }

    Assert.assertTrue(path.toString + " must be empty", path.toFile.listFiles().isEmpty)

    tempDirectory.toFile.delete()

  }

}


