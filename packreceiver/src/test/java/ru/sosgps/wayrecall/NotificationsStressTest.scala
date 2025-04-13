package ru.sosgps.wayrecall

import java.net.InetSocketAddress
import java.util.Date
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.{TimeUnit, Executors}

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory}
import org.jboss.netty.channel.socket.nio.{NioWorkerPool, NioClientSocketChannelFactory}
import org.jboss.netty.handler.codec.frame.FixedLengthFrameDecoder
import org.jboss.netty.handler.timeout.{WriteTimeoutHandler, ReadTimeoutHandler}
import org.jboss.netty.util.{HashedWheelTimer, ThreadNameDeterminer}
import ru.sosgps.wayrecall.core.{DbConf, GPSData, ObjectsRepositoryReader, MongoDBManager}
import com.mongodb.casbah.Imports._
import ru.sosgps.wayrecall.retranslators.{WialonNettyClientHandler, WialonRealTimeRetranslator}

import scala.util.Random


object NotificationsStressTest {

  def main(args: Array[String]) {

   val mdbm = new MongoDBManager(new DbConf("Seniel-dev2"))
   
    val uids = mdbm.getDatabase()("objects").find().map(_.as[String]("uid")).toIndexedSeq
    val reader = new ObjectsRepositoryReader
    reader.mdbm = mdbm

    val uidimei = uids.map(uid => (uid, reader.getMainTerminal(uid).map(_.as[String]("eqIMEI")))).collect({case (uid, Some(imei)) => (uid, imei)})

    println(uidimei)
    val timer = new HashedWheelTimer
    val mbootstrap = {
      val bootstrap = new ClientBootstrap(
        new NioClientSocketChannelFactory(
          Executors.newCachedThreadPool(), 1,
          new NioWorkerPool(Executors.newCachedThreadPool(), 1, new ThreadNameDeterminer {
            override def determineThreadName(currentThreadName: String, proposedThreadName: String): String = proposedThreadName + " wwwialonClient"
          })));

      // Set up the pipeline factory.
      bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
        def getPipeline: ChannelPipeline = Channels.pipeline(
          //new ReadTimeoutHandler(timer, 5, TimeUnit.SECONDS),
          new WriteTimeoutHandler(timer, 5, TimeUnit.SECONDS),
          new FixedLengthFrameDecoder(1),
          new WialonNettyClientHandler
        )
      });
      bootstrap
    }

    def rtr = {
      val wlrlt = new WialonRealTimeRetranslator(){
        override protected[this] lazy val bootstrap = mbootstrap
      }
      wlrlt.configure(Seq((new InetSocketAddress("localhost", 9087), (gps: GPSData) => true)))
      wlrlt
    }

    val rtrs = IndexedSeq.fill(400)(rtr)

    val rtrsIterator = Iterator.continually(rtrs.iterator).flatten

    val random = new Random()

    var i = 0
    while(true){
      val (uid, imei) = uidimei(random.nextInt(uidimei.size))
      rtrsIterator.next()(new GPSData(
        uid, imei,
        50.0 + 10.0*random.nextDouble(), 30.0 + 10.0*random.nextDouble(),
        new Date(),
        random.nextInt(10).toShort, random.nextInt(360).toShort, random.nextInt(15).toByte)
      )
      LockSupport.parkNanos(10000)
      //Thread.sleep(0,1000);
      i = i + 1
      if(i % 10000 == 0)
        println(i)

    }


  }

}
