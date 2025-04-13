package ru.sosgps.wayrecall.core

import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.{tryDouble, tryInt, tryLong, tryNumerics, tryParseDouble}

class ObjectDataConversions(uid: String, param: String, mdbm: MongoDBManager) extends grizzled.slf4j.Logging {
  
  var tempVal = 0.0
  var tempCanFuel = 0L
  
  val sensorSettings = {
    val settings = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("settings" -> 1))
      .flatMap(_.getAs[BasicDBObject]("settings"))
      .getOrElse(new BasicDBObject())
    val sensors = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("sensors" -> 1))
      .flatMap(_.getAs[MongoDBList]("sensors"))
      .getOrElse(new MongoDBList())
      .toSeq.map(_.asInstanceOf[BasicDBObject])

    val sensor = sensors.find(_.as[String]("paramName") == param).getOrElse(new BasicDBObject())
    val dataTable: Any = if (!sensor.isEmpty) sensor.getOrElse("dataTable", false) else false
    val ratio: Double = if (!sensor.isEmpty) tryParseDouble(sensor.getAsOrElse[String]("ratio", "1")).getOrElse({
      warn(s"cant convert ratio of sensor $sensor for $uid")
      1.0
    }) else 1.0
    val minValue: Double = if (!sensor.isEmpty && !sensor.getAsOrElse[String]("minValue", "").isEmpty) sensor.getAs[String]("minValue").get.toDouble else if (Seq("Digital_fuel_sensor_B1", "fuel_lvl", "io_2_67", "Fuel_level_%").contains(param)) 1.0 else Int.MinValue.toDouble
    val maxValue: Double = if (!sensor.isEmpty && !sensor.getAsOrElse[String]("maxValue", "").isEmpty) sensor.getAs[String]("maxValue").get.toDouble else if (Seq("Digital_fuel_sensor_B1", "io_2_67").contains(param)) 2048.0 else if (Seq("fuel_lvl", "Fuel_level_%", "can2").contains(param)) 254.0 else Int.MaxValue.toDouble
    
    Map(
      "fuelVolume" -> settings.getAsOrElse[Int]("repFuelFullVolume", 100),
      "minDraining" -> settings.getAsOrElse[Int]("repFuelMinDraining", 10),
      "minFueling" -> settings.getAsOrElse[Int]("repFuelMinFueling", 10),
      "minTime" -> settings.getAsOrElse[Int]("repFuelMinTime", 60),
      "filtering" -> settings.getAsOrElse[Int]("repFuelFiltering", 10),
      "dataTable" -> dataTable,
      "ratio" -> ratio,
      "minValue" -> minValue,
      "maxValue" -> maxValue
    )
  }
  
  val conversionTable = {
    val dataTable = sensorSettings.get("dataTable").getOrElse(false)
    val ratio: Double = sensorSettings.getOrElse("ratio", 1.0).asInstanceOf[Double]
    
    dataTable match {
      case l: BasicDBList =>
        l.sliding(2).map(a => {processDataPair(a.asInstanceOf[Seq[BasicDBObject]])}).toSeq
      case e =>
        Seq(new conversionRule(Int.MinValue, Int.MaxValue, ratio))
    }
  }

  def getPointValues(point: GPSData): (Double, Double) = {
    val paramValue = point.data.get(param)
    val isValidIfCAN = if (Set("fuel_lvl", "Fuel_level_%").contains(param)) {
      val canFuel = tryLong(Set("CANBUS_Fuel_used", "can_fuel_used").flatMap(p => Option(point.data.get(p))).headOption.getOrElse(0L))
      if (canFuel != 0L && tempCanFuel != 0L && (canFuel < tempCanFuel || canFuel > 2 * tempCanFuel)) {
        false
      } else {
        tempCanFuel = canFuel
        true
      }
    } else {
      true
    }
    scala.util.control.Exception.allCatch.opt(tryDouble(paramValue)).map(
      {
      case i => {
        if (i < sensorSettings.get("minValue").get.asInstanceOf[Double] || i > sensorSettings.get("maxValue").get.asInstanceOf[Double]) {
          (getRealValue(tempVal), tempVal)
        } else if (isValidIfCAN) {
          tempVal = i
          (getRealValue(i), i)
        } else {
          (getRealValue(tempVal), tempVal)
        }
      }
    }).getOrElse((getRealValue(tempVal), tempVal))
  }
  
  def getRealValue(i: Double): Double = {
    val ratio: Double = sensorSettings.getOrElse("ratio", 1.0).asInstanceOf[Double]
    val rule = conversionTable.find(_.isInRange(i)).getOrElse(new conversionRule(Int.MinValue, Int.MaxValue, ratio))
    
    rule.calculate(i)
  }
  
  def processDataPair(a: Seq[BasicDBObject]) = {
    if (a.nonEmpty && a.head.ne(a.last)) {
      val x1 = tryInt(a.head.get("x"))
      val y1 = tryInt(a.head.get("y"))
      val x2 = tryInt(a.last.get("x"))
      val y2 = tryInt(a.last.get("y"))
      val k = (y2 - y1).toDouble / (x2 - x1)

      new conversionRule(x1, x2, k, (y1 - k * x1).toDouble)
    } else {
      
      new conversionRule(Int.MinValue, Int.MaxValue)
    }
  }
  
}

object ObjectDataConversions {
  val fuelSensors = Set("fuel_lvl", "Digital_fuel_sensor_B1", "Fuel_level_%", "io_2_67")

  def getConverter(uid: String, sensorName: String, mdbm: MongoDBManager): ObjectDataConversions = {
    if(fuelSensors.contains(sensorName)) {
      new FuelConversions(uid, sensorName, mdbm)
    }
    else {
      new ObjectDataConversions(uid, sensorName, mdbm)
    }
  }

  def isFuelSensor(sensorName: String): Boolean= fuelSensors.contains(sensorName)

  def getFuelData(fdt: String, gd: GPSData, mdbm: MongoDBManager) = {
    fdt match {
      case "can" => {
        if (gd.data.containsKey("fuel_lvl")) {
          val conv = new FuelConversions(gd.uid, "fuel_lvl", mdbm)
          conv.getPointValues(gd)._1
        } else if (gd.data.containsKey("Fuel_level_%")) {
          val conv = new FuelConversions(gd.uid, "Fuel_level_%", mdbm)
          conv.getPointValues(gd)._1
        } else 0.0
      }
      case "fls" => {
        if (gd.data.containsKey("io_2_67")) {
          val conv = new FuelConversions(gd.uid, "io_2_67", mdbm)
          conv.getPointValues(gd)._1
        } else if (gd.data.containsKey("Digital_fuel_sensor_B1")) {
          val conv = new FuelConversions(gd.uid, "Digital_fuel_sensor_B1", mdbm)
          conv.getPointValues(gd)._1
        } else 0.0
      }
      case _ => 0.0
    }
  }

  def getGPSDataSensors(g: GPSData, mdbm: MongoDBManager): Seq[Map[String, Any]] = {
    val sensors = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> g.uid), MongoDBObject("sensors" -> 1))
      .flatMap(_.getAs[MongoDBList]("sensors").map((m: MongoDBList) => m.toSeq.map(_.asInstanceOf[BasicDBObject]))).getOrElse(Seq.empty)

    sensors.filter(_.contains("paramName")).map(
      s => {
        val paramName = s("paramName").toString
        val conv = ObjectDataConversions.getConverter(g.uid, paramName, mdbm)
        //        val conv = if (Set("Digital_fuel_sensor_B1", "fuel_lvl", "io_2_67", "Fuel_level_%").contains(s("paramName").toString))
        //          new FuelConversions(g.uid, s("paramName").toString, mdbm) else new ObjectDataConversions(g.uid, s("paramName").toString, mdbm)
        if (s.contains("showInInfo") && s("showInInfo") == false) {
          Map.empty
        } else {
          Map(
            "name" -> (if (s.contains("name")) s("name") else paramName),
            "paramName" -> paramName,
            "value" -> (
              if (ObjectDataConversions.isFuelSensor(paramName))
                conv.getPointValues(g)._1.round
              else
                conv.getPointValues(g)._1),
            "unit" -> (if (s.contains("unit")) s("unit") else "")
          )
        }
      }
    ).filter(_.isEmpty == false).asInstanceOf[Seq[Map[String, Any]]]
  }
}

class FuelConversions(uid: String, param: String, mdbm: MongoDBManager) extends ObjectDataConversions(uid, param, mdbm) {
  
  val maxFuelValue: Int = {
    val dataTable = sensorSettings.get("dataTable").getOrElse(false)
    
    dataTable match {
      case b: Boolean => {
        val fuelVolume = sensorSettings.get("fuelVolume").getOrElse(255).asInstanceOf[Int]
        if (fuelVolume > 0) fuelVolume else 255
      }
      case l: BasicDBList => {
        l.last.asInstanceOf[BasicDBObject].get("y") match {
          case s: String => s.toInt
          case i: Integer => i
          case e => 100
        }
      }
      case e => {
        100
      }
    }
  }
  
  override val conversionTable = {
    val dataTable = sensorSettings.get("dataTable").getOrElse(false)
    
    dataTable match {
      case b: Boolean => 
        if (param == "Digital_fuel_sensor_B1" || param == "io_2_67") {
          Seq(new conversionRule(Int.MinValue, Int.MaxValue))
        } else {
          val ratio = if (tryInt(sensorSettings.get("fuelVolume").get) > 0) (tryInt(sensorSettings.get("fuelVolume").get) / 255.0) else tryDouble(sensorSettings.get("ratio").get)
          Seq(new conversionRule(0, 256, ratio))
        }
      case l: BasicDBList =>
        val table = l.sliding(2).map(a => {processDataPair(a.asInstanceOf[Seq[BasicDBObject]])}).toSeq
        val last = table.lastOption.getOrElse(new conversionRule(tryInt(sensorSettings.get("minValue").getOrElse(1)), tryInt(sensorSettings.get("maxValue").getOrElse(2048))))
        val first = table.headOption.getOrElse(last)
        (new conversionRule(0, first.min, first.k, first.b)) +:
          table :+
        (new conversionRule(last.max, tryInt(sensorSettings.get("maxValue").getOrElse(2048)), last.k, last.b))
      case e =>
        Seq(new conversionRule(Int.MinValue, Int.MaxValue))
    }
  }
    
  override def getRealValue(i: Double) = {
    val rule = conversionTable.find(_.isInRange(i)).getOrElse(new conversionRule(Int.MinValue, Int.MaxValue))

    rule.calculate(i)
  }
}

class ObjectFuelSettings(uid: String, mdbm: MongoDBManager) {
  
  val settings = mdbm.getDatabase()("objects").findOne(MongoDBObject("uid" -> uid), MongoDBObject("settings" -> 1)).flatMap(_.getAs[BasicDBObject]("settings")).getOrElse(new BasicDBObject())
  
  lazy val fuelVolume = tryInt(settings.getOrElse("repFuelFullVolume", 100))
  lazy val minDraining = tryInt(settings.getOrElse("repFuelMinDraining", 10))
  lazy val minFueling = tryInt(settings.getOrElse("repFuelMinFueling", 10))
  lazy val minTime = tryInt(settings.getOrElse("repFuelMinTime", 60))
  lazy val filtering = tryInt(settings.getOrElse("repFuelFiltering", 0))
  lazy val isNoFiltering = settings.getAsOrElse[Boolean]("repFuelIsNoFiltering", false)
  lazy val idling = tryDouble(settings.getOrElse("repFuelNormIdling", 0))
  lazy val urban = tryDouble(settings.getOrElse("repFuelNormUrban", 0))
  lazy val extraurban = tryDouble(settings.getOrElse("repFuelNormXUrban", 0))
  lazy val loadfactor = tryDouble(settings.getOrElse("repFuelNormKLoad", 0))
  lazy val winternorm = tryDouble(settings.getOrElse("repFuelNormWinter", 0))
  lazy val summernorm = tryDouble(settings.getOrElse("repFuelNormSummer", 0))

  lazy val refuelingCombineTime = tryInt(settings.getAsOrElse[Int]("repFuelRefuelingTimeout", 300))
  lazy val useBasicStandards = settings.getAsOrElse[Boolean]("repFuelUseBasicStandards", false)
  lazy val useCalcStandards =  settings.getAsOrElse[Boolean]("repFuelUseCalcStandards", false)
  lazy val maxInterval = tryInt(settings.getOrElse("repFuelMaxInterval", 300))
  lazy val refuelingStandstill = settings.getAsOrElse[Boolean]("repFuelRefuelingStandstill", false)
  lazy val drainingOnTheRun = settings.getAsOrElse[Boolean]("repFuelDrainingOnTheRun", false)
  lazy val urbanByCoordinates = settings.getAsOrElse[Boolean]("repFuelUrbanByCoordinates", false)
  lazy val maxUrbanSpeed = settings.getAsOrElse[Int]("repFuelMaxUrbanSpeed", 36)
  lazy val ignoreMessagesAfterMoving = settings.getAsOrElse[Int]("repFuelIgnoreMessagesAfterMoving", 60)
  lazy val useRefuelingsInExpectedFuelLevel = settings.getAsOrElse[Boolean]("repFuelUseRefuelingsInExpectedFuelLevel", true)
}

class conversionRule(val min: Int, val max: Int, val k: Double = 1.0, val b: Double = 0.0) {
  
  def calculate(n: Double) = n * k + b
  
  def isInRange(n: Double) = (min <= n.floor.toInt && max > n)
  
  override def toString = "conversionRule(range = (" + min + ", " + max + "), k = " + k + ", b = " + b + ")"
}