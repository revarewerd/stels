package ru.sosgps.wayrecall.billing.dealer

import javax.annotation.PostConstruct
import javax.jms.{ObjectMessage, Message, MessageListener}


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import ru.sosgps.wayrecall.core.InstanceConfig
import ru.sosgps.wayrecall.sms.{SMS, SmsListener}
import ru.sosgps.wayrecall.utils.{typingMap, typingMapJava}

import scala.beans.BeanProperty
import scala.collection.immutable.ListMap

/**
 * Created by nickl on 29.12.14.
 */
class SMSPayer extends SmsListener with DealersManagementMixin with grizzled.slf4j.Logging {

  @BeanProperty
  var smsOwner: String = "default"

  @Autowired
  var jmsTemplate: JmsTemplate = null

  override def onSmsReceived(smses: Seq[SMS]): Unit = {}

  private var resolvedSmsOwner: Option[String] = None

  @PostConstruct
  def init(): Unit = {
    resolvedSmsOwner = resolveSmsOwner(mscfg.instances.find(_.name == this.instanceName).get).map(_.name)
    if (resolvedSmsOwner.exists(_ == instanceName))
      resolvedSmsOwner = None
    debug("resolvedSmsOwner = " + resolvedSmsOwner)
  }


  override def onSmsSent(sms: SMS): Unit = {
    resolvedSmsOwner.foreach(resolvedSmsOwner => {
      val target = resolvedSmsOwner + ".externalCommand"
      val command = ListMap(
        "command" -> "payforSMS",
        "count" -> 1,
        "dealerId" -> this.instanceName
      )
      debug("publiching paying " + target + " " + command)
      jmsTemplate.convertAndSend(target, command)
    })
  }

  override def onSmsSentBatch(smses: Seq[SMS]): Unit = {

    info("batch sms sending paying was disabled beause of false calls with m2mcommunicator")

    //mscfg.instances.find(_.name == this.instanceName).flatMap(_.)
    //    resolvedSmsOwner.foreach(resolvedSmsOwner => {
    //      val target = resolvedSmsOwner + ".externalCommand"
    //      val command = ListMap(
    //        "command" -> "payforSMS",
    //        "count" -> smses.size,
    //        "dealerId" -> this.instanceName
    //      )
    //      debug("publiching paying " + target + " " + command)
    //      jmsTemplate.convertAndSend(target, command)
    //    })
  }

  override def onSmsStatusChanged(smses: Seq[SMS]): Unit = {}

  def resolveSmsOwner(cfg: InstanceConfig): Option[InstanceConfig] = {
    if (cfg.name == smsOwner)
      return Some(cfg)
    else {
      val parentCgf = cfg.properties.getAs[String]("instance.parent")
        .flatMap(parent => mscfg.instances.find(_.name == parent))

      return parentCgf.flatMap(resolveSmsOwner)
    }
  }
}

class ClientSmsPaysProcessor extends MessageListener with grizzled.slf4j.Logging {


  @Autowired
  var dealersTariffPlans: DealersTariffPlans = null


  def onMessage(p1: Message) {
    p1 match {
      case m: ObjectMessage => {
        m.getObject match {
          case externalCommand: scala.collection.Map[String, Any] => {
            debug("received external command " + externalCommand)

            if (externalCommand("command") == "payforSMS") {
              dealersTariffPlans.ensurePermissions()
              dealersTariffPlans.changeBalance(
                externalCommand.as[String]("dealerId"),
                externalCommand.as[Int]("count") * -400,
                "sms payment", "n=" + externalCommand.as[Int]("count")
              )
            }

          }
          case e: Any => warn("received unrecognized Message with object" + e)
        }
      }
      case _ => warn("received unrecognized message")
    }
  }

}
