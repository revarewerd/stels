package ru.sosgps.wayrecall.core

import java.util.Date

import org.junit.{Assert, Test}
import scala.collection.JavaConverters.mapAsJavaMapConverter

/**
 * Created by nickl on 04.05.15.
 */
class GPSDataConversionsSymmertyTest {


  @Test
  def testSymmetry(): Unit ={

    val gps = new GPSData(
      null: String,
      "863071014179243",
      37.769100189208984,
      55.74689865112305,
      new Date(1427980022000L),
      0,
      0,
      3,
      null: String,
      null: Date,
      Map(
        "MCC" -> 250,
        "MNC" -> 17.toShort,
        "gsm_st" -> 1,
        "gsmlevel" -> 1.toShort,
        "height" -> 11.toShort,
        "info" -> 400,
        "mw" -> 0,
        "nav_st" -> 3,
        "protocol" -> "Skysim",
        "protocolversion" -> 34,
        "pwr_ext" -> 94,
        "pwr_in" -> 3,
        "sim_in" -> 1,
        "sim_t" -> 0,
        "st0" -> 0,
        "st1" -> 0,
        "st2" -> 0,
        "vol1" -> 4135,
        "vol2" -> 13943).mapValues(_.asInstanceOf[AnyRef]).asJava
    );

    gps.privateData = Map("mh" -> 100.asInstanceOf[AnyRef]).asJava

    val dbo = GPSDataConversions.toMongoDbObject(gps)

    Assert.assertEquals(gps, GPSDataConversions.fromDbo(dbo) );

  }

}
