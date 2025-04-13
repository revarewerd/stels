package ru.sosgps.wayrecall.packreceiver.netty

import java.nio.ByteOrder
import java.util.Date

import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.netty.channel.ChannelPipeline
import io.netty.handler.logging.LoggingHandler
import ru.sosgps.wayrecall.avlprotocols.navtelecom.FlexParser
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.data.sleepers.{LBS, LBSConverter}
import ru.sosgps.wayrecall.packreceiver.Netty4Server
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.{ByteBufOps, CompositeByteBufOps}
import ru.sosgps.wayrecall.utils.concurrent.FuturesUtils.{extensions, promiseExtension}
import ru.sosgps.wayrecall.utils.{definitely, intBitReads, longBitReads}
import ru.sosgps.wayrecall.utils.io.{ByteArrayOps, Utils}
import ru.sosgps.wayrecall.utils.io.asyncs.{ByteBufAsyncIn, _}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.{Future, Promise}

/**
  * Created by nmitropo on 15.8.2016.
  */
class MicroMayakServer(lbsConverter: LBSConverter) extends Netty4Server with grizzled.slf4j.Logging {


  override protected def initPipeLine(pipeline: ChannelPipeline): Unit = {
    pipeline.addLast(
      new LoggingHandler(classOf[MicroMayakServer]),
      NettyFutureHandler(futureHandler)
    )
  }

  import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext

  private val START_MARKER = 0x24

  private val AUTH_KEY: Int = 0x02

  private val AUTH_CONFIRMATION: Int = 0x04

  private val END_MARKER: Int = 0x0d

  private val COMPLETED_CORRECTLY = 0x00.toByte

  private val COMPLETED_INCORRECTLY = 0x01.toByte

  private def futureHandler(conn: Connection): Future[Any] = {

    implicit val alloc = conn.allocator

    val imei = authenticate(conn)

    imei.onComplete({
      case r => debug("imei: " + r)
    })


    val Events = new {

      val EventsCodes = Set(0xE0, 0xE1, 0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7, 0xe8, 0xe9, 0xea, 0xeb, 0xec, 0xed, 0xee, 0xef, 0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0xfb, 0xfc, 0xfd, 0xfe, 0xfa).map(_.toByte)

      def unapply(i: Byte): Option[Byte] = {
        //debug(f"check if event: $i ($i%2X ")
        if (EventsCodes(i)) Some(i) else None
      }
    }


    case class State(time: Option[Date])

      imei.flatMap(imei => {

        def process(state: State): Future[Any] = {

        readPackage(conn.in) {
          case 0x05 /*Видимые соты*/ => Mono((body, whenReady) => for (
            lbsArr <- body.readByteBuf(11);
            numberOfBaseStations <- body.readByte;
            _ = debug(s"numberOfBaseStations: $numberOfBaseStations");
            baseStationsData <- Future.sequence((0 until numberOfBaseStations).map(i => body.readByteBuf(9)));
            lbs = parseLBS(lbsArr, baseStationsData);
            ready <- whenReady;
            s = saveGPSbyLBS(imei, lbs, state.time.getOrElse(new Date(nowTime())));
            _ <- sendConfirmationOrFailure(s, conn, ready)
          ) yield state)
          case 0x03 /*Запрос стартовой точки по LBS*/ => Mono((body, whenReady) => for (
            content <- body.readByteBuf(10);
            lbs = readLBS(content);
            ready <- whenReady;
            s = saveGPSbyLBS(imei, Vector(lbs), state.time.getOrElse(new Date(nowTime())));
            r <- sendConfirmation(conn.out, MonoConfirmation(ready, COMPLETED_INCORRECTLY))
          ) yield state)
          case 0x08 /* Состояние блока*/ => Mono((body, whenReady) => for (
            content <- body.readByteBuf(14);
            time = {
              val input = content.order(ByteOrder.LITTLE_ENDIAN)
              val date = new Date(1000 * input.readUnsignedInt())
              debug(s"time = $date")
              date
            };
            ready <- whenReady;
            _ <- sendConfirmation(conn.out, MonoConfirmation(ready, COMPLETED_CORRECTLY))
          ) yield state.copy(time = Some(time)))
          case 0x0C /* Описание аппаратного и программного обеспечения блока */ => Mono((body, whenReady) => for (
            content <- body.readByteBuf(5);
            ready <- whenReady;
            _ <- sendConfirmation(conn.out, MonoConfirmation(ready, COMPLETED_CORRECTLY))
          ) yield state)
          case 0x09 /* Сообщения из черного ящика */ => Mono((body, whenReady) => for (
          //date <- body.order(ByteOrder.BIG_ENDIAN).readInt.map(i => new Date(i * 1000));
            pointData <- body.readByteBuf(14);
            additionalPoints <- body.readByte;
            additionalPointsData <- body.readByteBuf(additionalPoints * 5);
            ready <- whenReady;
            insering = {
              val gpses = parseGPS(imei, pointData, additionalPoints, additionalPointsData)
              assert(gpses.nonEmpty)
              Future.sequence(gpses.map(store.addGpsDataAsync))
            };
            _ <- sendConfirmationOrFailure(insering, conn, ready)
          ) yield state)
          case 0x0A /* Состояние Блока и упакованные географические координаты */ => Mono((body, whenReady) => for (
            firstBlock <- body.readByteBuf(16);
            coordinates1 <- body.readByteBuf(10);
            additionalPoints <- body.readByte;
            additionalPointsData <- body.readByteBuf(additionalPoints * 5);
            ready <- whenReady;
            _ <- {
              error(f"received 0x0A message: ${firstBlock.toHexString}  ${coordinates1.toHexString}  $additionalPoints%2X ${additionalPointsData.toHexString}")
              sendConfirmation(conn.out, MonoConfirmation(ready, COMPLETED_CORRECTLY))
            }) yield state)
          case Events(i) /* Состояние блока*/ => Mono((body, whenReady) => for (
            date <- body.order(ByteOrder.BIG_ENDIAN).readInt.map(i => new Date(i * 1000));
            ready <- whenReady;
            _ <- {
              debug("date:" + date)
              sendConfirmation(conn.out, MonoConfirmation(ready, COMPLETED_CORRECTLY))
            }
          ) yield state)
        }.flatMap(process).recoverWith({
          case e: Exception =>
            error("error in processing:", e)
            conn.close()
            Future.failed(e)
        })

        }

        process(State(None))
      })


  }

  private def saveGPSbyLBS(imei: String, lbs: IndexedSeq[LBS], time: Date): Future[IndexedSeq[GPSData]] = {
    val inserting = Future(lbs.map(lbs => {
      val (lon, lat) = if (lbs.MCC != 65535) {
        val ll = lbsConverter.convertLBS(lbs)
        (ll.lon, ll.lat)
      } else (Double.NaN, Double.NaN)
      val gps = new GPSData(null, imei, lon, lat, time, 0, 0, 0)
      gps.data.put("protocol", "MicroMayak LBS")
      gps.data.put("lbs", lbs.toHexString)
      gps
    }))(scala.concurrent.ExecutionContext.Implicits.global).flatMap(gpses => Future.sequence(gpses.map(store.addGpsDataAsync)))
    inserting
  }

  private def readLBS(input0: ByteBuf): LBS = {
    val input = input0.order(ByteOrder.LITTLE_ENDIAN)
    val mcc_mnc = input.readUnsignedMedium()
    val mnc = mcc_mnc.ofShift(12, 12)
    val mcc = mcc_mnc.ofShift(0, 12)

    val lac = input.readUnsignedShort()
    val cid = input.readInt()
    LBS(mcc, mnc, lac, cid)
  }

  private def parseLBS(lbsArr: ByteBuf, baseStationsData: IndexedSeq[ByteBuf]) = {

    val input = lbsArr.order(ByteOrder.LITTLE_ENDIAN)

    val lbs = readLBS(input)
    debug(s"main lbs:${lbs.toHexString}")
    val ta = input.readUnsignedByte()
    input.skipBytes(1) // Q_GSM, REG_GSM

    assert(input.readableBytes() == 0)

    val otherLBSS = baseStationsData.map(readLBS)
    debug(s"otherLBSS: ${otherLBSS.map(_.toHexString).mkString(", ")}")
    lbs +: otherLBSS
  }


  private def parseGPS(imei: String, m: ByteBuf, additionalCount: Int, additional: ByteBuf): Seq[GPSData] = {
    debug("maingps:" + m.toHexString)
    val input = m.order(ByteOrder.LITTLE_ENDIAN)
    val date = new Date(1000 * input.readUnsignedInt())


    val long = input.readLong()
    val speed = long.ofShift(28 + 28, 8)
    val lon = long.ofShift(28, 28) / 600000.0
    val lat = long.ofShift(0, 28) / 600000.0

    debug(s"lls: $lon $lat $speed $date")

    val dops = input.readUnsignedShort()

    val nglonas = dops.ofShift(12, 4)
    val qGSM = dops.ofShift(8, 4)
    val dop = dops.ofShift(4, 4)
    val ngps = dops.ofShift(0, 4)

    debug(s"sats: $nglonas $ngps dops: $dop $qGSM ")

    val startGPS = new GPSData(null, imei, lon, lat, date, speed.toShort, 0, (nglonas + ngps).toByte)
    startGPS.data.put("protocol", "MicroMayak")

    val additionalData = additional.order(ByteOrder.LITTLE_ENDIAN)

    IndexedSeq.iterate(startGPS, 1 + additionalCount)(gps => {
      val timeShift = additionalData.readByte()
      val shifts = additionalData.readInt()

      val speed = shifts.ofShift(12 + 12, 8)
      val lon = shifts.ofShift(12, 12) / 600000.0
      val lat = shifts.ofShift(0, 12) / 600000.0

      val addGps = gps.clone()
      addGps.time.setTime(addGps.time.getTime + speed * 1000)
      addGps.speed = (addGps.speed + speed).toShort
      addGps.lon += lon
      addGps.lat += lat
      addGps
    })
  }

  private def authenticate(conn: Connection): Future[String] =

    readPackage(conn.in) {
      case AUTH_KEY => Mono((body, whenReady) => for (
        imei <- body.readByteBuf(8).map(readImei);
        ready <- whenReady;
        r <- conn.out.write(
          mkPacket(
            conn.out.alloc(8)
              .writeByte(AUTH_CONFIRMATION)
              .writeByte(ready.kvitok)
              .order(ByteOrder.BIG_ENDIAN).writeInt((nowTime() / 1000).toInt)
              .writeByte(COMPLETED_CORRECTLY)
          )(conn.allocator)
        )) yield imei)
    }

  private def readImei(pack: ByteBuf): String = {
    readNumericString(pack, 8).stripPrefix("0")
  }

  private def readNumericString(pack: ByteBuf, length: Int): String = {
    (0 until length).map(i => pack.readUnsignedByte().toInt.formatted("%02X")).mkString
  }

  abstract sealed class PackHandler[T]

  case class Mono[T](f: (ByteBufAsyncIn, Future[Info]) => Future[T]) extends PackHandler[T]

  case class Info(marker: Byte, kvitok: Byte)

  case class MonoConfirmation(kvitok: Byte, maker: Byte, state: Byte)

  object MonoConfirmation {

    def apply(info: Info, state: Byte): MonoConfirmation = MonoConfirmation(info.kvitok, info.marker, state)

  }

  case class PackageInfo(marker: Int, body: ByteBufAsyncIn, ready: Future[Info])

  private def readPackage[T](in: ByteBufAsyncIn)(bodyReaders: PartialFunction[Byte, PackHandler[T]]): Future[T] = {

    for (
      startByte <- in.readByte if definitely(startByte == START_MARKER, f"$startByte%2X must be $START_MARKER%2X (START_MARKER)");
      recording = new RecordingByteBufAsyncIn(in);
      marker <- recording.readByte;
      reader = bodyReaders(marker);
      (ready, bodyFuture) <- reader match {
        case Mono(bodyReader) => for (
          kvitok <- recording.readByte;
          ready = Promise[Info];
          bodyFuture = bodyReader(recording, ready.future)
        ) yield (ready.delegateBy[Unit](_ => Info(marker, kvitok)), bodyFuture)
      };
      crc <- in.readByte;
      ending <- in.readByte if definitely(ending == END_MARKER, f"$ending%2X must be $END_MARKER%2X (END_MARKER)");
      _ = ready.success();
      r <- bodyFuture
    ) yield r

  }


  protected def nowTime(): Long = {
    System.currentTimeMillis()
  }

  def checkSum(bb: ByteBuf): Int = {
    //val crc8readable = FlexParser.crc8readable(bb)
    val crc8readable = (~bb.toIteratorReadable.fold(COMPLETED_CORRECTLY.toByte)((a, b) => (a + b).toByte) + 1) & 0xff
    debug(s"checkSum bb: ${bb.toHexString} is $crc8readable")
    crc8readable
  }


  def sendConfirmation(out: ByteBufAsyncOut, conf: MonoConfirmation)(implicit alloc: ByteBufAllocator): Future[_] = {
    out.write(mkPacket(out.alloc(3)
      .writeByte(COMPLETED_CORRECTLY)
      .writeByte(conf.kvitok << 2 | conf.state & 0x03)
      .writeByte(conf.maker)
    ))
  }

  def sendConfirmationOrFailure(s: Future[_], conn: Connection, ready: Info)(implicit alloc: ByteBufAllocator): Future[_] = {
    s
      .map(data => sendConfirmation(conn.out, MonoConfirmation(ready, COMPLETED_CORRECTLY)))
      .recoverWith({ case e =>
        error("sendConfirmationOfFailure:", e)
        sendConfirmation(conn.out, MonoConfirmation(ready, COMPLETED_INCORRECTLY)).andFinally(_ => conn.close())
      })
  }

  def mkPacket(bb: ByteBuf)(implicit alloc: ByteBufAllocator): ByteBuf = {
    val crc1 = checkSum(bb)
    debug(s"mkPacket bb: ${bb.toHexString}")
    alloc.buffer().writeByte(START_MARKER).writeAndRelease(bb).writeByte(crc1).writeByte(END_MARKER)
  }

}
