package ru.sosgps.wayrecall.sms

import java.util.Date

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 14.04.13
 * Time: 14:37
 * To change this template use File | Settings | File Templates.
 */
trait SmsGate {

  def sendSms(phone: String, text: String): SMS

  def setSmsListener(l: SmsListener)

  def getSmsListener: SmsListener

}

trait SmsListener {

  def onSmsReceived(smses: Seq[SMS]): Unit

  def onSmsSent(sms:SMS): Unit

  /**
   * WARNING: not suppotered by [[ru.sosgps.wayrecall.billing.dealer.SMSPayer]]
   * @param smses
   */
  def onSmsSentBatch(smses: Seq[SMS]): Unit

  def onSmsStatusChanged(smses: Seq[SMS]): Unit

}

class SmsListenersCollection extends SmsListener with grizzled.slf4j.Logging {

  def setListeners(list: java.util.List[SmsListener]): Unit = {
    import scala.collection.JavaConversions.asScalaBuffer
    buffer = list
  }

  private var buffer: mutable.Buffer[SmsListener] = new ListBuffer[SmsListener]()

  private def safeForeach(f: SmsListener => Unit) = {
    for (i <- buffer) try {
      f(i)
    } catch {
      case e: Exception => warn("exception in " + i, e)
    }
  }

  override def onSmsReceived(smses: Seq[SMS]): Unit = safeForeach(_.onSmsReceived(smses))

  override def onSmsSentBatch(smses: Seq[SMS]): Unit = safeForeach(_.onSmsSentBatch(smses))

  override def onSmsStatusChanged(smses: Seq[SMS]): Unit = safeForeach(_.onSmsStatusChanged(smses))

  override def onSmsSent(sms: SMS): Unit = safeForeach(_.onSmsSent(sms))
}
