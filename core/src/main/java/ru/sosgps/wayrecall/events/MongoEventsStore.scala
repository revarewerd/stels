package ru.sosgps.wayrecall.events

import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.utils.io.{deserialize, serialize, tryDeserialize}
import javax.annotation.PostConstruct

import com.mongodb
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons
import kamon.Kamon
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.utils.io.{serialize, tryDeserialize}

import javax.annotation.PostConstruct
import scala.beans.BeanProperty
import scala.concurrent._

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 20.06.13
 * Time: 18:37
 * To change this template use File | Settings | File Templates.
 */
class MongoEventsStore extends EventsStore with grizzled.slf4j.Logging {


  @Autowired
  var mdbm: MongoDBManager = null

  @PostConstruct
  def init() {
    eventsCollection.ensureIndex(MongoDBObject(
      "time" -> -1,
      "targetType" -> 1,
      "targetId" -> 1,
      "eventType" -> 1
    ))
  }

  @BeanProperty
  var localEventsStore = new LocalEventsStore

  private[this] def eventsCollection = mdbm.getDatabase()("events")

  def publish(e: Event) {
    localEventsStore.publish(e)
    if (e.persistent) blocking {
      eventsCollection.insert(MongoDBObject(
        "eventType" -> e.eventType,
        "targetType" -> e.targetType,
        "targetId" -> e.targetId,
        "time" -> e.time,
        "additionalData" -> e.additionalData,
        "event" -> serialize(e)
      ))
    }
  }

  def subscribe[T <: Event](eventTarget: EventQuery[T], listener: (T) => Any) {
    localEventsStore.subscribe(eventTarget: EventQuery[T], listener: (T) => Any)
  }

  def subscribeTyped[T <: Event : Manifest](eventTarget: EventQuery[T], listener: (T) => Any) =
    localEventsStore.subscribeTyped(eventTarget: EventQuery[T], listener: (T) => Any)

  def unsubscribe[T <: Event](eventTarget: EventQuery[T], listener: (T) => Any)
  = localEventsStore.unsubscribe(eventTarget: EventQuery[T], listener: (T) => Any)

  def unsubscribeTyped[T <: Event](eventTarget: EventQuery[T], listener: (T) => Any) =
    localEventsStore.unsubscribeTyped(eventTarget: EventQuery[T], listener: (T) => Any)

  def getEventsFiltered[T <: Event](filterParams: Map[String,Any]) = getEventsFiltered[T](NoCriteria[T], filterParams)

  def getEventsFiltered[T <: Event](e: EventQuery[T], filterParams: Map[String,Any]) = {
      val critertia: commons.MongoDBObject = (filterParams.map(tp=>("additionalData."+tp._1,tp._2)).asDBObject ++ (e match {
        case NoCriteria() => MongoDBObject.empty
        case EventTopic(targetType, eventType) =>
          MongoDBObject("eventType" -> eventType, "targetType" -> targetType)
        case OnTarget(target, id) =>
          MongoDBObject("eventType" -> target.eventType, "targetType" -> target.targetType, "targetId" -> id)
        case OnTargets(targets, eventTarget) =>
          MongoDBObject("eventType" -> eventTarget.eventType, "targetType" -> eventTarget.targetType) ++ ("targetId" $in targets)
      }
        )).filter({case (k, v) => (!v.isInstanceOf[String] || v.asInstanceOf[String].nonEmpty)})

      debug("critertia"+critertia.toString())

      val cursor = eventsCollection.find(critertia)
      cursor.underlying.setReadPreference(mongodb.ReadPreference.secondaryPreferred())
      debug("cursor count"+cursor.count())

    val events = cursor.sort(MongoDBObject("time" -> -1))
      .flatMap(dbo => tryDeserialize[T](dbo.as[Array[Byte]]("event")) match {
        case Right(d) => Some(d)
        case Left(t) => warn("deserializationError:", t); None}
      ).toStream
      val latestMongoMsg = events.headOption.map(_.time).getOrElse(1L)
     new QueryResult[T](events,latestMongoMsg)
  }

  def getEventsAfter[T <: Event](time: Long) = getEventsAfter[T](NoCriteria[T], time)

  def getEventsAfter[T <: Event](e: EventQuery[T], time: Long) = {

    val eldestStored = localEventsStore.eldestStored
    if (time >= eldestStored) {
      Kamon.metrics.counter("getEventsAfter", Map("branch" -> "local")).increment()
      localEventsStore.getEventsAfter(e, time)
    }
    else {
      //debug(s"getEventsAfter from database because time=$time eldestStored=$eldestStored")
      Kamon.metrics.counter("getEventsAfter", Map("branch" -> "db")).increment()
      val criteria: commons.MongoDBObject = (("time" $gte time) ++ (e match {
        case NoCriteria() => MongoDBObject.empty
        case EventTopic(targetType, eventType) =>
          MongoDBObject("eventType" -> eventType, "targetType" -> targetType)
        case OnTarget(target, id) =>
          MongoDBObject("eventType" -> target.eventType, "targetType" -> target.targetType, "targetId" -> id)
        case OnTargets(targets, eventTarget) =>
          MongoDBObject("eventType" -> eventTarget.eventType, "targetType" -> eventTarget.targetType) ++ ("targetId" $in targets)
      })).filter({case (k, v) => (!v.isInstanceOf[String] || v.asInstanceOf[String].nonEmpty)})

      val cursor = eventsCollection.find(criteria)
      cursor.underlying.setReadPreference(mongodb.ReadPreference.secondaryPreferred())
      val events = cursor.sort(MongoDBObject("time" -> -1))
        .flatMap(dbo => tryDeserialize[T](dbo.as[Array[Byte]]("event")) match {
        case Right(d) => Some(d)
        case Left(t) => warn("deserializationError:", t); None
      }).toStream

      val latestMongoMsg = events.headOption.map(_.time).getOrElse(time)
      val localEvents = localEventsStore.getEventsAfter(e, latestMongoMsg + 1)
      new QueryResult[T](events ++ localEvents.filter(_.time > latestMongoMsg), math.max(latestMongoMsg, localEvents.lastAddedEventTime))
    }

  }
}
