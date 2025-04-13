package ru.sosgps.wayrecall.monitoring.processing.urban

import org.locationtech.jts.geom.{Coordinate, GeometryFactory, Point}
import org.locationtech.jts.index.strtree.STRtree

/**
  * Created by ivan on 16.05.17.
  */
class UrbanSTRtree(val radius: Double, centerLon: Double, centerLat: Double) extends STRtree() {
  val factory = new GeometryFactory()
  val centerPoint = factory.createPoint(new Coordinate(centerLon, centerLat))

  def includes(lon: Double, lat: Double) = {
    centerPoint.distance(factory.createPoint(new Coordinate(lon,lat))) < radius
  }

  def includesArea(lon: Double, lat: Double, searchRadius: Double) = {
    centerPoint.distance(factory.createPoint(new Coordinate(lon,lat))) < radius + searchRadius
  }
}