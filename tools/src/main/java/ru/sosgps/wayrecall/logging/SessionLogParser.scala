package ru.sosgps.wayrecall.logging


import java.io.{File, BufferedInputStream, FileInputStream}

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.joda.time.DateTime

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source


object SessionLogParser {


  // 2015-01-22 18:02:29:565 (qs=0) - loading session for


  val headerReg = """(\d\d\d\d)-(\d\d)-(\d\d) (\d\d):(\d\d):(\d\d):(\d\d\d) \(qs=(\d)\) - (.*)""".r

  val stline = """\s+at.*""".r
  val multilineParam = """\s+.*""".r

  type Param = (String, String)

  type StackTrace = String


  private def parseStackTrace(data: Stream[String]): (Option[StackTrace], Stream[String]) = {
    if (data.take(2).size < 2) return (None, data)

    val (trace, tail) = data.tail.span(stline.pattern.matcher(_).matches())
    if (trace.nonEmpty)
      (Some((data.head #:: trace).mkString("\n")), tail)
    else
      (None, data)
  }

  private def aggregate(funcs: ((Stream[String]) => (Option[String], Stream[String]))*)(data: Stream[String])
  : (Seq[Option[String]], Stream[String]) = {
    var tail = data
    val r = funcs.map(f => {
      val (r, ftail) = f(data)
      tail = ftail
      r
    })
    (r, tail)
  }

  private def allErrorsAndParams(data: Stream[String]): (Seq[Either[Param, StackTrace]], Stream[String]) = {

    var tail = data
    var prevTail: Stream[String] = null;

    val result = new ListBuffer[Either[Param, StackTrace]]

    while (tail ne prevTail) {
      prevTail = tail
      val (st, t) = parseStackTrace(tail)
      tail = t
      st.foreach(v => result += Right(v))
      val (p, t1) = parseParam(tail)
      tail = t1
      p.foreach(v => result += Left(v))
    }

    (result.toList, tail)
  }

  private def parseParam(data: Stream[String]): (Option[Param], Stream[String]) = {
    if (data.isEmpty) return (None, data)

    val s = data.head.indexOf(":")
    if (s != -1) {
      val paramName = data.head.substring(0, s)

      val (otherLines, tail) = data.tail.span(multilineParam.pattern.matcher(_).matches())

      (Some(paramName, (data.head.substring(s + 1) #:: otherLines).mkString("\n")), tail)
    }
    else
      (None, data)
  }

  def parse(data: Stream[String]): Stream[SessionEntry] = {
    if (data.isEmpty) return Stream.empty

    data.head match {
      case headerReg(year, month, day, hour, minutes, seconds, mills, qs, text) =>
        //println("parsed:" + data.head)
        val date = new DateTime(year.toInt, month.toInt, day.toInt, hour.toInt, minutes.toInt, seconds.toInt, mills.toInt)
        val (trace, tail) = parseStackTrace(data.tail)

        val (others, tail1) = allErrorsAndParams(tail)

        SessionEntry(
          date, qs.toInt, text, trace, others.collect({ case Left(param) => (param._1, param._2)}).toMap
        ) #:: parse(tail1)
      case _ => parse(data.tail)
    }
  }

  def parseFile(file: File): Stream[SessionEntry] = {
    val fileStream = new BufferedInputStream(new FileInputStream(file))

    val is = if (file.getName.endsWith(".gz"))
      new GzipCompressorInputStream(fileStream)
    else
      fileStream

    def linesTail = Source.fromInputStream(is, "UTF-8").getLines().toStream

    val entries = parse(linesTail)
    entries
  }


  //val file = new File("/home/nickl/Загрузки/ses-94.25.131.211-2015-01-22-18-02-29-542.txt.gz") // 18200


}

case class SessionEntry(date: DateTime, qs: Int, text: String, stacktrace: Option[String], attrs: Map[String, String])