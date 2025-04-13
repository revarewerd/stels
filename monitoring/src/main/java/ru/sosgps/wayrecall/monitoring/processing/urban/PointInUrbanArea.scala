package ru.sosgps.wayrecall.monitoring.processing.urban

import grizzled.slf4j.Logging
import org.locationtech.jts.geom.Coordinate
import org.postgis.Geometry
import ru.sosgps.wayrecall.regeocoding.DirectNominatimRegeocoder

import scala.collection.JavaConverters._


/**
  * Created by ivan on 16.05.17.
  */


class PointInUrbanArea(val regeo: DirectNominatimRegeocoder) extends  Logging {
  var index: UrbanSTRtree = null

  var indexBuildCount = 0
  def buildIndex(lon: Double, lat: Double) = {

    val radius = 0.28
    val places = regeo.getSearchArea(lon, lat, radius)
    val index = new UrbanSTRtree(radius, lon, lat)
    val geometryFactory = new UrbanGeometryFactory
    places.foreach(obj => {
      val jtsObject = PostgisToJTS.toJtsGeometry(obj, geometryFactory)
      index.insert(jtsObject.getEnvelopeInternal, jtsObject)
    })
    index.build()
    indexBuildCount += 1
    index
  }


  def fitness(obj: org.locationtech.jts.geom.Geometry with UrbanGeometry,
              searchPoint: org.locationtech.jts.geom.Point) = {
    val distance = obj.distance(searchPoint)
    distance
    // Полигоны используются для представления больших объектов, в том числе агломераций.
    // В результате, территории вроде новой Москвы считается большим городом
    // Введём приоритет в некотором радиусе для точек и линий
    //println("candidate is " + obj.prettyString + ", distance  " + distance)

//    val maxForPolygonToLose = 0.02 // approx. 2.22km
//    if(obj.isPolygon)
//      distance + 180 // Штрафуем полигоны
//    else
//    if(distance < maxForPolygonToLose)
//      distance
//    else 180 + distance // Уравниваем шансы
  }

  // Найти ближайший (в соотв. с алгоритмом) геометр. объект к точке
  def search(lon: Double, lat: Double ): Option[UrbanGeometry] = {
    // Строим R-дерево объектов вокруг точки, если она не попадает в предыдущий индекс
    if(index == null || !index.includes(lon,lat))
      index = buildIndex(lon,lat)

    val geometryFactory = new UrbanGeometryFactory
    val searchPoint = geometryFactory.createPoint(new Coordinate(lon,lat))

    val maxSearchRadius = 0.28 // Около 32 км

    var stop = false
    while(!stop) {
      var searchRadius = 0.01

      // На каждом шаге строим квадрат вокруг точки с большей площадью, чем на предыдущем
      // Если окружность радиусом которой явл. расстояние от точки до ребра квадрата, не полностью входит в зону (окружность) поиска
      // то объекты за зоной поиска могут оказаться ближе таковых внутри, поэтому строим индекс заново
      while (searchRadius <= maxSearchRadius && index.includesArea(lon,lat, searchRadius)) {
        //println("searchRadius is " + searchRadius)
        val coordinates = Array(new Coordinate(lon - searchRadius, lat - searchRadius),
          new Coordinate(lon - searchRadius, lat + searchRadius),
          new Coordinate(lon + searchRadius, lat + searchRadius),
          new Coordinate(lon + searchRadius, lat - searchRadius),
          new Coordinate(lon - searchRadius, lat - searchRadius))

        val boundingBox = geometryFactory.createPolygon(coordinates)
        //println("BoundingBox is " + boundingBox)

        // Находим объекты, пересекающие квадрат
        val qr = index.query(boundingBox.getEnvelopeInternal).asScala.map(_.asInstanceOf[org.locationtech.jts.geom.Geometry with UrbanGeometry])

        // из них находим самый близкий (условно, см. fitness)
        val inter = if (qr.nonEmpty)
          Some(qr.minBy(fitness(_,searchPoint)))
        else
          None

        if (inter.nonEmpty)
          return inter

        searchRadius += 0.02
      }

      val needsRefresh = searchRadius <= maxSearchRadius
      if(needsRefresh)
        index = buildIndex(lon, lat)
      else
        stop = true
    }

    None
  }

  def isUrban(lon: Double, lat: Double) = {
    search(lon,lat).exists(o => PointInUrbanArea.urbanPlaceTypes(o.placeType))
  }

}

/*
      type
-------------------
locality
farm
hamlet
islet
island
village
suburb
region
isolated_dwelling
town
city

*/

object PointInUrbanArea {
  val urbanPlaceTypes = Set("city", "town", "suburb")
}
