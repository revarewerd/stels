package ru.sosgps.wayrecall.tools.excelimport

import java.io.{FileInputStream, File}
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import scala.collection.JavaConversions.iterableAsScalaIterable
import ru.sosgps.wayrecall.utils.POIUtils.toTypedValue
import org.apache.poi.ss.usermodel.{Cell, Row}
import org.apache.poi.openxml4j.opc.OPCPackage
import scala.collection.immutable.IndexedSeq
import org.apache.poi.ss.util.CellRangeAddress
import ru.sosgps.wayrecall.tools.Utils

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 12.08.13
 * Time: 20:50
 * To change this template use File | Settings | File Templates.
 */
object ExcelImport {

  implicit class cellReader(cell: Cell){

    def readOrEmpty:String = Option(cell).map(_.getStringCellValue).getOrElse("")

  }

  def main(args: Array[String]) {
//    val s1 = new File("/media/7414-4CA5/Книга1.xlsx")
//    val loadedData = readDataFromExcel(s1)
//    Utils.storeSerializableData(loadedData.map(_.asInstanceOf[Serializable]), "exportedData.ser")

    val loadedData = Utils.readSerializedData[Map[String,Any]]("exportedData.ser").filter(_("Запись.Статус") == "Последняя")

    //println(loadedData)

     // .filter(_.exists(_ == ("Запись.Статус","Последняя")))

    loadedData.groupBy(r => r("Абонентский терминал.IMEI")).filter(_._2.size > 1).foreach(println)

    //loadedData

  }


  def readDataFromExcel(s1: File):Iterator[Map[String, Any]]= {
    val opcPackage = OPCPackage.open(s1);
    val workbook = new XSSFWorkbook(opcPackage)
    val sheet = workbook.getSheetAt(0)
    val lastCellNum = sheet.getRow(2).getLastCellNum
    val cellNumRange = 0 to lastCellNum

    workbook.setMissingCellPolicy(Row.RETURN_BLANK_AS_NULL)

    val mergedRegions = (0 until sheet.getNumMergedRegions).map(i => sheet.getMergedRegion(i))

    def getMergedRegionDataCell(rowIndex: Int, cellIndex: Int) = mergedRegions.find(_.isInRange(rowIndex, cellIndex)).map(mr => sheet.getRow(mr.getFirstRow).getCell(mr.getFirstColumn))

    val headers = cellNumRange.map(i => {
      val leastHeader = sheet.getRow(2).getCell(i).readOrEmpty
      if (!leastHeader.matches("\\s*"))
        (Seq(getMergedRegionDataCell(0, i), getMergedRegionDataCell(1, i)).flatten.distinct.map(_.getStringCellValue).filter(!_.matches("\\s*")) :+
          leastHeader).mkString(".")
      else
        ""
    }
    ).toIndexedSeq

    println(headers)

    sheet.drop(3).toIterator.map((r: Row) => {
      cellNumRange.map(i => (headers(i), Option(r.getCell(i)).map(_.getTypedValue()).orNull)).filter(_._1.nonEmpty).toIndexedSeq
      //r.zipWithIndex.toIterator.map({case (cell,i) => (i,cell.getTypedValue())}).toIndexedSeq
    }).takeWhile(_.nonEmpty).map(_.toMap)
  }
}
