package ru.sosgps.wayrecall.avlprotocols.navtelecom

import java.util.Date

import io.netty.buffer.{ByteBufInputStream, Unpooled, ByteBuf}
import ru.sosgps.wayrecall.utils.ScalaNettyUtils
import ScalaNettyUtils._
import ru.sosgps.wayrecall.utils.io.{Utils, RichDataInput}
import com.google.common.io.{LittleEndianDataOutputStream, LittleEndianDataInputStream}
import java.io.{ByteArrayOutputStream, InputStream, DataInput, ByteArrayInputStream}
import com.google.common.base.Charsets
import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.JavaConversions.mutableMapAsJavaMap
import java.nio.{ ByteOrder}
import org.joda.time.{DateTimeZone, DateTime}
import resource._
import ru.sosgps.wayrecall.core.GPSData
import java.util

import scala.io.Source


/**
 * Created by nickl on 03.06.14.
 */
object NavtelecomParser extends grizzled.slf4j.Logging {


  def parsePrefix(channelBuffer: ByteBuf): NavtelecomPackage = {
    (for (is <- managed( new ByteBufInputStream(channelBuffer))) yield {
      val input = new RichDataInput(is:DataInput)
      val transheader = input.readNumberOfBytes(16)
      val transheaderinput = new RichDataInput(new LittleEndianDataInputStream(new ByteArrayInputStream(transheader)).asInstanceOf[DataInput])
      //val transheaderinput = new RichDataInput(transheader)
      val preambule = new String(transheaderinput.readNumberOfBytes(4), Charsets.US_ASCII)
      trace("preambule=" + preambule)
      val IDr = transheaderinput.readInt()
      trace("IDr=" + IDr)
      val IDs = transheaderinput.readInt()
      trace("IDs=" + IDs)
      val size = transheaderinput.readUnsignedShort()
      trace("size=" + size)
      val CSd = transheaderinput.readUnsignedByte()
      val CSp = transheaderinput.readUnsignedByte()
      trace("CSd=" + CSd + " CSp=" + CSp)
      //trace("transheader=" + Utils.toHexString(transheader, ""))
      trace("transheaderxor=" + xorSum(transheader.dropRight(1)))

      val body = input.readNumberOfBytes(size)

      trace("body=" + Utils.toHexString(body, ""))
      trace("bodystr=" + new String(body, Charsets.US_ASCII))
      trace("bodyxor=" + xorSum(body))
      NavtelecomPackage(IDr, IDs, body)
    }).opt.get
  }


  def makePackage(preambule: String, IDr: Int, IDs: Int, body: ByteBuf): ByteBuf = {

    //val result = ByteStreams.newDataOutput(16 + body.length)
    //val result = new ByteArrayOutputStream(16 + body.length)
    //val stream = new LittleEndianDataOutputStream(result)
    val header = Unpooled.buffer(16).order(ByteOrder.LITTLE_ENDIAN)
    //val stream = new DataOutputStream(result)
    header.writeBytes(preambule.getBytes(Charsets.US_ASCII).take(4))
    header.writeInt(IDr)
    header.writeInt(IDs)
    header.writeShort(body.readableBytes())
    header.writeByte(xorSumReadable(body))
    //stream.flush()
    header.writeByte(xorSumReadable(header))
    Unpooled.wrappedBuffer(header, body)

  }

  def makeCommand(preambule: String, IDr: Int, IDs: Int, command: NavtelecomCommand) =
    makePackage(preambule, IDr, IDs, Unpooled.wrappedBuffer(("*"+command.text).getBytes(Charsets.US_ASCII)))

  val flexAnswer: Array[Byte] = "*<FLEX".getBytes(Charsets.US_ASCII)

  def makeFlexPackage( protocol: Short, protocolVersion: Short, structVersion: Short): ByteBuf = {

    //val result = ByteStreams.newDataOutput(16 + body.length)
    val header = Unpooled.buffer(16).order(ByteOrder.LITTLE_ENDIAN)
    //val stream = new DataOutputStream(result)
    header.writeBytes(flexAnswer)
    header.writeByte(protocol)
    header.writeByte(protocolVersion)
    header.writeByte(structVersion)
    header
  }

  def parseTelemetry(imei: String, data: Array[Byte]): (Int, GPSData) = {

    val inputStream: InputStream = new ByteArrayInputStream(data)
    parseTelemetry(imei, inputStream)
  }

  def parseTelemetry(imei: String, inputStream: InputStream): (Int, GPSData) = {
    val input = new RichDataInput(new LittleEndianDataInputStream(inputStream): DataInput)
    //val input = new RichDataInput(data)


    val format = input.readByte() // r 1
    format match {
      case 0x06 => parseF6(imei, input)
      case 0x25 => parseF5_2(imei, input)
      case _ => throw new IllegalArgumentException("unsipported format " + format)
    }
  }

  private def parseF5_2(imei: String, input: RichDataInput): (Int, GPSData) = {
    val index = input.readInt() // r 2
    //trace("index=" + index)

    val data = new util.HashMap[String, AnyRef]()
    data.put("protocol", "Navtelecom")
    data.put("protocolversion", "F5.2")

    val event = input.readUnsignedShort() // r 3
    //trace("event=" + event)

    data.put("event", event.asInstanceOf[AnyRef])

    val time: DateTime = readDate(input) // r 4
    trace("time=" + time)

    val status = input.readUnsignedByte() // r 5
    //trace("status=" + status)
    data.put("status", status.asInstanceOf[AnyRef])
    val modstatus = input.readUnsignedByte() // r 6
    //trace("modstatus=" + modstatus)
    data.put("modstatus", modstatus.asInstanceOf[AnyRef])

    val gsmlevels = input.readUnsignedByte() // r 7
    //trace("gsmlevels=" + gsmlevels)
    data.put("gsmlevels", gsmlevels.asInstanceOf[AnyRef])

    val inputs = input.readUnsignedByte() // r 8
    //trace("inputs=" + inputs)

    data.put("out1", ((inputs >> 0) & 0x1).asInstanceOf[AnyRef])
    data.put("out2", ((inputs >> 1) & 0x1).asInstanceOf[AnyRef])
    val satteliteNum = (inputs >> 2) & 0x3f

    data.put("inputs", inputs.asInstanceOf[AnyRef]) // TODO:Избыточно

    val digsens = input.readUnsignedByte() // r 9
    //trace("digsens=" + digsens)
    data.put("ignition", ((digsens >> 0) & 0x1).asInstanceOf[AnyRef])
    data.put("digsens", digsens.asInstanceOf[AnyRef])

    val mainvoltage = input.readUnsignedShort() // r 10
    //trace("mainvoltage=" + mainvoltage)
    data.put("voltage", mainvoltage.asInstanceOf[AnyRef])

    val reservvoltage = input.readUnsignedShort() // r 11
    //trace("reservvoltage=" + reservvoltage)
    data.put("voltage-reserve", reservvoltage.asInstanceOf[AnyRef])

    val ain1 = input.readUnsignedShort() // r 13
    //trace("ain1=" + ain1)
    data.put("ain1", ain1.asInstanceOf[AnyRef])

    val ain2 = input.readUnsignedShort() // r 14
    //trace("ain2=" + ain2)
    data.put("ain2", ain2.asInstanceOf[AnyRef])

    val imc1 = input.readInt() // r 15
    //trace("in2=" + in2)
    data.put("imc1", imc1.asInstanceOf[AnyRef])

    val imc2 = input.readInt() // r 16
    //trace("in3=" + in3)
    data.put("imc2", imc2.asInstanceOf[AnyRef])

    val gpsstate = input.readByte() // r 17
    val (gpsEnabled, gpsValid, satteliteType) =
      extractGPSState(gpsstate) //& 0x3f
    //debug("gpsstate=" + gpsstate)
    data.put("gpsstate", gpsstate.asInstanceOf[AnyRef])

    val vtime: DateTime = readDate(input) // r 18
    //trace("vtime=" + vtime)

    val lat = input.readFloat().toDegrees // r 19
    val lon = input.readFloat().toDegrees // r 20
    //trace("lat=" + lat + " lon=" + lon)

    val speed = input.readFloat() // r 21
    //trace("speed=" + speed)

    val course = input.readUnsignedShort() // r 22
    //trace("course=" + course)

    val dist = input.readFloat() // r 23
    data.put("c_dist", dist.asInstanceOf[AnyRef])
    //trace("dist=" + dist)

    val lastdist = input.readFloat() // r 24
    data.put("l_dist", lastdist.asInstanceOf[AnyRef])
    //trace("lastdist=" + lastdist)

    val sec = input.readUnsignedShort() // r 25
    data.put("l_dist_s", lastdist.asInstanceOf[AnyRef])
    //trace("sec=" + sec)

    val secv = input.readUnsignedShort() // r 26
    data.put("l_d_sv", lastdist.asInstanceOf[AnyRef])
    //trace("secv=" + secv)

    for (i <- 1 to 3) {

      val flsf = input.readUnsignedShort() // r 27 30 33
      data.put("flsf"+i, flsf.asInstanceOf[AnyRef])

      val flst = input.readByte() // r 28 31 34
      data.put("flst"+i, flsf.asInstanceOf[AnyRef])

      val flevel = input.readUnsignedShort() // r 29 32 35
      data.put("flevel"+i, flevel.asInstanceOf[AnyRef])

    }

    for (i <- 1 to 4) {
      val tem = input.readByte() // r 36 37 38 39
      //trace("temdig" + i + "=" + tem)
      if (tem != -128)
        data.put("temdig" + i, tem.asInstanceOf[AnyRef])
      else
        data.put("temdig" + i + "-err", "n/c")
    }


    //trace("secv=" + secv)

    val gps = new GPSData(null, imei, lon, lat, vtime.toDate, speed.toShort, course.toShort, satteliteNum.toByte)
    gps.data = data
    //debug("gps="+gps)
    (index, gps)
  }

  private def parseF6(imei: String, input: RichDataInput): (Int, GPSData) = {
    val index = input.readInt() // r 2
    //trace("index=" + index)

    val data = new util.HashMap[String, AnyRef]()
    data.put("protocol", "Navtelecom")
    data.put("protocolversion", "F6")

    val event = input.readUnsignedShort() // r 3
    //trace("event=" + event)

    data.put("event", event.asInstanceOf[AnyRef])

    val time: DateTime = readDate(input) // r 4
    trace("time=" + time)

    val status = input.readUnsignedByte() // r 5
    //trace("status=" + status)
    data.put("status", status.asInstanceOf[AnyRef])
    val modstatus = input.readUnsignedByte() // r 6
    //trace("modstatus=" + modstatus)
    data.put("modstatus", modstatus.asInstanceOf[AnyRef])

    val gsmlevels = input.readUnsignedByte() // r 7
    //trace("gsmlevels=" + gsmlevels)
    data.put("gsmlevels", gsmlevels.asInstanceOf[AnyRef])

    val inputs = input.readUnsignedByte() // r 8
    //trace("inputs=" + inputs)
    data.put("inputs", inputs.asInstanceOf[AnyRef])

    data.put("out1", ((inputs >> 0) & 0x1).asInstanceOf[AnyRef])
    data.put("out2", ((inputs >> 1) & 0x1).asInstanceOf[AnyRef])
    data.put("out3", ((inputs >> 2) & 0x1).asInstanceOf[AnyRef])
    data.put("out4", ((inputs >> 3) & 0x1).asInstanceOf[AnyRef])

    val digsens = input.readUnsignedByte() // r 9
    //trace("digsens=" + digsens)
    data.put("ignition", ((digsens >> 0) & 0x1).asInstanceOf[AnyRef])
    data.put("digsens", digsens.asInstanceOf[AnyRef])

    val mainvoltage = input.readUnsignedShort() // r 10
    //trace("mainvoltage=" + mainvoltage)
    data.put("voltage", mainvoltage.asInstanceOf[AnyRef])

    val reservvoltage = input.readUnsignedShort() // r 11
    //trace("reservvoltage=" + reservvoltage)
    data.put("voltage-reserve", reservvoltage.asInstanceOf[AnyRef])

    val ain1 = input.readUnsignedShort() // r 12
    //trace("ain1=" + ain1)
    data.put("ain1", ain1.asInstanceOf[AnyRef])

    val ain2 = input.readUnsignedShort() // r 13
    //trace("ain2=" + ain2)
    data.put("ain2", ain2.asInstanceOf[AnyRef])

    val ain3 = input.readUnsignedShort() // r 14
    //trace("ain3=" + ain3)
    data.put("ain3", ain3.asInstanceOf[AnyRef])

    val in2 = input.readInt() // r 15
    //trace("in2=" + in2)
    data.put("in2", in2.asInstanceOf[AnyRef])

    val in3 = input.readInt() // r 16
    //trace("in3=" + in3)
    data.put("in3", in3.asInstanceOf[AnyRef])

    val freq1 = input.readUnsignedShort() // r 17
    //trace("freq1=" + freq1)
    data.put("freq1", freq1.asInstanceOf[AnyRef])

    val freq2 = input.readUnsignedShort() // r 18
    //trace("freq2=" + freq2)
    data.put("freq2", freq2.asInstanceOf[AnyRef])

    val CANperc = input.readByte() // r 19
    //trace("CANperc=" + CANperc)
    if (CANperc != -1)
      data.put("CANperc", CANperc.asInstanceOf[AnyRef])

    val CANlvl = input.readShort() // r 20
    //trace("CANlvl=" + CANlvl)
    if (CANlvl != -1)
      data.put("CANlvl", CANlvl.asInstanceOf[AnyRef])

    def putfuel(fuelname: String, fuel: Int): Unit = {
      if (fuel < 65530)
        data.put(fuelname, fuel.asInstanceOf[AnyRef])
      else {
        val err = fuel match {
          case 65535 => "answer"
          case 65534 => "command code"
          case 65533 => "address"
          case 65532 => "CRC"
          case 65531 => "n/i"
          case 65530 => "no conn"
        }
        data.put(fuelname + "-err", err)
      }
    }

    for (i <- 1 to 6) {
      val tem = input.readByte() // r 21 23 25 27 29 31
      //trace("tem" + i + "=" + tem)
      data.put("tem" + i, tem.asInstanceOf[AnyRef])

      val fuel = input.readUnsignedShort() // r 22 24 26 28 30 32
      //trace("fuel" + i + "=" + fuel)
      putfuel("fuel" + i, fuel)
    }

    val tem = input.readByte() // r 33
    //trace("tem=" + tem)
    data.put("tem", tem.asInstanceOf[AnyRef])

    val fuel = input.readUnsignedShort() // r 34
    //trace("fuel=" + fuel)
    putfuel("fuel", fuel)

    for (i <- 1 to 4) {
      val tem = input.readByte() // r 35 36 37 38
      //trace("temdig" + i + "=" + tem)
      if (tem != -128)
        data.put("temdig" + i, tem.asInstanceOf[AnyRef])
      else
        data.put("temdig" + i + "-err", "n/c")
    }

    val weight = input.readInt() // r 39
    //trace("weight=" + weight)
    if (weight != -1)
      data.put("weight", weight.asInstanceOf[AnyRef])

    val enginespeed = input.readUnsignedShort() // r 40
    //trace("enginespeed=" + enginespeed)
    if (enginespeed <= 25000)
      data.put("enginespeed", enginespeed.asInstanceOf[AnyRef])

    val gpsstate = input.readByte() // r 41
    val (gpsEnabled, gpsValid, satteliteNum) =
      extractGPSState(gpsstate) //& 0x3f
    //debug("gpsstate=" + gpsstate)
    data.put("gpsstate", gpsstate.asInstanceOf[AnyRef])

    val vtime: DateTime = readDate(input) // r 42
    //trace("vtime=" + vtime)

    val lat = input.readInt() / 600000.0 // r 43
    val lon = input.readInt() / 600000.0 // r 44
    //trace("lat=" + lat + " lon=" + lon)

    val h = input.readInt() / 10.0 // r 45
    //trace("h=" + h)

    val speed = input.readFloat() // r 46
    //trace("speed=" + speed)

    val course = input.readUnsignedShort() // r 47
    //trace("course=" + course)

    val dist = input.readFloat() // r 48
    data.put("c_dist", dist.asInstanceOf[AnyRef])
    //trace("dist=" + dist)

    val lastdist = input.readFloat() // r 49
    data.put("l_dist", lastdist.asInstanceOf[AnyRef])
    //trace("lastdist=" + lastdist)

    val sec = input.readUnsignedShort() // r 50
    data.put("l_dist_s", lastdist.asInstanceOf[AnyRef])
    //trace("sec=" + sec)

    val secv = input.readUnsignedShort() // r 51
    data.put("l_d_sv", lastdist.asInstanceOf[AnyRef])
    //trace("secv=" + secv)

    val gps = new GPSData(null, imei, lon, lat, vtime.toDate, speed.toShort, course.toShort, satteliteNum.toByte)
    gps.data = data
    (index, gps)
  }

  def extractGPSState(gpsstate: Byte): (Boolean, Boolean, Int) = {
    (((gpsstate >> 0) & 0x01) == 0, ((gpsstate >> 1) & 0x01) == 1, gpsstate >> 2)
  }

  private[this] def readDate(input: RichDataInput): DateTime = {
    val datearr = input.readNumberOfBytes(6)

    val h = datearr(0)
    val m = datearr(1)
    val s = datearr(2)
    val d = datearr(3)
    val mon = datearr(4) + 1
    val y = datearr(5)

    trace(s"datear: $h:$m:$s $d.$mon.$y")

    val time = new DateTime(2000 + y, mon, d, h, m, s, DateTimeZone.UTC)
    time
  }

  def xorSum(body: Array[Byte]): Byte = body.fold(0.toByte)((s, a) => (s ^ a).toByte)

  def xorSumReadable(buff: ByteBuf): Byte = buff.fold(buff.readerIndex(), buff.readableBytes())(0.toByte)((s, a) => (s ^ a).toByte)

}

case class NavtelecomPackage(IDr: Int, IDs: Int, body: Array[Byte])

object FlexParser extends grizzled.slf4j.Logging{

  def crc8readable(buffer: ByteBuf): Int  = crc8(buffer.toIteratorReadable)

  def crc8(buffer: TraversableOnce[Byte]): Int = {
    var crc = 0xFF
    for (b <- buffer) {
      crc = (crc ^ b);
      for (i <- 0 until 8) {
        //debug("crc="+crc)
        crc = if ((crc & 0x80) != 0) ((crc << 1) ^ 0x31) else (crc << 1);
      }
    }
    crc & 0xFF
  }


  val fields = (for ( (i, line) <- Iterator.from(1) zip
    Source.fromInputStream(this.getClass.getClassLoader.getResourceAsStream(
      "ru.sosgps.wayrecall.avlprotocols.navtelecom/flex.txt"
    )).getLines()) yield {
    (i, line.split("\t"))
  }).toMap

  //fields.values.map(_(1)).toSet.foreach((v:String) => println("\""+v+"\""))


  val readers = Map[String, (ByteBuf) => Any](
    "U8" -> (_.readUnsignedByte()),
    "U16" -> (_.readUnsignedShort()),
    "U32" -> (_.readUnsignedInt()),
    "I8" -> (_.readByte()),
    "I16" -> (_.readShort()),
    "I32" -> (_.readInt()),
    "Float" -> (_.readFloat())
  )

  val sizes = fields.mapValues(_(0).toInt)

  def parseBody(imei: String, data: ByteBuf, flexBits: SortedSet[Int]) = {
    val result = parseBodyToMap(data, flexBits)
    val llconverter: PartialFunction[Any, Double] = {case i: Int => i / 600000.0}

    def removeAs[T](key: String): Option[T] = result.remove(key).map(_.asInstanceOf[T])

    val lon = result.remove("lon").map(llconverter).getOrElse(Double.NaN)
    val lat = result.remove("lat").map(llconverter).getOrElse(Double.NaN)

    val time = removeAs[Long]("time").map(ti => new Date(ti.toLong * 1000)).orNull

    val satnum = removeAs[Short]("gpsstatus").map(a => NavtelecomParser.extractGPSState(a.toByte)).map(_._3).getOrElse(-1)

    result ++= removeAs[Short]("modstatus1").map(modstatus1 =>  Seq(
        "gsm" -> (modstatus1 >> 0 & 1),
        "usb" -> (modstatus1 >> 1 & 1),
        "reserv" -> (modstatus1 >> 2 & 1),
        "clockSync" -> (modstatus1 >> 3 & 1),
        "sim" -> (modstatus1 >> 4 & 1),
        "network" -> (modstatus1 >> 5 & 1),
        "roaming" -> (modstatus1 >> 6 & 1),
        "ignition" -> (modstatus1 >> 7 & 1)
      )).getOrElse(Seq.empty)

    val gpsb = new GPSData(null, imei, lon, lat, time,
      removeAs[Float]("speed").map(_.toShort).getOrElse(0): Short,
      removeAs[Int]("course").map(_.toShort).getOrElse(0),
      satnum.toByte)

    result.foreach(kv => gpsb.data.put(kv._1, kv._2.asInstanceOf[AnyRef]))
    gpsb.data.put("protocol", "Navtelecom-flex")
    //gpsb.data.put("protocolversion", "flex")
    trace("gpsb=" + gpsb)
    gpsb
  }


  def parseBodyToMap(data: ByteBuf, flexBits: SortedSet[Int]): mutable.HashMap[String, Any] = {
    trace("parseBody: " + Utils.toHexString(data, ""))

    val extracted = flexBits.iterator.map(i => {
      val record = fields(i)
      val fieldName = record(2)
      val fieldType = record(1)
      val fieldSize = record(0).toInt
      val rindex = data.readerIndex()
      val r = (fieldName, readers(fieldType)(data))
      trace("reading " + i + "(" + fieldType + ") = " + r)
      r
    })

    val result = mutable.HashMap.newBuilder[String, Any].++=(extracted).result()
    //debug("result=" + result)
    result
  }
}
