package ru.sosgps.wayrecall.data

import scala.io.Source
import ru.sosgps.wayrecall.core.GPSData
import scala.collection.mutable


@org.springframework.stereotype.Component("packDataConverter")
class MapProtocolPackConverter extends PackDataConverter with grizzled.slf4j.Logging {

  import collection.JavaConversions.mapAsJavaMap
  import collection.JavaConversions.mapAsScalaMap
  import collection.mutable

  val rupMap = mapProtocol("RuptelaDataIds.txt")
  val telMap = mapProtocol("TeltonikaDataIds.txt")

  val protocolFunctions = Map[String, (String) => String](
    "ruptela" -> (k => rupMap.getOrElse(k.toInt, "I/O_" + k)),
    "teltonika" -> (k => telMap.getOrElse(k.toInt, "param" + k))
  )

  private[this] def findConverter(p: AnyRef): Option[(String) => String] = {
    protocolFunctions.find(kv => p.asInstanceOf[String].toLowerCase.contains(kv._1)).map(_._2)
  }

  private[this] def mapProtocol(s1: String): Map[Int, String] = {
    Source.fromInputStream(this.getClass.getClassLoader
      .getResourceAsStream(s1)).getLines().filterNot(_.isEmpty).map(s => {
      val split = s.split("\t")
      (split(0).toInt, split(1))
    }).toMap
  }

  def convert(data: mutable.Map[String, AnyRef]): mutable.Map[String, AnyRef] = {

    data.get("protocol").flatMap(findConverter) match {
      case Some(f) =>
        val (numKeys, others) = data.partition(_._1.matches("\\d+"))
        new mutable.HashMap() ++ others ++ numKeys.map(kv => (f(kv._1), kv._2))
      case _ => data
    }
  }


}

abstract class PackDataConverter {

  import collection.JavaConversions.mutableMapAsJavaMap
  import collection.JavaConversions.mapAsScalaMap
  import collection.mutable

  def convert(data: mutable.Map[String, AnyRef]): mutable.Map[String, AnyRef]

  def convertData(gps: GPSData): GPSData = {
    //debug("converting " + gps)
    if (gps.data != null)
      gps.data = convert(gps.data)
    //debug("to " + gps)
    gps
  }
}

class DummyPackDataConverter extends PackDataConverter{
  def convert(data: mutable.Map[String, AnyRef]) = data
}

class ProxyedPackDataConverter(cvtpvd: => PackDataConverter) extends PackDataConverter{
  override def convertData(gps: GPSData): GPSData = cvtpvd.convertData(gps)

  override def convert(data: mutable.Map[String, AnyRef]): mutable.Map[String, AnyRef] = cvtpvd.convert(data)
}
