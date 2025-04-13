package ru.sosgps.wayrecall.sms

import java.util.concurrent.atomic.AtomicInteger
import java.util.{Timer, TimerTask}

/**
 * Created by nickl on 25.11.14.
 */
//TODO: лучше эмулировать через имитацию JMS
class MockGprsCommandProcessor extends GprsCommandProcessor{


  var answerTimeOut = -1

  var answerSuccess = true;

  val sentCount = new AtomicInteger(0)

  override protected def doSend(d: DeviceCommand) {
    sentCount.incrementAndGet()
    if (answerTimeOut > 0)
      new Timer().schedule(new TimerTask {
        override def run(): Unit = receive(d)
      }, answerTimeOut)


  }
}
