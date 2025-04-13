package ru.sosgps.wayrecall.monitoring.web

import java.util
import java.util.{Locale, Date}

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethodType, ExtDirectMethod}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.casbah.Imports._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{MongoDBManager, Translations}
import ru.sosgps.wayrecall.data.PermissionChekedPackageStore
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor
import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.asScalaSet


@ExtDirectService
class TimeZonesStore extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {
  val model = Model(
    "id",
    "offset",
    "name"
  )
  val name = "Timezones"
  //override val storeType: String = "Ext.data.TreeStore"
  override val autoSync = false
  val idProperty = "id"
  val lazyLoad = false

  @Autowired
  var translations: Translations = null

  @Autowired
  var pc: ObjectsPermissionsChecker = null

  @Autowired
  var mdbm: MongoDBManager = null

  @ExtDirectMethod
  def getUserTimezone(): String = {
    mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> pc.username)).flatMap(_.getAs[String]("timezoneId")).orNull
  }

  @ExtDirectMethod(value = ExtDirectMethodType.STORE_READ, streamResponse = true)
  def loadObjects(request: ExtDirectStoreReadRequest) = {

    val now = new DateTime()

    val locale = translations.getRequestFromSpringOrDefault.locale

    val loc = Locale.forLanguageTag(locale)

    debug(s"locale=$locale loc=$loc")

    val timeZones = DateTimeZone.getAvailableIDs.toIndexedSeq
      .map(id => DateTimeZone.forID(id)).sortBy(_.getOffset(now))

    debug("timeZones=" + timeZones.mkString("\n"))

    val dateTimeFormatter = DateTimeFormat.forPattern("ZZ");

    val r = timeZones
      .map(tz => {
      Map(
        "id" -> tz.getID,
        "name" -> (dateTimeFormatter.withZone(tz).print(now) + " " + tz.getID + " (" + tz.getName(now.getMillis, loc)+")"),
        "offset" -> tz.getOffset(now)
      )
    })

    debug("r=" + r)

    r

  }


}

