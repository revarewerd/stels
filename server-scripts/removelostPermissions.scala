#!/usr/bin/env scalas

/***
scalaVersion := "2.10.3"

unmanagedBase := new File("/home/niks/Wayrecallhome/bin/")

//libraryDependencies ++= Seq(
//"org.mongodb" % "casbah-core_2.10" % "2.6.4",
//"org.mongodb" % "casbah-commons_2.10" % "2.6.4"
//)

*/

 import com.mongodb.casbah.Imports._
    import ru.sosgps.wayrecall.utils._


    val db = MongoConnection().getDB("Seniel-dev2")


    for(permdbo <- db("usersPermissions").find()){

     val defined = permdbo.as[String]("recordType") match {
        case "object" => db("objects").findOneByID(permdbo.as[AnyRef]("item_id")).isDefined
        case "account" => db("accounts").findOneByID(permdbo.as[AnyRef]("item_id")).isDefined
        case _ => true
      }

      if(!defined) {
        println(permdbo)
        db("usersPermissions").remove(MongoDBObject("_id" -> permdbo.as[ObjectId]("_id")))
      }

    }




