package ru.sosgps.wayrecall.data

import java.util.Date
import javax.annotation.PostConstruct

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import grizzled.slf4j.Logging
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager

/**
 * Created by ilazarevsky on 9/24/15.
 */
class RemovalIntervalsManager extends Logging {

  var dbPeriods: MongoCollection = null

  @Autowired
  var mdbm: MongoDBManager = null

  @PostConstruct
  def init() = {
    debug("Initializing dbPeriods")
    dbPeriods = mdbm.getDatabase()("removalPeriods")
  }

  def getDates(list: MongoDBList) = {
    list
      .map(_.asInstanceOf[BasicDBObject])
      .map((dbo) => (dbo.getAs[Date]("begin").get, dbo.getAs[Date]("end").get))
  }


  def getRemovalPeriods(uid: String) = {
    dbPeriods.findOne(MongoDBObject("uid" -> uid))
      .map(_.getAs[BasicDBList]("periods").get)
      .map(getDates(_))
      .getOrElse(Seq())
  }

  type Interval = (Date,Date)

  def minDate(d1: Date, d2: Date) = {
    if (d1.compareTo(d2) < 0)  d1  else d2
  }

  def maxDate(d1: Date, d2: Date) = {
    if (d1.compareTo(d2) < 0)  d2  else d1
  }

  def intersections(i1: Interval, is: Seq[Interval]) = {
    is.map(i2 => intersection(i1,i2)).filter(_.isDefined).map(_.get)
  }

  def intersection (i1: (Date,Date), i2: (Date,Date)) = {
    val (a,b) = i1;
    val (c,d) = i2;
    val x = maxDate(a,c);
    val y = minDate(b,d);
    if(x.compareTo(y) <= 0)
      Option((x,y))
    else
      None
  }

  def diff(i1: (Date,Date), i2: (Date,Date)) = {
    val inter = intersection(i1,i2)
    if(inter.isDefined) {
      val (x, y) = inter.get;
      val (a, b) = i1;
      Seq((a, x), (y, b)).filter { case (x, y) => x != y }
    }
    else
      Seq(i1)
  }

  def diffSetHelper(cut1: Seq[Interval], cut2: Seq[Interval]) = {
    cut1.flatMap(in => intersections(in,cut2))
  }

//  def diffSet(i1 : (Date,Date), intervals: Seq[(Date,Date)]) = {
//    val cuts = intervals.map(i2 => diff(i1,i2))
//    if(cuts.isEmpty)
//      if(intervals.isEmpty) Seq(i1) else Seq.empty
//    else
//      cuts.reduce((r,c) => diffSetHelper(r,c))
//  }

  // Интервалы должны быть отсортированны
  def diffSet(i1 : (Date,Date), intervals: Seq[(Date,Date)]) = {
    intervals.foldLeft(Seq(i1))((r,i) => {
      val head = r.headOption
      val newHead = head.map(diff(_,i).reverse)
      if(newHead.isDefined)
        newHead.get ++ r.tail
      else
        r
    }).reverse
  }

  def getFilteredIntervals(uid: String, from: Date, to: Date): Seq[Interval] = {
    val removalPeriods = getRemovalPeriods(uid)
    diffSet((from,to), removalPeriods)
  }

  val minTime = new Date(Long.MinValue)
  val maxTime = new Date(Long.MaxValue)
  def getFilteredIntervals(uid:String): Seq[Interval] = {
    getFilteredIntervals(uid,minTime,maxTime)
  }
}

object RemovalIntervalsManager {
  type Interval = (Date,Date)

  implicit def intervalMethods(in: Interval) = new {
    def includes( d: Date): Boolean = {
      val (min,max) = in
      min.compareTo(d) <= 0 && d.compareTo(max) <= 0
    }
  }

}

