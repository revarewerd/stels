package ru.sosgps.wayrecall

import java.io.File
import java.util.Collections
import javax.annotation.PostConstruct

import com.mongodb.casbah.commons.MongoDBObject
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.springframework.beans.factory.annotation.Autowired
import ru.sosgps.wayrecall.core.MongoDBManager
import ru.sosgps.wayrecall.utils.io.DboReader

import scala.beans.BeanProperty
import scala.collection.JavaConverters._


/**
 * Created by nickl on 15.04.14.
 */
class TestDataBaseFiller extends grizzled.slf4j.Logging{

  @Autowired
  var mdbm: MongoDBManager = null

  @BeanProperty
  var mongoDumps: java.util.List[String] = Collections.emptyList()

  @BeanProperty
  var dbName: String = null

  private lazy val db = if(dbName == null) mdbm.getDatabase() else  mdbm.getConnection().getDB(dbName)

  @PostConstruct
  def init() {

    debug("dropping and filling "+db.name)
    db.dropDatabase()

    for (entry <- mongoDumps.asScala) {

      if(entry.endsWith(".zip"))
      readZip(entry)
      else if(entry.endsWith("/"))
        new File(ClassLoader.getSystemResource(entry).getFile).listFiles().foreach(f => {
          debug("reading "+f)
          new DboReader(f.getAbsolutePath).iterator.foreach(
            dbo => db(f.getName.stripSuffix(".bson.gz")).insert(dbo)
          )
        })

    }

    def readZip(entry: String) {
      val stream = new ZipArchiveInputStream(this.getClass.getResourceAsStream(entry))
      try {
        for (ze <- Iterator.continually(stream.getNextEntry).takeWhile(null !=)) {
          info(ze.getName)
          val collName = ze.getName.stripSuffix(".bson").drop(ze.getName.lastIndexOf("/") + 1)
          if (collName.nonEmpty) {
            info("filling:" + collName)
            db(collName).remove(MongoDBObject.empty)
            new DboReader(stream).setAutoClose(false).iterator.grouped(300).foreach(dbo => {
              db(collName).insert(dbo: _*)
            })
          }
        }
      }
      finally
        stream.close()
    }
  }

}
