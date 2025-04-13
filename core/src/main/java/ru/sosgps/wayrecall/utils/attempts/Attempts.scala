package ru.sosgps.wayrecall.utils.attempts


sealed abstract class Action;

case object Skip extends Action

case object Retry extends Action

case object Succeed extends Action

class AttemptsExhaustedException(str: String) extends Exception(str)

class Attempts(
                var handler: PartialFunction[Throwable, Action] = {case e: Throwable => throw e},
                var maxRetryAttempts: Int = 10,
                var maxSkip: Int = 5,
                var skipAfterRetries: Boolean = false
                ) {

  var skipped = 0;
  var retryAttemt = 0;
  def doTry[T](f: => T): Option[T] = {

    var result: Option[T] = None;
    var callState: Action = Retry
    retryAttemt = 0;
    while (callState == Retry) {
      callState = {
        try {
          result = Some(f)
          Succeed
        }
        catch handler
      }

      def processSkip() = {
        skipped = skipped + 1;
        if (skipped > maxSkip)
          throw new AttemptsExhaustedException("Skip enhausted " + skipped + " of " + maxSkip)
      }

      callState match {
        case Retry =>
          retryAttemt = retryAttemt + 1
          if (retryAttemt > maxRetryAttempts) {
            if (skipAfterRetries) {
              retryAttemt = 0;
              callState = Skip
              processSkip()
            }
            else {
              throw new AttemptsExhaustedException("Retry attemts enhausted " + retryAttemt + " of " + maxRetryAttempts)
            }
          }
        case Skip => processSkip()
        case Succeed => skipped = 0;
      }
    }

    result

  }

}