package ru.sosgps.wayrecall.utils.web

import org.junit.{Assert, Test}
import java.util.{Calendar, TimeZone, GregorianCalendar, Date}


class DateConverterTest {


  @Test
  def testGMT0400(){
    val converter = new DateConverter()

    val r = converter.convert("Thu Oct 11 2012 00:00:00 GMT+0400 (MSK)")
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+0400"))

    calendar.set(2012, 9, 11, 0, 0, 0)
    calendar.set(Calendar.MILLISECOND,0)
    val time = calendar.getTime
    println("r="+r+" time="+time)
    Assert.assertEquals("dates",time,r)
  }

  @Test
  def testGMT0000(){
    val converter = new DateConverter()

    val r = converter.convert("Thu Oct 11 2012 00:00:00 GMT+0000")
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+0000"))

    calendar.set(2012, 9, 11, 0, 0, 0)
    calendar.set(Calendar.MILLISECOND,0)
    val time = calendar.getTime
    println("r="+r+" time="+time)
    Assert.assertEquals("dates",time,r)
  }

  @Test
  def convertISO(){
    val converter = new DateConverter()

    val r = converter.convert("2012-11-16T01:20:31")
    val calendar = Calendar.getInstance()
    calendar.set(2012, 10, 16, 1, 20, 31)
    calendar.set(Calendar.MILLISECOND,0)
    val time = calendar.getTime
    println("r="+r+" time="+time)
    Assert.assertEquals("dates",time,r)
  }


}
