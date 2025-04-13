package ru.sosgps.wayrecall.core

import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.utils.io.Utils
import java.io.{ByteArrayInputStream, InputStream}
import ru.sosgps.wayrecall.wialonparser.{WialonPackageBlock, WialonPackager, WialonPackage}
import ru.sosgps.wayrecall.wialonparser.WialonParser._
import ru.sosgps.wayrecall.core.GPSDataConversions
import collection.JavaConversions.asScalaBuffer
import java.util.Comparator

/**
 * Created by nickl on 16.12.13.
 */
class WialonEncodingTest {

  @Test
  def decodeAndEncodeTest() {
    val srces = IndexedSeq("74000000333533393736303133343435343835004B0BFB70000000030BBB000000270102706F73696E666F00A027AFDF5D9848403AC7253383DD4B400000000000805A40003601460B0BBB0000001200047077725F657874002B8716D9CE973B400BBB00000011010361766C5F696E707574730000000001",
    "b9 00 00 00 33 35 32 38 34 38 30 32 38 39 36 31 38 38 32 00 52 af 3f 21 00 00 00 03 0b bb 00 00 00 27 01 02 70 6f 73 69 6e 66 6f 00 29 ff ff a4 46 2d 42 40 4c 34 48 c1 53 48 4b 40 00 00 00 00 00 40 69 40 00 15 00 89 0c 0b bb 00 00 00 15 00 03 62 61 74 74 65 72 79 5f 63 68 61 72 67 65 00 00 00 00 01 0b bb 00 00 00 0f 00 03 70 61 72 61 6d 31 37 39 00 00 00 00 00 0b bb 00 00 00 0f 00 03 70 61 72 61 6d 31 38 30 00 00 00 00 00 0b bb 00 00 00 12 00 04 70 77 72 5f 65 78 74 00 e5 d0 22 db f9 de 3b 40 0b bb 00 00 00 11 01 03 61 76 6c 5f 69 6e 70 75 74 73 00 00 00 00 01 ",
    "eb 00 00 00 31 32 38 39 36 30 30 32 38 34 32 36 35 33 00 52 af 3f 72 00 00 00 03 0b bb 00 00 00 27 01 02 70 6f 73 69 6e 66 6f 00 15 83 d1 f6 f9 c2 42 40 3d af c2 77 ac e0 4b 40 cd cc cc cc cc 5c 61 40 00 37 00 46 08 0b bb 00 00 00 0f 00 04 68 64 6f 70 00 33 33 33 33 33 33 f3 3f 0b bb 00 00 00 10 00 03 69 6f 5f 63 61 75 73 65 64 00 00 00 00 08 0b bb 00 00 00 11 00 03 67 73 6d 5f 73 69 67 6e 61 6c 00 00 00 00 10 0b bb 00 00 00 0f 00 03 70 63 62 5f 74 65 6d 70 00 00 00 00 10 0b bb 00 00 00 0c 00 03 70 6f 77 65 72 00 00 00 6b d0 0b bb 00 00 00 0e 00 03 69 6f 5f 32 5f 36 37 00 00 00 01 d4 0b bb 00 00 00 0d 00 03 69 6f 5f 32 5f 30 00 00 00 00 00 0b bb 00 00 00 11 01 03 61 76 6c 5f 69 6e 70 75 74 73 00 00 00 00 08",
    "d1 00 00 00 33 35 32 38 34 38 30 32 36 31 38 38 31 32 34 00 52 af 3f 8e 00 00 00 03 0b bb 00 00 00 27 01 02 70 6f 73 69 6e 66 6f 00 5a 04 10 88 21 e3 42 40 f3 3b 4d 66 bc ed 4b 40 00 00 00 00 00 a0 64 40 00 56 00 84 0c 0b bb 00 00 00 15 00 03 62 61 74 74 65 72 79 5f 63 68 61 72 67 65 00 00 00 00 01 0b bb 00 00 00 0f 00 03 70 61 72 61 6d 31 37 39 00 00 00 00 00 0b bb 00 00 00 0f 00 03 70 61 72 61 6d 31 38 30 00 00 00 00 00 0b bb 00 00 00 12 00 04 70 77 72 5f 65 78 74 00 bc 74 93 18 04 d6 2c 40 0b bb 00 00 00 12 00 04 70 77 72 5f 69 6e 74 00 be 9f 1a 2f dd a4 21 40 0b bb 00 00 00 11 01 03 61 76 6c 5f 69 6e 70 75 74 73 00 00 00 00 01",
    "ad 00 00 00 31 32 38 39 36 30 30 34 33 38 33 37 38 39 00 52 af 41 06 00 00 00 03 0b bb 00 00 00 27 01 02 70 6f 73 69 6e 66 6f 00 94 d0 a7 9f 81 bd 42 40 ac ca be 2b 82 f1 4b 40 00 00 00 00 00 10 68 40 00 2e 00 f6 12 0b bb 00 00 00 0f 00 04 68 64 6f 70 00 66 66 66 66 66 66 e6 3f 0b bb 00 00 00 10 00 03 69 6f 5f 63 61 75 73 65 64 00 00 00 00 08 0b bb 00 00 00 0f 00 03 69 6f 5f 31 5f 31 37 36 00 00 00 00 2d 0b bb 00 00 00 0c 00 03 70 6f 77 65 72 00 00 00 33 44 0b bb 00 00 00 11 01 03 61 76 6c 5f 69 6e 70 75 74 73 00 00 00 00 00",
    "d9 00 00 00 31 33 32 32 36 30 30 35 33 39 34 33 33 39 00 52 af 41 07 00 00 00 03 0b bb 00 00 00 27 01 02 70 6f 73 69 6e 66 6f 00 1e 9e 6f 55 23 66 43 40 53 62 32 b0 e9 ec 4b 40 9a 99 99 99 99 39 60 40 00 6e 01 0b 08 0b bb 00 00 00 0f 00 04 68 64 6f 70 00 33 33 33 33 33 33 f3 3f 0b bb 00 00 00 10 00 03 69 6f 5f 63 61 75 73 65 64 00 00 00 00 08 0b bb 00 00 00 11 00 03 6d 6f 64 65 6d 5f 74 65 6d 70 00 00 00 00 09 0b bb 00 00 00 11 00 03 67 73 6d 5f 73 69 67 6e 61 6c 00 00 00 00 1b 0b bb 00 00 00 0c 00 03 70 6f 77 65 72 00 00 00 36 09 0b bb 00 00 00 0d 00 03 69 6f 5f 32 5f 30 00 00 00 00 00 0b bb 00 00 00 11 01 03 61 76 6c 5f 69 6e 70 75 74 73 00 00 00 00 08"
    )

    for(src <- srces) {

    val bytes: Array[Byte] = Utils.asBytesHex(src)
//    Assert.assertTrue(src.equalsIgnoreCase(Utils.toHexString(bytes, "")))
    val is: InputStream = new ByteArrayInputStream(bytes)
    val pack: WialonPackage = parsePackage(is)
    println("pack="+pack)
    println()
    val binary = WialonPackager.wlnPackToBinary(pack)

//    Assert.assertArrayEquals(bytes,binary)

    val gps = GPSDataConversions.fromWialonPackage("uid", pack)
    println("gps="+gps)
    println()
    val pack2 = GPSDataConversions.toWialonPackage(gps)

    //pack2.getCoordinatesBlock.height =  pack.getCoordinatesBlock.height

    val blocksOrder =  pack.blocks.map(_.name).zipWithIndex.toMap

     java.util.Collections.sort(pack2.blocks, new Comparator[WialonPackageBlock]{
       def compare(o1: WialonPackageBlock, o2: WialonPackageBlock) = blocksOrder(o1.name) - blocksOrder(o2.name)
     })

    for( (a,b) <- pack2.blocks zip pack.blocks)
    {
      a.hidden = b.hidden
    }

    println("pack2="+pack2)
    println()
    val binary2 = WialonPackager.wlnPackToBinary(pack2)
    println(Utils.toHexString(bytes," "))
    println(Utils.toHexString(binary2," "))
    println("gps2="+GPSDataConversions.fromWialonPackage("uid", pack2))

    Assert.assertArrayEquals(bytes,binary2)

//    println(pack.blocks)
//    println(parsePackage(new ByteArrayInputStream(binary)).blocks)
//
//    println("src=")
//    println(Utils.toHexString(bytes," "))
//    println("fff=")
//    println("45 00 00 00 33 35 33 39 37 36 30 31 33 34 34 35 34 38 35 00 4b 0b fb 70 00 00 00 00 0b bb 00 00 00 27 01 02 70 6f 73 69 6e 66 6f 00 a0 27 af df 5d 98 48 40 3a c7 25 33 83 dd 4b 40 00 00 00 00 00 80 5a 40 00 36 01 46 0b")
//    println("res=")
//    println(Utils.toHexString(binary," "))
//    Assert.assertArrayEquals(bytes,binary);


    }
  }

}
