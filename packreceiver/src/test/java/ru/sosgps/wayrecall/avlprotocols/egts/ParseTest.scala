package ru.sosgps.wayrecall.avlprotocols.egts

import java.nio.ByteOrder

import com.google.common.base.Charsets
import io.netty.buffer.{ByteBuf, Unpooled}

import ru.sosgps.wayrecall.avlprotocols.navtelecom.FlexParser
import ru.sosgps.wayrecall.utils.{ScalaNettyUtils, iff}
import ScalaNettyUtils.ByteBufOps

import ru.sosgps.wayrecall.utils.iff
import ru.sosgps.wayrecall.utils.io.{CRC16CCITT, CRC16, Utils}

/**
 * Created by nickl on 22.01.15.
 */
object ParseTest extends grizzled.slf4j.Logging {

  def main(args: Array[String]) {

    val src = Unpooled.wrappedBuffer(
      Utils.asBytesHex("01 00 00 0b 00 1e 00 00 00 01 ef 17 00 00 00 00 01 01 01 14 00 00 00 00 00 02 33 35 34 33 33 30 30 33 30 39 39 32 39 34 38 20 68")
    ).order(ByteOrder.LITTLE_ENDIAN)

    val pack1 = new TransportPackage().read(src)


    //parseFrameData(pack1)

    val src1 = Unpooled.wrappedBuffer(
      Utils.asBytesHex("01 00 00 0b 00 03 00 01 00 00 16 00 00 99 0c de")
    ).order(ByteOrder.LITTLE_ENDIAN)

    val pack2 = new TransportPackage().read(src1)
    //parseFrameData(pack2)


  }


//  def parseFrameData(transportPackage: TransportPackage) {
//    if (transportPackage.packetType == 1)
//     new AppDataFrame().read(transportPackage.frameData)
//    else if (transportPackage.packetType == 0)
//     new ResponseFrame().read(transportPackage.frameData)
//  }






}


