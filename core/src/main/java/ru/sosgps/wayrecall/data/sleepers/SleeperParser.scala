package ru.sosgps.wayrecall.data.sleepers

import java.util.Date

import ru.sosgps.wayrecall.sms.SMS
import com.mongodb.casbah.Imports._
import java.net.URL

import scala.util.{Failure, Success, Try}


case class LBS(MCC: Int, MNC: Int, LAC: Int, CID: Int) {
  def toHexString: String = "LBShex(MCC=" + MCC + ", MNC=" + MNC + ", LAC=0x" + LAC.toHexString + ", CID=0x" + CID.toHexString + ")"
}

trait MatchResult {

  def lbs: Option[LBS]

  def positionURL: Option[URL]

  def lonlat: Option[(Double, Double)]

  lazy val hasPositionData: Boolean = (lbs orElse lonlat orElse positionURL).isDefined

  def alarm: Option[Alarm]

  //def apply(v1: String): Option[String]

  def batValue: Option[String]

  def batPercentage: Option[String]

  def canEqual(other: Any): Boolean = other.isInstanceOf[MatchResult]

  override def toString(): String = "LBS=" + lbs.map(_.toString).getOrElse("None") +
    " lonlat=" + lonlat.map(_.toString).getOrElse("None") +
    " alarm=" + alarm.map(_.toString).getOrElse("None") +
    " posurl=" + this.positionURL.map(_.toString.take(10) + "...").getOrElse("None") +
    " batValue=" + batValue +
    " batPercentage=" + batPercentage

  override def equals(obj: scala.Any): Boolean = obj match {
    case other: MatchResult =>
      this.lbs == other.lbs &&
        this.positionURL == other.positionURL &&
        this.lonlat == other.lonlat &&
        this.alarm == other.alarm &&
        this.batValue == other.batValue &&
        this.batPercentage == other.batPercentage
    case _ => false
  }

  def contaisAllOf(other: MatchResult): Boolean = {
    (this.lbs == other.lbs || other.lbs.isEmpty) &&
      (this.positionURL == other.positionURL || other.positionURL.isEmpty) &&
      (this.lonlat == other.lonlat || other.lonlat.isEmpty) &&
      (this.alarm == other.alarm || other.alarm.isEmpty) &&
      (this.batValue == other.batValue || other.batValue.isEmpty) &&
      (this.batPercentage == other.batPercentage || other.batPercentage.isEmpty)
  }
}

abstract class Alarm

case object Moving extends Alarm

case object Power extends Alarm

case object LightInput extends Alarm




object SleeperParser extends grizzled.slf4j.Logging {

  val parserCombinators = List(
    AutoPhoneSleeperParserCombinator,
    PromaSleeperParserCombinator,
    AutoPhoneRusSleeperParserCombinator,
    AlphaMayakSleeperParserCombinator,
    StarLineSleeperParserCombinator,
    VegaSleeperParserCombinator)

  val milsinday = 1000L * 60 * 60 * 24

  private def parseWithCombinators(text: String, remaining: List[SleeperParserCombinator], errors: List[Exception]): Either[Seq[Exception], MatchResult] = {
    remaining match {
      case Nil => Left(new NoSuchElementException("no parsers") :: errors)
      case head :: tail =>
        head(text) match {
          case head.Success(result: MatchResult, _) => Right(result)
          case head.NoSuccess(mess, left) =>
            val errorsNew = new IllegalArgumentException(head + ": " + mess + " left:" + left.source.subSequence(left.pos.column - 1, left.source.length())) :: errors
            if (tail.nonEmpty) {
              parseWithCombinators(text, tail, errorsNew)
            }
            else
              Left(errorsNew)
        }
    }
  }

  def parseReport(text: String): Try[MatchResult] = {
    val textPreprocessed = text.replaceAll("\n", " ")
    val parsedWithCombinators = try {
      parseWithCombinators(textPreprocessed, parserCombinators, Nil)
    } catch {
      case e: Exception =>
        error(s"error parsing $textPreprocessed with combinators", e)
        Left(List(e))
    }

    parsedWithCombinators match {
      case Right(matchResult) =>
        Success(matchResult)
      case Left(errors) =>
        Failure(new IllegalArgumentException(errors.map(_.getMessage).mkString("\n") + "\n text was:" + text))
    }
  }

  def timeIntervals(dates: Seq[Date]): Seq[Long] = {
    if (dates.length < 3)
      return Seq.empty

    val times = dates.map(_.getTime)
    val intervals = times.sliding(2).map(s => {
      val l = s(1) - s(0)
      assert(l >= 0, "dates was not sorted")
      l
    })

    intervals.map(i => (i + milsinday / 5) / milsinday).filter(_ > 0).toSeq
  }


  def expectedDate(smses: Seq[SMS]): Option[Date] = {
    val dates = smses.filter(sms => parseReport(sms.text) match {
      case Failure(e) => false
      case Success(mr) => mr.alarm.isEmpty
    }).map(_.sendDate).take(5).sorted
    for (
      lastDate <- dates.lastOption;
      days <- detectInterval(dates)
    ) yield new Date(lastDate.getTime + days * milsinday + 1000 * 60 * 30)
  }

  def detectInterval(dates: Seq[Date]): Option[Int] = {
    val roundToDays: Seq[Long] = timeIntervals(dates)
    if (roundToDays.isEmpty)
      return None
    //debug("roundToDays:"+roundToDays.groupBy(a => a).mapValues(_.length))
    val mostFreqDays = roundToDays.groupBy(a => a).maxBy(t => (t._2.size, t._1))._1
    Some(mostFreqDays.toInt)
  }

}
