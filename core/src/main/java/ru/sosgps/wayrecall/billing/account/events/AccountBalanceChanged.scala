package ru.sosgps.wayrecall.billing.account.events

import java.util

import org.bson.types.ObjectId
import ru.sosgps.wayrecall.core.WayrecallAxonEvent
import collection.JavaConverters.mapAsJavaMapConverter
/**
 * Created by nickl on 30.06.14.
 */
case class AccountBalanceChanged(id: ObjectId, balance: Long) // Эм, что?

case class AccountStatusChanged(id: ObjectId, status: Boolean) extends WayrecallAxonEvent {
  def toHRString() = "Аккаунт " + (if (status) "включен" else "отключен")

  override def toHRTable: util.Map[String, AnyRef] = Map[String,AnyRef]("Включен" -> (if (status) "Да" else "Нет")).asJava

  override def toString = "AccountEnabledChanged(id = " + id + ", status = " + status + ")"
}
