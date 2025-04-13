package ru.sosgps.wayrecall.core.axon

/**
  * Created by ivan on 23.02.16.
  */
object AggregateType {
  val AccountAggregate = "AccountAggregate"
  val ObjectAggregate = "ObjectAggregate"
  val EquipmentAggregate = "EquipmentAggregate"
  val EquipmentTypesAggregate = "EquipmentTypesAggregate"
  val PermissionAggregate = "PermissionAggregate"
  val UserAggregate = "UserAggregate"
  val TariffPlanAggregate = "TariffPlanAggregate"
  val DealerAggregate = "DealerAggregate"
  val TicketAggregate = "TicketAggregate"

  private val withName = Set(
    AccountAggregate, ObjectAggregate, EquipmentAggregate,
    EquipmentTypesAggregate, PermissionAggregate, UserAggregate,
    TariffPlanAggregate, DealerAggregate)

  val values = Set(AccountAggregate, ObjectAggregate, EquipmentAggregate,
    EquipmentTypesAggregate, PermissionAggregate, UserAggregate,
    TariffPlanAggregate, DealerAggregate, TicketAggregate)

  def hasName(agType: String) =
    withName.contains(agType)
}