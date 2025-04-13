/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.sosgps.wayrecall.billing

import com.mongodb.DBObject
import org.springframework.context.support.FileSystemXmlApplicationContext
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.core.ObjectsRepositoryReader
import java.io.OutputStream
import ru.sosgps.wayrecall.data.sleepers.SleepersDataStore

object SleeperDataTest {
  def main(args: Array[String]) {
    System.setProperty("wayrecall.skipCachePreload", "true")

    context = new FileSystemXmlApplicationContext("src/main/webapp/WEB-INF/applicationContext.xml")

    try {
      val latestData = context.getBean(classOf[SleepersDataStore]).getLatestData("o1603556978227868060").filter(_.latestMatchResult.flatMap(_.lbs).isDefined)
      for (sl <- latestData) {
        println(sl.latestMatchResult.get.lbs.get)
      }
      println(latestData.get.missTimeWarnings)
    }
    finally {
      context.close
    }
  }

  private[billing] var context: FileSystemXmlApplicationContext = null

}