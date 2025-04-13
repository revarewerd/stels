package ru.sosgps.wayrecall.wialonparser

import ru.sosgps.wayrecall.data.{MapProtocolPackConverter, MongoPackagesStore}
import ru.sosgps.wayrecall.core.{GPSData, GPSDataConversions, MongoDBManager, ObjectsRepositoryReader}
import org.joda.time.DateTime
import ru.sosgps.wayrecall.utils.io.{DboReader, DboWriter, RichDataInput}
import java.util.zip.GZIPInputStream
import org.junit.{Assert, Test}

/**
 * Created by nickl on 06.02.14.
 */
class EncodeDecodeTest {

  @Test
  def test() {

//    val packStore = new PerObjectMongoPackStore()
//    packStore.pconv = new PackDataConverter
//
//    val mdbm = new MongoDBManager
//    mdbm.setDatabaseName("Seniel-dev2")
//    packStore.mongoDbManager =  mdbm
//    val or = new ObjectsRepository
//    or.mdbm = mdbm
//    packStore.or = or
//
//    val iter = packStore.getHistoryFor("o351870825327299786", new DateTime(2014,1,29,0,0).toDate,  new DateTime(2014,2,5,0,0).toDate).take(5).toList
//
//    val writer = new DboWriter("gpses.bson")
//    for (gps <- iter) {
//      writer.write(GPSDataConversions.toPerObjectMongoDb(gps))
//    }
//    writer.close()


    val iter = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/gpses.bson.gz"))).iterator.
      map(GPSDataConversions.fromDbo).toList

    val wlns = iter.map(GPSDataConversions.toWialonPackage)

    println(wlns)

    val bytes = wlns.map(WialonPackager.wlnPackToBinary).reduce(_ ++ _)

    val input = new RichDataInput(bytes)
    for (orig <- wlns) {
      val pack = WialonParser.parsePackage(input)
      Assert.assertEquals(orig, pack)
    }

  }

//  @Test
//  def test2() {
//
//    val iter = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/o2924312945438203094badwln.bson.gz"))).iterator.
//      map(GPSDataConversions.fromPerObjectDbo).toList.take(1)
//
//    val wlns2 = new DboReader(new GZIPInputStream(this.getClass.getResourceAsStream("/gpses.bson.gz"))).iterator.
//      map(GPSDataConversions.fromPerObjectDbo).toList.take(1).map(GPSDataConversions.toWialonPackage)
//
//    val wlns = iter.map(GPSDataConversions.toWialonPackage)
//    Assert.assertEquals(wlns.head, wlns2.head)
//    for ((wp,gps )<- wlns zip iter) {
//      assertEqualToGps(gps, wp)
//    }
//
//
//    //iter.foreach(println)
//
//    println(wlns)
//
//    val bytes = wlns.map(WialonPackager.wlnPackToBinary).reduce(_ ++ _)
//
//    val input = new RichDataInput(bytes)
//    for ((orig, gps) <- wlns zip iter) {
//      val pack = WialonParser.parsePackage(input)
//      //println(pack)
//      assertEqualToGps(gps, pack)
//      Assert.assertEquals(orig, pack)
//    }
//
//  }


  private[this] def assertEqualToGps(gps: GPSData, wp: WialonPackage) {
    val gpsre = GPSDataConversions.fromWialonPackage(gps.uid, wp)
    gpsre.placeName = gps.placeName
    gpsre.insertTime = gps.insertTime
    Assert.assertEquals(gps, gpsre)
  }
}
