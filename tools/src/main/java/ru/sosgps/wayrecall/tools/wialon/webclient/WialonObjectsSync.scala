package ru.sosgps.wayrecall.tools.wialon.webclient

import com.beust.jcommander.{JCommander, Parameter, Parameters}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import java.util.Date
import scala.collection.JavaConversions.collectionAsScalaIterable
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.jsoup.nodes.Element
import scala.concurrent.ExecutionContext.Implicits.global
import java.io._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import resource._
import scala.Some
import scala.Serializable
import scala.collection.mutable

object WialonObjectsSync {

  def main(args: Array[String]) {

    val jc = new JCommander();
    val sync = new WialonObjectsSync

      jc.addObject(sync)
      jc.parse(args: _*)
      sync.process()


  }

}


@Parameters(commandDescription = "Updates objects data from wialon(b3) server")
class WialonObjectsSync extends CliCommand {

  def log(x: String) = {
    println(new Date() + " " + x);
  }


  lazy val wialonClient = new WialonAdminHttpClient(wialonUrl, user, password)

  @Parameter(names = Array("--wialonUrl"), description = " Wialon Server URL")
  var wialonUrl = "http://b3.sosgps.ru:8021"

  @Parameter(names = Array("-l"), description = "server login")
  var user = "admin"

  @Parameter(names = Array("-p"), description = "server password")
  val password = "ADMIN098passwd"


  //№	Unit	ID	Creator	Local Storage	Device Type	Unique ID	Phone	Mileage	Last Message	Last Position	Actions
  var attrmap: Seq[Option[String]] = Seq(
    None, // "№"
    Some("name"), // "Объект"
    Some("oid"), //"ID"
    None, //"Создатель"
    None, //"Локальное хранилище"
    Some("equipmentType"), //"Тип устройства"
    Some("equipmentIMEI"), // "Уникальный идентификатор"
    Some("phone"), //"Телефон"
    None, // "Пробег"
    None, //"Последняя позиция"
    None, //"Последнее положение"
    None //"Действия"
  )

  def process() {

        wialonClient.authorize()

        val wlnEnts = importWialonObjectsEntries()

        storeWialonEntries(wlnEnts)
//
//    val wlnEnts = readWialonEntries()
//
    wlnEnts.foreach(println)
//
//    val allUsers = wlnEnts.map(_.allUsers.toSet).reduce(_ ++ _)
//
//    val userToObjMap = new mutable.HashMap[String, mutable.Set[WialonObjectEntry]]() with mutable.MultiMap[String, WialonObjectEntry]
//
//    for (user <- allUsers; wln <- wlnEnts) {
//      if (wln.allUsers.contains(user))
//        userToObjMap.addBinding(user, wln)
//    }
//
//
//    val userObjectsCount = userToObjMap.mapValues(_.size).filter(_._2 < 150).toMap
//
//    val accMap = wlnEnts.map(oe => {
//      val acc = oe.allUsers.toSeq.filter(userObjectsCount.isDefinedAt).sortBy(userObjectsCount)(Ordering[Int].reverse).headOption
//      (oe, acc)
//    })
//
//    for ((obj, acc) <- accMap.sortBy(_._2)) {
//      println(acc.getOrElse("") + "\t" + obj.name)
//    }
//
//    val accCandidates = accMap.flatMap(_._2).toSet
//
//    println()
//
//    val sortBy: Seq[(String, mutable.Set[WialonObjectEntry])] = userToObjMap.toSeq.sortBy(_._2.size)(Ordering[Int].reverse)
//
//    for ((user, objects) <- sortBy) {
//      println(user + "\t" + objects.size + "\t" + accCandidates(user) + "\t" + objects.map(_.name).mkString(", "))
//    }

    //        val objectsCollection = Main.getMongoDatabase()("objects")
    //
    //        for (record <- seq) {
    //          objectsCollection.update(MongoDBObject("oid" -> record("oid")), record, true)
    //        }
    //
    //        seq.foreach(println)

  }

  def readWialonEntries(): Stream[WialonObjectEntry] = ru.sosgps.wayrecall.tools.Utils.readSerializedData[WialonObjectEntry]("wialonObjects.ser")

  def storeWialonEntries(result: List[WialonObjectEntry]) = ru.sosgps.wayrecall.tools.Utils.storeSerializableData(result,"wialonObjects.ser")

  def importWialonObjectsEntries(): List[WialonObjectEntry] = {
    val seq: Seq[Map[String, String]] = loadObjectData()
    println("seq.size=" + seq.size)

    val r = (for (objRec <- seq) yield {
      //http://b3.sosgps.ru:8021/dlg/accessors.html?height=560&width=800&modal=true&id=745213&random=1375982356559

      val rtext = wialonClient.requestAsync("dlg/accessors.html?height=560&width=800&modal=true&id=" + objRec("oid") + "&random=1375982356559")
      rtext.map(rtext => {
        val doc = Jsoup.parse(rtext)
        def pr(els: Elements) = {
          els.first().children().map(_.text()).toList
        }

        new WialonObjectEntry(
          objRec("name"),
          objRec("oid"),
          objRec("equipmentType"),
          objRec("equipmentIMEI"),
          objRec("phone"),
          pr(doc.select("#users_view")),
          pr(doc.select("#users_cmd")),
          pr(doc.select("#users_edit")),
          pr(doc.select("#users_manage"))
        )
      })
    }).toList

    Await.result(Future.sequence(r), Duration.Inf)
  }

  def loadObjectData(): Seq[Map[String, String]] = {
    val result: String = wialonClient.requestURL("index.html?page=2&pn=0&row_pp=5000")

    //println(result)

    val doc = Jsoup.parse(result)

    val table = doc.select("#sort_table_1").first()

    val pagestable: Element = table.nextElementSibling()
    val pages = Option(pagestable.select("tr").first().select("a").last())
    println("pages=" + pages)

    val tbody: Element = table.select("tbody").first()


    val seq: Seq[Map[String, String]] = (for (row <- tbody.select("tr")) yield {
      val builder = Map.newBuilder[String, String]
      for ((cell, i) <- row.children().zipWithIndex) {
        attrmap(i).foreach(attr => {
          //println(attr+ ":" + cell.text()))
          val tuple: (String, String) = (attr, cell.text())
          builder += tuple
        })

      }
      builder.result()
    }).toSeq
    seq
    seq
  }

  val commandName = "wlnobjsync"


}

@SerialVersionUID(1L)
class WialonObjectEntry(
                         val name: String,
                         val wialonOid: String,
                         val equipmentType: String,
                         val equipmentIMEI: String,
                         val phone: String,
                         val viewPermissins: List[String],
                         val cmdPermissins: List[String],
                         val editPermissins: List[String],
                         val managePermissins: List[String]
                         ) extends Serializable {

  override def toString: String = s"WialonObjectEntry(name=$name, wialonOid=$wialonOid ,equipmentType=$equipmentType, equipmentIMEI=$equipmentIMEI, phone=$phone, " +
    s"viewPermissins=$viewPermissins, cmdPermissins=$cmdPermissins, editPermissins=$editPermissins, managePermissins=$managePermissins)"

  def allUsers = viewPermissins.iterator ++ cmdPermissins.iterator ++ editPermissins.iterator ++ managePermissins.iterator

}


