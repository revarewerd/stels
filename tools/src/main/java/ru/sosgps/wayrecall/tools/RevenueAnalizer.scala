package ru.sosgps.wayrecall.tools

import java.io.PrintWriter
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZonedDateTime}
import java.util.Date

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import ru.sosgps.wayrecall.initialization.MultiserverConfig
import ru.sosgps.wayrecall.utils._

object RevenueAnalizer {

  def main(args: Array[String]): Unit = {

    // ssh -L 27017:localhost:27017  niks@wayrecall.ksb-stels.ru
    // mkdir wayrecall-server
    // sshfs -o allow_other,defer_permissions  niks@wayrecall.ksb-stels.ru:/home/niks wayrecall-server

    val config = new MultiserverConfig(Paths.get("/Users/nickl-mac/wayrecall-server/Wayrecallhome/conf"))
    val client = MongoClient()
    val begin = LocalDateTime.of(2016, 1, 1, 0, 0, 0).toMSKDate
    val end = LocalDateTime.of(2018, 1, 1, 0, 0, 0).toMSKDate
    for (instance <- config.instances
      //      .filter(_.isDefault)
    ) {
      val db = client.getDB(instance.dbconf.name)
      val balanceCollection = db("balanceHistoryWithDetails")
      val dealersCollection = db("dealers.balanceHistory")
      val accountsCollection = db("accounts")

      def account(id: AnyRef) = accountsCollection.findOneByID(id).map(_.get("name")).orNull

      val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu hh:mm:ss")

      def formatDate(date: Date) = dateTimeFormatter.format(ZonedDateTime.ofInstant(date.toInstant, MSK))

      {
        val writer = new PrintWriter(instance.name + ".csv")
        writer.println(List("Аккаунт", "Время", "Сумма", "Комментарий").mkString(", "))
        try {
          for (elem <- balanceCollection.find(MongoDBObject("timestamp" -> MongoDBObject("$gte" -> begin, "$lte" -> end), "ammount" -> MongoDBObject("$gte" -> 0)))
            .sort(MongoDBObject("timestamp" -> 1))) {
            writer.println(List(account(elem.get("account")), formatDate(elem.as[Date]("timestamp")), elem.get("ammount").tryLong / 100.0, elem.get("comment")).mkString(", "))
          }
        } finally writer.close()
      }

      if (dealersCollection.size > 0) {
        val writer = new PrintWriter(instance.name + "-dealers.csv")
        writer.println(List("Аккаунт", "Время", "Сумма", "Комментарий").mkString(", "))
        try {
          for (elem <- dealersCollection.find(MongoDBObject("timestamp" -> MongoDBObject("$gte" -> begin, "$lte" -> end), "type" -> "Зачислить"))
            .sort(MongoDBObject("timestamp" -> 1))) {
            writer.println(List(elem.get("dealer"), formatDate(elem.as[Date]("timestamp")), elem.get("ammount").tryLong / 100.0, elem.get("comment")).mkString(", "))
          }
        } finally writer.close()
      }

    }
  }

}
