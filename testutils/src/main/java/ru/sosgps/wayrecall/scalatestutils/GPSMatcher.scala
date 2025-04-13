package ru.sosgps.wayrecall.scalatestutils

import org.scalatest.matchers.{MatchResult, Matcher}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.testutils.DataHelpers

import scala.collection.JavaConversions.mapAsScalaMap

/**
  * Created by nickl-mac on 03.09.16.
  */
trait GPSMatcher {

  class GPSMatcher(expected: GPSData) extends Matcher[GPSData] {

    def apply(gps: GPSData): MatchResult = {
      MatchResult(
        gps.equalsIngoringData(expected, true) && expected.data.forall({ case (k, v) => gps.data.get(k) == v }),
        s"""gps "${DataHelpers.toScalaCode(gps)}" did not match expected "${DataHelpers.toScalaCode(expected)}"""",
        s"""gps "${DataHelpers.toScalaCode(gps)}" matches "${DataHelpers.toScalaCode(expected)}""""
      )
    }
  }

  def matchGPS(expected: GPSData) = new GPSMatcher(expected)

  class GPSesMatcher(expected: Seq[GPSData]) extends Matcher[Seq[GPSData]] {

    val sorter = (g: GPSData) => g.time

    def gpsSeq(seq: Seq[GPSData]) = seq.map(gps => DataHelpers.toScalaCode(gps)).mkString("Seq(\n",",\n",")")

    def apply(gpss: Seq[GPSData]): MatchResult = {
      MatchResult(
        expected.size == gpss.size &&
          expected.sortBy(sorter).zip(gpss.sortBy(sorter)).forall(
            { case (expected, gps) =>  gps.equalsIngoringData(expected, true) &&
              expected.data.forall({ case (k, v) => gps.data.get(k) == v })}
          ),
        s"""gps "${gpsSeq(gpss)}" did not match expected "${gpsSeq(expected)}"""",
        s"""gps "${gpsSeq(gpss)}" matches "${gpsSeq(expected)}""""
      )
    }
  }

  def matchGPSsOrdered(expected: Seq[GPSData]) = new GPSesMatcher(expected)

}

object GPSMatcher extends GPSMatcher