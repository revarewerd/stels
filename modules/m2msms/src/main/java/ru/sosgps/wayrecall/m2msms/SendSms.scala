package ru.sosgps.wayrecall.m2msms

import generated._
import java.security.MessageDigest
import org.apache.cxf.interceptor.{LoggingOutInterceptor, LoggingInInterceptor}
import java.util.{GregorianCalendar, Date}
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import java.util
import SoapUtils._
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.joda.time.DateTime
import org.springframework.context.annotation.Lazy
import scala.beans.BeanProperty
import ru.sosgps.wayrecall.sms.SMS


class SendSms {

  //  val userName: String = "Stels"
  //  var password: String = "934388"

  val userName: String = "79160967555"
  var password: String = "Stels@2030"

  var sendingNum = "4938"

  var federalNum = "79857707575"

  @BeanProperty
  var federalByDefault = false

  private def senNum(forceFederal: Boolean) = if(forceFederal) federalNum else sendingNum

  password = ru.sosgps.wayrecall.utils.io.Utils.toHexString(MessageDigest.getInstance("MD5").digest(password.getBytes), "")

  @BeanProperty
  var port: MTSX0020CommunicatorX0020M2MX0020XMLX0020APISoap = null

  def sendSms(addr: String, msg: String): Long = sendSms(addr: String, msg: String, federalByDefault)

  def sendSms(addr: String, msg: String, forceFederal: Boolean): Long = {
    val message = port.sendMessage(addr, msg, senNum(forceFederal), userName, password)
    //println("message=" + message)
    message
  }

  def sendSms(addrs: Seq[String], msg: String): ArrayOfSendMessageIDs = sendSms(addrs: Seq[String], msg: String, false)

  def sendSms(addrs: Seq[String], msg: String, forceFederal: Boolean): ArrayOfSendMessageIDs = {
    val message = port.sendMessages(SendSms.seqToArrayString(addrs), msg, senNum(forceFederal), userName, password)
    //println("message=" + message)
    message
  }

  def readLastHourSms(addr: Seq[String], hours:Int = 1, mo: RequestMessageType = RequestMessageType.MO): ArrayOfMessageInfo =
    readSms(addr, new DateTime().minusHours(4+hours).toDate, new DateTime().plusHours(6).toDate, mo)

  def readSms(addr: Seq[String], from: Date, to: Date, mo: RequestMessageType = RequestMessageType.MO): ArrayOfMessageInfo =
    readSms(addr, gregorian(from), gregorian(to), mo)

  def readSms(addr: Seq[String], from: XMLGregorianCalendar, to: XMLGregorianCalendar,
              mo: RequestMessageType): ArrayOfMessageInfo = {

    val message = port.getMessages(mo, SendSms.seqToArrayString(addr),
      from,
      to,
      userName,
      password
    )

    message
  }

  def addUser(name: String, phone: String, emeil: String, group: Long, webAccess: Boolean = false, accessLevel: AccessLevel = AccessLevel.BASE_USER) = {
    port.addUser(userName, password, name, phone, emeil, webAccess, accessLevel, group)
  }

}

class RichMessage(val mi:MessageInfo){

  //   f.getDeliveryInfo.getDeliveryInfoExt.toList.map(f => f.getTargetMsid+" "+f.getDeliveryStatus)+" "+f.getSenderMsid+" "+f.getMessageText+" "+f.getCreationDate+" "+f.getMessageID

  def mkString = delivered +" "+getTargetMsid+" "+mi.getSenderMsid+" "+mi.getMessageText+" "+mi.getCreationDate.toGregorianCalendar.getTime

  def delivered = mi.getDeliveryInfo.getDeliveryInfoExt.map(_.getDeliveryStatus == DeliveryStatus.DELIVERED).head

  def deliveryStatus = mi.getDeliveryInfo.getDeliveryInfoExt.map(_.getDeliveryStatus)
    .find(null !=)
    //.getOrElse(DeliveryStatus.PENDING)

  def getTargetMsid = mi.getDeliveryInfo.getDeliveryInfoExt.map(_.getTargetMsid).head

  def incoming = getTargetMsid.isEmpty

  def outgoing = !incoming

  def getDeviceMsid = {
    if (incoming) mi.getSenderMsid else getTargetMsid
  }

  def toSms= {
    new SMS(
      mi.getMessageID,
      mi.getMessageText,
      true,
      mi.getSenderMsid,
      this.getTargetMsid,
      mi.getCreationDate,
      mi.getDeliveryInfo.getDeliveryInfoExt.head.getDeliveryDate,
      this.deliveryStatus.map(_.value()).orNull
    )
  }

  override def toString = mkString
}

object SendSms {

  implicit def richMessage(mi:MessageInfo) = new RichMessage(mi)

  def seqToArrayString(seq: Seq[String]): ArrayOfString = new ArrayOfString {
    this.string = seq
  }

  def main(args: Array[String]): Unit = {

    val context = setupSpring()

    val addr: String = "79857757673"

    val bean: SendSms = context.getBean(classOf[SendSms])


    //sendSms(addr)


    //        val message = port.addUser(userName,password,"nickl","79164108305","lkuka@yandex.ru",false,AccessLevel.BASE_USER,6290)
    //        println("message="+message)

    val sms = bean.readSms(Seq(addr), new DateTime().minusDays(5).toDate, new Date())

    for (x <- sms.getMessageInfo) {
      println(x.getMessageText)
    }


  }


  def setupSpring() = {
    val context = new ClassPathXmlApplicationContext();
    context.setConfigLocation("m2msms-spring.xml");
    context.refresh();
    context.registerShutdownHook();
    context
  }
}
