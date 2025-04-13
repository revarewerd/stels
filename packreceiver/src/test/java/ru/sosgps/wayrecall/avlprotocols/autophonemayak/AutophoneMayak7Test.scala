package ru.sosgps.wayrecall.avlprotocols.autophonemayak

import java.util.Date

import io.netty.buffer.{ByteBuf, Unpooled}
import org.hamcrest.CoreMatchers._
import org.junit.Assert._
import org.junit.{Ignore, Test}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.testutils.Matchers._
import ru.sosgps.wayrecall.utils.ScalaNettyUtils
import ru.sosgps.wayrecall.utils.ScalaNettyUtils.ByteBufOps
import ru.sosgps.wayrecall.utils.io.Utils
import ru.sosgps.wayrecall.utils.io.Utils._

import scala.collection.JavaConverters.mapAsJavaMapConverter


class AutophoneMayak7Test extends grizzled.slf4j.Logging {


  @Test
  def testAuthorizationFromDocs(): Unit = {
    val packet = asByteBuffer("24" +
      "01" +
      "FF" +
      "71" +
      "41" +
      "0860719020025346" +
      "89701010085279876318" +
      "0000" +
      "C0EBFCF4E02DCCE0FFEA0000000000000000000000000000416C66612D4D6179616B0000000000000000000000000000" +
      "060714" +
      "04D2" +
      "00" +
      "3739313733343834303032000000000000000000000000000000" +
      "050978170515000000000000" +
      "696E7465726E65742E6D74732E727500000000000000000000000000000000006D7473000000000000000000000000006D747300000000000000000000000000" +
      "0101021E040103010103020201033C0101170006060001050100000000000000" +
      "010078000002012C003C02012C003C02012C003C" +
      "00000000000000000000000000000000000000000000"
    )
    debug("packet.readableBytes() = " + packet.readableBytes())
    val (imei, crc) = AutophoneMayak7.parseAuthorization(packet)
    assertThat(packet.readableBytes(), is(0))
    assertThat(imei, is("860719020025346"))

  }

  @Test
  def testSinglePackFromDoc(): Unit = {
    val packet = asByteBuffer(
      "24" +
        "03" +
        "1C" +
        "10030E120723" +
        "033DBD24" +
        "03578584" +
        "0084" +
        "003D" +
        "007E" +
        "0096" +
        "05" +
        "00" +
        "05" +
        "FF" // CRC пакета (в данном примере не является истинным значением)
    )
    val (gPSData, crc) = AutophoneMayak7.parseWorkingPack("123", packet)
    assertThat(packet.readableBytes(), is(0))
    assertThat(gPSData, containsDataOf(new GPSData(null: String,
      "123",
      56.11243438720703,
      54.629608154296875,
      new Date(1394978855000L) /* Sun Mar 16 18:07:35 MSK 2014 */ ,
      61,
      126,
      5,
      null: String,
      null: Date,
      Map("height" -> 150.toShort, "protocol" -> "AutophoneMayak").mapValues(_.asInstanceOf[AnyRef]).asJava))
    )
  }

  @Test
  def testMultiPackFromDoc(): Unit = {
    val packet = asByteBuffer(
      "24" +
        "04" +
        "FF" +
        "19020E062931033DBD1D0357852A00F00017" +
        "19020E062A05033DBD210357853500EB0024" +
        "19020E062A15033DBD230357855500AA002A" +
        "19020E062A25033DBD270357856800AA001A" +
        "19020E062A35033DBD230357857C00AA0006" +
        "19020E062B09033DBD240357855D00AA001F" +
        "19020E062B1B033DBD240357855100AA0016" +
        "19020E062B2B033DBD0D0357856800A90041" +
        "19020E062B3B033DBD0E035784F4007B000D" +
        "19020E062C0F033DBD0B035784F3007B0016" +
        "19020E062C1F033DBD0A035784EA007B001E" +
        "19020E062C2F033DBD1C035784CA007B0010" +
        "19020E062D03033DBD1F035784C4007E0013" +
        "19020E062D14033DBD25035784B4007B0014" +
        "FF" // CRC пакета (в данном примере не является истинным значением)
    )
    val (gPSData, crc) = AutophoneMayak7.parseSeveralPacks("123", packet)
    assertThat(packet.readableBytes(), is(0))
    assertThat(gPSData.length, is(14))
    assertThat(gPSData(0), containsDataOf(new GPSData(null: String,
      "123",
      56.112281799316406,
      54.62959671020508,
      new Date(1393296109000L) /* Tue Feb 25 06:41:49 MSK 2014 */,
      23,
      0,
      0,
      null: String,
      null: Date,
      Map("protocol" -> "AutophoneMayak").mapValues(_.asInstanceOf[AnyRef]).asJava)))
    assertThat(gPSData.last, containsDataOf(new GPSData(null: String,
      "123",
      56.11208724975586,
      54.629608154296875,
      new Date(1393296320000L) /* Tue Feb 25 06:45:20 MSK 2014 */,
      20,
      0,
      0,
      null: String,
      null: Date,
      Map("protocol" -> "AutophoneMayak").mapValues(_.asInstanceOf[AnyRef]).asJava)))
  }

  @Test
  def sysStateFromDoc(): Unit = {
    val packet = asByteBuffer("24" +
      "02" +
      "60" +
      "10030E122F09" +
      "110C0005A021000000" +
      "110C004EC031000000" +
      "50" +
      "00FA000175950018C7" +
      "00FA000175950018CA" +
      "00FA00017596001467" +
      "00FA000175950009EE" +
      "116B" +
      "00132BE4" +
      "1A" +
      "0000" +
      "0110030E121B2800" +
      "0000000000000000000000000000 00"
    )
    debug("packet.readableBytes() = " + packet.readableBytes())
    val crc = AutophoneMayak7.parseStateAndLBS(packet)
    assertThat(packet.readableBytes(), is(0))
    //assertThat(imei, is("860719020025346"))

  }


}
