package ru.sosgps.wayrecall.monitoring

import java.util.Date
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, RequestMapping}
import ru.sosgps.wayrecall.monitoring.web.{ParkingReport, MovementStatsReport}
import ru.sosgps.wayrecall.utils.POIUtils.toTypedValue
import scala.collection.JavaConversions.mapAsJavaMap

@Controller
class ExcelExporter {

  @Autowired
  var msr:MovementStatsReport = null

  @Autowired
  var prcs:ParkingReport = null

  @RequestMapping(Array("/excelExport"))
  def process(
               @RequestParam("uid") uid: String,
               @RequestParam("from") from: Date,
               @RequestParam("to") to: Date,
               request: HttpServletRequest,
               response: HttpServletResponse) {
    response.setContentType("application/octet-stream")
    response.setHeader("Content-Disposition", "attachment; filename=\"report.xls\"");

    val workbook = new XSSFWorkbook();
    val sheet1 = workbook.createSheet("Статистика")

    val request1 = new ExtDirectStoreReadRequest

    request1.setParams(Map(
      "selected" -> uid,
      "from" -> from.getTime.toString,
      "to" -> to.getTime.toString
    ))

    for((m,i) <- msr.loadData(request1).zipWithIndex){
      val row = sheet1.createRow(i)
      row.createCell(0).setTypedValue(m("name"))
      row.createCell(1).setTypedValue(m("value").toString)
    }

    for (i <- 0 until 2) {
      sheet1.autoSizeColumn(i)
    }

    val sheet2 = workbook.createSheet("Остановки")

    for((m,i) <- prcs.loadData(request1).zipWithIndex){
      val row = sheet2.createRow(i)
      for((v, j) <- m.values.zipWithIndex){
        row.createCell(j).setTypedValue(v)
      }
    }

    for (i <- 0 until 10) {
      sheet2.autoSizeColumn(i)
    }


    val out = response.getOutputStream
    workbook.write(out)
    out.close()

  }


  }
