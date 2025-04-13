package ru.sosgps.wayrecall.avlprotocols.gosafe

import ru.sosgps.wayrecall.utils.io.Utils
import com.google.common.base.Charsets

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 26.05.13
 * Time: 17:30
 * To change this template use File | Settings | File Templates.
 */
trait GosafePacket {
  def isText: Boolean

  def data: Array[Byte]
}

case class GosafeBinaryPacket(data: Array[Byte]) extends GosafePacket {
  val isText = false

  override def toString = "GosafeBinaryPacket("+Utils.toHexString(data,"")+")"
}

case class GosafeTextPacket(data: Array[Byte]) extends GosafePacket {
  val isText = true

  lazy val text = new String(data, Charsets.US_ASCII)

  override def toString =  "GosafeTextPacket(" + text + ")"
}