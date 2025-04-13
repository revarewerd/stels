package ru.sosgps.wayrecall.data

import java.util.Date

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.junit.{Assert, Test}
import org.springframework.context.ApplicationContext
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.GPSData
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.data.CachedPackageStore
import ru.sosgps.wayrecall.data.Posgenerator
import ru.sosgps.wayrecall.utils.durationAsJavaDuration

import scala.concurrent.duration.DurationInt


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array("/spring-test.xml"))
class CachedTest {

  val testUid = "o8325087825488753638"
  
  @Autowired
  var cp: CachedPackageStore = null


  @Test
  def test1 {

    val gen = new Posgenerator(testUid, 1415897528335L)
    cp.onGpsEvent(new GPSEvent(gen.last))
    
    gen.genParking(360 seconds, 6).foreach(g => {
      println(cp.getLatestFor(Iterable(testUid)))
      cp.onGpsEvent(new GPSEvent(g))
    })
  
    gen.genMoving(10 minutes, 20).foreach(g => {
      println(cp.getLatestFor(Iterable(testUid)))
      cp.onGpsEvent(new GPSEvent(g))
    })
  
    gen.genParking(30 minutes, 6).foreach(g => {
      println(cp.getLatestFor(Iterable(testUid)))
      cp.onGpsEvent(new GPSEvent(g))
    })

  }
  
}