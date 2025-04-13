package ru.sosgps.wayrecall.avlprotocols.ruptela

import scala.io.{BufferedSource, Source}
import ru.sosgps.wayrecall.utils.io.{CRC16CCITT, RichDataInput, Utils}
import java.io.{FileOutputStream, BufferedInputStream, FileInputStream}
import scala.util.control.Exception._
import com.google.common.base.Charsets
import TestUtils._

object FirmWareUploadExperiment {


  def biloadreader(file: String, in: Boolean) = {

    val source = Source.fromFile(file)
    blioadReader(in, source)

  }


  def packFirmwr(msg: String, additionalBytes: Byte*): Array[Byte] = {
    val packdata = 0x68.toByte +: (msg.getBytes(Charsets.US_ASCII) ++ additionalBytes)
    RuptelaParser.packBody(packdata)
  }

  def packFirmwrData(data: Iterator[Array[Byte]]) = {
    for ((a, i) <- data zipWithIndex) yield {

      val crc: Int = CRC16CCITT.invertedKermit(a).asInstanceOf[Int]
      val xs = 0x1f + i
      val size = a.length + 6
      packFirmwr("|FU_PCK*",
        Array(size & 0xff, size >> 8, xs & 0xff, 0x01 + (xs >> 8)).map(_.toByte) ++
          a ++
          Array(crc & 0x00FF, (crc & 0x0000FF00) >> 8, 0x0d, 0x0a).map(_.toByte):_*)
    }
  }

  def main0(args: Array[String]) {
    // f4 c2 b1 51 96
    val bis = new BufferedInputStream(getResource("FMpro3_000239.efwp"))
    val firmvareData = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
    val frombiload = blioadReader(false,getTextResource("fwuploadwln.log.gz")).toList


    val myPacks =Iterator(RuptelaParser.recordACKcommand(true)++packFirmwr("|FU_STRT*",0x0d,0x0a)) ++
      packFirmwrData(firmvareData.grouped(512)) ++
      Iterator(packFirmwr("|FU_WRITE*",0x0d,0x0a))
    for ((a, bi) <- (myPacks zip frombiload.iterator).take(20)) {

      println(Utils.toHexString(bi, " "))
      println(Utils.toHexString(a, " "))
      println()

    }
  }

//    def main(args: Array[String]) {
//      val iterator = biloadreader("/home/nickl/Загрузки/bilod2.log", true)
//
//      for ((bytes,i) <- iterator.zipWithIndex) {
//         val o = new FileOutputStream(i+".bin")
//        o.write(bytes)
//        o.close();
//      }
//
//    }

  def main(args: Array[String]) {
    val iterator = blioadReader(true,getTextResource("fwuploadwln.log.gz"))

    for (bytes <- iterator.take(5)) {
      val input = new RichDataInput(bytes)
      ignoring(classOf[java.io.EOFException]) {
        while (true) {
          val pack = RuptelaParser.parsePackage(input)
          println(pack)
          if(pack.commandId == 1)
            {
              val gpsDatas = RuptelaParser.getGpsDatas(pack)
              println(gpsDatas.size()+": "+gpsDatas)
            }

          println(new String(pack.dataBytes.take(7),Charsets.US_ASCII))
          if(pack.dataBytes.length == 11)
          {
            val n = pack.dataBytes(8) & 0xff << 8 | pack.dataBytes(7) & 0xff
            println(n)
          }
        }
      }
      println()
    }

  }

}
