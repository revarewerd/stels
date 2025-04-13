package ru.sosgps.wayrecall.core

import java.io.Serializable

import org.bson.types.ObjectId

/**
 * Created by IVAN on 08.11.2016.
 */
class TicketUserReadStatusChangedEvent(val ticketId: ObjectId, val read:Boolean) extends WayrecallAxonEvent {
  def toHRString()={
    "user read status: "+read+" for ticketId: "+ticketId
  }

  override def toString = "TicketReadStatusChangedEvent(ticketId = " + ticketId + ", read:" + read+")"
}

class TicketSupportReadStatusChangedEvent(val ticketId: ObjectId, val read:Boolean) extends WayrecallAxonEvent {
  def toHRString()={
    "support read status: "+read+" for ticketId: "+ticketId
  }

  override def toString = "TicketReadStatusChangedEvent(ticketId = " + ticketId + ",read:" + read+")"
}

class SupportTicketDeletedEvent(val ticketId: ObjectId) extends WayrecallAxonEvent {
  def toHRString() = {
    "ticket with id:" + ticketId + " was deleted"
  }

  override def toString = "TicketDeletedEvent(ticketId = " + ticketId + ")"
}