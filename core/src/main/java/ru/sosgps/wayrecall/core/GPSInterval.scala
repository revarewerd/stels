package ru.sosgps.wayrecall.core

import java.util.Date

/**
  * Created by ivan on 17.07.17.
  */

trait TimeInterval {
  def firstTime: Date
  def lastTime: Date
}

case class SimpleTimeInterval(firstTime: Date, lastTime: Date) extends TimeInterval

trait GPSInterval extends TimeInterval {
  def first: GPSData
  def last: GPSData

  override def firstTime: Date = first.time

  override def lastTime: Date = last.time
}
