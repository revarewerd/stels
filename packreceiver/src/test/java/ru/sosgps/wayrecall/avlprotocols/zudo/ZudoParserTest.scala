package ru.sosgps.wayrecall.avlprotocols.zudo

import java.nio.ByteOrder

import io.netty.buffer.{ByteBuf, Unpooled}
import org.scalatest.FunSpec
import org.scalatest.Matchers._
import org.scalatest.AppendedClues._
import ru.sosgps.wayrecall.avlprotocols.zudo.ZudoParser.AccumulatingCFloat
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.io.Utils

/**
  * Created by nmitropo on 6.10.2016.
  */
class ZudoParserTest extends FunSpec {


  describe("Zudo parser") {
    it("parse command") {

      //      ZudoParser.parse(Utils.asByteBuffer("32 05 49 00 ef bf bd ef bf bd 08 0f ef bf bd 2a 67 0f ef bf bd ef bf bd ef bf bd 12 43 28 04 07 7c 02 00 ef bf bd 08 00 ef bf bd 2c ef bf bd 00 00 00 00 02 ef bf bd ef bf bd 74 1a ef bf bd 00 00 00 00 00 00 00 00 00 00 ef bf bd 0f ef bf bd 0f ef bf bd ef bf bd ef bf bd 2a 49 00"
      //      ))

      val s = "32 05 01 00 80 ff 09 0f de 64 01 0f " +
        "ca 33 21 12 00 fe a0 07 7d 00 00 99 04 6d e2 3e " +
        "34 00 00 00 00 01 97 d1 aa 14 f0 00 00 00 00 00 " +
        "00 00 00 00 00 cf 0f df 0f df a4 9d 36 4a ff 09 " +
        "a5 8e b0 1f bc d0 00 00 98 04 6e e2 3e 34 00 00 " +
        "00 00 01 97 d1 ab 14 f0 00 00 00 00 00 00 00 00 " +
        "00 00 cf 0f df 0f df a4 9d 36 3c ff 09 a7 25 ad " +
        "06 ba f0 00 00 8f 04 6b e2 3e 33 00 00 00 00 01 " +
        "97 d1 ab 14 f0 00 00 00 00 00 00 00 00 00 00 cf " +
        "00"
      println("bytes = " + s)


      //scanForCords(s)


      val r = ZudoParser.parse(Utils.asByteBuffer(s))
      println(r)
      // ff 09 0f de 64 01 0f ca 33 21 12 00 fe a0 07 7d 00 00 99 04 6d   e2 3e 34 00 00 00 00 01 97 d1 aa 14 f0 00 00 00 00 00 00 00 00 00 00 cf 0f df 0f df a4 9d 36 4a
      // ff 09                         a5 8e b0 1f bc d0 00 00 98 04 6e   e2 3e 34 00 00 00 00 01 97 d1 ab 14 f0 00 00 00 00 00 00 00 00 00 00 cf 0f df 0f df a4 9d 36 3c
      // ff 09                         a7 25 ad 06 ba f0 00 00 8f 04 6b   e2 3e 33 00 00 00 00 01 97 d1 ab 14 f0 00 00 00 00 00 00 00 00 00 00 cf 00

    }

    it("parse command2") {

      val s = "32 05 01 00 ef bf bd ef bf bd 09 0f ef bf bd 61 22 0f ef bf bd 13 ef bf bd 12 00 ef bf bd 00 07 7d 00 00 ef bf bd 04 31"
      scanForCords(s)
      //println("bytes = " + s)
      val r = ZudoParser.parse(Utils.asByteBuffer(s))

      println(r)


    }

    it("scanForCords") {

      scanForCords("50 4f 53 54 20 6d 6f 6e 2f 67 70 72 73 2e 64 6c " +
        " 6c 3f 64 61 74 61 20 48 54 54 50 2f 31 2e 31 0d " +
        " 0a 48 6f 73 74 3a 20 39 31 2e 32 33 30 2e 32 31 " +
        " 35 2e 31 32 0d 0a 43 6f 6e 74 65 6e 74 2d 54 79 " +
        " 70 65 3a 20 61 70 70 6c 69 63 61 74 69 6f 6e 2f " +
        " 62 69 6e 61 72 79 0d 0a 43 6f 6e 74 65 6e 74 2d " +
        " 4c 65 6e 67 74 68 3a 20 38 37 0d 0a 0d 0a" +
        "69 64 3d 38 39 37 30 31 30 31 30 30 36 35 30 34 " +
        " 35 38 38 35 37 32 38 26 62 69 6e 3d 32 05 0e 00 " +
        " 81 ff 08 0f de 45 12 0f ca 30 4b 11 ff 47 e0 07 " +
        " 87 02 00 ff 08 00 00 2c 98 00 00 00 00 02 32 41 " +
        " 22 1a 92 00 00 00 00 00 00 00 00 00 00 cf 0f fe " +
        " 0f fe a4 b6 2b 30 00")

    }

  }

  describe("Zudo float decoder") {
    it("should accumulated decode") {
      val acc = new AccumulatingCFloat
      acc.readNext(Utils.asByteBuffer("0f de 64 01")).toDouble.toDegrees shouldEqual 55.855005749181494 +- 0.00001
      acc.readNext(Utils.asByteBuffer("a5 8e b0 1f")).toDouble.toDegrees shouldEqual 55.88782480814341 +- 0.0001f
      acc.readNext(Utils.asByteBuffer("ad 06 ba f0")).toDouble.toDegrees shouldEqual 55.89435446898994 +- 0.0001f

    }
  }


  def scanForCords(s: String): Unit = {
    def readSeveralWays(bb: ByteBuf): Unit = {
      val hexString = bb.toHexString
      bb.markReaderIndex()
      val le = Unpooled.buffer(4).writeInt(bb.order(ByteOrder.LITTLE_ENDIAN).readInt() << 2).readFloat().toDouble.toDegrees
      if (le > 30 && le < 60) {
        println(hexString)
        println("le=" + le)
      }
      bb.resetReaderIndex()
      val be = Unpooled.buffer(4).writeInt(bb.order(ByteOrder.BIG_ENDIAN).readInt() << 2).readFloat().toDouble.toDegrees
      if (be > 30 && be < 60) {
        println(hexString)
        println("be=" + be)
      }
      bb.resetReaderIndex()

    }

    val bb = Utils.asByteBuffer(s)

    for (i <- 0 to bb.readableBytes() - 4) {
      bb.readerIndex(i)
      readSeveralWays(bb.readSlice(4))
      println("--------------")
    }
  }
}
