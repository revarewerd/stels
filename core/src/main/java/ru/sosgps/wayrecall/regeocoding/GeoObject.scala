package ru.sosgps.wayrecall.regeocoding

/**
  * Created by ivan on 16.05.17.
  */
case class GeoObject(placeId: Long, placeType: String, name: String, geometry: org.postgis.Geometry)
