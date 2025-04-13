package ru.sosgps.wayrecall.avlprotocols.ruptela

import java.io._
import com.google.common.base.Charsets
import ru.sosgps.wayrecall.utils.io._
import scala.beans.BeanProperty
import scala.Some
import java.util
import scala.collection.mutable.ListBuffer
import java.util.Date
import ru.sosgps.wayrecall.avlprotocols.ruptela.StateMachine.StateEvent


object StateMachine {

  class StateEvent(val eventSourceName: String, val name: String, val imei: String, val time: Date, val dataSourceName: String)

  type Listener = StateEvent => Unit
  val zeroListener: Listener = (_) => {}
}

trait StateMachine extends grizzled.slf4j.Logging {

  def imei: String

  protected def dataSourceName: String

  protected def eventSourceName: String

  protected class State(val name: String)(f: PartialFunction[Array[Byte], Array[Byte]]) {
    def apply(b: Array[Byte]) = f(b)
  }

  protected def initialState: State

  private[this] var _state: State = null

  val stateListeners = new ListBuffer[StateMachine.Listener]

  protected def changeStateTo(astate: State) = synchronized {
    debug("changing to " + astate.name + " (" + imei + ")")
    notifyStateChange(astate.name)
    this._state = astate
  }


  protected[this] def notifyStateChange(name: String) {
    val stateEvent = new StateEvent(eventSourceName, name, imei, new Date(), dataSourceName)
    stateListeners.foreach(_(stateEvent))
  }

  protected def state = if (_state != null) _state else initialState

}

abstract class GenericFileSender(val imei: String, file: ConfigSource, listener: StateMachine.Listener) extends StateMachine with grizzled.slf4j.Logging {

  if (listener != null && listener != StateMachine.zeroListener)
    stateListeners += listener

  protected[this] var blocksIterator: CurIterator[(Array[Byte], Int)] = new CurIterator(Iterator.empty.buffered)

  protected[this] def started = state != initialState

  protected[this] def haveBlocks = blocksIterator.hasNext

  protected var lastTell: Long = Long.MaxValue

  var maxAnswerWait: Long = 1000 * 60 * 30

  def ask(): Array[Byte] = {
    if (started && ((System.currentTimeMillis() - lastTell) > maxAnswerWait)) {
      warn("changing to initialState by error timeout (" + imei + ") processing pack")
      notifyStateChange("error")
      changeStateTo(initialState)
    }
    Array.empty
  }

  def tell(pack: RuptelaIncomingPackage): Array[Byte] = {
    lastTell = System.currentTimeMillis()
    val bytes = pack.dataBytes
    try {
      state(bytes)
    } catch {
      case e: Exception => {
        debug("changing to initialState by error (" + imei + ") processing pack: " + pack, e)
        notifyStateChange("error")
        file.delete()
        changeStateTo(initialState)
        throw new IllegalArgumentException("state cannot process bytes: " + Utils.toHexString(bytes, " "), e)
      }
    }
  }

  protected[this] def getBlock(i: Int): Array[Byte] = {
    debug("getting block  (" + imei + ")" + i)
    if (!started) {
      blocksIterator.current._1
    }
    else if (blocksIterator.current._2 == i)
      blocksIterator.current._1
    else if (blocksIterator.current._2 + 1 == i) {
      blocksIterator.next()
      blocksIterator.current._1
    }
    else
      throw new IllegalArgumentException("cant get " + i + "-th block when " + blocksIterator.find(_ => true).map(_._2 + "").getOrElse("None") + " is active")
  }


}

/**
 *
 * @param imei
 * @param file
 * @param indexshift package indexShift.
 *                   in wialon for unknown reasons indexes are shifted by 286,
 *                   but in documentation it is equal to 0
 */
class RuptelaFWUpdaterState(imei: String, file: ConfigSource,
                            listener: StateMachine.Listener = StateMachine.zeroListener,
                            @BeanProperty
                            var indexshift: Int = 0) extends GenericFileSender(imei, file, listener) {

  override def ask() = if (!started && file.exists) packFirmwr("|FU_STRT*", 0x0d, 0x0a) else super.ask()

  private[this] val FW_OK = new {
    def unapplySeq(req: Array[Byte]): Option[Seq[Int]] = {
      req match {
        case Array(0x2a, 0x46, 0x55, 0x5f, 0x4f, 0x4b, 0x7c, 0x0d, 0x0a) => Some(Seq.empty)
        case Array(0x2a, 0x46, 0x55, 0x5f, 0x4f, 0x4b, 0x7c, x, y, 0x0d, 0x0a) => Some(Seq((y & 0xff) << 8 | x & 0xff))
        case _ => None
      }
    }
  }

  protected[this] val initialState = new State("initialState")({
    case FW_OK() => {

      val firmvareData = iterateStream(new BufferedInputStream(file.getInputStream), (a: BufferedInputStream) => {
        val buffer = Array.ofDim[Byte](512)
        val len = a.read(buffer)
        if (len == buffer.length) buffer else util.Arrays.copyOf(buffer, len)
      }).iterator

      blocksIterator = new CurIterator(packFirmwrData(firmvareData).zip(Iterator.from(1)).buffered)
      changeStateTo(blockSendingState)
      getBlock(1)
    }
  })

  private[this] val blockSendingState: State = new State("blockSendingState")({
    def processblockreqest(i: Int): Array[Byte] = {
      if (haveBlocks)
        getBlock(i + 1)
      else {
        changeStateTo(finishState)
        packFirmwr("|FU_WRITE*", 0x0d, 0x0a)
      }
    }
    {
      case FW_OK(index) => processblockreqest(index - indexshift)

    }
  })

  private[this] val finishState = new State("finishState")({
    {
      case FW_OK() => {
        notifyStateChange("finished")
        file.delete()
        changeStateTo(initialState)
        Array.empty
      }
    }
  })

  def packFirmwr(msg: String, additionalBytes: Byte*): Array[Byte] = {
    val packdata = 0x68.toByte +: (msg.getBytes(Charsets.US_ASCII) ++ additionalBytes)
    RuptelaParser.packBody(packdata)
  }

  def packFirmwrData(data: Iterator[Array[Byte]]) = {
    for ((a, i) <- data zipWithIndex) yield {

      val crc: Int = CRC16CCITT.invertedKermit(a).asInstanceOf[Int]
      val xs = indexshift + i + 1
      val size = a.length + 6
      packFirmwr("|FU_PCK*",
        Array(size & 0xff, size >> 8, xs & 0xff, (xs >> 8) & 0xff).map(_.toByte) ++
          a ++
          Array(crc & 0x00FF, (crc & 0x0000FF00) >> 8, 0x0d, 0x0a).map(_.toByte): _*)
    }
  }

  protected def eventSourceName = "fwupdater"

  override protected def dataSourceName: String = file.getName
}

class RuptelaConfiguratorState(imei: String,
                               file: ConfigSource,
                               listener: StateMachine.Listener = StateMachine.zeroListener
                                ) extends GenericFileSender(imei, file, listener) with grizzled.slf4j.Logging {


  override def ask = if (!started && file.exists) RuptelaConfigParser.packConfig("#cfg_start@", 0x0D, 0x0A) else super.ask()

  protected[this] class CfgState(name: String)(f: PartialFunction[Array[Byte], Array[Byte]]) extends State(name)(f) {
    override def apply(b: Array[Byte]) = {
      val s = new String(b, Charsets.US_ASCII)
      require(s.startsWith("@cfg_sts#"))
      super.apply(b.drop(9))
    }
  }

  protected[this] val initialState = new CfgState("initialState")({
    case Array(0x31, 0x30, 0x0d, 0x0a) => {
      //_initialMessage = Array.empty
      blocksIterator = new CurIterator(getConfigPacks.zip(Iterator.from(1)).buffered)
      //println("blocksIterator="+blocksIterator.map(b => Utils.toHexString(b._1.takeRight(2)," ")).take(5).toList)
      changeStateTo(blockSendingState)
      getBlock(1)
    }
  })


  private[this] val blockSendingState: State = new CfgState("blockSendingState")({
    def processblockreqest(i: Int): Array[Byte] = {
      if (haveBlocks)
        getBlock(i + 1)
      else {
        changeStateTo(cfgWriteState)
        RuptelaConfigParser.packConfig("#cfg_write@", 0x0D, 0x0A)
      }
    }
    {
      case Array(0x31, i: Byte, 0x0d, 0x0a) => {
        processblockreqest(i)
      }
      case Array(0x30, 0x31, 0x0d, 0x0a) => {
        //val li = blocksIterator.head._2
        warn("strange answer 30 31 0d 0a")
        changeStateTo(initialState)
        ask
        //processblockreqest(li)
      }
    }
  })


  private[this] val cfgWriteState = new CfgState("cfgWriteState")({

    case Array(0x31, 0x30, 0x0d, 0x0a) => {
      changeStateTo(new CfgState("completed")({
        case Array(0x31, 0x30, 0x0d, 0x0a) => {
          notifyStateChange("finished")
          debug("removing sent file (" + imei + ")")
          file.delete()
          changeStateTo(initialState)
          Array.empty
        }
      }))
      RuptelaConfigParser.packConfig("#cfg_end@", 0x0D, 0x0A)
    }

  })

  private[this] def getConfigPacks = {
    def params = RuptelaConfigParser.readParamsBlocks(file.getInputStream)

    (for ((pack, i) <- params.zipWithIndex) yield {
      val packdata = 0x66.toByte +: ("#cfg_send@".getBytes(Charsets.US_ASCII) ++ pack) :+ 0x0d.toByte :+ 0x0a.toByte
      RuptelaParser.packBody(packdata)
    }).iterator
  }

  protected def eventSourceName = "configurator"

  override protected def dataSourceName: String = file.getName
}

class CurIterator[T](iter: Iterator[T]) extends Iterator[T] {
  def hasNext = iter.hasNext

  private[this] var hasValue: Boolean = false

  private[this] var _cur: T = _

  def current = if (hasValue) {
    _cur
  } else next()

  def next() = {
    _cur = iter.next()
    hasValue = true
    _cur
  }
}

trait ConfigSource {

  def getName: String

  def exists: Boolean

  def getInputStream: InputStream

  def delete()

}

