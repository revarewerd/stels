package ru.sosgps.wayrecall.monitoring.notifications

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Result
import org.eclipse.jetty.client.util.{BufferingResponseListener, StringContentProvider}
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.{MongoDBManager, Translations}
import ru.sosgps.wayrecall.data.UserMessage
import ru.sosgps.wayrecall.events.{EventsStore, LocalEventsStore, PredefinedEvents}
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService.implicitSameThreadContext
import ru.sosgps.wayrecall.utils.web.ScalaCollectionJson

import javax.annotation.{PostConstruct, PreDestroy}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, Promise}


class OneSignalPusher(private val id: String, private val restKey: String) extends grizzled.slf4j.Logging {

  debug(s"OneSignalPusher($id, $restKey)")

  @Autowired
  var es: EventsStore = null

  @Autowired
  var translations: Translations = _

  @Autowired
  var playerIdStore: OneSignalPlayerIdStorage = _

  val httpClient: HttpClient = new HttpClient(new SslContextFactory(true))

  def onNewUserMessage(um: UserMessage): Unit = {
    playerIdStore.getPlayerIdsAsync(um.targetId).map(sendNotification(um, _))
  }

  @PostConstruct
  def init(): Unit = {
    if (id == "null" || restKey == "null") {
      warn("OneSignalPusher is not configured", new Exception())
      return
    }
    httpClient.start()
    es.subscribe(PredefinedEvents.userMessage, onNewUserMessage)
    debug("HttpClient started")
  }

  @PreDestroy
  def destroy(): Unit = {
    httpClient.stop()
    debug("HttpClient stoped")
  }

  def sendNotification(um: UserMessage, playerIds: Seq[String]): Future[String] = {
    if (playerIds.isEmpty) return Future.successful("nothing to do")
    val languages = translations.tr.keys
    val jsonValue = ScalaCollectionJson.writeValueAsString(
      Map(
        "app_id" -> id,
        "contents" -> languages.map((_, um.message)).toMap,
        "headings" -> languages.map(lang => (lang, translations.translate(lang, um.`type`))).toMap,
        "include_player_ids" -> playerIds
      )
    )
    debug(s"jsonValue = $jsonValue")
    val request = httpClient.POST("https://onesignal.com/api/v1/notifications")
      .header("Authorization", s"Basic $restKey")
      .content(new StringContentProvider(jsonValue), "application/json")
    debug(s"request = ${request}")
    val promise = Promise[String]()
    request.send(new BufferingResponseListener() {
      override def onComplete(result: Result): Unit = {
        debug(s"response = ${result.getResponse}")
        val content = getContentAsString
        debug(s"response.getContentAsString = $content")
        promise.success(content)
      }
    })
    promise.future
  }

}

class OneSignalPlayerIdStorage extends grizzled.slf4j.Logging {

  @Autowired
  var mdbm: MongoDBManager = null

  val playerIdDbName: String = "usersPlayerIds"

  private def collection = mdbm.getDatabase()(playerIdDbName)

  @PostConstruct
  def init() {
    collection.createIndex(MongoDBObject("userId" -> 1))
    collection.createIndex(MongoDBObject("playerId" -> 1), MongoDBObject("unique" -> true))
  }


  def assocUserId(userName: String, playerId: String): Unit = {
    val userObjectId = getUserIdByName(userName).getOrElse(return)
    collection.findOne(MongoDBObject("playerId" -> playerId)) match {
      case Some(rec) =>
        if (rec.as[ObjectId]("userId") != userObjectId)
          collection.remove(MongoDBObject("_id" -> rec.as[ObjectId]("_id")))
        else
          return
      case None =>
    }
    collection.insert(MongoDBObject("userId" -> userObjectId, "playerId" -> playerId))
  }

  private def getUserIdByName(userName: String): Option[ObjectId] = {
    mdbm.getDatabase()("users").findOne(MongoDBObject("name" -> userName), MongoDBObject("_id" -> 1))
      .map(_.as[ObjectId]("_id"))
  }

  def getPlayerIdsAsync(userName: String): Future[Seq[String]] = {
    mdbm.future {
      getUserIdByName(userName).map(userObjectId =>
        collection.find(MongoDBObject("userId" -> userObjectId)).map(_.as[String]("playerId")).toIndexedSeq
      ).getOrElse(Seq.empty)
    }
  }


}

object OneSignalPusher {
  def main(args: Array[String]): Unit = {
    val pusher = new OneSignalPusher(args(0), args(1))
    try {
      pusher.translations = new Translations
      pusher.es = new LocalEventsStore()
      pusher.init()
      Await.result(
        pusher.sendNotification(
          new UserMessage("target0", "Тестовое сообщение", "rules.overspeeding", None), Seq("test-key")),
        10.seconds
      )
    } finally {
      pusher.destroy()
    }
  }
}
