package ru.sosgps.wayrecall.regeocoding

import java.lang
import java.sql.{Connection, DriverManager, ResultSet}
import java.util.concurrent.{ArrayBlockingQueue, ThreadPoolExecutor, TimeUnit}

import com.google.common.cache.CacheBuilder
import resource._
import javax.sql.DataSource

import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService

import scala.beans.BeanProperty
import javax.annotation.Resource

import kamon.trace.Tracer
import org.postgis.PGgeometry

import scala.collection.{TraversableOnce, immutable}
import ru.sosgps.wayrecall.utils.{PGHStore, funcLoadingCache}

import scala.collection.JavaConversions.mapAsScalaMap
import scala.concurrent.Future


/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 24.08.13
 * Time: 14:57
 * To change this template use File | Settings | File Templates.
 */
class DirectNominatimRegeocoder extends ReGeocoder with grizzled.slf4j.Logging {

  def this(nominatimDatabase: DataSource){
    this()
    this.dataSource = nominatimDatabase
  }

  implicit def toResultSetIterator(rs: ResultSet) = new ResultSetIterator(rs)

  private[this] val sLanguagePrefArraySQL = "ARRAY['short_name:ru','name:ru','place_name:ru','official_name:ru','short_name','name','place_name','official_name','ref','type']"

  //@Autowired
  @Resource(name = "nominatimDatabase")
  var dataSource: DataSource = null

  @BeanProperty
  var enabled = true

  @BeanProperty
  var parallel: Int = 4;

  @BeanProperty
  var regeoQueuelimit = 500

  def getPosition(lon: Double, lat: Double): Future[Either[java.lang.Throwable, String]] =
    getPosition(lon: Double, lat: Double, false, false)

  private lazy val pool = new ScalaExecutorService(
    "regeocodingPool", parallel, (parallel * 1.5).toInt,
    true, 30, TimeUnit.MINUTES,
    new ArrayBlockingQueue[Runnable](regeoQueuelimit), new ThreadPoolExecutor.DiscardOldestPolicy
  )

  private val regeoCache = CacheBuilder.newBuilder()
    .concurrencyLevel(16)
    .expireAfterAccess(30, TimeUnit.SECONDS)
    .maximumSize(50000)
    .buildWithFunction[(Double, Double, Boolean, Boolean), Future[Either[java.lang.Throwable, String]]](arg => {
    Tracer.currentContext.withNewAsyncSegment("loadfromdb", "pg", "wrc")(
      pool.future(loadFromDb _ tupled arg)
    )
  })


  @BeanProperty
  var cachePrecision = 0.0003

  private def round(n: Double): Double = { math.round(n / cachePrecision) * cachePrecision }

  def getPosition(lon: Double, lat: Double,
                  reverseAddr: Boolean, addrOnly: Boolean): Future[Either[java.lang.Throwable, String]] =
    Tracer.withNewContext("regeocode", true)(
      regeoCache(round(lon), round(lat), reverseAddr, addrOnly)
    )

  private def loadFromDb(lon: Double, lat: Double, reverseAddr: Boolean, addrOnly: Boolean): Either[Throwable, String] = {
    scala.util.control.Exception.nonFatalCatch.either {
      managed(dataSource.getConnection).acquireAndGet(conn => regeocode(conn, lon, lat, 18, reverseAddr, addrOnly))
    }
  }


  def setTimeout(timeout: Int) {}

  def toGeoObject(rec: ResultSetWrapper) = {
    GeoObject(rec("place_id").asInstanceOf[Long], rec.as[String]("type"),
      "",
      rec("geometry").asInstanceOf[PGgeometry].getGeometry)
  }

  // Получаем окружность заданного радиуса в виде списка пересекающих её объектов. Мб сделать квадрат?
  def getSearchArea(lon: Double, lat: Double, radius: Double): Vector[GeoObject]= {
    for(conn <- managed(dataSource.getConnection)) {

      conn.unwrap(classOf[org.postgresql.PGConnection])
        .addDataType("hstore", Class.forName("ru.sosgps.wayrecall.utils.PGHStore"))

      val sPointSQL = s"ST_SetSRID(ST_Point($lon,$lat),4326)"
      val query= s"SELECT place_id, type, name, geometry FROM placex" +
        s" WHERE ST_DWithin($sPointSQL, geometry, ?) AND rank_search <= 20 AND" +
        s" rank_search >= 16 AND" +
        s" class ='place';"

      for(stmt <- managed(conn.prepareStatement(query))) {
        stmt.setDouble(1, radius)
        for(rs <- managed(stmt.executeQuery())) {
          return rs.map(toGeoObject).toVector
        }
      }
    }
    throw new RuntimeException("Shit happens")
  }

  private[this] def regeocode(conn: Connection, lon: Double, lat: Double, zoom: Int,
                              reverseAddr: Boolean, addrOnly:Boolean): String = {

    conn.unwrap(classOf[org.postgresql.PGConnection])
      .addDataType("hstore", Class.forName("ru.sosgps.wayrecall.utils.PGHStore"));

    val sPointSQL = s"ST_SetSRID(ST_Point($lon,$lat),4326)"
    var iMaxRank = aZoomRank.getOrElse(zoom, 28)

    var fSearchDiam = 0.0001
    var iPlaceID: java.lang.Long = null
    var iParentPlaceID: java.lang.Long = null
    val aArea = false
    val fMaxAreaDistance = 1

    for (stmt <- managed(conn.prepareStatement(s"SELECT place_id,parent_place_id,rank_search FROM" +
      s" placex WHERE ST_DWithin($sPointSQL, geometry, ?) AND rank_search != 28 AND" +
      s" rank_search >= ? AND (name IS NOT null OR housenumber IS NOT null) AND" +
      s" class NOT IN (\'waterway\',\'railway\',\'tunnel\',\'bridge\') AND " +
      s"(ST_GeometryType(geometry) NOT IN (\'ST_Polygon\',\'ST_MultiPolygon\')  OR" +
      s" ST_DWithin($sPointSQL, ST_Centroid(geometry), ?)) ORDER BY ST_distance($sPointSQL, geometry) ASC LIMIT 1")))
      while (iPlaceID == null && fSearchDiam < fMaxAreaDistance) {
        fSearchDiam = fSearchDiam * 2

        // If we have to expand the search area by a large amount then we need a larger feature
        // then there is a limit to how small the feature should be
        if (fSearchDiam > 2 && iMaxRank > 4) iMaxRank = 4
        if (fSearchDiam > 1 && iMaxRank > 9) iMaxRank = 8
        if (fSearchDiam > 0.8 && iMaxRank > 10) iMaxRank = 10
        if (fSearchDiam > 0.6 && iMaxRank > 12) iMaxRank = 12
        if (fSearchDiam > 0.2 && iMaxRank > 17) iMaxRank = 17
        if (fSearchDiam > 0.1 && iMaxRank > 18) iMaxRank = 18
        if (fSearchDiam > 0.008 && iMaxRank > 22) iMaxRank = 22
        if (fSearchDiam > 0.001 && iMaxRank > 26) iMaxRank = 26

        //val sSQL = s"select place_id,parent_place_id,rank_search from placex WHERE ST_DWithin($sPointSQL, geometry, $fSearchDiam) and rank_search != 28 and rank_search >= $iMaxRank and (name is not null or housenumber is not null) and class not in (\'waterway\',\'railway\',\'tunnel\',\'bridge\') and (ST_GeometryType(geometry) not in (\'ST_Polygon\',\'ST_MultiPolygon\')  OR ST_DWithin($sPointSQL, ST_Centroid(geometry), $fSearchDiam)) ORDER BY ST_distance($sPointSQL, geometry) ASC limit 1"
        //var_dump($sSQL);

        //println("sSQL="+sSQL)
        stmt.setDouble(1, fSearchDiam)
        stmt.setInt(2, iMaxRank)
        stmt.setDouble(3, fSearchDiam)

        //println("stmt="+stmt.toString)

        for (rs <- managed(stmt.executeQuery())) {
          rs.toStream.headOption.foreach(ho => {
            iPlaceID = ho.getAs[java.lang.Long]("place_id").orNull
            iParentPlaceID = ho.getAs[java.lang.Long]("parent_place_id").orNull
          })
        }

      }

    if (iPlaceID != null) {

      val ad = managed(conn.createStatement()).acquireAndGet(stmt =>
      // stmt.executeQuery(s"SELECT * FROM get_addressdata($iPlaceID) WHERE type = 'house_number' OR rank_address <= 26 AND rank_address >= 8 AND type NOT IN ('administrative')  ORDER BY rank_address DESC LIMIT 5")
        stmt.executeQuery(s"SELECT * FROM get_addressdata($iPlaceID) as r WHERE (type = 'house_number' OR rank_address <= 26 AND rank_address >= 8) AND name NOTNULL ORDER BY rank_address DESC LIMIT 5")
          .map(_.toMap).toList)
      //      println(ad.headOption.map(_.getClass).orNull)
      //      println(ad.headOption.map(_.get("name").map(_.getClass)).flatten.orNull)
      //println("ad=" + ad.mkString("[\n", ",\n", "]"))

      def getName(hStore: PGHStore): String = {
        require(hStore != null, "hStore must not be null at " + lon + ", " + lat)
        val hs = hStore.toMap.asInstanceOf[Map[String, AnyRef]]
        hs.get("name:ru").orElse(hs.get("name")).orElse(hs.get("ref")).getOrElse("").toString
      }

      val withAdministrative = ad.filter(map => map("isaddress").asInstanceOf[Boolean])
      val withoutAdministrative = withAdministrative.filter(_("type") != "administrative")
      val addresList0: List[String] = (if (withoutAdministrative.nonEmpty) withoutAdministrative else withAdministrative).map(e => {
        val hStore = e("name").asInstanceOf[PGHStore]
        getName(hStore)
      })

      val addresList = if(reverseAddr) addresList0.reverse else addresList0
      //println("addresList=" + addresList)
      val tn: Map[String, String] = if(addrOnly) Map.empty else
        ad.groupBy(_("type").toString).mapValues(l => getName(l.head("name").asInstanceOf[PGHStore]))
      //println("tn=" + tn.mkString("[\n", ",\n", "]"))
      (addresList ++ List(
        //        tn.get("house_number"),
        tn.get("city")
          orElse tn.get("suburb")
          orElse tn.get("hamlet")
          orElse tn.get("town")
          orElse tn.get("state")
          orElse tn.get("county")
      ).flatten).distinct.filter(!_.matches("\\s*")).mkString(", ")

      //      val sSQL = s"select placex.*, get_address_by_language(place_id, $sLanguagePrefArraySQL) as langaddress, get_name_by_language(name, $sLanguagePrefArraySQL) as placename, get_name_by_language(name, ARRAY['ref']) as ref, st_y(st_centroid(geometry)) as lat, st_x(st_centroid(geometry)) as lon from placex where place_id = $iPlaceID";
      //
      //      managed(conn.createStatement()).acquireAndGet(stmt => stmt.executeQuery(sSQL).map(_.as[String]("langaddress")).toStream.head)

    }
    else
      throw new NoSuchElementException("no regeodata for" + lon + " " + lat)

  }


  private[this] val aZoomRank = Map(
    0 -> 2, // Continent / Sea
    1 -> 2,
    2 -> 2,
    3 -> 4, // Country
    4 -> 4,
    5 -> 8, // State
    6 -> 10, // Region
    7 -> 10,
    8 -> 12, // County
    9 -> 12,
    10 -> 17, // City
    11 -> 17,
    12 -> 18, // Town / Village
    13 -> 18,
    14 -> 22, // Suburb
    15 -> 22,
    16 -> 26, // Street, TODO: major street?
    17 -> 26,
    18 -> 30, // or >, Building
    19 -> 30 // or >, Building
  )


}

class ResultSetIterator(rs: ResultSet) extends Iterator[ResultSetWrapper] {

  private[this] val wrapper = new ResultSetWrapper(rs)

  def hasNext = rs.next()

  def next() = wrapper
}


class ResultSetWrapper(val resultSet: ResultSet) extends Map[String, AnyRef] {
  def get(key: String): Option[AnyRef] = Option(resultSet.getObject(key))

  def iterator = (1 to resultSet.getMetaData.getColumnCount).iterator.map(i => (resultSet.getMetaData.getColumnName(i), resultSet.getObject(i)))

  def getAs[T](key: String): Option[T] = get(key).map(_.asInstanceOf[T])

  def as[T](key: String): T = getAs[T](key).get

  def +[B1 >: AnyRef](kv: (String, B1)): Map[String, B1] = toMap + kv

  def -(key: String): Map[String, AnyRef] = toMap - key

  def toMap: immutable.Map[String, AnyRef] = {
    val b = immutable.Map.newBuilder[String, AnyRef]
    for (x <- this)
      b += x

    b.result()
  }
}

