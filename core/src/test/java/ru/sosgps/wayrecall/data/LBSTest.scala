package ru.sosgps.wayrecall.data

import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.data.sleepers.{MongoStoredLBSConverter, LBS, LBSHttp}

/**
 * Created by nickl on 30.09.14.
 */
object LBSTest {

  def main(args: Array[String]) {

    val lbs = new LBSHttp

    //http://www.opencellid.org/cell/get?key=1604dfa5ac4d64df0945327fb764b0b7&mnc=1&mcc=250&lac=6402&cellid=51024


    //   println(openCellId(6402, 51024, 250, 1))
    //   println(lbs.yandex(LBS(MCC = 6402, MNC = 51024, LAC = 250, CID = 1)))
    //   println(lbs.ultrastar(lac = 250, cellId = 1, mcc = 6402, mnc = 51024))
    //println(lbs.yandex(LBS(0x1902, 0xC750, 250, 1)))
    //println(lbs.ultrastar(0x1902, 0xC750, 250, 1))

    // LBShex(MCC=716, MNC=10, LAC=3fe, CID=3cd)

    //val lbs1 = LBS(MCC = 716, MNC = 10, LAC = 1022, CID = 973)
    //   MCC=716, MNC=10, LAC=0462, CID=0fd3
    val lbs1 = LBS(MCC = 716, MNC = 10, LAC = 0x0462, CID = 0x0fd3)
    println("lbs1="+lbs1)
    println(lbs.yandex(lbs1))
    println(lbs.ultrastar(lbs1))
    println(lbs.convertLBS(lbs1))

    val dbLbs = new MongoStoredLBSConverter
    val manager = new MongoDBManager()
    manager.databaseName = "Seniel-dev2"
    dbLbs.setMdbm(manager)

    println(dbLbs.convertLBS(lbs1))


  }

}
