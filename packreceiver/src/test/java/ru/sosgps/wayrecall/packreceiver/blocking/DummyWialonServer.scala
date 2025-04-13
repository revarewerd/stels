package ru.sosgps.wayrecall.packreceiver.blocking

import ru.sosgps.wayrecall.packreceiver.DummyPackSaver

import ru.sosgps.wayrecall.utils.runnable

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 08.10.13
 * Time: 17:33
 * To change this template use File | Settings | File Templates.
 */
object DummyWialonServer {


  def main(args: Array[String]) {

    val wialonPackReceiver = new WialonPackReceiver(new DummyPackSaver, 10012)
    wialonPackReceiver.setDaemon(false)
    wialonPackReceiver.start()

    Runtime.getRuntime.addShutdownHook(new Thread(runnable{
      wialonPackReceiver.stop();
    }))


  }

}
