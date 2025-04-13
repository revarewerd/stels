package ru.sosgps.wayrecall.utils

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject

/**
 * Created by ivan on 22.10.15.
 */
object DBQueryUtils {

//  def notRemoved(dbo: DBObject) = {
//    MongoDBObject("removed" -> MongoDBObject("$exists" -> false)) ++ dbo
//  }
//
//  val notRemovedQuery = MongoDBObject("removed" -> MongoDBObject("$exists" -> false))
//
//  implicit def utilsOnDBObject(dbo: DBObject) = new {
//    def notRemoved: DBObject =  {
//      DBQueryUtils.notRemoved(dbo)
//    }
//  }
//
//  implicit def utilsOnMongoDBObject(dbo: MongoDBObject) = new {
//    def notRemoved: DBObject = {
//      DBQueryUtils.notRemoved(dbo)
//    }
//  }
  val notRemoved = MongoDBObject("removed" -> MongoDBObject("$exists" -> false))
  val isRemoved = MongoDBObject("removed" -> true)
  def isRemoved(dbo: DBObject) = dbo.getAs[Boolean]("removed").getOrElse(false)
}
