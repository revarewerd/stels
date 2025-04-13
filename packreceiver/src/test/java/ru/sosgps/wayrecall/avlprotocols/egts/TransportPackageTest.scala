package ru.sosgps.wayrecall.avlprotocols.egts

import java.nio.ByteOrder

import io.netty.buffer.Unpooled
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.utils.ScalaNettyUtils
import ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.io.Utils

/**
 * Created by nickl on 25.01.15.
 */
class TransportPackageTest {


  def testSimmetry(parser: Package, pack: String): Unit = {

    val src = Unpooled.wrappedBuffer(
      Utils.asBytesHex(pack)
    ).order(ByteOrder.LITTLE_ENDIAN)

    val pack1 = parser.read(src)

    val written = pack1.write(Unpooled.buffer())

    val encoded = Utils.toHexString(written," ")
    src.resetReaderIndex()
    Assert.assertEquals(Utils.toHexString(src," "), encoded)
    src.release()
    written.release()
    Assert.assertEquals(0, src.refCnt())
    Assert.assertEquals(0, written.refCnt())

  }

  @Test
  def testAuth(): Unit ={

    testSimmetry(new TransportPackage(), "01 00 00 0b 00 1e 00 00 00 01 ef 17 00 00 00 00 01 01 01 14 00 00 00 00 00 02 33 35 34 33 33 30 30 33 30 39 39 32 39 34 38 20 68")
    testSimmetry(new TransportPackage(), "01 00 00 0b 00 03 00 01 00 00 16 00 00 99 0c de")



  }
  @Test
  def testMessage1(): Unit ={
      testSimmetry(new TransportPackage(),"01 00 00 0b 00 41 00 01 00 01 71 3a 00 00 00 00 " +
        "02 02 10 1a 00 8e 65 89 09 3f dc 71 9e 5c 4a 9c 35 81 00 00 7e 00 00 00 00 00 00" +
        " 00 00 00 00 10 1a 00 43 66 89 09 3f dc 71 9e 5c 4a 9c 35 81 00 00 7e 00 00 00" +
        " 00 00 00 00 00 00 00 6a 94")

      testSimmetry(new TransportPackage(),"01 00 00 0b 00 41 00 01 00 01 71 3a 00 00 00 00" +
        " 02 02 10 1a 00 c8 69 89 09 3f dc 71 9e 5c 4a 9c 35 81 00 00 7e 00 00 00 00 00 00" +
        " 00 00 00 00 10 1a 00 14 69 89 09 3f dc 71 9e 5c 4a 9c 35 81 00 00 7e 00 00 00 00" +
        " 00 00 00 00 00 00 47 3e")


    testSimmetry(new TransportPackage(),
        "01 00 00 0b 00 ba 01 01 00 01 66 b3 01 00 00 00 " +
          " 02 02 10 1a 00 c3 0c 8a 09 b8 22 4a 9e c5 c7 ae " +
          " 35 91 6e 80 04 00 00 00 00 00 00 00 00 00 00 10 " +
          " 1a 00 83 0c 8a 09 d0 44 49 9e 5e b7 ae 35 91 fa " +
          " 00 3e 00 00 00 00 00 00 00 00 00 00 10 1a 00 9e " +
          " 0c 8a 09 16 06 4a 9e 0d dc ae 35 91 46 80 4e 00 " +
          " 00 00 00 00 00 00 00 00 00 10 1a 00 8f 0c 8a 09 " +
          " 65 8c 49 9e 67 f5 ae 35 91 fa 80 64 00 00 00 00 " +
          " 00 00 00 00 00 00 10 1a 00 8a 0c 8a 09 69 67 49 " +
          " 9e c1 ec ae 35 91 aa 00 31 00 00 00 00 00 00 00 " +
          " 00 00 00 10 1a 00 80 0c 8a 09 fc 26 49 9e 7d a2 " +
          " ae 35 91 c8 80 66 00 00 00 00 00 00 00 00 00 00 " +
          " 10 1a 00 8d 0c 8a 09 88 74 49 9e b4 f5 ae 35 91 " +
          " b4 00 22 00 00 00 00 00 00 00 00 00 00 10 1a 00 " +
          " 78 0d 8a 09 67 14 4a 9e c8 3c ae 35 81 00 80 01 " +
          " 00 00 00 00 00 00 00 00 00 00 10 1a 00 79 0c 8a " +
          " 09 fb e2 48 9e bc ab ae 35 91 8c 80 5c 00 00 00 " +
          " 00 00 00 00 00 00 00 10 1a 00 98 0c 8a 09 74 e9 " +
          " 49 9e 00 e5 ae 35 91 a0 80 5a 00 00 00 00 00 00 " +
          " 00 00 00 00 10 1a 00 8e 0c 8a 09 45 7f 49 9e e5 " +
          " f6 ae 35 91 d2 00 0b 00 00 00 00 00 00 00 00 00 " +
          " 00 10 1a 00 82 0c 8a 09 d9 3e 49 9e 04 af ae 35 " +
          " 91 e6 00 2f 00 00 00 00 00 00 00 00 00 00 10 1a" +
          " 00 72 0c 8a 09 ce bc 48 9e ce b0 ae 35 91 6e 80 " +
          " 66 00 00 00 00 00 00 00 00 00 00 10 1a 00 c2 0c " +
          " 8a 09 e9 23 4a 9e a6 cb ae 35 91 3c 80 31 00 00 " +
          " 00 00 00 00 00 00 00 00 10 1a 00 81 0c 8a 09 56 " +
          " 2f 49 9e 47 a4 ae 35 91 c8 00 0a 00 00 00 00 00 " +
          " 00 00 00 00 00 bc 34")


  }

  @Test
  def testPositionSimmetry(): Unit ={

      testSimmetry(new TransportPackage(),
        "01 00 00 0b 00 5e 00 01 00 01 68 57 00 00 00 00 " +
        "02 02 10 1a 00 d2 70 89 09 3f dc 71 9e 5c 4a 9c " +
        "35 81 00 00 7e 00 00 00 00 00 00 00 00 00 00 10 " +
        "1a 00 3b 72 89 09 3f dc 71 9e 5c 4a 9c 35 81 00 " +
        "00 7e 00 00 00 00 00 00 00 00 00 00 10 1a 00 87 " +
        "71 89 09 3f dc 71 9e 5c 4a 9c 35 81 00 00 7e 00 " +
        "00 00 00 00 00 00 00 00 00 50 f1")


  }

  @Test
  def testAppdata(): Unit ={
      testSimmetry(new AppDataFrame,"3a 00 00 00 00 02 02 10 1a 00 8e 65 89 09 3f dc 71 9e 5c 4a 9c 35 81 00 00 7e 00 00 00 00 00 00 00 00 00 00 10 1a 00 43 66 89 09 3f dc 71 9e 5c 4a 9c 35 81 00 00 7e 00 00 00 00 00 00 00 00 00 00")
    }

  @Test
  def testPositionData(): Unit ={
      testSimmetry(new PositionData,"3b 72 89 09 3f dc 71 9e 5c 4a 9c 35 81 00 00 7e 00 00 00 00 00 00 00 00 00 00")
    }

}
