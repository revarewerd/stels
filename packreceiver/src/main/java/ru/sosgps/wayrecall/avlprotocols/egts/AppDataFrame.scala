package ru.sosgps.wayrecall.avlprotocols.egts

import java.nio.ByteOrder
import java.util.Date

import _root_.io.netty.buffer.ByteBuf
import com.google.common.base.Charsets
import org.joda.time.{DateTimeZone, DateTime}
import ru.sosgps.wayrecall.utils._
import ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.io.Utils

import scala.collection.mutable

object AppDataFrame {
  val EGTS_SR_TERM_IDENTITY: Int = 1
  val EGTS_SR_POS_DATA: Int = 16

  val baseDateTime = new DateTime(2010, 1, 1, 0, 0, 0, DateTimeZone.UTC).getMillis
}

class AppDataFrame extends Frame with grizzled.slf4j.Logging {

  import AppDataFrame._

  var recordNumber: Int = 0

  var recordFlags: Byte = 0

  var oid: Option[Int] = None

  var evid: Option[Int] = None

  var tm: Option[Int] = None

  var sourceServiceType: Byte = 0

  var recepientServiceType: Byte = 0

  var subrecords: mutable.Buffer[AppDataFrameSubrecord] = mutable.Buffer.empty

  override def read(frameData0: ByteBuf) = {
    val frameData = frameData0.order(ByteOrder.LITTLE_ENDIAN)
    debug("body readableBytes = " + frameData.readableBytes())

    val recordLength = frameData.readUnsignedShort()
    debug(s"recordLength = $recordLength")

    recordNumber = frameData.readUnsignedShort()
    debug(s"recordNumber = $recordNumber")

    recordFlags = frameData.readByte()
    debug(s"recordFlags = $recordFlags")

    val tmfe = (recordFlags >> 2 & 0x1) == 1
    val emfe = (recordFlags >> 1 & 0x1) == 1
    val omfe = (recordFlags >> 0 & 0x1) == 1

    debug(s"tmfe = $tmfe")
    debug(s"emfe = $emfe")
    debug(s"omfe = $omfe")

    oid = iff(omfe)(frameData.readUnsignedShort())
    evid = iff(emfe)(frameData.readUnsignedShort())
    tm = iff(tmfe)(frameData.readUnsignedShort())

    sourceServiceType = frameData.readByte()
    debug(s"sourceServiceType = $sourceServiceType")
    recepientServiceType = frameData.readByte()
    debug(s"recepientServiceType = $recepientServiceType")

    val recData = frameData.readSlice(recordLength)
    debug("recData=" + recData.toString(Charsets.UTF_8))
    subrecords.clear()
    while (recData.readableBytes() > 0) {
      val subrecordType = recData.readByte()
      debug("body subrecordType = " + subrecordType)
      val subrecordLength = recData.readUnsignedShort()
      debug("body subrecordLength = " + subrecordLength)
      val subrecordData = recData.readSlice(subrecordLength)
      subrecords.append(
        (subrecordType match {
          case EGTS_SR_TERM_IDENTITY => new TermIdentity()
          // case EGTS_SR_POS_DATA => new PositionData()
          case i => new OthersAppDataFrameSubrecord(i)
        }).read(subrecordData))
    }

    debug("body subrecord readableBytes = " + recData.readableBytes())
    debug("body readableBytes = " + frameData.readableBytes())

    this
  }

  override def write(out0: ByteBuf): ByteBuf = {
    val out = out0.order(ByteOrder.LITTLE_ENDIAN)
    val start = out.writerIndex()
    out.writeShort(-1) // reserve for record length
    out.writeShort(recordNumber)

    recordFlags = (recordFlags & 0xF8 | bit(tm) << 2 | bit(evid) << 1 | bit(oid) << 0).toByte
    out.writeByte(recordFlags)
    oid.foreach(out.writeShort)
    evid.foreach(out.writeShort)
    tm.foreach(out.writeShort)

    out.writeByte(sourceServiceType)
    out.writeByte(recepientServiceType)
    val subrecordsStart = out.writerIndex()
    for (subrecord <- subrecords) {
      val subrecordType = subrecord.subrecordType
      out.writeByte(subrecordType)
      val subrecordLengthPosition = out.writerIndex()
      out.writeShort(-1) // reserve for subrecordLengthPosition length
      val subrecordStart = out.writerIndex()
      subrecord.write(out)
      val subrecoredLength = out.writerIndex() - subrecordStart
      out.setShort(subrecordLengthPosition, subrecoredLength)
    }
    val subrecordsTotalLength = out.writerIndex() - subrecordsStart
    out.setShort(start, subrecordsTotalLength)

  }
}

abstract class AppDataFrameSubrecord(val subrecordType: Int) extends Package


class TermIdentity extends AppDataFrameSubrecord(AppDataFrame.EGTS_SR_TERM_IDENTITY) with grizzled.slf4j.Logging {

  var terminalIdentifier: Long = 0L

  var homeDispatcherIdentifier: Option[Int] = None

  var SSRA: Boolean = false

  var imei: Option[String] = None
  var ims: Option[String] = None

  var langCode: Option[String] = None
  var networkid: Option[Array[Byte]] = None
  var bufferSize: Option[Int] = None
  var MSISDN: Option[String] = None


  override def read(subrecordData: ByteBuf) = {

    terminalIdentifier = subrecordData.readUnsignedInt()
    debug("identity terminalIdentifier = " + terminalIdentifier)
    val flags = subrecordData.readByte()
    val MNe = (flags >> 7 & 0x1) == 1
    val BSe = (flags >> 6 & 0x1) == 1
    val NIDe = (flags >> 5 & 0x1) == 1
    SSRA = (flags >> 4 & 0x1) == 1
    val LNGCe = (flags >> 3 & 0x1) == 1
    val IMSIe = (flags >> 2 & 0x1) == 1
    val IMEIe = (flags >> 1 & 0x1) == 1
    val HDIe = (flags >> 0 & 0x1) == 1
    debug(s"flags=$MNe|$BSe|$NIDe|$SSRA|$LNGCe|$IMSIe|$IMEIe|$HDIe")
    homeDispatcherIdentifier = iff(HDIe) {
      subrecordData.readUnsignedShort()
    }
    imei = iff(IMEIe)(subrecordData.readSlice(15).toString(Charsets.US_ASCII))
    ims = iff(IMSIe)(subrecordData.readSlice(16).toString(Charsets.US_ASCII))

    langCode = iff(LNGCe)(subrecordData.readSlice(3).toString(Charsets.US_ASCII))
    networkid = iff(NIDe)(subrecordData.readSlice(3).toIteratorReadable.toArray)
    bufferSize = iff(BSe)(subrecordData.readUnsignedShort())
    MSISDN = iff(MNe)(subrecordData.readSlice(15).toString(Charsets.US_ASCII))

    debug(s"imei=$imei")
    this
  }

  override def write(out: ByteBuf): ByteBuf = {
    out.writeInt(terminalIdentifier.toInt)

    val flags =
      bit(MSISDN) << 7 |
        bit(bufferSize) << 6 |
        bit(networkid) << 5 |
        bit(SSRA) << 4 |
        bit(langCode) << 3 |
        bit(ims) << 2 |
        bit(imei) << 1 |
        bit(homeDispatcherIdentifier) << 0

    out.writeByte(flags)

    homeDispatcherIdentifier.foreach(out.writeShort)

    @inline
    def toExactly(data: String, num: Int): Array[Byte] = {
      require(data.length == num, s"string $data must be $num length")
      data.getBytes(Charsets.UTF_8)
    }

    imei.foreach(s => out.writeBytes(toExactly(s, 15)))
    ims.foreach(s => out.writeBytes(toExactly(s, 16)))

    langCode.foreach(s => out.writeBytes(toExactly(s, 3)))
    networkid.foreach(d => {
      require(d.length == 3, "networkid.length must be 3");
      out.writeBytes(d)
    })
    bufferSize.foreach(out.writeShort)
    MSISDN.foreach(s => out.writeBytes(toExactly(s, 15)))

    out
  }
}

class PositionData extends AppDataFrameSubrecord(AppDataFrame.EGTS_SR_POS_DATA) with grizzled.slf4j.Logging {

  var navigationTime: Date = null

  var lat: Double = 0

  var long: Double = 0

  var flags: Byte = 0

  var spd: Int = 0

  var direction: Byte = 0

  var odm: Int = 0

  var din: Byte = 0

  var srcc: Byte = 0

  var alt: Option[Int] = None

  var srcd: Option[Short] = None

  override def read(src0: ByteBuf): this.type = {
    val src = src0.order(ByteOrder.LITTLE_ENDIAN)
    debug(s"readable=${src.readableBytes()}")
    val secsSince = src.readUnsignedInt()
    navigationTime = new Date(AppDataFrame.baseDateTime + secsSince * 1000)
    debug(s"navigationTime=$navigationTime")
    lat = src.readUnsignedInt().toDouble/0xFFFFFFFFL*90.0
    long = src.readUnsignedInt().toDouble/0xFFFFFFFFL*180.0
    debug(s"long,lat=$long, $lat")     //55.7032384 37.6948896
    //debug(s"long,lat=${long.toDouble/0xFFFFFFFFL*180.0} , ${long.toDouble/0xFFFFFFFFL*90.0}")
    flags = src.readByte()
    debug(s"flags=$flags")
    val altE = (flags >> 7 & 0x1) == 1
    spd = src.readUnsignedShort()
    debug(s"spd=$spd")
    direction = src.readByte()
    debug(s"direction=$direction")
    odm = src.readMedium()
    debug(s"odm=$odm")
    din = src.readByte()
    debug(s"din=$din")
    srcc = src.readByte()
    debug(s"srcc=$srcc")
    debug(s"readable=${src.readableBytes()}")
    alt = iff(altE)(src.readMedium)
    srcd = iff(src.readableBytes()>0)(src.readShort)
    debug(s"alt=$alt srcd=$srcd")

    debug(s"readable=${src.readableBytes()}")
    this
  }

  override def write(out1: ByteBuf): ByteBuf = {
    val out = out1.order(ByteOrder.LITTLE_ENDIAN)
    val secsSince = (navigationTime.getTime - AppDataFrame.baseDateTime) / 1000
    out.writeInt(secsSince.toInt)
    out.writeInt((lat/90.0*0xFFFFFFFFL).toLong.toInt)
    out.writeInt((long/180.0*0xFFFFFFFFL).toLong.toInt)
    out.writeByte(flags)
    out.writeShort(spd)
    out.writeByte(direction)
    out.writeMedium(odm)
    out.writeByte(din)
    out.writeByte(srcc)
    alt.foreach(out.writeMedium)
    srcd.foreach(i => out.writeShort(i.toShort))
    out
  }
}

class OthersAppDataFrameSubrecord(subrecordType: Int) extends AppDataFrameSubrecord(subrecordType) with grizzled.slf4j.Logging {

  var data: Array[Byte] = Array.empty

  override def read(src: ByteBuf) = {
    data = src.toIteratorReadable.toArray
    debug(s"subrecordType=$subrecordType data=${Utils.toHexString(data, " ")}")
    this
  }

  override def write(buffer1: ByteBuf): ByteBuf = {
    buffer1.writeBytes(data)
  }
}