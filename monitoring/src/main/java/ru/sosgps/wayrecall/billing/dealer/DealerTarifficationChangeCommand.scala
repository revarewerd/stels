package ru.sosgps.wayrecall.billing.dealer

import java.util
import javax.annotation.Resource

import org.axonframework.eventhandling.annotation.EventHandler
import org.axonframework.repository.AggregateNotFoundException
import ru.sosgps.wayrecall.utils.CollectionUtils
import ru.sosgps.wayrecall.utils.io.Utils

import scala.collection.JavaConversions.mapAsScalaMap
import org.axonframework.commandhandling.annotation.{CommandHandler, TargetAggregateIdentifier}
import org.axonframework.eventsourcing.EventSourcingRepository
import org.axonframework.eventsourcing.annotation.{AggregateIdentifier, AbstractAnnotatedAggregateRoot}
import ru.sosgps.wayrecall.core.{WayrecallAxonEvent, CommandEntityInfo}
import collection.JavaConverters.mapAsJavaMapConverter
@SerialVersionUID(1L)
@deprecated("not used now")
class DealerTarifficationChangeCommand(
                                        @TargetAggregateIdentifier val id: String,
                                        val tariffication: java.util.HashMap[String, Long]
                                        ) extends Serializable with CommandEntityInfo {


  override def getEntity(): String = "Dealer"

  override def getEntityId(): Any = id

  override def toString = s"DealerTarifficationChangeCommand($id, $tariffication)";

}

@SerialVersionUID(1L)
@deprecated("not used now")
case class DealerTarifficationChangeEvent(
                                           val id: String,
                                           val tarifficationChange: java.util.HashMap[String, Long]
                                           ) extends Serializable with WayrecallAxonEvent {


  override def toHRString: String = s"DealerTarifficationChangeEvent($id, $tarifficationChange)";

  override def toString = s"DealerTarifficationChangeEvent($id, $tarifficationChange)";

}

@SerialVersionUID(1L)
class DealerBaseTariffChangeCommand(
                                        @TargetAggregateIdentifier val id: String,
                                        val baseTariff: Long
                                        ) extends Serializable with CommandEntityInfo {


  override def getEntity(): String = "Dealer"

  override def getEntityId(): Any = id

  override def toString = s"DealerBaseTariffChangeCommand($id, $baseTariff)";

}

@SerialVersionUID(1L)
case class DealerBaseTariffChangeEvent(
                                           id: String,
                                           baseTariff: Long
                                           ) extends Serializable with WayrecallAxonEvent {


  override def toHRString: String = s"DealerBaseTariffChangeEvent($id, $baseTariff)";

  override def toHRTable: util.Map[String, AnyRef] = Map[String,AnyRef]("Дилер" -> id, "Тариф" -> java.lang.Long.valueOf(baseTariff)).asJava

  override def toString = s"DealerBaseTariffChangeEvent($id, $baseTariff)";

}

@SerialVersionUID(1L)
class DealerBalanceChangeCommand(
                                     @TargetAggregateIdentifier val id: String,
                                   //  val oldbalance: Long,
                                     val amount: Long
                                     ) extends Serializable with CommandEntityInfo {


  override def getEntity(): String = "Dealer"

  override def getEntityId(): Any = id

  override def toString = s"DealerBalanceChangeCommand($id, $amount)";

}

@SerialVersionUID(1L)
case class DealerBalanceChangeEvent(
                                        id: String,
                                        amount: Long
                                        ) extends Serializable with WayrecallAxonEvent {


  override def toHRString: String = s"DealerBalanceChangeEvent($id, $amount)";
  override def toHRTable: util.Map[String, AnyRef] = Map[String,AnyRef]("Дилер" -> id, "Баланс" -> java.lang.Long.valueOf(amount)).asJava

  override def toString = s"DealerBalanceChangeEvent($id, $amount)";

}


class DealerCommandsHandler extends grizzled.slf4j.Logging {

  @Resource(name = "dealerAggregatesRepository")
  var repo: EventSourcingRepository[DealerAggregate] = null

  @CommandHandler
  @deprecated("not used now")
  def tarifficationChangeHandler(command: DealerTarifficationChangeCommand): Unit = {
    val aggregate = loadOrCreateAggregate(command.id)
    aggregate.processTariffChange(command.tariffication)
    debug("tariffChangeHandler:" + command)
  }

  @CommandHandler
  def baseTariffChangeHandler(command: DealerBaseTariffChangeCommand): Unit = {
    val aggregate = loadOrCreateAggregate(command.id)
    aggregate.processBaseTariffChange(command.baseTariff)
    debug("tariffChangeHandler:" + command)
  }

  @CommandHandler
  def baseBalanceHandler(command: DealerBalanceChangeCommand): Unit = {
    val aggregate = loadOrCreateAggregate(command.id)
    aggregate.processBalanceChange(command.amount)
    debug("baseBalanceHandler:" + command)
  }

  @CommandHandler
  def dealerBlocking(command: DealerBlockingCommand): Unit = {
    val aggregate = loadOrCreateAggregate(command.id)
    aggregate.processDealerBlocking(command.block)
    debug("dealerBlockingHandler:" + command)
  }

  private def loadOrCreateAggregate(id: String): DealerAggregate = {
    debug("loading aggregate "+id)
    try {
      repo.load(id)
    } catch {
      case e: AggregateNotFoundException => {
        val aggregate1: DealerAggregate = new DealerAggregate(id)
        repo.add(aggregate1)
        aggregate1
      }
    }
  }
}

class DealerAggregate extends AbstractAnnotatedAggregateRoot with Serializable with grizzled.slf4j.Logging {

  var balance = 0L
  var block = false

  @AggregateIdentifier private var id: String = null

  def this(id: String) = {
    this()
    this.id = id;
  }

  var tar: java.util.HashMap[String, Long] = new util.HashMap[String, Long]()

  private val removeKey = Long.MinValue

  @deprecated("not used now")
  def processTariffChange(newTar: java.util.HashMap[String, Long]): Unit = {
    val removed: util.Map[String, Long] = CollectionUtils.detectChangedMarkRemoved[Long](tar, newTar, removeKey)
    val changed = Utils.ensureJavaHashMap(removed)
    if (!changed.isEmpty)
      this.apply(DealerTarifficationChangeEvent(id, changed))
  }

  def processBaseTariffChange(l: Long) = {
    require(id != null,"id cant be null")
    this.apply(DealerBaseTariffChangeEvent(id, l))
  }

  @EventHandler
  def tarrificationChanged(event: DealerTarifficationChangeEvent) {
    debug("appling " + event)
    this.id = event.id;

    tar.putAll(event.tarifficationChange)

    event.tarifficationChange.toIterator.filter(_._2 == removeKey)
      .foreach(a => tar.remove(a._1))

    debug("tar is " + tar)

  }

  @EventHandler
  def baseTariffChangeChanged(event: DealerBaseTariffChangeEvent) {
    debug("appling " + event)
    this.id = event.id;
    //debug("balance is " + balance)
  }

  def processBalanceChange(amount: Long) = {
    this.apply(DealerBalanceChangeEvent(id,amount))
  }

  @EventHandler
  def balanceChanged(event: DealerBalanceChangeEvent) {
    debug("appling " + event)
    this.id = event.id;

    this.balance = this.balance + event.amount

    debug("balance is " + balance)

  }

  def processDealerBlocking(block: Boolean) = {
    this.apply(DealerBlockingEvent(id,block))
  }

  @EventHandler
  def dealerBlocking (event: DealerBlockingEvent) {
    debug("appling " + event)
    this.id = event.id;

    this.block = event.block

    debug("blocking dealer is " + block)

  }


}
