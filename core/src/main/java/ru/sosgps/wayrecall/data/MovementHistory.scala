/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.data

import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.utils.{PartialIterable, LimitsSortsFilters, impldotChain}
import collection.mutable

class MovementHistory protected
(
  val total: Int,
  val start: Int,
  val limit: Int,
  it: => Iterator[GPSData],
  val additionalData: mutable.SynchronizedMap[String, Any]
  ) extends PartialIterable[GPSData] with grizzled.slf4j.Logging {


  def this(total: Int, start: Int, limit: Int, it: => Iterator[GPSData]) =
    this(total, start, limit, it, new mutable.HashMap[String, Any]() with mutable.SynchronizedMap[String, Any])

  override type Repr = MovementHistory

  override def size = throw new UnsupportedOperationException("size does not matter, total does ")

  def count = math.max(0, math.min(total - start, limit))

  def iterator: Iterator[GPSData] = it

  override protected def subInstance(_total: Int, _start: Int, _limit: Int, it: => Iterator[GPSData]): Repr =
    new MovementHistory(_total, _start, _limit, it, additionalData)




  override def toString() = s"MovementHistory(total=$total, start=$start, limit=$limit, additionalData=$additionalData, iterator=$iterator)"
}

