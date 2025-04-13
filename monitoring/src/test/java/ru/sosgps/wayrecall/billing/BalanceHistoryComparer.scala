package ru.sosgps.wayrecall.billing

import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.context.ApplicationContext
import org.apache.xbean.spring.context.FileSystemXmlApplicationContext
import ru.sosgps.wayrecall.core.{ObjectsRepositoryReader, MongoDBManager}
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.joda.time.DateTime
import ru.sosgps.wayrecall.core.finance.TariffPlans

/**
 * Created by nickl on 04.03.14.
 */
object BalanceHistoryComparer {

  val mdbm = new MongoDBManager()
  mdbm.databaseName = "Seniel-dev2"

  def main(args: Array[String]) {
    //val ctx: ApplicationContext = new FileSystemXmlApplicationContext("billing/src/main/webapp/WEB-INF/applicationContext.xml")
    //val mdbm = ctx.getBean(classOf[MongoDBManager])

    val or = new ObjectsRepositoryReader
    or.mdbm = mdbm

    val tp = new TariffPlans
    tp.mdbm = mdbm
    tp.or = or

    val data = (for (acc <- mdbm.getDatabase()("accounts")) yield {

      //val accId = new ObjectId("522df121e4b0a3d0ac39ba0f")
      val accId = acc.as[ObjectId]("_id")

      //println("acc=" + acc.as[String]("name"))

      val fromDate = new DateTime(2014, 2, 1, 0, 0).toDate
      val toDate = new DateTime(2014, 3, 1, 0, 0).toDate
      def loadFebHistory(collection1: String) = {
        mdbm.getDatabase().apply(collection1).find(
          MongoDBObject("account" -> accId, "type" -> "dailypay") ++ ("timestamp" $gte fromDate $lte toDate),
          MongoDBObject("ammount" -> 1)
        ).sort(MongoDBObject("timestamp" -> 1)).map(_.as[Long]("ammount")).sum
      }

      val a = loadFebHistory("balanceHistory")
      //println(b)
      //b.foreach(println)

      val b = loadFebHistory("balanceHistoryWithDetails")
      //println(a)
      //println(a - b)
      //a.foreach(println)
      //println()

      (acc, Math.abs(a), Math.abs(b), tp.calcTotalCost(acc))
    }).toIndexedSeq

    for ((acc, a, b, tc) <- data.sortBy(t => t._4 - t._2)) {
      println("acc=" + acc.as[String]("name"))
      println("a=" + a)
      println("b=" + b)
      println("tc=" + tc)
      println("a-b=" + (a - b))
      println("tc - b=" + (tc - b))
    }


  }

}
