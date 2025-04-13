package ru.sosgps.wayrecall.avlprotocols.autophonemayak

import io.netty.channel.nio.NioEventLoopGroup
import org.junit._
import ru.sosgps.wayrecall.packreceiver.DummyPackSaver
import ru.sosgps.wayrecall.packreceiver.netty.{AutophoneMayakServer, NavTelecomNettyServer}
import ru.sosgps.wayrecall.testutils.TalkTestSetup

/**
 * Created by nickl on 01.06.14.
 */
class ServerTest extends grizzled.slf4j.Logging with TalkTestSetup {

  val serverPort: Int = ServerTest.TEST_PORT

  import talk._


  @Test
  def docDataTest() {
    send("10 55 61 03 59 ")
    send("               23 10 31 48 25 72 8F")

    expect("72 65 73 70 11 63 72 63 3D 8F")

    send("12 06 " +
      "00 00" +
      "00 75 01 01 0B 0C 00 08 9C FF FF FF FF FF FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 E7 FF FF 02" +
      "00 76 01 01 0B 0C 00 26 9C FF FF FF FF FF FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 E7 FF FF 4A" +
      "00 76 01 01 0B 0C 01 08 9C FF FF FF FF FF FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 E7 FF FF 04" +
      "00 75 01 01 0B 0C 01 26 9C FF FF FF FF FF FF FF FF FF 00 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 00 00 03 E7 FF FF 80" +
      "00 76 01 01 0B 0C 02 08 9C FF FF FF FF FF FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 E7 FF FF A6" +
      //"00 75 01 01 0B 0C 02 23 1F 56 00 FA 00 01 76 F2 18 C7 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 03 E7 FF FF 96 " +
      "00" +
      "75" +
      "01 01 0B" +
      "0C 02 23" +
      "1F" +
      "56" +
      "00 FA" +
      "00 01" +
      "76 F2" +
      "18 C7" +
      "85" +
      "0C 0B 0B" +
      "0C 20 3A" +
      "03 3D BD 46" +
      "03 57 83 EF" +
      "00 9E" +
      "00" +
      "32" +
      "00 14" +
      "FF FF" +
      "96" +
      "29"
    )

    expect("72 65 73 70 11 63 72 63 3D 29")

    send("11" + // признак рабочего пакета
      "1E" + //интервал передачи gprs-пакетов 30 секунд
      "00 00 00 00 00 00 00 00" + // различные установки маяка (см. выше)
      "01 " + // внешнее питание включено (1) (0-й бит), тревожный вход в нормальном состоянии (0) (1-й бит), тревожная кнопка в нормальном состоянии (0) (2-й бит), маяк в покое (0) (3-й бит), мотивация отправки пакета - истек таймаут (0) (6-й бит), тип gps-данных: реальные (0) (7-й бит)
      "00 00" + // оставшееся время работы канала 0 секунд (выключен)
      "71" + // напряжение питания батареи 5,65 вольт: 0x71 = 113 -> 113 * 0,05 = 5.65в
      "01 01 0B" + // текущая дата RTC маяка: 1 января 2011 года
      "0C 02 03" + // текущее время RTC маяка: 12 часов, 2 минуты, 3 секунды
      "02 01 0B 0C 00 05 A0 53 FF FF FF FF" +
      "02 01 0B 0C 00 05 A0 53 FF FF FF FF" +
      "1F" + // температура: 0x1F = +31C
      "56" + // уровень GSM-сигнала: 0x56 = 86 -> -86dB
      "00 FA" + // MCC: 00 FA = 250
      "00 01" + // MNC: 00 01 -> 001
      "76 F2" + // LAC: 76F2
      "18 C7" + // CID: 18C7
      "85" + // статус GPS-данных: данные действительные: 2 (биты 6-7), количество видимых спутников 5 (биты 0-5)
      "0C 0B 0B" + // GPS день / месяц / год: 12.11.11
      "0C 20 3A" + // GPS час / минута / секунда: 12:32:58
      "03 3D BD 46" + // широта: 54377798 -> N54 37.7798
      "03 57 83 EF" + // долгота: 56067055 -> E056 06.7055
      "00 9E" + // высота: 158м.
      "00" + // скорость: 0 узлов
      "32" + // курс: 0x32 -> 0x32 * 2 = 100град.
      "00 14" + // HDOP: 00 14 -> 20/10 = 2.0
      "FF FF" + // резервные байты
      "45" // CRC пакета (в данном примере не является истинным значением)
    )

    expect("72 65 73 70 11 63 72 63 3D 45")



  }


}

object ServerTest extends grizzled.slf4j.Logging {

  val TEST_PORT = ru.sosgps.wayrecall.testutils.getFreePort

  var server: AutophoneMayakServer = null

  @BeforeClass
  def initserver() {
    server = new AutophoneMayakServer
    //server.timer = new HashedWheelTimer()
    server.setStore(new DummyPackSaver)
    //server.commands = commands
    server.bossPool = new NioEventLoopGroup()
    server.workerPool = server.bossPool


    server.setPort(TEST_PORT)
    server.start()
  }

}


