// run as
// scala -nc -cp $WAYRECALL_HOME/bin/tools.jar  unansweredImeigrep.scala

import java.io.FileInputStream
import java.nio.file.{Files, Paths, FileSystem, Path}
import java.util.zip.ZipInputStream
import org.joda.time.DateTime
import scala.collection.JavaConversions.asScalaIterator
import scala.collection.mutable
import scala.io.Source

val d = Paths.get("/home/niks/packreceiver-logs")

val ld = new DateTime().minusDays(1).getMillis

val files = Files.newDirectoryStream(d).iterator().map(_.toFile)
  .filter(f => f.getName.startsWith("main") && f.lastModified() > ld && f.length() > 1)

//println(files.toList)

val strs = files.map(f => try {

  if(f.getName.endsWith("zip"))
  {
    val is = new ZipInputStream(new FileInputStream(f))
    is.getNextEntry
    Source.fromInputStream(is).getLines()
  }
  else
  Source.fromFile(f, "UTF8").getLines().toStream.iterator

}).flatten.filter(_.contains("IllegalImeiException"))

val pattern = """.*IllegalImeiException: no objects for imei=(\w+)""".r

val imeiSet = new mutable.HashSet[String]()

for (s <- strs) {
  s match {
    case pattern(imei) => imeiSet += imei
    case _ => println("unmatched:" + s)
  }
}

for (imei <- imeiSet) {
  println(imei)
}
