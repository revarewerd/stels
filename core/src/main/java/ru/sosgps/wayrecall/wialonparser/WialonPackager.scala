package ru.sosgps.wayrecall.wialonparser

import java.io._
import ru.sosgps.wayrecall.wialonparser.WialonParser._
import collection.mutable
import java.nio.ByteBuffer

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import java.net.{InetAddress, Socket}
import ru.sosgps.wayrecall.wialonparser.{WialonPackageBlock, WialonCoordinatesBlock, WialonPackage, WialonParser}
import java.util.Date
import java.util
import scala.collection.JavaConversions.iterableAsScalaIterable
import com.google.common.base.Charsets

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 17.08.12
 * Time: 14:15
 * To change this template use File | Settings | File Templates.
 */


object WialonPackager {

  def parseSemicolonPairs(paramString: String): mutable.IndexedSeq[(String, String)] = {
    wrapRefArray(paramString.split(",")).filterNot(_.isEmpty).map(e => {
      val i = e.split(":");
      i(0) -> i(1)
    })
  }

  def longToDouble(readLong: Long): Double = {
    return java.lang.Double.longBitsToDouble(java.lang.Long.reverseBytes(readLong))
  }

  def doubleToLong(d: Double): Long = {
    return java.lang.Long.reverseBytes(java.lang.Double.doubleToLongBits(d))
  }

  def createCoordinatesBlock(coord: WialonCoordinatesBlock): Array[Byte] = {
    val packbody = new ByteArrayOutputStream()
    val packbodyDataStream = new DataOutputStream(packbody)
    packbodyDataStream.writeByte(if (coord.hidden) 1 else 0) //hidden
    packbodyDataStream.writeByte(2) //datatype
    packbodyDataStream.writeBytes("posinfo") //blockName
    packbodyDataStream.write(0)
    packbodyDataStream.writeLong(doubleToLong(coord.lon)) //long-lat
    packbodyDataStream.writeLong(doubleToLong(coord.lat))
    packbodyDataStream.writeLong(doubleToLong(coord.height)) //height
    packbodyDataStream.writeShort(coord.speed) //speed
    packbodyDataStream.writeShort(coord.course) //course
    packbodyDataStream.writeByte(coord.satelliteNum) //satelliteNum number
    packbodyDataStream.flush()
    packbody.toByteArray
  }


  def createBlock(b: WialonPackageBlock): Array[Byte] = {
    val packbody = new ByteArrayOutputStream()
    val packbodyDataStream = new DataOutputStream(packbody)
    packbodyDataStream.writeByte(if (b.hidden) 1 else 0) //hidden

    b.value match {
      case d: String =>
        require(0x1 == b.dataType)
        packbodyDataStream.writeByte(0x1)
        packbodyDataStream.writeBytes(b.name)
        packbodyDataStream.write(0)
        packbodyDataStream.write(d.getBytes(Charsets.US_ASCII))
        packbodyDataStream.write(0)
      case d: Array[Byte] =>
        require(0x2 == b.dataType)
        packbodyDataStream.writeByte(0x2)
        packbodyDataStream.writeBytes(b.name)
        packbodyDataStream.write(0)
        packbodyDataStream.write(d)
      case d: java.lang.Integer =>
        require(0x3 == b.dataType)
        packbodyDataStream.writeByte(0x3)
        packbodyDataStream.writeBytes(b.name)
        packbodyDataStream.write(0)
        packbodyDataStream.writeInt(d)
      case d: java.lang.Double =>
        require(0x4 == b.dataType)
        packbodyDataStream.writeByte(0x4)
        packbodyDataStream.writeBytes(b.name)
        packbodyDataStream.write(0)
        packbodyDataStream.writeLong(doubleToLong(d))
      case d: java.lang.Long =>
        require(0x5 == b.dataType)
        packbodyDataStream.writeByte(0x5)
        packbodyDataStream.writeBytes(b.name)
        packbodyDataStream.write(0)
        packbodyDataStream.writeLong(d)
    }

    packbodyDataStream.flush()
    packbody.toByteArray
  }


  // REG;1359662453;37.5562656;55.7350592;0;0;ALT:119.0,adc2:0.0,pwr_int:10.546,pwr_ext:12.377;in1:0,,SATS:11,gsm:4,counter1:0,battery_charge:0;;;;

  def parseWLNlineToBinary(line: String, uid: String): WialonPackage = try {
    val items: IndexedSeq[String] = line.split(";"): IndexedSeq[String]

    require(items.size >= 8, "line '" + line + "' is now well-formed")

    //items.zipWithIndex.map(z => z._2 + " " + z._1).foreach(println)

    val time = items(1)
    val lon = items(2)
    val lat = items(3)
    val speed = items(4)
    val course = items(5)

    val params1 = parseSemicolonPairs(items(6))
    val params2 = parseSemicolonPairs(items(7))

    val additionalParams = mutable.HashMap() ++ (params1 ++ params2)

    val r: WialonPackage = new WialonPackage()
    r.imei = uid
    r.time = new Date(time.toLong * 1000)
    r.size = -1
    r.blocks = new util.ArrayList[WialonPackageBlock]()

    val block: WialonCoordinatesBlock = new WialonCoordinatesBlock
    block.course = course.toShort
    block.lon = lon.toDouble
    block.lat = lat.toDouble
    block.speed = speed.toShort
    block.satelliteNum = additionalParams.remove("SATS").map(v => (v.toInt & 0xFF).toByte).getOrElse(0.toByte)
    block.height = additionalParams.remove("ALT").map(_.toDouble).getOrElse(0.0)

    r.blocks.add(block)

    for ((k, v) <- additionalParams) {
      val block1: WialonPackageBlock = new WialonPackageBlock

      block1.`type` = 3003
      block1.name = k
      block1.hidden = false
      block1.value = v;

      if (v.matches( """[+-]*\d+""")) {
        if (v.length > 5) {
          block1.dataType = 0x5
          block1.value = v.toLong.asInstanceOf[AnyRef]
        }
        else {
          block1.dataType = 0x3
          block1.value = v.toInt.asInstanceOf[AnyRef]
        }
      }
      else if (v.matches( """[+-]*\d+\.\d+""")) {
        block1.dataType = 0x4
        block1.value = v.toDouble.asInstanceOf[AnyRef]
      }
      else {
        block1.dataType = 0x1
        block1.value = v
      }

      r.blocks.add(block1)
    }

    r
  } catch {
    case e: Exception => throw new RuntimeException("error parsing line: '" + line + "'", e)
  }

  def wlnPackToBinary(wln: WialonPackage): Array[Byte] = {


    val packag = new ByteArrayOutputStream()
    val packagDataStream = new DataOutputStream(packag)

    val body = new ByteArrayOutputStream()
    val bodyDataStream = new DataOutputStream(body)

    bodyDataStream.writeBytes(wln.imei)
    bodyDataStream.writeByte(0)

    bodyDataStream.writeInt((wln.time.getTime / 1000).toInt)

    // flags
    bodyDataStream.writeInt(wln.flags)

    // blocktype
    bodyDataStream.writeShort(3003)

    val coordBlock: WialonCoordinatesBlock = wln.getCoordinatesBlock


    val packbody: Array[Byte] = createCoordinatesBlock(
      coordBlock
    )

    bodyDataStream.writeInt(packbody.length)

    bodyDataStream.write(packbody)

    for (x <- wln.blocks.filter(_ ne coordBlock)) {
      // blocktype
      bodyDataStream.writeShort(x.`type`)
      val packbody: Array[Byte] = createBlock(x)
      bodyDataStream.writeInt(packbody.length)
      bodyDataStream.write(packbody)
    }

    bodyDataStream.flush()


    val packSize = body.size()

    val bytearr = ByteBuffer.allocate(4).putInt(packSize).array();

    packagDataStream.write(bytearr.reverse)
    packagDataStream.write(body.toByteArray)
    packagDataStream.flush()


    val pack: Array[Byte] = packag.toByteArray
    pack

  }


}



