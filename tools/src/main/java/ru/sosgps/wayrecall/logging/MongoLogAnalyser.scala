package ru.sosgps.wayrecall.logging

import java.util.Locale

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatterBuilder, DateTimeFormat}
import org.parboiled2._
import shapeless.{::, HNil}

import scala.io.Source
import scala.util.{Failure, Success}


class LogStringParser(val input: ParserInput) extends Parser {
  val frmt = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss.SSS").withLocale(Locale.US).withDefaultYear(2015)
  // .withYear(DateTime.now().getYear)

  //val frmt = new DateTimeFormatterBuilder().appendDayOfWeekText().appendPattern()

  //println(frmt.parseDateTime("Sun May 10 23:53:02.730"))

  def Date = rule {
    capture(3.times(CharPredicate.Alpha) ~ ' ' ~ 3.times(CharPredicate.Alpha) ~ oneOrMore(' ') ~
      (1 to 2).times(CharPredicate.Digit) ~ ' ' ~ oneOrMore(CharPredicate.Digit | ':' | '.')) ~> (dstr => frmt.parseDateTime(dstr.replaceAll("  ", " ")))
  }

  def ManCommand = rule {
    Date ~ Spaces ~ "[conn" ~ oneOrMore(CharPredicate.Digit) ~ "]" ~ Spaces ~
      """command admin.$cmd command""" ~ Text ~ ExecutionTime ~> ((date: DateTime, text: String, i) => ManualCommand(date, i))
  }

  def DbOperation = rule {Date ~ Text ~ ExecutionTime ~> ((date: DateTime, text: String, i) => DbOperationEntry(date, text, i))}

  def Flushing = rule {Date ~ " [DataFileSync] flushing mmaps took " ~ Time ~ Text ~ EOI ~> ((date: DateTime, i, text: String) => FlushingMap(date, i))}

  def InputLine: Rule[HNil, ::[Entry, HNil]] = rule {ManCommand | Flushing | DbOperation}

  def Text = rule {capture(zeroOrMore(!ExecutionTime ~ CharPredicate.Visible | ' ' | '«' | '»' | CharPredicate.from(_.isLetter)))}

  def Time = rule {capture(oneOrMore(CharPredicate.Digit)) ~ "ms" ~> (_.toInt)}

  def ExecutionTime: Rule[HNil, ::[Int, HNil]] = rule {Time ~ EOI}

  def Spaces = rule {oneOrMore(' ')}

  //  def InputLine = rule { Expression ~ EOI }
  //
  //  def Expression: Rule1[Int] = rule {
  //    Term ~ zeroOrMore(
  //      '+' ~ Term ~> ((_: Int) + _)
  //        | '-' ~ Term ~> ((_: Int) - _))
  //  }
  //
  //  def Term = rule {
  //    Factor ~ zeroOrMore(
  //      '*' ~ Factor ~> ((_: Int) * _)
  //        | '/' ~ Factor ~> ((_: Int) / _))
  //  }
  //
  //  def Factor = rule { Number | Parens }
  //
  //  def Parens = rule { '(' ~ Expression ~ ')' }
  //
  //  def Number = rule { capture(Digits) ~> (_.toInt) }
  //
  //  def Digits = rule { oneOrMore(CharPredicate.Digit) }
}


object MongoLogAnalyser {

  implicit class ops(seq: Seq[Int]){
    def avr = seq.sum / seq.size
  }

  def main(args: Array[String]) {
    println(new LogStringParser(
      """Sat Apr 18 07:56:35.016 [conn11193] update Seniel-dev2.objPacks.buffer.A query: { _id: ObjectId('5531e3dfcd1ab6bd1df8054f'), uid: "o1992497763948899069", imei: "356307041379479", time: new Date(1429332930790), lon: 38.965344, lat: 55.8769408, spd: 94, crs: 66, stn: 9, insTime: new Date(1429332939496), data: { 66: 14040, pwr_ext: 14.04, 1: 1, protocol: "Teltonika", 179: 0, alt: 134.0, 180: 0, mh: 210397860, tdist: 1372.835644728513, 240: 1 } } update: { $set: { pn: "М-7" } } nscanned:1 nupdated:1 keyUpdates:0 locks(micros) w:408 1624ms""" //       """.stripMargin
    ).InputLine.run())

    println(new LogStringParser(
      """Sat Apr 25 20:20:06.534 [DataFileSync] flushing mmaps took 21317ms  for 1001 files""" //       """.stripMargin
    ).InputLine.run())

    println(new LogStringParser(
      """Tue May 19 17:37:10.579 [conn13969] command admin.$cmd command: { fsync: 1 } ntoreturn:1 keyUpdates:0 locks(micros) W:8714 reslen:51 1331ms""" //       """.stripMargin
    ).InputLine.run())

    val iterator: Iterator[Entry] = (for (line <- Source.fromFile("/home/nickl/Загрузки/mongo.log.2", "utf8").getLines()) yield {
      new LogStringParser(line).InputLine.run() match {
        case Success(ok: Entry) => /*println(ok);*/ Some(ok)
        case Failure(err) => /*println(line + " " + err);*/ None
      }
    }).flatten

 //   val day = new DateTime(2015, 5, 19, 0, 0, 0)
    val day = new DateTime(2015, 5, 11, 0, 0, 0)
    val inDay = iterator.filter((entry: Entry) => {
      entry.date.withTime(0, 0, 0, 0) == day
    })

    val grouped = inDay.toStream.groupBy(e => e.date.getMinuteOfDay / 15).mapValues(entries => {
      entries.groupBy(_.getClass).mapValues(_.map(_.time))
    })

    for (i <- grouped.keys.min to grouped.keys.max) {

      val aggregated = grouped.getOrElse(i, Map.empty)
      println(i + "\t"
        + aggregated.getOrElse(classOf[DbOperationEntry], Stream(0)).max  + "\t"
        + aggregated.getOrElse(classOf[DbOperationEntry], Stream(0)).avr + "\t"
        + aggregated.getOrElse(classOf[FlushingMap], Stream(0)).max + "\t"
        + aggregated.getOrElse(classOf[FlushingMap], Stream(0)).avr + "\t"
        + aggregated.getOrElse(classOf[ManualCommand], Stream(0)).max
      )

    }

  }

}

abstract sealed class Entry {
  def date: DateTime

  def text: String

  def time: Int
}

case class DbOperationEntry(date: DateTime, text: String, time: Int) extends Entry

case class FlushingMap(date: DateTime, time: Int) extends Entry {
  override val text: String = "flushing mmap"
}

case class ManualCommand(date: DateTime, time: Int) extends Entry {
  override val text: String = "command admin.$cmd command"
}