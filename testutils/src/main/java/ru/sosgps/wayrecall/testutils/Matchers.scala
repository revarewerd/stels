package ru.sosgps.wayrecall.testutils

import java.util

import io.netty.buffer.ByteBuf
import org.hamcrest.{Matcher, Description, BaseMatcher}
import ru.sosgps.wayrecall.core.{GPSDataConversions, GPSData}
import ru.sosgps.wayrecall.utils.ScalaNettyUtils
import ru.sosgps.wayrecall.utils.io.Utils
import ScalaNettyUtils.ByteBufOps

import scala.collection.JavaConversions.mapAsScalaMap
/**
  * Created by nmitropo on 19.12.2015.
  */
object Matchers {

  def bin(str: String): Matcher[ByteBuf] = new BinaryMatcher(str)

  def isBin(str: String): Matcher[ByteBuf] = bin(str)

  def containsDataOf(expected: GPSData) = new GPSIncludeMatcher(expected)

}

class BinaryMatcher(expected: String) extends BaseMatcher[ByteBuf] {
  override def matches(o: scala.Any): Boolean = o match {
    case bb: ByteBuf => util.Arrays.equals(Utils.asBytesHex(expected), bb.toIteratorReadable.toArray)
    case _ => false
  }

  override def describeMismatch(item: AnyRef, description: Description): Unit = {
    description.appendText("was ").appendValue(toString(item))
  }


  private def toString(item: AnyRef): String = {
    item match {
      case bb: ByteBuf => bb.toHexString;
      case str: String => try Utils.toHexString(Utils.asBytesHex(str), " ") catch {
        case e: Exception => str
      }
      case _ => item.toString
    }
  }

  override def describeTo(description: Description): Unit = {
    description
      .appendValue(toString(expected))
  }

}

class GPSIncludeMatcher(expected: GPSData) extends BaseMatcher[GPSData] {
  override def matches(item: scala.Any): Boolean = item match {
    case gps: GPSData => {
      gps.equalsIngoringData(expected, true) && expected.data.forall({case (k,v) => gps.data.get(k) == v})

    }
  }

  override def describeMismatch(item: AnyRef, description: Description): Unit = {
    description.appendText("was ").appendText(toString(item))
  }

  private def toString(item: AnyRef): String = {
    item match {
      case gps: GPSData => DataHelpers.toScalaCode(gps)
      case _ => item.toString
    }
  }

  override def describeTo(description: Description): Unit = description
    .appendText(toString(expected))
}