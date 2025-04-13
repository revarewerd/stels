package ru.sosgps.wayrecall.tools.wialon.packaging

import scala.io.Source

/**
 * Created by nickl on 20.02.15.
 */
object ThreadDumpReader {

  def main(args: Array[String]) {

    val traces = Source.fromFile("/home/nickl/Загрузки/ttt.tdump").getLines().mkString("\n").split("\n\n").toIndexedSeq

    println(traces.size)

    traces.sorted.foreach(println)

  }

}
