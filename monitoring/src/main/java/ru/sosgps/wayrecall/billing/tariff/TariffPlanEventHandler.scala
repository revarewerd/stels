package ru.sosgps.wayrecall.billing.tariff

import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.eventhandling.annotation.EventHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import ru.sosgps.wayrecall.billing.tariff.events.{TariffPlanCreateEvent, TariffPlanDataSetEvent, TariffPlanDeleteEvent}
import ru.sosgps.wayrecall.utils.salat_context._
import com.novus.salat._
import scala.collection.JavaConversions.mapAsScalaMap

/**
  * Created by IVAN on 29.03.2017.
  */

@Order(-100)
class TariffPlanEventHandler extends grizzled.slf4j.Logging {

  @Autowired
  val tariffDAO: TariffDAO = null

  @Autowired
  val commandGateway: CommandGateway = null

  @EventHandler
  def handleTariffCreatedEvent(event: TariffPlanCreateEvent) {
    debug("Tariff plan create=" + event.getTariffId)
    debug("data=" + event.getData)
    val tariff = grater[Tariff].fromMap(Map("_id" -> event.getTariffId) ++ event.getData)
    tariffDAO.insert(tariff)
  }

  @EventHandler
  def handleTariffDataSetEvent(event: TariffPlanDataSetEvent) {
    val tariffId = event.getTariffId
    val changes = event.getData
    debug("Tariff plan data set tariffId=" + event.getTariffId)
    debug("data=" + event.getData)
    tariffDAO.update(tariffId, changes.toMap)

  }

  @EventHandler
  def handleTariffDeleteEvent(event: TariffPlanDeleteEvent) {
    debug("Delete tariff plan tariffId=" + event.getTariffId)
    tariffDAO.remove(event.getTariffId)
  }

}
