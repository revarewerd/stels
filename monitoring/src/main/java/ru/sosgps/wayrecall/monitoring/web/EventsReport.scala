package ru.sosgps.wayrecall.monitoring.web

import java.text.SimpleDateFormat
import java.util
import java.util.Date

import ch.ralscha.extdirectspring.annotation.{ExtDirectMethod, ExtDirectMethodType}
import ch.ralscha.extdirectspring.bean.ExtDirectStoreReadRequest
import com.mongodb.casbah.commons.MongoDBObject
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{MongoDBManager, Translations}
import ru.sosgps.wayrecall.events._
import ru.sosgps.wayrecall.utils.ExtDirectService
import ru.sosgps.wayrecall.monitoring.web.ReportsTransitionalUtils
import ru.sosgps.wayrecall.security.ObjectsPermissionsChecker
import ru.sosgps.wayrecall.utils.web.extjsstore.EDSStoreServiceDescriptor

import scala.collection.JavaConverters.{asJavaCollectionConverter, mapAsJavaMapConverter}
import scala.collection.JavaConversions.mapAsScalaMap


/**
  * Created by ivan on 01.02.17.
  */

@ExtDirectService
class EventsReport @Autowired()
(val permissions: ObjectsPermissionsChecker,
 val eventsStore: EventsStore,
 val translations: Translations)

  extends EDSStoreServiceDescriptor with grizzled.slf4j.Logging {


  val model = Model("num", "time", "type", "message", "lon", "lat", "additionalData")

  case class ModelInstance(num: Int, time: Long, `type`: String, message: String, additionalData: Map[String, Any])

  val name = "EventsReport"
  val idProperty: String = "num"
  override val lazyLoad: Boolean =  false



  def getUserMessages(uid: String, from: Date, to: Date) = {
    eventsStore.getEventsAfter(OnTarget(PredefinedEvents.userMessage, permissions.username), from.getTime)
      .filter(message => message.subjObject.contains(uid))
      .dropWhile(message => message.time > to.getTime)
      .reverse
  }
  @ExtDirectMethod (value = ExtDirectMethodType.STORE_READ)
  def getData(request: ExtDirectStoreReadRequest) = {

    val (from, to) = ReportsTransitionalUtils.getWorkingDates(request.getParams)
    val selectedUid = ReportsTransitionalUtils.getSelectedId(request.getParams)

    debug("from, to are " + from + ", " + to)

    // TODO нормальный запрос в базу, решить вопрос с сортировкой
    val qr = getUserMessages(selectedUid, from, to)

    debug("qr size is " + qr.size)

    val additionalParams = Set("lat","lon", "geozoneId")
    qr.zipWithIndex.map { case (ev, i) =>
      debug("UserMessage = " + ev)
      //ModelInstance(i, ev.time, ev.fromUser.getOrElse(""), ev.message, ev.additionalData.mkString(","))}
      ModelInstance(
        num = i + 1,
        time = ev.time,
        `type` = ev.`type`,
        message = ev.message,
        additionalData = ev.additionalData.filterKeys(additionalParams)
      )
    }
  }

  def getJRData(selected: String, from: Date, to: Date) = {
    val locale = translations.getRequestFromSpringOrDefault
    import locale.tr

    val dateFormat = new SimpleDateFormat(tr("format.scala.datetime"))

    val qr = getUserMessages(selected, from, to)
    qr.zipWithIndex.map { case (ev, i) =>
      Map[String, AnyRef](
        "num" -> java.lang.Integer.valueOf(i + 1),
        "time" -> dateFormat.format(new Date(ev.time)),
        "type" -> tr(ev.`type`),
        "message" -> ev.message
      ).asJava
    }.asJavaCollection.asInstanceOf[util.Collection[util.Map[String,_]]]
  }

}
