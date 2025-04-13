/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.data

import java.util.Date
import ru.sosgps.wayrecall.utils.{PartialIterable, LimitsSortsFilters}
import ru.sosgps.wayrecall.core.GPSData

import scala.reflect.ClassTag
import scala.reflect._
import scala.reflect.runtime.universe._

trait PackagesStore {
  def getLatestFor(uids: Iterable[String]): Iterable[GPSData]

  def getHistoryFor(uid: String, from: Date, to: Date, limitsAndSorts: LimitsSortsFilters = LimitsSortsFilters.empty): MovementHistory

  def getNearestPosition(uid: String, from: Date, to: Date, lon: Double, lat: Double, radius: Double): Option[GPSData]

  def removePositionData(uid: String, from: Date, to: Date): Unit

  def getPositionsCount(uid: String, from: Date, cur: Date): Int

  def refresh(uid: String)

  def at(uid: String, date: Date): Option[GPSData]

  def prev(uid: String, date: Date): Option[GPSData]

  def prev(uid: String, date: Date, lowerBound : Date): Option[GPSData] = prev(uid,date)

  def next(uid: String, date: Date): Option[GPSData]

  def next(uid: String, date: Date, upperBound: Date) : Option[GPSData] = next(uid,date)
  
  
  def unwrap[T <: PackagesStore : ClassTag]: Option[T] =  {
   this match {
     case x: T => Some(x)
     case _ => None
   }
  }


  def getLatestWithinIntervals(uid: String, intervals: Seq[(Date,Date)]) = getLatestFor(Iterable(uid)).headOption


}
