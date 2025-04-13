package ru.sosgps.wayrecall.avlprotocols.navtelecom

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.JmsTemplate
import ru.sosgps.wayrecall.avlprotocols.common.{DeviceCommanderJMS, DeviceCommander, ListenableStoredCommand}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.packreceiver.netty.{NavtelecomAvlDataDecoder, NavTelecomNettyServer}
import ru.sosgps.wayrecall.sms.{IOSwitchDeviceCommand, DeviceCommandsBatch, DeviceCommandException, DeviceCommand}
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService

import scala.collection.mutable
import scala.concurrent
import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}

/**
  * Created by nickl on 21.11.14.
  */
class NavTelecomCommander extends DeviceCommander with grizzled.slf4j.Logging {

  implicit var ectxt = new ScalaExecutorService("NavTelecomCommander", 1, 1, false, 1, TimeUnit.DAYS, new LinkedBlockingQueue[Runnable](100)).executionContext

  @Autowired
  var navtelecomServer: NavTelecomNettyServer = null

// Похоже что теперь Сигналы работают без аутентификации... даже из документации убрали этот раздел
//  private var autorized = new mutable.WeakHashMap[NavtelecomConnectionProcessor, Boolean]()
//    with mutable.SynchronizedMap[NavtelecomConnectionProcessor, Boolean]


  override def connectedTo(imei: String): Boolean = navtelecomServer.activeConnections.devices.isDefinedAt(imei)

  def receiveCommand(d: DeviceCommand): Future[DeviceCommand] = {
    val promise = Promise[DeviceCommand]
    try {
      debug("received command:" + d)

      val connection = navtelecomServer.activeConnections(d.target).connproc
      //if (autorized.get(connection).getOrElse(false)) {
      debug("sending directly command:" + d)
      promise.completeWith(sendMainCommand(d))
      //}
      //else {
      //  val command = new ListenableStoredCommand(new PasswordCommand(d.password)) with NavtelecomCommand
      //  debug("sending password command:" + d)
      //  navtelecomServer.sendCommand(d.target, command)
      //  command.onComplete {
      //    case Success(true) => {
      //      debug("sending password command success:" + d)
      //      autorized(connection) = true
      //      promise.completeWith(sendMainCommand(d))
      //    }
      //    case Failure(t) => promise.failure(new DeviceCommandException(d, t))
      //    case _ => warn("unexpected result for:" + d)
      //  }
      //}
    }
    catch {
      case e: Exception => promise.failure(new DeviceCommandException(d, e))
    }

    promise.future

  }


  private def sendMainCommand(d: DeviceCommand): Future[DeviceCommand] = {
    val promise = Promise[DeviceCommand]
    d match {

      case d: DeviceCommandsBatch => {

        val commands = d.batch.iterator.map(d => new ListenableStoredCommand(new IOSwitchCommand(commandText(d))) with NavtelecomCommand)

        def sendNext(): Unit = try{
          val command = commands.next()
          debug("sending subcommand:" + command.text)
          navtelecomServer.sendCommand(d.target, command)
          command.onComplete {
            case Success(true) => if (commands.hasNext) sendNext() else promise.success(d)
            case Success(false) => promise.failure(new DeviceCommandException(d, new IllegalArgumentException("unexpected answer")))
            case Failure(t) => promise.failure(new DeviceCommandException(d, t))
          }(ScalaExecutorService.implicitSameThreadContext)
        } catch {
          case e: Exception => promise.failure(e)
        }

        sendNext()
      }

      case d: DeviceCommand => {
        debug("sending main command:" + d + " of class " + d.getClass)
        val command = new ListenableStoredCommand(new IOSwitchCommand(commandText(d))) with NavtelecomCommand
        navtelecomServer.sendCommand(d.target, command)
        command.onComplete {
          case Success(true) => promise.success(d)
          case Success(false) => promise.failure(new DeviceCommandException(d, new IllegalArgumentException("unexpected answer")))
          case Failure(t) => promise.failure(new DeviceCommandException(d, t))
        }
      }
    }
    promise.future
  }

  private def commandText(cmd: DeviceCommand): String = cmd match {
    case ioswc: IOSwitchDeviceCommand => "!" + ioswc.num.getOrElse(2) + (ioswc.state match {
      case true => "Y"
      case false => "N"
    })

    case d: DeviceCommand => d.text
  }


}


