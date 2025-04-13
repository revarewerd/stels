package ru.sosgps.wayrecall.odsmosru

import java.io.File
import java.nio.file.{Path, Files, Paths}
import java.util.Date
import java.util.concurrent.{Executor, Executors}

import com.google.common.util.concurrent.MoreExecutors
import org.junit.{Assert, Test}
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.utils.concurrent.{SameThreadExecutorService, ScalaExecutorService}

import scala.collection.immutable.TreeSet
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.duration.DurationLong
import scala.concurrent.{future, ExecutionContext, Await, Future}
import scala.util.control

/**
  * Created by nickl on 07.10.14.
  */
class TemporaryStorageTest extends grizzled.slf4j.Logging {


  @Test
  def testWriteOne(): Unit = {

    val td = Files.createTempDirectory("TemporaryStorageTest")
    val ts = new TemporaryStorage(td)
    Assert.assertTrue(td.toFile.list().isEmpty)

    val gpsgenerator = gpsGenerator()
    val firstPack = gpsgenerator.take(10).toList

    firstPack.foreach(ts.store)
    val restored = ts.retrieveBunch.toList
    Assert.assertArrayEquals(firstPack.toArray[AnyRef], restored.toArray[AnyRef])
    assertEmptyDir(td)

  }

  @Test
  def testWriteMultiple(): Unit = {

    val td = Files.createTempDirectory("TemporaryStorageTest")
    val ts = new TemporaryStorage(td)
    Assert.assertTrue(ts.retrieveBunch.isEmpty)
    Assert.assertTrue(td.toFile.list().isEmpty)

    val gpsgenerator = gpsGenerator()
    val firstPack = gpsgenerator.take(10).toList
    ts.storeAll(firstPack)
    val restored = ts.retrieveBunch
    restored.foreach(ts.store)
    Assert.assertArrayEquals(firstPack.toArray[AnyRef], ts.retrieveBunch.toArray[AnyRef])

    assertEmptyDir(td)

  }

  @Test
  def testWriteParallelMultiple(): Unit = {

    val td = Files.createTempDirectory("TemporaryStorageTest")
    val ts = new TemporaryStorage(td)
    Assert.assertTrue(ts.retrieveBunch.isEmpty)
    Assert.assertTrue(td.toFile.list().isEmpty)

    val gpsgenerator = gpsGenerator()
    val firstPack = gpsgenerator.take(10).toList
    val pool = new ScalaExecutorService(Executors.newFixedThreadPool(10))
    try {
      implicit val ex = ExecutionContext.fromExecutor(pool)
      Await.result(Future.sequence(firstPack.map(gps => pool.future(ts.store(gps)))), 20.seconds)
      Assert.assertEquals(firstPack.toSet, ts.retrieveBunch.toSet)

      assertEmptyDir(td)
    }
    finally pool.shutdown()

  }

  @Test
  def testRepeatedlyResender(): Unit = {

    val pool = new ScalaExecutorService(Executors.newFixedThreadPool(10))
    try {
      //val pool = new SameThreadExecutorService
      implicit val ex = ExecutionContext.fromExecutor(pool)

      val td = Files.createTempDirectory("TemporaryStorageTest")
      val ts = new TemporaryStorage(td)

      val aggregated = new ArrayBuffer[GPSData]() with mutable.SynchronizedBuffer[GPSData]

      @volatile var doError = false;

      def send(gps: GPSData): Unit = {
        if (doError) {
          debug("error receiving " + gps)
          throw new Exception("test error requested")
        }
        debug("receiving " + gps)
        aggregated += gps
      }

      val rrTasks = new ArrayBuffer[Future[Unit]]() with mutable.SynchronizedBuffer[Future[Unit]]
      val rr = new RepeatedlyResender(ts, send, new Executor {
        override def execute(command: Runnable): Unit = {
          debug("executing  RepeatedlyResender")
          rrTasks += future {
            command.run()
          }
        }
      }, MoreExecutors.sameThreadExecutor(), 1)

      val gpsgenerator = gpsGenerator()
      val firstPack = gpsgenerator.take(10).toList
      val errPack = gpsgenerator.take(10).toList
      val secondPack = gpsgenerator.take(10).toList
      val thirdPack = gpsgenerator.take(2).toList

      def sendAsync(gps: GPSData): Future[Unit] = {
        future {
          control.Exception.ignoring(classOf[Exception]) {
            rr.safeSend(gps)
          }
        }
      }
      val firstPackFuture = firstPack.map(gps => sendAsync(gps))
      doError = true
      val errFutures = errPack.map(gps => sendAsync(gps))
      Await.result(errFutures.head, 20.seconds)

      doError = false
      rr.lastResendTime = 0
      val secondPackFuture = secondPack.map(gps => sendAsync(gps))
      Await.result(Future.sequence(firstPackFuture ++ secondPackFuture ++ errFutures), 20.seconds)
      debug("rrTasks count1:" + rrTasks.size)
      Await.result(Future.sequence(rrTasks.toList), 20.seconds)

      rr.lastResendTime = 0
      val thirdPackFuture = thirdPack.map(gps => sendAsync(gps))
      Await.result(Future.sequence(thirdPackFuture), 20.seconds)
      debug("rrTasks count2:" + rrTasks.size)
      Await.result(Future.sequence(rrTasks.toList), 20.seconds)

      val sent = (firstPack ++ errPack ++ secondPack ++ thirdPack).toSet
      val received = aggregated.toSet

      //Assert.assertEquals(sent.size, received.size)
      Assert.assertEquals(sent.map(_.data.get("testid").toString.toInt).to[TreeSet], received.map(_.data.get("testid").toString.toInt).to[TreeSet])
      Assert.assertEquals(sent, received)

      assertEmptyDir(td)
    }
    finally pool.shutdown()

  }

  def gpsGenerator(prefix: String = ""): Iterator[GPSData] = {
    Iterator.from(1).map(i => {
      val data = new GPSData("testuid", "testimei", 1.0 + i * 0.0001, 1.0, new Date(1 + i), 1, 1, 1)
      data.data.put("testid", prefix + i)
      data
    })
  }

  private def assertEmptyDir(td: Path) {
    Assert.assertFalse(td.toString + " must be empty but: " + td.toFile.list().mkString(", "), td.toFile.listFiles().exists(_.exists()))
  }
}
