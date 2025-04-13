package ru.sosgps.wayrecall.utils

import org.axonframework.commandhandling.{CommandCallback, CommandMessage, CommandHandler, CommandBus}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

/**
 * Created by nickl on 19.05.14.
 */
class SpringContextCommandBusWrapper(wrapped: CommandBus) extends CommandBus {

  def this(wrapped: CommandBus, applicationContext: ApplicationContext) = {
    this(wrapped)
    this.applicationContext = applicationContext;
  }

  @Autowired
  var applicationContext: ApplicationContext = null

  def withAppContext(f: => Any): Unit = {
    require(applicationContext != null, "cant process without appcontext")
    try {
      SpringContextCommandBusWrapper.contextHolder.set(applicationContext)
      f
    }
    finally {
      SpringContextCommandBusWrapper.contextHolder.remove()
    }
  }

  def withAppContext(runnable: Runnable): Unit = {
    withAppContext(runnable.run())
  }

  override def dispatch(command: CommandMessage[_]): Unit = withAppContext(wrapped.dispatch(command: CommandMessage[_]))

  override def dispatch[R](command: CommandMessage[_], callback: CommandCallback[R]): Unit = withAppContext(wrapped.dispatch(command: CommandMessage[_], callback: CommandCallback[R]))

  override def subscribe[C](commandName: String, handler: CommandHandler[_ >: C]): Unit = wrapped.subscribe(commandName: String, handler: CommandHandler[_ >: C])

  override def unsubscribe[C](commandName: String, handler: CommandHandler[_ >: C]): Boolean = wrapped.unsubscribe(commandName: String, handler: CommandHandler[_ >: C])
}

object SpringContextCommandBusWrapper {

  private val contextHolder = new ThreadLocal[ApplicationContext]

  def getApplicationContext() = {
    val context = contextHolder.get()
    if (context == null)
      throw new IllegalStateException("no Application Context")
    context
  }

}