package ru.sosgps.wayrecall.avlprotocols.gosafe

import com.google.common.base.Charsets
import ru.sosgps.wayrecall.core.{LBSData, GPSData}
import ru.sosgps.wayrecall.utils.io.CRC16CCITT
import ru.sosgps.wayrecall.utils.io.RichDataInput
import ru.sosgps.wayrecall.utils.io.Utils
import java.io._

import java.util.{Date, List}
import scala.util.control.Breaks._
import scala.collection.mutable
import collection.JavaConversions.seqAsJavaList
import java.text.SimpleDateFormat
import scala.collection.mutable.ListBuffer
import java.util
import ru.sosgps.wayrecall.data.sleepers.{LBSConverter, LBSHttp, LBS}
import scala.io.Source
import org.joda.time.{DateTimeZone, DateTime}
import ru.sosgps.wayrecall.avlprotocols.gosafe.GosafeParseResult

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 26.05.13
 * Time: 15:23
 * To change this template use File | Settings | File Templates.
 */
object GosafeParser extends grizzled.slf4j.Logging {


  private val deviceStatuses: Map[Int, Array[String]] = readFields("deviceStatus.txt")
  private val deviceAlarms: Map[Int, Array[String]] = readFields("deviceAlarms.txt")


  private[this] def readFields(name: String): Map[Int, Array[String]] = {
    val asStream = this.getClass.getResourceAsStream(name)
    require(asStream != null)
    Source.fromInputStream(asStream, "UTF8")
      .getLines().map(_.split("\t")).map(a => (a.head.toInt, a.tail)).toMap

  }

  private val bF8: Byte = 0xF8.asInstanceOf[Byte]
  private val b1B: Byte = 0x1B
  private val b00: Byte = 0x00
  private val bE3: Byte = 0xE3.asInstanceOf[Byte]
  //private val startTimeMills = 946674000000L
  private val startTime = new DateTime(2000, 1, 1, 0, 0, 0, DateTimeZone.UTC)
  //private val startTimeMills = new Date(100, 0, 1, 0, 0, 0).getTime

  private def readBinaryTail(di: RichDataInput): Array[Byte] = {
    val result = new mutable.ArrayBuilder.ofByte

    while (true) {
      val b: Byte = di.readByte
      if (b == b1B) {
        val b2: Byte = di.readByte
        if (b2 == b00) result += b1B
        else {
          if (b2 == bE3) result += bF8
          else {
            throw new IllegalArgumentException("unexpected seq: " + String.format("%02x%02x", b.asInstanceOf[AnyRef], b2.asInstanceOf[AnyRef]))
          }
        }
      }
      else if (b == bF8) return result.result()
      else result += b
    }

    return result.result()
  }

  private def readTextTail(di: RichDataInput): Array[Byte] = {
    val result = new mutable.ArrayBuilder.ofByte
    while (true) {
      val b: Byte = di.readByte
      if (b == '(') {
        result += di.readByte
      }
      else if (b == '#') return result.result()
      else result += b
    }
    return result.result()
  }


  def readPacket(di: RichDataInput): GosafePacket = {
    val b: Byte = di.readByte
    trace("pack first byte = " + b)
    if (b == bF8) {
      val packet: Array[Byte] = readBinaryTail(di)
      return new GosafeBinaryPacket(packet)
    }
    else if (b == '*') {
      val packet: Array[Byte] = readTextTail(di)
      return new GosafeTextPacket(packet)
    }
    else throw new IllegalArgumentException(String.format("illegal start byte %02x", b.asInstanceOf[AnyRef]))
  }

  def processPackage(in: RichDataInput): GosafeParseResult = {
    val gosafePacket: GosafePacket = readPacket(in)
    debug("gosafePacket=" + gosafePacket);

    gosafePacket match {
      case tp: GosafeTextPacket => parseTextPackage(tp)
      case bp: GosafeBinaryPacket => parseBinaryPacket(bp)
    }

  }


  def parseBinaryPacket(bp: GosafeBinaryPacket) = {
    val inp = new RichDataInput(bp.data)
    val protocol = inp.readByte()
    val typeId = inp.readByte()
    //val imei = inp.readLong().toString
    val imei = Utils.toHexString(inp.readNumberOfBytes(8), "").dropWhile(_ == '0')

    val domainDataLen = bp.data.length - 12
    var readCount = 0
    //Device domain data
    val gpsResult = new ListBuffer[GPSData]
    val lbsResult = new ListBuffer[LBSData]
    val statuses = new ListBuffer[String]
    val alarms = new ListBuffer[String]
    val adData = new ListBuffer[(String, Double)]
    while (readCount < domainDataLen) {

      val dataId = inp.readByte()
      val len = inp.readByte()
      val pb = new RichDataInput(inp.readNumberOfBytes(len))
      readCount = readCount + len + 2

      dataId match {
        case 1 => gpsResult += readGpsData(pb, len, imei)
        case 2 => lbsResult += readLBSData(pb, imei)
        case 3 => {
          val (statuses1, alarms1) = readAlarms(pb)
          statuses ++= statuses1
          alarms ++= alarms1
        }
        case 5 => adData ++= readADdata(len, pb)
        case _ => warn("unknown dataId=" + dataId)
      }

    }

    new GosafeParseResult(gpsResult.toList, lbsResult.toList, statuses.toList, alarms.toList, adData.toList)
  }


  def readADdata(len: Byte, pb: RichDataInput): IndexedSeq[(String, Double)] = {
    for (b <- 0 until len by 2) yield {
      val addata = pb.readShort()
      val typ = (addata & 0xF000) >> 12
      val valueRaw = addata & 0x0FFF
      val paramName = typ match {
        case 0 => "Device Battery Voltage (Internal)" // External Power Supply Voltage
        case 1 => "Device Temperature"
        case 2 => "Ext Power"
        case 3 => "Analog Input Voltage (connect to device IO port)"
        case _ => "Unknown(" + typ + ")"
      }

      val (adMin, adMax) = if (typ == 1) (-55, 125) else (-10, 100)
      /*
            (AD_MAX – AD_MIN)/4096 + AD_MIN

             */
      val value = valueRaw.toDouble * (adMax - adMin) / 4096 + adMin

      (paramName, value)
    }
  }

  private[this] def readAlarms(pb: RichDataInput): (Seq[String], Seq[String]) = {
    val statuses1 = new ListBuffer[String]
    val alarms1 = new ListBuffer[String]
    val deviceStatus = pb.readUnsignedShort()
    val deviceAlarm = pb.readUnsignedShort()
    //println("deviceStatus =" + deviceStatus.toBinaryString)
    //println("deviceAlarm =" + deviceAlarm.toBinaryString)

    for (b <- 0 to 15) {
      val st = deviceStatuses(b)
      val bv = (deviceStatus >> b) & 0x01
      //println(b + " -> "+st(0)+" = "+bv)
      if (bv == 1)
        statuses1 += st(0) + ":" + st(1 + bv)
    }

    for (b <- 0 to 15;
         st <- deviceAlarms.get(b)
    ) {
      val bv = (deviceAlarm >> b) & 0x01
      //println(b + " -> "+st(0)+" = "+bv)
      if (bv == 1)
        alarms1 += st(0) //+":"+st(1+bv)
    }
    (statuses1, alarms1)
  }

  private[this] def readLBSData(pb: RichDataInput, imei: String): LBSData = {
    val date = readDate(pb)
    val mcc = pb.readUnsignedShort()
    val mnc = pb.readUnsignedByte()
    val lac = pb.readUnsignedShortLE()
    val cid = pb.readUnsignedShortLE()
    val data = new LBSData(null, imei, LBS(mcc, mnc, lac, cid), date)
    data
  }

  private[this] def readDate(pb: RichDataInput): Date = {
    val ltd = pb.readInt()
    val loc = ltd >> 31
    val time = ltd & 0x7fffffff
    //val date = new Date(time * 1000L + startTimeMills)
    val date = startTime.plusSeconds(time).toDate
    date
  }

  private[this] def readGpsData(pb: RichDataInput, len: Byte, imei: String): GPSData = {
    val date = readDate(pb)

    val lat = pb.readInt() / 1000000.0
    val lon = pb.readInt() / 1000000.0

    val (angle, speed, hdop) = if (len == 14) {
      val angleAndSpeed = pb.readShort()
      val angd = (angleAndSpeed >> 13).toShort
      val speed = (angleAndSpeed & 0x1FFF).toShort
      (angd, speed, 0)
    } else {
      val speed = pb.readShort()
      val angle = pb.readShort()
      val hdop = pb.readShort()
      (angle, speed, hdop)
    }

    val gps = new GPSData(null, imei, lon, lat, date, speed, angle, 0)
    gps.data.put("protocol", "gosafe bin")
    gps
  }

  def parseTextPackage(tp: GosafeTextPacket) = {
    val result = new ListBuffer[GPSData]

    val commands = tp.text.split(",")
    require(commands(0) == "GS02" || commands(0) == "GS22", "commands(0)=" + commands(0) + " but must be \"GS02\" or \"GS22\"  ")
    val imei = commands(1)

    //GPS:065633;A;N23.164865;E113.428970;0;0;150411
    val dfParser1 = new SimpleDateFormat("HHmmssddMMyy")
    for (gpsString <- commands.filter(_.startsWith("GPS:"))) {
      val params: Array[String] = gpsString.split(";")

      val timeStr = params(0).stripPrefix("GPS:")
      if (params(1) == "A") {
        val lat = params(2).stripPrefix("N").toDouble
        val lon = params(3).stripPrefix("E").toDouble
        val speed = params(4).toShort
        val angle = params(5).toShort
        val dateStr = params(6)
        val date = dfParser1.parse(timeStr + dateStr)

        val gps = new GPSData(null, imei, lon, lat, date, speed, angle, 0)
        gps.data.put("protocol", "gosafe text")
        result += gps
      }
    }
    new GosafeParseResult(result, Seq.empty, Seq.empty, Seq.empty, Seq.empty)
  }

  def convertLBSToGpsData(lbsConv:LBSConverter,  parseResult: GosafeParseResult): Seq[GPSData] = {

    for (lbs <- parseResult.lbsData) yield {
      debug("getting gps for lbs: " + lbs)
      val (lon, lat) = if (lbs.lbs.MCC != 65535) {
        val ll = lbsConv.convertLBS(lbs.lbs)
        (ll.lon, ll.lat)
      } else (Double.NaN, Double.NaN)
      val gps = new GPSData(lbs.uid, lbs.imei, lon, lat, lbs.time, 0, 0, 0)
      gps.data.put("protocol", "Gosafe LBS")
      gps.data.put("lbs", lbs.lbs.toHexString)
      debug("writing gps from lbs: " + gps)
      gps
    }
  }


  def appendAdditionalData(parseResult: GosafeParseResult, gps: GPSData) {
    if (parseResult.alarms.nonEmpty)
      gps.data.put("alarms", parseResult.alarms.mkString("{", ", ", "}"))
    if (parseResult.statuses.nonEmpty)
      gps.data.put("statuses", parseResult.statuses.mkString("{", ", ", "}"))
    for ((k, v) <- parseResult.addata) {
      gps.data.put(k, String.format(java.util.Locale.US, "%.2f", v.asInstanceOf[AnyRef]))
    }
  }
}

class GosafeParseResult(
                         val gpsData: Seq[GPSData],
                         val lbsData: Seq[LBSData],
                         val statuses: Seq[String],
                         val alarms: Seq[String],
                         val addata: Seq[(String, Double)]

                         ) {
  override def toString() = "GosafeParseResult(" +
    "gpsData=" + gpsData +
    " lbsData=" + lbsData +
    " statuses=" + statuses +
    " alarms=" + alarms +
    " addata=" + addata +
    ")"
}