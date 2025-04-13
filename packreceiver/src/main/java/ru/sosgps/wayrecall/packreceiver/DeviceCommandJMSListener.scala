package ru.sosgps.wayrecall.packreceiver

import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.avlprotocols.common.{DeviceCommanderJMS, DeviceCommander, ListenableStoredCommand}
import ru.sosgps.wayrecall.avlprotocols.navtelecom.{NavtelecomCommand, PasswordCommand}
import ru.sosgps.wayrecall.sms.{DeviceCommandException, DeviceCommand}
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService

import scala.util.{Failure, Success}
import scala.collection.JavaConversions.collectionAsScalaIterable

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by nickl-mac on 09.03.16.
  */
class DeviceCommandJMSListener extends DeviceCommanderJMS with grizzled.slf4j.Logging{


  @Autowired
  var deviceCommnders: java.util.List[DeviceCommander] = null


  def receiveCommand(d: DeviceCommand): Unit = {
    try {
      debug("received command:" + d)

      deviceCommnders.find(_.connectedTo(d.target)) match {
        case Some(commander) => commander.receiveCommand(d).onComplete{
          case Success(r) => notifySuccess(r)
          case Failure(e) => e match {
            case e: DeviceCommandException => notifyFailure(e)
            case `e` => notifyFailure(new DeviceCommandException(d, e))
          }
        }
        case None =>
          warn(s"no connected devices for ${d.target}")
          notifyFailure(new DeviceCommandException(d, new NoSuchElementException(s"no commanders connected to ${d.target}")))
      }

    }
    catch {
      case e:Exception => notifyFailure(new DeviceCommandException(d,e))
    }

  }

}
