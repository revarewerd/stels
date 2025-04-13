package ru.sosgps.wayrecall.retranslators

import java.net.InetSocketAddress

import ru.sosgps.wayrecall.core.GPSData

/**
  * Created by nickl on 01.10.14.
  */
trait ConfigurableRetranslator {

   def configure(rules: Seq[(InetSocketAddress, (GPSData) => Boolean)])
}
