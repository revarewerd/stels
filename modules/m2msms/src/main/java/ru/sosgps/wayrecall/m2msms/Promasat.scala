package ru.sosgps.wayrecall.m2msms

import ru.sosgps.wayrecall.core.MongoDBManager
import com.mongodb.casbah.Imports._
import java.util.Date
import SendSms.richMessage
import collection.JavaConversions.iterableAsScalaIterable
import ru.sosgps.wayrecall.m2msms.generated.{ArrayOfMessageInfo, MessageInfo, DeliveryStatus, RequestMessageType}
import scala.collection.mutable.ArrayBuffer
import scala.io.{BufferedSource, Source}
import org.joda.time.DateTime
import scala.collection.{TraversableLike, mutable}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.{Font, HorizontalAlignment}
import java.io.{File, FileInputStream, FileOutputStream}
import java.util.concurrent.atomic.AtomicInteger
import SoapUtils.toDate
import org.apache.poi.openxml4j.opc.{PackageAccess, OPCPackage}

/**
 * Created by nickl on 14.01.14.
 */
object Promasat {

  val spring = SendSms.setupSpring();

  var real = false

  lazy val sendSms = spring.getBean(classOf[SendSms])

  val equipments = spring.getBean("originalmongoDBManager", classOf[MongoDBManager]).getDatabase()("equipments")

  val millsInHour = 60 * 60 * 1000

  lazy val tendays = Source.fromInputStream(this.getClass.getClassLoader
    .getResourceAsStream("autophones/autophones56.txt"))

  def main(args: Array[String]) {

    real = false

    println("Promasat started " + equipments.size)

    val phones = (for (sleeper <- equipments.find(
      MongoDBObject("eqtype" -> "Спящий блок.*".r) ++ ("eqMark" $ne "АвтоФон"),
      MongoDBObject("eqtype" -> 1, "eqMark" -> 1, "simNumber" -> 1))
    ) yield {
      println(sleeper)
      sleeper.getAs[String]("simNumber")
    }).flatten.filter(_.nonEmpty).map(_.stripPrefix("+")).toList

    println(phones)

    //val initialMessMap: Map[String, Seq[MessageInfo]] = tendays.getLines().map(a => (a.split("\t")(1), Seq.empty[MessageInfo])).toMap
    val initialMessMap: Map[String, Seq[MessageInfo]] = phones.map(a => (a, Seq.empty[MessageInfo])).toMap

    val aggr = new ArrayBuffer[String]()
    println("Promasat downloading sms " + phones.size)
    val fromDate = new DateTime().minusDays(1).toDate
    val downloadedSms = phones.grouped(10).map(ph => {
      println("Promasat downloading phones: " + ph)
      sendSms.readSms(ph, fromDate, new Date(), RequestMessageType.ALL).getMessageInfo.toSeq
    }).flatten.toSeq
    println("downloading completed")
    for ((phone, allsmses) <- initialMessMap ++ downloadedSms.reverse.groupBy(_.getDeviceMsid)) {
      val a = analize(allsmses)
      import a._

      if (
        !allsmses.filter(_.outgoing).exists(m => m.deliveryStatus == DeliveryStatus.DELIVERED && m.getMessageText.contains("2121,UNO;+79857707575")) &&
          !sentButNotAnswered.filter(_.getCreationDate.getTime > (System.currentTimeMillis() - 23 * millsInHour))
            .exists(_.deliveryStatus == DeliveryStatus.SENT) &&
          sentButNotAnswered.count(m =>
            m.deliveryStatus == DeliveryStatus.DELIVERED && m.getMessageText.contains("2121,UNO;+79857707575")
          ) < 3
      ) {
        //      if (/*lastIncoming.length == 0 && */sentButNotAnswered.headOption.map(_.deliveryStatus == DeliveryStatus.NOT_SENT).getOrElse(false)) {
        aggr += phone

        println("(" + phone + ") -> passwd=" + activePassword.orNull)
        println("sentButNotAnswered=\n" + sentButNotAnswered.map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))
        println("lastIncoming=\n" + lastIncoming.map(m => "* " + m.getMessageText + "\n").mkString(""))
        println("last sent =\n" + smsess.remains.take(5).map(m => "* " + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))

        val smsToSent = "2121,UNO;+79857707575"
        println("smsToSent=" + smsToSent)
        if (real) sendSms.sendSms(phone, smsToSent)
      }
    }

    println("aggregated count=" + aggr.size)

  }

  def analize(smi: Seq[MessageInfo]) = AutoPhones.analize(smi)

}
