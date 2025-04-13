package ru.sosgps.wayrecall.sms

import org.apache.xbean.spring.context.ClassPathXmlApplicationContext
import org.junit.Ignore

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 08.04.13
 * Time: 23:36
 * To change this template use File | Settings | File Templates.
 */
@Ignore
object SmsReceiverTest {

  def main(args: Array[String]) {

    val context = new ClassPathXmlApplicationContext("m2msms-spring-test.xml")
    context.registerShutdownHook();

    val commandProcessor = context.getBean(classOf[SMSCommandProcessor])

//    commandProcessor.sendCommand("79160899516", new SmsConversation(Seq(
//      new TeltonikaParamCommand("21s21", "5803", "1245", "91.230.215.12"),
//      new TeltonikaParamCommand("21s21", "5803", "1246", "9088")
//    )))
//
//    commandProcessor.sendCommand("79151191720", new SmsConversation(Seq(
//      new TeltonikaParamCommand("21s21", "5803", "1245", "91.230.215.12"),
//      new TeltonikaParamCommand("21s21", "5803", "1246", "9088")
//    )))

    println("storedConversations=")
    commandProcessor.loadConversations().foreach(println)


    val seq = Seq(
      new SMSCommand {
        def acceptResponse(s: SMS) = s.text.contains("abc")

        val text = "please send me abc"
      },
      new SMSCommand {
        def acceptResponse(s: SMS) = s.text.contains("mnk")

        val text = "please send me mnk"
      }
    )

    commandProcessor.sendCommand(new SmsConversation("79164108305", seq))


  }

}
