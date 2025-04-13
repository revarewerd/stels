package ru.sosgps.wayrecall.avlprotocols.common

import io.netty.buffer.ByteBuf
import ru.sosgps.wayrecall.avlprotocols.navtelecom.NavtelecomParser._
import ru.sosgps.wayrecall.core.GPSData

import scala.collection.mutable

/**
  * Created by nickl-mac on 27.02.16.
  */
trait StoredDeviceCommandsQueue[CommandType <: StoredDeviceCommand] {



  protected def sendingCommands(): mutable.Seq[CommandType] = {
    val sendingCommands = commandsToSend.dequeueAll(_ => true)
    commandsToAnswer.enqueue(sendingCommands: _*)
    sendingCommands
  }

  private val commandsToAnswer = mutable.Queue[CommandType]()

  private val commandsToSend = mutable.Queue[CommandType]()

  def enqueueCommands(c: CommandType*) = commandsToSend.enqueue(c: _*)

  def hasPendingCommands = !(commandsToSend.isEmpty && commandsToAnswer.isEmpty)

  protected def notifyCommands(g: GPSData): mutable.Seq[CommandType] = {
    commandsToAnswer.dequeueAll(c => {
      c.accept(g);
      c.answerAccepted
    })
  }

  protected def notifyCommands(retrievedGPSses: Seq[GPSData]): mutable.Seq[CommandType] = {
    commandsToAnswer.dequeueAll(c => retrievedGPSses.exists(gpsdata => {
      c.accept(gpsdata); c.answerAccepted
    }));
  }

  protected def notifyCommands(body: Array[Byte]): mutable.Seq[CommandType] = {
    commandsToAnswer.dequeueAll(c => {
      c.accept(body);
      c.answerAccepted
    })
  }

}
