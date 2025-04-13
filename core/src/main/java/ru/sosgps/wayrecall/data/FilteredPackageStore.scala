package ru.sosgps.wayrecall.data

import java.util.Date

import grizzled.slf4j.Logging
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils.LimitsSortsFilters
import ru.sosgps.wayrecall.data.RemovalIntervalsManager._
import scala.beans.BeanProperty
import scala.reflect.ClassTag

/**
 * Created by ivan on 25.10.15.
 */
class FilteredPackageStore extends PackagesStore with Logging {
  @BeanProperty
  var mongoStore: PackagesStore = null;

  @Autowired
  var removalIntervals: RemovalIntervalsManager = null

  val MinDate = new Date(Long.MinValue)
  val MaxDate = new Date(Long.MaxValue)

  override def getLatestFor(uids: Iterable[String]): Iterable[GPSData] = {
//    wrapped.getLatestForIntervals(uids.map(uid => (uid, removalIntervals.getFilteredIntervals(uid, MinDate, new Date))))
    // Не забыть отредактировать perObject

    uids.flatMap(uid => mongoStore.getLatestWithinIntervals(uid, removalIntervals.getFilteredIntervals(uid, MinDate, new Date)))
  }

  override def at(uid: String, date: Date): Option[GPSData] = {
    if(removalIntervals.getFilteredIntervals(uid,date,date).exists(_.includes(date)))
      mongoStore.at(uid,date)
    else
      None
  }


  override def next(uid: String, date: Date): Option[GPSData] = {
    val intervalsAfter = removalIntervals.getFilteredIntervals(uid,date,MaxDate)
    intervalsAfter.toStream.flatMap(i => mongoStore.next(uid, i._1, i._2)).headOption
  }

  override def prev(uid: String, date: Date): Option[GPSData] = {
    // Считаем их отсортированными по возрастанию
    val intervalsBefore = removalIntervals.getFilteredIntervals(uid,MinDate, date).reverse
    intervalsBefore.toStream.flatMap(i => mongoStore.prev(uid, i._2, i._1)).headOption
  }

  override def getPositionsCount(uid: String, from: Date, cur: Date): Int = {

    removalIntervals.getFilteredIntervals(uid,from,cur)
      .map{ case (begin,end) => mongoStore.getPositionsCount(uid,begin,end)}
      .sum
  }

  override def getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters): MovementHistory = {
    val intervals = removalIntervals.getFilteredIntervals(uid,from,to)
    val (start,limit) = (limitsAndSorts.start, limitsAndSorts.limit)
   // debug(s"Gethistory start = ${limitsAndSorts.start}, limitOpt = ${limitsAndSorts.limit}, uid = $uid, intervals = $intervals")

    val init = new MovementHistory(0, start, limit.getOrElse(0), Iterator.empty)

    intervals.foldLeft(init)((result, interval) => {
      val ls = new LimitsSortsFilters(math.max(0,start - result.total), limit.map(_ - result.count), limitsAndSorts.sortName, limitsAndSorts.order)
      val part = mongoStore.getHistoryFor(uid, interval._1, interval._2, ls)
      val total = result.total + part.total
      new MovementHistory(total, start, limit.getOrElse(total), result.iterator ++ part.iterator)
    })
  }

  override def refresh(uid: String): Unit = {
    mongoStore.refresh(uid)
  }

  override def removePositionData(uid: String, from: Date, to: Date): Unit = {
    mongoStore.removePositionData(uid,from,to) // Вроде не нужно переопределять
  }

  override def getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double): Option[GPSData] = {
    def distToTarget(g: GPSData) = GPSData.eulcidian2distance(g, lon, lat)

    removalIntervals.getFilteredIntervals(uid,from,to)
      .flatMap{case(begin,end) => mongoStore.getNearestPosition(uid,begin,end,lon,lat,radius) }
      .reduceOption((a,b) =>  if (distToTarget(a) < distToTarget(b)) a else b)
  }

  override def unwrap[T <: PackagesStore : ClassTag]: Option[T] = super.unwrap.orElse(mongoStore.unwrap[T])

}
