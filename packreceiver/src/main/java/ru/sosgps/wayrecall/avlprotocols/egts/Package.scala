package ru.sosgps.wayrecall.avlprotocols.egts
import io.netty.buffer.ByteBuf

/**
 * Created by nickl on 26.01.15.
 */
trait Package {

  def read(src: ByteBuf):this.type

  def write(buffer1: ByteBuf): ByteBuf
}
