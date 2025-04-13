package ru.sosgps.wayrecall.m2msms

import java.util.Date
import SendSms.richMessage
import collection.JavaConversions.iterableAsScalaIterable
import ru.sosgps.wayrecall.m2msms.generated.RequestMessageType
import org.joda.time.DateTime
import java.util.concurrent.atomic.AtomicInteger


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 20.05.13
 * Time: 0:36
 * To change this template use File | Settings | File Templates.
 */
object RetargetServer {

  val phones = """+79160861642
                 |+79160861646
                 |+79160862725
                 |+79857756872
                 |+79160862508
                 |+79160877316
                 |+79160861523
                 |+79160863955
                 |+79160893307
                 |+79160892315
                 |+79160893315
                 |+79160893497
                 |+79164093995
                 |+79853346834
                 |+79164172395
                 |+79164101683
                 |+79160873776
                 |+79163351845
                 |+79854925833
                 |+79854928497
                 |+79151191131
                 |+79854928518
                 |+79151189774
                 |+79854928499
                 |+79854928496
                 |+79160873749
                 |+79151185749
                 |+79854925832
                 |+79151286729
                 |+79165875466
                 |+79165680283
                 |+79151393368
                 |+79163145927
                 |+79163159664
                 |+79160879168
                 |+79160874568
                 |+79160897151
                 |+79160898574
                 |+79164389004""".stripMargin.split("\n").map(_.stripPrefix("+")).toList

  val spring = SendSms.setupSpring()

  def main(args: Array[String]) {

    val sendSms = spring.getBean(classOf[SendSms])

    val downloadedSms = sendSms.readSms(phones, new DateTime().minusDays(160).toDate, new Date(), RequestMessageType.ALL)

    val count = new AtomicInteger(0)

    for ((phone, allsmses) <- downloadedSms.getMessageInfo.toSeq.reverse.groupBy(_.getDeviceMsid)) {

      val incoming = allsmses.filter(_.incoming)
      val portSet = incoming.find(_.getMessageText == "Param ID:1246 New Val:9088").isDefined
      val ipSet = incoming.find(_.getMessageText == "Param ID:1245 New Text:91.230.215.12").isDefined

      val outgoing = allsmses.filter(!_.incoming)
      val portSetMessage = (outgoing.filter(_.delivered) ++ outgoing).find(_.getMessageText == "21s21 5803 setparam 1246 9088")
      val ipSetMessage = (outgoing.filter(_.delivered) ++ outgoing).find(_.getMessageText == "21s21 5803 setparam 1245 91.230.215.12")

      if (!ipSet && ipSetMessage.map(!_.delivered).getOrElse(true))
      //if(allsmses.head.incoming && !portSet)
      //if(!allsmses.head.incoming && !allsmses.head.delivered) {
      {
        println(count.incrementAndGet() + " " + phone)
        println("allsmses=\n" + allsmses.map(m => "* " + (if (m.incoming) "<- " else "-> ") + "(" + m.deliveryStatus + ") " + m.getMessageText + "\n").mkString(""))

        // sendSms.sendSms(phone,"21s21 5803 setparam 1245 91.230.215.12")
        // sendSms.sendSms(phone,"21s21 5803 setparam 1246 9088")

      }

    }

    // sendSms.sendSms(phones,"21s21 5803 setparam 1245 91.230.215.12")
    //sendSms.sendSms(phones,"21s21 5803 setparam 1246 9088")
  }

}
