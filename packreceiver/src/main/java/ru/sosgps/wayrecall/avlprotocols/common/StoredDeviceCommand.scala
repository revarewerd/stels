package ru.sosgps.wayrecall.avlprotocols.common

import ru.sosgps.wayrecall.avlprotocols.navtelecom.NavtelecomCommand
import ru.sosgps.wayrecall.core.GPSData

import scala.concurrent.{Future, Promise}
import scala.reflect.ClassTag

/**
  * Created by nickl-mac on 27.02.16.
  */
trait StoredDeviceCommand {

  def text: String

  def accept(gpsData: GPSData): Unit

  def accept(binary: Array[Byte]): Unit

  def answerAccepted: Boolean

  def unwrap[T <: StoredDeviceCommand : ClassTag] = this.asInstanceOf[T]

}

class ListenableStoredCommand(wrapped: StoredDeviceCommand) extends StoredDeviceCommand {

  private val acceptPromise = Promise[Boolean]()

  override def text: String = wrapped.text

  override def answerAccepted: Boolean = wrapped.answerAccepted

  override def accept(gpsData: GPSData): Unit = {
    wrapped.accept(gpsData)
    tryCompletePromise()
  }

  override def accept(binary: Array[Byte]): Unit = {
    wrapped.accept(binary)
    tryCompletePromise()
  }

  private def tryCompletePromise() {
    if (answerAccepted && !acceptPromise.isCompleted)
      acceptPromise.success(true)
  }

  def future = acceptPromise.future

  override def unwrap[T <: StoredDeviceCommand : ClassTag]: T = this match {
    case t: T => t
    case _ => wrapped.unwrap[T]
  }
}

object ListenableStoredCommand {
  implicit def asFuture(cmd: ListenableStoredCommand): Future[Boolean] = cmd.future
}