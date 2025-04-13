package ru.sosgps.wayrecall.tools

import java.io.FileInputStream
import java.util.Properties

import com.beust.jcommander.{Parameters, Parameter}
import ru.sosgps.wayrecall.jcommanderutils.CliCommand
import ru.sosgps.wayrecall.monitoring.geozones.GeozonesStore

@Parameters(commandDescription = "loads kml file as geozones to specified user")
object KMLImport extends CliCommand {

  val commandName = "kmlimport"

  @Parameter(names = Array("-f", "--from"), description = "filename", required = true)
  var from: String = null

  @Parameter(names = Array("-u", "--user"), description = "username", required = true)
  var username: String = null

  def process() {

    val properties = new Properties()
    properties.load(new FileInputStream(System.getenv("WAYRECALL_HOME") + "/conf/global.properties"))

    val dataSource = new org.apache.tomcat.jdbc.pool.DataSource()
    dataSource.setDriverClassName("org.postgresql.Driver")
    dataSource.setUrl(properties.getProperty("global.defaultpg.url"))
    dataSource.setUsername(properties.getProperty("global.defaultpg.user"))
    dataSource.setPassword(properties.getProperty("global.defaultpg.password"))

    val geozonesStore = new GeozonesStore
    geozonesStore.pgStore = dataSource

    val kml = scala.xml.XML.loadFile(from)
    val placemarks = kml \ "Document" \ "Placemark"

    for (placemark <- placemarks) {
      val name = (placemark \ "name").head.text.trim
      println("name=" + name)
      val coordintes = placemark \\ "coordinates"
      require(coordintes.size == 1)
      val coordStrings = coordintes.head.text.trim.split(" ").toIndexedSeq.filterNot(_.isEmpty)
      //println("coordString="+coordString)
      val coords = coordStrings.map(cs => {
        val split = cs.split(",")
        (split(0).toDouble, split(1).toDouble)
      })
      println("coords=" + coords)
      try {
        geozonesStore.addGeozone(username, name, "#5599FF", coords)
      }
      catch {
        case e: Exception => e.printStackTrace()
      }
    }

  }


}
