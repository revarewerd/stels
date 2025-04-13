package ru.sosgps.wayrecall.monitoring.web

import java.awt.{BasicStroke, Color}
import java.util.Date

import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.mongodb.casbah.Imports._
import org.jfree.chart.ChartFactory
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.StandardXYItemRenderer
import org.jfree.data.time.{FixedMillisecond, TimeSeries, TimeSeriesCollection, TimeSeriesDataItem}
import org.jfree.ui.RectangleInsets
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core._
import ru.sosgps.wayrecall.data.{MovementHistory, PackagesStore}
import ru.sosgps.wayrecall.monitoring.processing.fuelings.FuelingReportService

/**
  * Created by ivan on 18.07.16.
  */

object ReportType extends Enumeration {
  val
  SENSOR,
  SPEED,
  FUEL_GRAPH,
  FUELING,
  MOVING,
  PARKING,
  OBJECT_EVENTS,
  MAP,
  MOVEMENT_STATS = Value
}


class ReportDataProvider {

  @Autowired
  var packStore: PackagesStore = null

  @Autowired
  var repStat: MovementStatsReport = null

  @Autowired
  var repParking: ParkingReport = null

  @Autowired
  var repMoving: MovingReport = null

  @Autowired
  var eventsReport: EventsReport = _

  @Autowired
  var repFueling: FuelingReport = null

  @Autowired
  var fuelingReportService: FuelingReportService = _

  @Autowired
  var mdbm: MongoDBManager = null

  @Autowired
  var translations: Translations = null

  @Autowired
  var or: ObjectsRepositoryReader = null


  def getJRData(repType: ReportType.Value, uid: String, from: Date, to: Date) = {
    repType match {
      case ReportType.SENSOR => new java.util.Vector[java.util.Map[String, _]]()
      case ReportType.SPEED => new java.util.Vector[java.util.Map[String, _]]()
      case ReportType.FUEL_GRAPH => new java.util.Vector[java.util.Map[String, _]]()
      case ReportType.FUELING => repFueling.genJRDataSource(uid, from, to)
      case ReportType.MOVING => repMoving.genJRDataSource(uid, from, to)
      case ReportType.PARKING => repParking.genJRDataSource(uid, from, to)
      case ReportType.OBJECT_EVENTS => eventsReport.getJRData(uid,from,to)
      case ReportType.MOVEMENT_STATS => repStat.genJRDataSource(uid, from, to)
      case ReportType.MAP => new java.util.Vector[java.util.Map[String, _]]()
    }
  }


  def getXYChartData(chartType: ReportType.Value, uid: String, history: MovementHistory, param: String) = {
    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val fuelParam = if (chartType == ReportType.FUEL_GRAPH)
    {
      fuelingReportService.fuelSensorsNames(uid).headOption.getOrElse("")
    } else ""


    val seriesName = chartType match {
      case ReportType.FUEL_GRAPH => tr("fuelgraphreport.axisLabels.fuel")
      case ReportType.SPEED => tr("fuelgraphreport.axisLabels.speed")
      case ReportType.SENSOR => if (param.nonEmpty) param else "Values"
    }

    // TODO отделить все эти ветвления от непосредственой генерации графика
    val series = new TimeSeries(seriesName, classOf[FixedMillisecond])

    var prevTime: java.util.Date = null
    val converter = if (param.nonEmpty) {
      if (Set("fuel_lvl", "Digital_fuel_sensor_B1", "Fuel_level_%", "io_2_67").contains(param)) {
        Some(new FuelConversions(uid, param, mdbm))
      } else {
        Some(new ObjectDataConversions(uid, param, mdbm))
      }
    } else if (fuelParam.nonEmpty) {
      Some(new FuelConversions(uid, fuelParam, mdbm))
    } else None

    history.foreach(gd => {
      if (!gd.time.equals(prevTime)) {
        val value = chartType match {
          case ReportType.SENSOR => converter match {
            case Some(c) => c.getPointValues(gd)._1
            case None => 0.0
          }
          case ReportType.FUEL_GRAPH => converter match {
            case Some(c) => c.getPointValues(gd)._1
            case None => 0.0
          }
          case ReportType.SPEED => gd.speed
        }

        series.add(new TimeSeriesDataItem(new FixedMillisecond(gd.time), value), false)
//        if (chartType == ReportType.FUEL_GRAPH) {
//          fuelSeries.get.add(new TimeSeriesDataItem(new FixedMillisecond(gd.time), gd.speed), false)
//        }
      }
      prevTime = gd.time
    })

    val tsCol = new TimeSeriesCollection(series)
    tsCol
  }

  def getXYChart(chartType: ReportType.Value, uid: String, history: MovementHistory, params: Array[String] = Array()) = {
    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr
    val yAxis = chartType match {
      case ReportType.SENSOR => tr("basereport.sensors.valuesof") + " «" + params(0) + "»"
      case ReportType.FUEL_GRAPH => tr("fuelgraphreport.axisLabels.fuel") + ", " + tr("units.fuellevel")
      case ReportType.SPEED => tr("fuelgraphreport.axisLabels.speed") + ", " + tr("units.speed")
    }

    val y2Axis = chartType match {
      case ReportType.SENSOR => params.tail.headOption.map(tr("basereport.sensors.valuesof") + " «" + _ + "»")
      case ReportType.FUEL_GRAPH => Some(tr("fuelgraphreport.axisLabels.speed") + ", " + tr("units.speed"))
      case _ => None
    }


    val data = getXYChartData(chartType, uid, history, params.headOption.getOrElse(""))

    val isLegend = if (chartType == ReportType.FUEL_GRAPH || y2Axis.isDefined) true else false

    val chart = ChartFactory.createTimeSeriesChart("", "", yAxis, data, isLegend, false, false)
    val plot = chart.getPlot().asInstanceOf[XYPlot]


    plot.setBackgroundPaint(Color.WHITE)
    plot.setDomainGridlinePaint(Color.LIGHT_GRAY)
    plot.setRangeGridlinePaint(Color.LIGHT_GRAY)
    plot.setAxisOffset(new RectangleInsets(0.0, 0.0, 0.0, 0.0))
    plot.getRenderer().setSeriesPaint(0, Color.decode("0X008080"))
    plot.getRenderer().setSeriesStroke(0, new BasicStroke(2))
    plot.getRenderer().setSeriesStroke(1, new BasicStroke(2))
    y2Axis.foreach(y2name => {
       val y2 = new NumberAxis(y2name)
       val y1 = plot.getDomainAxis(0)
       y2.setLabelFont(y1.getLabelFont)
       y2.setLabelPaint(y1.getLabelPaint)
       y2.setTickLabelFont(y1.getTickLabelFont)
       y2.setTickLabelPaint(y1.getTickLabelPaint)
       y2.setAutoRangeIncludesZero(false)
       plot.setRangeAxis(1, y2)
       val secondAxisData = chartType match {
         case ReportType.SENSOR => getXYChartData(ReportType.SENSOR, uid, history, params(1))
         case _ => getXYChartData(ReportType.SPEED, uid, history, "")
       }

       plot.setDataset(1, secondAxisData)
       plot.mapDatasetToRangeAxis(1,1)
       val secondAxisRenderer = new StandardXYItemRenderer()

       secondAxisRenderer.setSeriesPaint(0, Color.decode("0XFF0000"))
       secondAxisRenderer.setSeriesStroke(0, new BasicStroke(2))
       plot.setRenderer(1,secondAxisRenderer)
    })

    chart
  }
}
