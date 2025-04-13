package ru.sosgps.wayrecall.avlprotocols.gosafe

import ru.sosgps.wayrecall.utils.io.{RichDataInput, Utils}

import GosafeParser._
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.core.{LBSData, GPSData}
import ru.sosgps.wayrecall.utils.parseDate
import java.util.Date
import ru.sosgps.wayrecall.data.sleepers.LBS
import org.joda.time.{DateTimeZone, DateTime}

/**
 * Created by nickl on 21.01.14.
 */
class ParseTest {

  @Test
  def test1() {
    val s2 = Utils.asBytesHex("F802010357852034572894010E153AA8A6016177B906C2C9D9000103044000000004040000380B050401DC19B806080000000000000000070341077E080402FC4AB0733EF8")
    val in = new RichDataInput(s2)
    val p1 = processPackage(in)

    val data = new GPSData(null, "357852034572894", 113.428953, 23.164857, new DateTime(2011, 4, 15, 6, 56, 38, DateTimeZone.UTC).toDate, 1, 0, 0)
    data.data.put("protocol", "gosafe bin")
    Assert.assertEquals(data, p1.gpsData.head)
    println(p1);
    //    println(processPackage(in));

  }

  @Test
  def test2 {
    val s3 = Utils.asBytesHex("F8" + "02010353301056526200020b19390a1000fa013202170d03040202000004040000c20005060371179f2213164c" + "F8") //
    val p = processPackage(new RichDataInput(s3)) //
    println(p.lbsData)
    println(p.alarms)
    println(p.statuses)
  }


  @Test
  def lbsreal {
    val s3 = Utils.asBytesHex("02010357207055634729020b1a7166da00fa01a102dca90304000000000506020e17b0232209040000001ced5d") //
    val p = parseBinaryPacket(new GosafeBinaryPacket(s3)) //
    val lbs = p.lbsData.head.lbs
    println(lbs.toHexString)
    println(p.alarms)
    println(p.statuses)
    //println(LBSHttp.convertLBS(lbs))
  }

  @Test
  def lbsreal2 {
    val s3 = Utils.asBytesHex("02010359394050304140020b1a73656000fa0128027d320304040200000506020e171c21740904000000124c46") //
    val p = parseBinaryPacket(new GosafeBinaryPacket(s3)) //
    val lbs = p.lbsData.head.lbs
    println(lbs.toHexString)
    println(p.alarms)
    println(p.statuses)
    //println(LBSHttp.convertLBS(lbs))
  }

  @Test
  def lbsreal3 {
    val s3 = Utils.asBytesHex("02010357207055634729020b1a73be0800fa01a102dca90304000000000506020e17e9232109040000001b3f15") //
    val p = parseBinaryPacket(new GosafeBinaryPacket(s3))
    val lBSData = p.lbsData.head
    println(lBSData)
    Assert.assertEquals(new LBSData(null, "357207055634729", LBS(250, 1, 673, 43484), new DateTime(2014, 1, 23, 11, 48, 24, DateTimeZone.UTC).toDate), lBSData)
    //
    val lbs = lBSData.lbs
    println(lbs.toHexString)
    println(p.alarms)
    println(p.statuses)
    //println(LBSHttp.convertLBS(lbs))
  }

  @Test
  def lbstest {

    val default = DateTimeZone.getDefault
    println("defaultTimezone=" + default + " " + default.toTimeZone)

    val s3 = Utils.asBytesHex("F8" + "0201" + "0353301056526200" + "020B153AA84D01CC0031276D43" + "F8") //
    val p = processPackage(new RichDataInput(s3))
    Assert.assertArrayEquals(Array[AnyRef](
      new LBSData(null, "353301056526200", LBS(460, 0, 10033, 17261), new DateTime(2011, 4, 15, 6, 55, 9, DateTimeZone.UTC).toDate)),
      p.lbsData.toArray.asInstanceOf[Array[AnyRef]])
    val lbs = p.lbsData.head.lbs
    println(lbs.toHexString)
    //println(LBSHttp.convertLBS(lbs))
  }
}
