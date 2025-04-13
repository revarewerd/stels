package ru.sosgps.wayrecall.data

import java.io.File

import org.junit.{Assert, Assume, Test}
import ru.sosgps.wayrecall.data.sleepers._
import java.net.URL

import org.apache.commons.compress.archivers.zip.ZipFile
import ru.sosgps.wayrecall.data.sleepers.LBS
import ru.sosgps.wayrecall.data.MatchResultTemplateWithoutBats
import ru.sosgps.wayrecall.utils.io.DboReader

import scala.Some
import scala.collection.mutable.ListBuffer
import scala.util.Success


/**
  * Created by nickl on 10.03.14.
  */
class SleeperParserTest {


  @Test
  def testPromaSat() {

    Assert.assertEquals(None, SleeperParser.parseReport("Proma Sat 1000 V3.28.RIC UNO:4938").toOption)
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(Some(Moving), None, None, Option(LBS(250, 1, 562, 2774)))),
      SleeperParser.parseReport("Proma Sat 1000 V3.28.RIC LTM 13-11-11 14:23:03 MCC=250 MNC=1 LAC=232 CID=AD6 Alarm: Moving GSM:-72dBm T=31.5C Bat=3.7V-32.9% Ex_Batt=0.0V ")
    )
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, Some((37.44691, 55.726344)), Some(new URL( """http://m.maps.yandex.ru/?ll=37.446910,55.726344&pt=37.446910,55.726344&z=12""")), None)),
      SleeperParser.parseReport( """Proma Sat 1000 V3.28.RIC LTM 13-11-11 18:30:28 http://m.maps.yandex.ru/?ll=37.446910,55.726344&pt=37.446910,55.726344&z=12 GSM:-94dBm T=33.5C Bat=3.7V-31.9% Ex_Batt=0.0V """)
    )

  }

  @Test
  def testPromaSat2() {

    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, None)),
      SleeperParser.parseReport( """PROMA SAT 777 V2.05S 10 day SMS test! GSM:-58dBm ATM0:12:00;U1 ATM1:OFF Bat=3.05V-100% T=15.5C #41SMS """)
    )

    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 561, 27226)))),
      SleeperParser.parseReport( """PROMA SAT 777 V2.05S GMT04:00 00-00-00 00:00 No GPS data! MCC:250 MNC:1 LAC:0231 CID:6A5A GSM:-72dBm ATM0:12:00;U1 ATM1:OFF Bat=2.90V-80% T=41.5C #28SMS""")
    )

  }


  @Test
  def test2() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 562, 2774)))),
      SleeperParser.parseReport("Proma Sat 1000 V3.36.RIC LTM 14-03-12 18:24:41 MCC=250 MNC=1 LAC=232 CID=AD6 GSM:-70dBm T=28.5C Bat=4.2V-100.0% Ex_Batt=11.4V")
    )
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 562, 3355)))),
      SleeperParser.parseReport("Proma Sat 1000 V3.36.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=232 CID=D1B Alarm: Low Inter_Batt GSM:-72dBm T=40.5C Bat=3.9V-58.6% Ex_Batt=11.5V")
    )
  }


  @Test
  def testPhoto() {


    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(Some(LightInput), None, None, None)),
      SleeperParser.parseReport("Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 Alarm: Light-Input GSM:-56dBm T=18.0C Bat=5.8V-89.1% M=1 #34")
    )
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6380, 16205)))),
      SleeperParser.parseReport( """Proma Sat 78 V1.19.RC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=18EC,CID=3F4D,-81dBm LAC1=FFFF,CID=FFFF,-63dBm LAC2=FFFF,CID=FFFF,-63dBm LAC3=FFFF,CID=FFFF,-63dBm GSM:-70dBm T=7.5C Bat=5.7V-83.8% M=1 #37""")
    )

    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, Some((124.210466, 54.007436)), None, None)),
      SleeperParser.parseReport( """Proma Sat 78 V1.19.RC LTM 14-04-01 17:58:05 GPS 9.60/4/56 N54.007436 E124.210466 SPD:0km/h 0 GSM:-72dBm T=-5.5C Bat=5.9V-94.7% M=1 #7""")
    )

    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, Some((37.635961, 55.871421)), None, None)),
      SleeperParser.parseReport( """Proma Sat 78 V1.21T.RC LTM 14-05-28 03:40:03 GPS 2.40/5/440 N55.871421 E37.635961 SPD:0km/h 0 GSM:-58dBm T=26.0C Bat=6.1V-100.0% M=1 #12""")
    )

    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(lbs = Some(LBS(250, 1, 6300, 12145)), lonlat = None, alarm = None, positionURL = None)),
      SleeperParser.parseReport( """Proma Sat 777 V2.11.RCU UTC 14-05-14 18:06:37 MCC=250 MNC=1 LAC=189C,CID=2F71,-89dBm LAC1=189F,CID=CC9,-86dBm LAC2=189C,CID=FFFF,-87dBm LAC3=189F,CID=4045,-90dBm GSM:-72dBm T=21.5C Bat=6.0V-100.0%,I M=SNP #57""")
    )

    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 1605, 50265)))),
      SleeperParser.parseReport( """Proma Sat 78 V1.21T.RC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=645,CID=C459,-78dBm GSM:-70dBm T=38.0C Bat=5.9V-95.0% M=1 #8""")
    )

  }

  @Test
  def testLonLat() {

    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 574, 60485)))),
      SleeperParser.parseReport( """Colubris V2.05S GMT04:00 03-24-14 15:11 No GPS data! MCC:250 MNC:1 LAC:023E CID:EC45 GSM:-94dBm FID:003H;G Bat=2.76V-60% T=20.5C #5SMS""")
    )


    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, Some((37.79783833333333, 55.708578333333335)), None, None)),
      SleeperParser.parseReport( """Colubris V2.05S GMT04:00 03-05-14 11:39 GPS 4/109 N55 42.5147 E37 47.8703 V=0km/h 0 GSM:-58dBm ATM0:12:00;U1 ATM1:OFF Bat=2.83V-70% T=9.5C #10SMS""")
    )


  }

  @Test
  def testProma1(): Unit = {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(Some(Power), None, None, Some(LBS(250,1,552,21262)))),
      SleeperParser.parseReport( """Proma Sat 1000 V3.28.RIC LTM 00-00-00 00:00:00 MCC=250 MNC=1 LAC=228 CID=530E Alarm: Low Inter_Batt Low Ex_Batt GSM:-94dBm T=42.5C Bat=4.2V-100.0% Ex_Batt=11.3V""")
    )
  }

  @Test
  def testProma2(): Unit = {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6341, 52596)))),
      SleeperParser.parseReport( """Proma Sat 1000 V3.28.RIC LTM 15-05-31 10:12:06 MCC=250 MNC=1 LAC=18C5 CID=CD74 Alarm: Low Inter§Batt GSM:-70dBm T=23.0C Bat=3.6V-17.6% Ex§Batt=0.0V""")
    )
  }


  @Test
  def testColibrus1(): Unit = {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, None)),
      SleeperParser.parseReport( """Colubris V203 FID:024H;L  Bat=307V-100% T=330C #3SMS """)
    )
  }

  @Test
  def testAutoPhone1() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, Some((37.53311, 55.68071)), Some(new URL("http://m.maps.yandex.ru/?l=maps&ll=037.533110,55.680710&pt=037.533110,55.680710&z=13")), None)),
      SleeperParser.parseReport( """AutoFon E-Mayak v5.6i 29-10-2012 12:06:09 Sat: 5 at 155s. E037.533110 N55.680710 http://m.maps.yandex.ru/?l=maps&ll=037.533110,55.680710&pt=037.533110,55.680710&z=13  Speed:0km/h Accur:16m T1: 30-10-2012 12:00, 01D,F T2: 05-11-2012 12:00, 07D,G Sensor=0/1 (off) Bat:5.95v(100%) T:+38C Mode:sleep sms#39""")
    )
  }

  @Test
  def testAutoPhone2() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 700, 53414)))),
      SleeperParser.parseReport( """AutoFon E-Mayak v5.6i Sat: 0 at 60s. GSM -90dB MCC: 250 MNC: 001 LAC: 02BC CID: D0A6 Sensor=0/1 (off) Bat:5.87v(100%) T:+21C sms#11""")
    )
  }

  @Test
  def testAutoPhone3() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6101, 13635)))),
      SleeperParser.parseReport( """AutoF E-5.6i Sat: 0 at 120s. GSM -64dB MCC: 250 MNC: 001 LAC: 17D5 CID: 3543 Sensor=0/1 (off) Bat:5.80v(100%) T:+20C sms#4""")
    )
  }

  @Test
  def testAutoPhone4() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 802, 20885)))),
      SleeperParser.parseReport( """AutoFon SE-Mayak v6.2d Sat: 0 at 120s. GSM -59dB MCC: 250 MNC: 001 LAC: 0322 CID: 5195 Sensor=0/1 (off) Bat:6.06v(100%) T:+24C sms#1""")
    )
  }

  @Test
  def testSEMayak() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6302, 53596)))),
      SleeperParser.parseReport( """SE-Mayak v6.3n 23-01-2012 12:03:37 GSM -83dB MCC: 250 MNC: 001 LAC: 189E 189E 184D 189E CID: 00D15C 00D15B 00F4C5 00AA03 Bat:5.90v(93%) T:+11C sms#8""")
    )
  }

  @Test
  def testSEMayak2() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, Some(37.435926, 55.852131), None, None)),
      SleeperParser.parseReport( """SE-Mayak v6.3n Sat: 6 at 44s. E037.435926 N55.852131 Speed:0km/h Altitude:186m Accur:17m Bat:5.85v(86%) T:+14C sms#26""")
    )
  }

  @Test
  def testAutoPhone5() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, Some(37.539583, 55.774896), None, None)),
      SleeperParser.parseReport( """AutoFon SE-Mayak v6.2d Sat: 7 at 44s. E037.539583 N55.774896 Speed:0km/h Altitude:36m Accur:10m Bat:5.80v(87%) T:+15C sms#16""")
    )
  }

  @Test
  def testAutoPhone6() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6405, 51366)))),
      SleeperParser.parseReport( """AutoFon SE-Mayak v6.2i Sat: 0 at 120s. GSM -59dB MCC: 250 MNC: 001 LAC: 1905 CID: C8A6 Sensor=0/1 (off) Bat:5.70v(54%) T:-4C sms#3""")
    )
  }

  @Test
  def testAutoPhone7() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, Some((37.799698, 55.810671)), None, None)),
      SleeperParser.parseReport( """AutoFon Mayak SE-6.2d Sat: 6 at 26s. E037.799698 N55.810671 Speed:0km/h Altitude:158m Accur:16m Bat:5.88v(97%) T:+5C sms#22""")
    )
  }


  @Test
  def testAutoPhone8() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6221, 5876)))),
      SleeperParser.parseReport( """AutoFon SE-Mayak v6.3k Sat: 0 at 120s. GSM -69dB MCC: 250 MNC: 001 LAC: 184D CID: 16F4 Sensor=0/1 (off) Bat:5.72v(69%) T:+1C sms#3""")
    )
  }

  @Test
  def testAutoPhone9() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 717, 62586)))),
      SleeperParser.parseReport( """AutoFon SE-Mayak v6.3n 09-11-2014 12:03:32 GSM -85dB MCC: 250 MNC: 001 LAC: 02CD 02CD 02CD 18D8 CID: 00F47A 00F479 00F478 00EFAA Bat:6.11v(100%) T:+24C sms#2""")
    )
  }

  @Test
  def testAutoPhone10() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6404, 52956)))),
      SleeperParser.parseReport( """AGPS Mayak v5.6r Sat: 0 at 120s. GSM -67dB MCC: 250 MNC: 001 LAC: 1904 CID: CEDC Sensor=0/1 (off) Bat:5.69v(52%) T:+10C sms#4""")
    )
  }

  @Test
  def testAutoPhone11() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 717, 62586)))),
      SleeperParser.parseReport( """AutoFon SE-Mayak v6.3n 09-11-2014 12:03:32 GSM -85dB MCC: 250 MNC: 001 LAC: 02CD 02CD 02CD 18D8 CID: 00F47A 00F479 00F478 00EFAA Bat:6.11v(100%) T:+24C sms#2""")
    )
  }

  @Test
  def testAutoPhone12() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 818, 13020)))),
      SleeperParser.parseReport( """AutoFon SE-Mayak v6.2n 09-01-2012 12:03:36 GSM -70dB MCC: 250 MNC: 001 LAC: 0332 0332 17D5 0321 CID: 0032DC 0032C8 00129D 0053C4 Bat:6.11v(100%) T:+28C sms#5""")
    )
  }

  @Test
  def testAutoPhone13() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6110, 16605)))),
      SleeperParser.parseReport( """AutoFon S-Mayak v5.6m 06-01-2014 12:03:44 GSM -98dB MCC: 250 MNC: 001 LAC: 17DE 17DE 17DE .... CID: 40DD A63B A63C .... Bat:5.90v(100%) T:+27C sms#1""")
    )
  }

  @Test
  def testAutoPhone14() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6101, 13635)))),
      SleeperParser.parseReport( """AutoF E-5.6i 06-10-2014 12:03:39 GSM -85dB MCC: 250 MNC: 001 LAC: 17D5 17D5 0321 0332 CID: 3543 3540 53C5 32D2 Bat:6.16v(100%) T:+20C sms#43""")
    )
  }

  @Test
  def testAutoPhone15() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, Some((37.83314166666667, 55.751353333333334)), None, None)),
      SleeperParser.parseReport( """AutoFon E-Mayak v3.1d 03-10-2012 12:03 GPS satellites: 3 at 60s. N55 45.0812 E037 49.9885 Speed: 0km/h T1: 04-10-2012 12:00, 01D,S T2: 07-10-2012 12:00, 07D,G Battery: 5.87v (100%) Temp.: +20C sms# 52 Mode: sleep""")
    )
  }


  @Test
  def testAutoPhone16() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 574, 15896)))),
      SleeperParser.parseReport("""AutoFon E-Mayak v3.1d 06-10-2012 12:02 GSM -51dB LBS: MCC: 250 MNC: 001 LAC: 023E 023E 023E 024E CID: 3E18 3E15 3E13 2EF5 T1: 07-10-2012 12:00, 01D,S T2: 07-10-2012 12:00, 07D,G Battery: 5.98v (100%) Temp.: +23C sms# 55 Mode: sleep""")
    )
  }

  @Test
  def testAutoPhone17() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 592, 43535)))),
      SleeperParser.parseReport( """AutoFon E-Mayak v3.1d 25-11-2012 12:03 GPS satellites: 0 at 60s. T1: 26-11-2012 12:00, 01D,S T2: 02-12-2012 12:00, 07D,G MCC: 250 MNC: 001 LAC: 0250 CID: AA0F Battery: 5.82v (100%) Temp.: +15C sms# 98 Mode: sleep""")
    )
  }

  @Test
  def testAutoPhone18() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6300, 41484)))),
      SleeperParser.parseReport( """AutoFon E-Mayak v3.1d 23-08-2012 12:02 GSM -49dB LBS: MCC: 250 MNC: 001 LAC: 189C 189C 189C 189C CID: A20C A209 40BC 0D38 T1: 24-08-2012 12:00, 01D,S T2,G Battery: 5.46v (70%) Temp.: +22C sms# 59 Mode: sleep""")
    )
  }

  @Test
  def testAutoPhone19() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, Some((38.05911166666667, 56.0135)), None, None)),
      SleeperParser.parseReport( """AutoFon E-Mayak v3.1d 01-04-2012 12:03 GPS satellites: 4 at 29s. N56 00.8100 E038 03.5467 Speed: 0km/h T1: 02-04-2012 12:00, 01D,F T2: 08-04-2012 12:00,: 28-10-2012 12:00, 07D,G Battery: 6.00v (100%) Temp.: +24C sms# 18 Mode: sleep""")
    )
  }

  @Test
  def testAutoPhone20() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 801, 52084)))),
      SleeperParser.parseReport( """AutoFon E-Mayak v3.1d 17-09-2012 12:02 GSM -67dB LBS: MCC: 250 MNC: 001 LAC: 0321 0321 0321 0321 CID: CB74 CB75 3196 68D3 T1: 18-09-2012 12:00, 01D,S T2 07D,G Battery: 5.74v (100%) Temp.: +23C sms# 67 Mode: sleep""")
    )
  }

  @Test
  def testAutoPhone21() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 1601, 16076)))),
      SleeperParser.parseReport( """AutoFon E-Mayak v3.1d 03-10-2014 12:02 GSM -79dB LBS: MCC: 250 MNC: 001 LAC: 0641 1903 1903 0641 CID: 3ECC 046E 046B A201 T1: 04-10-2014 12:00, 01D,S T2: 6EEA Battery: 5.98v (100%) Temp.: +26C sms# 132 Mode: sleep""")
    )
  }

  @Test
  def testAlphaMayak1(): Unit = {
    val message = "Alfa-Mayak v7.1E\nTime>02-02-19 12:05:30\nGSM:-95dB(min)\nMCC:250 MNC:001\nLAC:035C 02BD 02BD 02BD\nCID:00DD08 001617 001613 00E9D0\nAl1>02-14:00,1D(S1)\nAl2>03-22:00,7D(S1)\nBat:4.75v(norm) T:+4C\nSMS:5(5)"
    val expected = MatchResultTemplate(None, None, None, Some(LBS(250,1, 860, 56584)),Some("4.75v"), Some("norm"))
    val res = SleeperParser.parseReport(message)
    Assert.assertEquals(Success(expected), res)
  }

  @Test
  def testAlphaMayak2(): Unit = {
    val message = "Alfa-Mayak v7.1E\nTime>02-02-19 12:04:07\nCHECK IT!:\nAl1>02-14:00,1D(S1)\nAl2>03-22:00,7D(S1)\nBat:4.68v(norm) T:+2C\nSMS:4(4)"
    val expected = MatchResultTemplate(None, None, None, None,Some("4.68v"), Some("norm"))
    val res = SleeperParser.parseReport(message)
    Assert.assertEquals(Success(expected), res)
    //    Assert.assertEquals(Success(expected), AutoPhoneSleeperParserCombinator.apply(message))
  }

  @Test
  def testAlphaMayak3(): Unit = {
    val message =  "Alfa-Mayak v7.1E\nTime>04-02-19 14:00:36\nGSM:-85dB(norm)\nMCC:250 MNC:001\nLAC:02C8 .... .... ....\nCID:00ADBA ...... ...... ......\nAl1>05-14:00,1D(S1)\nAl2>10-22:00,7D(S1)\nBat:5.16v(max) T:+30C\nSMS:9(13)"
    val expected = MatchResultTemplate(None, None, None, Some(LBS(250,1, 712, 44474)),Some("5.16v"), Some("max"))
    val res = SleeperParser.parseReport(message)
    Assert.assertEquals(Success(expected), res)

    //    Assert.assertEquals(Success(expected), AutoPhoneSleeperParserCombinator.apply(message))
  }

  @Test
  def testAlphaMayak4(): Unit = {
    val message = "Alfa-Mayak v7.1E\nTime>07-02-19 14:00:28\nGSM:-63dB(max)\nMCC:250 MNC:001\nLAC:18D3 18D3 18D3 18D3\nCID:00D740 00D741 00D73E 00D742\nAl1>08-14:00,1D(S1)\nAl2>10-22:00,7D(S1)\nBat:5.25v(max) T:+36C\nSMS:12(19)"
    val expected = MatchResultTemplate(None, None, None, Some(LBS(250,1, 6355, 55104)),Some("5.25v"), Some("max"))
    val res = SleeperParser.parseReport(message)
    Assert.assertEquals(Success(expected), res)

    //    Assert.assertEquals(Success(expected), AutoPhoneSleeperParserCombinator.apply(message))
  }


  @Test
  def testAutoPhoneRus() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250,1,672,20686)))),
      SleeperParser.parseReport( "Автофон Микро-Маяк v.9.1a 12.08.2016 09:00 GSM:-89 Db MCC:250 MNC:1 LAC:2A0 CID:50CE Sms# 72")
    )
  }

  @Test
  def testAutoPhoneRus2() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250,1,674,3714)))),
      SleeperParser.parseReport( """Автофон Микро-Маяк v.9.1a 04.08.2016 16:19 GSM:-59 Db MCC:250 MNC:1 LAC:2A2 CID:E82 Координаты GPS недоступны. Sms# 59""")
    )
  }

  @Test
  def testAutoPhoneRus3() {
    Assert.assertEquals(
      Success(MatchResultTemplate(None, None, None, Some(LBS(250, 1, 671, 15955)), Some("5.67в"), Some("96%"))),
      SleeperParser.parseReport( """АвтоФон E-Маяк v5.6i 09-12-2016 12:02:56 GSM -93dB MCC: 250 MNC: 001 LAC: 029F 029F 029F 029F CID: 3E53 10BC 3E50 3E52 Бат:5.67в(96%) T:+2C смс#23""")
    )
  }

  @Test
  def testAutoPhoneRus4() {
    Assert.assertEquals(
      Success(MatchResultTemplate(None, None, None, Some(LBS(250, 1, 670, 8666)), Some("5.77в"), Some("76%"))),
      SleeperParser.parseReport(
        """АвтоФон SE-Маяк v6.3k
          |27-11-2016 12:02:31
          |GSM -58dB
          |MCC: 250 MNC: 001
          |LAC: 029E 029E 029E 029E
          |CID: 21DA 21D9 0FD5 21D8
          |Бат:5.77в(76%) T:0C
          |смс#40""".stripMargin('|'))
    )
  }

  @Test
  def testAutoPhoneRus5() {
    Assert.assertEquals(
      Success(MatchResultTemplate(None, None, None, Some(LBS(250, 1, 690, 43126)), Some("5.69в"), Some("98%"))),
      SleeperParser.parseReport(
        """АвтоФон E-Маяк v5.6i
          |15-11-2016 12:02:37
          |GSM -44dB
          |MCC: 250 MNC: 001
          |LAC: 02B2 02B2 02B2 02B2
          |CID: A876 A874 A872 9BB1
          |Бат:5.69в(98%) T:-3C
          |смс#18""".stripMargin('|'))
    )
  }


  @Test
  def testBadLBS() {
    Assert.assertEquals(
      Success(MatchResultTemplate(None, None, None, Some(LBS(250, 1, 35, 52624)), Some("5.95v"), Some("100%"))),
      SleeperParser.parseReport(
        """AutoFon SE-Mayak v6.3k
          |07-08-2016 12:03:30
          |GSM -17dB
          |MCC: 250 MNC: 001
          |LAC: 0023 01FF 0203 0203
          |CID: CD90 7949 0899 CD8F
          |Bat:5.95v(100%) T:+6C
          |sms#40""".stripMargin('|'))
    )
  }

  @Test
  def testAutoPhoneDotless() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, Some((6362.8, 630.8)), None, Some(LBS(250, 1, 19856, 7303)))),
      SleeperParser.parseReport( """AutoFon Mayak m44w GPS 5/42s GMT 060613 23:33 N45 035148 E043 379188 Speed: 0km/h MCC=250 MNC=01 LAC=4D90 CID=1C87 Mode=024HGK- GPRS to server: off Bat=99% t=+20C""")
    )
  }

  @Test
  def testAutoPhoneDotless2() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 712, 60675)))),
      SleeperParser.parseReport( """AutoFon Mayak m44w GPS 0/360s No GPS data MCC=250 MNC=01 LAC=02C8 CID=ED03 Mode=120HGK- GPRS to server: off Bat=99% t=+22C""")
    )
  }

  @Test
  def testAutoPhoneLinebreakes() {
    Assert.assertEquals(
      // Some(LBS=None lonlat=(37.539583,55.774896) alarm=None posurl=None)
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 6401, 51724)))),
      SleeperParser.parseReport("AutoFon SE-Mayak v6.3n\n13-09-2015 12:03:31\nGSM -61dB\nMCC: 250 MNC: 001\nLAC: 1901 1901 1901 1901\nCID: 00CA0C 00CA0E 00335A 00CA0D\nBat:6.01v(100%) T:+20C\nsms#46")
    )
  }

  @Test
  def testAutoPhoneLowBattery() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, None)),
      SleeperParser.parseReport("AutoFon E-Mayak v3.1d 11-06-2013 12:08 Warning: Low battery. T1: 12-06-2013 12:00, 01D,S T2: 16-06-2013 12:00, 07D,G Battery: 4.97v (8%) Temp.: +10C sms# 282 Mode: sleep")
    )
  }

  @Test
  def testAutoPhoneDaySmsTest() {
    Assert.assertEquals(
      Success(MatchResultTemplateWithoutBats(None, None, None, Some(LBS(250, 1, 574, 5426)))),
      SleeperParser.parseReport("AutoFon Mayak m4.4w 10 day test sms. GPS 0/360s No GPS data MCC=250 MNC=01 LAC=023E CID=1532 Mode=120H,F,K- GPRS to server: off Bat=99% t=+24C")
    )
  }

//  @Test
//  def testStarLineDaySMSTest(): Unit = {
//    val message = "StarLine M10 m4.4v\nGPS 4/219s\nGMT 151118 20:49\nN57 01.6090\nE035 16.9075\nSpeed: 123km/h, 300\nMCC=250\nMNC=01\nLAC=051E\nCID=22BE\nMode=095H,G,K-\nGPRS to server: off\nBat=99%\nt=+8C"
//    val expected = MatchResultTemplate(None, None, None, Some(LBS(250, 1, 1310, 8894)), None, Some("99%"))
//    Assert.assertEquals(Success(expected), SleeperParser.parseReport(message))
//  }

  @Test
  def testStarLine2(): Unit = {
    val message = "StarLine M10 m4.4r\n10 day test sms.\nGPS 0/360s\nNo GPS data\nMCC=250\nMNC=01\nLAC=184C\nCID=F43D\nMode=029H,F,K-\nGPRS to server: off\nBat=99%\nt=+15C"
    val expected = MatchResultTemplate(None, None, None, Some(LBS(250, 1, 0x184c, 0xf43d)), None, Some("99%"))
    Assert.assertEquals(Success(expected), SleeperParser.parseReport(message))
  }

  @Test
  def testStarLine3(): Unit = {
    val message = "StarLine M10 m4.4r\nNew phone write to sim card.\n+79857707575\nMode=029H,F,K-\nGPRS to server: off\nBat=99%\nt=+22C"
    val expected = MatchResultTemplate(None, None, None, None, None, Some("99%"))
    Assert.assertEquals(Success(expected), SleeperParser.parseReport(message))
  }

  @Test
  def testStarLine4(): Unit = {
    val message = "StarLine M6 v5.6m\n08-09-2018 09:33:28\nPASS: 1234\nIMEI: 869158007343268\n1: +79857707575\nSETUP=0135520751000019600000000\nI1=internet.mts.ru\nT1: 09-09-2018 09:30, 01D,F\nT2: 08-09-2018 16:40, 07D,S\nSensor=0/1 (off)\nExternal power:off\nBat:5.98v(100%) T:+32C\nMode:sleep\nsms#4 "
    val expected = MatchResultTemplate(None, None, None, None, Some("5.98v"), Some("100%"))
    Assert.assertEquals(Success(expected), SleeperParser.parseReport(message))
  }

  @Test
  def testStarLine5(): Unit = {
    val message = "StarLine M6 v5.6m\n17-11-2018 16:43:21\nGSM -70dB\nMCC: 250 MNC: 001\nLAC: 029F 029F 029F 029F\nCID: DF3C 6A50 1C21 DF3D\nBat:5.95v(100%) T:-9C\nsms#1"
    val expected = MatchResultTemplate(None, None, None, Some(LBS(250, 1, 0x29f, 0xdf3c)), Some("5.95v"), Some("100%"))
    Assert.assertEquals(Success(expected), SleeperParser.parseReport(message))
  }

  @Test
  def testVegam1(): Unit = {
    val message = "24.06.19\n14:22:40\nN 55.74677\nE 37.76983\n" +
      "Sat=6\nTs=01:34\n0 km/h\nA=194\nT=26\nA/h=100%\n12=1\nN=7\n4/4\nBal=9874\nMCC=250\nMNC=01\nLAC=0232\nCID=411F\n"
    val expected = MatchResultTemplate(None, Some((37.76983, 55.74677)),None, Some(LBS(250,1, 0x232, 0x411f)), None, Option("100%"))

    Assert.assertEquals(Success(expected), SleeperParser.parseReport(message))
  }

  @Test
  def testVegam2(): Unit = {
    val message ="10.07.19\n03:02:01\nN 55.70649\nE 37.59623\n" +
      "Sat=8\nTs=01:28\n0 km/h\nA=173\nT=14\nA/h=100%\n12=1\nN=7\n4/4\nBal=32509\nMCC=250\nMNC=01\nLAC=18C0\nCID=0927\n"

    val expected = MatchResultTemplate(None, Some((37.59623, 55.70649)),None, Some(LBS(250,1, 0x18c0, 0x927)), None, Option("100%"))
    Assert.assertEquals(Success(expected), SleeperParser.parseReport(message))
  }

  @Test
  def testVegam3(): Unit = {
    val message = "02.07.19\n03:01:52\nN 55.88223\nE 36.87320\n" +
      "Sat=10\nTs=01:25\n0 km/h\nA=122\nT=16\nA/h=100%\n12=1\nN=7\n3/4\nBal=32133\nMCC=250\nMNC=01\nLAC=0339\nCID=F455\n"

    val expected = MatchResultTemplate(None, Some((36.87320, 55.88223)),None, Some(LBS(250,1, 0x339, 0xf455)), None, Option("100%"))
    Assert.assertEquals(Success(expected), SleeperParser.parseReport(message))
  }

//  @Test
//  def testStarLine6(): Unit = {
//    val message = "StarLine M10 m4.4v\n10 day test sms.\nGPS 4/235s\nGMT 160918 05:28\nN55 34.4414\nE037 40.4707\nSpeed: 0km/h\nMCC=250\nMNC=01\nLAC=02A1\nCID=F1BC\nMode=095H,G,K-\nGPRS to server: off\nBat=99%\nt=+15C"
//    val expected = MatchResultTemplate(None, None, None, Some(LBS(250, 1, 0x2a1, 0xf1bc)), None, Some("99%"))
//    Assert.assertEquals(Success(expected), SleeperParser.parseReport(message))
//  }
}

@deprecated()
case class MatchResultTemplateWithoutBats(
                                alarm: Option[Alarm],
                                lonlat: Option[(Double, Double)],
                                positionURL: Option[URL],
                                lbs: Option[LBS]
                              ) extends MatchResult {


  override def batValue: Option[String] = None

  override def batPercentage: Option[String] = None

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case m: MatchResult => alarm == m.alarm &&
        lonlat == m.lonlat &&
        positionURL == m.positionURL &&
        lbs == m.lbs
      case _ => false
    }
  }
}

case class MatchResultTemplate(
                                       alarm: Option[Alarm],
                                       lonlat: Option[(Double, Double)],
                                       positionURL: Option[URL],
                                       lbs: Option[LBS],
                                       batValue: Option[String],
                                       batPercentage: Option[String]
                                     ) extends MatchResult {


  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case m: MatchResult => alarm == m.alarm &&
        lonlat == m.lonlat &&
        positionURL == m.positionURL &&
        lbs == m.lbs &&
        batValue == m.batValue &&
        batPercentage == m.batPercentage
      case _ => false
    }
  }
}