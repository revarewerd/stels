package ru.sosgps.wayrecall.utils.web

//import com.mongodb.casbah.Imports._
import ch.ralscha.extdirectspring.bean.{SortInfo, SortDirection, ExtDirectStoreReadRequest}
import ch.ralscha.extdirectspring.filter.StringFilter
import java.util.regex.Pattern
import collection.JavaConversions.{seqAsJavaList, mapAsJavaMap, mapAsScalaMap, collectionAsScalaIterable}
import com.mongodb.casbah.MongoCursor
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.DBObject
import ru.sosgps.wayrecall.utils.containsPattern
import java.util
import scala.collection.mutable

object EDSMongoUtils {

  implicit def edsUtilsOnMongo(cursor: MongoCursor) = new {

    def requestedPart(request: ExtDirectStoreReadRequest): MongoCursor = EDSMongoUtils.requestedPart(cursor, request)

    def sortWithSorters(request: ExtDirectStoreReadRequest): MongoCursor = EDSMongoUtils.sortWithSorters(cursor, request)

    def sortWithSorters(sorters: java.util.List[SortInfo]): MongoCursor = EDSMongoUtils.sortWithSorters(cursor, sorters)

    def applyEDS(request: ExtDirectStoreReadRequest) = EDSMongoUtils.applyEDS(cursor, request)

    def totalCountAndApplyEDS(request: ExtDirectStoreReadRequest): (Int, MongoCursor) = (cursor.count, EDSMongoUtils.applyEDS(cursor, request))

  }

  def applyEDS(allobjects: MongoCursor, request: ExtDirectStoreReadRequest) = requestedPart(sortWithSorters(allobjects, request), request)

  def requestedPart(sorted: MongoCursor, request: ExtDirectStoreReadRequest): MongoCursor = {
    sorted.skip(
      Option(request.getStart()).getOrElse(Integer.valueOf(0)).intValue()
    ).limit(
      Option(request.getLimit()).getOrElse(Integer.valueOf(10000)).intValue()
    )
  }

  def sortWithSorters(allobjects: MongoCursor, request: ExtDirectStoreReadRequest): MongoCursor = {
    val sorters = request.getSorters
    sortWithSorters(allobjects, sorters)
  }


  def sortWithSorters(allobjects: MongoCursor, sorters: java.util.List[SortInfo]): MongoCursor = {
    sorters.foldLeft(allobjects)((allo, sortInfo) => {
      allo.$orderby(MongoDBObject(
        sortInfo.getProperty() -> (sortInfo.getDirection match {
          case SortDirection.ASCENDING => 1
          case SortDirection.DESCENDING => -1
        })))
    })
  }

  def searchFieldFromQuery(request: ExtDirectStoreReadRequest) {
    if (request.getQuery != null) {
      val map = new util.HashMap[String, AnyRef]()
      map.put("searchfield", "name")
      map.put("searchstring", request.getQuery)
      map.putAll(request.getParams)
      request.setParams(map)
    }
  }

  def searchAndFilters(request: ExtDirectStoreReadRequest): DBObject = {
    val elems = searchAndFiltersList(request)
    MongoDBObject(elems.toList)
  }


  def searchAndFiltersList(request: ExtDirectStoreReadRequest) = {
    val filterObject = request.getFilters.collect({
      case s: StringFilter => (s.getField -> (".*" + s.getValue + ".*").r.pattern)
    }).toList

    val searchObject = {
      (for (
        searchfield: String <- request.getParams().toMap.get("searchfield").flatMap(Option(_)).map(_.toString);
        searchstring <- request.getParams().toMap.get("searchstring");
        if (searchstring != "")
      ) yield (searchfield -> containsPattern(searchstring.asInstanceOf[String]))).toList
    }

    val elems = filterObject ++ searchObject
    elems.toMap
  }

  def setDefaultSorterIfEmpty(request: ExtDirectStoreReadRequest, name: String, dir: SortDirection = SortDirection.ASCENDING) = {
    if (request.getSorters().isEmpty() || request.getSorters.exists(_.getProperty == null))
      request.setSorters(mutable.Buffer(new SortInfo(name, dir)))
  }

}
