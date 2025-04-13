package ru.sosgps.wayrecall.sms

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 10.04.13
 * Time: 14:47
 * To change this template use File | Settings | File Templates.
 */
@SerialVersionUID(8898161069505603780L)
class TeltonikaParamCommand(val login: String, val password: String, val paramid: String, val paramValue: String) extends SMSCommand {
  val text = login + " " + password + " setparam " + paramid + " " + paramValue

  private[this] lazy val regex = """Param ID:(\d+) (?:New Text|Text|New Val|Val):(.+)""".r

  def acceptResponse(s: SMS) = {
    s.text match {
      case regex(id, value) => if (id == paramid) {
        if (value != paramValue) SmsConversation.logger.warn("Incorrect Param:" + value + ", but currect paramid:" + id)
        true
      } else
        false
      case _ => false
    }

  }
}
