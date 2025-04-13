package ru.sosgps.wayrecall.domain.balance

import java.util.Date

import com.novus.salat.annotations._
import org.bson.types.ObjectId


// сейчас делаю два раздельных класса для истории. При записи хинтов в объекты в базе, детали можно сделать полиморфными


case class DailyPayEntry(account: ObjectId,
                               @Key("ammount") amount: Long,
                               @Key("type") addType: String,
                               timestamp: Date,
                               newbalance: Long,
                               comment: String,
                               details: DailyPayEntryDetails)

case class SMSNotificationPaymentEntry(account: ObjectId,
                                       @Key("ammount") amount: Long,
                                       @Key("type") addType: String,
                                       timestamp: Date,
                                       newbalance: Long,
                                       comment: String,
                                       details: SMSNotificationDetails)

case class SMSNotificationDetails(user: String, phone: String)




case class DailyPayEntryDetails(objectDetails: List[ObjectDetails] = List.empty, additionalPrice: List[AdditionalPrice] = List.empty)
case class ObjectDetails(uid: String, name: String, equipments: List[EquipmentDetails] = Nil)
case class EquipmentDetails(@Key("_id") id: ObjectId, eqIMEI: String, @Key("eqtype")eqtype: String, todayCost: Long)
case class AdditionalPrice(name: String, todayCost: Long)
