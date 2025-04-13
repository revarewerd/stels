package ru.sosgps.wayrecall.avlprotocols.autophonemayak

import java.nio.charset.Charset
import java.util

import com.google.common.base.Charsets
import io.netty.buffer.ByteBuf
import ru.sosgps.wayrecall.avlprotocols.autophonemayak.AutophoneMayak._
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.io.Utils
import ru.sosgps.wayrecall.utils.java8TLocalDateTimeOps

/**
  * Created by nmitropo on 22.12.2015.
  */
object AutophoneMayak7 extends grizzled.slf4j.Logging {

  def parseAuthorization(pack0: ByteBuf): (String, Byte) = {

    val begin = pack0.readerIndex()

    //    Описание полей:
    //    1. 1 байт: преамбула (символ «$»).
    val preamb = pack0.readByte().toChar
    debug("preamb = " + preamb)
    require(preamb == '$', s"preablula expected to be $$ not $preamb")

    //    2. 1 байт: идентификатор: (1).
    val id = pack0.readUnsignedByte()
    debug("id = " + id)
    //    3. 1 байт: длина сообщения (255).
    val length = pack0.readUnsignedByte()
    debug("length = " + length)
    val pack = pack0.readSlice(length - 3)
    //    4. 1 байт: hardware системы. Старший полубайт – тип системы, младший – версия. В примере: 7.1
    val hardware = pack.readByte()
    debug("hardware = " + hardware)
    //    5. 1 байт: software системы. В примере: А
    val software = pack.readByte()
    debug("software = " + software)
    //    6. 8 байт: IMEI системы. Старший полубайт первого байта – незначащий (равен 0). В примере: 860719020025346
    val imei = AutophoneMayak.readImei(pack)
    debug("imei = " + imei)
    //    7. 10 байт: ICCID симкарты системы. В примере: 89701010085279876318
    val ICCID = AutophoneMayak.readNumericString(pack, 10)
    debug("ICCID = " + ICCID)
    //    8. 2 байта: ID системы. В примере: 0000
    val systemId = pack.readUnsignedShort()
    debug("systemId = " + systemId)
    //    9. 48 байт: название системы (русское + английское, по 24 байта). В примере: Альфа-Маяк и Alfa-Mayak
    val systemName = pack.readBytesAsString(48, charset)
    debug("systemName = " + systemName)
    //    10. 3 байта: дата создания прошивки системы (день, месяц, год). В примере: 06-07-14
    val frmvDay = pack.readUnsignedByte()
    val frmvMonth = pack.readUnsignedByte()
    val frmvYear = pack.readUnsignedByte()
    debug(s"frmvDay, frmvMonth, frmvYear = $frmvDay-$frmvMonth-${frmvYear.toInt.toHexString}")
    //    11. 2 байта: пароль системы. В примере: 1234
    val passwrd = pack.readUnsignedShort()
    debug("passwrd = " + passwrd)
    //    12. 1 байт: аппаратная платформа системы (7-й бит – микроконтроллер, 6-й бит – наличие flash памяти, 5-й бит – GNSS навигационный приемник).
    val hardwarePlatform = pack.readByte()
    debug("hardwarePlatform = " + hardwarePlatform)
    val phones = pack.readBytesAsString(26, charset)
    //    13. 26 байт: номера телефонов (2 номера по 13 байт). В примере: 79173484123 и 79173484567
    debug("phones = " + phones)
    //    14. 12 байт: IP-адреса серверов (2 IP-адреса по 6 байт). В примере: 0.0.0.0.0 и 5.9.120.23.1301
    val ipS = Iterator.continually().map(_ => pack.readUnsignedByte().toInt).take(12).toList
    debug("ipS = " + ipS)
    //    15. 64 байта: точка доступа GPRS (32 байта – название, 16 байт – пользователь, 16 байт – пароль). В примере: internet.mts.ru / mts / mts

    val gprsName = pack.readBytesAsString(32, charset)
    val gprsUser = pack.readBytesAsString(16, charset)
    val gprsPasswd = pack.readBytesAsString(16, charset)

    debug(s"gprs= $gprsName, $gprsUser, $gprsPasswd")

    //    16. 32 байта: установки (setup) системы.
    val settings = Utils.toHexString(pack.readBytes(32), "")
    debug("settings = " + settings)
    //    17. 20 байт: установки типов задач системы (4 типа задач (онлайн по будильнику, дозвон, отправка смс-сообщения, отправка gprs-пакета) по 5 байт (количество попыток (1 байт), время в онлайн к секундах после успешного выполнения задачи (2 байта) и невыполнения задачи (2 байта)).
    pack.skipBytes(20)
    //    18.резерв.
    pack.skipBytes(1)

    //    19. 1 байт: контрольная сумма.

    val crc = pack.readByte()
    debug("crc = " + crc)
    val end = pack.readerIndex()
    val calcedCRC = checkSum(pack, begin, end - 2)

    if (crc != calcedCRC)
      warn(s"crc != calcedCRC : $crc != $calcedCRC")
    (imei, crc)
  }

  def parseStateAndLBS(pack0: ByteBuf) = {

    //    Описание полей:
    //    1. 1 байт: преамбула (символ «$»).
    val preamb = pack0.readByte().toChar
    debug("preamb = " + preamb)
    require(preamb == '$', s"preablula expected to be $$ not $preamb")
    //    2. 1 байт: идентификатор: (2).
    val id = pack0.readUnsignedByte()
    debug("id = " + id)

    //    3. 1 байт: длина сообщения (96).
    val length = pack0.readUnsignedByte()
    debug("length = " + length)
    val pack = pack0.readerIndex(pack0.readerIndex() - 3).readSlice(length)
    val begin = pack.readerIndex()
    pack.skipBytes(3)
    //    4. 6 байт: дата/время системы. В примере: 16-03-14 18:47:09
    val systeime = readDateWithSecond(pack)
    debug("systeime = " + systeime)

    readAlarm(pack)

    //    6. 9 байт: 2-й будильник. Аналогично 1-му. В примере: 17-12:00,14D(G1)
    readAlarm(pack)
    //    7. 1 байт: Уровень GSM сигнала. Передается положительным числом. В примере: -80dB.
    val signalLevel = pack.readUnsignedByte()
    debug("signalLevel = " + signalLevel)

    //    8. 9 байт: LBS1. MCC (2 байта), MNC (2 байта), LAC (2 байта), CID (3 байта). В примере: 250 – 01 – 7595 – 0018C7
    //      9. 9 байт: LBS2. MCC (2 байта), MNC (2 байта), LAC (2 байта), CID (3 байта).
    //    10. 9 байт: LBS3. MCC (2 байта), MNC (2 байта), LAC (2 байта), CID (3 байта).
    //    11. 9 байт: LBS4. MCC (2 байта), MNC (2 байта), LAC (2 байта), CID (3 байта).
    val lbses = (1 to 4).map(i => {
      val MCC = pack.readUnsignedShort()
      val MNC = pack.readUnsignedShort()
      val LAC = pack.readUnsignedShort()
      val CID = pack.readUnsignedMedium()
      (MCC, MNC, LAC, CID)
    })

    debug("lbses = " + lbses)
    //    12. 2 байта: напряжение батареи (в милливольтах). В примере: 4.459в
    val voltage = pack.readUnsignedShort()
    debug("voltage = " + voltage)
    //      13. 4 байта: потребленная энергия в мкА*ч. В примере: 1256420
    val enConsuming = pack.readUnsignedInt()
    debug("enConsuming = " + enConsuming)
    //    14. 1 байт: температура (в градусах, число со знаком). В примере: +26C
    val temperature = pack.readByte()
    debug("temperature = " + temperature)
    //      15. 2 байта: состояние системы при формировании пакета.
    //    0-й бит – состояние SOS-кнопки
    //    15-й бит – «выключенное» состояние маяка (=1)
    //    В примере: на момент формирования пакета SOS–кнопка отпущена
    val state = pack.readShort()
    debug("state = " + state)
    //      16. Поле тревоги (8 байт). При отсутствии тревоги имеет нулевое значение.
    //    2 байта: зона тревоги.
    //    0-й бит – нажата SOS кнопка.
    //      В примере: нажата SOS –кнопка (установлен 0-й бит)
    //    6 байт: дата и время срабатывания тревоги. В примере: 16-03-14 18:27:40
    val sosState = pack.readByte() // по доке тут должен быть short
    debug("sosState = " + sosState.toInt.toHexString)
    val alarmTime = readDateWithSecond(pack)
    debug("sosTime = " + alarmTime)
    //    17. резерв.
    pack.skipBytes(1)
    //    18. 1 байт: контрольная сумма.
    val crc = pack.readByte()
    debug("crc = " + crc)
    val end = pack.readerIndex()

    val calcedCRC = checkSum(pack, begin, end - 1)

    if (crc != calcedCRC)
      warn(s"crc != calcedCRC : $crc != $calcedCRC")

    //    Внимание! Поля 15 и 16 могут иметь «противоположное» значение, т.к. в поле 15 указано текущее состояние, а в поле 16 указано возникшее событие (тревога). Пример: в 16-м поле может быть указана тревога о нажатии кнопки, но в 15-м ее состояние уже может быть НЕ нажатым.
    crc
  }

  def parseWorkingPack(imei: String, pack0: ByteBuf) = {

    //    1. 1 байт: преамбула (символ «$»).
    val preamb = pack0.readByte().toChar
    debug("preamb = " + preamb)
    require(preamb == '$', s"preablula expected to be $$ not $preamb")
    //    2. 1 байт: идентификатор: (3).
    val id = pack0.readUnsignedByte()
    debug("id = " + id)

    //    3. 1 байт: длина сообщения (28).
    val length = pack0.readUnsignedByte()
    debug("length = " + length)
    val pack = pack0.readerIndex(pack0.readerIndex() - 3).readSlice(length + 1)
    val begin = pack.readerIndex()
    pack.skipBytes(3)
    val gps: GPSData = readNavMinimun(imei, pack)
    //    5. 2 байта: курс. В примере: 007E -> 126
    val course = pack.readShort()
    debug("course = " + course)
    gps.course = course
    //    6. 2 байта: высота (в метрах). Число со знаком. В примере: 0096 -> 150м
    val height = pack.readShort()
    debug("height = " + height)
    gps.data.put("height", height.asInstanceOf[AnyRef])
    //      7. 1 байт: общее количество используемых спутников. В примере: 5
    val satellitsNum = pack.readUnsignedByte()
    debug("satellitsNum = " + satellitsNum)
    gps.satelliteNum = satellitsNum.toByte
    //    8. 1 байт: общее количество используемых GNSS спутников. В примере: 0
    val satellitsNumGNSS = pack.readUnsignedByte()
    debug("satellitsNumGNSS = " + satellitsNumGNSS)
    //    9. 1 байт: общее количество используемых GPS спутников. В примере: 5
    val satellitsNumGPS = pack.readUnsignedByte()
    debug("satellitsNumGPS = " + satellitsNumGPS)

    //    10. 1 байт: контрольная сумма.
    val end = pack.readerIndex()
    val calcedCRC = checkSum(pack, begin, end - 1)
    val crc = pack.readUnsignedByte()
    debug("crc = " + crc + "(" + crc.toInt.toHexString + ")")
    if (crc != calcedCRC)
      warn(s"crc != calcedCRC : $crc != $calcedCRC")



    (gps, crc)

  }

  def parseSeveralPacks(imei: String, pack0: ByteBuf) = {
    //    1. 1 байт: преамбула (символ «$»).
    //    2. 1 байт: идентификатор: (4).
    //    3. 1 байт: длина сообщения (3 + (18 x N)). N – количество точек в пакете. Максимальное количество – 14 точек.  В примере: 255
    //    4. 18 x N  полей навигационных минимумумов для N точек. В примере: 14 точек
    //      5. 1 байт: контрольная сумма.

    //    1. 1 байт: преамбула (символ «$»).
    val preamb = pack0.readByte().toChar
    debug("preamb = " + preamb)
    require(preamb == '$', s"preablula expected to be $$ not $preamb")
    //    2. 1 байт: идентификатор: (3).
    val id = pack0.readUnsignedByte()
    debug("id = " + id)

    //    3. 1 байт: длина сообщения (28).
    val length = pack0.readUnsignedByte()
    debug("length = " + length)
    val pack = pack0.readerIndex(pack0.readerIndex() - 3).readSlice(length + 1)
    val begin = pack.readerIndex()
    pack.skipBytes(3)
    val packsNum = length / 18

    debug("packsNum = " + packsNum)

    val gpses = scala.Iterator.continually(readNavMinimun(imei, pack)).take(packsNum).toList
    val crc = pack.readByte()
    debug("crc = " + crc)
    val end = pack.readerIndex()
    val calcedCRC = checkSum(pack, begin, end - 1)
    if (crc != calcedCRC)
      warn(s"crc != calcedCRC : $crc != $calcedCRC")

    (gpses, crc)
  }

  private def readNavMinimun(imei: String, pack: ByteBuf): GPSData = {
    val begin = pack.readerIndex()
    //    4. навигационный минимум: 18 байт:
    //      а) 6 байт: дата/время (навигационные данные). В примере: 16-03-14 18:07:35
    val date = readDateWithSecond(pack)
    debug("date = " + date)
    //    б) 4 байта: широта. Число со знаком (отрицательное – южная). Формат: градусы, минуты (всегда 2 знака), доли минут (всегда 4 знака). В примере: 033DBD24 -> N54 37.7764
    val lat = readCoordinate(pack)
    //    в) 4 байта: долгота. Число со знаком (отрицательное – западная). Формат: градусы, минуты (всегда 2 знака), доли минут (всегда 4 знака). В примере: 03578584 -> E056 06.7460
    val lon = readCoordinate(pack)
    debug(s"lonlat = ($lon, $lat)")
    //    г) 2 байта: HDOP (в сотых долях: 286 -> 2.86). В примере: 0084 -> 1.32
    val hdop = pack.readUnsignedShort() / 10.0
    debug("hdop = " + hdop)
    //    д) 2 байта: скорость (в сотых долях узлов: 6125 -> 61.25). В примере: 003D -> 0.61
    val speed = pack.readUnsignedShort()
    debug("speed = " + speed)

    debug(s"readNavMinimun took ${pack.readerIndex() - begin} bytes")
    val map = new util.HashMap[String, AnyRef]()
    map.put("protocol", "AutophoneMayak")
    val gps = new GPSData(null, imei, lon.toDouble, lat.toDouble, date.get.toDate, speed.toShort, 0, 0, null, null, map)
    gps
  }

  private def readAlarm(pack: ByteBuf): Unit = {
    //    5. 9 байт: 1-й будильник (день (1 байт), часы (1 байт), минуты (1 байт),
    // интервал (в минутах, 2 байта), 4 задачи (4 байта)).
    //      Задачи будильника имеют следующую интерпретацию:
    //    0x00 – отсутствует, 0x10 – F, 0x21 – A (дозвон на 1-й номер), 0x22 – a (дозвон на 2-й номер), 0x31 – S (смс с LBS на 1-й номер), 0x32 – s (смс с LBS на 2-й номер), 0x3A – i (смс с координатами на сервер), 0x41 – G (смс с координатами на 1-й номер), 0x42 – g (смс с координатами на 2-й номер), 0x4A – I (смс с координатами на сервер). Значение периода 0 означает, что установка будильника при срабатывании не изменяется (в результате он будет срабатывать в один и тот же день каждого месяца (если такое значение дня есть в месяце)).
    //      В примере: 17-12:00,1D(S1)

    val day = pack.readByte()
    val hours = pack.readByte()
    val minutes = pack.readByte()
    debug(s"alarm1 = $day-$hours:$minutes ")
    val alarm1Interval = pack.readUnsignedShort()
    debug("alarm1Interval = " + alarm1Interval)
    val alarmMode = pack.readBytes(4)
    debug("alarmMode = " + alarmMode.toHexString)
  }

  val charset: Charset = Charset.forName("cp1251")


}
