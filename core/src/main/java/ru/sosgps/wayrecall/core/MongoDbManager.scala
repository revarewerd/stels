package ru.sosgps.wayrecall.core

import com.mongodb.{MongoOptions, ServerAddress}
import org.springframework.stereotype.Component

import com.mongodb.casbah.{MongoClient, MongoClientOptions, MongoConnection}
import ru.sosgps.wayrecall.utils.concurrent.ScalaExecutorService
import java.util.concurrent.{LinkedBlockingQueue, ArrayBlockingQueue, TimeUnit}
import scala.beans.BeanProperty
import scala.concurrent._

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 15.08.12
 * Time: 15:03
 * To change this template use File | Settings | File Templates.
 */

class MongoDBManager extends grizzled.slf4j.Logging {

  @BeanProperty
  var databaseName = "Seniel"

  @BeanProperty
  var servers = List("127.0.0.1")

  @BeanProperty
  var maxWaitTime = 1000 * 60 * 2

  @BeanProperty
  var connectionsPerHost = 32

  private var _instance: Option[InstanceConfig] = None

  def instance = _instance

  def this(dbconf: DbConf) {
    this()
    databaseName = dbconf.name
    servers = dbconf.servers
    maxWaitTime = dbconf.waitTime
    connectionsPerHost = dbconf.connectionsPerHost
  }

  def this(instance: InstanceConfig) = {
    this(instance.dbconf);
    this._instance = Some(instance)
  }

  private lazy val mongoConnection = {
    val client = MongoClient(
      servers.map(url => {
        val params = url.split(":").zipWithIndex.map(_.swap).toMap
        new ServerAddress(params(0), params.getOrElse(1, "27017").toInt)
      })
      ,
      MongoClientOptions(maxWaitTime = maxWaitTime, connectionsPerHost = connectionsPerHost)
    )

    def tryConnect(): Unit = try {
      info(s"trying connect to: ${client.allAddress.mkString(", ")}")
      client.getDatabaseNames()
    } catch {
      case e: Exception =>
        warn(s"connection failed to ${client.allAddress.mkString(", ")}", e)
        tryConnect()
    }

    tryConnect()
    client
  }

  def getClient = mongoConnection.underlying

  private lazy val dbQueryExecutors = new ScalaExecutorService("dbQueryExecutors", 8, 8, true, 5, TimeUnit.MINUTES, new LinkedBlockingQueue[Runnable](30000))

  def future[T](f: => T): Future[T] = dbQueryExecutors.future(f)

  def getConnection() = mongoConnection

  lazy val defaultDb = {
    debug("loading default db " + databaseName)
    val db = getConnection.getDB(databaseName)
    //db.authenticate()
    db
  }

  def getDatabase() = defaultDb


  def canEqual(other: Any): Boolean = other.isInstanceOf[MongoDBManager]

  override def equals(other: Any): Boolean = other match {
    case that: MongoDBManager =>
      (that canEqual this) &&
        databaseName == that.databaseName &&
        servers == that.servers
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(databaseName, servers)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }

  @deprecated("use `servers`")
  def setHost(hosts: String): Unit ={
    servers = hosts.split(",").map(_.trim).toList
  }

  @deprecated("do nothing")
  def setPort(port: Int): Unit = {}

}