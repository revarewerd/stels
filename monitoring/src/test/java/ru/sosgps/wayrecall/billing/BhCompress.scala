package ru.sosgps.wayrecall.billing

import ru.sosgps.wayrecall.core.MongoDBManager
import com.mongodb.casbah.Imports._
import java.io.ByteArrayOutputStream
import ru.sosgps.wayrecall.utils.io.DboWriter
import java.util.zip.GZIPOutputStream

/**
 * Created by nickl on 01.02.14.
 */
object BhCompress {

  def main(args: Array[String]) {

    val bhcollection = new MongoDBManager{
      databaseName = "Seniel-dev2"
    }.getDatabase()("balanceHistoryWithDetails")

    val alldata = bhcollection.find().toList
    bhcollection.drop()
    for (dbo <- alldata) {

      val details = dbo.as[BasicDBObject]("details")

      val out = new ByteArrayOutputStream()
      val writer = new DboWriter(new GZIPOutputStream(out))
      writer.write(details)
      writer.close()
      dbo.put("details",out.toByteArray)
      bhcollection.insert(dbo)
    }


  }

}
