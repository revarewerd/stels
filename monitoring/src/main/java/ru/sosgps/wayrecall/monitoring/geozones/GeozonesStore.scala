package ru.sosgps.wayrecall.monitoring.geozones

import java.io._
import java.sql.{Connection, ResultSet, Statement}
import javax.annotation
import javax.annotation.PostConstruct
import javax.sql.DataSource

import com.mongodb.casbah.commons.MongoDBObject
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.postgis.{LinearRing, PGgeometry, Point, Polygon}
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection
import org.springframework.beans.factory.annotation.{Autowired, Value}
import resource._
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.initialization.MultiDbManager
import ru.sosgps.wayrecall.utils

import scala.beans.BeanProperty

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 23.09.13
 * Time: 18:08
 * To change this template use File | Settings | File Templates.
 */
class GeozonesStore extends grizzled.slf4j.Logging {

  import JDBCUtils._

  @Autowired
  var pgStore: DataSource = null

  @BeanProperty
  @Value("${instance.name}")
  var instanceName: String = null


  @PostConstruct
  def chekDb(): Unit = try {
    withinConnection(conn => {

      migrateToInstanceAware(conn)

    })
  }
  catch {
    case e: Exception => error("GeozonesStore initialization exception", e)
  }

  @Autowired
  var mdbm: MongoDBManager = null

  @annotation.Resource(name = "multiDbManager")
  var mmdbm: MultiDbManager = null

  private def migrateToInstanceAware(conn: Connection) {
    val columnsNames = conn.getTableColumns("geozones")
    debug("columnsNames = " + columnsNames)
    if (!columnsNames.contains("instance")) conn.withTransaction {

      val dump: Writer = new OutputStreamWriter(new GzipCompressorOutputStream(
        new BufferedOutputStream(new FileOutputStream(utils.io.newFile("geodump.txt.gz")))))
      new CopyManager(conn.unwrap(classOf[BaseConnection])).copyOut("COPY geozones TO STDOUT", dump)
      dump.close()

      val logins = conn.withStatement(_.executeQuery("SELECT userName FROM geozones").iterator.map(_.getString("userName")).toSet)

      val ess = (for (login <- logins.iterator;
                      (inst, dbm) <- mmdbm.dbmanagers.toList;
                      res <- dbm.getDatabase()("users").findOne(MongoDBObject("name" -> login)).map(_.get("name"))
      ) yield (inst, login)).toSeq.groupBy(_._2).mapValues(_.map(_._1).toList)

      debug("ess = " + ess.mkString("\n"))

      conn.withStatement(_.executeUpdate( """ALTER TABLE "geozones" ADD "instance" VARCHAR(100) NULL"""))
      val stmt = conn.prepareStatement( """UPDATE "geozones" set "instance" = ? WHERE username = ?""")

      for ((login, instances) <- ess) {
        stmt.setString(1, instances.head)
        stmt.setString(2, login)
        stmt.addBatch();
      }

      stmt.executeBatch();
      stmt.close()
    }
  }

  def addGeozone(user: String, name: String, color: String, points: Iterable[(Double, Double)]) = {
    withinConnection(conn => {
      val s1 = conn.prepareStatement("INSERT INTO geozones (userName, instance, name, color, points) VALUES (?, ?, ?, ?, ?)");

      s1.setString(1, user);
      s1.setString(2, instanceName);
      s1.setString(3, name);
      s1.setString(4, color);

      val ring = new LinearRing(points.map(p => new Point(p._1, p._2)).toArray)
      s1.setObject(5, new org.postgis.PGgeometry(new Polygon(Array(ring))))

      warn("addGeozone="+s1)
      s1.executeUpdate();
    })
  }

  def editGeozone(id: Int, user: String, name: String, color: String, points: Iterable[(Double, Double)]) = {
    withinConnection(conn => {
      val s1 = conn.prepareStatement("UPDATE geozones SET userName = ?, instance = ?, name = ?, color = ?, points = ? WHERE id = ?");

      s1.setString(1, user);
      s1.setString(2, instanceName);
      s1.setString(3, name);
      s1.setString(4, color);

      val ring = new LinearRing(points.map(p => new Point(p._1, p._2)).toArray)
      s1.setObject(5, new org.postgis.PGgeometry(new Polygon(Array(ring))))
      s1.setInt(6, id);

      s1.executeUpdate();
    })
  }

  def deleteGeozone(userName: String, geozoneId: Int) = withinConnection(conn => {
    val s1 = conn.prepareStatement("DELETE FROM geozones where userName = ? and instance = ? and id = ?");
    s1.setString(1, userName);
    s1.setString(2, instanceName);
    s1.setInt(3, geozoneId);
    s1.executeUpdate();
  })

  private def withinConnection[T](f: Connection => T) = {
    pgStore.withGeometryConnection(f)
  }



  def getUsersGeozones(userName: String): Seq[Geozone] =
    withinConnection(conn => {
      val s = conn.prepareStatement("SELECT id, userName, name, color, points FROM geozones WHERE userName = ? ORDER BY id");
      s.setString(1, userName)
      val resultSet = s.executeQuery();
      toGeozones(resultSet)
    })

  def getById(id: Int): Geozone =
    withinConnection(conn => {
      val s = conn.prepareStatement("SELECT id, userName, name, color, points FROM geozones WHERE id = ? and instance = ?");
      s.setInt(1, id)
      s.setString(2, instanceName)
      val resultSet = s.executeQuery();
      toGeozones(resultSet)
    }).head

  def getByIdAndUser(id: Int, userName: String): Geozone =
    withinConnection(conn => {
      val s = conn.prepareStatement("SELECT id, userName, name, color, points FROM geozones WHERE id = ? and instance = ? and userName = ?");
      s.setInt(1, id)
      s.setString(2, instanceName)
      s.setString(3, userName)
      val resultSet = s.executeQuery();
      toGeozones(resultSet)
    }).head

  def getUsersGeozonesWithPoint(userName: String, lon: Double, lat: Double): Seq[Geozone] = {
    if (lon.isNaN || lat.isNaN)
      return List.empty

    val startTime = System.currentTimeMillis()
    val geozones = withinConnection(conn => {
      val s = conn.prepareStatement("SELECT id, userName, name, color, points FROM geozones WHERE userName = ? and instance = ? and ST_Covers(points, ?)");
      s.setString(1, userName)
      s.setString(2, instanceName)
      val ggeometry = new org.postgis.PGgeometry(new Point(lon, lat))
      ggeometry.setType("geography")
      s.setObject(3, ggeometry)
      val resultSet = s.executeQuery();
      toGeozones(resultSet)
    })
    val elapsed = System.currentTimeMillis() - startTime
    if (elapsed > 100)
      debug(geozones.size + " geozones were loaded within " + elapsed + " ms")
    geozones
  }

  def getUsersGeozonesWithPoint(ids: Array[Int], lon: Double, lat: Double): Seq[Geozone] = {
    if (lon.isNaN || lat.isNaN)
      return List.empty

    val startTime = System.currentTimeMillis()
    val geozones = withinConnection(conn => {
      val s = conn.prepareStatement("SELECT id, userName, name, color, points FROM geozones WHERE id = ANY(?) and instance = ? and ST_Covers(points, ?)");
      s.setArray(1, conn.createArrayOf("integer", ids.map(_.asInstanceOf[Integer])))
      s.setString(2, instanceName)
      val ggeometry = new org.postgis.PGgeometry(new Point(lon, lat))
      ggeometry.setType("geography")
      s.setObject(3, ggeometry)
      val resultSet = s.executeQuery();
      toGeozones(resultSet)
    })
    val elapsed = System.currentTimeMillis() - startTime
    if (elapsed > 100)
      debug(geozones.size + " geozones were loaded within " + elapsed + " ms")
    geozones
  }


  private[this] def toGeozones(resultSet: ResultSet): IndexedSeq[Geozone] = {
    val result = IndexedSeq.newBuilder[Geozone]
    while (resultSet.next()) {
      val value = resultSet.getObject("points").asInstanceOf[PGgeometry].getGeometry.asInstanceOf[Polygon].getRing(0)
      result += new Geozone(
        resultSet.getInt("id"),
        resultSet.getString("userName"),
        resultSet.getString("name"),
        resultSet.getString("color"),
        value.getPoints.map(p => (p.getX, p.getY))
      )
    }
    result.result()
  }
}

class Geozone(val id: Int, val user: String, val name: String, val color: String, val points: Seq[(Double, Double)]) {


  override def toString = "Geozone(" + name + "," + points.mkString("[", ",", "]") + ")"
}

object JDBCUtils {

  implicit class dswrapper(ds: DataSource) {
    def withGeometryConnection[T](f: Connection => T) = {
      managed(ds.getConnection).acquireAndGet(conn => {
        conn.unwrap(classOf[org.postgresql.PGConnection])
          .addDataType("geography", Class.forName("org.postgis.PGgeometry"));
        f(conn)
      })
    }
  }

  implicit class connectionwrapper(conn: Connection) {

    def getTableColumns(table: String): Set[String] = {
      val metadata = conn.getMetaData
      val columns = metadata.getColumns(conn.getCatalog, null, table, null)
      val rsm = columns.getMetaData
      Iterator.continually().takeWhile(_ => columns.next()).map(_ =>
        //         (1 to rsm.getColumnCount).map(i => columns.getObject(i))
        columns.getString(4)
      ).toSet
    }

    def withTransaction[T](f: => T) = {
      val prevAc = conn.getAutoCommit
      conn.setAutoCommit(false);
      try {
         f
         conn.commit()
      } catch {
        case e: Throwable => conn.rollback()
      } finally {
        conn.setAutoCommit(prevAc)
      }
    }

    def withStatement[T](f: Statement => T) = {
      val statement = conn.createStatement()
      try {
        f(statement)
      } finally {
        statement.close()
      }
    }

  }

  implicit class RSUtils(resultSet: ResultSet) {
    def iterator = Iterator.continually().takeWhile(_ => resultSet.next()).map(_ => resultSet)

  }


}

