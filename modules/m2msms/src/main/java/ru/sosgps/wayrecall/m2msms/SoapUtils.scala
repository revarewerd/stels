package ru.sosgps.wayrecall.m2msms

import java.util.{GregorianCalendar, Date}
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.03.13
 * Time: 15:56
 * To change this template use File | Settings | File Templates.
 */
object SoapUtils {

  def gregorian(time: Date): XMLGregorianCalendar = {
    val calendar = new GregorianCalendar()
    calendar.setTime(time)
    toXml(calendar)
  }


  def toXml(calendar: GregorianCalendar): XMLGregorianCalendar = {
    DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar)
  }

  implicit def toDate(calendar: XMLGregorianCalendar): Date = {
    calendar.toGregorianCalendar.getTime
  }

}
