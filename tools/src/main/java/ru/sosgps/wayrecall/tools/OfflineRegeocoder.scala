package ru.sosgps.wayrecall.tools

import java.util.Collections

import com.beust.jcommander.Parameter
import com.mongodb.DBCollection
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.core.GPSDataConversions
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import ru.sosgps.wayrecall.regeocoding.DirectNominatimRegeocoder
import ru.sosgps.wayrecall.utils._
import ru.sosgps.wayrecall.utils.concurrent.Semaphore

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.asScalaIterator
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
/**
  * Created by nickl on 13.07.16.
  */
object OfflineRegeocoder  extends CliCommand with grizzled.slf4j.Logging {


  @Parameter(names = Array("-d", "-db"), required = false, description = "database name, default loaded from config")
  var dbname: String = null

  @Parameter(names = Array("-f", "--from"), description = "from date (format 2013-08-28T00:00)", required = true)
  var from: String = null

  @Parameter(names = Array("-t", "--to"), description = "to date (format 2013-08-28T00:00)", required = true)
  var to: String = null

  @Parameter(names = Array("-u", "--uid"), description = "object uid")
  var uids: java.util.List[String] = Collections.emptyList()

  override val commandName: String = "regeocode"

  override def process(): Unit = {


    val props = Main.wrcPropertiesFromFile("packreceiver.properties")

    val datasource = new org.apache.tomcat.jdbc.pool.DataSource()
    datasource.setDriverClassName("org.postgresql.Driver")
    datasource.setUrl(props.getProperty("packreceiver.nominatim.url"))
    datasource.setUsername(props.getProperty("packreceiver.nominatim.user"))
    datasource.setPassword(props.getProperty("packreceiver.nominatim.password"))
    datasource.setMaxActive(props.getProperty("packreceiver.nominatim.maxActive", "4").toInt)

    try {


      val regeocoder = new DirectNominatimRegeocoder(datasource)


      val uidall = uids.toSeq

      require(uidall.nonEmpty, "uids or imeis must be set")

      val mongoDB = Main.getMdbm(Option(dbname)).getDatabase()

      def collectionFor(uid: String): DBCollection = {
        mongoDB.getCollection("objPacks." + uid)
      }

      def histories = {
        uidall.toStream.map(uid =>
          collectionFor(uid)
            .find(MongoDBObject("lon" -> MongoDBObject("$exists" -> true), "pn" -> MongoDBObject("$exists" -> false)) ++
              ("time" $lte parseDate(to) $gte parseDate(from)))

        )
      }

      val total = histories.map(_.count()).sum

      val packs = for ((x, i) <- histories.iterator.map(_.iterator()).flatten.zipWithIndex) yield {
        println("sending " + (i + 1) + "/" + total + " : " + x)
        x
      }

      val semaphore = new Semaphore(10)

      for (dbo <- packs) {

        semaphore.acquiriedFuture(regeocoder.getPosition(
          dbo.as[Double]("lon"), dbo.as[Double]("lat"))).onComplete {

          case Success(Right(positionName)) =>
            blocking(collectionFor(dbo.as[String]("uid")).update(dbo, $set("pn" -> positionName), false, false))

          case Success(Left(error)) => error.printStackTrace(System.out)
          case Failure(error) => error.printStackTrace(System.out)

        }

      }

      semaphore.acquire(10)
      semaphore.release()


    }
    finally {
      datasource.close()
    }

    System.exit(0)


  }


}
