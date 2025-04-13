package ru.sosgps.wayrecall.processing.lazyseq

import java.util
import java.util.Date

import org.joda.time.DateTimeUtils
import ru.sosgps.wayrecall.core.GPSData

/**
 * Created by nickl on 02.04.15.
 */
class CachingGPSHistoryWalker(src: GPSHistoryWalker) extends GPSHistoryWalker with grizzled.slf4j.Logging {


  private object loadCached {
    private val cachetree = new util.TreeMap[Date, LoadedNode]() {

      //      def get(k:UnloadedNodeState):Option[LoadedNode] = k  match {
      //        case Before(date) => Option(lowerEntry(date)).map(_.getValue)
      //        case After(date) => Option(higherEntry(date)).map(_.getValue)
      //        case At(date) => Option(get(date))
      //      }

    }

    import scala.collection.JavaConversions.asScalaIterator

    def printNodes: String = cachetree.entrySet().iterator().map(e => e.getKey + " : " + e.getValue).mkString("\n")

    def put(gpsData: GPSData): LoadedNode = synchronized {
      val n: LoadedNode = new LoadedNode(gpsData)
      src.put(gpsData)
      assert(cachetree.put(gpsData.time, n) == null, " already have " + gpsData.time)
      n
    }

    private def tryLoadNext(date: Date): Node = {
      src.next(date).map(putIfNotExists).getOrElse(EmptyNode: Node)
    }

    private def tryLoadPrev(date: Date): Node = {
      src.prev(date).map(putIfNotExists).getOrElse(EmptyNode: Node)
    }

    private def putIfNotExists(gpsData: GPSData): LoadedNode =
      synchronized {
        Option(cachetree.get(gpsData.time)).map(_.ensuring(_.gps == gpsData)).getOrElse(put(gpsData))
      }

    private def chainDebug[T](message: String)(value: T): T = {
      debug(message + ":" + value);
      value
    }

    def apply(q: NodeQuery): Node = synchronized {
      //debug("loading " + q)
      q match {
        case Before(date) =>
          Option(cachetree.lowerEntry(date))
            .filterNot(_.getValue.next.gpsOpt.exists(_.time.before(date)))
            .map(_.getValue).getOrElse({
            //debug("not floor trying ceiling " + q)
            Option(cachetree.ceilingEntry(date)) match {
              case Some(ceiling) =>
                //debug("our ceiling: " + ceiling)
                val r = if (ceiling.getKey == date) {
                  //debug("our ceiling.getKey equal to date")
                  ceiling.getValue.prevOpt.getOrElse(ceiling.getValue.updatePrev(tryLoadPrev(date)))
                } else {
                  //debug("our ceiling.getKey not equal to date")
                  ceiling.getValue.prevOpt. /*filter(_.gps.time.before(date)).*/ getOrElse(tryLoadPrev(date))
                }
                //debug("so result is: " + r)
                r
              case None => tryLoadPrev(date)
            }
          })

        case After(date) =>
          Option(cachetree.higherEntry(date)).map(_.getValue)
            //.filterNot(_.prevOpt.flatMap(_.gpsOpt).exists(_.time.after(date)))
            //.filter(_.prevOpt.flatMap(_.gpsOpt).exists(!_.time.after(date)))
            .flatMap(v => v.prevOpt match {
            case Some(prev) if prev.gpsOpt.exists(!_.time.after(date)) => Some(v)
            case Some(prev) if prev.gpsOpt.isEmpty => Some(v)
            //case None => Some(v)
            case _ => None
          })
            .getOrElse({
              //debug("not upper trying floor " + q)
              Option(cachetree.floorEntry(date)) match {
                case Some(floor) =>
                  if (floor.getKey == date) {
                    floor.getValue.nextOpt.getOrElse(floor.getValue.updateNext(tryLoadNext(date)))
                  } else {
                    floor.getValue.nextOpt. /*filter(_.gps.time.before(date)).*/ getOrElse(tryLoadNext(date))
                  }
                case None => tryLoadNext(date)
              }
            })
        case At(date) =>
          //Option(cachetree.get(date)).getOrElse(src.at(date).map(put).getOrElse(EmptyNode: Node))
          Option(cachetree.get(date))
            .orElse(
              Option(cachetree.lowerEntry(date))
                .flatMap(_.getValue.nextOpt) match {
                case Some(nextNode) if nextNode.gpsOpt.exists(_.time.after(date)) => Some(EmptyNode: Node)
                case Some(nextNode) if nextNode.gpsOpt.exists(_.time.equals(date)) => Some(nextNode)
                case _ => None
              }
            ).getOrElse(src.at(date).map(put).getOrElse(EmptyNode: Node))


        //        case Before(date) => new LoadedNode(src.prev(date).map(Loaded).getOrElse(NotExists))
        //        case After(date) => new LoadedNode(src.next(date).map(Loaded).getOrElse(NotExists))
        //        case At(date) => new LoadedNode(src.at(date).map(Loaded).getOrElse(NotExists))
      }

    }

    def remove(gpsData: GPSData): LoadedNode = {
      cachetree.remove(gpsData.time)
    }

    def nodes: List[LoadedNode] = synchronized {
      cachetree.values().iterator().toList
    }

  }


  trait Node {
    def gpsOpt: Option[GPSData]

    def gps: GPSData

    def next: Node

    def prev: Node

    def evict(): Unit
  }

  class LoadedNode(_gps: GPSData) extends Node {

    var lastAccess: Long = DateTimeUtils.currentTimeMillis()

    def gps = {
      lastAccess = DateTimeUtils.currentTimeMillis()
      _gps
    }

    def gpsOpt: Option[GPSData] = Some(gps)

    private[this] var _next: Option[Node] = None
    private[this] var _prev: Option[Node] = None

    def nextOpt = _next

    def prevOpt = _prev

    def next = _next match {
      case Some(value) => value
      case None => synchronized {
        _next match {
          case Some(value) => value
          case None => {
            //debug("loading next for " + gps.time)
            updateNext(loadCached(After(gps.time)))
          }
        }
      }
    }

    def prev = _prev match {
      case Some(value) => value
      case None => synchronized {
        _prev match {
          case Some(value) => value
          case None => {
            //debug("loading prev for " + gps.time)
            updatePrev(loadCached(Before(gps.time)))
          }
        }
      }
    }


    def updateNext(next: Node, recur: Boolean = true): Node = {
      //debug("updating next of " + this.gps.time + " with " + next.gpsOpt.map(_.time).getOrElse("None"))
      if (recur)
        next match {
          case ln: LoadedNode => ln.updatePrev(this, false)
          case _ =>
        }
      _next = Some(next)
      next
    }


    def updatePrev(prev: Node, recur: Boolean = true): Node = {
      //debug("updating prev of " + this.gps.time + " with " + prev.gpsOpt.map(_.time).getOrElse("None"))
      if (recur)
        prev match {
          case ln: LoadedNode => ln.updateNext(this, false)
          case _ =>
        }
      _prev = Some(prev)
      prev
    }

    def evict(): Unit = {
      prevOpt match {
        case Some(l: LoadedNode) => l.forgetNext()
        case _ =>
      }
      nextOpt match {
        case Some(l: LoadedNode) => l.forgetPrev()
        case _ =>
      }
      //debug("before evicting " + gps.time + " :\n" + loadCached.printNodes)
      assert(loadCached.remove(gps).gpsOpt.get == gps)
    }

    private def forgetNext() = _next = None

    private def forgetPrev() = _prev = None

    override def toString: String = prevOpt.map(_.gpsOpt.map(_.time).getOrElse("NE")).getOrElse("NL") +
      " <= (" + System.identityHashCode(this) + ")" + gpsOpt.map(_.time).getOrElse("NE") + " => " +
      nextOpt.map(_.gpsOpt.map(_.time).getOrElse("NE")).getOrElse("NL")

  }

  val EmptyNode = new Node {
    override def gps = ???

    override def gpsOpt = None

    override def next: LoadedNode = ???

    override def prev: LoadedNode = ???

    override def toString: String = "EmptyNode"

    override def evict(): Unit = {}
  }

  //private abstract sealed class NodeState(val loaded: Boolean)

  abstract sealed class NodeQuery

  //extends NodeState(loaded = false)

  //private abstract sealed class LoadedNodeState(val gpsOpt: Option[GPSData]) extends NodeState(loaded = true)

  case class Before(date: Date) extends NodeQuery

  case class After(date: Date) extends NodeQuery

  case class At(date: Date) extends NodeQuery

  //private case class Loaded(gps: GPSData) extends LoadedNodeState(Some(gps))

  // private case object NotExists extends LoadedNodeState(None)

  override def prev(date: Date): Option[GPSData] = getNode(Before(date)).gpsOpt

  override def next(date: Date): Option[GPSData] = getNode(After(date)).gpsOpt

  override def atOrNext(date: Date): Option[GPSData] = getNode(At(date)).gpsOpt.orElse(next(date))

  override def put(data: GPSData): Unit = {
    //debug("putting " + data.time)
    val r = loadCached.put(data)
    //debug("after put before loading neighbours:\n" + loadCached.printNodes)
    r.next
    r.prev
    //debug("after put all:\n" + loadCached.printNodes)
  }

  override def at(date: Date): Option[GPSData] = loadCached(At(date)).gpsOpt

  def getNode(q: NodeQuery): Node = loadCached(q)

  def nodes = loadCached.nodes

}