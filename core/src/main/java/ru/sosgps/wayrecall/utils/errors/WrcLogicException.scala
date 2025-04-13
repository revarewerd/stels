package ru.sosgps.wayrecall.utils.errors

/**
  * Created by nickl on 02.04.15.
  */
class WrcLogicException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)

  def this() = this(null)
}

class UserInputException(message: String, cause: Throwable) extends WrcLogicException(message, cause) {
  def this(message: String) = this(message, null)

  def this() = this(null)
}

class ImpossibleActionException(message: String, cause: Throwable) extends WrcLogicException(message, cause) {
  def this(message: String) = this(message, null)

  def this() = this(null)
}

class NotPermitted(message: String, cause: Throwable) extends WrcLogicException(message, cause) {
  def this(message: String) = this(message, null)

  def this() = this(null)
}

class BadRequestException(message: String, cause: Throwable) extends IllegalArgumentException(message, cause) {
  def this(message: String) = this(message, null)

  def this() = this(null)
}

object BadRequestException {

  def unapply(ex: Throwable): Option[BadRequestException] = ex match {
    case wrc: BadRequestException => Some(wrc)
    case nonwrc => Option(nonwrc.getCause).flatMap(unapply)
  }

}

object UnwrappedWrcLogicException {

  def unapply(ex: Throwable): Option[WrcLogicException] = ex match {
    case wrc: WrcLogicException => Some(wrc)
    case nonwrc => Option(nonwrc.getCause).flatMap(unapply)
  }

}