package ru.sosgps.wayrecall.monitoring.web

import java.io.PrintWriter
import java.util.{Calendar, Date}
import javax.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired

import scala.util.Random
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping


/**
  * Created by ivan on 06.03.16.
  */

case class ColumnProperties(columnName: String, index: String, width: Int )   // Flex добавить позже, сначала желательно убедиться в жизненспособности
abstract class AbstractReportPrinter {
  def printGrid(writer: PrintWriter, tableModel: Seq[ColumnProperties], data: Iterable[Map[String,Any]])  = { // TODO определиться с форматом данных
    writer.println("<table style=\"width: 100%;text-align:left;font-size:11px;font-family:arial;border-collapse:collapse;\">")

    writer.println("<tr>")
    val thStyle = "text-align:left;ont-weight: bold;padding:4px 3px 4px 5px;border:1px solid #d0d0d0;border-left-color:#eee;background-color:#ededed;margin:0;"
    tableModel.foreach(props => writer.println(s"""<th style="$thStyle" width="${props.width}" style="text-align: left">${props.columnName}</th>"""))
    writer.println("</tr>")

    var odd = true
    data.foreach(map => {
      if(odd)
        writer.println("<tr style=\"background-color:#ffffff;\">")
      else
        writer.println("<tr style=\"background-color: #f9f9f9;\">")
      odd = !odd
      tableModel.foreach(props => {
        writer.println("<td style=\"margin:0;padding:3px 3px 3px 5px;border-style:none solid solid;border-width:1px;border-color:#ededed;\">")

        writer.println(s""" <div style="text-align:left">${map.getOrElse(props.index,"")}</div>""")

        writer.println("</td>")
      })
      writer.println("</tr>")
    })
    writer.println("</table>")
  }

}

@Controller
class GeneralReportPrinter extends AbstractReportPrinter {
  @Autowired
  var movementStats: MovementStatsReport = null
  val movementStackColumnModel = {
    Seq(
      ColumnProperties("Название", "name", 300),
      ColumnProperties("Значение", "value", 400)
    )
  }

  def getTestData = {
    val uid = "o1068965758008219167"
    val cal = Calendar.getInstance
    cal.set(Calendar.YEAR, 2016)
    cal.set(Calendar.MONTH, Calendar.MARCH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val dateFrom = cal.getTime
    val dateTo = new Date()
    movementStats.prepareReport(uid,dateFrom, dateTo)
  }

  @RequestMapping(value=Array("/reportPrinter/stats"))
  def print(response: HttpServletResponse) = {
    response.setContentType("text/html; charset=UTF-8")
    val writer = response.getWriter

    writer.println("<!DOCTYPE html>")
    writer.println("<html style=\"border:0;margin:0;padding:0;\">")

    writer.println("<body style=\"border:0;margin:0;padding:0;\">")

    printGrid(writer,movementStackColumnModel, getTestData)

    writer.println("</body")
    writer.println("</html")
  }
}
@Controller
class AddressReportPrinter extends AbstractReportPrinter {
  //  Map(
  //    "num" -> counter,
  //    "date" -> curdate,
  //    "time" -> format2.format(gps.time),
  //    "address" -> gps.placeName,
  //    "lon" -> gps.lon,
  //    "lat" -> gps.lat
  //  )
  val columnModel = Seq(
    ColumnProperties("№", "num", 60),
    ColumnProperties("Дата", "date", 160),
    ColumnProperties("Время", "time", 160),
    ColumnProperties("Адрес", "address", 400)
  )

  @Autowired
  var addressReportService: AddressesReport = null

  def genData() = {
    val uid = "o1068965758008219167"
    val dateFrom = new Date(0)
    val dateTo = new Date()

    addressReportService.fakeLoadByUid(uid,dateFrom,dateTo)
  }

  @RequestMapping(value=Array("/reportPrinter/address"))
  def print(response: HttpServletResponse) = {
    response.setContentType("text/html; charset=UTF-8")
    val writer = response.getWriter

    writer.println("<!DOCTYPE html>")
    writer.println("<html style=\"border:0;margin:0;padding:0;\">")

    writer.println("<body style=\"border:0;margin:0;padding:0;\">")

    printGrid(writer,columnModel, genData())

    writer.println("</body")
    writer.println("</html")
  }
}

@Controller
class ReportPrinterTest extends AbstractReportPrinter {

  val testModel = Seq(
    ColumnProperties("#", "num", 30),
    ColumnProperties("Тип", "type", 60),
    ColumnProperties("Сообщения", "messageCount", 60),
    ColumnProperties("Местоположение", "address", 400)
  )

  val types = Array("Стоянка", "Заправка", "Остановка")
  val locations = Array("улица Сталеваров, Ивановское, Москва","16, Свободный проспект, Новогиреево, Москва","8/1, улица Борисовские Пруды, Зябликово", "33А, Свободный проспект, Новогиреево, Москва")

  def genData() = {
    val size = 60
    (1 to size).map(n => {
      Map[String,AnyRef]("num" -> Int.box(n), "type" -> types(Random.nextInt(types.length)), "messageCount" -> Int.box(Random.nextInt(7557)), "address" -> locations(Random.nextInt(locations.length)))
    })
  }
  @RequestMapping(value=Array("/reportPrinter/test"))
  def print(response: HttpServletResponse) = {
    response.setContentType("text/html; charset=UTF-8")
    val writer = response.getWriter

    writer.println("<!DOCTYPE html>")
    writer.println("<html style=\"border:0;margin:0;padding:0;\">")

    writer.println("<body style=\"border:0;margin:0;padding:0;\">")

    printGrid(writer,testModel, genData())

    writer.println("</body")
    writer.println("</html")
  }
}


