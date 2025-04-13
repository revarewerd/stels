package ru.sosgps.wayrecall

import java.io.{FileInputStream, File, FileOutputStream}
import java.text.NumberFormat
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import org.apache.commons.compress.archivers.zip.ZipFile
import org.joda.time.DateTime
import ru.sosgps.wayrecall.core.{DistanceUtils, GPSDataConversions}
import ru.sosgps.wayrecall.utils.io.{DboWriter, DboReader}
import ru.sosgps.wayrecall.utils.tryNumerics

import scala.collection.JavaConversions.enumerationAsScalaIterator

/**
 * Created by nickl on 05.05.15.
 */
object AggregationAnalyzer {

  val baddir = new File("badfile")
  baddir.mkdirs()
  baddir.listFiles().foreach(_.delete())

  def main(args: Array[String]) {
    val file = new ZipFile("/media/f308b432-a735-4b23-8430-c497d2292510/dump-2015-05-17-Seniel-dev2.zip")


    var bad = 0;
    var good = 0;
    val beggining = new DateTime(2015, 5, 15, 0, 0, 0).toDate

    for (e <- file.getEntries /*if e.getName.contains("objPacks.o4361404363657947275.bson")*/ ) {

      val gpses = new DboReader(file.getInputStream(e)).iterator
        .map(GPSDataConversions.fromDbo).toIndexedSeq.sortBy(_.time)
        .dropWhile(_.time.before(beggining))
        .dropWhile(_.privateData.isEmpty)
        .filter(!_.privateData.isEmpty)
        .toIndexedSeq
      if (gpses.size > 1000) {

        //       println(gpses.takeRight(10).map(_.privateData))

        val sumdist = DistanceUtils.sumdistance(gpses)
        val max = gpses.iterator.map(_.privateData.get("tdist").tryDouble).reduce((a, b) => if (a > b) a else b)
        val min = gpses.iterator.map(_.privateData.get("tdist").tryDouble).reduce((a, b) => if (a > b) b else a)
        //val diffdist = gpses.last.privateData.get("tdist").tryDouble - gpses.head.privateData.get("tdist").tryDouble
        val diffdist = max - min
        val diffdist2 = gpses.last.data.get("tdist").tryDouble - gpses.head.data.get("tdist").tryDouble

        if (Math.abs(sumdist - diffdist) > sumdist * 0.1) {
          bad = bad + 1;
          println()
          println("" + e)
          println(gpses.size)
          println(gpses.head.privateData)
          println(gpses.last.privateData)
          println(min + " " + max)
          println(s"$sumdist  $diffdist $diffdist2")
          val uid = gpses.head.uid
          println("dumping " + uid)
          new DboWriter(new GZIPOutputStream(new FileOutputStream(new File(baddir, uid + ".gz")))).writeAndClose(gpses.map(gps => {
            //gps.privateData.clear()
            GPSDataConversions.toMongoDbObject(gps)
          }): _*)
        } else {
          good = good + 1
        }


      }
    }

    println(s"bad=$bad good=$good")


  }

}

object AnalyseOne {

  implicit class toSplittable(i: Long) {
    def splitted = NumberFormat.getIntegerInstance().format(i)
  }

  def main(args: Array[String]) {
    val gpses = new DboReader(new GZIPInputStream(
      new FileInputStream("/home/nickl/Загрузки/o1189014804117835622.bson.gz"))
    ).iterator.map(GPSDataConversions.fromDbo).toIndexedSeq.toIndexedSeq.sortBy(_.insertTime)
      .slice(2000, 7000)
      .grouped(1).map(_.last).toIndexedSeq

    def aggrvalue(ii1: Int): Double = gpses(ii1).privateData.get("tdist").tryDouble

    for (i <- gpses.indices) {
      println(gpses(i).time.getTime.splitted + " " + gpses(i).insertTime.getTime.splitted + " " + gpses(i).privateData + " "
        + (aggrvalue(0) + DistanceUtils.sumdistance((0 to i).map(gpses).sortBy(_.time))))
    }

    val sumdist = DistanceUtils.sumdistance(gpses)
    val max = gpses.iterator.map(_.privateData.get("tdist").tryDouble).reduce((a, b) => if (a > b) a else b)
    val min = gpses.iterator.map(_.privateData.get("tdist").tryDouble).reduce((a, b) => if (a > b) b else a)
    //val diffdist = gpses.last.privateData.get("tdist").tryDouble - gpses.head.privateData.get("tdist").tryDouble
    val diffdist = max - min
    val diffdist2 = gpses.last.data.get("tdist").tryDouble - gpses.head.data.get("tdist").tryDouble

    println(s"sumdist=$sumdist diffdist=$diffdist diffdist2=$diffdist2")



    //    val badIndexes = gpses.indices.sliding(2).filter(ii => aggrvalue(ii(0)) > aggrvalue(ii(1))).map(_.head).toList
    //
    //    for (i <- badIndexes) {
    //      println("index:" + i + " " + aggrvalue(i) + " " + aggrvalue(i + 1))
    //      gpses.slice(i - 1, i + 3).foreach(println)
    //      println()
    //    }

    //    for (ii <- gpses.indices.sliding(2)) {
    //      val distutils = DistanceUtils.kmsBetween(gpses(ii(0)), gpses(ii(1)))
    //      val diff = aggrvalue(ii(1)) - aggrvalue(ii(0))
    //      if (Math.abs(distutils - diff) > 0.01) {
    //        val i = ii(0)
    //
    //        println("index:" + i + " " + distutils + " " + diff)
    //        gpses.slice(i - 1, i + 3).foreach(println)
    //        println()
    //      }
    //
    //    }


  }

}
