package ru.sosgps.wayrecall.utils.io

import java.io._
import org.bson.{BasicBSONDecoder, BasicBSONEncoder}
import com.mongodb.{DefaultDBDecoder, DBObject}
import java.util.zip.GZIPInputStream

/**
  * Created by nickl on 27.01.14.
  */


class DboWriter(stream: OutputStream) extends Closeable with Flushable {

  def this(resCollName: String) = this(new BufferedOutputStream(new FileOutputStream(new File(resCollName))))

  val ecoder = new BasicBSONEncoder

  def write(dbos: DBObject*) {
    for (dbo <- dbos) {
      stream.write(ecoder.encode(dbo))
    }
  }

  def writeAndClose(dbos: DBObject*): Unit = {
    try write(dbos: _*) finally close()
  }

  def flush() = stream.flush()

  def close() {
    stream.close()
  }
}

class DboReader(stream: InputStream) extends Closeable {

  def this(resCollName: String) = this({
    if (resCollName.endsWith(".gz"))
      new GZIPInputStream(new BufferedInputStream(new FileInputStream(resCollName)))
    else
      new BufferedInputStream(new FileInputStream(new File(resCollName)))
  })

  val decoder = new BasicBSONDecoder

  val cb = new DefaultDBDecoder()

  def read(): DBObject = {
    try {
      cb.decode(stream, null)
    }
    catch {
      case e: EOFException => null
      case e: java.io.IOException => null
    }
  }

  def iterator: Iterator[DBObject] = Iterator.continually({
    val read1 = read()
    if (read1 == null && autoclose) close()
    read1
  }).takeWhile(null !=)

  var autoclose = true

  def setAutoClose(v: Boolean) = {
    autoclose = v
    this
  }

  def close() {
    stream.close()
  }
}
