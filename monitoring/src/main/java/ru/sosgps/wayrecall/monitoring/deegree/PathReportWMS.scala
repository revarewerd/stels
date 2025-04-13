package ru.sosgps.wayrecall.monitoring.deegree


import org.deegree.commons.config.DeegreeWorkspace
import org.deegree.commons.config.ResourceManager
import org.deegree.commons.tom.gml.GMLObject
import org.deegree.commons.utils.CloseableIterator
import org.deegree.cs.coordinatesystems.ICRS
import org.deegree.cs.exceptions.UnknownCRSException
import org.deegree.feature.Feature
import org.deegree.feature.persistence.{FeatureStore, FeatureStoreProvider, FeatureStoreTransaction}
import org.deegree.feature.persistence.lock.LockManager
import org.deegree.feature.persistence.query.Query
import org.deegree.feature.property.{GenericProperty, SimpleProperty}
import org.deegree.feature.stream.FeatureInputStream
import org.deegree.feature.stream.IteratorFeatureInputStream
import org.deegree.feature.types.AppSchema
import org.deegree.feature.types.FeatureType
import org.deegree.feature.types.GenericAppSchema
import org.deegree.feature.types.GenericFeatureType
import org.deegree.feature.types.property._
import org.deegree.geometry.{Envelope, Geometry, GeometryTransformer}
import org.deegree.geometry.precision.PrecisionModel
import org.deegree.geometry.primitive.{LineString, Point}
import org.deegree.geometry.standard.points.PointsList
import org.deegree.geometry.standard.primitive.DefaultLineString
import org.deegree.geometry.standard.primitive.DefaultPoint
import org.slf4j.Logger
import javax.xml.namespace.QName
import java.net.URL
import java.util

import org.slf4j.LoggerFactory.getLogger
import org.springframework.web.context.support.WebApplicationContextUtils
import org.springframework.web.context.request.{RequestContextHolder, ServletRequestAttributes}
import javax.servlet.http.HttpServletRequest

import org.springframework.web.context.WebApplicationContext
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired

import scala.collection.JavaConverters.asJavaIteratorConverter
import scala.collection.JavaConverters.seqAsJavaListConverter
import util.Date

import org.deegree.filter.OperatorFilter
import org.deegree.filter.spatial.BBOX

import collection.mutable.ArrayBuffer
import org.deegree.geometry.standard.{AbstractDefaultGeometry, DefaultEnvelope}
import com.vividsolutions.jts.geom
import ru.sosgps.wayrecall.utils.web.{ScalaJson, springCurrentRequest, springWebContext}
import ru.sosgps.wayrecall.utils
import ru.sosgps.wayrecall.monitoring.web.ReportsTransitionalUtils
import org.deegree.commons.tom.gml.property.{Property, PropertyType}
import java.text.DateFormat

import ru.sosgps.wayrecall.billing.tariff.TariffEDS
import ru.sosgps.wayrecall.data.PackagesStore

import scala.collection.mutable

/**
  * Created with IntelliJ IDEA.
  * User: nickl
  * Date: 29.10.12
  * Time: 19:39
  * To change this template use File | Settings | File Templates.
  */


class PathReportWMS extends FeatureStoreProvider {

  def init(workspace: DeegreeWorkspace) {

  }

  val proxy = new FeatureStoreProxy() {
    override def getSource: FeatureStore = springWebContext.getBean(classOf[PathReportWMSStore])
  };

  def create(configUrl: URL): FeatureStore = {
    return proxy;
  }

  @SuppressWarnings(Array("unchecked")) def getDependencies: Array[Class[_ <: ResourceManager]] = {
    return Array.empty[Class[_ <: ResourceManager]];
  }

  def getConfigNamespace: String = {
    return "http://www.sosgps.ru/datasource/feature/pathreport"
  }

  def getConfigSchema: URL = {
    throw new UnsupportedOperationException("getConfigSchema")
  }
}


@Component
class PathReportWMSStore extends FeatureStore with grizzled.slf4j.Logging {

  private final val ftLocalName: String = "Feature"
  private final val ftNamespace: String = "http://www.deegree.org/app"
  private final val ftPrefix: String = "app"
  private var featureType: GenericFeatureType = null
  private val crs: ICRS = org.deegree.cs.persistence.CRSManager.lookup("EPSG:4326")
  private var schema: AppSchema = null
  private val transformer = new GeometryTransformer(this.crs)

  private val pm: PrecisionModel = new PrecisionModel
  private val gf = new geom.GeometryFactory()

  @Autowired
  var packStore: PackagesStore = null

  @Autowired
  var tariffEDS: TariffEDS = null

  init();

  private[this] def init() {
    val propDecls: util.ArrayList[PropertyType] = new util.ArrayList[PropertyType]
    propDecls.add(new GeometryPropertyType(new QName(ftNamespace, ftLocalName, ftPrefix), 0, 1, null, new util.ArrayList[PropertyType], GeometryPropertyType.GeometryType.LINE_STRING, GeometryPropertyType.CoordinateDimension.DIM_2_OR_3, ValueRepresentation.BOTH))
    featureType = new GenericFeatureType(new QName(ftNamespace, ftLocalName, ftPrefix), propDecls, false)
    schema = new GenericAppSchema(Array[FeatureType](featureType), null, null, null, null, null)
  }

  def isAvailable: Boolean = {
    throw new UnsupportedOperationException("PathReportWMSStore.isAvailable")
  }

  def getSchema: AppSchema = {
    trace("getSchema() " + schema)
    return schema
  }

  def getEnvelope(ftName: QName): Envelope = {
    throw new UnsupportedOperationException("PathReportWMSStore.getEnvelope")
  }

  def calcEnvelope(ftName: QName): Envelope = {
    throw new UnsupportedOperationException("PathReportWMSStore.calcEnvelope")
  }

  def query(query: Query): FeatureInputStream = {

    //trace("query.getFilter="+query.getFilter)

    val bbox: Envelope = transformer.transform(query.getPrefilterBBox.getBoundingBox)

    trace("bbox=" + bbox)

    val map = ScalaJson.parse[Map[String, AnyRef]](springCurrentRequest.getParameter("reportData".toUpperCase))

    trace("query map=" + map)

    val id: String = map("selected").toString // ReportsTransitionalUtils.getSelectedId(map)
    trace("id=" + id)

    val (from, to) = tariffEDS.correctReportWorkingDates(utils.parseDate(map("from")), utils.parseDate(map("to")))
    trace("from=" + from + " to=" + to)

    val iterator: Iterator[Feature] = genPoints(id, from, to, bbox).map(newDefaulFeature)

    val featureIter: CloseableIterator[Feature] = new IteratorToCloseable[Feature](iterator.asJava)
    val query1: FeatureInputStream = new IteratorFeatureInputStream(featureIter)
    trace("query(" + query + ") " + query1)
    return query1
  }


  private[this] def newDefaulFeature(geom: Geometry): Feature = {
    val props: util.LinkedList[Property] = new util.LinkedList[Property]
    props.add(new GenericProperty(featureType.getDefaultGeometryPropertyDeclaration, geom))
    featureType.newFeature((123) + "", props, null)
  }

  private[this] def genPoints(uid: String, from: Date, to: Date, bbox: Envelope): Iterator[Geometry] = {


    val it = packStore.getHistoryFor(uid, from, to)

    val isMainTerminal =
      it.additionalData.getOrElseUpdate("isMainTerminal", new mutable.WeakHashMap[Point, Boolean]().withDefaultValue(true))
        .asInstanceOf[mutable.Map[Point, Boolean]]

    trace(Thread.currentThread().getName + " it=" + System.identityHashCode(it) + " it.len=" + it.total)

    val filtred = it.additionalData.getOrElseUpdate("DefaultPointCache", {
      trace("creating DefaultPointCache")
      it.filter(gps => gps.goodlonlat).zipWithIndex.map(
        {
          case (gpsdata, i) => val point = new DefaultPoint("p" + i, crs, pm, Array[Double](gpsdata.lon, gpsdata.lat))
            val mainTerminal = !java.lang.Boolean.FALSE.equals(gpsdata.data.get("mainTerminal"))
            isMainTerminal(point) = mainTerminal
            point
        })
    }).asInstanceOf[Iterable[DefaultPoint]].iterator

    val inbox: Iterator[Point] = filtred ++ Iterator(null)

    val bbb = bbox.asInstanceOf[DefaultEnvelope].getJTSGeometry.getBoundary

    def getIntersection(prevpoint: Point, curpoint: Point): Point = {
      val intersection = bbb.intersection(gf.createLineString((Array(prevpoint, curpoint).map(p => new geom.Coordinate(p.get0(), p.get1()))))).asInstanceOf[geom.Point]
      newDefaultPoint(Array(intersection.getX, intersection.getY))
    }

    inbox.flatMap({
      var prevpoint: Point = null;
      var accomulated = new ArrayBuffer[Point](302)

      var i: Int = 0;

      curpoint: Point => {
        //debug("isMainTerminal(curpoint)="+isMainTerminal(curpoint))
        if (curpoint != null && !isMainTerminal(curpoint)) {
          if (!bbox.contains(curpoint))
            Iterator.empty
          else
            Iterator(curpoint)
        } else if (curpoint == null || !bbox.contains(curpoint)) {
          if (!accomulated.isEmpty) {
            trace("accomulated" + i + ".len:" + accomulated.length)
            if (curpoint != null && prevpoint != null) {
              val intersection = getIntersection(prevpoint, curpoint)
              //trace("0 prevpoint=" + prevpoint + "curpoint=" + curpoint + "\n + intersection1 =" + intersection)
              accomulated.append(intersection)
            }
            i = i + 1
            val curgeom = newDefaultLineString(accomulated)
            accomulated = new ArrayBuffer[Point]()
            prevpoint = curpoint
            Iterator(curgeom);
          }
          else {
            prevpoint = curpoint
            Iterator.empty
          }
        }
        else if (accomulated.length > 300) {
          accomulated.append(curpoint)
          trace("accomulated2 " + i + ".len:" + accomulated.length)
          i = i + 1
          val curgeom = newDefaultLineString(accomulated)
          accomulated = new ArrayBuffer[Point](302)
          accomulated.append(curpoint)
          prevpoint = curpoint
          Iterator(curgeom);
        }
        else {
          if (accomulated.isEmpty && prevpoint != null) {
            val intersection = getIntersection(prevpoint, curpoint)
            //trace("1 prevpoint=" + prevpoint + " curpoint=" + curpoint + "\n intersection=" + intersection)
            accomulated.append(intersection)
          }
          accomulated.append(curpoint)
          prevpoint = curpoint
          Iterator.empty;
        }
      }
    })

  }

  //private[this] def newDefaultLineString(points: Point*): DefaultLineString = newDefaultLineString(points)

  private[this] def newDefaultLineString(points: Seq[Point]): DefaultLineString = {
    new DefaultLineString("ls", crs, pm, new PointsList(points.asJava))
  }

  private[this] def newDefaultPoint(coordinates: Array[Double]): DefaultPoint = {
    new DefaultPoint("p", crs, pm, coordinates);
  }

  def query(queries: Array[Query]): FeatureInputStream = {
    if (queries.length == 1) return query(queries(0))
    else throw new UnsupportedOperationException("PathReportWMSStore.query with queries.length = " + queries.length)
  }

  def queryHits(query: Query): Int = {
    throw new UnsupportedOperationException("PathReportWMSStore.queryHits")
  }

  def queryHits(queries: Array[Query]): Array[Int] = {
    throw new UnsupportedOperationException("PathReportWMSStore.queryHits")
  }

  def getObjectById(id: String): GMLObject = {
    throw new UnsupportedOperationException("PathReportWMSStore.queryHits")
  }

  def acquireTransaction: FeatureStoreTransaction = {
    throw new UnsupportedOperationException("PathReportWMSStore.queryHits")
  }

  def getLockManager: LockManager = {
    throw new UnsupportedOperationException("PathReportWMSStore.queryHits")
  }

  def init(workspace: DeegreeWorkspace) {
    trace("init()")
  }

  def destroy {
    trace("destroy()")
  }

  def isMapped(p1: QName) = false
}

class FakeSeq[T](iter: Iterator[T], l: Int) extends Seq[T] {
  def length = l

  def apply(idx: Int) = throw new UnsupportedOperationException("FakeSeq index")

  def iterator = iter
}




