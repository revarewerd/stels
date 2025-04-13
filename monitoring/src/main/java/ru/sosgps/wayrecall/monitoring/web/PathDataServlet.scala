/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.sosgps.wayrecall.monitoring.web


import java.util.Date
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import ru.sosgps.wayrecall.monitoring.processing.fuelings._
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.core._
import com.mongodb.casbah.commons.MongoDBObject
import ru.sosgps.wayrecall.billing.tariff.TariffEDS
import ru.sosgps.wayrecall.core._

import collection.{immutable, mutable}
import ru.sosgps.wayrecall.utils.{LimitsSortsFilters, PartialIterable, tryDouble}
import ru.sosgps.wayrecall.data.{MovementHistory, PackagesStore}
import ru.sosgps.wayrecall.monitoring.processing
import ru.sosgps.wayrecall.monitoring.processing.urban.PointInUrbanArea
import ru.sosgps.wayrecall.regeocoding.DirectNominatimRegeocoder
import ru.sosgps.wayrecall.utils.web.{BasicScalaServlet, ScalaHttpRequest}

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.mutable.ArrayBuffer
import ru.sosgps.wayrecall.utils.web.{ScalaJson => Jerkson}
class PathDataServlet extends BasicScalaServlet with grizzled.slf4j.Logging {

  val fuelSensors = Set("fuel_lvl", "Digital_fuel_sensor_B1", "Fuel_level_%", "io_2_67")

  protected def processRequest(request: ScalaHttpRequest, response: HttpServletResponse): Unit = {

    val store = getSpringBean[PackagesStore]()
    val tariffEDS = getSpringBean[TariffEDS]

    val (from, to) = tariffEDS.correctReportWorkingDates(ReportsTransitionalUtils.getWorkingDates(request))

    response.setContentType("application/json;charset=UTF-8");

    val selectedUid: String = ReportsTransitionalUtils.getSelectedId(request)
    //        println("request.getParameterStrict(\"selected\")="+request.getParameterStrict("selected"))
    //        println("selectedUid="+selectedUid)

    val out = response.getWriter();
    val mdbm = getSpringBean[MongoDBManager]()
    val objects = mdbm.getDatabase()("objects")
    val sensors = mdbm.getDatabase()("sensorNames")
    val or = getSpringBean[ObjectsRepositoryReader]()


    try {

      def history: Iterable[GPSData] = store.getHistoryFor(selectedUid, from, to, LimitsSortsFilters.empty);

      request.getParameterOption("data").getOrElse("") match {

        case "speedgraph" =>
          debug("building speedgraph for " + selectedUid + " " + (from, to))
          //          val dateformat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
          history.iterator.foreach(gpsdata =>
            out.println(/*dateformat.format(gpsdata.time)*/ gpsdata.time.getTime + "," + gpsdata.speed)
          )

        case "sensors" =>
          debug("building sensorsgraph for " + selectedUid + " " + (from, to))
          val sensors = Option(request.getParameterValues("sensor")).getOrElse(Array("pwr_ext"))
          require(sensors.length < 3)
          //          val dateformat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
          val converters = sensors.map(sensor => {
            if (fuelSensors.contains(sensor)) {
              new FuelConversions(selectedUid, sensor, mdbm)
            } else {
              new ObjectDataConversions(selectedUid, sensor, mdbm)
            }
          })

          val sensorToConverter = (sensors, converters).zipped
          debug(s"Sensors are: $sensors, converters are $converters")
          history.iterator.foreach(gpsData => {
            out.println(
              gpsData.time.getTime + "," + sensorToConverter.map((sensor, converter) => converter.getPointValues(gpsData)._1).mkString(",")
            )
          })


        case "fuelgraph" => {
          debug("building fuelgraph for " + selectedUid + " " + (from, to))
          val graphs = Option(request.getParameterValues("graph")).getOrElse(Array.empty)
          // Убеждаемся, что параметры входят в множество
          val graphSet = graphs.toSet
          require((graphSet -- Set("speed", "fuel", "fuelSmoothed", "urban", "fuelLevelByCalcNorms","fuelLevelByNorms",
            "fuelStandards", "distance", "fuelWithCutStarts", "states")).isEmpty)
          // Ограничиваем двумя осями с учетом того, что fuel и fuelSmoothed могут разделять одну ось
          //require(graphs.size < 3 || graphs.size == 3 && graphSet("fuel") && graphSet("fuelSmoothed"))

          trait FuelGraphSeries {
            def getValueForPoint(date: Date): String
          }
          case class FuelSeries(var recs: Iterable[FuelDataRec]) extends FuelGraphSeries {
            override def getValueForPoint(date: Date): String = {
              while (recs.nonEmpty && recs.head.point.time.before(date)) {
                recs = recs.tail
              }
              if (recs.isEmpty || recs.head.point.time.after(date))
                ""
              else
                recs.head.value.toString
            }
          }

          debug("History class is " + history.getClass.getName)
          case class SpeedSeries(var history: Iterable[GPSData]) extends FuelGraphSeries {
            override def getValueForPoint(date: Date): String = {
              while (history.nonEmpty && history.head.time.before(date))
                history = history.tail
              if (history.isEmpty || history.head.time.after(date))
                ""
              else history.head.speed.toString
            }
          }

          val regeo = getSpringBean[DirectNominatimRegeocoder]()

          case class UrbanSeries(var history: Iterable[GPSData], isUrban: (GPSData) => Boolean) extends FuelGraphSeries {
            override def getValueForPoint(date: Date): String = {
              while (history.nonEmpty && history.head.time.before(date)) {
                history = history.tail
              }
              if (history.isEmpty || history.head.time.after(date))
                ""
              else
              {
                if (isUrban(history.head)) "1" else "0"
              }
            }
          }

          case class StandardSeries(var values: Iterable[(Date,Int)]) extends FuelGraphSeries  {
            override def getValueForPoint(date: Date): String = {
              while (values.nonEmpty && values.head._1.before(date)) {
                values = values.tail
              }
              if(values.isEmpty || values.head._1.after(date))
                ""
              else {
                values.head._2.toString
              }
            }
          }

          case class DerivativeSeries(var values: Iterable[(Date, Double)])  extends FuelGraphSeries  {
            override def getValueForPoint(date: Date): String = {
              while (values.nonEmpty && values.head._1.before(date)) {
                values = values.tail
              }
              if(values.isEmpty || values.head._1.after(date))
                ""
              else {
                values.head._2.toString
              }
            }
          }

          case class DistanceSeries(history: Iterable[GPSData]) extends FuelGraphSeries {

            import ru.sosgps.wayrecall.monitoring.processing
            var prev = 0.0
            var values = if (history.isEmpty)
              Iterable.empty
            else {
              var prev = history.head
              var acc = 0.0
              Iterable((history.head.time, 0.0)) ++ history.tail.toIterator.map(point => {
                val result = (point.time,acc + processing.distanceBetweenPackets(prev,point))
                prev = point
                acc = result._2
                result
              })
            }

            override def getValueForPoint(date: Date): String = {
              while (values.nonEmpty && values.head._1.before(date)) {
                values = values.tail
              }
              if (values.isEmpty || values.head._1.after(date))
                ""
              else {
                values.head._2.toString
              }
            }
          }

          case class StatesSeries(var values: Seq[FuelBehavior]) extends FuelGraphSeries {
            override def getValueForPoint(date: Date): String = {
              // Пропускаем интервалы до даты
              while (values.nonEmpty && values.head.lastPoint.point.time.before(date)) {
                values = values.tail
              }
              if(values.nonEmpty && !values.head.firstPoint.point.time.after(date) ) {
                values.head match {
                  case r: Refueling => "2"
                  case d: FuelDraining => "1"
                }
              }
              else {
                ""
              }


            }
          }




          val frs = getSpringBean[FuelingReportService]()
          lazy val fuelData = frs.getFuelings(selectedUid, history.toSeq)
          val fuelValues = fuelData.smoothedPoints

          lazy val mono = fuelData.toMonotonicSeq(fuelData.filterMovementStarts(fuelValues))

          val fuelSeries: Array[FuelGraphSeries] = graphs.map {
            case "speed" => SpeedSeries(history)
            case "fuel" => FuelSeries(fuelData.fuelDataRecs)
            case "fuelSmoothed" => FuelSeries(fuelValues)
            case "fuelWithCutStarts" => FuelSeries(mono)
            case "urban" => UrbanSeries(history, fuelData.isUrban)
            case "fuelLevelByCalcNorms" => DerivativeSeries(fuelData.expectedFuelLevel(true))
            case "fuelLevelByNorms" => DerivativeSeries(fuelData.expectedFuelLevel(false))
            case "fuelStandards" => StandardSeries(
              fuelData.pointsAndStandards(fuelValues).map{case (rec, st) => (rec.point.time, fuelData.standardToNumber(st))}
            )
            case "distance" => DistanceSeries(history)
            case "states" => StatesSeries(fuelData.newStates)
          }

          val timeSeries = if(graphs.size == 1 && graphs.head == "fuelWithCutStarts")
            mono.map(_.point.time)
          else
            history.toStream.map(_.time)


          timeSeries.map(date => date.getTime + "," + fuelSeries.map(s => s.getValueForPoint(date)).mkString(","))
            .toIterator
            .foreach(out.println)

        }
        case "path" =>
          debug("building path for " + selectedUid + " " + (from, to))
          var minlat = Double.PositiveInfinity
          var minlon = Double.PositiveInfinity
          var maxlat = Double.NegativeInfinity
          var maxlon = Double.NegativeInfinity

          val first = history.headOption

          var last: GPSData = null

          history.iterator.filter(_.goodlonlat).foreach((he: GPSData) => {
            if (minlat > he.lat)
              minlat = he.lat
            if (maxlat < he.lat)
              maxlat = he.lat
            if (minlon > he.lon)
              minlon = he.lon
            if (maxlon < he.lon)
              maxlon = he.lon

            last = he
          })

          Jerkson.generate(mutable.LinkedHashMap(
            "pathdata" -> Map(
              "uid" -> selectedUid
            ),
            "success" -> true,
            "maxlat" -> maxlat,
            "minlat" -> minlat,
            "minlon" -> minlon,
            "maxlon" -> maxlon,
            "first" -> first.map(first => Map("lon" -> first.lon, "lat" -> first.lat)).orNull,
            "last" -> Option(last).map(last => Map("lon" -> last.lon, "lat" -> last.lat)).orNull
          ), out)


        case "grid" =>
          debug("building grid for " + selectedUid + " " + (from, to))
          val limit = request.getParameterStrict("limit").toInt
          val page = request.getParameterStrict("page").toInt
          val start = request.getParameterStrict("start").toInt

          val (totalSize: Int, all: Iterator[GPSData]) = {
            val values = store.getHistoryFor(selectedUid, from, to, new LimitsSortsFilters(start, limit))
            (values.total, values.iterator)
          }


          //println("start/limit=" + start + "+" + limit)

          val gridItems: Iterator[Map[String, Any]] = all.zipWithIndex.map({
            case (g, i) => {
              Map(
                "num" -> (i + start + 1),
                "name" -> objects.findOne(MongoDBObject("uid" -> g.uid), MongoDBObject("name" -> 1)).map(_.get("name").asInstanceOf[String]).getOrElse(g.uid),
                ("uid" -> g.uid),
                ("lat" -> g.lat),
                ("lon" -> g.lon),
                ("satelliteNum" -> g.satelliteNum),
                ("speed" -> g.speed.toString),
                ("course" -> g.course.toString),
                //                  ("regeo" -> positions(i)),
                ("regeo" -> Option(g.placeName).getOrElse("")),
                ("time" -> ReportsTransitionalUtils.ISODateTime(g.time)),
                ("insertTime" -> Option(g.insertTime).map(ReportsTransitionalUtils.ISODateTime).getOrElse("")),
                "devdata" -> Option(g.data).map(_.map(kv => kv._1 + " = " + kv._2).toSeq.sorted.mkString(", ")).getOrElse("")
              )
            }
          }
          )

          Jerkson.generate(Map("success" -> true, "items" -> gridItems, "totalCount" -> totalSize), out)


      }
    } finally {
      out.close();
    }


  }

}

object ReportsTransitionalUtils extends grizzled.slf4j.Logging {

  import scala.collection.JavaConversions.mapAsScalaMap

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss"

  def getWorkingDates(request: scala.collection.Map[String, AnyRef]): (Date, Date) =
    (
      for (
        to <- request.get("to");
        from <- request.get("from")
      )
        yield (utils.parseDate(from), utils.parseDate(to))).getOrElse({
      {
        warn("request uses old WorkingDates " + request)

        val map = Jerkson.parse[Map[String, String]](request("workingDates").asInstanceOf[String])
        (utils.parseDate(map("from")), utils.parseDate(map("to")))
      }
    })


  def getSelectedId(request: scala.collection.Map[String, AnyRef]): String = {
    //TODO: убрать генерацию и парсинг JSON-а тут и везде в вызовах, так как передаётся всегда только один айдишник
    request("selected").asInstanceOf[String].split(",").head.stripPrefix("[").stripSuffix("]").stripPrefix("\"").stripSuffix("\"")
  }

  def getWorkingDates(request: HttpServletRequest): (Date, Date) = this.getWorkingDates(request.getParameterMap.mapValues(_.head))

  def getSelectedId(request: HttpServletRequest): String = getSelectedId(request.getParameterMap.mapValues(_.head))

  @deprecated("use ru.sosgps.wayrecall.utils.ISODateTime")
  def ISODateTime(date: Date): String = utils.ISODateTime(date)

  def formatPeriod(mills: Long): String = {
    val aDuration = mills / 1000
    val hours = aDuration / 3600;
    val minutes = (aDuration - hours * 3600) / 60;
    val seconds = (aDuration - (hours * 3600 + minutes * 60));
    String.format("%02d:%02d:%02d", Array(hours.asInstanceOf[AnyRef], minutes.asInstanceOf[AnyRef], seconds.asInstanceOf[AnyRef]): _*)
  }

}
