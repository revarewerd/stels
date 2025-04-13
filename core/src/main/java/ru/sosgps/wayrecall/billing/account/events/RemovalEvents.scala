package ru.sosgps.wayrecall.billing.account.events

import java.io.Serializable
import java.util

import org.bson.types.ObjectId
import ru.sosgps.wayrecall.core.WayrecallAxonEvent

/**
  * Created by ivan on 04.12.15.
  */
class AccountDeleteEvent(val accountId: ObjectId) extends Serializable with WayrecallAxonEvent {


  def toHRString: String = {
    return ""
  }


//  override def toHRTable: util.Map[String, AnyRef] = super.toHRTable

  def getAccountId: ObjectId = {
    return accountId
  }
}

class AccountRemoveEvent(val accountId: ObjectId, val options: java.util.Map[String, java.lang.Boolean]) extends Serializable with WayrecallAxonEvent {
  def toHRString: String = {
     ""
  }


 // override def toHRTable: util.Map[String, AnyRef] = super.toHRTable

  def getOption(opt: String): Boolean = {
      options.containsKey(opt) && options.get(opt)
  }


  def getAccountId: ObjectId = {
    accountId
  }
}

class AccountRestoreEvent(val accountId: ObjectId) extends Serializable with WayrecallAxonEvent {

 // override def toHRTable: util.Map[String, AnyRef] = super.toHRTable

  def toHRString: String = {
    return ""
  }

  def getAccountId: ObjectId = {
    return accountId
  }
}