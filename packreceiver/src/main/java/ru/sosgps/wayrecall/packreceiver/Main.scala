package ru.sosgps.wayrecall.packreceiver

import ru.sosgps.wayrecall.jcommanderutils.MultiCommandCli
import ru.sosgps.wayrecall.regeocoding.ReGeocoderRunner

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 03.01.13
 * Time: 15:17
 * To change this template use File | Settings | File Templates.
 */
object Main extends MultiCommandCli {
  val commands = Seq(ReceiverServer)
}
