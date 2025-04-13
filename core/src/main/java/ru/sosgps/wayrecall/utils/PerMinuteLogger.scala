package ru.sosgps.wayrecall.utils

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 17.02.13
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
class PerMinuteLogger(val milsperminute: Long = 60 * 1000) {
  @volatile
  private var minutePackCount = 0;
  private var minutelastUpdateMils: Long = 0;

  def logPerMinute(f: (Int, Int) => Unit) {
    synchronized {
      minutePackCount = minutePackCount + 1
      val timedif: Long = System.currentTimeMillis() - minutelastUpdateMils
      if (timedif > milsperminute) {
        f(minutePackCount, (timedif - milsperminute).toInt)
        //println("store packspersecond:" + minutePackCount/60 + " mis=" + (timedif - milsperminute))
        minutelastUpdateMils = System.currentTimeMillis()
        minutePackCount = 0;
      }
    }
  }
}
