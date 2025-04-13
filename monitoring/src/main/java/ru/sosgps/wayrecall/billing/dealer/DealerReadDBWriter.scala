package ru.sosgps.wayrecall.billing.dealer

import org.axonframework.eventhandling.annotation.EventHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.Order
import ru.sosgps.wayrecall.core.MongoDBManager

import com.mongodb.casbah.Imports._
import scala.collection.JavaConversions.mapAsScalaMap

@Order(-100)
class DealerReadDBWriter extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null


  @EventHandler
  def tarrificationChanged(event: DealerTarifficationChangeEvent): Unit = {

    debug("updating db with " + event)

    mdbm.getDatabase()("dealers").update(MongoDBObject("id" -> event.id),
      $set(event.tarifficationChange.toSeq.map(kv => ("tariffication." + kv._1, kv._2)): _*),
      upsert = true
    )

  }

  @EventHandler
  def baseTariffChanged(event: DealerBaseTariffChangeEvent): Unit = {

    debug("updating db with " + event)

    mdbm.getDatabase()("dealers").update(MongoDBObject("id" -> event.id),
      $set("baseTariff" -> event.baseTariff),
      upsert = true
    )

  }

  @EventHandler
  def baseBalanceChanged(event: DealerBalanceChangeEvent): Unit = {

    debug("updating db with " + event)

    mdbm.getDatabase()("dealers").update(MongoDBObject("id" -> event.id),
      $inc("balance" -> event.amount),
      upsert = true
    )

  }

  @EventHandler
  def dealerBlockingChanged(event: DealerBlockingEvent): Unit = {

    debug("updating db with " + event)

    mdbm.getDatabase()("dealers").update(MongoDBObject("id" -> event.id),
      $set("block" -> event.block),
      upsert = true
    )

  }



}
