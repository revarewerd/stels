package ru.sosgps.wayrecall.workflow

import java.io.Serializable

import org.bson.types.ObjectId
import ru.sosgps.wayrecall.core.CommandEntityInfo
import scala.collection.JavaConverters._
/**
 * Created by IVAN on 15.01.2015.
 */

class TicketCreateCommand(val id: ObjectId, val data: Map[String, Serializable]) extends CommandEntityInfo {
  def this(id: ObjectId,
           data: java.util.Map[String, Serializable]) = {
    this(id, data.asScala.toMap)
  }

  def getEntity() = {
    "Ticket"
  }

  def getEntityId() = {
    this.id
  }

  override def toString = "TicketCreatedCommand(id = " + id + ", data = " + data + ")"
}

class TicketDataSetCommand(val id: ObjectId, val data: Map[String, Serializable]) extends CommandEntityInfo {
  require(id != null, "id must not be null")
  require(data != null, "ticketData must not be null")

  def this(id: ObjectId,
           data: java.util.Map[String, Serializable]) = {
    this(id, data.asScala.toMap)
  }

  def getEntity() = {
    "Ticket"
  }

  def getEntityId() = {
    this.id
  }

  override def toString = "TicketDataSetCommand(id = " + id + ", data = " + data + ")"
}

class TicketDeleteCommand(val id: ObjectId) extends CommandEntityInfo {
  def getEntity() = {
    "Ticket"
  }

  def getEntityId() = {
    this.id
  }

  override def toString = "TicketDeleteCommand(id = " + id + ")"
}

