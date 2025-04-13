package ru.sosgps.wayrecall.m2msms

import ru.sosgps.wayrecall.core.MongoDBManager
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import collection.mutable
import collection.JavaConversions.iterableAsScalaIterable
import java.io.FileOutputStream
import org.apache.poi.ss.usermodel.{HorizontalAlignment, Font}
import ru.sosgps.wayrecall.utils.POIUtils.toTypedValue

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import java.util.Date

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 28.03.13
 * Time: 14:34
 * To change this template use File | Settings | File Templates.
 */
object ExportExcel {

  val spring = SendSms.setupSpring()

  val collection = spring.getBean(classOf[MongoDBManager]).getDatabase()("equipment")


  val headersSeq = IndexedSeq(
    //"_id",
    "status",
    "imei",
    "phone",
    "type",
    "pcount",
    "login",
    "password",
    "p1login",
    "p1password",
    "p2login",
    "p2password",
    "p3login",
    "p3password",
    "p4login",
    "p4password",
    "err",
    "cantConnect",
    "old",
    //"rightPassword",
    //"canConnect",
    "wrongPasswords"
  )

  val translateHeaders = Map(
    "status" -> "Статус",
    "phone" -> "Номер",
    "type" -> "Тип",
    "pcount" -> "Профилей",
    "login" -> "Логин",
    "password" -> "Пароль",
    "err" -> "Странное поведение",
    "cantConnect" -> "Не могу соединиться",
    "old" -> "Давно не было сообщений"
    //"rightPassword",
    //"canConnect",
    //"wrongPasswords" -> "Статус"
  ).withDefault(s => s)

  val workbook = new XSSFWorkbook();

  val sheet = workbook.createSheet()

  val allDataFromDb = collection.find().toIndexedSeq

  val headerToIndex = headersSeq.zipWithIndex.toMap

  def main1(arg: Array[String]): Unit = {

    val smses = spring.getBean(classOf[MongoDBManager]).getDatabase()("smses")
      .find(MongoDBObject("senderPhone" -> "79851773268"))
      .sort(MongoDBObject("sendDate" -> 1))

    val workbook = new XSSFWorkbook();
    val sheet = workbook.createSheet()

    println("smses.count=" + smses.count)

    for ((sms, i) <- smses zipWithIndex) {
      val row = sheet.createRow(i)
      row.createCell(0).setTypedValue(sms.as[Date]("sendDate"))
      row.createCell(1).setTypedValue(sms.as[String]("text"))
    }

    val out = new FileOutputStream("smses.xlsx")
    workbook.write(out)
    out.close()

  }

  def main(arg: Array[String]): Unit = {

    //    val headers = new mutable.LinkedHashSet[String]();
    //
    //    allData.foreach(dbo => {headers.++=(dbo.keySet())})
    //
    //    println("headers = "+headers.map(s => "\""+s+"\"").mkString("Seq(",",",")"))


    createHeaderRow()

    val allData = allDataFromDb.map(dbo => {
      val status = getStatus(dbo)
      dbo.put("status", status)
      if (status == "OK") {
        dbo.put("login", dbo.get("p1login"))
        dbo.put("password", dbo.get("p1password"))
      }
      dbo
    }).sortBy(dbo => (dbo.as[String]("status"), dbo.getAs[Any]("pcount").filter(None !=).map(allToInt).getOrElse(-1)))


    for ((dbo, i) <- allData.zipWithIndex) {
      val row = sheet.createRow(i + 1)

      for ((k, v) <- dbo; hi <- headerToIndex.get(k)) {
        row.createCell(hi).setCellValue(v.toString)
      }

    }

    for (i <- 0 until headersSeq.size) {
      sheet.autoSizeColumn(i)
    }

    val out = new FileOutputStream("databaseResults.xlsx")
    workbook.write(out)
    out.close()

  }

  def allToInt(x: Any): Int = x match {
    case x: Int => x
    case x: Double => x.toInt
  }

  def getStatus(dbo: MongoDBObject): String = {

    if (
      dbo.getAs[Any]("pcount").filter(None !=).getOrElse(-1) == 4 &&
        dbo.isDefinedAt("p1login") &&
        dbo.isDefinedAt("p2login") &&
        dbo.isDefinedAt("p3login") &&
        dbo.isDefinedAt("p4login") &&
        dbo.isDefinedAt("p1password") &&
        dbo.isDefinedAt("p2password") &&
        dbo.isDefinedAt("p3password") &&
        dbo.isDefinedAt("p4password")
        ||

        dbo.getAs[Any]("pcount").filter(None !=).getOrElse(-1) == 1 &&
          dbo.isDefinedAt("p1login") &&
          dbo.isDefinedAt("p1password")
    )
      "OK"
    else if (dbo.getAsOrElse[Boolean]("old", false))
      "Давно не было сообщений"
    else if (dbo.getAsOrElse[Boolean]("cantConnect", false))
      "Не удалось соединиться"
    else if (dbo.getAsOrElse[Boolean]("err", false))
      "Странное поведение"
    else
      "Неизвестно"

  }

  def createHeaderRow() {
    val cs = workbook.createCellStyle();
    cs.setAlignment(HorizontalAlignment.CENTER)
    val f = workbook.createFont();
    f.setBoldweight(Font.BOLDWEIGHT_BOLD);
    cs.setFont(f);

    val headerRow = sheet.createRow(0)
    headerRow.setRowStyle(cs)

    for (i <- 0 until headersSeq.size) {
      headerRow.createCell(i)
      headerRow.getCell(i).setCellValue(translateHeaders(headersSeq(i)))
    }
  }
}
