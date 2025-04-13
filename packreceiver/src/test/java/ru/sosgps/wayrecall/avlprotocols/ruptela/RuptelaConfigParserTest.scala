package ru.sosgps.wayrecall.avlprotocols.ruptela

import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.utils.io.Utils

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 11.06.13
 * Time: 20:01
 * To change this template use File | Settings | File Templates.
 */
class RuptelaConfigParserTest {


  @Test
  def test(){

    val asStream = this.getClass.getClassLoader
      .getResourceAsStream("ru/sosgps/wayrecall/avlprotocols/ruptela/RupProGlo_WRC_30sec.fp3c")

    val params: Stream[Array[Byte]] = RuptelaConfigParser.readParams(asStream)

    val flatten = RuptelaConfigParser.groupWithLimit(512, params).flatten.toList


//    println(params.toList.map(Utils.toHexString(_," ")))
//    println(flatten.toList.map(Utils.toHexString(_," ")))

    Assert.assertEquals(params.toList, flatten.toList)


  }

}
