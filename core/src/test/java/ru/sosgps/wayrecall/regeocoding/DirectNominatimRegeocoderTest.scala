package ru.sosgps.wayrecall.regeocoding

import javax.sql.DataSource
import java.util.Properties
import java.io.{FileInputStream, PrintStream, PrintWriter, StringWriter}
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 14.10.13
 * Time: 18:03
 * To change this template use File | Settings | File Templates.
 */
object DirectNominatimRegeocoderTest {

  def createPgStore(): DataSource = {
    val properties = new Properties()
    properties.load(new FileInputStream(System.getenv("WAYRECALL_HOME") + "/conf/packreceiver.properties"))

    val dataSource = new org.apache.tomcat.jdbc.pool.DataSource()
    dataSource.setDriverClassName("org.postgresql.Driver")
    dataSource.setUrl(properties.getProperty("packreceiver.nominatim.url"))
    dataSource.setUsername(properties.getProperty("packreceiver.nominatim.user"))
    dataSource.setPassword(properties.getProperty("packreceiver.nominatim.password"))
    dataSource.setMaxActive(4)
    dataSource
  }

  def procPos(r: Either[Throwable, String]):String =
    r match {
      case Right(s) => s
      case Left(t) => {
        val sw = new StringWriter()
        t.printStackTrace(new PrintWriter(sw))
        sw.toString
      }
    }


 def placeTypeTest(regeocoder: DirectNominatimRegeocoder): Unit = {}
//   val coords = List(
//       (39.51185703, 56.6925359),
//       (39.5527339, 56.68315539),
//      (40.81404805, 56.56544166),
//      (39.76280451, 57.69857499)
////       (37.6294336, 55.7810752),
////       (38.9162688, 55.5809728),
////       (37.5845984, 55.8286144),
////       (37.456272, 55.811392),
////       (37.5852288, 55.5276224),
////       (37.478032, 55.1422528),
////       (39.5526496, 56.2191936)
//     )
//   coords.foreach{case(lon, lat) => regeocoder.researchPlaces(lon,lat)}
// }
//  def main(args: Array[String]) {
//    val regeocoder = new DirectNominatimRegeocoder
//    regeocoder.dataSource = createPgStore()
//    placeTypeTest(regeocoder)
//
////    println("position=" + Await.result(regeocoder.getPosition(37.6294336, 55.7810752), Duration(10, TimeUnit.SECONDS)))
////    println("position=" + Await.result(regeocoder.getPosition(38.9162688, 55.5809728), Duration(10, TimeUnit.SECONDS)))
////    println("position=" + Await.result(regeocoder.getPosition(37.5845984, 55.8286144), Duration(10, TimeUnit.SECONDS)))
////    println("position=" + Await.result(regeocoder.getPosition(37.456272, 55.811392), Duration(10, TimeUnit.SECONDS)))
////    println("position=" + Await.result(regeocoder.getPosition(37.5852288, 55.5276224), Duration(10, TimeUnit.SECONDS)))
////    println("position=" + Await.result(regeocoder.getPosition(37.478032, 55.1422528), Duration(10, TimeUnit.SECONDS)))
////    println("position=" + Await.result(regeocoder.getPosition(39.5526496, 56.2191936), Duration(10, TimeUnit.SECONDS)))
////    println("position=" + procPos(regeocoder.getPosition(37.412992, 55.738752)))
////    println("position=" + procPos(regeocoder.getPosition(37.5517733, 55.7048383)))
//
//
//  }

}
