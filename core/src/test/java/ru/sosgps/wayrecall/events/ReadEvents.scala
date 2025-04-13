package ru.sosgps.wayrecall.events

import java.io.FileInputStream
import java.util.Date
import java.util.zip.GZIPInputStream

import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.utils.io.DboReader

import scala.io.Source

/**
 * Created by nickl on 20.07.15.
 */
object ReadEvents {

  def main(args: Array[String]) {

    new DboReader(new GZIPInputStream(new FileInputStream("/home/nickl/Рабочий стол/events.bson.gz"))).iterator
      .flatMap(dbo => utils.io.tryDeserialize[Event](dbo.get("event").asInstanceOf[Array[Byte]]).right.toOption)
      .collect {
      case um: UserMessage if um.message == "Объект вошел из геозоны «ВЫЕЗД В МО»."
      => (new Date(um.time) + um.toString)
    }
      .foreach(println)

  }

}
