package ru.sosgps.wayrecall.tools

import java.io._
import java.nio.file.{Files, Paths}
import java.util.Properties

import com.beust.jcommander.{Parameters, Parameter}
import com.google.common.io.LineReader
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import ru.sosgps.wayrecall.jcommanderutils.CliCommand

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaIterator
import scala.collection.immutable.TreeSet
import scala.io.Source


@Parameters(commandDescription = "reads and manipulates translations files")
class TranslatorUtils extends CliCommand {

  val commandName = "trans"

  @Parameter
  var command: java.util.List[String] = null

  @Parameter(names = Array("-s", "--srs"), description = "filename", required = true)
  var src: java.util.List[String] = null

  @Parameter(names = Array("-d", "--data"), description = "data", required = false)
  var data: String = null

  @Parameter(names = Array("-t", "--target"), description = "target", required = true)
  var target: String = null

  override def process(): Unit = {

    command.head match {
      case "getStrings" =>{
        val props = readPairs(src.head)
        val writer = new PrintWriter(target, "UTF8")
        for((k,v) <- props){
          writer.println(v)
        }
        writer.close()
      }

      case "mkPairs" =>{
        val props = readPairs(src.head)
        val datalines = Source.fromFile(data, "UTF8").getLines()
        val writer = new PrintWriter(target, "UTF8")
        for(((k,v), l )<- props zip datalines){
          val ls = l.trim
          val s = if(v.head.isUpper) {
            ls.head.toUpper + ls.tail
          } else ls
          writer.println(k + " = " + s)
        }
        writer.close()
      }

      case "makeExcel" => {

        val shead = Paths.get(src.head)
        val props = if(Files.isDirectory(shead)){
          val ds = Files.newDirectoryStream(shead)
          val props = readProps(ds.iterator().map(_.toString).toList)
          ds.close()
          props
        } else {
          val list = src.toSeq
          readProps(list)
        }

        val keys = props.flatMap(_._2.keySet).to[TreeSet]

        val workbook = new XSSFWorkbook()
        val sheet1 = workbook.createSheet()
        val headrow = sheet1.createRow(0)
        for( (p, i) <- props.zipWithIndex){
          headrow.createCell(i+1).setCellValue(p._1)
        }

        for( (key, i) <- keys zipWithIndex){
          val keyrow = sheet1.createRow(i+1)
          keyrow.createCell(0).setCellValue(key)
          for( (p, i) <- props zipWithIndex){
            keyrow.createCell(i + 1).setCellValue(p._2.getOrElse(key,""))
          }
        }

        val out = new FileOutputStream(target)
        workbook.write(out)
        out.close()
      }

      case "splitExcel" => {

        val stream = new FileInputStream(src.head)
        val workbook = new XSSFWorkbook(stream)
        val sheet = workbook.getSheetAt(0)
        val headrow = sheet.getRow(0)
        val translations = (1 until headrow.getLastCellNum).map(headrow.getCell).map(_.getStringCellValue).toList

        println("translations="+translations)

        Files.createDirectories(Paths.get(target))

        val translationsWriters = translations.map(n => new PrintWriter(new File(target, n + ".properties"), "UTF8"))

        for (row <- sheet.rowIterator().drop(1)) {
          val key = row.getCell(0).getStringCellValue
          for ((v, w) <- row.cellIterator().drop(1).zip(translationsWriters.iterator)) {
            w.println(key + " = " + v)
          }
        }

        translationsWriters.foreach(_.close())


      }
    }


  }

  def readProps(list: Seq[String]): Seq[(String, Map[String, String])] = {
    list.map(fname => (Paths.get(fname).getFileName.toString.stripSuffix(".properties"), readPairs(fname).toMap))
  }

  def readPairs(fname: String): Iterator[(String, String)] = {
    Source.fromFile(fname, "UTF8").getLines().filter(_.nonEmpty).map(s => {
      val i = s.indexOf("=")
      val (a, b) = s.splitAt(i)
      (a.trim, b.tail.trim)
    })
  }
}
