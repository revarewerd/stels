package ru.sosgps.wayrecall.monitoring.processing.urban
import org.locationtech.jts.geom._
import ru.sosgps.wayrecall.regeocoding.GeoObject

/**
  * Created by ivan on 16.05.17.
  */
trait UrbanGeometry {
  def placeId: Long
  def name: String
  def placeType: String
  def isPolygon = false
  def prettyString = s"id: $placeId name: $name type: $placeType payload: ${this.toString}"
}

class UrbanPoint(override val placeId: Long, override val name: String, override val placeType: String,
                 coordinates: CoordinateSequence, factory: GeometryFactory)
  extends Point(coordinates,factory) with  UrbanGeometry {
}

class UrbanPolygon(override val placeId: Long, override val name: String, override val placeType: String,
                   shell: LinearRing, holes: Array[LinearRing], factory: GeometryFactory)
  extends  Polygon(shell, holes, factory) with  UrbanGeometry {
  override def isPolygon = true
}

class UrbanMultipolygon(override val placeId: Long, override val name: String, override val placeType: String,
                        polygons: Array[Polygon], factory: GeometryFactory)
  extends MultiPolygon(polygons, factory) with  UrbanGeometry {
  override def isPolygon = true
}

class UrbanLineString(override val placeId: Long, override val name: String, override val placeType: String,
                      coordinates: CoordinateSequence, factory: GeometryFactory)
  extends LineString(coordinates, factory) with UrbanGeometry {

}

class UrbanGeometryFactory extends GeometryFactory {
  def createUrbanPoint(placeId: Long, name: String, placeType: String, lon: Double, lat: Double) = {
    val coordsSeq = getCoordinateSequenceFactory.create(Array(new Coordinate(lon,lat)))
    new UrbanPoint(placeId, name, placeType, coordsSeq, this)
  }

  def createUrbanPolygon(placeId: Long, name: String, placeType: String,shell: LinearRing, holes: Array[LinearRing]) = {
    new UrbanPolygon(placeId, name, placeType, shell, holes, this)
  }

  def createUrbanLineString(placeId: Long, name: String, placeType: String, coords: Array[Coordinate]) =
    new UrbanLineString(placeId, name, placeType, getCoordinateSequenceFactory.create(coords), this)

  def createUrbanMultipolygon(placeId: Long, name: String, placeType: String, polygons: Array[Polygon]) =
    new UrbanMultipolygon(placeId, name, placeType, polygons, this)
}

object PostgisToJTS {
  def toJtsGeometry(obj: GeoObject, factory: UrbanGeometryFactory) = {
    val geom = obj.geometry
    val name = obj.name

    def toLinearRing(r: org.postgis.LinearRing) = {
      //println("ring is " + r)
      factory.createLinearRing(r.getPoints.map(p => new Coordinate(p.x, p.y)))
    }

    def toPolygon(gp: org.postgis.Polygon) = {
      factory.createPolygon(toLinearRing(gp.getRing(0)), (1 until gp.numRings).map(i => toLinearRing(gp.getRing(i))).toArray)
    }
    // println("Source " + geom)
    val result = geom match {
      case p: org.postgis.Point => factory.createUrbanPoint(obj.placeId, name, obj.placeType, p.x, p.y)
      case p: org.postgis.Polygon => factory.createUrbanPolygon(obj.placeId, name, obj.placeType,
        toLinearRing(p.getRing(0)), (1 until p.numRings).map(i => toLinearRing(p.getRing(i))).toArray)
      case ls: org.postgis.LineString => factory.createUrbanLineString(obj.placeId, name, obj.placeType,
        ls.getPoints.map(p => new Coordinate(p.x, p.y)))
      case mp: org.postgis.MultiPolygon => factory.createUrbanMultipolygon(obj.placeId, name, obj.placeType,
        mp.getPolygons.map(toPolygon))
    }

    // println(" got " + result)
    result
  }
}