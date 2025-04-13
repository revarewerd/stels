package ru.sosgps.wayrecall.tools.wialon.packaging

import java.io._
import scala.io.Source
import java.net.{InetAddress, Socket}
import ru.sosgps.wayrecall.wialonparser.{WialonPackager, WialonPackage, WialonParser}
import collection.mutable
import ru.sosgps.wayrecall.wialonparser.WialonParser._
import scala.collection.JavaConversions.asScalaBuffer
import org.junit.{Assert, Test, Ignore}
import ru.sosgps.wayrecall.utils.io.Utils
import collection.immutable.IndexedSeq


class WialonPackagerTest {


  @Test
  def test1() {
    val src: String = "74000000333533393736303133343435343835004B0BFB70000000030BBB000000270102706F73696E666F00A027AFDF5D9848403AC7253383DD4B400000000000805A40003601460B0BBB0000001200047077725F657874002B8716D9CE973B400BBB00000011010361766C5F696E707574730000000001"


    val bytes: Array[Byte] = Utils.asBytesHex(src)
    Assert.assertTrue(src.equalsIgnoreCase(Utils.toHexString(bytes, "")))
    val is: InputStream = new ByteArrayInputStream(bytes)
    val pack: WialonPackage = parsePackage(is)

    val binary = WialonPackager.wlnPackToBinary(pack)
    println(pack.blocks)
    println(parsePackage(new ByteArrayInputStream(binary)).blocks)

    println("src=")
    println(Utils.toHexString(bytes," "))
    println("fff=")
    println("45 00 00 00 33 35 33 39 37 36 30 31 33 34 34 35 34 38 35 00 4b 0b fb 70 00 00 00 00 0b bb 00 00 00 27 01 02 70 6f 73 69 6e 66 6f 00 a0 27 af df 5d 98 48 40 3a c7 25 33 83 dd 4b 40 00 00 00 00 00 80 5a 40 00 36 01 46 0b")
    println("res=")
    println(Utils.toHexString(binary," "))
    Assert.assertArrayEquals(bytes,binary);

  }


  @Test
  def test2() {

    val arg = """REG;1344841762;37.7113156;55.7080378;0;0;ALT:130.0,pwr_int:9.411,pwr_ext:25.994;in1:0,,SATS:18,param22:2,battery_charge:0;;;;
                |REG;1341604894;37.404224;55.766048;1;205;ALT:193.0,adc1:0.044,adc2:0.066;in1:0,,SATS:11;;;;
                |REG;1341607207;37.4041792;55.7661312;1;235;ALT:239.0,adc1:0.05,adc2:0.062;in1:0,,SATS:10;;;;""".stripMargin('|')

    val uid = "861785003703109";


    val lines = arg.split("\n")

    val parsed: IndexedSeq[WialonPackage] = (for (line <- lines.take(1)) yield {
      val pack: Array[Byte] = WialonPackager.wlnPackToBinary(WialonPackager.parseWLNlineToBinary(line, uid))
      //Utils.printBytes(pack)
      val parsePackage = WialonParser.parsePackage(new ByteArrayInputStream(pack))
      println(parsePackage)
      parsePackage
    }).toIndexedSeq

    parsed


  }


  def main0(args: Array[String]) {
    val is: InputStream = new BufferedInputStream(new FileInputStream("/home/nickl/NetBeansProjects/forStels/data/withoutDoubling/readData1335530876874.wrp"))
    //val writer: PrintWriter = new PrintWriter("log1.txt")
    try {

      val paramss = new mutable.HashMap[String, String]()

      while (is.available > 0) {
        val pack = parsePackage(is)

        pack.blocks.foreach(b => {
          b.name
        })

        // writer.println(parsePackage(is))
      }
    }
    finally {
      is.close
      //writer.close
    }
  }

}