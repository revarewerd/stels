package ru.sosgps.wayrecall.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.mail._
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.io.UnsupportedEncodingException
import java.util.Date

import java.util.Properties

import scala.collection.immutable
import scala.collection.immutable.HashSet
import scala.collection.mutable


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 17.10.13
 * Time: 17:28
 * To change this template use File | Settings | File Templates.
 */


class MailSender extends grizzled.slf4j.Logging{

  private var email: String = null
  private var host: String = null
  private var login: String = null
  private var password: String = null

  def this(email: String, host: String, login: String, password: String, allowedAddress: String ){
    this()
    this.email = email
    this.host = host
    this.login = login
    this.password = password
    //this.allowedAddress.clear()
    this.setAllowedAddress(allowedAddress)
  }

  private val allowedAddress: mutable.Set[String] = new mutable.HashSet[String]()

  def sendEmail(to: String, subject: String, text: String) {
    sendEmail(to, subject, text, "text/plain; charset=UTF8")
  }

  def sendEmail(to: String, subject: String, text: String, contentType: String) {
    try {
      val msg: Message = new MimeMessage(getSession)
      try {
        msg.setFrom(new InternetAddress(getEmail, "Wayrecall"))
      }
      catch {
        case e: UnsupportedEncodingException => {
          throw new RuntimeException(e)
        }
      }
      val address: Array[Address] = Array(new InternetAddress(to))
      msg.setRecipients(Message.RecipientType.TO, address)
      msg.setSubject(subject)
      msg.setSentDate(new Date)
      msg.setContent(text, contentType)
      send(msg)
    }
    catch {
      case mex: MessagingException => {
        logger.warn("mail sending error", mex)
      }
    }
  }

  @throws(classOf[MessagingException])
  def send(msg: Message) {
    if(allowedAddress.nonEmpty) {
      val forbiddenAddresses = msg.getAllRecipients.filterNot(a => allowedAddress(a.toString))
      if(forbiddenAddresses.nonEmpty) {
        warn("not allowed to send mails to " + forbiddenAddresses.mkString(", "))
        return
      }
    }
    Transport.send(msg)
  }

  def getSession: Session = {
    val props: Properties = new Properties
    props.put("mail.smtp.host", getHost)
    props.put("mail.smtp.port", 25.asInstanceOf[AnyRef])
    props.put("mail.smtp.auth", true.asInstanceOf[AnyRef])
//    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
//    props.put("mail.smtp.socketFactory.fallback", "false")
    return Session.getInstance(props, new Authenticator  {
      protected override def getPasswordAuthentication: PasswordAuthentication =
      {
        return new PasswordAuthentication(getLogin, getPassword)
      }
    })
  }

  def getEmail: String = {
    return email
  }

  def setEmail(email: String) {
    this.email = email
  }

  def getHost: String = {
    return host
  }

  def setHost(host: String) {
    this.host = host
  }

  def getLogin: String = {
    return login
  }

  def setLogin(login: String) {
    this.login = login
  }

  def getPassword: String = {
    return password
  }

  def setPassword(password: String) {
    this.password = password
  }


  def setAllowedAddress(address: String) {
    for (s <- address.split(",").map(_.trim).filter(_.nonEmpty)) {
      debug("allowing to send to: "+ s)
      this.allowedAddress.add(s)
    }
  }


  override def toString = s"MailSender(email=$getEmail, host=$getHost, login=$getLogin, password=$getPassword)"
}