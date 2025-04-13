package ru.sosgps.wayrecall.data.sleepers

import java.text.DateFormat

import org.joda.time.DateTime
import ru.sosgps.wayrecall.sms.SMS
import java.util.{Locale, Date}
import ru.sosgps.wayrecall.utils.CollectionUtils.additionalMethods

@SerialVersionUID(1L)
class SleeperData(val uid: String, val history: Map[String, Stream[SMS]]) extends Serializable with grizzled.slf4j.Logging {

  val time: Option[Date] = history.values.flatMap(_.headOption).reduceOption((a, b) => if (a.sendDate.getTime > b.sendDate.getTime) a else b).map(_.sendDate)

  val missTimeWarnings: Map[String, Option[String]] = history.mapValues(smses => {
    val expectedDate = SleeperParser.expectedDate(smses)
    //debug(uid + " expectedDate=" + expectedDate + " " + smses.headOption.map(_.senderPhone).orNull)
    expectedDate.map(_.before(new DateTime().toDate)).map {
      case true => "Пропущено сообщение " + expectedDate.map(localeFormat).orNull
      case false => ""
    }
  })

  val l:Locale = new Locale("ru","RU");

  private def localeFormat: (Date) => String = {
    DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, l).format
  }

  val sleeperState: SleeperState = {
    lazy val latestHasAlarm = latestMatchResult.flatMap(_.alarm).isDefined
    if ( missTimeWarnings.values.exists(_.exists(_.nonEmpty)) || latestHasAlarm) {
      val missTimeInfo = missTimeWarnings collect {
        case (k, v) if v.isDefined && v.get.nonEmpty => (k, v.get)
      }
      Warning(
        if (latestHasAlarm)
          missTimeInfo + ((latest.get._1.senderPhone, latestMatchResult.get.alarm.get.toString))
        else missTimeInfo
      )
    }
    else if (missTimeWarnings.values.exists(_.map(_.isEmpty).getOrElse(false))) //TODO: но вообще тут должно быть что-то более строгое
      OK
    else
      Unknown
  }


  lazy val lastIsMatched: Boolean = history.values.flatMap(_.headOption).maxByOpt(_.sendDate).exists(s => SleeperParser.parseReport(s.text).toOption.nonEmpty)

  private lazy val latest: Option[(SMS, MatchResult)] = {
    val matched: Map[String, Option[(SMS, MatchResult)]] = history.mapValues(hs => hs.map(sms => (sms, SleeperParser.parseReport(sms.text).toOption)).collect({
      case (sms, Some(matcher)) => (sms, matcher)
    }).headOption)

    matched.values.flatten.maxByOpt(_._1.sendDate)
  }


  def batValue = latest.flatMap(_._2.batValue)

  def batPercentage = latest.flatMap(_._2.batPercentage)

  def latestMatchResult: Option[MatchResult] = latest.map(_._2)

}

sealed abstract class SleeperState {
  def stateName: String
}

case object Unknown extends SleeperState{
  def stateName = "Unknown"
}

case class Warning(info: Map[String, String]) extends SleeperState {
  def stateName = "Warning"
}

case object OK extends SleeperState{
  def stateName = "OK"
}