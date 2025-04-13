//package ru.sosgps.wayrecall.m2msms
//
//import ru.sosgps.wayrecall.tools.WialonObjectsSync
//import ru.sosgps.wayrecall.core.MongoDBManager
//import com.mongodb.casbah.commons.MongoDBObject
//import com.mongodb.casbah.Imports._
//import java.util.Date
//import java.text.{ParseException, SimpleDateFormat}
//import scala.util.control.Exception.catching
//import org.joda.time.DateTime
//
///**
// * Created with IntelliJ IDEA.
// * User: nickl
// * Date: 27.03.13
// * Time: 18:41
// * To change this template use File | Settings | File Templates.
// */
//object WialonObjDataLoader {
//
//  val spring = SendSms.setupSpring()
//
//  val collection = spring.getBean(classOf[MongoDBManager]).getDatabase()("equipment")
//
//  def main(args: Array[String]) = {
//
//    val pivot = new DateTime().minusWeeks(2).toDate
//
//    println(pivot)
//
//    val sync = new WialonObjectsSync()
//    sync.attrmap = Seq(
//      None, // "№"
//      Some("name"), // "Объект"
//      Some("oid"), //"ID"
//      None, //"Создатель"
//      None, //"Локальное хранилище"
//      Some("equipmentType"), //"Тип устройства"
//      Some("uid"), // "Уникальный идентификатор"
//      Some("phone"), //"Телефон"
//      None, // "Пробег"
//      Some("lastmsg"), //"Последняя позиция"
//      None, //"Последнее положение"
//      None //"Действия"
//    )
//
//    sync.authorize()
//    val data = sync.loadObjectData().toIndexedSeq
//
//    //println("data=" + data)
//
//
//    val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//
//    val filter = data.filter(d => {
//      println(d)
//      val date = catching(classOf[ParseException]).opt(df.parse(d("lastmsg"))).getOrElse(new Date())
//      date.before(pivot)
//    })
//
//    println("filter=" + filter.size)
//    //println("filter=" + filter)
//
//       filter.map(f => f("phone").stripPrefix("+")).foreach(phone => {
//          println(phone)
//          try {
//           //println(collection.findOne(MongoDBObject("phone" -> phone)))
//            collection.update(MongoDBObject("phone" -> phone),$set("old" -> true))
//          }
//          catch {
//            case e => println("error in " + phone)
//            throw e
//          }
//        })
//
//
//  }
//
//}
