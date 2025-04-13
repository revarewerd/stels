package ru.sosgps.wayrecall.m2msms

import generated.RequestMessageType
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.{Date, Iterator}
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import xml.{NodeSeq, Elem}
import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.iterableAsScalaIterable
import collection.immutable.{IndexedSeq, Seq}
import ru.sosgps.wayrecall.core.MongoDBManager

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime
import SendSms.richMessage
import collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.03.13
 * Time: 22:29
 * To change this template use File | Settings | File Templates.
 */
object ExcelReader {

  val spring = SendSms.setupSpring()

  val collection = spring.getBean(classOf[MongoDBManager]).getDatabase()("equipment")

  val sendSms = spring.getBean(classOf[SendSms])

  val profileCount = Map(
    "FM4100" -> 4,
    "FM4200" -> 4,
    "FM2100" -> 1,
    "FM2200" -> 1,
    "FM3200" -> 1,
    "FM3200Q" -> 1,
    //    "Teltonika FM4100" -> 0,
    "Teltonika FM5300" -> 4,
    //    "" -> 0,
    "FM1100" -> 1,
    "FM5300" -> 4,
    "Teltonika 1100" -> 1
  )

  val LOGIN_ID: String = "252"
  val PASSWORD_ID: String = "253"

  val dbEnabled = true

  def ifdb(f: => Any): Unit = {
    if (dbEnabled) f
  }

  def main(args: Array[String]) {

    //  sendGetInfo()

    //  escalatingPassword1()

    updateLoginAndPassword()

    0
    //
    //            val data =
    //              collection.find(MongoDBObject("pcount" -> 4)).filter(dbo =>
    //              !dbo.getAs[Boolean]("cantConnect").getOrElse(false) && (
    //                !dbo.isDefinedAt("p1login") ||
    //                !dbo.isDefinedAt("p2login") ||
    //                !dbo.isDefinedAt("p3login") ||
    //                !dbo.isDefinedAt("p4login") ||
    //                !dbo.isDefinedAt("p1password") ||
    //                !dbo.isDefinedAt("p2password") ||
    //                !dbo.isDefinedAt("p3password") ||
    //                !dbo.isDefinedAt("p4password"))
    //            ).toIndexedSeq  ++
    //              collection.find(MongoDBObject("pcount" -> 1)).filter(dbo =>
    //              !dbo.getAs[Boolean]("cantConnect").getOrElse(false) &&(
    //                !dbo.isDefinedAt("p1login") ||
    //                !dbo.isDefinedAt("p1password"))
    //            ).toIndexedSeq
    //
    //            println("size=" + data.size)
    //            data.foreach(println)

    ////        for (dbo <- data ) {
    ////
    ////          collection.update(MongoDBObject("phone" -> dbo.as[String]("phone")), $set("err" -> true))
    ////
    ////        }

    //    val data2 =
    //      collection.find(MongoDBObject("pcount" -> 4)).filter(dbo =>
    //
    //        dbo.isDefinedAt("p1login") &&
    //          dbo.isDefinedAt("p2login") &&
    //          dbo.isDefinedAt("p3login") &&
    //          dbo.isDefinedAt("p4login") &&
    //          dbo.isDefinedAt("p1password") &&
    //          dbo.isDefinedAt("p2password") &&
    //          dbo.isDefinedAt("p3password") &&
    //          dbo.isDefinedAt("p4password")
    //      ).toIndexedSeq ++
    //        collection.find(MongoDBObject("pcount" -> 1)).filter(dbo =>
    //          dbo.isDefinedAt("p1login") &&
    //            dbo.isDefinedAt("p1password")
    //        ).toIndexedSeq
    //
    //    println("size=" + data2.size)
    //    data2.foreach(println)


  }

  def escalatingPassword1() {
    val data = collection.find(MongoDBObject("pcount" -> 1)).filter(dbo =>
      !dbo.getAs[Boolean]("cantConnect").getOrElse(false) &&
        //                dbo.isDefinedAt("login") &&
        dbo.isDefinedAt("password") &&
        dbo.getAsOrElse[String]("p1login", "") == "21s21" &&
        !dbo.isDefinedAt("p1password") &&
        dbo.as[String]("password") == "5803" //&&
      //           dbo.as[String]("login") == "21s21"
    ).toIndexedSeq

    println("size=" + data.size)
    data.foreach(println)

    for (dbo <- data; profile <- 1 to 1) {
      val crit: DBObject = MongoDBObject("phone" -> dbo.as[String]("phone"))
      val com: DBObject = $set(("p" + profile + "password") -> "5803", ("p" + profile + "login") -> "21s21")
      println("" + crit + " " + com)
      //   ifdb(collection.update(crit, com))
    }
  }

  def escalatingPassword4() {
    val data = collection.find(MongoDBObject("pcount" -> 4)).filter(dbo =>
      !dbo.getAs[Boolean]("cantConnect").getOrElse(false) &&
        // dbo.isDefinedAt("login") &&
        // dbo.isDefinedAt("password") &&
        dbo.isDefinedAt("p1login") &&
        dbo.isDefinedAt("p2login") &&
        dbo.isDefinedAt("p3login") &&
        dbo.isDefinedAt("p4login") &&
        !dbo.isDefinedAt("p1password") &&
        dbo.as[String]("password") == "5803"
    ).toIndexedSeq

    println("size=" + data.size)
    data.foreach(println)

    for (dbo <- data; profile <- 1 to 4) {
      val crit: DBObject = MongoDBObject("phone" -> dbo.as[String]("phone"))
      val com: DBObject = $set(("p" + profile + "password") -> "5803")
      println("" + crit + " " + com)
      //ifdb(collection.update(crit, com))
    }
  }

  def updateLoginAndPassword() {

    //            val data = collection.find(MongoDBObject("pcount" -> 1)).filter(dbo =>
    //              !dbo.getAs[Boolean]("cantConnect").getOrElse(false) &&
    //                !dbo.getAs[Boolean]("old").getOrElse(false) &&
    //                !dbo.getAs[Boolean]("err").getOrElse(false) &&
    //                dbo.isDefinedAt("p1login") &&
    //                  !dbo.isDefinedAt("p1password")
    //            ).toIndexedSeq

    //        val data = collection.find(MongoDBObject("pcount" -> 1)).filter(dbo =>
    //          !dbo.getAs[Boolean]("cantConnect").getOrElse(false) &&
    //            dbo.getAs[String]("login").map(_ != "21s21").getOrElse(false) && dbo.as[String]("password") != "5803" &&
    //            //dbo.isDefinedAt("pcount") &&
    //            dbo.isDefinedAt("p1login") &&
    //            !dbo.isDefinedAt("p1password") &&
    //            !dbo.isDefinedAt("p2login") &&
    //            !dbo.isDefinedAt("p3login") &&
    //            !dbo.isDefinedAt("p4login")
    //        ).toIndexedSeq

    //    val data = collection.find(MongoDBObject("pcount" -> 4)).filter(dbo =>
    //      !dbo.getAs[Boolean]("cantConnect").getOrElse(false) &&
    //        dbo.isDefinedAt("login") &&
    //        dbo.isDefinedAt("password") &&
    //        dbo.isDefinedAt("p1login") &&
    //        dbo.isDefinedAt("p2login") &&
    //        dbo.isDefinedAt("p3login") &&
    //        dbo.isDefinedAt("p4login") &&
    //        dbo.isDefinedAt("p1password") &&
    //        dbo.isDefinedAt("p2password") &&
    //        dbo.isDefinedAt("p3password") &&
    //        !dbo.isDefinedAt("p4password")
    //    ).toIndexedSeq

    val data = collection.find(MongoDBObject("pcount" -> 4)).filter(dbo =>
      !dbo.getAs[Boolean]("cantConnect").getOrElse(false) &&
        !dbo.getAs[Boolean]("old").getOrElse(false) &&
        !dbo.getAs[Boolean]("err").getOrElse(false) &&
        dbo.isDefinedAt("p1login") &&
        dbo.isDefinedAt("p2login") &&
        dbo.isDefinedAt("p3login") &&
        dbo.isDefinedAt("p4login") &&
        dbo.isDefinedAt("p1password") &&
        dbo.isDefinedAt("p2password") &&
        dbo.isDefinedAt("p3password") &&
        !dbo.isDefinedAt("p4password")
      /*(
        !dbo.isDefinedAt("p2login") ||
        !dbo.isDefinedAt("p3login") ||
        !dbo.isDefinedAt("p4login") ||
        !dbo.isDefinedAt("p1password") ||
        !dbo.isDefinedAt("p2password") ||
        !dbo.isDefinedAt("p3password") ||
        !dbo.isDefinedAt("p4password"))*/).
      toIndexedSeq

    println("size=" + data.size)
    data.foreach(println)

    //    data.foreach(dbo => {
    //      collection.update(MongoDBObject("phone" -> dbo.as[String]("phone")),$unset("wrongPasswords"))
    //    })

    val profile = 4
    //      val paramid = LOGIN_ID
    //      val value = "21s21"
    val paramid = PASSWORD_ID
    val value = "5803"

    val paramStr: String = "setparam " + profile + paramid + " " + value
    //val paramStr: String = "getparam " + profile + paramid


    doSending(data, paramStr, {
      case (login, password) => Seq(
        (login, password),
        ("21s21", password),
        ("21s21", "5803"),
        (login, "5803"),
        ("", ""),
        ("5803", "5803"),
        ("5802", "5802"),
        ("0101", "0101"),
        ("5802", "sosgp"),
        ("21s21", "5802"),
        ("21s21", ""),
        ("21s21", "sosgp"),
        ("21s21", "0101"))
    })

    for (i <- 1 to 12) {
      println("waiting " + i)
      Thread.sleep(10 * 1000)
    }

    upDateParamsFromResponses(data)

  }


  def doSending(data: IndexedSeq[DBObject], paramStr: String, passw: (String, String) => Seq[(String, String)]) {
    for (dbo <- data) {
      val phone: String = dbo.as[String]("phone")
      var login: String = remspace(dbo.getAs[String]("login").getOrElse("5803"))
      var password: String = remspace(dbo.getAsOrElse[String]("password", "5803"))

      val wrongPasswords = dbo.getAsOrElse[MongoDBList]("wrongPasswords", MongoDBList.empty).map(_.asInstanceOf[DBObject])
        .map(dbo => (dbo.as[String]("login"), dbo.as[String]("password"))).toSet

      //     val lpo = Seq((login, password),("21s21", password),("21s21", "5803")).find(e => !wrongPasswords.contains(e))
      val lpo = passw(login, password).find(e => !wrongPasswords.contains(e))

      lpo match {
        case Some(lpo) =>
          login = lpo._1
          password = lpo._2
          val smsStr: String = login + " " + password + " " + paramStr
          println((phone, smsStr))
          println(sendSms.sendSms(phone, smsStr))

        case None =>
          //login = "21s21"
          //password = lpo._2
          println("all passwords are invalid for " + phone)
        //ifdb(collection.update(MongoDBObject("phone" -> phone), $set("err" -> true)))
      }


    }
  }

  def upDateParamsFromResponses(data: IndexedSeq[DBObject]) {
    val by = sendSms.readLastHourSms(data.map(_.as[String]("phone")), 1, RequestMessageType.ALL).getMessageInfo.
      map(f => richMessage(f)).groupBy(_.getDeviceMsid)

    for ((k, v) <- by) {
      println(k)
      println(v.mkString("\n"))

      val vi = v.toSeq.reverse.take(2)
      if (!vi(0).incoming && vi(0).delivered) {
        val regex = """(\S*) *(\S*) ((setparam)|(getparam)|(getinfo)).*""".r
        vi(0).mi.getMessageText match {
          case regex(login, password, _, _, _, _) =>
            println("wrong password " + login + "|" + password)
            ifdb(collection.update(MongoDBObject("phone" -> k), $push("wrongPasswords" -> MongoDBObject("login" -> login, "password" -> password))))
          case _ => println("unmatched:" + vi(0).mi.getMessageText)
        }

      }
      else if (!vi(0).incoming && !vi(0).delivered) {
        println("cantConnect")
        ifdb(collection.update(MongoDBObject("phone" -> k), $set("cantConnect" -> true)))
      }
      else if (vi.length > 1) {
        if (!vi(1).incoming) {

          val regex = """(\S*) *(\S*) ((setparam)|(getparam)|(getinfo)).*""".r
          vi(1).mi.getMessageText match {
            case regex(login, password, _, _, _, _) =>
              val wrongPasswords: Set[(String, String)] = collection.findOne(MongoDBObject("phone" -> k)).get.getAsOrElse[MongoDBList]("wrongPasswords", MongoDBList.empty).map(_.asInstanceOf[DBObject])
                .map(dbo => (dbo.as[String]("login"), dbo.as[String]("password"))).toSet
              println("right passwords")
              ifdb(collection.update(MongoDBObject("phone" -> k), $set("login" -> login, "password" -> password, "rightPassword" -> true)))

              if (wrongPasswords.contains((login, password))) {
                println("nonWrong passwords " +(login, password))

                val newwp = (wrongPasswords -- Set((login, password), ("21s21", "5803"))).map(t => MongoDBObject("login" -> t._1, "password" -> t._2)).toList
                println("newwp=" + newwp)

                ifdb(collection.update(MongoDBObject("phone" -> k), $set("wrongPasswords" -> newwp)))

              }

            case _ => println("unmatched:" + vi(1).mi.getMessageText)
          }


          val text = vi(0).mi.getMessageText

          updateParamsFromSms(k, text)
        }

      }
      println()
    }
  }

  def updateParamsFromSms(phone: String, messageText: String): Any = {
    val regex = """Param ID:(\d)(\d+) ((New Text)|(Text)):(.+)""".r

    messageText match {
      case regex(profile, paramId, _, _, _, value) =>
        println("profile=" + profile + " id=" + paramId + " value=" + value)

        val param = if (paramId == LOGIN_ID)
          "login"
        else {

          if (paramId == PASSWORD_ID)
            "password"
          else
            throw new IllegalArgumentException("paramId == " + paramId)
        }

        println("updating " + phone + " " + (("p" + profile + param) -> value))

        ifdb(collection.update(MongoDBObject("phone" -> phone), $set(("p" + profile + param) -> value)))

      case _ => println("unsupported:" + messageText)
    }
  }

  def remspace(str: String) = if (str == " ") "" else str

  def logCantConnect {
    val data = collection.find().filter(dbo =>
      dbo.getAs[Boolean]("cantConnect").getOrElse(false)
    ).toIndexedSeq
    println("size=" + data.size)
    data.foreach(dbo => {
      println(dbo)
    })

    val phones = data.map(_.as[String]("phone"))

    val by = sendSms.readLastHourSms(phones, 1, RequestMessageType.ALL).getMessageInfo.
      map(f => richMessage(f)).groupBy(_.getDeviceMsid)

    for ((k, v) <- by) {
      println(k)
      println(v.mkString("\n"))
    }
  }

  def sendGetInfo() {

    val data = collection.find().filter(dbo =>
      !dbo.getAs[Boolean]("cantConnect").getOrElse(false) &&
        !dbo.getAs[Boolean]("old").getOrElse(false) &&
        !dbo.getAs[Boolean]("err").getOrElse(false) &&
        !dbo.isDefinedAt("pcount")

    ).toIndexedSeq
    println("size=" + data.size)
    data.foreach(dbo => {
      println(dbo)
    })

    for (dbo <- data) {
      val phone: String = dbo.as[String]("phone")
      var login: String = remspace(dbo.getAs[String]("login").getOrElse("5803"))
      var password: String = remspace(dbo.getAsOrElse[String]("password", "5803"))

      val wrongPasswords = dbo.getAsOrElse[MongoDBList]("wrongPasswords", MongoDBList.empty).map(_.asInstanceOf[DBObject])
        .map(dbo => (dbo.as[String]("login"), dbo.as[String]("password"))).toSet

      //     val lpo = Seq((login, password),("21s21", password),("21s21", "5803")).find(e => !wrongPasswords.contains(e))
      val lpo = Seq(
        //        (login, password),
        //        ("21s21", password),
        //        ("21s21", "5803"),
        //        (login, "5803"),
        ("5803", "5803"),
        ("5802", "5802"),
        ("", ""),
        ("0101", "0101"),
        ("5802", "sosgp"),
        ("21s21", "5803"),
        ("21s21", "5802"),
        ("21s21", ""),
        ("21s21", "sosgp"),
        ("21s21", "0101")
      ).find(e => !wrongPasswords.contains(e))

      lpo match {
        case Some(lpo) =>
          login = lpo._1
          password = lpo._2

          val smsStr: String = login + " " + password + " getinfo"
          println((phone, smsStr))
          println(sendSms.sendSms(phone, login + " " + password + " getinfo"))

        case None =>
          println("all passwords are invalid for " + phone)
          ifdb(collection.update(MongoDBObject("phone" -> phone), $set("err" -> true)))
      }

    }

    for (i <- 1 to 12) {
      println("waiting " + i)
      Thread.sleep(10 * 1000)
    }

    testGetInfo(data)
  }


  def testGetInfo(data: IndexedSeq[DBObject]) {
    val by = sendSms.readLastHourSms(data.map(_.as[String]("phone")), 36, RequestMessageType.ALL).getMessageInfo.
      map(f => richMessage(f)).groupBy(_.getDeviceMsid)

    for ((k, v) <- by) {
      println(k)
      println(v.mkString("\n"))
      val vi = v.toSeq.reverse.take(2)
      if (!vi(0).incoming && vi(0).delivered) {
        val regex = """(\S*) *(\S*) ((setparam)|(getinfo)).*""".r
        vi(0).mi.getMessageText match {
          case regex(login, password, _, _, _) =>
            println("wrong password " + login + "|" + password)
            ifdb(collection.update(MongoDBObject("phone" -> k), $push("wrongPasswords" -> MongoDBObject("login" -> login, "password" -> password))))
          case _ => println("unmatched:" + vi(0).mi.getMessageText)
        }

      }
      else if (!vi(0).incoming && !vi(0).delivered) {
        println("cantConnect")
        ifdb(collection.update(MongoDBObject("phone" -> k), $set("cantConnect" -> true)))
      }
      else if (vi.length > 1) {
        if (!vi(1).incoming && vi(1).mi.getMessageText.contains("getinfo")) {

          val regex = """(\S*) *(\S*) ((setparam)|(getinfo)).*""".r
          vi(1).mi.getMessageText match {
            case regex(login, password, _, _, _) =>
              val wrongPasswords: Set[(String, String)] = collection.findOne(MongoDBObject("phone" -> k)).get.getAsOrElse[MongoDBList]("wrongPasswords", MongoDBList.empty).map(_.asInstanceOf[DBObject])
                .map(dbo => (dbo.as[String]("login"), dbo.as[String]("password"))).toSet
              println("right passwords")
              ifdb(collection.update(MongoDBObject("phone" -> k), $set("login" -> login, "password" -> password, "rightPassword" -> true)))

              if (wrongPasswords.contains((login, password))) {
                println("nonWrong passwords " +(login, password))

                val newwp = (wrongPasswords -- Set((login, password), ("21s21", "5803"))).map(t => MongoDBObject("login" -> t._1, "password" -> t._2)).toList
                println("newwp=" + newwp)

                ifdb(collection.update(MongoDBObject("phone" -> k), $set("wrongPasswords" -> newwp)))

              }

            case _ => println("unmatched:" + vi(1).mi.getMessageText)
          }


          val text = vi(0).mi.getMessageText

          if (text.contains("P0") && text.contains("P1")) {
            println("pc=" + 4)
            ifdb(collection.update(MongoDBObject("phone" -> k), $set("pcount" -> 4)))
          }
          else if (text.contains("NOGPS")) {
            println("pc=" + 1)
            ifdb(collection.update(MongoDBObject("phone" -> k), $set("pcount" -> 1)))
          }
          else {
            println("unknown")
          }

        }


      }

      println()

    }
  }

  def setPcountByType {
    collection.find().foreach(dbo => {
      val s = dbo.as[String]("type")
      println(s + "->" + profileCount.get(s).orNull)
      profileCount.get(s).foreach(pc => {
        collection.update(MongoDBObject("phone" -> dbo.as[String]("phone")), $set("pcount" -> pc))
      })
    })
  }

  def updateFromReports {
    val report = readCommunicatorReport("/home/nickl/Загрузки/SMS-ответы_на_рассылку_21s21_5803.xls") ++
      readCommunicatorReport("/home/nickl/Загрузки/SMS-ответы_на_рассылку_5803_5803_P2__21s21.xls") ++
      readCommunicatorReport("/home/nickl/Загрузки/SMS-ответы_на_рассылку_5803_5803_P3_21s21.xls") ++
      readCommunicatorReport("/home/nickl/Загрузки/SMS-ответы_на_рассылку_5802_5802_P1_21s21.xls") ++
      readCommunicatorReport("/home/nickl/Загрузки/SMS-ответы_на_рассылку_5802_5802_P3_21s21.xls") ++
      readCommunicatorReport("/home/nickl/Загрузки/SMS-ответы_на_рассылку_5803_5803_P1_21s21.xls")

    report.foreach(r => {

      val phone = r("Номер")
      println(r)
      val messageText: String = r("Текст сообщения")

      updateParamsFromSms(phone, messageText)
    })
  }


  def updateFromYchet {
    val read = xlsxRead(0, 0, 3)

    for (seq <- read) yield {
      println(seq)

      val phone = seq(3)
      val typ = seq(0)
      var login = seq(1)
      var passw = seq(2)

      if (login == "пробел")
        login = " "

      if (passw == "пробел")
        passw = " "

      if (!login.contains("?") && !passw.contains("?") && !login.isEmpty && !passw.isEmpty)
        collection.update(MongoDBObject("phone" -> phone), $set("type" -> typ, "login" -> login, "password" -> passw))

      //      if (login.contains("?") || passw.contains("?") || login.isEmpty || passw.isEmpty )
      //        collection.update(MongoDBObject("phone" -> phone), $unset("login", "password"))


    }
  }

  def addEquipmentAsUsers {

    collection.find().toIterator.foreach(e => {
      try {
        val phone = e.as[String]("phone")
        println("adding:" + phone)
        val res = sendSms.addUser(phone, phone, null, 6290)
        println("res:" + res)
      }
      catch {
        case e: Exception =>
          println(e)
          e.printStackTrace()
          Thread.sleep(2000)
      }
    })
  }

  def readB3 {
    val read = xlsxRead(1, 0, 2)

    collection.ensureIndex(MongoDBObject("phone" -> 1), MongoDBObject("unique" -> true))
    collection.ensureIndex(MongoDBObject("imei" -> 1), MongoDBObject("unique" -> true))

    for (seq <- read) {
      collection.insert(MongoDBObject("type" -> seq(0), "imei" -> seq(1), "phone" -> seq(2)), WriteConcern.Safe)
    }
  }

  def readCommunicatorReport(communicatorReportXml: String): Seq[Map[String, String]] = {
    val file = scala.xml.XML.loadFile(communicatorReportXml)

    val rows: NodeSeq = file \\ "Table" \ "Row"

    rows.map(r => r \ ("Data"))

    val alldata = rows.map(r => (r \\ ("Data")).map(n => n.text).toIndexedSeq)
    val header = alldata.head

    val result = alldata.drop(1).map(v => header.zip(v).toMap)
    result
  }

  def xlsxRead(page: Int, firstCell: Int, lastCell: Int) = {
    val fis: FileInputStream = new FileInputStream("/home/nickl/Загрузки/Обший список.xlsx")
    //val fis = new FileInputStream("/home/nickl/Загрузки/SMS-ответы_на_рассылку_5803_5803_P3_21s21.xls")
    val workbook = new XSSFWorkbook(fis)
    fis.close
    val p2 = workbook.getSheetAt(page)
    val lastRowNum: Int = p2.getLastRowNum

    val res = p2.rowIterator().map(row => {
      (firstCell to lastCell).map(i => {
        val cell = row.getCell(i);
        if (cell != null) {
          cell.setCellType(Cell.CELL_TYPE_STRING)
          cell.getStringCellValue
        }
        else
          null
      })
    }).takeWhile(_(lastCell) != null).toIndexedSeq

    //    println("res size=" + res.size)
    //    println(res.mkString("\n"))

    res
  }
}