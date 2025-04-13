/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.tools.wialon.dataconverters

import com.mongodb.casbah.commons.MongoDBObject
import java.io.File
import scala.io.Source
import collection.mutable
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import com.mongodb.casbah.MongoDB
import ru.sosgps.wayrecall.tools.Main
import com.beust.jcommander.{Parameter, Parameters}

//TODO: убрать это апи нафиг и перейти на прямую загруку с сервера как в WialonObjectsSync

object AccountsTableParser extends TableParser {

  val attrmap = Map(
    "№" -> None,
    "Учётная запись" -> Some("name"),
    "Администратор" -> None,
    "Баланс" -> Some("balance"),
    "Валюта" -> Some("currency"),
    "Тарифные планы" -> Some("plan"),
    "Объекты" -> None,
    "Пользователи" -> None,
    "Действия" -> None
  )


  def process(file:File): Unit = {

    val parsedmaps = parseLinesToMap(file).toStream

    val b: MongoDB = Main.getMongoDatabase()
    val collection = b.apply("accounts")
    collection.dropCollection()

    parsedmaps.foreach(e => e("balance") = (e("balance").toString.toFloat * 100).toInt)
    parsedmaps.foreach(elem => {
      collection.insert(MongoDBObject(elem.toList))
    })

    println("Hello, world!")
  }

}

object ObjectsTableParser extends TableParser {


  val attrmap = Map(
    "№" -> None,
    "Объект" -> Some("name"),
    "ID" -> None,
    "Создатель" -> None,
    "Локальное хранилище" -> None,
    "Тип устройства" -> Some("equipmentType"),
    "Тип оборудования" -> Some("equipmentType"),
    "Уникальный идентификатор" -> Some("uid"),
    "Уникальный ID" -> Some("uid"),
    "Телефон" -> Some("phone"),
    "Учётная запись" -> Some("accountName"),
    "Соединения" -> None,
    "Пробег" -> None,
    "Последнее сообщение" -> None,
    "Последняя позиция" -> None,
    "Последнее положение" -> None,
    "Действия" -> None
  )



  def process(file:File): Unit = {

    val dB = Main.getMongoDatabase()
    val accounts = dB.apply("accounts")
    val collection = dB.apply("objects")
    collection.dropCollection()

    for (pairs <- parseLinesToMap(file)) {
      val pairsu = pairs.updated("account", accounts.findOne(MongoDBObject("name" -> pairs("accountName")), MongoDBObject("_id" -> 1)).map(_.get("_id")).getOrElse(""))
      collection.insert(MongoDBObject(pairsu.toList))
    }

    println("Hello, world!")
  }

}

trait TableParser {

  val attrmap: Map[String, Option[String]]

  def process(file:File): Unit

  @deprecated
  def println(x: Any)= {}

  def parseLinesToMap(file: File): Iterator[mutable.Map[String, Any]] = parseLinesToMap(file, attrmap)

  private[this] def parseLinesToMap(file: File, attrmap: String => Option[String]): Iterator[mutable.Map[String, Any]] = {
    val lines = Source.fromFile(file, "UTF8").getLines;
    val haders = lines.next().split("\t").toIndexedSeq;

    haders.map("\"" + _ + "\"").foreach(println)

    (for (line <- lines) yield {
      val split = line.split("\t")

      if (split.length == haders.length) {
        val m = haders.zip(split).toMap

        println(haders.map(k => k + "->" + m(k)).mkString("\n"))

        val pairs = mutable.HashMap[String, Any](m.filterKeys(attrmap(_).isDefined).map({
          case (k, v) => {
            (attrmap(k).get, v)
          }
        }).toSeq: _ *)

        Some(pairs)
      }
      else {
        System.err.println("is not parseble (" + split.length + " != " + haders.length + ") line: \"" + line + "\"")
        None
      }
    }).flatten
  }

}

@Parameters(commandDescription = "Updates objects data from files")
object TableUpserter extends CliCommand{
  val commandName = "wlnupsert"

  @Parameter(names = Array("-i", "-f"), required = true)
  var fileName:String = null

  @Parameter(names = Array("-c", "-collection"), required = true)
  var collection:String = null


  def process() {

    val file = new File(fileName)
    require(file.exists(),fileName+"must be readable file")

   val tp:TableParser = collection match {
      case "accounts" => AccountsTableParser
      case "objects" => ObjectsTableParser
      case _ => throw new IllegalArgumentException("collection must be \"accounts\" or \"objects\"")
    }

    tp.process(file)

  }
}




