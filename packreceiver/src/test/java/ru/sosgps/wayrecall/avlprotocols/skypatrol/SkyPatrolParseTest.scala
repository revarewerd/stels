package ru.sosgps.wayrecall.avlprotocols.skypatrol

import org.junit.Test
import ru.sosgps.wayrecall.packreceiver.DummyPackSaver
import ru.sosgps.wayrecall.packreceiver.netty.SkyPatrolMessageReceiver
import ru.sosgps.wayrecall.utils.io.{Utils, RichDataInput}

/**
 * Created by nickl on 09.10.14.
 */
class SkyPatrolParseTest {


  @Test
  def testFromDocs(): Unit ={

    val receiver = new SkyPatrolMessageReceiver(new DummyPackSaver)

    val exampleData = Utils.asBytesHex("00 05 02 10 04 FF FF FF FF 00 00 00 0D 31 31 34 37 37 35 38 33 00 CB 00\n00 00 00 0E 11 07 0C 01 01 84 D0 32 FB 38 41 37 00 00 00 00 16 07 2B 00\n00 17 05 00 32 00 00 00 00 00 00 02 4E 0C 07 11 16 07 2C 10 59 00 05 00\n00 00 00 00 05 00 00 00 00 00 05 00 00 00 00 03 10 02 60 B7 36 3B 63 06\nC1 1A 00 B7 36 37 F2 06 BF 19 B7 36 37 F1 06 B5 0E B7 36 38 B1 06 BB 0B\nB7 36 3B 61 06 B8 0A B7 36 37 F3 06 B7 09 00 00 00 00 00 00 00 00 0C")

    val r = receiver.parseData(new RichDataInput(exampleData))
    println(r)

  }


}
