package ru.sosgps.wayrecall.monitoring.web

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date

import javax.imageio.ImageIO
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import net.sf.jasperreports.engine._
import net.sf.jasperreports.engine.data._
import net.sf.jasperreports.engine.export._
import net.sf.jasperreports.engine.export.ooxml._
import net.sf.jasperreports.export._
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestParam}
import ru.sosgps.wayrecall.billing.tariff.TariffEDS
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.data.PackagesStore
import ru.sosgps.wayrecall.security.{ObjectsPermissionsChecker, PermissionValue}

import scala.collection.JavaConversions._

/*
  TODO отделить слой получения данных от слоя обработки запросов и высылки ответов, статическая типизация
 */
@Controller
class ReportGenerator extends grizzled.slf4j.Logging {

  @Autowired
  var packStore: PackagesStore = null

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var or: ObjectsRepositoryReader = null

  @Autowired
  var iconf: InstanceConfig = null

  @Autowired
  var translations: Translations = null

  @Autowired
  var repStat: MovementStatsReport = null

  @Autowired
  var repParking: ParkingReport = null

  @Autowired
  var repMoving: MovingReport = null

  @Autowired
  var repFueling: FuelingReport = null

  @Autowired
  var permissions: ObjectsPermissionsChecker = null

  @Autowired
  var reportDataProvider: ReportDataProvider = null

  @Autowired
  var reportMapGenerator: ReportMapGenerator = null

  @Autowired
  var tariffEDS: TariffEDS = null

  def getReportType(repType: String) = {
    repType match {
      case "map" => ReportType.MAP
      case "sensor" => ReportType.SENSOR
      case "speed" => ReportType.SPEED
      case "fuelgraph" => ReportType.FUEL_GRAPH
      case "fueling" => ReportType.FUELING
      case "moving" => ReportType.MOVING
      case "parking" => ReportType.PARKING
      case "objectEvents" => ReportType.OBJECT_EVENTS
      case _ => ReportType.MOVEMENT_STATS // TODO безобразие
    }
  }

  def getReportFilename(repType: ReportType.Value) = {
    repType match {
      case ReportType.MAP => "imgChartReport.jasper"
      case ReportType.SENSOR => "imgChartReport.jasper"
      case ReportType.SPEED => "imgChartReport.jasper"
      case ReportType.FUEL_GRAPH => "imgChartReport.jasper"
      case ReportType.FUELING => "fuelingGridReport.jasper"
      case ReportType.MOVING => "movingGridReport.jasper"
      case ReportType.PARKING => "parkingGridReport.jasper"
      case ReportType.OBJECT_EVENTS => "objectEventsGridReport.jasper"
      case ReportType.MOVEMENT_STATS => "statGridReport.jasper"
    }
  }

  def fitImageToRatio(image: BufferedImage, ratio: Double) = {
    val height = image.getHeight
    val width = image.getWidth
    val currentRatio = width / height.toDouble
    if (currentRatio < ratio) {
      val desiredHeight = (width / ratio).toInt
      require(height > desiredHeight, "height > desiredHeight")
      val diff = height - desiredHeight
      image.getSubimage(0, diff / 2, width, desiredHeight)
    }
    else {
      val desiredWidth = (height * ratio).toInt
      require(width > desiredWidth, "width > desiredWidth")
      val diff = width - desiredWidth
      image.getSubimage(diff / 2, 0, desiredWidth, height)
    }
  }

  def getReportParams(repType: ReportType.Value, uid: String, from: Date, to: Date, request: HttpServletRequest) = {
    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val image = if (Files.exists(iconf.path.resolve("monitoringlogo.png"))) {
      ImageIO.read(Files.newInputStream(iconf.path.resolve("monitoringlogo.png")))
    } else {
      //TODO:Убрать хардкод
      ImageIO.read(new java.net.URL("http://wayrecall.ksb-stels.ru:9080/EDS/logo/monitoringlogo.png"))
    }
    val dateFormat = new SimpleDateFormat(tr("format.scala.datetime"))
    val params = scala.collection.mutable.Map(
      "imgLogo" -> image,
      "trPage" -> tr("basereport.page"),
      "trOf" -> tr("basereport.pageof"),
      "formatDate" -> tr("format.scala.datetime"),
      "trObject" -> tr("basereport.objectfield"),
      "trSdate" -> tr("basereport.fromdate"),
      "trFdate" -> tr("basereport.toDate"),
      "dataObject" -> or.getObjectName(uid),
      "dataSdate" -> dateFormat.format(from),
      "dataFdate" -> dateFormat.format(to),
      "subDataSource" -> reportDataProvider.getJRData(repType, uid, from, to)
    )

    repType match {
      case ReportType.MAP => {
        val maxWidth = 802
        val maxHeight = 430
        params.put("trTitle", tr("main.report") + " «" + tr("main.reports.map") + "»")
        val history = packStore.getHistoryFor(uid, from, to)
        if (history.nonEmpty) {
          val image = reportMapGenerator.genPathImage(history, Some(maxWidth.toDouble / maxHeight))
          params.put("imgChart", fitImageToRatio(image, maxWidth.toDouble / maxHeight))
        }
        params
      }
      case ReportType.SENSOR => {
        val history = packStore.getHistoryFor(uid, from, to)
        val sensors = Option(request.getParameterValues("sensor")).getOrElse(Array.empty)
        val chart = reportDataProvider.getXYChart(ReportType.SENSOR, uid, history, sensors)
        params.put("trTitle", tr("main.report") + " «" + tr("main.reports.sensors") + "»")
        val imgChart = chart.createBufferedImage(802, 430)
        params.put("imgChart", imgChart)
        params
      }
      case ReportType.SPEED => {
        val history = packStore.getHistoryFor(uid, from, to)
        val chart = reportDataProvider.getXYChart(ReportType.SPEED, uid, history)
        params.put("trTitle", tr("main.report") + " «" + tr("main.reports.speed") + "»")
        val imgChart = chart.createBufferedImage(802, 430)
        params.put("imgChart", imgChart)
        params
      }
      case ReportType.FUEL_GRAPH => {
        val history = packStore.getHistoryFor(uid, from, to)
        val chart = reportDataProvider.getXYChart(ReportType.FUEL_GRAPH, uid, history)
        params.put("trTitle", tr("main.report") + " «" + tr("main.reports.fuelgraph") + "»")
        val imgChart = chart.createBufferedImage(802, 430)
        params.put("imgChart", imgChart)
        params
      }
      case ReportType.FUELING => {
        params.put("trTitle", tr("main.report") + " «" + tr("main.reports.fuelings") + "»")
        params.put("trNum", tr("basereport.num"))
        params.put("trStart", tr("fuelinggrid.columns.datetime"))
        params.put("trType", tr("fuelinggrid.columns.type"))
        params.put("trStartVal", tr("fuelinggrid.columns.startval"))
        params.put("trEndVal", tr("fuelinggrid.columns.endval"))
        params.put("trVolume", tr("fuelinggrid.columns.volume"))
        params.put("trPlace", tr("pathgrid.columns.place"))
        params
      }
      case ReportType.MOVING => {
        params.put("trTitle", tr("main.report") + " «" + tr("main.reports.movings") + "»")
        params.put("trNum", tr("basereport.num"))
        params.put("trStart", tr("movinggroupgrid.columns.sDate"))
        params.put("trInterval", tr("movinggrid.columns.duration"))
        params.put("trDistance", tr("movinggrid.columns.distance"))
        params.put("trMaxSpeed", tr("movinggrid.columns.speed"))
        params.put("trSPlace", tr("movinggrid.columns.startposition"))
        params.put("trFPlace", tr("movinggrid.columns.finishposition"))
        params
      }
      case ReportType.PARKING => {
        params.put("trTitle", tr("main.report") + " «" + tr("main.reports.parkings") + "»")
        params.put("trNum", tr("basereport.num"))
        params.put("trType", tr("parkinggrid.columns.intervalType"))
        params.put("trStart", tr("movinggroupgrid.columns.sDate"))
        params.put("trInterval", tr("movinggrid.columns.duration"))
        params.put("trPlace", tr("pathgrid.columns.place"))
        params
      }
      case ReportType.OBJECT_EVENTS => {
        params.put("trTitle", tr("main.report") + " «" + tr("main.reports.objectevents") + "»")
        params.put("trNum", tr("basereport.num"))
        params.put("trTime", tr("eventsreportgrid.time"))
        params.put("trEventType", tr("eventsreportgrid.type"))
        params.put("trMessage", tr("eventsreportgrid.message"))
        params
      }
      case ReportType.MOVEMENT_STATS => {
        params.put("trTitle", tr("main.report") + " «" + tr("main.reports.statistics") + "»")
        params
      }
    }
  }

  def getFilledReport(rType: String, uid: String, from: Date, to: Date, request: HttpServletRequest) = {

    val reportType = getReportType(rType)
    val params = getReportParams(reportType, uid, from, to, request)
    val dataSource = new JRMapCollectionDataSource(reportDataProvider.getJRData(reportType, uid, from, to))
    val fileName = System.getenv("WAYRECALL_HOME") + "/reports/" + getReportFilename(reportType)

    JasperFillManager.fillReport(fileName, new java.util.HashMap[String, Object](params), dataSource);
  }

  @RequestMapping(Array("/generatePDF/{repType}.pdf"))
  def generatePDFReport(@PathVariable("repType") repType: String,
                        @RequestParam("uid") uid: String,
                        @RequestParam("from") from: Date,
                        @RequestParam("to") to: Date,
                        request: HttpServletRequest,
                        response: HttpServletResponse) {

    if (permissions.hasPermission(uid, PermissionValue.VIEW)) {
      response.setContentType("application/pdf")
      response.setCharacterEncoding("UTF-8")

      val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(from, to)

      try {
        val input = getFilledReport(repType, uid, correctFrom, correctTo, request)
        JasperExportManager.exportReportToPdfStream(input, response.getOutputStream)

      } catch {
        case jre: JRException => jre.printStackTrace()
        case e: Exception => e.printStackTrace()

      }
    } else {
      response.sendError(403)
    }

  }

  @RequestMapping(Array("/generateXLS/{repType}.xls"))
  def generateXLSReport(@PathVariable("repType") repType: String,
                        @RequestParam("uid") uid: String,
                        @RequestParam("from") from: Date,
                        @RequestParam("to") to: Date,
                        request: HttpServletRequest,
                        response: HttpServletResponse) {

    if (permissions.hasPermission(uid, PermissionValue.VIEW)) {
      response.setContentType("application/vnd.ms-excel")

      val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(from, to)

      try {
        val exporter = new JRXlsExporter()
        val input = getFilledReport(repType, uid, correctFrom, correctTo, request)
        exporter.setExporterInput(new SimpleExporterInput(input));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream));
        val config = new SimpleXlsExporterConfiguration();
        exporter.setConfiguration(config);

        exporter.exportReport();

      } catch {
        case jre: JRException => jre.printStackTrace()
        case e: Exception => e.printStackTrace()

      }
    } else {
      response.sendError(403)
    }

  }

  @RequestMapping(Array("/generateCSV/{repType}.csv"))
  def generateCSVReport(@PathVariable("repType") repType: String,
                        @RequestParam("uid") uid: String,
                        @RequestParam("from") from: Date,
                        @RequestParam("to") to: Date,
                        request: HttpServletRequest,
                        response: HttpServletResponse) {

    if (permissions.hasPermission(uid, PermissionValue.VIEW)) {
      response.setContentType("text/csv")

      val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(from, to)

      try {
        val exporter = new JRCsvExporter()
        val input = getFilledReport(repType, uid, correctFrom, correctTo, request)
        exporter.setExporterInput(new SimpleExporterInput(input));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(response.getOutputStream));
        val config = new SimpleCsvExporterConfiguration();
        exporter.setConfiguration(config);

        exporter.exportReport();

      } catch {
        case jre: JRException => jre.printStackTrace()
        case e: Exception => e.printStackTrace()

      }
    } else {
      response.sendError(403)
    }

  }

  @RequestMapping(Array("/export2PDF/report.pdf"))
  def generateBatchPDFReport(@RequestParam("uid") uid: String,
                             @RequestParam("from") from: Long,
                             @RequestParam("to") to: Long,
                             @RequestParam("repList") repList: String,
                             request: HttpServletRequest,
                             response: HttpServletResponse) {

    if (permissions.hasPermission(uid, PermissionValue.VIEW)) {
      response.setContentType("application/pdf")
      response.setCharacterEncoding("UTF-8")

      val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(new Date(from), new Date(to))

      val checkedReps = if (repList.nonEmpty) {
        repList.split(";")
      } else Array("stat")


      val inputReps = checkedReps.map(s => {
        getFilledReport(s, uid, correctFrom, correctTo, request)
      })

      try {
        val exporter = new JRPdfExporter()
        exporter.setExporterInput(SimpleExporterInput.getInstance(inputReps.toList))
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream))

        exporter.exportReport()

      } catch {
        case jre: JRException => jre.printStackTrace()
        case e: Exception => e.printStackTrace()

      }
    } else {
      response.sendError(403)
    }

  }

  @RequestMapping(Array("/export2XLS/report.xls"))
  def generateBatchXLSReport(@RequestParam("uid") uid: String,
                             @RequestParam("from") from: Long,
                             @RequestParam("to") to: Long,
                             @RequestParam("repList") repList: String,
                             request: HttpServletRequest,
                             response: HttpServletResponse) {

    if (permissions.hasPermission(uid, PermissionValue.VIEW)) {
      response.setContentType("application/vnd.ms-excel")

      val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(new Date(from), new Date(to))

      val checkedReps = if (repList.nonEmpty) {
        repList.split(";")
      } else Array("stat")

      val inputReps = checkedReps.map(s => {
        getFilledReport(s, uid, correctFrom, correctTo, request)
      })

      try {
        val exporter = new JRXlsExporter()
        exporter.setExporterInput(SimpleExporterInput.getInstance(inputReps.toList))
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream))

        exporter.exportReport()

      } catch {
        case jre: JRException => jre.printStackTrace()
        case e: Exception => e.printStackTrace()

      }
    } else {
      response.sendError(403)
    }

  }

  @RequestMapping(Array("/export2DOCX/report.docx"))
  def generateBatchDOCXReport(@RequestParam("uid") uid: String,
                              @RequestParam("from") from: Long,
                              @RequestParam("to") to: Long,
                              @RequestParam("repList") repList: String,
                              request: HttpServletRequest,
                              response: HttpServletResponse) {

    if (permissions.hasPermission(uid, PermissionValue.VIEW)) {
      response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")

      val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(new Date(from), new Date(to))

      val checkedReps = if (repList.nonEmpty) {
        repList.split(";")
      } else Array("stat")

      val inputReps = checkedReps.map(s => {
        getFilledReport(s, uid, correctFrom, correctTo, request)
      })

      try {
        val exporter = new JRDocxExporter()
        exporter.setExporterInput(SimpleExporterInput.getInstance(inputReps.toList))
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(response.getOutputStream))

        exporter.exportReport()

      } catch {
        case jre: JRException => jre.printStackTrace()
        case e: Exception => e.printStackTrace()

      }
    } else {
      response.sendError(403)
    }

  }

  @RequestMapping(Array("/xychart/{repType}.png"))
  def generateChart(@PathVariable("repType") rType: String,
                    @RequestParam("uid") uid: String,
                    @RequestParam("from") from: Date,
                    @RequestParam("to") to: Date,
                    request: HttpServletRequest,
                    response: HttpServletResponse) {

    if (permissions.hasPermission(uid, PermissionValue.VIEW)) {
      response.setContentType("image/png")

      val (correctFrom, correctTo) = tariffEDS.correctReportWorkingDates(from, to)

      val reportType = getReportType(rType)
      val params = Option(request.getParameterValues("sensorName")).getOrElse(Array.empty)
      // val param = Option(request.getParameter("sensorName")).getOrElse("")
      val history = packStore.getHistoryFor(uid, correctFrom, correctTo)
      val chart = reportDataProvider.getXYChart(reportType, uid, history, params)
      val image = chart.createBufferedImage(802, 535)

      ImageIO.write(image, "png", response.getOutputStream)
    } else {
      response.sendError(403)
    }

  }

}

@Controller
class ReportGeneratorEndpoint(@Qualifier("org.springframework.security.authenticationManager") @Autowired a: AuthenticationManager,
                               @Autowired rg: ReportGenerator
                             ) extends EDSEndpointBase(a) {
  @RequestMapping(Array("/getPDF/report.pdf"))
  def getPdf(@RequestParam("uid") uid: String,
             @RequestParam("from") from: Long,
             @RequestParam("to") to: Long,
             @RequestParam("repList") repList: String,
             request: HttpServletRequest,
             response: HttpServletResponse
            ) {
    basicAuth(request, response) {
      println("ReportGeneratorEndpoint")
      rg.generateBatchPDFReport(uid, from, to, repList, request, response)
    }
  }
}