package ru.sosgps.wayrecall.billing.dealer

import java.util

import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier
import ru.sosgps.wayrecall.core.{WayrecallAxonEvent, CommandEntityInfo}
import collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.JavaConversions.mapAsScalaMap


/**
 * Created by IVAN on 30.03.2016.
 */
@SerialVersionUID(1L)
class DealerBlockingCommand (@TargetAggregateIdentifier val id: String,  val block: Boolean)
  extends Serializable with CommandEntityInfo {
    override def getEntity(): String = "Dealer"
    override def getEntityId(): Any = id
    override def toString = s"DealerBlockingCommand($id, $block)";
}

@SerialVersionUID(1L)
case class DealerBlockingEvent(id: String,val block: Boolean)
  extends Serializable with WayrecallAxonEvent {

  override def toHRString: String = s"DealerBalanceChangeEvent($id, $block)";
  override def toHRTable: util.Map[String, AnyRef] = Map[String,AnyRef]("Дилер" -> id, "Заблокирован" -> java.lang.Boolean.valueOf(block)).asJava
  override def toString = s"DealerBalanceChangeEvent($id, $block)";
}