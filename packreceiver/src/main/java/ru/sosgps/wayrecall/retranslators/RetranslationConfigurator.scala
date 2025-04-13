package ru.sosgps.wayrecall.retranslators

/**
 * Created by nickl on 01.10.14.
 */
trait RetranslationConfigurator {

  def configure(r: ConfigurableRetranslator): Unit

}
