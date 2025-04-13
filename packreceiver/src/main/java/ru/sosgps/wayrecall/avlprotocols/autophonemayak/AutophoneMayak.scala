package ru.sosgps.wayrecall.avlprotocols.autophonemayak


import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util

import io.netty.buffer.{Unpooled, ByteBuf}
import org.apache.commons.io.Charsets
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils.{ScalaNettyUtils, java8TLocalDateTimeOps}
import ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.io.Utils
import ru.sosgps.wayrecall.utils.java8TLocalDateTimeOps

import scala.Predef

/**
  * Created by nmitropo on 18.12.2015.
  * https://github.com/oxmix/tracking/blob/master/server.py
  */
object AutophoneMayak extends grizzled.slf4j.Logging {

  val AUTH_PACK_CODE: Int = 0x10
  val WORKING_PACK_CODE: Int = 0x11
  val BLACKBOX_PACK_CODE: Int = 0x12

  object AuthorizationPack {
    def unapply(buff: ByteBuf): Option[(String, Int)] = {
      if (buff.getByte(0) == AUTH_PACK_CODE)
        Some(parseAuthorization(buff))
      else
        None
    }
  }

  object WorkingPack {
    def unapply(buff: ByteBuf): Option[(String) => (GPSData, Int)] = {
      if (buff.getByte(0) == WORKING_PACK_CODE)
        Some(imei => parseWorkingPack(imei, buff))
      else
        None
    }
  }

  object BlackBoxPack {
    def unapply(buff: ByteBuf): Option[(String) => (Seq[GPSData], Int)] = {
      if (buff.getByte(0) == BLACKBOX_PACK_CODE)
        Some(imei => parseBlackBoxPack(imei, buff))
      else
        None
    }
  }

  def parseAuthorization(pack: ByteBuf): (String, Int) = {
    val begin = pack.readerIndex()
    val code = pack.readByte()
    require(code == AUTH_PACK_CODE, s"authorization pack expected but have $code")
    val systype = pack.readByte()
    debug("systype = " + systype)
    val version = pack.readByte()
    debug("version = " + version)

    val imei = readImei(pack)
    debug("imei = " + imei)

    val crc = pack.readByte()
    debug("crc = " + crc)
    val end = pack.readerIndex()

    val calcedCRC = checkSum(pack, begin, end - 2)

    if (crc != calcedCRC)
      warn(s"crc != calcedCRC : $crc != $calcedCRC")

    (imei, crc)
  }

  def parseWorkingPack(imei: String, pack: ByteBuf): (GPSData, Int) = {
    val begin = pack.readerIndex()
    val code = pack.readByte()
    require(code == WORKING_PACK_CODE, s"working pack expected but have $code")
    val interval = pack.readByte()
    debug("interval = " + interval)
    val variousSettings = pack.readBytes(8)
    debug("variousSettings = " + variousSettings)

    val mayakState = pack.readByte()
    debug("mayakState = " + mayakState)

    val channelTimeRemains = pack.readUnsignedShort()
    debug("channelTimeRemains = " + channelTimeRemains)

    val voltage = pack.readUnsignedByte() * 0.05
    debug("voltage = " + voltage)

    val rts = readDateWithSecond(pack)
    debug("rts = " + rts)

    readAlarm(pack)
    readAlarm(pack)

    val gps: GPSData = readGPS(imei, pack).get

    val crc = pack.readByte()
    debug("crc = " + crc)
    val end = pack.readerIndex()

    val calcedCRC = checkSum(pack, begin, end - 1)

    if (crc != calcedCRC)
      warn(s"crc != calcedCRC : $crc != $calcedCRC")

    (gps, crc)
  }

  def parseBlackBoxPack(imei: String, pack: ByteBuf): (Seq[GPSData], Int) = {
    val begin = pack.readerIndex()
    val code = pack.readByte()
    require(code == BLACKBOX_PACK_CODE, s"blackbox pack expected but have $code")

    val header = pack.readByte()
    val memoryType = (header >> 3) & 0x01
    debug("memoryType = " + memoryType)

    val numberOfBLocks1 = header & 0x07 //pack.readUnsignedShort()
    debug("numberOfBLocks1 = " + numberOfBLocks1)

    val numberOfBLocks2 = pack.readUnsignedShort()
    debug("numberOfBLocks2 = " + numberOfBLocks2)

    val numberOfBLocks = Math.max(numberOfBLocks1, numberOfBLocks2)

    val gps = (0 until numberOfBLocks).flatMap(i => {
      val mayakState = pack.readByte()
      debug("mayakState = " + mayakState)
      val voltage = pack.readUnsignedByte() * 0.05
      debug("voltage = " + voltage)
      val rts = readDateWithSecond(pack)
      debug("rts = " + rts)

      val gps = readGPS(imei, pack)
      val crc = pack.readUnsignedByte()
      debug("crc = " + crc + "(" + crc.toInt.toHexString + ")")
      gps
    })


    val end = pack.readerIndex()

    val calcedCRC = checkSum(pack, begin, end - 1)
    val crc = pack.readUnsignedByte()
    debug("crc = " + crc + "(" + crc.toInt.toHexString + ")")
    if (crc != calcedCRC)
      warn(s"crc != calcedCRC : $crc != $calcedCRC")


    (gps, crc)
  }

  private[autophonemayak] def readImei(pack: ByteBuf): String = {
    readNumericString(pack, 8).stripPrefix("0")
  }

  private[autophonemayak] def readNumericString(pack: ByteBuf, length: Int): String = {
    (0 until length).map(i => pack.readUnsignedByte().toInt.formatted("%02X")).mkString
  }

  private def readGPS(imei: String, pack: ByteBuf): Option[GPSData] = {
    val temperature = pack.readByte()
    debug("temperature = " + temperature)

    val signalLevel = pack.readUnsignedByte()
    debug("signalLevel = " + signalLevel)

    val MCC = pack.readUnsignedShort()
    val MNC = pack.readUnsignedShort()
    val LAC = pack.readUnsignedShort()
    val CID = pack.readUnsignedShort()

    debug(s"MCC, MNC, LAC, CID = $MCC, $MNC, $LAC, $CID")

    val gpsStatus = pack.readByte()
    debug("gpsStatus = " + gpsStatus)

    val gpsDate = readDateWithSecond(pack)
    debug("gpsDate = " + gpsDate)

    val lat = readCoordinate(pack)
    val lon = readCoordinate(pack)
    debug(s"lonlat = ($lon, $lat)")

    val height = pack.readShort()
    debug("height = " + height)

    val speed = pack.readUnsignedByte()
    debug("speed = " + speed)

    val course = pack.readUnsignedByte() * 2
    debug("course = " + course)
    val hdop = pack.readUnsignedShort() / 10.0
    debug("hdop = " + hdop)

    val reserv = pack.readShort()
    debug("reserv = " + reserv)

    for (gpsDate <- gpsDate) yield {

      val map = new util.HashMap[String, AnyRef]()
      map.put("protocol", "AutophoneMayak")
      val gps = new GPSData(null, imei, lon.toDouble, lat.toDouble, gpsDate.toDate, speed.toShort, course.toShort, gpsStatus, null, null, map)
      gps
    }
  }

  private[autophonemayak] def readCoordinate(pack: ByteBuf): Double = {

    val cint = pack.readInt()

    val degrees = cint / 1000000
    val minutes = cint % 1000000
    debug("degrees = " + degrees + " minutes = " + minutes)
    degrees + Math.signum(degrees) * minutes / 10000 / 60
  }

  private[autophonemayak] def readDateWithSecond(pack: ByteBuf): Option[LocalDateTime] = {
    val rts0 = readDateTimeMinutes(pack)

    val second = pack.readUnsignedByte()
    debug("second = " + second)

    val rts = rts0.map(_.withSecond(second))
    rts
  }

  private def readAlarm(pack: ByteBuf): Unit = {
    val alarm1 = readDateTimeMinutes(pack)
    debug("alarm1 = " + alarm1)

    val alarm1Interval = pack.readUnsignedShort()
    debug("alarm1Interval = " + alarm1Interval)
    val alarmMode = pack.readBytes(5)
    debug("alarmMode = " + alarmMode)
  }

  private[autophonemayak] def readDateTimeMinutes(pack: ByteBuf): Option[LocalDateTime] = {
    val date: Option[LocalDate] = readDate(pack)

    val hour = pack.readUnsignedByte()
    debug("hour = " + hour)

    val minute = pack.readUnsignedByte()
    debug("minute = " + minute)

    date.map(_.atTime(hour, minute))

  }

  private[autophonemayak] def readDate(pack: ByteBuf): Option[LocalDate] = {
    val realTimeDay = pack.readUnsignedByte()
    debug("realTimeDay = " + realTimeDay)

    val month = pack.readUnsignedByte()
    debug("month = " + month)

    val year = pack.readUnsignedByte() + 2000
    debug("year = " + year)

    val date = if (month > 0 && month < 13)
      Some(LocalDate.of(year, month, realTimeDay))
    else None
    date
  }

  def checkSum(message: ByteBuf, checkStart: Int, length: Int): Byte = {
    var GPRS_CRC = 0x3B.toByte
    for (i <- Iterator.from(checkStart).take(length)) {
      var byte = message.getByte(i)
      // void CRC (byte) {GPRS_CRC+=0x56^byte; GPRS_CRC++; GPRS_CRC^=0xC5+byte; GPRS_CRC--;}
      GPRS_CRC = (GPRS_CRC + 0x56 ^ byte).toByte;
      GPRS_CRC = (GPRS_CRC + 1).toByte;
      GPRS_CRC = (GPRS_CRC ^ 0xC5 + byte).toByte;
      GPRS_CRC = (GPRS_CRC - 1).toByte;
    }
    GPRS_CRC
  }

  def answerCRC(crc: Int): ByteBuf = {
    //  resp_crc=x
    val answer = Unpooled.buffer(10)
    //    val utfButed = "resp_crc=".getBytes("UTF8")
    //    debug("utfButed = " + Utils.toHexString(utfButed, " "))
    //    val respString = new String(utfButed,Charsets.toCharset("SCGSM"))
    val respString = "resp_crc="
    //val respString = new Predef.String(Utils.asBytesHex("72 65 73 70 11 63 72 63 3d"), Charsets.toCharset("latin1"))
    debug("respString = " + respString)
    answer.writeBytes(respString.getBytes("SCGSM"))
    answer.writeByte(crc)
    answer
  }

}
