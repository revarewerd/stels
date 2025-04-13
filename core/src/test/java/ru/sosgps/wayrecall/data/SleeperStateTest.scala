package ru.sosgps.wayrecall.data

import java.io.InputStream
import java.util.Date
import java.util.zip.GZIPInputStream

import com.mongodb.casbah.Imports

import org.joda.time.{DateTime, DateTimeUtils}
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.data.sleepers._
import ru.sosgps.wayrecall.sms.SMS
import ru.sosgps.wayrecall.utils.io.{DboReader, DboWriter}
import com.mongodb.casbah.Imports._

/**
 * Created by nickl on 25.08.14.
 */
class SleeperStateTest {

  @Test
  def export(): Unit = {
    val phone = "79163145291"
    export(phone)
  }

  @Test
  def test1(): Unit = {
    val smses = loadHistory("79857893468")
    withMockedTime(new DateTime(2014, 8, 25, 17, 55)) {
      val sd = new SleeperData("uiduid", Map("79857893468" -> smses))
      Assert.assertEquals(OK, sd.sleeperState)
    }

    withMockedTime(new DateTime(2014, 8, 31, 17, 55)) {
      val sd = new SleeperData("uiduid", Map("79857893468" -> smses))
      Assert.assertEquals(Warning(Map("79857893468" -> "Пропущено сообщение 26.08.2014 16:57:12")), sd.sleeperState)
    }

  }

  @Test
  def test2(): Unit = {
    val smses = loadHistory("79154890510").sortBy(_.sendDate)(Ordering[Date].reverse)
    withMockedTime(new DateTime(2014, 11, 27, 15, 50)) {
      val sd = new SleeperData("uiduid", Map("79154890510" -> smses))
      Assert.assertEquals(OK, sd.sleeperState)
    }
  }

  @Test
  def test3(): Unit = {
    val smses = loadHistory("79163551421").sortBy(_.sendDate)(Ordering[Date].reverse)
    withMockedTime(new DateTime(2015, 6, 29, 15, 50)) {
      val sd = new SleeperData("uiduid", Map("79163551421" -> smses))
      Assert.assertEquals(OK, sd.sleeperState)
    }
  }

  @Test
  def testUnknown(): Unit = {
    val smses = loadHistory("79857893468").take(1)
    val sd = new SleeperData("uiduid", Map("79857893468" -> smses))
    Assert.assertEquals(Unknown, sd.sleeperState)
  }

  @Test
  def testPaired(): Unit = {
    val smses = loadHistory("79163145291").take(4)

    smses.foreach(s => println(s.text))

    withMockedTime(new DateTime(2014, 8, 25, 17, 55)) {
      val sd = new SleeperData("uiduid", Map("79163145291" -> smses))
      println(sd.missTimeWarnings)
      println(sd.latestMatchResult)
      println(sd.time)
      Assert.assertEquals(OK, sd.sleeperState)
    }

    withMockedTime(new DateTime(2014, 9, 4, 17, 55)) {
      val sd = new SleeperData("uiduid", Map("79163145291" -> smses))
      Assert.assertEquals(Warning(Map("79163145291" -> "Пропущено сообщение 03.09.2014 4:33:03")), sd.sleeperState)
    }

  }

  private def export(phone: String) {
    val manager = new MongoDBManager()
    manager.databaseName = "Seniel-dev2"
    val data = manager.getDatabase()("smses").find(MongoDBObject("senderPhone" -> phone)).sort(MongoDBObject("sendDate" -> -1)).limit(50)
    val writer = new DboWriter(phone + ".bson")
    data.foreach(writer.write(_))
    writer.close()
  }

  private def loadHistory(s: String): Stream[SMS] = {
    val stream = this.getClass.getClassLoader.getResourceAsStream("ru/sosgps/wayrecall/data/" + s + ".bson.gz")
    val smses = loadHistory(stream)
    smses
  }

  def loadHistory(stream: InputStream): Stream[SMS] = {
    val reader = new DboReader(new GZIPInputStream(stream))
    val smses = reader.iterator.map(dbo => SMS.fromMongoDbObject(dbo)).toStream.force
    Assert.assertTrue(smses.nonEmpty)
    reader.close()
    smses
  }

  private def withMockedTime(time: DateTime)(f: => Any): Unit = {
    try {
      DateTimeUtils.setCurrentMillisFixed(time.getMillis)
      f
    } finally {
      DateTimeUtils.setCurrentMillisSystem()
    }
  }

}
